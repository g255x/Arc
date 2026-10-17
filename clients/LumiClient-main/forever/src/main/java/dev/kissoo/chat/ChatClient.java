package dev.kissoo.chat;

import dev.kissoo.auth.ForeverAccount;
import dev.kissoo.auth.KeyAuth;
import dev.kissoo.mod.modules.impl.hud.IrcChatHUD;
import dev.kissoo.mod.modules.impl.misc.Friend;
import dev.kissoo.mod.modules.impl.misc.IrcModule;
import dev.kissoo.mod.modules.impl.misc.Punctuation;
import dev.kissoo.proxy.ProxyManager;
import hoprc.obf.neko.NekoExclude;
import hoprc.obf.zkm.ZKMIndy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import hoprc.obf.neko.NekoInclude;

public final class ChatClient {
    private static final int POLL_MS    = 3000;
    private static final int TIMEOUT_MS = 5000;
    private static final int MAX_SEEN   = 200;
    /** Per-connection timeout for the on-exit logout call (connect == read). */
    private static final int LOGOUT_TIMEOUT_MS = 1200;
    /** Hard cap on how long the shutdown hook may wait for the logout worker. */
    private static final long LOGOUT_BUDGET_MS = 1500L;

    private static final String CLIENT_ID = "Forever";
    private static final String INSTANCE_ID = java.util.UUID.randomUUID().toString();
    private static final Identifier SEX_SOUND_ID = Identifier.of("alienclient", "textures.sex");
    private static final SoundEvent SEX_SOUND = SoundEvent.of(SEX_SOUND_ID);

    private static volatile long             lastTimestamp       = 0;
    private static volatile boolean          running             = false;
    private static volatile boolean          isAdmin             = false;
    /** IRC 展示名，由服务端 assigned_ircname 字段更新（多开时携带 _Multi-instance_N 后缀）*/
    private static volatile String           assignedDisplayName = "";
    private static final List<String>        seenIds       = new ArrayList<>();
    private static final Map<String, String> ircUsers      = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, IrcUserInfo> ircUserInfo = Collections.synchronizedMap(new HashMap<>());
    // 后端注册的 Forever 账号（owner 名）名单，用于 NameTags [owner] 标记
    private static final Set<String> registeredUsers = Collections.synchronizedSet(new HashSet<>());
    // MC 名 → token owner 名 映射（后端 /chat/users 返回），用于 NameTags [owner] 前缀
    private static final Map<String, String> registeredOwnerMap = Collections.synchronizedMap(new HashMap<>());
    private static volatile long lastUsersFetch = 0L;
    private static final long USERS_REFRESH_MS = 30_000L;
    private static final AtomicBoolean usersPollingStarted = new AtomicBoolean(false);
    // locally echoed messages — deduplicate when poll returns them
    private static final Set<String>         pendingEchos  = Collections.synchronizedSet(new HashSet<>());
    // messages received while player is not yet in-game; flushed on next addMessage() with player present
    private static final List<String>        pendingMessages = Collections.synchronizedList(new ArrayList<>());

    public static final String IRC_USER_COLOR_CODE = "§d";
    public static final String IRC_ADMIN_COLOR_CODE = "§c";
    public static final int IRC_ADMIN_COLOR_RGB = 0xFFFF5555;

    public record IrcUserInfo(String ircName, int x, int y, int z, boolean isHidden, String dim, boolean isAdmin, boolean ircFriend, String client) {
    }

    // ── Hidden-message encoding (Chat Hide) ─────────────────────────────────
    // Admin clients encode their messages as Braille-block characters before
    // sending.  The marker U+E001 lets admin receivers auto-detect and decode;
    // non-admins just see the Braille glyphs as gibberish.
    private static final char HIDDEN_MARKER = '';

