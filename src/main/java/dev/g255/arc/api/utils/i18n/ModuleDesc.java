package dev.g255.arc.api.utils.i18n;

import java.util.HashMap;
import java.util.Map;

/**
 * 模块功能描述表（英文 / 中文）。
 * 每条描述按模块的实际设置项与行为整理，ClickGUI 悬停时显示，语言跟随 ClientSetting.chinese。
 */
public final class ModuleDesc {
   private static final Map<String, String> EN = new HashMap();
   private static final Map<String, String> ZH = new HashMap();

   private ModuleDesc() {
   }

   private static void d(String name, String en, String zh) {
      EN.put(name, en);
      ZH.put(name, zh);
   }

   public static String get(String name, boolean chinese) {
      if (name == null) {
         return null;
      }

      return chinese ? (String)ZH.get(name) : (String)EN.get(name);
   }

   public static String zh(String name) {
      return get(name, true);
   }

   public static String en(String name) {
      return get(name, false);
   }

   static {
      // ---------------- Client ----------------
      d("HUD", "Main HUD: watermark, module list, armor, FPS/ping/TPS and coords.", "主界面：水印、模块列表、盔甲、FPS/延迟/TPS 与坐标");
      d("ClickGui", "ClickGUI layout: panel size, animation, blur, snow and color theme.", "界面本身：面板尺寸、动画、模糊、雪花与配色主题");
      d("ClientSetting", "Global options: chat, notifications, animations and rotation sync.", "全局设置：聊天、通知、动画与转向同步");
      d("Colors", "Global colour source: custom hue or animated rainbow shared by every client colour setting.", "全局配色：自定义或彩色模式，决定所有客户端颜色的取值");
      d("AntiCheat", "Server-interaction tuning: hitbox, place/break timing, rotation and prediction.", "服务器交互调校：命中箱、放置/破坏时序、转向与预测");
      d("Baritone", "Built-in Baritone pathing settings: mine, place, parkour and legit mode.", "内置寻路参数：挖掘、放置、跑酷与 legit 模式");
      d("Fonts", "Custom GUI font, size, vertical shift and shadow.", "自定义界面字体、字号、偏移与阴影");
      d("Items", "On-screen counter for crystals, pearls, obsidian, totems and other key items.", "屏幕计数：水晶、珍珠、黑曜石、图腾等关键物品");
      d("TextRadar", "Text list of nearby players with health, pops and distance.", "文字雷达：附近玩家的血量、爆图腾数与距离");
      d("BackPackHUD", "Draws your backpack contents on screen with optional stack counts.", "在屏幕上显示背包内容，可显示数量");
      d("HoverSound", "Sound pack and volume for GUI hover, expand and toggle.", "界面悬停、展开、开关的音效包与音量");
      d("Friend", "Manages the friend list shared by combat and ESP modules.", "管理好友名单，战斗与透视模块共用");

      // ---------------- Combat ----------------
      d("AntiWeak", "Bypasses the weakness effect by swapping items when you attack.", "攻击时切换物品，绕过虚弱带来的减伤");
      d("Aura", "Attacks the best target in range with silent rotation and hit animation.", "自动攻击范围内最优目标，带静默转向与攻击动画");
      d("AutoCrystal", "Alien crystal aura (verbatim alien port): damage calc, base placement, breaking and render.", "自动水晶（Alien 原版）：伤害计算、底座放置、打爆与渲染");
      d("CrystalBot", "Forever-style crystal aura: places and breaks crystals around targets with damage/self-damage limits, multi-point raycast and base strategies.", "水晶光环（Forever 系）：围绕目标放置并打爆水晶，受伤害与自伤上限约束，带多点射线与基座策略");
      d("CrystalHurtPeople", "Spams crystal placement at a fixed CPS to hurt nearby players.", "以固定 CPS 刷水晶持续伤害附近玩家");
      d("AutoAnchor", "Respawn anchor aura: picks the best anchor spot from predicted positions, slips an anchor into the cell being mined, fires several place packets per click, then charges and detonates.", "重生锚光环：按预测位置挑最佳锚点，包挖快挖穿时直接往那一格塞锚，一次交互连拍多个放置包抢帧，再充能引爆。");
      d("AntiAnchor", "Fills the anchor-damage spots around you with obsidian.", "用黑曜石填掉周围可能被放锚的伤害点");
      d("BedAura", "Detonates beds next to targets for damage.", "在目标身边引爆床造成伤害");
      d("SelfTrap", "Traps yourself in obsidian (head/feet/chest); multi-cell surround, packet-driven pre-place and surround-break protection.", "把自己用黑曜石封住自保：头/脚/胸可单独开关，按包围盒多格补围，爆炸/水晶/破块时即时补，并可扫掉围点水晶");
      d("Surround", "Seals the four sides of the block you stand in with obsidian, so the enemy cannot place an anchor or crystal next to you.", "用黑曜石把自己脚下这层的四个侧面封死，挡住对面塞锚 / 塞水晶的落点");
      d("AutoTrap", "Traps targets in obsidian automatically, with head/chest/legs options.", "自动用黑曜石困住目标，可选头/胸/腿各层");
      d("Blocker", "Walls a target in with obsidian (burrow, face, feet, extend).", "用黑曜石把目标封死（埋身、正面、脚下、延伸）");
      d("BlockLag", "Burrows you into obsidian using lag-back modes, with yaw-step rotation, helper placement and blink cancel.", "用延迟回弹把黑曜石卡进自身位置实现埋身，含偏航步进转头、辅助放置与闪烁取消");
      d("HoleFiller", "Fills enemy holes with obsidian so they lose their safe spot.", "用黑曜石填掉敌人的洞，破坏安全位");
      d("AutoWeb", "Places cobwebs on targets to slow them, with feet/face/down modes.", "在目标身上放蜘蛛网减速，可选脚/头/下方");
      d("AutoPush", "Pushes targets out of burrows and surrounds with pistons (PistonKick logic: stuck detection, exit picking, consecutive/merge push, crystal clearing).", "用活塞把目标从埋身/围点里顶出去（移植 PistonKick：卡点判定、出口选择、连续推、合并推、推前清水晶）");
      d("PistonCrystal", "Places piston, redstone and crystal combo on a target.", "对目标使用活塞+红石+水晶组合连招");
      d("PistonCrystalPlus", "Damage-based piston + crystal combo ported from LeavesHack.", "伤害驱动的活塞水晶，选点更准（移植自 LeavesHack）");
      d("CevBreaker", "Breaks the block above the target's head and follows with crystals.", "打掉目标头顶方块，配合水晶完成击杀");
      d("AutoMine", "Automatically mines the burrow/surround blocks around targets.", "自动挖掉目标周围的埋身与围脚方块");
      d("AntiPiston", "Packet-mines pistons, redstone and torches near you.", "包挖掘拆掉周围的活塞、红石与火把");
      d("AntiCobweb", "Uses crystals to clear cobwebs around you.", "用水晶清掉身边的蜘蛛网");
      d("AntiPhase", "Blocks enemies from phasing into your space (ladder, frame, scaffolding).", "阻止敌人用梯子、物品框、脚手架穿进你的位置");
      d("AntiCrawl", "Stops you from being forced into crawl mode.", "防止被强制进入爬行状态");
      d("Offhand", "Manages the offhand slot: totem, crystal, gapple or shield.", "管理副手：图腾、水晶、金苹果或盾牌");
      d("Criticals", "Forces critical hits with packet and motion tricks (multiple modes).", "用发包与动作伪造暴击，多种模式");
      d("MaceAura", "Mace aura: spoofs fall height and smashes targets in range.", "重锤光环：伪造下落高度俯冲攻击范围内目标");
      d("MaceGodPlus", "Mace combo helper: auto swap, void kill and follow-up attack.", "重锤连招助手：自动换装、虚空击杀与补刀");
      d("FlatGroundMace", "Flat-ground mace hit: fakes fall distance to land a full-damage smash.", "平地重锤：伪造下落距离打出满伤");
      d("MacePlus", "Wind-charge boost to launch yourself for mace jumps.", "风弹起飞，为重锤跳跃加速");
      d("Quiver", "Quick bow shot: auto switches to a bow, charges and releases at the set power.", "快速射箭：自动切弓、蓄力到设定力度后松手");
      d("AutoArrow", "Bow/crossbow aimbot with charge sync and auto release.", "弓箭与弩的自动瞄准、蓄力同步与自动释放");
      d("BowBomb", "Bow damage exploit with aimbot and packet spoofing.", "弓箭伤害漏洞利用，含自瞄与发包伪造");
      d("AutoEZ", "Sends an 'ez' message when a target dies.", "击杀目标时自动发送嘲讽消息");
      d("PopEz", "Taunts in chat whenever an enemy totem pops, padded with random characters to dodge duplicate filters.", "爆图腾时发送消息嘲讽");
      d("Panic", "Emergency stop: instantly disables every module.", "紧急停止：瞬间关闭所有模块");
      d("AutoTrapDoor", "Closes trapdoors above targets to keep them from standing up.", "在目标上方关活板门，压住不让起身");
      d("AutoTK", "Team-kill helper that spawns fake player entities near targets.", "团队击杀辅助：在目标身边生成假玩家实体");
      d("TrapShulker", "Seals a target inside a placed shulker box.", "用潜影盒把目标堵死在里面");
      d("AutoPowder", "Places powder snow around targets to trap them.", "在目标周围铺细雪把人困住");
      d("AntiRegear", "Detects enemies trying to regear and interferes with it.", "侦测并干扰敌人补装");
      d("AutoSupplyPlus", "Opens shulkers/kit and refills a full loadout automatically, including a post-death regear pass.", "开潜影盒与套装自动补满整套物资，含死亡后的自动重整补给");
      d("AutoMend", "Swaps damaged armor out to mend it with XP, then swaps back.", "换下有损盔甲用经验修补，修好再穿回");

      // ---------------- Exploit ----------------
      d("Phase", "Phases through blocks using pearls, scaffolding or packet tricks.", "利用珍珠、脚手架或发包穿进方块");
      d("Clip", "Clips into blocks through hitbox desync or invalid movement packets.", "用碰撞箱错位或非法移动包卡进方块");
      d("Blink", "Holds your packets and flushes them to teleport; supports pulse mode.", "缓存数据包后集中发送实现瞬移，支持脉冲模式");
      d("AntiPacket", "Cancels or delays selected incoming packets (slots, rotation, GUI).", "拦截或延迟指定入站包（槽位、转向、界面）");
      d("PacketControl", "Controls packet timing and position sync to slip past checks.", "控制发包时序与位置同步以绕过检测");
      d("Disabler", "Turns off anticheat checks; includes slow-falling disabler.", "关闭反作弊检测，含缓降 disabler");
      d("AntiHunger", "Cancels sprint and ground packets so hunger drains slower.", "取消疾跑与着地包，减缓饥饿消耗");
      d("FastFall", "Faster falling, optionally with timer and anchor modes.", "加速下落，可选变速与锚定模式");
      d("FastLatency", "Forces the client to update latency values faster.", "强制客户端更快刷新延迟数值");
      d("FakeVanilla", "Strips client brand and mod channels so servers see vanilla.", "清掉客户端品牌与模组通道，伪装原版");
      d("FakeName", "Spoofs the name other players see for you.", "伪造其他玩家看到的你的名字");
      d("PingSpoof", "Sends a fake ping value to the server.", "向服务端发送伪造的延迟数值");
      d("NoMineAnimation", "Hides your mining animation and progress from others.", "隐藏挖掘动画与进度");
      d("RaytraceBypass", "Allows interacting with blocks through walls.", "允许隔墙与方块交互");
      d("FastProjectile", "Sends extra projectile packets so arrows and pearls travel faster.", "多发投掷物数据包，让箭与珍珠飞得更快");
      d("InfiniteTrident", "Keeps riptide usable without water or durability cost.", "三叉戟激流无需水与耐久即可使用");
      d("RocketExtend", "Extends firework rocket boost duration for longer elytra flight.", "延长烟花火箭推进时间，飞得更远");
      d("XCarry", "Keeps items inside crafting slots so they survive death.", "把物品藏在合成格内，死亡不掉落");
      d("ChorusControl", "Delays chorus fruit teleport until you press the confirm key.", "紫颂果传送前需要按键确认");
      d("DeathRespawn", "Respawns you at custom X/Y/Z coordinates.", "在自定义坐标处重生");
      d("ServerLagger", "Lags the server using packet spam modes (AAC, armor, window...).", "用多种数据包刷屏模式卡服（AAC、盔甲、窗口等）");
      d("ChunkESP", "Highlights old, unloaded and exploit-detected chunks.", "高亮旧区块、未加载区块与漏洞区块");
      d("SkinFlicker", "Flickers your skin layers (cape, jacket, sleeves...).", "闪烁皮肤各层（披风、外套、袖子等）");
      d("TeleportLogger", "Logs teleports and chorus jumps with distance and timing.", "记录传送与紫颂果位移的距离与时间");
      d("32KMace", "32k/lag exploit that smashes targets with a mace.", "利用卡顿打出 32k 重锤伤害");
      d("MaceSpoof", "Spoofs fall distance so the mace deals full damage.", "伪造下落距离，让重锤打出满伤");

      // ---------------- Misc ----------------
      d("VisualRange", "Chat alerts when players enter or leave your render distance.", "玩家进入或离开视野时给出聊天提示");
      d("AutoLog", "Disconnects on low HP, totem pop or when out of totems.", "低血、爆图腾或缺图腾时自动断线");
      d("AutoRespawn", "Respawns automatically right after dying.", "死亡后立即自动重生");
      d("AutoReconnect", "Rejoins the server after a disconnect, with optional auto-auth and queue.", "掉线后自动重连，可自动登录与排队");
      d("AntiAFK", "Moves, jumps or replies to avoid AFK kicks.", "自动移动、跳跃或回复，避免被踢");
      d("AutoCommand", "Runs a configured command the moment the module is enabled.", "开启时自动执行设定命令");
      d("AutoKit", "Runs the /kit command automatically.", "自动执行 /kit 命令");
      d("AutoDrop", "Drops unwanted or excess items based on kit rules.", "按套装规则丢弃无用或多余物品");
      d("AutoEnderChest", "Automatically places an ender chest and takes items out of it.", "自动放置末影箱并从中取出物品");
      d("DropShulkerBox", "Drops full or empty shulker boxes automatically.", "自动丢弃满的或空的潜影盒");
      d("DropAntiRegear", "Finds shulker boxes nearby, opens them and throws every item out (optionally mines the box) so enemies cannot regear.", "侦测附近的潜影盒，打开后把里面的物品全部丢出（可选再挖掉），阻止敌人补装");
      d("ArmorPreserve", "Unequips armor before it breaks below a durability threshold.", "耐久低于阈值前脱下盔甲，避免爆掉");
      d("ShulkerViewer", "Previews shulker contents in tooltips or on the HUD.", "在提示框或HUD中预览潜影盒内容");
      d("Nuker", "Breaks every block in range instantly.", "瞬间破坏范围内所有方块");
      d("LavaFiller", "Places blocks to block off lava flows.", "放方块挡住岩浆");
      d("BedCrafter", "Crafts beds automatically from wool and planks.", "用羊毛和木板自动合成床");
      d("NoResourcePack", "Ignores server resource packs so nothing gets forced on you.", "忽略服务器资源包，避免被强制替换材质");
      d("NoSound", "Mutes selected sounds: explosions, hits, teleports, potions.", "屏蔽指定音效：爆炸、攻击、传送、药水");
      d("NoTerrainScreen", "Removes the terrain/loading overlay when joining a world.", "去掉进入世界时的地形加载遮罩");
      d("MiddleClick", "Runs a bound action on middle click (friend by default).", "中键执行绑定操作（默认加好友）");
      d("ChatAppend", "Appends fixed text to every chat message you send.", "在每条发出的聊天后追加固定文本");
      d("Spammer", "Spams a message with random delay and optional whisper mode.", "按随机间隔刷屏，可切换私聊对象");
      d("CoordLogger", "Copies your coordinates to clipboard on logout and death.", "下线与死亡时把坐标复制到剪贴板");
      d("AntiAim", "Makes enemy aimbots lock onto the wrong position.", "让敌方自瞄锁定错误位置");
      d("Tips", "Chat tips for visual range, pops, lagback and potions.", "聊天提示：视距、爆图腾、回弹与药水");
      d("KillThunder", "Spawns staggered lightning bolts on an enemy death within 20 blocks, spread over a set duration.", "敌人死亡时在 20 格内按设定时长分批落下闪电");
      d("ItemCounter", "Centered HUD counting your totems and crystals, with icon, pulse colors and head-blocked / enemy-trap warnings.", "居中显示图腾与水晶数量，可带图标、脉冲配色，并对盖头与敌人开陷阱给出提示");
      d("TargetFollow", "Locks your rotation onto the closest player and auto-walks forward, mainly for elytra chasing.", "把视角锁到最近的玩家并自动前进，主要用于鞘翅追人");
      d("KillEffect", "Kill effects ported from earthquake: lightning and sounds, picture ring, or light pillar, plus kill-streak sounds.", "移植自 earthquake 的击杀特效：闪电音效 / 圆环贴图 / 光柱三种模式，带连杀音效");
      d("Punctuation", "Binds a key to mark positions and syncs the marks through encrypted chat, with a sound cue and auto-clear.", "按键标记坐标，并将标记用加密聊天同步给队友，带提示音与超时自动清除");
      d("AntiQuit", "Blocks accidental quit or disconnect actions.", "防止误触退出与断线");
      d("FakePlayer", "Spawns a fake player that can record and replay your actions.", "生成假玩家，可录制并回放你的动作");
      d("Bot", "Automation bot: trading, duping and farming routines.", "自动化机器人：交易、复制与刷取流程");
      d("PacketLogger", "Prints selected outgoing packets to chat for debugging.", "把指定的出站数据包打印到聊天栏");
      d("ExtraTab", "Enlarges the tab player list with custom size and columns.", "放大玩家列表，可自定义尺寸与列数");

      // ---------------- Movement ----------------
      d("Speed", "Movement speed with strafe/grim modes, boat and boost options.", "移动加速：Strafe/Grim 模式，含船与爆炸加速");
      d("Fly", "Creative-like flight with speed and anti-kick.", "类创造飞行，可调速度并防踢");
      d("ElytraFly", "Elytra flight modes: control, boost, bounce, freeze and packet.", "鞘翅飞行：控制、加速、弹跳、悬停与发包模式");
      d("PacketFly", "Packet-based flight with multiple bypass modes.", "基于数据包的飞行，多种绕过模式");
      d("FakeFlight", "Fakes flight state to glide and control movement.", "伪造飞行状态进行滑翔与移动控制");
      d("HighJump", "Increases your jump height by a multiplier.", "按倍数提高跳跃高度");
      d("HighJump+", "Throws a wind charge under you and boosts you upward while grounded.", "原地投掷风弹，把自己向上弹起");
      d("WindJump", "Throws a wind charge to launch yourself upward and forward.", "投掷风弹把自己向上向前顶起");
      d("LongJump", "Long jumps with configurable speed (Plain/GrimHop).", "远距离跳跃，可选普通或 Grim 模式");
      d("Step", "Steps up blocks instantly with height control.", "瞬间登上台阶，高度可调");
      d("NoJumpDelay", "Removes the jump cooldown so you can jump every tick.", "移除跳跃冷却，每刻都能跳");
      d("NoSlow", "No slowdown while eating, blocking or climbing, plus soul sand and GUI movement.", "进食、举盾、爬梯不再减速，并含灵魂沙与界面中移动");
      d("FastWeb", "Faster movement inside cobwebs.", "在蜘蛛网里加速移动");
      d("FastSwim", "Faster swimming in water and lava.", "在水中与岩浆中加速游动");
      d("Velocity", "Reduces or cancels knockback with mode filters.", "减少或抵消击退，支持多种模式");
      d("AntiVoid", "Stops you from falling into the void.", "阻止你掉进虚空");
      d("Scaffold", "Places blocks under you automatically while walking.", "边走边在脚下自动放方块搭路");
      d("SafeWalk", "Prevents you from walking off block edges.", "防止走出方块边缘掉落");
      d("Strafe", "Modifies air strafe movement.", "调整空中移动方式");
      d("BlockStrafe", "Strafes around blocks at a configurable speed.", "贴着方块绕行，速度可调");
      d("Sprint", "Keeps you sprinting permanently with pause filters.", "永久保持疾跑，可设置暂停条件");
      d("HoleSnap", "Snaps you into the nearest hole quickly.", "快速把你吸进最近的洞里");
      d("AutoWalk", "Walks forward or follows a path automatically.", "自动前进或沿路径行走");
      d("Spiral", "Moves you in a spiral pattern.", "螺旋轨迹移动");
      d("Spin", "Spins your body (visible to others), first person unaffected.", "身体高速旋转（他人可见），第一人称不变");
      d("MovementSync", "Keeps movement packets consistent with the server position.", "让移动包与服务端位置保持一致");
      d("EntityControl", "Lets you control boats and mounts on land.", "在陆地上控制船与坐骑");
      d("VClip", "Teleports you vertically through blocks.", "垂直穿过方块传送");
      d("Flatten", "Clears the blocks above your head while walking.", "行走时清掉头顶方块");
      d("AntiSlowFalling", "Cancels slow falling so gravity behaves normally.", "取消缓降效果，恢复正常下落");

      // ---------------- Player ----------------
      d("PacketMine", "Starts mining with packets to break blocks instantly, with render.", "包挖掘瞬间破坏方块，含进度渲染");
      d("ChestStealer", "Takes everything from the container you open.", "自动拿空你打开的箱子");
      d("Sorter", "Sorts inventory, drops trash and keeps kit items.", "整理背包、丢垃圾并保留套装物品");
      d("Replenish", "Refills nearly-empty stacks from your inventory.", "从背包补满快用完的堆叠");
      d("AutoTool", "Selects the best tool for the job; supports auto fishing.", "自动选最合适的工具，支持自动钓鱼");
      d("AutoArmor", "Equips the best armor automatically.", "自动穿上最好的盔甲");
      d("AutoEat", "Eats food automatically when needed.", "需要时自动进食");
      d("AutoPot", "Throws configured potions automatically.", "自动投掷设定好的药水");
      d("AutoPearl", "Throws ender pearls at a set angle with rotation control.", "按设定角度自动投掷末影珍珠");
      d("AirPlace", "Places blocks in mid-air without a support block.", "无需支撑方块即可空中放置");
      d("NoFall", "Prevents fall damage via packets.", "发包规避摔落伤害");
      d("NoRotate", "Ignores rotations the server forces on you.", "忽略服务端强制设置的转向");
      d("InteractTweaks", "Interaction QoL: reach, multitask, no mine reset, faster use.", "交互增强：触及距离、多任务、不重置挖掘、加快使用");
      d("Freecam", "Detached camera you can fly around freely.", "脱离本体的自由飞行视角");
      d("FreeLook", "Look around without turning your body.", "自由视角，身体朝向保持不动");
      d("Yaw", "Locks your yaw and pitch to fixed angles.", "把水平角与俯仰角锁定为固定值");
      d("Follower", "Follows a chosen player automatically with pathing.", "自动跟随指定玩家");
      d("AntiEffects", "Removes levitation and slow falling effects.", "移除漂浮与缓降效果");
      d("PacketEat", "Eats without slowing down or releasing the use key.", "进食不减速、不用松手");
      d("Timer", "Changes game speed with hold-key and charge options.", "变速齿轮，支持按键与蓄力");
      d("IQBoost", "Cosmetic IQ value shown in the HUD and module list.", "装饰性 IQ 数值，显示在界面与列表中");

      // ---------------- Render ----------------
      d("ESP", "3D/2D boxes, health and armor for players, items and containers.", "玩家、物品与容器的3D/2D描框、血量与耐久");
      d("TwoDESP", "2D screen-space boxes with health, armor and effects.", "屏幕空间2D描框，含血量、耐久与效果");
      d("HoleESP", "Highlights safe and unsafe holes around you.", "高亮周围安全与不安全的洞");
      d("PhaseESP", "Highlights blocks you can phase through.", "高亮可穿过去的方块位置");
      d("Chams", "Renders entities through walls with fill and outline colors.", "穿墙渲染实体，可设置填充与描边色");
      d("Tracers", "Lines from your crosshair to players and containers.", "从准星指向玩家与容器的连线");
      d("Trails", "Renders trails behind players and projectiles.", "为玩家与投掷物绘制移动拖尾");
      d("Trajectories", "Predicts projectile paths for pearls, arrows and XP bottles.", "预测珍珠、箭与经验瓶的弹道");
      d("NameTags", "Custom nametags with health, ping, armor and pops.", "自定义名牌：血量、延迟、装备与爆图腾数");
      d("NameTag2D", "2D nametags with info, armor and totem pops.", "2D名牌，含信息、装备与图腾数");
      d("TargetESP", "Highlights your current and recent targets.", "高亮当前目标与近期目标");
      d("LogoutSpots", "Marks where players logged out with their gear.", "标记玩家下线位置与其装备");
      d("PopEsp", "Marks where totems popped.", "标记图腾爆开的位置");
      d("PopChams", "Renders a fading player model when a totem pops.", "爆图腾时渲染渐隐的玩家模型");
      d("BreakESP", "Shows mining progress boxes on blocks being broken.", "显示方块破坏进度的框");
      d("BreakerEsp", "Highlights blocks being packet-mined around you.", "高亮周围正在被包挖掘的方块");
      d("PlaceRender", "Animates blocks that were just placed.", "为刚放置的方块显示动画");
      d("HighLight", "Highlights the block you are looking at.", "高亮你正在看的方块");
      d("CrystalPlaceRender", "Animated crystals when placed or removed.", "水晶放置与消失时的动画");
      d("ArmorHUD", "Armor durability HUD with optional percentages.", "盔甲耐久HUD，可显示百分比");
      d("BAHalo", "Blue Archive character halo above heads.", "头顶显示碧蓝档案角色光环");
      d("JumpCircle", "Renders a circle effect when jumping.", "跳跃时绘制圆环特效");
      d("LastWalkEsp", "Shows the path you recently walked.", "显示最近走过的路径");
      d("TotemParticle", "Custom totem pop particles with velocity and colors.", "自定义图腾粒子，速度与颜色可调");
      d("SkyColor", "Custom sky and fog colors per dimension.", "按维度自定义天空与雾的颜色");
      d("Skybox", "Custom skybox: layered nebula and star textures with per-layer colors and a star toggle.", "自定义星空：双层星云与星空贴图叠加，各层颜色与星星开关可调");
      d("Ambience", "Custom sky, cloud, fog and world tint, plus fullbright.", "自定义天空、云、雾与世界色调，含夜视");
      d("NoRender", "Disables overlays, particles and visual effects.", "关闭各类覆盖层、粒子与视觉效果");
      d("Xray", "See through blocks with an editable block list.", "透视方块，可编辑方块列表");
      d("ViewModel", "Changes held item scale, position and rotation.", "调整手持物品的大小、位置与旋转");
      d("CameraClip", "Lets the third-person camera pass through blocks.", "让第三人称镜头穿过方块");
      d("MotionCamera", "Adds smooth/inertial motion to the camera.", "为镜头增加平滑与惯性");
      d("AspectRatio", "Forces a custom aspect ratio for a wider view.", "强制自定义宽高比，获得更宽视野");
      d("Fov", "Custom field of view, including held item FOV.", "自定义视野，含手持物品的视野");
      d("Zoom", "Optifine-like zoom with smooth animation.", "类 Optifine 的平滑缩放");
      d("Crosshair", "Custom crosshair length, thickness, gap and color.", "自定义准星的长度、粗细、间距与颜色");
      d("Tooltips", "Extra tooltips for shulkers and beehives.", "为潜影盒与蜂巢提供额外提示");
      d("Shader", "Outline/glow shader with per-entity-type filters.", "描边发光着色器，可按实体类型筛选");
      d("Weather", "Custom weather: rain, snow or none.", "自定义天气：雨、雪或关闭");

      // ---------------- HUD ----------------
      d("NotificationHud", "HUD notifications when modules are toggled.", "模块开关时的 HUD 通知");
      d("AnimatedGif", "Animated GIF overlay on the screen.", "屏幕上的 GIF 动画叠加");
      d("PlayerModel", "Renders your own player model on screen.", "在屏幕上渲染自己的角色模型");

      // ---------------- 移植模块 ----------------
      d("WaterMark", "Watermark text at a corner: custom title, font and color.", "角落水印文字：自定义内容、字体与颜色");
      d("ArrayList", "Module list HUD, sorted by name length or alphabetically.", "模块列表 HUD，可按名称长度或字母排序");
      d("Info", "FPS / ping / TPS readout with toggleable lines.", "帧数 / 延迟 / TPS 信息，逐项开关");
      d("Coords", "Shows your coordinates, dimension and biome.", "显示坐标、维度与生物群系");
      d("Armor", "Armor HUD with durability bars and percentages.", "护甲 HUD，含耐久条与百分比");
      d("HUDSetting", "Global font and shadow switches for HUD elements.", "HUD 元素的全局字体与阴影开关");
      d("AutoRegear", "Automatically re-equips your kit from the inventory.", "自动从背包中重新装备套装");
      d("Breaker", "Breaks the block you are aiming at with extra speed.", "按额外速率破坏准星所指方块");
      d("Burrow", "Places a block inside your own hitbox to prevent knockback.", "在自己脚下塞方块防击退");
      d("LowInputLatency", "Reduces input latency by queueing clicks earlier.", "提前排队点击以降低输入延迟");
   }
}
