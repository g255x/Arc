package dev.g255.arc.mod.modules.settings;

import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import java.util.HashMap;
import java.util.Map;

public final class ChineseTranslator {
   private static final Map<String, String> SETTINGS = new HashMap();
   private static final Map<String, String> ENUMS = new HashMap();
   private static final Map<String, String> CATEGORY = new HashMap();
   private static final String[] RAW = new String[]{"1.12|1.12版本", "1.21|1.21版本", "2DArmorDura|2D护甲耐久", "2DHealth|2D生命条", "2DItem|2D物品", "2DOutline|2D描边", "AACPackets|AAC发包", "AbortPacket|中止发包", "AccelTime|加速时间", "Accelerate|加速", "ActiveColor|激活颜色", "ActiveKey|激活按键", "ActiveTime|激活时间", "AfterBreak|破坏后", "AfterLoginTime|登录后时间", "Aimbot|自瞄", "AirBox|空中包围盒", "AirFade|空中淡出", "AirFill|空中填充", "AirHeight|空中高度", "AirPlace|空中放置", "AirStop|空中停止", "AirYCheck|空中Y检查", "AllowCenter|允许居中", "AllowOffhand|允许副手", "AllowSneak|允许潜行", "AllowWeb|允许蛛网", "Alpha|不透明度", "Alternate|交替", "AlwaysSend|总是发送", "Ambients|环境音", "Anchor|锚点", "AnchorSpeed|锚点速度", "AnimEase|动画缓动", "AnimTime|动画时间", "Animals|动物", "Animation|动画", "AnimationTime|动画时长", "AntiCheat|反作弊", "AntiKick|防踢出", "AntiLag|防卡顿", "AntiStandUp|防起身", "AntiStep|防台阶", "AntiSuicide|防自杀", "AntiSurround|防包围", "AnyHole|任意坑", "ApiKey|API密钥", "ApplyYaw|应用偏航", "Armor|护甲", "ArmorBreaker|破甲", "ArmorDetection|护甲检测", "ArmorDuraColor|护甲耐久颜色", "ArmorEquip|护甲装备", "ArmorGlint|护甲附魔光", "ArmorLow|护甲过低", "ArmorOffset|护甲偏移", "ArmorPackets|护甲发包", "ArmorParts|护甲部位", "ArmorTrim|护甲纹饰", "ArrayList|列表显示", "Arrow|箭矢", "Arrows|箭矢", "Ascending|上坡", "Assist|协助", "AssistDamage|辅助伤害", "AssistDelay|辅助延迟", "AssistRange|辅助范围", "Attack|攻击", "AttackCrystals|攻击水晶", "AttackRange|攻击范围", "AttackRotation|攻击转头", "AttackSwing|攻击挥臂", "AttackVecStep|攻击向量步进", "Auto|自动", "AutoAnswer|自动应答", "AutoAuth|自动认证", "AutoBoost|自动加速", "AutoBurrow|自动埋身", "AutoCast|自动施放", "AutoDisable|自动关闭", "AutoElytra|自动鞘翅", "AutoFollow|自动跟随", "AutoJump|自动跳跃", "AutoLaunch|自动发射", "AutoOff|自动关闭", "AutoPhase|自动相位", "AutoQueue|自动排队", "AutoRelease|自动松开", "AutoReply|自动回复", "AutoReset|自动重置", "AutoShoot|自动射击", "AutoStart|自动开始", "AutoStop|自动停止", "AutoSwap|自动切换", "AutoSwitch|自动切换", "AutoSync|自动同步", "AutoTotem|自动图腾", "AutoWalk|自动行走", "BB-Offset|包围盒偏移", "BGColor|背景色", "BWallRange|隔墙范围", "BackGround|背景", "Background|背景", "BackgroundAlpha|背景不透明度", "BackgroundStyle|背景样式", "Balance|平衡", "BalanceOffset|平衡偏移", "BannerFade|横幅淡出", "BannerHold|横幅保持", "BannerPitch|横幅音调", "BannerSound|横幅音效", "BannerStack|横幅堆叠", "BannerStyle|横幅样式", "BaseBalance|基础平衡", "BaseDelay|基础延迟", "BaseMax|基础最大值", "BaseMin|基础最小值", "BasePlace|基础放置", "BasePlaceRange|基础放置范围", "Bed|床", "Beehive|蜂巢", "BeingUpdated|正在更新", "Bevel|斜面", "BevelCev|斜面Cev", "BevelDistance|斜面距离", "Blindness|失明", "BlindnessPause|失明暂停", "Blink|闪烁", "BlinkSpeed|闪烁速度", "BlockCheck|方块检查", "BlockExploit|方块漏洞", "BlockForHead|头部挡方块", "BlockOverlay|方块覆盖", "BlockRange|方块范围", "BlockTickNausea|方块刻眩晕", "BlocksPer|每击方块数", "PreExplosion|预判爆炸", "PreCrystal|预判水晶", "PreBroke|预判破块", "CrystalProtect|防围爆", "Blur|模糊", "BlurType|模糊类型", "BoatExpand|船只扩展", "BoatJump|船只跳跃", "BoatLongJump|船只远跳", "BoatSpeed|船只速度", "Body|身体", "Boost|加速", "BounceHeight|弹跳高度", "BouncePitch|弹跳音调", "BounceSpeed|弹跳速度", "Bows|弓", "Box|包围盒", "BoxColor|包围盒颜色", "Brand|服务器信息", "Break|破坏", "BreakCrystal|破坏水晶", "BreakDelay|破坏延迟", "BreakFailed|破坏失败", "BreakRange|破坏范围", "BreakTWall|破坏上方墙", "BreakTime|破坏时长", "BreakWall|破坏墙", "BreakerMin|破坏最小值", "Breaks|破坏次数", "Burrow|埋身", "BypassGround|绕过地面", "BypassTime|绕过时长", "C-Multiplier|C轴倍率", "C-Y|C轴Y", "C03Check|C03检查", "C03Range|C03范围", "CAPE|披风", "CPS|每秒点击", "CampFire|营火", "Cancel|取消", "CancelAll|取消全部", "CardBg|卡片背景", "CardWidth|卡片宽度", "CastDelay|施放延迟", "CastShadow|投影", "CategoryBarHeight|分类栏高度", "CategoryWidth|分类宽度", "CaughtException|捕获异常", "CensorCommands|隐藏命令", "CensorCoords|隐藏坐标", "Center|居中", "CevPause|Cev暂停", "ChamsFill|透视填充", "ChamsLine|透视线条", "ChamsTexture|透视纹理", "Charge|充能", "ChargeDelay|充能延迟", "Charging|充能中", "ChatControl|聊天控制", "ChatMode|聊天模式", "CheckDouble|检查双次", "CheckElytra|检查鞘翅", "CheckGround|检查地面", "CheckRange|检查范围", "CheckSelf|检查自身", "CheckSpeed|检查速度", "CheckWeb|检查蛛网", "Chest|箱子", "ChestCount|箱子数量", "ChestFill|箱子填充", "ChestOutline|箱子描边", "ChestUp|箱子上方", "Chests|箱子", "Chinese|中文", "Chorus|紫颂果", "ChunkColor|区块颜色", "Chunks-Distance|区块距离", "CircleSize|圆圈大小", "ClearTime|清除时间", "ClickDelay|点击延迟", "ClickSlot|点击槽位", "ClientCommand|客户端命令", "ClientSide|客户端侧", "ClientStatus|客户端状态", "Climb|攀爬", "CloseDelay|关闭延迟", "CloseScreen|关闭屏幕", "CloudColor|云颜色", "CollideSkip|碰撞跳过", "CollideSpeed|碰撞速度", "Color|颜色", "Color1|颜色1", "Color2|颜色2", "Color3|颜色3", "Color4|颜色4", "ColorMode|颜色模式", "ColorSync|颜色同步", "Colors|颜色", "Columns|列数", "Command|命令", "Completed|已完成", "Confirm|确认", "CoolDown|冷却", "Cooldown|冷却", "CooldownMode|冷却模式", "Coords|坐标", "Core|核心", "Correct|校正", "Count|数量", "Counter|计数器", "Cover|覆盖", "CraftPackets|合成发包", "Crawl|爬行", "CrawlingDisable|禁用爬行", "CrawlingSkip|跳过爬行", "Creatures|生物", "Crossbow|弩", "CrossbowSupport|弩支持", "Crossbows|弩", "Crosshair|准星", "Crystal|水晶", "CrystalMaxStage|水晶最大阶段", "CrystalScale|水晶缩放", "CrystalStage|水晶阶段", "Crystals|水晶", "Custom|自定义", "CustomLuminance|自定义亮度", "CustomMessage|自定义消息", "CustomName|自定义名称", "CustomTime|自定义时间", "CuteMode|可爱模式", "CuteStep|可爱步进", "Damage|伤害", "DamageText|伤害文字", "Darkening|变暗", "Darkness|黑暗", "DeSync|失同步", "Death|死亡", "DeathCoords|死亡坐标", "Debug|调试", "DebugException|调试异常", "Decode|解码", "DefaultColor|默认颜色", "DefaultTextColor|默认文字颜色", "Delay|延迟", "DelayUntilAttack|攻击前延迟", "Depth|深度", "DetectDouble|检测双次", "DetectMining|检测挖掘", "DetectMode|检测模式", "DetectProgress|检测进度", "DetectionRange|检测范围", "DiagonalAscend|对角上坡", "DiagonalDescend|对角下坡", "Died|已死亡", "DimensionColor|维度颜色", "Direction|方向", "Disable|禁用", "DisableLength|禁用时长", "DisableNotification|禁用通知", "DisableTime|禁用时间", "DisplayTime|显示时间", "Distance|距离", "Divider|分隔线", "Donkey|驴", "DonkeyCount|驴数量", "Double|双次", "DoubleBreak|双次破坏", "DoubleFill|双次填充", "DoubleOutline|双次描边", "DoubleStep|双次步进", "DoubleTapDelay|双击延迟", "DoubleTimeout|双次超时", "Down|下", "DownFactor|下降系数", "DownSpeed|下降速度", "Draw|绘制", "DrawItem|绘制物品", "Drawn|显示", "Drop|丢弃", "DuraScale|耐久缩放", "Durability|耐久", "DynamicPitch|动态俯仰", "E-GApple|末影金苹果", "EGapple|末影金苹果", "ESP|透视显示", "Ease|缓动", "Easing|缓动", "EatingBreak|进食打断", "EatingPause|进食暂停", "Edit|编辑", "EditTrash|编辑废料", "Effect|效果", "Effects|效果", "Eggs|鸡蛋", "Elements|元素", "Elytra|鞘翅", "Empty|空", "EnableLength|启用时长", "EnableStep|启用步进", "EnableTextColor|启用文字颜色", "Enabled|已启用", "Enchantments|附魔", "EncryptKey|加密密钥", "End|末地", "EndFill|末地填充", "EndOldChunks|末地旧区块", "EndOutline|末地描边", "EndPortalFill|末地传送门填充", "EndPortalOutline|末地传送门描边", "EndRotate|结束转头", "EndSwing|结束挥臂", "EnderChest|末影箱", "EnderChestFill|末影箱填充", "EnderChestOutline|末影箱描边", "EnderPearls|末影珍珠", "Enemies|敌人", "EnemyRange|敌人范围", "EnemySpot|敌人位置", "EntityFire|实体火焰", "EntityId|实体ID", "EntityRange|实体范围", "Exceed|超出", "Exp|经验", "ExpandIcon|展开图标", "ExpandLine|展开线", "Explode|爆炸", "ExplodeMode|爆炸模式", "Exploit|漏洞", "ExplosionMin|爆炸最小值", "Explosions|爆炸", "ExplosionsBoost|爆炸加速", "Extend|扩展", "FOV|视野", "FPS|帧率", "Face|朝向", "FaceUp|面朝上", "Factor|系数", "Flash|闪烁", "StreakSound|连杀音效", "StreakReset|连杀重置", "PillarDuration|光柱时长", "PillarWidth|光柱宽度", "PillarHeight|光柱高度", "PillarAlpha|光柱不透明度", "Fade|淡出", "FadeEase|淡出缓动", "FadeLength|淡出长度", "FadeSpeed|淡出速度", "FadeTime|淡出时间", "FailedSkip|失败跳过", "FakeMove|伪移动", "FallFlying|滑翔", "FallSpeed|下落速度", "FarCancel|远处取消", "Fast|快速", "FastArmor|快速护甲", "FastMove|快速移动", "Feet|脚部", "FeetExtend|脚部扩展", "Fill|填充", "FillDelay|充能延迟", "Filter|过滤", "Fire|火焰", "FireMaxStage|火焰最大阶段", "FireOverlay|火焰覆盖", "FireStage|火焰阶段", "FireWorkBind|烟花绑定", "Firework|烟花", "FireworkBind|烟花绑定", "Fireworks|烟花", "FirstColor|第一颜色", "FirstPersonSpeed|第一人称速度", "Fish|钓鱼", "FixedTicks|固定刻", "FlagInWall|墙内标记", "Flight|飞行", "Fog|雾", "FogColor|雾颜色", "FogDistance|雾距离", "FogEnd|雾结束", "FogStart|雾开始", "FollowSpeed|跟随速度", "Font|字体", "ForceBar|强制栏", "ForceDelay|强制延迟", "ForceDouble|强制双次", "ForceHead|强制头部", "ForceMin|强制最小值", "ForceOverworld|强制主世界", "ForcePlace|强制放置", "ForceQuickMove|强制快移", "ForceSneak|强制潜行", "ForceSync|强制同步", "ForceUpdate|强制更新", "Fov|视野", "FreeLook|自由视角", "Freeze|冻结", "Friend|好友", "FriendBox|好友包围盒", "FriendFill|好友填充", "FriendProtect|好友保护", "Friends|好友", "FuckFPSLimit|解除帧率限制", "FullBright|夜视", "FullPackets|完整发包", "GApple|金苹果", "Gamemode|游戏模式", "Gap|金苹果", "Gap-Pickaxe|金苹果-镐", "Gap-Sword|金苹果-剑", "Gap-Totem|金苹果-图腾", "GapSwitch|金苹果切换", "Gapple|金苹果", "Gear|齿轮", "GhRange|鬼手范围", "GhostHand|鬼手", "Glint|附魔光", "Glow|发光", "GlowAlpha|发光不透明度", "Glowstone|萤石", "GodMode|无敌模式", "GoldenOnly|仅金苹果", "Grim|Grim绕过", "GrimBypass|Grim绕过", "GrimDisabler|Grim禁用器", "GrimRotation|Grim转头", "Ground|地面", "GroundDistance|地面距离", "GroundSpoof|地面伪装", "Guardian|守卫者", "Gui|界面", "GuiMoveBypass|界面移动绕过", "GuiPause|界面暂停", "GuiSound|界面音效", "GuiToast|界面提示", "H-Factor|H轴系数", "HAT|帽子", "HP|生命", "HSpeed|水平速度", "Hand|手", "HandSwing|挥手", "Hands|双手", "Head|头部", "HeadExtend|头部扩展", "HeadFill|头部填充", "HeadKey|头部按键", "Health|生命", "Height|高度", "Helper|助手", "HideEmpty|隐藏空项", "HideIndicator|隐藏指示器", "HighHealth|高生命", "HitBoxSize|碰撞箱大小", "HitColor|命中颜色", "HitOutlineColor|命中描边颜色", "HitboxExpand|碰撞箱扩展", "HoldKey|按住键", "HoleRange|坑范围", "Horizontal|水平", "Hostiles|敌对", "HotbarAnim|快捷栏动画", "HotbarSwap|快捷栏切换", "HotbarTime|快捷栏时间", "HoverAlpha|悬停不透明度", "HoverColor|悬停颜色", "HpBarBg|血条背景", "HpBarFg|血条前景", "Hunger|饥饿", "HurtCam|受击视角", "HurtTime|受击时间", "IP|IP地址", "IPackets|入站发包", "IQ|智商", "Icon|图标", "IgnoreArmorStand|忽略盔甲架", "IgnoreBedrock|忽略基岩", "IgnoreCrawling|忽略爬行", "IgnoreCurse|忽略诅咒", "IgnoreFriends|忽略好友", "IgnoreMine|忽略挖掘", "IgnoreNakeds|忽略裸装", "IgnorePearlLag|忽略珍珠延迟", "InAir|空中", "InAirPause|空中暂停", "InAirSkip|空中跳过", "InBlock|方块内", "InBlockPause|方块内暂停", "InLiquid|液体中", "InMovement|移动中", "InWater|水中", "InWaterPause|水中暂停", "InWebPause|蛛网中暂停", "Indicator|指示器", "InfiniteChat|无限聊天", "InfiniteDura|无限耐久", "InfiniteMaxHeight|无限最大高度", "InfiniteMaxSpeed|无限最大速度", "InfiniteMinSpeed|无限最小速度", "InfoColor|信息颜色", "InnerFrame|内框", "Instant|瞬时", "InstantRotate|瞬时转头", "Intelligence|智能", "InteractBlock|交互方块", "InteractEntity|交互实体", "InteractEntityRange|交互实体范围", "InteractInCalc|计算中交互", "InteractItem|交互物品", "InteractOnRender|渲染时交互", "InteractRotation|交互转头", "InteractSwing|交互挥臂", "Interval|间隔", "Invalid-Offset|无效偏移", "InvalidPacket|无效发包", "Invalids|无效", "InventoryPause|背包暂停", "InventorySwap|背包取用", "Invisible|隐身", "Item|物品", "ItemFill|物品填充", "ItemFov|物品视野", "ItemFrame|物品展示框", "ItemName|物品名称", "ItemOutline|物品描边", "ItemSpoof|物品伪装", "Items|物品", "JACKET|外套", "Jitter|抖动", "Jitter-XZ|水平抖动", "Jitter-Y|垂直抖动", "Join|加入", "JoinQueueDelay|加入队列延迟", "Joins|加入", "Jump|跳跃", "JumpAt256|256格跳跃", "JumpDisable|禁用跳跃", "JumpSpeed|跳跃速度", "KeepHistory|保留历史", "KeepRotate|保持转头", "Key|按键", "KillRange|击杀范围", "Kit|装备包", "KitExceed|装备包超出", "KitName|装备包名称", "LMaxPitch|升级音最高", "LMinPitch|升级音最低", "Ladder|梯子", "LagBack|回弹", "LagDelay|延迟传输", "LagFly|延迟飞行", "LagMode|延迟模式", "LagPause|延迟暂停", "LagTime|延迟时长", "LagbackCheck|回弹检查", "Leave|离开", "LegAnchor|腿部锚定", "Legit|合法", "LegitMine|合法挖掘", "Legs|腿部", "Length|长度", "Lerp|插值", "LessCPU|降低CPU占用", "LethalCrystal|致命水晶", "LethalOverride|致命覆盖", "LevelUp|升级", "LeverButton|拉杆按钮", "Levitation|漂浮", "Lightning|闪电", "LightsUpdate|光照更新", "LimitPackets|限制发包", "Line|线条", "Line1|第一行", "Line2|第二行", "Line3|第三行", "Line4|第四行", "LineHeight|线高", "LineWidth|线宽", "LiquidExploit|液体漏洞", "ListSort|列表排序", "Lock|锁定", "LockPitch|锁定俯仰", "LockTarget|锁定目标", "Log|日志", "LogOnArrival|到达时记录", "LogOnEnable|启用时记录", "Look|注视", "LookAtHead|注视头部", "LookTime|注视时长", "LowArmor|低护甲", "LowColor|低颜色", "LowHealth|低生命", "LowerCase|小写", "LowerThan|低于", "Luminance|亮度", "MPackets|移动发包", "MaceOnly|仅重锤", "MainHandTotem|主手图腾", "MainhandSwap|主手切换", "Max|最大", "MaxBlocks|最大方块数", "MaxBreak|最大破坏数", "MaxDurable|最大耐久", "MaxEntries|最大条目", "MaxMotionXZ|最大水平动量", "MaxMotionY|最大垂直动量", "MaxOverride|最大覆盖", "MaxPackets|最大发包", "MaxRange|最大范围", "MaxSample|最大采样", "MaxSelf|最大自伤", "MaxSelfDmg|最大自身伤害", "MaxSpeed|最大速度", "MaxTargets|最大目标数", "MaxTime|最大时长", "MaxWebs|最大蛛网数", "Message|消息", "MessageDelay|消息延迟", "MessageMode|消息模式", "MidHealth|中等生命", "Min|最小", "MinAge|最小年龄", "MinCharge|最小充能", "MinDamage|最小伤害", "MinFallSpeed|最小下落速度", "MinLength|最小长度", "MinPDamage|最小玩家伤害", "MinRange|最小范围", "MinReleaseCharge|最小释放充能", "MinSpeed|最小速度", "MinStep|最小步进", "MinTimeout|最小超时", "Mining|挖掘时", "Mine|挖掘", "MineAir|挖掘空", "MineDownward|向下挖掘", "Mini|迷你", "Minimize|最小化", "MiningProgress|挖掘进度", "MioCompatible|Mio兼容", "Mobs|怪物", "Mode|模式", "Model|模型", "Module|模块", "ModuleBinds|模块按键", "ModuleButtonHeight|模块按钮高度", "ModuleButtonWidth|模块按钮宽度", "ModuleToggle|模块切换", "Monsters|怪物", "Motion|动量", "MotionStop|动量停止", "MotionY|垂直动量", "Mouse|鼠标", "MouseMove|鼠标移动", "MouseMoveSmooth|鼠标移动平滑", "MouseMoveStrength|鼠标移动强度", "Move|移动", "MoveDis|移动距离", "MoveDis2|移动距离2", "MoveDisable|禁用移动", "MoveFull|全量移动", "MoveGround|地面移动", "MoveIn|移入", "MoveLagMode|移动延迟模式", "MoveLook|移动注视", "MovePosition|移动位置", "Movement|移动", "MovingPause|移动暂停", "MultiPlace|多重放置", "MultiTask|多任务", "Nakeds|裸装", "Name|名称", "NameColor|名称颜色", "Nausea|眩晕", "NetherOldChunks|下界旧区块", "New|新", "NoBadSlot|无坏槽位", "NoBlockDisable|无方块禁用", "NoBlockPush|无方块推挤", "NoClimb|无攀爬", "NoCollide|无碰撞", "NoCrystal|无水晶", "NoDrag|无拖拽", "NoEating|无进食", "NoEntityPush|无实体推挤", "NoEntityTrace|无实体射线", "NoFail|无失败", "NoFirstPerson|无第一人称", "NoFishBob|无鱼漂", "NoFront|无正面", "NoInWall|无墙内", "NoInteract|无交互", "NoLag|无延迟", "NoLimb|无四肢", "NoMineAbort|挖掘不中止", "NoMineDelay|无挖掘延迟", "NoMineReset|挖掘不重置", "NoMove|无移动", "NoMovePacket|无移动发包", "NoPing|无Ping", "NoRelease|无释放", "NoRotation|无转头", "NoSelf|排除自己", "NoSelfPos|排除自身位置", "NoSuicide|防自杀", "NoWaterPush|无水推挤", "Notification|通知", "Notify|通知", "OPackets|出站发包", "Obsidian|黑曜石", "Octaves|八度", "OffhandSwap|副手切换", "Offset|偏移", "OffsetY|Y偏移", "Old|旧", "OldVersion|旧版本", "OnAdd|添加时", "OnBreak|破坏时", "OnEnable|启用时", "OnKey|按键时", "OnPlace|放置时", "OnPop|爆裂时", "OnlyActive|仅激活", "OnlyBelow|仅下方", "OnlyBreak|仅破坏", "OnlyBreaking|仅破坏中", "OnlyBroken|仅已破坏", "OnlyBurrow|仅埋身", "OnlyCev|仅Cev", "OnlyCrawling|仅爬行", "OnlyCrystal|仅水晶", "OnlyCustom|仅自定义", "OnlyElytra|仅鞘翅", "OnlyEnemies|仅敌人", "OnlyEnemy|仅敌人", "OnlyGround|仅地面", "OnlyHard|仅坚硬", "OnlyHead|仅头部", "OnlyHold|仅按住", "OnlyHole|仅坑", "OnlyInsideBlock|仅方块内", "OnlyLooking|仅注视", "OnlyMining|仅在挖掘时", "OnlyMove|仅移动", "OnlyMoving|仅移动中", "OnlyObby|仅黑曜石", "OnlyOne|仅一个", "OnlyPickaxe|仅镐", "OnlyPress|仅按下", "OnlySelf|仅自身", "OnlySneak|仅潜行", "OnlySurround|仅包围", "OnlyTargets|仅目标", "OnlyWeapon|仅武器", "Opacity|不透明度", "Open|打开", "OptimizedCalc|优化计算", "Others|其他", "OuterFrame|外框", "Outline|描边", "OutlineColor|描边颜色", "OverrideMax|覆盖最大值", "OverworldOldChunks|主世界旧区块", "PWallRange|穿墙范围", "Packet|发包", "PacketDelay|发包延迟", "PacketInteract|发包交互", "PacketMine|发包挖掘", "PacketPlace|发包放置", "Packets|发包", "Page|页面", "Paint|涂漆", "PaletteExploit|调色板漏洞", "Parkour|跑酷", "ParkourAscend|跑酷上坡", "ParkourPlace|跑酷放置", "PathingPause|寻路暂停", "Pause|暂停", "PauseEat|暂停进食", "PauseModule|暂停模块", "Pearl|珍珠", "PearlOwner|珍珠主人", "Pearls|珍珠", "Peek|窥视背包", "PerRow|每行", "PerTick|每刻", "Percent|百分比", "PerferOpen|优先打开", "Phase|相位", "PhasePause|相位暂停", "PhaseTime|相位时间", "PickInventory|拾取背包", "PickSwap|拾取切换", "Pickaxe|镐", "Ping|延迟", "Piston|活塞", "PistonMaxStage|活塞最大阶段", "PistonPacket|活塞发包", "PistonStage|活塞阶段", "Pitch|俯仰", "PitchJitter|俯仰抖动", "PitchLock|俯仰锁定", "PitchValue|俯仰值", "Place|放置", "PlaceDelay|放置延迟", "PlaceKey|放置按键", "PlaceMode|放置模式", "PlacePosMode|放置位置模式", "PlaceRange|放置范围", "PlaceRotate|放置转头", "PlaceTWall|放置上方墙", "Placement|放置", "Play|播放", "PlaySound|播放音效", "Player|玩家", "PlayerAction|玩家动作", "PlayerCollision|玩家碰撞", "PlayerContent|玩家内容", "PlayerFill|玩家填充", "PlayerOutline|玩家描边", "PlayerScale|玩家缩放", "Players|玩家", "Pong|Pong包", "PopCounter|爆裂计数", "Pops|爆裂", "Portal|传送门", "PosUpdateDelay|位置更新延迟", "PositionMainX|主场X", "PositionMainY|主场Y", "PositionMainZ|主场Z", "PositionOffX|副场X", "PositionOffY|副场Y", "PositionOffZ|副场Z", "PositionPackets|位置发包", "PositionSync|位置同步", "Potion|药水", "Potions|药水", "PotionsIcon|药水图标", "PowerMaxStage|能量最大阶段", "PowerPacket|能量发包", "PowerStage|能量阶段", "PreCorrect|预校正", "Predict|预测", "PredictTarget|预测目标", "PredictTicks|预测刻", "PredictionFactor|预测系数", "PredictionTicks|预测刻", "Prefer|优先伤害", "PreferAnchor|优先锚点", "PreferCrystal|优先水晶", "PreferHead|优先头部", "PreferWeb|优先蛛网", "Prefix|前缀", "Pressed|已按下", "PriorHotbar|前快捷栏", "Priority|优先级", "Progress|进度", "Pulse|脉冲", "PulseSize|脉冲大小", "PulseSpeed|脉冲速度", "RIGHT_PANTS_LEG|右裤腿", "RIGHT_SLEEVE|右袖", "Radius|半径", "Rainbow|彩色", "RainbowSpeed|彩色速度", "Random|随机", "RandomChars|随机字符", "RandomWhisper|随机私聊", "Range|范围", "Ratio|比例", "Reach|触及距离", "ReceiveMessage|接收消息", "RecenterLayout|重新居中布局", "Record|记录", "Rect|矩形", "Rectangle|矩形", "Red|红色", "RedStone|红石", "Redstone|红石", "RegearPause|重整暂停", "Rejoin|重新加入", "ReleaseMode|释放模式", "ReleaseSneak|释放潜行", "Remove|移除", "RemovePortal|移除传送门", "RemoveRenderDist|移除渲染距离", "RemoveTotem|移除图腾", "Rename|重命名", "Render|渲染", "RenderBox|渲染包围盒", "RenderHeight|渲染高度", "RenderSidesOfItems|渲染物品侧面", "RenderTime|渲染时间", "RenderingUp|向上渲染", "Repeater|中继器", "Replace|替换", "Reply|回复", "ReplyColor|回复颜色", "ReplyPrefix|回复前缀", "Reserve|保留", "Reset|重置", "ResetAttack|重置攻击", "ResetDelay|重置延迟", "Resistance|抗性", "ResistanceKey|抗性按键", "ResistanceLevelCheck|抗性等级检查", "Respawn|重生", "Rotate|转头", "RotateMode|转头模式", "Rotation|转头", "RotationBypass|转头绕过", "RotationDelay|转头延迟", "RotationMainX|转头主场X", "RotationMainY|转头主场Y", "RotationMainZ|转头主场Z", "RotationOffX|转头副场X", "RotationOffY|转头副场Y", "RotationOffZ|转头副场Z", "RubberbandDelay|回弹延迟", "S2CCloseScreen|关屏发包", "S2CInventory|背包发包", "S2CRotate|转头发包", "S2CSlot|槽位发包", "S2cVelocity|速度发包", "SPackets|服务器发包", "Safe|安全", "SafeBox|安全包围盒", "SafeFade|安全淡出", "SafeFill|安全填充", "SafeRange|安全范围", "SafeWalk|安全行走", "Sand|沙子", "Saturation|饱和度", "Scaffolding|脚手架", "Scale|缩放", "ScaleMainX|缩放主场X", "ScaleMainY|缩放主场Y", "ScaleMainZ|缩放主场Z", "ScaleOffX|缩放副场X", "ScaleOffY|缩放副场Y", "ScaleOffZ|缩放副场Z", "Scaling|缩放", "ScrollAnim|滚动动画", "ScrollAnimEasing|滚动缓动", "ScrollAnimLength|滚动长度", "Second|第二", "SecondBox|第二包围盒", "SecondColor|第二颜色", "SecondFill|第二填充", "Segments|分段", "Self|自身", "SelfGround|自身地面", "SelfPredict|自身预测", "SelfPredictTicks|自身预测刻", "SelfRange|自身范围", "SemiSafeBox|半安全包围盒", "SemiSafeFill|半安全填充", "SendMessage|发送消息", "ServerLag|服务器延迟", "ServerSide|服务器侧", "SetAir|设置空气", "SetFlag|设置标记", "SetNoGround|设置无地面", "Shadow|阴影", "Shift|潜行键", "ShiftTimer|潜行计时", "ShowPlayerName|显示玩家名", "ShowReason|显示原因", "ShowRotations|显示转头", "Shrink|收缩", "Shulker|潜影盒", "ShulkerBox|潜影盒", "ShulkerBoxFill|潜影盒填充", "ShulkerBoxOutline|潜影盒描边", "Shulkers|潜影盒", "SideCheck|侧面检查", "Sign|告示牌", "SignContent|告示牌内容", "SignCount|告示牌数量", "Silent|静默", "Simulation|模拟", "SingleBlock|单方块", "Size|大小", "SkyColor|天空颜色", "SliderSpeed|滑块速度", "Slimes|史莱姆", "Slot|槽位", "SlotPackets|槽位发包", "SlowCheck|缓慢检查", "SlowFalling|缓降", "SlowSend|缓慢发送", "Slowness|缓慢", "Smart|智能", "SmartDisable|智能禁用", "SmartDist|智能距离", "SmartDistance|智能距离", "SmartDown|智能向下", "SmartTrace|智能射线", "SmartUp|智能向上", "SmartXZ|智能水平", "Smooth|平滑", "Smoothness|平滑度", "SnapBack|回位", "Sneak|潜行", "Sneaking|潜行中", "SneakingPause|潜行暂停", "Snow|雪花", "SnowAlpha|雪花不透明度", "SnowAmount|雪花数量", "SnowShape|雪花形状", "SnowSize|雪花大小", "SnowSpeed|雪花速度", "SnowWind|雪花风", "Snowballs|雪球", "Sort|排序", "SoulSand|灵魂沙", "Sound|音效", "SoundPitch|音调", "Space|空格", "Spam|连炸", "SpamDelay|连炸延迟", "SpamPlace|连续放置", "Speed|速度", "SpeedKey|速度按键", "SpeedLimit|速度限制", "SpinSpeed|旋转速度", "SpinSync|旋转同步", "SplashDetection|喷溅检测", "Spoof|伪装", "SpoofMode|伪装模式", "Spoofs|伪装", "Sprint|疾跑", "Stack|堆叠", "Stage|阶段", "Start|开始", "StartDelay|开始延迟", "StartFade|开始淡出", "StartFill|开始填充", "StartOutline|开始描边", "StartRotate|开始转头", "Static|静态", "Step|步进", "StepHeight|步进高度", "Steps|步数", "StopDelay|停止延迟", "Strength|强度", "StrengthKey|强度按键", "Strict|严格", "Strict-Liquid|严格液体", "StrictDirection|严格方向", "StrictSpeed|严格速度", "Style|样式", "Surround|包围", "SurroundCheck|包围检查", "SurroundPause|包围暂停", "SwapMode|切换模式", "Swing|挥臂", "SwingSide|挥臂侧", "SwingSpeed|挥臂速度", "Switch|切换", "SwitchEat|切换进食", "SwitchPause|切换暂停", "SwitchReset|切换重置", "SwitchToBow|切换至弓", "Sword|剑", "Sync|同步", "SyncWeb|同步蛛网", "TMaxPitch|三叉戟音最高", "TMinPitch|三叉戟音最低", "TPS|TPS", "TPSSync|TPS同步", "Take|拿取", "Target|目标", "TargetColor|目标颜色", "TargetESP|目标透视", "TargetMode|目标模式", "TargetOutlineColor|目标描边颜色", "TargetPlayer|目标玩家", "TargetRange|目标范围", "TasksPerTick|每刻任务数", "Teleport|传送", "TeleportConfirm|传送确认", "TerrainIgnore|忽略地形", "Text|文字", "TextColor|文字颜色", "TextOffset|文字偏移", "Texture|纹理", "Thickness|厚度", "Thread|线程", "Threshold|阈值", "ThroughWall|穿墙", "Throw|投掷", "TickShift|刻位移", "TicksAutoCast|自动施放刻", "TicksCatch|捕捉刻", "TicksThrow|投掷刻", "Tights|紧身", "Time|时间", "TimeOut|超时", "Timeout|超时", "TimeoutDelay|超时延迟", "TimeoutMin|最小超时", "Timer|计时器", "TimerBypass|计时器绕过", "Timestamp|时间戳", "Timing|时序", "Tint|着色", "Tips|提示", "Title|标题", "TitleFix|标题修复", "TitleOffset|标题偏移", "TitleOverride|标题覆盖", "ToggleBanner|切换横幅", "ToolTips|工具提示", "Top|顶部", "TopAlpha|顶部不透明度", "Torch|火把", "Consecutive|连续推", "Merge|合并推", "OutOfBlock|推出卡点", "Totem|图腾", "TotemLess|少图腾", "Totems|图腾", "Tower|高塔", "Translate|平移", "Trash|废料", "TrashMode|废料模式", "Trident|三叉戟", "Tridents|三叉戟", "TrueAttackCD|真实攻击冷却", "TryPlaceBox|尝试放置包围盒", "TryPlaceFill|尝试放置填充", "Type|类型", "UPDirectionLimit|上传方向限制", "UnbreakableCancel|不可破坏取消", "UnsafeBox|危险包围盒", "UnsafeFade|危险淡出", "UnsafeFill|危险填充", "Up|上", "UpFactor|上升系数", "UpPitch|上升俯仰", "UpSpeed|上升速度", "UpdateDelay|更新延迟", "UpdateInterval|更新间隔", "UpdateSlot|更新槽位", "UpwardVelocity|上升速度", "UseDelay|使用延迟", "UseInventory|使用背包", "UseTimer|使用计时器", "UsingPause|使用中暂停", "V-Factor|V轴系数", "VPackets|速度发包", "VSpeed|垂直速度", "Value|值", "VehicleMove|载具移动", "Velocity|速度", "VelocityBoost|速度加速", "VelocityXZ|水平速度", "VelocityY|垂直速度", "Vertical|垂直", "VerticalSpeed|垂直速度", "Verticals|垂直", "Villagers|村民", "Vines|藤蔓", "Visible|可见", "VisualRange|视觉范围", "VoidHeight|虚空高度", "Wait|等待", "WaitHurt|等待受击", "WaitPlace|等待放置", "WaitTime|等待时间", "WaitTimeOut|等待超时", "WalkRadius|行走半径", "WalkShake|行走晃动", "WalkShakeMax|行走晃动最大", "WalkShakeSmooth|行走晃动平滑", "WalkShakeSpeed|行走晃动速度", "WalkShakeStrength|行走晃动强度", "WallBox|墙包围盒", "WallFade|墙淡出", "WallFill|墙填充", "WallHeight|墙高度", "WallRange|墙范围", "WallSideBox|墙侧包围盒", "WallSideFade|墙侧淡出", "WallSideFill|墙侧填充", "WaterBucketFall|水桶下落", "WaterMark|水印", "WaterOverlay|水面覆盖", "WeaponOnly|仅武器", "WeaponsOnly|仅武器", "Weather|天气", "Web|蛛网", "WebReset|蛛网重置", "WebTime|蛛网时间", "WhenLower|低于时", "WhenSpamming|连炸时", "While|当…时", "WhileLiquid|液体中时", "WhilePushOut|推出时", "WhileUsing|使用时", "Width|宽度", "WindowPackets|窗口发包", "WindowTitle|窗口标题", "WithOffhand|用副手", "WorldColor|世界颜色", "WorldLeaveRemove|离开世界移除", "X|X轴", "X/Z-Multiplier|X/Z倍率", "XOffset|X偏移", "XP|经验", "XZSpeed|水平速度", "Y|Y轴", "Y-Multiplier|Y倍率", "YOffset|Y偏移", "YSpeed|垂直速度", "Yaw|偏航", "YawDeceive|偏航欺骗", "YawLock|偏航锁定", "YawStep|偏航步进", "Zero-Speed|零速度", "Zero-Teleport|零传送", "Zero-Y|零Y", "ZoomFov|缩放视野", "Zoomies|缩放", "恢复启用|恢复启用", "白名单|白名单", "blockUpdateExploit|方块更新漏洞", "delayUntilSwitch|切换前延迟", "password|密码", "ActiveHRange|激活水平范围", "AimJitter|自瞄抖动", "AllowKey|允许按键", "AllowMove|允许移动", "AllowSprint|允许疾跑", "AlwaysLag|总是加伤", "AmbientsBox|环境生物框", "AnchorY|锚点Y", "APIUrl|API地址", "Armor Dura|护甲耐久", "Armor Dura Color|护甲耐久颜色", "AttackAPS|攻击速度", "AttackFactor|攻击系数", "AutoElytraFlight|自动鞘翅飞行", "AutoMaceSpoof|自动风锤加伤", "AutoStartElytra|自动起飞", "AutoTarget|自动选目标", "Avoid|回避", "AvoidContainers|回避容器", "AvoidRange|回避范围", "AvoidSide|回避侧面", "AvoidUp|回避上方", "BackDelay|回弹延迟", "Bg Color|背景颜色", "Bitrate|比特率", "BlockIn|方块内", "BlockInGrim|方块内Grim", "BlockInKeepY|方块内保持Y", "BlockSlow|方块减速", "BlockSpecial|特殊方块", "Boots|靴子", "Border|边框", "BorderColor|边框颜色", "BorderPadding|边框内边距", "ButtonColor|按钮颜色", "Check|检查", "CheckUpH|检查上升高度", "Chestplate|胸甲", "CollarColor|项圈颜色", "CreaturesBox|生物框", "CrystalColor|水晶颜色", "CrystalColor2|水晶颜色2", "CrystalsBox|水晶框", "CustomFont|自定义字体", "DeathFadeMs|死亡淡出时长", "Disable Color|禁用颜色", "DisableInteract|禁用交互", "draw Item Count|显示物品数量", "draw Item Name|显示物品名称", "DurabilityThreshold|耐久阈值", "Enable|启用", "Enable Color|启用颜色", "EnemyOffsetX|敌人X偏移", "EnemyOffsetY|敌人Y偏移", "ExcludeWater|排除水源", "ExplodeRange|爆炸范围", "EzHoldMs|连杀按住时长", "FakeSneak|伪装潜行", "FakeSneakMode|伪装潜行模式", "FakeY4|上移4格包", "FirstPerson|第一人称", "FlightGraceMs|飞行宽限时长", "FollowPlayer|跟随玩家", "ForwardMove|前进移动", "ForwardSpeed|前进速度", "ForwardVelocity|前进动量", "Friendly|友方", "FriendsBox|好友框", "General|通用", "GrimSafe|Grim安全", "HealthBar|血条", "HealthBarColor|血条颜色", "HealthThreshold|生命阈值", "HeightPos|高度位置", "Helmet|头盔", "High Health Color|高血量颜色", "HitAnimation|受击动画", "HitSounds|命中音效", "HoldMode|按住模式", "Horizon|地平线", "Hud|HUD面板", "HudScale|HUD缩放", "HudX|HUD横移", "HudY|HUD纵移", "IgnoreTeams|忽略队伍", "Info Color|信息颜色", "InitialDelay|初始延迟", "InstantBreak|瞬间破坏", "InternalCam|内置视角", "Item Count Color|物品数量颜色", "Item Name Color|物品名称颜色", "ItemId|物品ID", "ItemTransform|物品变换", "JumpTick|跳跃刻", "LandDist_Normal|普通落地距离", "LandDist_Prog|渐进落地距离", "LandDistance|落地距离", "LEFT_PANTS_LEG|左裤腿", "LEFT_SLEEVE|左袖", "Leggings|护腿", "LocalOnly|仅本地", "LockOne|锁定一个", "LoveAnimMax|爱心动画最大", "LoveAnimMin|爱心动画最小", "Low Health Color|低血量颜色", "MaintainTime|维持时间", "MaxDistance|最大距离", "MaxFPS|最大帧率", "MaxQueued|最大队列", "Mid Health Color|中血量颜色", "MinFallHeight|最小下落高度", "MinFPS|最小帧率", "MinY|最小Y", "ModelSize|模型大小", "MonstersBox|怪物框", "MovePause|移动暂停", "Msg Color|消息颜色", "Nebula1|星云1", "Nebula2|星云2", "NextTarget|下一目标", "NoAnimation|无动画", "NoExplode|无爆炸", "NoFallFlying|禁用滑翔", "NormalTimer|普通计时倍率", "NoWeb|无蛛网", "NurikZapen|作者彩蛋", "OffsetX|X偏移", "OnlyInside|仅方块内", "OnlyNoInside|仅方块外", "OthersBox|其他框", "Overshoot|过冲", "PauseCity|暂停城市", "Peers|节点列表", "PlayersBox|玩家框", "Port|端口", "PosX|X坐标", "PosY|Y坐标", "ProgMaxHeight|渐进最大高度", "ProgStep|渐进步长", "ProgTimer|渐进计时倍率", "PulseCounter|脉冲计数", "QueueCheck|队列检查", "renderHealth|渲染血量", "RetargetRange|换目标范围", "Rotations|转头", "RotationSpeed|转头速度", "SafeHealth|安全生命", "SBThreshold|回弹阈值", "SBWindow|回弹窗口", "Settings|设置", "ShakeDuration|晃动时长", "ShakeIntensity|晃动强度", "Shoulder|肩膀", "ShowCount|显示数量", "ShowIcon|显示图标", "Side|侧面", "SilentSwap|静默切换", "SilentSwitch|静默切换", "SkipBuried|跳过掩埋", "SkipDead|跳过死亡", "SkipFriends|跳过好友", "SkipLogged|跳过已登录", "SlowFallingCheck|缓降检查", "SmartCheck|智能检查", "SmartDelay|智能延迟", "SmartH|智能水平", "SmartLag|智能加伤", "SmartV|智能垂直", "Sounds|音效", "Source|音源", "Spacing|间距", "SpoofGround|伪装地面", "SpringBack|回弹", "Stand|站立", "Stars|星星", "Stars1|星星1", "Stars2|星星2", "StrongLoop|强力循环", "SuppressEquip|抑制装备动作", "SwingDuration|挥臂时长", "SwingTicks|挥臂刻", "SwitchDelay|切换延迟", "SwitchRange|切换范围", "TargetCount|目标数量", "TargetSwitch|目标切换", "Text Offset|文字偏移", "TextScale|文字缩放", "ThemeColor|主题颜色", "ThirdPerson|第三人称", "ThrowDelay|投掷延迟", "TimerMode|计时模式", "TimerMultiplier|计时倍率", "TinyMobs|小型生物", "Title Color|标题颜色", "TotemColor|图腾颜色", "TotemColor2|图腾颜色2", "TransitionDelay|过渡延迟", "TurnDelay|转向延迟", "UseAnimation|使用动画", "UseFirework|使用烟花", "UseItem|使用物品", "UseItemBypass|使用物品绕过", "UseSounds|使用音效", "UVStars1|星图UV1", "UVStars2|星图UV2", "Variant|变体", "ViaUseFallback|使用回退", "VisualSpoof|视觉伪装", "Volume|音量", "WidthSize|宽度大小", "YMove|Y移动", "StealAll|全部拿取", "BounceInterval|弹跳间隔", "MinFallDist|最小落距", "FakeHeight|伪造高度", "FakeHeightVal|伪造高度值", "Rubberband|橡皮筋", "RBHeight|橡皮筋高度", "FakeFall|伪造下落", "FakeFallDistance|伪造落距", "FakeFallSteps|伪造落距步数", "Vanilla|原版", "NCP|NCP", "SwapVanilla|切换原版", "Bounce|弹跳", "VoidY|虚空Y", "APT|每次攻击数", "AutoMace|自动锤", "BreachSwap|破甲切换", "AutoCrystalSync|水晶同步", "CustomDelay|自定义延迟", "AttackDelay|攻击延迟", "DisableInteract|禁用交互", "TargetRange|目标范围", "TriggerDistance|触发距离", "LagbackDistance|回弹距离", "SpoofHeight|伪造高度", "CooldownTicks|冷却刻", "TimerSpeed|计时速度", "MaceOnly|仅锤", "Silent|静默", "RenderMode|渲染模式", "SilentPlayerBox|静默玩家包围盒", "OnlyFalling|仅下落", "OnlyAir|仅空中", "AutoAttack|自动攻击", "Set|定点", "GroundSpoof|地面伪造", "AttackPull|攻击拉扯", "DelayUntilSwitch|切换延迟", "DelayUntilAttack|攻击延迟", "SetRange|定点范围", "NoWait|无需等待", "Swing|挥臂", "Fly|飞行", "FallHeight|下落高度", "ResetDistance|重置距离", "RequireMace|需要锤", "LockView|锁定视角", "SplitStep|分割步长", "BlinkDelay|闪烁延迟", "Spoof|伪造", "VMMode|虚空锤模式", "VoidY|虚空Y", "VMFallHeight|虚空锤落距", "TPCooldown|传送冷却", "Villagers|村民", "Animals|动物", "Slimes|史莱姆", "KeepVelocity|保留速度", "AntiAir|防空检查", "AttackPackets|攻击包数", "VerticalSplit|垂直分割", "Hybrid|混合", "BlinkBuffer|闪烁缓冲", "MotionSpoof|移动伪造", "TheVoid|虚空", "VoidMace|虚空锤", "TP|传送", "DoublePacket|双包", "Legacy|旧版", "Range|范围", "Trap|陷阱", "TrapExtend|陷阱扩展", "Support|支撑", "CrawlHead|爬行头围", "Selection|选择", "Whitelist|白名单", "Blacklist|黑名单", "FriendSync|好友同步", "MineRange|挖掘范围", "FastBypass|快速绕过", "Restart|重启挖掘", "RestartDelay|重启延迟", "GlassDelay|玻璃延迟", "BlockList|方块列表"};