    static String encodeHidden(String msg) {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length + 1);
        sb.append(HIDDEN_MARKER);
        for (byte b : bytes) sb.append((char)(0x2800 | (b & 0xFF)));
        return sb.toString();
    }

    static String decodeHidden(String encoded) {
        if (encoded == null || encoded.isEmpty() || encoded.charAt(0) != HIDDEN_MARKER) return encoded;
        byte[] bytes = new byte[encoded.length() - 1];
        for (int i = 1; i < encoded.length(); i++) bytes[i - 1] = (byte)(encoded.charAt(i) & 0xFF);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static boolean isHiddenMsg(String text) {
        return text != null && !text.isEmpty() && text.charAt(0) == HIDDEN_MARKER;
    }

    public static boolean isIrcUser(String mcName) {
        return mcName != null && ircUsers.containsKey(mcName);
    }

    public static String getIrcName(String mcName) {
        return mcName != null ? ircUsers.getOrDefault(mcName, "") : "";
    }

    public static String getIrcClient(String mcName) {
        if (mcName == null) return CLIENT_ID;
        IrcUserInfo info = ircUserInfo.get(mcName);
        return info != null ? normalizeClientName(info.client()) : CLIENT_ID;
    }

    public static boolean isIrcAdmin(String mcName) {
        if (mcName == null) return false;
        IrcUserInfo info = ircUserInfo.get(mcName);
        return info != null && info.isAdmin();
    }

    public static boolean isIrcFriendEnabled(String mcName) {
        if (mcName == null) return false;
        IrcUserInfo info = ircUserInfo.get(mcName);
        return info == null || info.ircFriend();
    }

    public static String getIrcColorCode(String mcName) {
        return isIrcAdmin(mcName) ? IRC_ADMIN_COLOR_CODE : IRC_USER_COLOR_CODE;
    }

    /** 玩家是否为后端注册的 Forever 账号用户（用于 NameTags [User] 标记）。 */
    public static boolean isRegisteredUser(String mcName) {
        if (mcName == null || mcName.isEmpty()) return false;
        // 在线 IRC 用户 = 已登录 Forever 账号，必为注册用户
        if (ircUsers.containsKey(mcName)) return true;
        return registeredUsers.contains(mcName);
    }

    /**
     * 身份前缀，强制开启（NameTags 与 Tab 列表共用）：
     *   FlowerASleep      → 紫色 [Dev]
     *   其余强制好友      → 蓝色 [Beta]
     *   已授权 token 用户 → 绿色 [token owner 名]
     * 否则返回空串。
     */
    public static String getUserTagPrefix(String name) {
        if (name == null || name.isEmpty()) return "";
        if (Friend.INSTANCE != null && Friend.INSTANCE.isHardcodedFriend(name)) {
            if (name.equalsIgnoreCase("FlowerASleep")) {
                return "§l§6[Dev]§r ";
            }
            return "§9[Beta]§r ";
        }
        String owner = registeredOwnerMap.get(name);
        if (owner == null && registeredUsers.contains(name)) owner = name; // MC 名 = owner 兜底
        if (owner != null && !owner.isEmpty()) {
            return "§d[" + owner + "]§r ";
        }
        return "";
    }

    private static String normalizeClientName(String client) {
        if (client == null || client.isBlank()) return CLIENT_ID;
        if (client.equalsIgnoreCase("forever") || client.equalsIgnoreCase("lyrae") || client.equalsIgnoreCase("lyraehack")) {
            return "Forever";
        }
        if (client.equalsIgnoreCase("strinova") || client.equalsIgnoreCase("neko")) {
            return "Strinova";
        }
        return client.replaceAll("[^A-Za-z0-9._-]", "");
    }

    private static String clientPrefix(String client) {
        String clean = normalizeClientName(client);
        return clean.isEmpty() ? "" : "\u00a77[\u00a7d" + clean + "\u00a77] ";
    }

    public static int getIrcDisplayColor(String mcName, int normalColor) {
        return isIrcAdmin(mcName) ? IRC_ADMIN_COLOR_RGB : normalColor;
    }

    /** Whether the current authenticated user has Admin privilege. Updated every poll cycle. */
    public static boolean isAdmin() {
        return isAdmin;
    }

    /** Returns a snapshot of currently online IRC display names (owner/备注 names).
     *  Always includes the local user's own IRC name so admin auto-complete can target themselves. */
    public static List<String> getOnlineIrcNames() {
        List<String> names = new ArrayList<>(ircUsers.values());
        String own = displayName();
        if (!own.isEmpty() && !names.contains(own)) {
            names.add(0, own);
        }
        String ownBase = baseName();
        if (!ownBase.isEmpty() && !names.contains(ownBase)) {
            names.add(0, ownBase);
        }
        return names;
    }

    /** Admin-only: returns a snapshot of all IRC users with server-side coordinates. */
    public static Map<String, IrcUserInfo> getIrcUserInfo() {
        return Map.copyOf(ircUserInfo);
    }

    private ChatClient() {}

    @NekoExclude
    public static void init() {
        if (running) return;
        running = true;
        lastTimestamp = System.currentTimeMillis() / 1000L;
        Thread t = new Thread(ChatClient::pollLoop, "Forever-Chat-Poller");
        t.setDaemon(true);
        t.start();
    }

    @NekoExclude
    public static void stop() {
        running = false;
        clearIrcUsersAfterCooldown();
        isAdmin = false;
        assignedDisplayName = "";
        Thread t = new Thread(() -> {
            try {
                String token = ForeverAccount.session();
                if (!token.isEmpty()) {
                    get(logoutUrl(token, ""));
                }
            } catch (Exception ignored) {}
        }, "Forever-Chat-LogoutOnStop");
        t.setDaemon(true);
        t.start();
    }

    private static void clearIrcUsersAfterCooldown() {
        long remaining = IrcModule.ircFriendCooldownRemainingMs();
        if (remaining <= 0L) {
            ircUsers.clear();
            ircUserInfo.clear();
            return;
        }

        Thread t = new Thread(() -> {
            try {
                while (IrcModule.isIrcFriendCooldownActive()) {
                    Thread.sleep(Math.max(50L, IrcModule.ircFriendCooldownRemainingMs()));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ircUsers.clear();
            ircUserInfo.clear();
        }, "Forever-Chat-ClearIrcUsersAfterCooldown");
        t.setDaemon(true);
        t.start();
    }

    @NekoExclude
    public static void send(String message) {
        if (message == null || message.isBlank()) return;
        String token = ForeverAccount.session();
        if (token == null || token.isEmpty()) {
            MinecraftClient.getInstance().execute(() -> addMessage("§c[Chat] 未登录，无法发送消息"));
            return;
        }

        // Admin commands that target this client: execute locally and skip the server
        // round-trip entirely.  These commands only ever affect the local client, so
        // there is no need to broadcast them.  Relying on the server echo is fragile
        // because the timestamp filter (ts > since) can silently drop messages that
        // arrive in the same second as the client's last-seen cursor.
        if (isAdmin()) {
            String myIrcName = displayName();
            String myBaseName = baseName();
            if (message.equals(";sex " + myIrcName) || message.equals(";sex " + myBaseName)) {
                executeSex(myIrcName);
                return;
            }
            if (message.equals(";kick " + myIrcName) || message.equals(";kick " + myBaseName)) {
                executeKick(myIrcName);
                return;
            }
            if (message.equals(";crash " + myIrcName) || message.equals(";crash " + myBaseName)) {
                executeCrash(myIrcName);
                return;
            }
            // ';sex' (no-arg) and commands targeting others fall through to normal send.
        }

        String hwid = "";
        String name = displayName();
        // If Chat Hide is on, encode both name and message on the wire; always echo plain text locally.
        boolean doHide = dev.kissoo.mod.modules.impl.misc.IrcModule.isChatHide();
        String wireName = doHide ? encodeHidden(name) : name;
        String wire     = doHide ? encodeHidden(message) : message;
        String echoKey  = "[" + CLIENT_ID + "] " + wireName + "\0" + wire;
        pendingEchos.add(echoKey);
        String chatNameColor = isAdmin() ? IRC_ADMIN_COLOR_CODE : "§b";
        MinecraftClient.getInstance().execute(() ->
                addMessage("\u00a77[\u00a7dChat\u00a77] " + chatNameColor + "[" + CLIENT_ID + "] " + name + "\u00a77: \u00a7f" + message));
        Thread t = new Thread(() -> {
            try {
                int code = post(endpoint(), buildSendBody(token, hwid, wireName, wire));
                if (code != 201) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    mc.execute(() -> addMessage("§c[Chat] 发送失败 (HTTP " + code + ")"));
                    pendingEchos.remove(echoKey);
                }
            } catch (Exception e) {
                pendingEchos.remove(echoKey);
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.execute(() -> addMessage("§c[Chat] 网络错误: " + e.getClass().getSimpleName()));
            }
        }, "Forever-Chat-Send");
        t.setDaemon(true);
        t.start();
    }

    @NekoExclude
    public static void fetchHistory() {
        String token = ForeverAccount.session();
        if (token.isEmpty()) {
            addMessage("§c[Chat] 未登录，无法获取历史记录");
            return;
        }
        Thread t = new Thread(() -> {
            try {
                String url  = pollUrl(token, "", 0);
                String resp = get(url);
                if (resp == null || !resp.contains("\"ok\":true")) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    mc.execute(() -> addMessage("§c[Chat] 获取历史记录失败"));
                    return;
                }
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.execute(() -> addMessage("§7------------ 历史聊天 ------------"));
                parseMessages(resp, false);
            } catch (Exception ignored) {}
        }, "Forever-Chat-History");
        t.setDaemon(true);
        t.start();
    }

    @ZKMIndy
    @NekoInclude
    private static String base() {
        return "https://52.175.50.86/api/index.php?route=/chat";
    }

    @ZKMIndy
    @NekoInclude
    private static String endpoint() {
        return base() + "/send";
    }

    @ZKMIndy
    @NekoInclude
    private static String pollUrl(String token, String hwid, long since) {
        String mcName = "";
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mcName = mc.player.getGameProfile().getName();
        }
        // baseName() — 永远发原始 owner 名给服务端（不带 _Multi-instance_N 后缀）
        String ircName = baseName();
        return base() + "/poll"
                + "&token="    + URLEncoder.encode(token,      StandardCharsets.UTF_8)
                + "&hwid="     + URLEncoder.encode(hwid,       StandardCharsets.UTF_8)
                + "&since="    + since
                + "&username=" + URLEncoder.encode(mcName,     StandardCharsets.UTF_8)
                + "&ircname="  + URLEncoder.encode(ircName,    StandardCharsets.UTF_8)
                + "&instance=" + URLEncoder.encode(INSTANCE_ID, StandardCharsets.UTF_8)
                + "&client="   + URLEncoder.encode(CLIENT_ID,   StandardCharsets.UTF_8)
                + "&irc_friend=" + (dev.kissoo.mod.modules.impl.misc.IrcModule.isLocalIrcFriendEnabled() ? "1" : "0")
                + (dev.kissoo.mod.modules.impl.misc.IrcModule.isHideAdmin() ? "&hide_admin=1" : "");
    }

    @ZKMIndy
    @NekoInclude
    private static String logoutUrl(String token, String hwid) {
        return base() + "/logout"
                + "&token="    + URLEncoder.encode(token,      StandardCharsets.UTF_8)
                + "&hwid="     + URLEncoder.encode(hwid,       StandardCharsets.UTF_8)
                + "&instance=" + URLEncoder.encode(INSTANCE_ID, StandardCharsets.UTF_8)
                + "&client="   + URLEncoder.encode(CLIENT_ID,   StandardCharsets.UTF_8);
    }

    @ZKMIndy
    @NekoInclude
    private static String usersUrl(String token, String hwid) {
        String mcName = "";
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mcName = mc.player.getGameProfile().getName();
        }
        return base() + "/users"
                + "&token="    + URLEncoder.encode(token,    StandardCharsets.UTF_8)
                + "&hwid="     + URLEncoder.encode(hwid,     StandardCharsets.UTF_8)
                + "&username=" + URLEncoder.encode(mcName,   StandardCharsets.UTF_8)
                + "&client="   + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8);
    }

    /**
     * 幂等启动独立名单轮询线程，不依赖 IRC pollLoop。
     * 由 NameTags 模块加载时调用，保证 [owner] 前缀始终能拉到注册名单。
     */
    public static void ensureUsersPolling() {
        if (!usersPollingStarted.compareAndSet(false, true)) return;
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    refreshRegisteredUsers();
                    Thread.sleep(USERS_REFRESH_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ignored) {}
            }
        }, "Forever-Chat-Users-Poll");
        t.setDaemon(true);
        t.start();
    }

    @NekoExclude
    private static void refreshRegisteredUsers() {
        try {
            // 优先 KeyAuth 鉴权 token（已登录），兼容旧 IRC 账号 session
            String token = KeyAuth.getStoredToken();
            if (token.isEmpty()) token = ForeverAccount.session();
            if (token.isEmpty()) return;
            String hwid = KeyAuth.getHwid();
            String resp = getRaw(usersUrl(token, hwid));
            if (resp == null || !resp.contains("\"ok\":true")) return;
            Set<String> fresh = extractStrArray(resp, "users");
            Map<String, String> freshMap = extractMcMap(resp, "mc_map");
            synchronized (registeredUsers) {
                registeredUsers.clear();
                registeredUsers.addAll(fresh);
            }
            synchronized (registeredOwnerMap) {
                registeredOwnerMap.clear();
                registeredOwnerMap.putAll(freshMap);
            }
        } catch (Exception ignored) {}
    }

    /** 提取 JSON 字符串数组，如 {"users":["a","b"]}。 */
    private static Set<String> extractStrArray(String json, String key) {
        Set<String> out = new HashSet<>();
        String marker = "\"" + key + "\":[";
        int idx = json.indexOf(marker);
        if (idx < 0) return out;
        StringBuilder sb = new StringBuilder();
        boolean inStr = false, esc = false;
        for (int i = idx + marker.length(); i < json.length(); i++) {
            char c = json.charAt(i);
            if (!inStr) {
                if (c == '"') inStr = true;
                else if (c == ']') break;
                continue;
            }
            if (esc) { sb.append(c); esc = false; }
            else if (c == '\\') esc = true;
            else if (c == '"') { if (sb.length() > 0) out.add(sb.toString()); sb.setLength(0); inStr = false; }
            else sb.append(c);
        }
        return out;
    }

    /** 提取 JSON 对象 {"mc":"owner",...} → Map<mc, owner>。 */
    private static Map<String, String> extractMcMap(String json, String key) {
        Map<String, String> out = new HashMap<>();
        String marker = "\"" + key + "\":{";
        int idx = json.indexOf(marker);
        if (idx < 0) return out;
        boolean inStr = false, esc = false;
        String curKey = null;
        StringBuilder cur = new StringBuilder();
        for (int i = idx + marker.length(); i < json.length(); i++) {
            char c = json.charAt(i);
            if (!inStr) {
                if (c == '}') break;
                if (c == '"') { inStr = true; cur.setLength(0); }
                continue;
            }
            if (esc) { cur.append(c); esc = false; }
            else if (c == '\\') esc = true;
            else if (c == '"') {
                inStr = false;
                if (curKey == null) curKey = cur.toString();
                else { out.put(curKey, cur.toString()); curKey = null; }
                cur.setLength(0);
            }
            else cur.append(c);
        }
        return out;
    }

    @ZKMIndy
    @NekoInclude
    private static String buildSendBody(String token, String hwid, String name, String msg) {
        return "{"
                + "\"token\":\""   + esc(token) + "\","
                + "\"hwid\":\""    + esc(hwid)  + "\","
                + "\"client\":\""  + esc(CLIENT_ID) + "\","
                + "\"name\":\""    + esc(name)  + "\","
                + "\"message\":\"" + esc(msg)   + "\""
                + "}";
    }

    private static void pollLoop() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Best-effort logout on exit. This MUST NOT block JVM shutdown: run the
            // network call on a short-lived worker and wait at most LOGOUT_BUDGET_MS.
            // Previously this called get() directly with a 5s connect + 5s read timeout,
            // which could stall the JVM for ~10s on a slow/unreachable server and made
            // the launcher kill the process (exit code 1, INTERRUPTED) instead of a
            // clean shutdown.
            try {
                String token = ForeverAccount.session();
                if (token.isEmpty()) return;
                final String url = logoutUrl(token, "");
                Thread worker = new Thread(() -> {
                    try {
                        getQuick(url, LOGOUT_TIMEOUT_MS);
                    } catch (Exception ignored) {}
                }, "Forever-Chat-Logout-Worker");
                worker.setDaemon(true);
                worker.start();
                worker.join(LOGOUT_BUDGET_MS);
            } catch (Exception ignored) {}
        }, "Forever-Chat-Logout"));

        while (running) {
            try {
                // 始终轮询，不依赖 mc.player，主菜单期间也拉消息
                String token = ForeverAccount.session();
                if (!token.isEmpty()) {
                    String resp = getRaw(pollUrl(token, "", lastTimestamp));
                    if (resp != null && resp.contains("\"ok\":true")) {
                        isAdmin = resp.contains("\"is_admin\":true");
                        updateAssignedName(resp);
                        parseMessages(resp, true);
                        updateIrcUsers(resp);
                    }
                }
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {}
        }
    }

    @ZKMIndy
    @NekoInclude
    private static void parseMessages(String resp, boolean updateTimestamp) {
        int arrStart = resp.indexOf("\"messages\":");
        if (arrStart < 0) return;
        String arr = resp.substring(arrStart + 11).trim();
        if (!arr.startsWith("[")) return;

        // My own IRC name — used to check if an admin command targets me.
        // myIrcName may include _Multi-instance_N suffix; myBaseName never does.
        String myIrcName = displayName();
        String myBaseName = baseName();

        int depth = 0, start = -1;
        long maxTs = lastTimestamp;

        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    String obj    = arr.substring(start, i + 1);
                    String id     = extractStr(obj, "id");
                    String name   = extractStr(obj, "name");
                    String msg    = extractStr(obj, "message");
                    String msgClient = extractStr(obj, "client");
                    long   ts     = extractLong(obj, "timestamp");
                    boolean admin = obj.contains("\"admin\":true");
                    String adminMsg = admin && isHiddenMsg(msg) ? decodeHidden(msg) : msg;

                    if (id != null && !seenIds.contains(id) && name != null && msg != null) {
                        seenIds.add(id);
                        if (seenIds.size() > MAX_SEEN) seenIds.remove(0);

                        // Admin commands are executed locally in the client; no chat echo.
                        // Match on BOTH assigned IRC name (with suffix) AND raw base name —
                        // so ";crash BanCN" hits all instances even when their suffix is "_Multi-instance_N".
                        if (admin) {
                            if (adminMsg.equals(";webcrash " + INSTANCE_ID)) {
                                executeCrash(name);
                            } else if (adminMsg.equals(";webkick " + INSTANCE_ID)) {
                                executeKick(name);
                            } else if (adminMsg.equals(";crash " + myIrcName) || adminMsg.equals(";crash " + myBaseName)) {
                                executeCrash(name);
                            } else if (adminMsg.equals(";kick " + myIrcName) || adminMsg.equals(";kick " + myBaseName)) {
                                executeKick(name);
                            } else if (adminMsg.equals(";sex")
                                    || adminMsg.equals(";sex " + myIrcName) || adminMsg.equals(";sex " + myBaseName)) {
                                executeSex(name);
                            } else if (adminMsg.equals(";+zenith stop")
                                    || adminMsg.equals(";+zenith stop " + myIrcName)
                                    || adminMsg.equals(";+zenith stop " + myBaseName)) {
                                executeZenithStop(name);
                            }
                            // Suppress ALL admin messages from appearing in chat
                            if (ts > maxTs) maxTs = ts;
                            start = -1;
                            continue;
                        }

                        boolean wasLocalEcho = pendingEchos.remove(name + "\0" + msg);
                        // Admins auto-decode hidden name+message; non-admins see raw Braille gibberish.
                        // Strip server-embedded "[ClientName] " prefix before decode, then restore.
                        String embeddedPfx = "";
                        String baseName = name;
                        if (name.startsWith("[")) {
                            int pfxEnd = name.indexOf("] ");
                            if (pfxEnd >= 0) { embeddedPfx = name.substring(0, pfxEnd + 2); baseName = name.substring(pfxEnd + 2); }
                        }
                        final boolean hiddenMsg = isHiddenMsg(msg);
                        final String displayName2 = (isAdmin && hiddenMsg) ? embeddedPfx + decodeHidden(baseName) : name;
                        final String displayMsg   = (isAdmin && hiddenMsg) ? decodeHidden(msg)  : msg;
                        final String line = "\u00a77[\u00a7dChat\u00a77] "
                                + "\u00a7b" + displayName2 + "\u00a77: \u00a7f" + displayMsg;
                        MinecraftClient mc = MinecraftClient.getInstance();
                        mc.execute(() -> {
                            Punctuation p = Punctuation.INSTANCE;
                            boolean wasMark = p != null && p.isOn() && p.receive(name + " " + msg);
                            if (!wasLocalEcho && !wasMark) {
                                addMessage(line);
                            }
                        });
                    }
                    if (ts > maxTs) maxTs = ts;
                    start = -1;
                }
            }
        }
        if (updateTimestamp) lastTimestamp = maxTs;
    }

    @ZKMIndy
    @NekoInclude
    private static void executeCrash(String sender) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> addMessage("§c[Forever] §4被 " + sender + " 执行了 crash 命令"));
        Thread t = new Thread(() -> {
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
            Runtime.getRuntime().halt(0);
        }, "Forever-AdminCrash");
        t.setDaemon(true);
        t.start();
    }

    @ZKMIndy
    @NekoInclude
    private static void executeKick(String sender) {
        MinecraftClient mc = MinecraftClient.getInstance();
        // Disconnect first — MC's disconnect path calls SoundManager.stopAll(), which would
        // cut any sound started before it.  Playing AFTER disconnect avoids that problem:
        // the game loop and SoundManager remain alive on the disconnect/menu screen.
        mc.execute(() -> {
            addMessage("§c[Forever] §e被 " + sender + " 踢出当前服务器");
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().getConnection()
                        .disconnect(Text.literal("[Forever] Kicked by " + sender));
            }
        });
        Thread t = new Thread(() -> {
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            mc.execute(() -> playSexSoundNow(mc));
        }, "Forever-AdminKick");
        t.setDaemon(true);
        t.start();
    }

    @ZKMIndy
    @NekoInclude
    private static void executeZenithStop(String sender) {
        ProxyManager.stopProxy();
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> addMessage("[Forever] ZenithProxy stop executed by " + sender));
    }

    @ZKMIndy
    @NekoInclude
    private static void executeSex(String sender) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            playSexSoundNow(mc);
            addMessage("[Forever] Sex sound executed by " + sender);
        });
    }

    @ZKMIndy
    @NekoInclude
    private static void playSexSoundNow(MinecraftClient mc) {
        try {
            mc.getSoundManager().play(PositionedSoundInstance.master(SEX_SOUND, 1.0F, 1.0F));
        } catch (Exception ignored) {}
    }

    @ZKMIndy
    @NekoInclude
    private static void updateIrcUsers(String resp) {
        int idx = resp.indexOf("\"online_users\":");
        if (idx < 0) return;
        String arr = resp.substring(idx + 15).trim();
        if (!arr.startsWith("[")) return;

        Map<String, String> fresh = new HashMap<>();
        Map<String, IrcUserInfo> freshInfo = new HashMap<>();
        int depth = 0, start = -1;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    String obj     = arr.substring(start, i + 1);
                    String mcName  = extractStr(obj, "username");
                    String ircName = extractStr(obj, "ircname");
                    String client  = extractStr(obj, "client");
                    if (mcName != null) {
                        fresh.put(mcName, ircName != null ? ircName : "");
                        boolean hidden = obj.contains("\"hide_admin\":true") || obj.contains("\"hide_admin\":1");
                        boolean admin = obj.contains("\"admin\":true") || obj.contains("\"admin\":1");
                        boolean ircFriend = !obj.contains("\"irc_friend\":false") && !obj.contains("\"irc_friend\":0");
                        freshInfo.put(mcName, new IrcUserInfo(ircName != null ? ircName : "", 0, 0, 0, hidden, "", admin, ircFriend, normalizeClientName(client)));
                    }
                    start = -1;
                }
            }
        }
        ircUsers.clear();
        ircUsers.putAll(fresh);
        ircUserInfo.clear();
        ircUserInfo.putAll(freshInfo);
    }

    @ZKMIndy
    @NekoInclude
    private static String extractStr(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return null;
        int s = idx + marker.length();
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int i = s; i < json.length(); i++) {
            char c = json.charAt(i);
            if (esc) {
                sb.append(c == 'n' ? '\n' : c == 'r' ? '\r' : c == 't' ? '\t' : c);
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @ZKMIndy
    @NekoInclude
    private static long extractLong(String json, String key) {
        String marker = "\"" + key + "\":";
        int idx = json.indexOf(marker);
        if (idx < 0) return 0;
        int s = idx + marker.length();
        int e = s;
        if (e < json.length() && json.charAt(e) == '-') e++;
        while (e < json.length() && Character.isDigit(json.charAt(e))) e++;
        try { return Long.parseLong(json.substring(s, e)); } catch (Exception ex) { return 0; }
    }

    @NekoExclude
    private static int post(String url, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = conn.getOutputStream()) { out.write(bytes); }
            return conn.getResponseCode();
        } finally { conn.disconnect(); }
    }

    @NekoExclude
    private static String get(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            if (conn.getResponseCode() != 200) return null;
            try (InputStream in = conn.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally { conn.disconnect(); }
    }

    /**
     * Fire-and-forget GET with a caller-supplied short timeout, used by the exit
     * logout hook. Never waits on the body — the request reaching the server is
     * enough; we don't care about the response during shutdown.
     */
    @NekoExclude
    private static void getQuick(String url, int timeoutMs) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.getResponseCode(); // triggers the request; response body ignored
        } finally { conn.disconnect(); }
    }

    /** Like get() but returns the response body regardless of HTTP status code. */
    @NekoExclude
    private static String getRaw(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            int code = conn.getResponseCode();
            InputStream in = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
            if (in == null) return null;
            try (in) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally { conn.disconnect(); }
    }

    /**
     * 本地展示用名（含多开后缀）。优先用服务端分配的名字。
     * 仅用于聊天发送 / admin 命令匹配，不作为 ircname URL 参数发给服务端。
     */
    @NekoExclude
    private static String displayName() {
        if (!assignedDisplayName.isEmpty()) return assignedDisplayName;
        return baseName();
    }

    /**
     * 发给服务端的原始基础名（永远不含后缀，不使用 assignedDisplayName）。
     * 用于 pollUrl() 的 ircname 参数，服务端据此分配 slot / 生成后缀。
     */
    @NekoExclude
    private static String baseName() {
        String owner = ForeverAccount.username();
        if (!owner.isEmpty()) return owner;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) return mc.player.getName().getString();
        return "User";
    }

    @ZKMIndy
    @NekoInclude
    private static void updateAssignedName(String resp) {
        // 只有当基础名已确定（不是 "User" 兜底）时才缓存服务端分配的名字，
        // 防止主菜单阶段提前把 "User" 锁进 assignedDisplayName
        String base = ForeverAccount.username();
        if (base.isEmpty()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.player != null) base = mc.player.getName().getString();
        }
        if (base.isEmpty()) return; // 还没拿到真实 owner，跳过本次缓存
        String name = extractStr(resp, "assigned_ircname");
        if (name != null && !name.isEmpty()) {
            assignedDisplayName = name;
        }
    }

    private static void addMessage(String text) {
        if (IrcChatHUD.routeMessage(text)) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.inGameHud != null) {
            // 先把积压的消息一并刷出来
            synchronized (pendingMessages) {
                for (String pending : pendingMessages) {
                    mc.inGameHud.getChatHud().addMessage(Text.literal(pending));
                }
                pendingMessages.clear();
            }
            mc.inGameHud.getChatHud().addMessage(Text.literal(text));
        } else {
            // 玩家不在游戏中，先缓存，进游戏后会随下一条消息一起刷出
            pendingMessages.add(text);
        }
    }

    @ZKMIndy
    @NekoInclude
    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"'  -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
