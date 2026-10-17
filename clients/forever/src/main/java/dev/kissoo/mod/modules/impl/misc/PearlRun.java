package dev.kissoo.mod.modules.impl.misc;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.MoveEvent;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.core.Manager;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.player.PlayerEntity; // [新增] 导入玩家实体
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PearlRun extends Module {

    public static PearlRun INSTANCE;

    public final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Send));
    public final EnumSetting<ChatMode> chatMode = this.add(new EnumSetting<>("ChatMode", ChatMode.Public, () -> mode.is(Mode.Send)));
    private final StringSetting targetPlayer = this.add(new StringSetting("TargetPlayer", "AltAccountName", () -> mode.is(Mode.Send) && chatMode.is(ChatMode.Private)));
    private final StringSetting sendMessage = this.add(new StringSetting("SendMessage", "Need Pearl Run!", () -> mode.is(Mode.Send)));
    private final SliderSetting messageDelay = this.add(new SliderSetting("MessageDelay", 5.0, 1.0, 60.0, 0.5, () -> mode.is(Mode.Send)));
    private final BooleanSetting auto = this.add(new BooleanSetting("Auto", true, () -> mode.is(Mode.Send)).setParent());
    private final BooleanSetting armorDetection = this.add(new BooleanSetting("ArmorDetection", true, () -> mode.is(Mode.Send) && auto.isOpen()));
    private final SliderSetting totemLimit = this.add(new SliderSetting("Totem", 2.0, 0.0, 10.0, 1.0, () -> mode.is(Mode.Send) && auto.isOpen()));
    private final SliderSetting hpLimit = this.add(new SliderSetting("HP", 10.0, 1.0, 36.0, 1.0, () -> mode.is(Mode.Send) && auto.isOpen()));
    private final SliderSetting enemyRange = this.add(new SliderSetting("EnemyRange", 50.0, 0.0, 300.0, 1.0, () -> mode.is(Mode.Send) && auto.isOpen()).setSuffix("m"));
    private final BooleanSetting messageMode = this.add(new BooleanSetting("Message Mode", true, () -> mode.is(Mode.Receive)));
    private final BooleanSetting smart = this.add(new BooleanSetting("Smart", true, () -> mode.is(Mode.Receive)).setParent());
    private final BooleanSetting autoReset = this.add(new BooleanSetting("AutoReset", true, () -> mode.is(Mode.Receive) && smart.isOpen()));
    private final SliderSetting resetDelay = this.add(new SliderSetting("ResetDelay", 5.0, 1.0, 60.0, 0.5, () -> mode.is(Mode.Receive) && smart.isOpen() && autoReset.getValue()));

    private final BooleanSetting autoWalk = this.add(new BooleanSetting("AutoWalk", true, () -> mode.is(Mode.Receive) && smart.isOpen()));
    private final SliderSetting walkRadius = this.add(new SliderSetting("WalkRadius", 30.0, 5.0, 100.0, 1.0, () -> mode.is(Mode.Receive) && smart.isOpen() && autoWalk.getValue()));

    private final SliderSetting playerContent = this.add(new SliderSetting("PlayerContent", 1.0, 1.0, 6.0, 1.0, () -> mode.is(Mode.Receive)));

    private final StringSetting[] receiveMessages = new StringSetting[6];
    private final StringSetting[] signContents = new StringSetting[6];
    private long lastSendTime = 0;
    private long lastInteractTime = 0;

    private final Map<String, Long> processedMessages = new ConcurrentHashMap<>();
    private final List<ResetTask> resetTasks = new ArrayList<>();

    private BlockPos walkTargetLever = null;
    private boolean isWalking = false;
    private long walkStartTime = 0;

    private final Random random = new Random();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public final List<String> whitelist = new ArrayList<>();
    public static File whitelistFile;

    private static final Pattern WHISPER_PATTERN = Pattern.compile("来自\\s+([^:]+):\\s*(.*)$");

    public PearlRun() {
        super("PearlRun", "Auto Pearl Request / Trigger", Module.Category.Misc);
        this.setChinese("珍珠交互");
        INSTANCE = this;

        for (int i = 0; i < 6; i++) {
            final int index = i + 1;
            receiveMessages[i] = this.add(new StringSetting("ReceiveMessage" + index, "Need Pearl Run!", () -> mode.is(Mode.Receive) && playerContent.getValueInt() >= index));
            signContents[i] = this.add(new StringSetting("SignContent" + index, "Pearl", () -> mode.is(Mode.Receive) && playerContent.getValueInt() >= index));
        }

        whitelistFile = Manager.getFile("PearlWhiteList.txt");
        loadWhitelist();
    }

    @Override
    public void onEnable() {
        processedMessages.clear();
        resetTasks.clear();
        stopWalking();
        lastSendTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        resetTasks.clear();
        stopWalking();
    }

    private void loadWhitelist() {
        whitelist.clear();
        try {
            if (!whitelistFile.exists()) {
                whitelistFile.createNewFile();
                saveDefaultWhitelist();
            }
            List<String> lines = IOUtils.readLines(new FileInputStream(whitelistFile), StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    whitelist.add(trimmed);
                }
            }
            if (whitelist.isEmpty()) {
                saveDefaultWhitelist();
            }
        } catch (Exception e) {
            e.printStackTrace();
            saveDefaultWhitelist();
        }
    }

    private void saveDefaultWhitelist() {
        whitelist.clear();
        whitelist.add("kiss_O_o");
        whitelist.add("Neko_4live");
        whitelist.add("HoPRCandCHI");
        whitelist.add("咱喵");
        saveWhitelist();
    }

    public void saveWhitelist() {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(whitelistFile), StandardCharsets.UTF_8))) {
            writer.println("# PearlRun Whitelist");
            writer.println("# Player in this list who are allowed to trigger pearl....");
            for (String name : whitelist) {
                writer.println(name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;

        if (mode.is(Mode.Send)) {
            handleSendMode();
        } else if (mode.is(Mode.Receive)) {
            handleWalkUpdate();
            handleResetTasks();
        }
    }

    private void handleSendMode() {
        if (!auto.getValue()) return;

        long delayMs = (long)(messageDelay.getValueFloat() * 1000);
        if (System.currentTimeMillis() - lastSendTime < delayMs) return;

        int totems = InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING);
        int totemThreshold = totemLimit.getValueInt();

        boolean shouldSend = false;

        if (totems < totemThreshold) {
            shouldSend = true;
        } else if (totems == totemThreshold) {
            float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (hp <= hpLimit.getValueFloat()) {
                shouldSend = true;
            }
        }

        if (!shouldSend) return;
        if (armorDetection.getValue() && !hasArmor()) return;
        if (enemyRange.getValue() > 0.0 && !hasEnemyInRange()) return;

        lastSendTime = System.currentTimeMillis();
        String suffix = generateRandomSuffix(5);
        String msgContent = sendMessage.getValue() + " [" + suffix + "]";

        try {
            if (chatMode.is(ChatMode.Private)) {
                String target = targetPlayer.getValue();
                if (target != null && !target.trim().isEmpty()) {
                    mc.getNetworkHandler().sendChatCommand("tell " + target.trim() + " " + msgContent);
                }
            } else {
                mc.getNetworkHandler().sendChatMessage(msgContent);
            }
        } catch (Exception ignored) {}
    }
    private boolean hasEnemyInRange() {
        if (mc.world == null || mc.player == null) return false;
        double range = enemyRange.getValue();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player
                    && mc.player.distanceTo(player) <= range
                    && player.isAlive()
                    && player.getDisplayName() != null
                    && !Forever.FRIEND.isFriend(player)) {
                return true;
            }
        }
        return false;
    }

    private void handleWalkUpdate() {
        if (!isWalking || walkTargetLever == null || mc.player == null) return;

        if (System.currentTimeMillis() - walkStartTime > 15000) {
            stopWalking();
            CommandManager.sendMessage("[PearlRun] AutoWalk timed out.");
            return;
        }

        Vec3d targetVec = Vec3d.ofCenter(walkTargetLever);
        double dist = mc.player.getEyePos().distanceTo(targetVec);

        if (dist <= 4.5) {
            stopWalking();
            pullLever(walkTargetLever);
            walkTargetLever = null;
        } else {
            if (mc.player.horizontalCollision && mc.player.isOnGround()) {
                mc.player.jump();
            }
        }
    }

    @EventListener
    public void onMove(MoveEvent event) {
        if (mode.is(Mode.Receive) && isWalking && walkTargetLever != null && mc.player != null) {
            Vec3d playerPos = mc.player.getPos();
            Vec3d targetVec = Vec3d.ofCenter(walkTargetLever);
            double dist = playerPos.distanceTo(targetVec);

            Vec3d diff = targetVec.subtract(playerPos);
            Vec3d dir = new Vec3d(diff.x, 0, diff.z).normalize();
            BlockPos frontPos = BlockPos.ofFloored(mc.player.getX() + dir.x * 0.8, mc.player.getY(), mc.player.getZ() + dir.z * 0.8);
            BlockPos frontDownPos = frontPos.down();

            if (isFluid(frontPos) || isFluid(frontDownPos)) {
                stopWalking();
                CommandManager.sendMessage("§c[PearlRun] Danger! Fluid detected ahead, stopping AutoWalk.");
                return;
            }

            float rotation = getRotationTo(playerPos, targetVec).x;
            float yawRad = rotation / 180.0F * (float) Math.PI;
            double cappedSpeed = Math.min(0.2873, dist);
            double x = -((float)Math.sin(yawRad)) * cappedSpeed;
            double z = (float)Math.cos(yawRad) * cappedSpeed;

            event.setX(x);
            event.setZ(z);
        }
    }

    private boolean isFluid(BlockPos pos) {
        if (mc.world == null) return false;
        BlockState state = mc.world.getBlockState(pos);
        return !state.getFluidState().isEmpty();
    }

    private void stopWalking() {
        if (isWalking) {
            if (mc.player != null) {
                mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            }
            isWalking = false;
        }
    }

    private void handleResetTasks() {
        if (!smart.getValue() || !autoReset.getValue() || resetTasks.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        resetTasks.removeIf(task -> {
            if (currentTime >= task.executeTime) {
                BlockState state = mc.world.getBlockState(task.pos);
                if (state.getBlock() instanceof LeverBlock && state.get(Properties.POWERED)) {
                    Direction leverFacing = getLeverFacing(state, task.pos);
                    Vec3d hitVec = Vec3d.ofCenter(task.pos);

                    Forever.ROTATION.lookAt(hitVec);
                    BlockHitResult hit = new BlockHitResult(hitVec, leverFacing, task.pos, false);
                    mc.getNetworkHandler().sendPacket(
                            new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 0)
                    );
                    Forever.ROTATION.snapBack();
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                return true;
            }
            return false;
        });
    }

    private boolean hasArmor() {
        if (mc.player == null) return false;
        for (ItemStack armorItem : mc.player.getArmorItems()) {
            if (armorItem != null && !armorItem.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @EventListener
    private void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;
        if (!mode.is(Mode.Receive)) return;

        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            if (packet.content() != null) {
                mc.execute(() -> this.receive(packet.content().getString()));
            }
        }

        if (event.getPacket() instanceof ChatMessageS2CPacket packet) {
            if (packet.unsignedContent() != null) {
                mc.execute(() -> this.receive(packet.unsignedContent().getString()));
            } else {
                mc.execute(() -> this.receive(packet.body().content()));
            }
        }
    }

    private void receive(String s) {
        if (s == null) return;
        String clean = s.replaceAll("(?i)§[0-9A-FK-OR]", "");
        long currentTime = System.currentTimeMillis();

        if (messageMode.getValue()) {
            Matcher matcher = WHISPER_PATTERN.matcher(clean);
            if (matcher.find()) {
                String senderName = matcher.group(1).trim();
                String secretKey = matcher.group(2).trim();

                if (whitelist.contains(senderName)) {
                    String matchedSignContent = null;
                    int activeCount = playerContent.getValueInt();
                    for (int i = 0; i < activeCount; i++) {
                        String targetKey = receiveMessages[i].getValue();
                        if (targetKey.trim().isEmpty()) continue;

                        if (secretKey.contains(targetKey)) {
                            matchedSignContent = signContents[i].getValue();
                            break;
                        }
                    }

                    if (matchedSignContent != null) {
                        String dedupeKey = "WHISPER::" + senderName + "::" + secretKey;
                        if (currentTime - processedMessages.getOrDefault(dedupeKey, 0L) < 1000) return;
                        processedMessages.put(dedupeKey, currentTime);

                        findAndHandleLever(matchedSignContent);
                        return;
                    }
                }
            }
        }

        String matchedReceiveMsg = null;
        String matchedSignContent = null;

        int activeCount = playerContent.getValueInt();
        for (int i = 0; i < activeCount; i++) {
            String targetMsg = receiveMessages[i].getValue();
            if (targetMsg.trim().isEmpty()) continue;

            if (clean.contains(targetMsg)) {
                matchedReceiveMsg = targetMsg;
                matchedSignContent = signContents[i].getValue();
                break;
            }
        }

        if (matchedReceiveMsg == null) return;

        boolean whitelisted = false;
        String whitelistedName = "";
        for (String name : whitelist) {
            if (clean.contains(name)) {
                whitelisted = true;
                whitelistedName = name;
                break;
            }
        }
        if (!whitelisted) return;

        String suffix = extractSuffix(clean);
        String dedupeKey = (suffix != null) ? suffix : (whitelistedName + "::" + matchedReceiveMsg);

        if (currentTime - processedMessages.getOrDefault(dedupeKey, 0L) < 1000) return;
        processedMessages.put(dedupeKey, currentTime);

        findAndHandleLever(matchedSignContent);
    }

    private void findAndHandleLever(String targetSignContent) {
        if (System.currentTimeMillis() - lastInteractTime < 1000) return;

        Vec3d playerVec = mc.player.getPos();
        int radius = (smart.getValue() && autoWalk.getValue()) ? walkRadius.getValueInt() : 5;
        List<BlockPos> matchedSigns = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = mc.player.getBlockPos().add(x, y, z);
                    BlockEntity be = mc.world.getBlockEntity(pos);

                    if (be instanceof SignBlockEntity sign) {
                        if (checkSignContent(sign, targetSignContent)) {
                            matchedSigns.add(pos);
                        }
                    }
                }
            }
        }

        if (matchedSigns.isEmpty()) return;
        matchedSigns.sort(Comparator.comparingDouble(pos ->
                playerVec.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
        ));

        for (BlockPos signPos : matchedSigns) {
            if (tryHandleAdjacentLever(signPos)) {
                lastInteractTime = System.currentTimeMillis();
                return;
            }
        }
    }

    private boolean tryHandleAdjacentLever(BlockPos signPos) {
        Direction[] dirs = {
                Direction.UP, Direction.DOWN,
                Direction.NORTH, Direction.SOUTH,
                Direction.EAST, Direction.WEST
        };
        List<BlockPos> availableLevers = new ArrayList<>();

        for (Direction dir : dirs) {
            BlockPos adj = signPos.offset(dir);
            BlockState state = mc.world.getBlockState(adj);

            if (state.getBlock() instanceof LeverBlock) {
                if (!state.get(Properties.POWERED)) {
                    availableLevers.add(adj);
                }
            }
        }

        if (availableLevers.isEmpty()) return false;
        Vec3d playerVec = mc.player.getPos();
        availableLevers.sort(Comparator.comparingDouble(pos ->
                playerVec.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
        ));

        BlockPos targetLever = availableLevers.get(0);
        Vec3d targetVec = Vec3d.ofCenter(targetLever);
        double dist = mc.player.getEyePos().distanceTo(targetVec);

        if (dist <= 4.5) {
            pullLever(targetLever);
            return true;
        } else if (smart.getValue() && autoWalk.getValue()) {
            this.walkTargetLever = targetLever;
            this.isWalking = true;
            this.walkStartTime = System.currentTimeMillis();
            return true;
        }

        return false;
    }

    private void pullLever(BlockPos targetLever) {
        BlockState leverState = mc.world.getBlockState(targetLever);
        if (!(leverState.getBlock() instanceof LeverBlock) || leverState.get(Properties.POWERED)) return;

        Direction leverFacing = getLeverFacing(leverState, targetLever);
        Vec3d hitVec = Vec3d.ofCenter(targetLever);

        Forever.ROTATION.lookAt(hitVec);
        BlockHitResult hit = new BlockHitResult(hitVec, leverFacing, targetLever, false);
        mc.getNetworkHandler().sendPacket(
                new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 0)
        );
        Forever.ROTATION.snapBack();
        mc.player.swingHand(Hand.MAIN_HAND);

        if (smart.getValue() && autoReset.getValue()) {
            long delayMs = (long) (resetDelay.getValueFloat() * 1000L);
            resetTasks.add(new ResetTask(targetLever, System.currentTimeMillis() + delayMs));
        }
    }

    private Direction getLeverFacing(BlockState state, BlockPos pos) {
        BlockFace face = state.get(Properties.BLOCK_FACE);
        switch (face) {
            case FLOOR: return Direction.UP;
            case CEILING: return Direction.DOWN;
            case WALL:
                Direction facing = state.get(Properties.HORIZONTAL_FACING);
                return facing.getOpposite();
            default: return Direction.UP;
        }
    }

    private boolean checkSignContent(SignBlockEntity sign, String target) {
        if (target == null || target.trim().isEmpty()) return false;
        for (Text t : sign.getFrontText().getMessages(false)) {
            if (t.getString().contains(target)) return true;
        }
        for (Text t : sign.getBackText().getMessages(false)) {
            if (t.getString().contains(target)) return true;
        }
        return false;
    }

    private String generateRandomSuffix(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        return sb.toString();
    }

    private String extractSuffix(String msg) {
        int s = msg.lastIndexOf("[");
        int e = msg.lastIndexOf("]");
        if (s != -1 && e != -1 && s < e) return msg.substring(s + 1, e);
        return null;
    }

    public static Vec2f getRotationTo(Vec3d posFrom, Vec3d posTo) {
        Vec3d vec3d = posTo.subtract(posFrom);
        return getRotationFromVec(vec3d);
    }

    private static Vec2f getRotationFromVec(Vec3d vec) {
        double d = vec.x;
        double d2 = vec.z;
        double xz = Math.hypot(d, d2);
        d2 = vec.z;
        double d3 = vec.x;
        double yaw = normalizeAngle(Math.toDegrees(Math.atan2(d2, d3)) - 90.0);
        double pitch = normalizeAngle(Math.toDegrees(-Math.atan2(vec.y, xz)));
        return new Vec2f((float)yaw, (float)pitch);
    }

    private static double normalizeAngle(double angleIn) {
        double angle;
        if ((angle = angleIn % 360.0) >= 180.0) {
            angle -= 360.0;
        }
        if (angle < -180.0) {
            angle += 360.0;
        }
        return angle;
    }

    public enum Mode {
        Send, Receive
    }

    public enum ChatMode {
        Public, Private
    }

    private static class ResetTask {
        final BlockPos pos;
        final long executeTime;
        ResetTask(BlockPos pos, long executeTime) {
            this.pos = pos;
            this.executeTime = executeTime;
        }
    }
}