   /** 后续补充的配置项中文名（Key|中文），由静态块并入 SETTINGS */
   private static final String[] RAW_EXTRA = new String[]{
         "Dynamic|动态包挖",
         "DynamicInterval|动态检测间隔",
         "InstantDelay|瞬挖间隔",
         "SelfDamage|自伤判定",
         "StickyFill|持续填充",
         "StickyTime|持续时长",
         "DropEnabled|启用丢弃",
         "DropExcess|丢弃多余",
         "DropWrongItems|丢弃杂物",
         "IgnoreArmor|忽略护甲",
         "IgnoreShulker|忽略潜影盒",
         "InventoryOnly|仅背包",
         "OnlyHotbar|仅快捷栏",
         "PredictMine|预判挖掘",
         "MaxSelfDamage|最大自身伤害",
         "OnlyMain|仅主手暂停",
         "PistonCrystalPlus|活塞水晶+",
         "EatShakeMultiplier|食用晃动倍率",
         "SortItems|整理顺序",
         "StoreCount|存放数量",
         "StoreItems|存放物品",
         "TargetShulkers|目标潜影盒",
         "CopyState|复制状态",
         "OnlyFriends|仅好友",
         "DynamicFloat|动态浮动",
         "FloatRange|浮动范围",
         "FloatSpeed|浮动速度",
         "MinOpacity|最小不透明度",
         "NegativePitchLimit|负俯仰上限",
         "PositivePitchLimit|正俯仰上限",
         "RaiseReduceOpacity|抬头降透明",
         "TrackCamera|跟随视角",
         "TrackRange|跟随距离",
         "2QE_|2QE填充",
         "AutoCenter|自动居中",
         "CancelBlink|取消闪现",
         "YawStepSteps|偏航分步数",
         "cancuiHeight|下压高度",
         "AI|AI模式",
         "AnyFood|任意食物",
         "AutoClose|自动关闭",
         "AutoEat|自动进食",
         "AutoOpen|自动打开",
         "DropTridents|丢弃三叉戟",
         "DurabilityManagement|耐久管理",
         "GetScore|获取分数",
         "Inventory|背包",
         "Nuker|范围破坏",
         "RedSand|红沙",
         "Repeat|重复",
         "TimeoutClose|超时关闭",
         "BurrowBox|埋身描边",
         "BurrowFill|埋身填充",
         "SurroundBox|包围描边",
         "SurroundFill|包围填充",
         "BeingUpdatedColor|更新中颜色",
         "Layout|布局",
         "TooltipAlpha|提示框透明度",
         "UI|界面主题",
         "FastLayer|快速层",
         "OnlyBestDmg|仅最高伤害",
         "PlaceRate|放置频率",
         "TranslateDistance|平移距离",
         "custom-x|自定义X",
         "custom-y|自定义Y",
         "custom-z|自定义Z",
         "DetectRange|检测范围",
         "DetectShulkers|检测潜影盒",
         "CheckHotbar|检查快捷栏",
         "CheckInventory|检查背包",
         "CheckOffhand|检查副手",
         "DropFullShulkers|丢弃满潜影盒",
         "OnlyWhenEmpty|仅在空时",
         "NoFriend|排除好友",
         "CheckFly|检查飞行",
         "ControlMove|接管移动",
         "DownPitch|下俯角",
         "MaxDelay|最大延迟",
         "MinDelay|最小延迟",
         "MoveDownPitch|下移俯角",
         "MovePitch|移动俯角",
         "MoveUpPitch|上移俯角",
         "SlowPass|慢速通过",
         "StrictStartFly|严格起飞",
         "SyncInv|同步背包",
         "Names|名字列表",
         "Aim|瞄准",
         "FallDistance|下落距离",
         "GroundDisable|落地关闭",
         "SendMSG|发送消息",
         "Multiplier|倍率",
         "ExpandPitch|展开音调",
         "ExpandSound|展开音效",
         "ExpandTone|展开音色",
         "ExpandVolume|展开音量",
         "TogglePitch|切换音调",
         "ToggleSound|切换音效",
         "ToggleTone|切换音色",
         "ToggleVolume|切换音量",
         "Duration|持续时间",
         "EndSize|结束尺寸",
         "FadeStart|淡出起点",
         "HeightOffset|高度偏移",
         "ImageOpacity|贴图不透明度",
         "MaxCircles|最大圈数",
         "OnlyJump|仅跳跃",
         "OnlyOnGround|仅地面",
         "StartSize|起始尺寸",
         "Amount|数量",
         "Swap|切换",
         "ArmorHeight|护甲高度",
         "ArmorScale|护甲缩放",
         "Enchants|附魔显示",
         "FriendColor|好友颜色",
         "God|神标记",
         "MinScale|最小缩放",
         "Scaled|随距离缩放",
         "TotemPops|图腾消耗",
         "Turtle|抗性标记",
         "PositionAdjust|位置校正",
         "Always|始终",
         "ForceSky|强制天空",
         "Nether|下界",
         "Overworld|主世界",
         "ThickFog|浓雾",
         "ExpandSize|扩散尺寸",
         "Multi|多重",
         "WeatherColor|天气颜色",
         "Blocks|方块列表",
         "StandHead|站立封头",
         "BPT|每刻放置数",
         "CalcDelay|计算间隔",
         "GroundOnly|仅地面上",
         "PauseOnFlying|滑翔时暂停",
         "PauseInFluid|液体中暂停",
         "DisableOnYDiff|Y变化自动关闭",
         "DisableOnJump|跳跃自动关闭",
         "DisableOnDeath|死亡自动关闭",
         "Protect|保护",
         "Broke|破坏预放",
         "Spawn|生成预放",
         "SnapRotate|瞬间转头", "UsingBreak|使用中破坏", "AssumeBreak|假设已破坏",
         "SwapAction|切换方式",
         "Prediction|预测挖掘",
         "PacketRepair|包级补塞",
         "CPS|每秒包数",
         "First|首次塞入",
         "Repeat|重复塞入",
         "Burst|首轮爆发",
         "Pre|提前量",
         "Retry|补发次数",
         "Body|贴身扩展",
};

   private ChineseTranslator() {
   }

   /** 新版移植模块里新增的设置名与枚举值（旧表没收录的） */
   static {
      SETTINGS.put("ActionLimit", "交互上限");
      SETTINGS.put("ArmorMode", "护甲模式");
      SETTINGS.put("AntiSelfTrap", "防自困");
      SETTINGS.put("MineProgress", "触发挖掘进度");
      SETTINGS.put("PlaceAttempts", "放置次数");
      SETTINGS.put("AutoPredict", "自动预测");
      SETTINGS.put("BreakFactor", "破坏系数");
      SETTINGS.put("Character", "角色");
      SETTINGS.put("Dynamic", "动态跟随");
      SETTINGS.put("DynamicInterval", "决策间隔");
      SETTINGS.put("SyncBackground", "背景跟随主题");
      SETTINGS.put("FacePlace", "贴脸放置");
      SETTINGS.put("FacePlaceArmor", "贴脸护甲值");
      SETTINGS.put("FacePlaceHealth", "贴脸生命值");
      SETTINGS.put("FontMode", "字体模式");
      SETTINGS.put("LimitActions", "限制交互");
      SETTINGS.put("MultiPoint", "多点");
      SETTINGS.put("PushPause", "推挤暂停");
      SETTINGS.put("RightAlign", "右对齐");
      SETTINGS.put("RotationLimit", "转向上限");
      SETTINGS.put("ShowZero", "显示零值");
      SETTINGS.put("SilentMode", "静默模式");
      SETTINGS.put("SurroundDelay", "围脚延迟");
      SETTINGS.put("SurroundFucker", "围脚强放");
      SETTINGS.put("UpdateMode", "更新模式");
      SETTINGS.put("UpdateModeControl", "更新模式控制");

      ENUMS.put("LeftTop", "左上");
      ENUMS.put("RightTop", "右上");
      ENUMS.put("LeftBottom", "左下");
      ENUMS.put("RightBottom", "右下");
      ENUMS.put("LEFT", "左");
      ENUMS.put("RIGHT", "右");
      ENUMS.put("Left", "左");
      ENUMS.put("Right", "右");
      ENUMS.put("MainHand", "主手");
      ENUMS.put("OnlyArmor", "仅护甲");
      ENUMS.put("Block", "方块");
      ENUMS.put("Item", "物品");
      ENUMS.put("Stone", "石头");
      ENUMS.put("Wood", "木头");
      ENUMS.put("Torch", "火把");
      ENUMS.put("Lever", "拉杆");
      ENUMS.put("Tripwire", "绊线");
      ENUMS.put("Amethyst", "紫水晶");
      ENUMS.put("Durability", "耐久");
      ENUMS.put("Tick", "刻");
      ENUMS.put("Toast", "提示");
      ENUMS.put("Yawstep", "分步转向");
      ENUMS.put("Fixed", "固定");
      ENUMS.put("Flat", "平坦");
      ENUMS.put("Fancy", "花哨");
      ENUMS.put("Force", "强制");
      ENUMS.put("Double", "双击");
      ENUMS.put("Plain", "朴素");
      ENUMS.put("PLAIN", "朴素");
      ENUMS.put("Open", "打开");
      ENUMS.put("Take", "拿取");
      ENUMS.put("Kill", "击杀");
      ENUMS.put("Pickup", "拾取");
      ENUMS.put("Tame", "驯服");
      ENUMS.put("Summon", "召唤");
      ENUMS.put("LevelUp", "升级");
      ENUMS.put("Resonate", "共振");
      ENUMS.put("Click", "点击");
      ENUMS.put("Xp", "经验");
      ENUMS.put("INFO", "信息");
      ENUMS.put("ENABLE", "启用");
      ENUMS.put("DISABLE", "禁用");
      ENUMS.put("IDLE", "空闲");
      ENUMS.put("FALLING", "下落");
      ENUMS.put("RELEASING", "松开");
      ENUMS.put("SNEAKING", "潜行");
      ENUMS.put("Bit", "8位");
      ENUMS.put("Bass", "贝斯");
      ENUMS.put("Snare", "军鼓");
      ENUMS.put("Bell", "铃铛");
      ENUMS.put("Chime", "风铃");
      ENUMS.put("Note", "音符");
      ENUMS.put("Pling", "叮咚");
      ENUMS.put("End", "末地");
      ENUMS.put("Module", "模块");
      ENUMS.put("Config", "配置");
      ENUMS.put("Hud", "HUD");
      ENUMS.put("Page", "分页");
      ENUMS.put("Setting", "设置");
      ENUMS.put("AiAssistant", "AI助手");
      ENUMS.put("CuiCan", "摧残");
   }

   public static String resetHint(boolean page) {
      if (!cn()) {
         return page ? "Reset Page" : "Reset Default";
      } else {
         return page ? "重置页面" : "重置默认";
      }
   }

   public static String bindPrompt() {
      return cn() ? "按下按键..." : "Press keyCodec Key...";
   }

   public static String toggleHold(boolean holdEnable) {
      return cn() ? (holdEnable ? "§7切换/§f按住" : "§f切换§7/按住") : (holdEnable ? "§7Toggle/§fHold" : "§fToggle§7/Hold");
   }

   public static String copyLabel() {
      return cn() ? "复制" : "copy";
   }

   public static String pasteLabel() {
      return cn() ? "粘贴" : "paste";
   }

   public static String rainbowLabel() {
      return cn() ? "彩色" : "rainbow";
   }

   public static String copiedMsg() {
      return cn() ? "已复制颜色到剪贴板。" : "Copied the color to your clipboard.";
   }

   public static String badColorMsg() {
      return cn() ? "§4颜色格式错误！请使用 Hex (#FFFFFFFF)" : "§4Bad color format! Use Hex (#FFFFFFFF)";
   }

   public static boolean cn() {
      return ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue();
   }

   public static String setting(String name) {
      if (!cn()) {
         return name;
      } else {
         String v = (String)SETTINGS.get(name);
         return v == null ? name : v;
      }
   }

   public static String enumValue(String name) {
      if (!cn()) {
         return name;
      } else {
         String v = (String)ENUMS.get(name);
         return v == null ? name : v;
      }
   }

   public static String category(String name) {
      if (!cn()) {
         return name;
      } else {
         String v = (String)CATEGORY.get(name);
         return v == null ? name : v;
      }
   }

   static {
      CATEGORY.put("Combat", "战斗");
      CATEGORY.put("Misc", "杂项");
      CATEGORY.put("Render", "渲染");
      CATEGORY.put("Movement", "移动");
      CATEGORY.put("Player", "玩家");
      CATEGORY.put("Exploit", "漏洞");
      CATEGORY.put("Client", "客户端");
      CATEGORY.put("HUD", "界面");

      for(String e : RAW) {
         int i = e.indexOf(124);
         if (i > 0) {
            SETTINGS.put(e.substring(0, i), e.substring(i + 1));
         }
      }

      for(String e : RAW_EXTRA) {
         int i = e.indexOf(124);
         if (i > 0) {
            SETTINGS.put(e.substring(0, i), e.substring(i + 1));
         }
      }

      ENUMS.put("None", "无");
      ENUMS.put("Normal", "普通");
      ENUMS.put("Default", "默认");
      ENUMS.put("Picture", "贴图");
      ENUMS.put("LightPillar", "光柱");
      ENUMS.put("Packet", "发包");
      ENUMS.put("Vanilla", "原版");
      ENUMS.put("Flood", "灌包");
      ENUMS.put("Legit", "合法");
      ENUMS.put("Instant", "瞬时");

      // —— 水晶光环（CrystalBot，forever 移植）新增的枚举值 ——
      ENUMS.put("Weak", "弱");
      ENUMS.put("Strong", "强");
      ENUMS.put("Both", "两者");
      ENUMS.put("Sequential", "顺序");
      ENUMS.put("Single", "单点");
      ENUMS.put("Alternative", "互换");
      ENUMS.put("Pick", "选格");
      ENUMS.put("X2", "2点");
      ENUMS.put("X4", "4点");
      ENUMS.put("X8", "8点");
      ENUMS.put("ExtraHitbox", "扩展判定");
      ENUMS.put("Point", "点位");
      ENUMS.put("PlaceAngle", "放置角度");
      ENUMS.put("Base", "基座");
      ENUMS.put("RayTrace", "射线");
      ENUMS.put("Misc", "杂项");
      ENUMS.put("Normal", "普通");
      ENUMS.put("None", "关闭");
      ENUMS.put("Off", "关闭");

      // —— 上面这 70 个设置名里有几个旧表没收录，补一下 ——
      SETTINGS.put("SilentMode", "静默模式");
      SETTINGS.put("FacePlace", "贴脸放置");
      SETTINGS.put("FacePlaceHealth", "贴脸血量");
      SETTINGS.put("FacePlaceArmor", "贴脸护甲");
      SETTINGS.put("BreakFactor", "攻击包数");
      SETTINGS.put("MultiPoint", "多点射线");
      SETTINGS.put("AutoPredict", "自动预测");
      SETTINGS.put("PushPause", "推人暂停");
      SETTINGS.put("Selection", "选择模式");
      SETTINGS.put("MineRange", "挖掘距离");
      SETTINGS.put("SkipFriend", "跳过好友");
      SETTINGS.put("PredictTicks", "预测刻数");
      ENUMS.put("Silent", "静默");
      ENUMS.put("Client", "客户端");
      ENUMS.put("Server", "服务器");
      ENUMS.put("Both", "双向");
      ENUMS.put("On", "开");
      ENUMS.put("Off", "关");
      ENUMS.put("Auto", "自动");
      ENUMS.put("Manual", "手动");
      ENUMS.put("Smart", "智能");
      ENUMS.put("Strict", "严格");
      ENUMS.put("Simple", "简单");
      ENUMS.put("Advanced", "高级");
      ENUMS.put("Static", "静态");
      ENUMS.put("Dynamic", "动态");
      ENUMS.put("Bypass", "绕过");
      ENUMS.put("Offhand", "副手");
      ENUMS.put("Mainhand", "主手");
      ENUMS.put("Sword", "剑");
      ENUMS.put("Gapple", "金苹果");
      ENUMS.put("Totem", "图腾");
      ENUMS.put("Crystal", "水晶");
      ENUMS.put("Instant", "瞬时");
      ENUMS.put("Delay", "延迟");
      ENUMS.put("Legit_Toggle", "合法切换");
      ENUMS.put("Matrix", "矩阵");
      ENUMS.put("Grim", "Grim");
      ENUMS.put("Vulcan", "Vulcan");
      ENUMS.put("NCP", "NCP");
      ENUMS.put("AAC", "AAC");
      ENUMS.put("Fast", "快速");
      ENUMS.put("Slow", "缓慢");
      ENUMS.put("Up", "上");
      ENUMS.put("Down", "下");
      ENUMS.put("Left", "左");
      ENUMS.put("Right", "右");
      ENUMS.put("Both", "双向");
      ENUMS.put("X", "X轴");
      ENUMS.put("Y", "Y轴");
      ENUMS.put("Z", "Z轴");
      ENUMS.put("None", "无");
      ENUMS.put("All", "全部");
      ENUMS.put("LowestDistance", "最近距离");
      ENUMS.put("HighestDistance", "最远距离");
      ENUMS.put("LowestHealth", "最低血量");
      ENUMS.put("HighestHealth", "最高血量");
      ENUMS.put("ClosestAngle", "最近视角");
      ENUMS.put("Own", "自己");
      ENUMS.put("Enemy", "敌人");
      ENUMS.put("Target", "目标");
      ENUMS.put("Always", "总是");
      ENUMS.put("Body", "贴身");
      ENUMS.put("Sequential", "顺序");
      ENUMS.put("OnActivate", "开启时");
      ENUMS.put("Mining", "挖掘中");
      ENUMS.put("Hotbar", "快捷栏");
      ENUMS.put("Screen", "背包取用");
      ENUMS.put("Never", "从不");
      ENUMS.put("Mode", "模式");
      ENUMS.put("Ring", "环形");
      ENUMS.put("Classic", "经典");
      ENUMS.put("Panel", "面板");
      ENUMS.put("Dropdown", "下拉");
      ENUMS.put("Static", "静态");
      ENUMS.put("RainbowDelay", "彩色延迟");
      ENUMS.put("SimpleRainbow", "简单彩色");
      ENUMS.put("Spectrum", "光谱");
      ENUMS.put("Pulse", "脉冲");
      ENUMS.put("Dark", "暗色");
      ENUMS.put("Transparent", "透明");
      ENUMS.put("Custom", "自定义");
      ENUMS.put("PlusMinus", "加减号");
      ENUMS.put("Chevron", "箭头");
      ENUMS.put("Gear", "齿轮");
      ENUMS.put("Snowflake", "雪花");
      ENUMS.put("Circle", "圆形");
      ENUMS.put("Box", "盒式");
      ENUMS.put("Tent", "帐篷");
      ENUMS.put("Gaussian", "高斯");
      ENUMS.put("Kawase", "川濑");
      ENUMS.put("Radial", "径向");
      ENUMS.put("Game", "游戏");
      ENUMS.put("Gui", "界面");
      ENUMS.put("Misc", "杂项");
      ENUMS.put("Notification", "通知");
      ENUMS.put("ChatHud", "聊天栏");
      ENUMS.put("Mio", "Mio");
      ENUMS.put("Debug", "调试");
      ENUMS.put("Lowercase", "小写");
      ENUMS.put("Future", "Future");
      ENUMS.put("Earth", "Earth");
      ENUMS.put("Moon", "Moon");
      ENUMS.put("Melon", "Melon");
      ENUMS.put("Chinese", "中文");
      ENUMS.put("iOS", "iOS");
      ENUMS.put("Classic", "经典");
      ENUMS.put("AAC2", "AAC");
      ENUMS.put("AimBot", "自瞄");
      ENUMS.put("Air", "空中");
      ENUMS.put("Anchor", "锚点");
      ENUMS.put("Armor", "护甲");
      ENUMS.put("Assist", "辅助");
      ENUMS.put("ATTACK", "攻击");
      ENUMS.put("AutoBow", "自动弓");
      ENUMS.put("AutoSex", "自动贴贴");
      ENUMS.put("Axe", "斧");
      ENUMS.put("Back", "后");
      ENUMS.put("BBTT", "弹回目标");
      ENUMS.put("Bedrock", "基岩");
      ENUMS.put("Blast", "爆炸");
      ENUMS.put("Blink", "闪烁");
      ENUMS.put("BLUE", "蓝");
      ENUMS.put("Boat", "船");
      ENUMS.put("Bot", "机器人");
      ENUMS.put("BOTH_SHOULDERS", "双肩");
      ENUMS.put("Bounce", "弹跳");
      ENUMS.put("Cancel", "取消");
      ENUMS.put("cat", "跳舞猫");
      ENUMS.put("Center", "居中");
      ENUMS.put("Check", "检查");
      ENUMS.put("Chest", "箱子");
      ENUMS.put("Chorus", "紫颂果");
      ENUMS.put("Chunk", "区块");
      ENUMS.put("ClickSlot", "点击槽位");
      ENUMS.put("Clip", "卡墙");
      ENUMS.put("Color", "颜色");
      ENUMS.put("Compatibility", "兼容");
      ENUMS.put("Concrete", "混凝土");
      ENUMS.put("Console", "控制台");
      ENUMS.put("Control", "控制");
      ENUMS.put("Crafting", "合成台");
      ENUMS.put("Crawling", "爬行");
      ENUMS.put("DISTANCE", "距离");
      ENUMS.put("Drop", "丢弃");
      ENUMS.put("Element", "元素");
      ENUMS.put("EnderChest", "末影箱");
      ENUMS.put("EXPIRED", "已过期");
      ENUMS.put("Factor", "系数");
      ENUMS.put("Fade", "淡出");
      ENUMS.put("FaweWorldEdit", "快速编辑");
      ENUMS.put("Fill", "填充");
      ENUMS.put("Flow", "流动");
      ENUMS.put("Fly", "飞行");
      ENUMS.put("Forward", "前进");
      ENUMS.put("Freeze", "冻结");
      ENUMS.put("Full", "完整");
      ENUMS.put("General", "通用");
      ENUMS.put("Glide", "滑翔");
      ENUMS.put("Glitch", "故障");
      ENUMS.put("GodMode", "无敌");
      ENUMS.put("Gradient", "渐变");
      ENUMS.put("GREEN", "绿");
      ENUMS.put("GREY", "灰");
      ENUMS.put("GrimPacket", "Grim发包");
      ENUMS.put("Ground", "地面");
      ENUMS.put("Grow", "生长");
      ENUMS.put("HEALTH", "生命");
      ENUMS.put("HitboxDesync", "碰撞箱失同步");
      ENUMS.put("Ignore", "忽略");
      ENUMS.put("IgnoreBlockExploit", "忽略方块漏洞");
      ENUMS.put("Increment", "递增");
      ENUMS.put("Interact", "交互");
      ENUMS.put("INTERACT", "交互");
      ENUMS.put("INTERACT_AT", "交互坐标");
      ENUMS.put("InteractBlock", "交互方块");
      ENUMS.put("InteractItem", "交互物品");
      ENUMS.put("InteractNoCom", "无命令交互");
      ENUMS.put("InteractOOB", "越界交互");
      ENUMS.put("Invalid", "无效");
      ENUMS.put("InvalidClickSlot", "无效点击槽位");
      ENUMS.put("Inventory", "背包");
      ENUMS.put("Jello", "果冻");
      ENUMS.put("Jump", "跳跃");
      ENUMS.put("kiss_cum", "亲亲");
      ENUMS.put("kou", "搞怪");
      ENUMS.put("Lectern", "讲台");
      ENUMS.put("LEFT_SHOULDER", "左肩");
      ENUMS.put("Legacy", "旧版");
      ENUMS.put("Line", "线条");
      ENUMS.put("Mace", "重锤");
      ENUMS.put("Mining", "挖掘");
      ENUMS.put("MovementSpam", "移动刷包");
      ENUMS.put("Multi", "多重");
      ENUMS.put("MultiverseCore", "多世界");
      ENUMS.put("NCP2", "NCP");
      ENUMS.put("New", "新");
      ENUMS.put("NONE", "无");
      ENUMS.put("NoPacket", "无发包");
      ENUMS.put("NullPosition", "空位置");
      ENUMS.put("Obsidian", "黑曜石");
      ENUMS.put("OffhandSwap", "副手切换");
      ENUMS.put("OldNCP", "旧版NCP");
      ENUMS.put("OneEight", "1.8");
      ENUMS.put("Oscillation", "振荡");
      ENUMS.put("OutOfBounds", "越界");
      ENUMS.put("Path", "路径");
      ENUMS.put("Pearl", "珍珠");
      ENUMS.put("Pick", "拾取");
      ENUMS.put("Pitch", "俯仰");
      ENUMS.put("Place", "放置");
      ENUMS.put("Plain", "朴素");
      ENUMS.put("Position", "位置");
      ENUMS.put("Post", "后");
      ENUMS.put("Pre", "前");
      ENUMS.put("Predict", "预测");
      ENUMS.put("PressKey", "按键");
      ENUMS.put("Progressive", "渐进");
      ENUMS.put("Promote", "提升");
      ENUMS.put("Protection", "保护");
      ENUMS.put("QuickMove", "快速移动");
      ENUMS.put("Rage", "暴怒");
      ENUMS.put("Rainbow", "彩色");
      ENUMS.put("Random", "随机");
      ENUMS.put("RED_BLUE", "红蓝");
      ENUMS.put("Release", "释放");
      ENUMS.put("RIGHT_SHOULDER", "右肩");
      ENUMS.put("Rotate", "旋转");
      ENUMS.put("Rotation", "转头");
      ENUMS.put("SCANNED", "已扫描");
      ENUMS.put("Selector", "选择器");
      ENUMS.put("Semi", "半自动");
      ENUMS.put("Setback", "回位");
      ENUMS.put("Shader", "着色器");
      ENUMS.put("shazi", "傻子");
      ENUMS.put("Shield", "盾牌");
      ENUMS.put("Shrink", "收缩");
      ENUMS.put("Single", "单个");
      ENUMS.put("Smoke", "烟雾");
      ENUMS.put("Snow", "雪花");
      ENUMS.put("Solid", "实心");
      ENUMS.put("Song", "歌曲");
      ENUMS.put("Spawner", "刷怪笼");
      ENUMS.put("Spin", "旋转");
      ENUMS.put("Stack", "堆叠");
      ENUMS.put("Steal", "偷取");
      ENUMS.put("Strafe", "平移");
      ENUMS.put("StrafeStrict", "严格平移");
      ENUMS.put("Strong", "强力");
      ENUMS.put("SUCCESS", "成功");
      ENUMS.put("Swap", "交换");
      ENUMS.put("Switch", "切换");
      ENUMS.put("SwitchSlot", "切换槽位");
      ENUMS.put("Sync", "同步");
      ENUMS.put("Teleport", "传送");
      ENUMS.put("ThreeD", "3D");
      ENUMS.put("Timer", "计时");
      ENUMS.put("ToVoid", "传送虚空");
      ENUMS.put("ToVoid2", "传送虚空2");
      ENUMS.put("TrollHack", "整蛊");
      ENUMS.put("TwoD", "2D");
      ENUMS.put("UpdatedNCP", "新版NCP");
      ENUMS.put("Vehicle", "载具");
      ENUMS.put("Velocity", "速度");
      ENUMS.put("WAITING_SCAN", "等待扫描");
      ENUMS.put("Wall", "墙");
      ENUMS.put("Web", "蛛网");
      ENUMS.put("Whitelist", "白名单");
      ENUMS.put("Window", "窗口");
      ENUMS.put("WorldEdit", "世界编辑");
      ENUMS.put("YELLOW_BLUE", "黄蓝");

      ENUMS.put("BackInOut", "回弹进出");
      ENUMS.put("BackOut", "回弹出场");
      ENUMS.put("CircIn", "圆形入场");
      ENUMS.put("CircInOut", "圆形进出");
      ENUMS.put("CircOut", "圆形出场");
      ENUMS.put("CubicIn", "三次入场");
      ENUMS.put("CubicInOut", "三次进出");
      ENUMS.put("CubicOut", "三次出场");
      ENUMS.put("Expo", "指数");
      ENUMS.put("Linear", "线性");
      ENUMS.put("QuadIn", "二次入场");
      ENUMS.put("QuadInOut", "二次进出");
      ENUMS.put("QuadOut", "二次出场");
      ENUMS.put("QuartIn", "四次入场");
      ENUMS.put("QuartInOut", "四次进出");
      ENUMS.put("QuartOut", "四次出场");
      ENUMS.put("QuintIn", "五次入场");
      ENUMS.put("QuintInOut", "五次进出");
      ENUMS.put("QuintOut", "五次出场");
      ENUMS.put("SineInOut", "正弦进出");
      ENUMS.put("SineOut", "正弦出场");
      ENUMS.put("AutoTrade", "自动交易");
      ENUMS.put("Base", "底座");
      ENUMS.put("Blacklist", "黑名单");
      ENUMS.put("BlinkBuffer", "闪现缓冲");
      ENUMS.put("BlockExploitMode", "方块漏洞");
      ENUMS.put("Boost", "加速");
      ENUMS.put("Break", "破坏");
      ENUMS.put("Calc", "预判");
      ENUMS.put("Combat", "战斗");
      ENUMS.put("DoublePacket", "双包");
      ENUMS.put("Essentials", "基础");
      ENUMS.put("Filter", "过滤");
      ENUMS.put("GrimHop", "Grim跳跃");
      ENUMS.put("Hybrid", "混合");
      ENUMS.put("Hypixel2K22", "Hypixel2K22");
      ENUMS.put("ItemFrameDupe", "展示框复制");
      ENUMS.put("Mine", "挖掘");
      ENUMS.put("MotionSpoof", "位移伪造");
      ENUMS.put("NPlusOneDupe", "N+1复制");
      ENUMS.put("No", "否");
      ENUMS.put("Ominous", "不祥");
      ENUMS.put("Phobos", "Phobos式");
      ENUMS.put("Prediction", "预测");
      ENUMS.put("Rain", "雨");
      ENUMS.put("RayTrace", "射线检测");
      ENUMS.put("Render", "渲染");
      ENUMS.put("Rise", "Rise");
      ENUMS.put("SandMiner", "挖沙");
      ENUMS.put("ScoreFarmer", "刷分");
      ENUMS.put("SlabPlacer", "半砖放置");
      ENUMS.put("Smash", "猛击");
      ENUMS.put("SwapVanilla", "原版切换");
      ENUMS.put("TP", "传送");
      ENUMS.put("TheVoid", "虚空");
      ENUMS.put("ThunderHack", "ThunderHack");
      ENUMS.put("Translate", "平移");
      ENUMS.put("TridentDupe", "三叉戟复制");
      ENUMS.put("Turtle", "龟速");
      ENUMS.put("TurtlePath", "龟速路径");
      ENUMS.put("VerticalSplit", "垂直分割");
      ENUMS.put("VoidMace", "虚空锤");
      ENUMS.put("WindJump", "风跳");
      ENUMS.put("All", "全部");
      ENUMS.put("LowestDistance", "最近距离");
      ENUMS.put("HighestDistance", "最远距离");
      ENUMS.put("LowestHealth", "最低血量");
      ENUMS.put("HighestHealth", "最高血量");
      ENUMS.put("ClosestAngle", "最近准星");
      ENUMS.put("XinDupe", "Xin复制");
   }
}
