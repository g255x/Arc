package dev.g255.arc.api.utils.i18n;

import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LangUtil {
   private static final Map<String, String> FULL = new LinkedHashMap();
   private static final Map<String, String> TOKENS = new HashMap();
   private static final Map<String, String> MERGE = new HashMap();
   private static final Map<String, String> CATEGORY = new HashMap();
   private static final Pattern CAMEL = Pattern.compile("(?<=[a-z0-9])[A-Z]|(?<=[A-Z])[A-Z](?=[a-z])");

   private static void t(String string, String string2) {
      TOKENS.put(string, string2);
   }

   private LangUtil() {
   }

   public static boolean isChinese() {
      if (Boolean.getBoolean("arc.forcechinese")) {
         return true;
      } else {
         try {
            ClientSetting clientSetting = ClientSetting.INSTANCE;
            if (clientSetting == null) {
               return false;
            } else {
               return clientSetting.chinese != null && clientSetting.chinese.getValue();
            }
         } catch (Throwable var1) {
            return false;
         }
      }
   }

   public static String ui(String string) {
      if (string != null && !string.isEmpty() && isChinese()) {
         if (containsCjk(string)) {
            return string;
         } else {
            String string2 = (String)FULL.get(string);
            if (string2 != null) {
               return string2;
            } else if (string.indexOf(32) < 0) {
               String string3 = translateSegment(string);
               return string3 != null ? string3 : string;
            } else {
               StringBuilder stringBuilder = new StringBuilder(string.length() + 8);
               boolean bl = false;

               for(String string4 : string.split(" ")) {
                  if (stringBuilder.length() > 0) {
                     stringBuilder.append(' ');
                  }

                  String string5;
                  if ((string5 = translateSegment(string4)) != null) {
                     stringBuilder.append(string5);
                     bl = true;
                  } else {
                     stringBuilder.append(string4);
                  }
               }

               return bl ? stringBuilder.toString() : string;
            }
         }
      } else {
         return string;
      }
   }

   private static String translateSegment(String string) {
      if (string != null && !string.isEmpty() && !containsCjk(string)) {
         String string2 = (String)FULL.get(string);
         if (string2 != null) {
            return string2;
         } else {
            String string3 = "";
            String string4 = string;
            if (string.length() > 1 && string.charAt(0) == 167) {
               string3 = string.substring(0, 2);
               string4 = string.substring(2);
            }

            if (!string4.isEmpty() && isNameLike(string4)) {
               String string5 = translateTokens(string4);
               return string5 != null ? string3 + string5 : null;
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static boolean isNameLike(String string) {
      char c = string.charAt(0);
      return Character.isUpperCase(c);
   }

   private static String translateTokens(String string) {
      String[] stringArray = splitTokens(string);
      if (stringArray.length == 0) {
         return null;
      } else {
         StringBuilder stringBuilder = new StringBuilder();
         int n = 0;
         boolean bl = false;

         while(n < stringArray.length) {
            String string2;
            if (n + 1 < stringArray.length && (string2 = (String)MERGE.get((stringArray[n] + stringArray[n + 1]).toLowerCase())) != null) {
               stringBuilder.append(string2);
               bl = true;
               n += 2;
            } else {
               string2 = (String)TOKENS.get(stringArray[n].toLowerCase());
               if (string2 != null) {
                  stringBuilder.append(string2);
                  bl = true;
               } else {
                  stringBuilder.append(stringArray[n]);
               }

               ++n;
            }
         }

         return bl ? stringBuilder.toString() : null;
      }
   }

   public static String setting(String string) {
      if (string != null && !string.isEmpty()) {
         if (isChinese() && !containsCjk(string)) {
            String string2 = (String)FULL.get(string);
            if (string2 != null) {
               return string2;
            } else {
               String string3 = translateTokens(string);
               return string3 != null ? string3 : string;
            }
         } else {
            return string;
         }
      } else {
         return string;
      }
   }

   public static String enumValue(Enum<?> enum_) {
      if (enum_ == null) {
         return null;
      } else {
         return enum_ instanceof Module.Category ? category((Module.Category)enum_) : setting(enum_.name());
      }
   }

   public static String category(Module.Category category) {
      if (category == null) {
         return null;
      } else if (!isChinese()) {
         return category.name();
      } else {
         String string = (String)CATEGORY.get(category.name());
         return string != null ? string : category.name();
      }
   }

   public static boolean containsCjk(String string) {
      for(char c : string.toCharArray()) {
         if (c >= 19968 && c <= '鿿') {
            return true;
         }
      }

      return false;
   }

   static String[] splitTokens(String string) {
      ArrayList<String> arrayList = new ArrayList();

      for(String string2 : string.split("[\\s_]+")) {
         if (!string2.isEmpty()) {
            Matcher matcher = CAMEL.matcher(string2);

            int n;
            for(n = 0; matcher.find(); n = matcher.start()) {
               if (matcher.start() > n) {
                  arrayList.add(string2.substring(n, matcher.start()));
               }
            }

            if (n < string2.length()) {
               arrayList.add(string2.substring(n));
            }
         }
      }

      ArrayList<String> arrayList2 = new ArrayList();

      for(String string3 : arrayList) {
         if (!arrayList2.isEmpty() && string3.length() == 1 && Character.isUpperCase(string3.charAt(0))) {
            int var10001 = arrayList2.size() - 1;
            String var10002 = (String)arrayList2.get(arrayList2.size() - 1);
            arrayList2.set(var10001, var10002 + string3);
         } else {
            arrayList2.add(string3);
         }
      }

      return (String[])arrayList2.toArray(new String[0]);
   }

   static {
      FULL.put("CoolDown", "冷却时间");
      FULL.put("Cooldown", "冷却时间");
      FULL.put("InWater", "水中");
      FULL.put("InBlock", "方块内");
      FULL.put("AirStop", "空停");
      FULL.put("LagTime", "延迟时间");
      FULL.put("WallRange", "隔墙范围");
      FULL.put("OnGround", "着地");
      FULL.put("InAir", "空中");
      FULL.put("HurtTime", "受伤时间");
      FULL.put("OnlyHold", "仅按住");
      FULL.put("WhileUsing", "使用时");
      FULL.put("Reset Default", "重置默认");
      FULL.put("Reset Page", "重置页面");
      FULL.put("Bind", "按键绑定");
      FULL.put("Search", "搜索");
      FULL.put("Hidden", "隐藏");
      FULL.put("Parent", "子项开关");
      FULL.put("Description", "描述");
      FULL.put("Combat", "战斗");
      FULL.put("Misc", "杂项");
      FULL.put("Render", "渲染");
      FULL.put("Movement", "移动");
      FULL.put("Player", "玩家");
      FULL.put("Exploit", "利用");
      FULL.put("Client", "客户端");
      FULL.put("Normal", "普通");
      FULL.put("Vanilla", "原版");
      FULL.put("Smart", "智能");
      FULL.put("Safe", "安全");
      FULL.put("Unsafe", "危险");
      FULL.put("None", "无");
      FULL.put("Always", "总是");
      FULL.put("Never", "从不");
      FULL.put("Single", "单人");
      FULL.put("Multi", "多人");
      FULL.put("Spectrum", "光谱");
      FULL.put("Silent", "静默");
      FULL.put("Instant", "瞬间");
      FULL.put("Legit", "合法");
      FULL.put("Packet", "数据包");
      FULL.put("Rotate", "旋转");
      FULL.put("Strict", "严格");
      MERGE.put("inwater", "水中");
      MERGE.put("inblock", "方块内");
      MERGE.put("airstop", "空停");
      MERGE.put("lagtime", "延迟时间");
      MERGE.put("cooldown", "冷却时间");
      MERGE.put("onground", "着地");
      MERGE.put("inair", "空中");
      MERGE.put("wallrange", "隔墙范围");
      MERGE.put("onlyhold", "仅按住");
      MERGE.put("whileusing", "使用时");
      FULL.put("OffsetX", "左右位置");
      FULL.put("AnchorY", "缩放锚点");
      MERGE.put("leftshoulder", "左肩");
      MERGE.put("rightshoulder", "右肩");
      MERGE.put("bothshoulders", "双肩");
      MERGE.put("redblue", "红蓝");
      MERGE.put("yellowblue", "黄蓝");
      t("shoulder", "肩膀");
      t("variant", "样式");
      t("blue", "蓝");
      t("green", "绿");
      t("grey", "灰");
      FULL.put("NoCollide", "无碰撞");
      FULL.put("NoGhostHand", "无幽灵手");
      FULL.put("Obsidian", "黑曜石");
      FULL.put("Crystal", "水晶");
      FULL.put("UsingPause", "使用暂停");
      FULL.put("OnlyMain", "仅主手");
      FULL.put("AutoSwitch", "自动切换");
      FULL.put("TryBreakTime", "重试时间");
      FULL.put("FarCancel", "超距取消");
      FULL.put("SwingHand", "挥手");
      FULL.put("InstantMine", "瞬挖");
      FULL.put("InstantDelay", "瞬挖延迟");
      FULL.put("FastBypass", "快速绕过");
      FULL.put("DoubleBreak", "双重破坏");
      FULL.put("CheckGround", "检测着地");
      FULL.put("BypassGround", "绕过着地");
      FULL.put("SwitchDamage", "切换伤害");
      FULL.put("SwitchTime", "切换时间");
      FULL.put("MineDelay", "挖掘延迟");
      FULL.put("PacketDelay", "发包延迟");
      FULL.put("RetryAfterStop", "停止后重试");
      FULL.put("Damage", "伤害");
      FULL.put("GlassDelay", "玻璃延迟");
      FULL.put("GlassDelayTime", "玻璃延迟时间");
      FULL.put("RenderProgress", "渲染进度");
      FULL.put("RenderBox", "渲染框");
      FULL.put("RenderMode", "渲染模式");
      FULL.put("Fading", "渐隐");
      FULL.put("RenderTime", "渲染时间");
      FULL.put("FadeTime", "渐隐时长");
      FULL.put("FadeSide", "渐隐面");
      FULL.put("FadeLine", "渐隐线");
      FULL.put("SideStart", "面起始");
      FULL.put("SideEnd", "面结束");
      FULL.put("LineStart", "线起始");
      FULL.put("LineEnd", "线结束");
      FULL.put("General", "通用");
      FULL.put("Box", "方框");
      FULL.put("Shrink", "收缩");
      FULL.put("Grow", "放大");
      MERGE.put("nocollide", "无碰撞");
      MERGE.put("noghosthand", "无幽灵手");
      MERGE.put("usingpause", "使用暂停");
      MERGE.put("onlymain", "仅主手");
      MERGE.put("autoswitch", "自动切换");
      MERGE.put("trybreaktime", "重试时间");
      MERGE.put("farcancel", "超距取消");
      MERGE.put("swinghand", "挥手");
      MERGE.put("instantmine", "瞬挖");
      MERGE.put("instantdelay", "瞬挖延迟");
      MERGE.put("fastbypass", "快速绕过");
      MERGE.put("doublebreak", "双重破坏");
      MERGE.put("checkground", "检测着地");
      MERGE.put("bypassground", "绕过着地");
      MERGE.put("switchdamage", "切换伤害");
      MERGE.put("switchtime", "切换时间");
      MERGE.put("minedelay", "挖掘延迟");
      MERGE.put("packetdelay", "发包延迟");
      MERGE.put("retryafterstop", "停止后重试");
      MERGE.put("glassdelay", "玻璃延迟");
      MERGE.put("glassdelaytime", "玻璃延迟时间");
      MERGE.put("renderprogress", "渲染进度");
      MERGE.put("renderbox", "渲染框");
      MERGE.put("rendermode", "渲染模式");
      MERGE.put("rendertime", "渲染时间");
      MERGE.put("fadetime", "渐隐时长");
      MERGE.put("fadeside", "渐隐面");
      MERGE.put("fadeline", "渐隐线");
      MERGE.put("sidestart", "面起始");
      MERGE.put("sideend", "面结束");
      MERGE.put("linestart", "线起始");
      MERGE.put("lineend", "线结束");
      t("collide", "碰撞");
      t("ghost", "幽灵");
      t("crystal", "水晶");
      t("damage", "伤害");
      t("box", "框");
      t("retry", "重试");
      t("bypass", "绕过");
      t("swing", "挥动");
      t("glass", "玻璃");
      t("done", "完成");
      FULL.put("PosX", "X坐标");
      FULL.put("PosY", "Y坐标");
      FULL.put("KeyWidth", "按键宽度");
      FULL.put("KeyHeight", "按键高度");
      FULL.put("Gap", "间距");
      FULL.put("Radius", "圆角");
      FULL.put("Glow", "发光强度");
      FULL.put("IdleColor", "空闲颜色");
      FULL.put("IdleStroke", "空闲边框");
      FULL.put("TextColor", "文字颜色");
      MERGE.put("posx", "X坐标");
      MERGE.put("posy", "Y坐标");
      MERGE.put("keywidth", "按键宽度");
      MERGE.put("keyheight", "按键高度");
      MERGE.put("idlecolor", "空闲颜色");
      MERGE.put("idlestroke", "空闲边框");
      MERGE.put("textcolor", "文字颜色");
      FULL.put("UseItem", "使用物品");
      FULL.put("BlockSlow", "方块减速");
      FULL.put("BlockSpecial", "方块特殊");
      FULL.put("BlockIn", "方块内");
      FULL.put("FakeSneak", "伪造潜行");
      FULL.put("UseItemBypass", "使用物品绕过");
      FULL.put("FakeSneakMode", "伪造潜行模式");
      FULL.put("BlockInGrim", "方块内Grim");
      FULL.put("BlockInKeepY", "方块内保持Y");
      MERGE.put("useitem", "使用物品");
      MERGE.put("blockslow", "方块减速");
      MERGE.put("blockspecial", "方块特殊");
      MERGE.put("blockin", "方块内");
      MERGE.put("fakesneak", "伪造潜行");
      MERGE.put("useitembypass", "使用物品绕过");
      MERGE.put("fakesneakmode", "伪造潜行模式");
      MERGE.put("blockingrim", "方块内Grim");
      MERGE.put("blockinkeepy", "方块内保持Y");
      t("sneak", "潜行");
      t("nobypass", "不绕过");
      t("grimlazy", "Grim懒人");
      t("grimtick", "Grim刻");
      t("badpacket", "坏包");
      t("interact", "交互");
      t("grimfallflying", "Grim滑翔");
      FULL.put("MinY", "最低挖掘层");
      FULL.put("Reach", "挖掘范围");
      FULL.put("RotSpeed", "旋转速度");
      FULL.put("AutoTool", "自动工具");
      FULL.put("Rotate", "自动转向");
      FULL.put("AvoidContainers", "保护容器");
      FULL.put("SkipBuried", "只挖表面");
      MERGE.put("miny", "最低挖掘层");
      MERGE.put("rotspeed", "旋转速度");
      MERGE.put("autotool", "自动工具");
      MERGE.put("avoidcontainers", "保护容器");
      MERGE.put("skipburied", "只挖表面");
      FULL.put("MaxDamage", "最大伤害");
      FULL.put("DisableOnHolePush", "禁止坑洞推出");
      FULL.put("MovementSync", "移动同步");
      FULL.put("ItemId", "物品ID");
      FULL.put("ItemName", "物品名称");
      FULL.put("FirstPerson", "第一人称");
      FULL.put("ThirdPerson", "第三人称");
      FULL.put("SuppressEquip", "抑制装备");
      FULL.put("UseAnimation", "使用动画");
      FULL.put("ItemTransform", "物品变换");
      FULL.put("SwingDuration", "挥动时长");
      FULL.put("GrimSafe", "Grim安全");
      FULL.put("AllowSprint", "允许冲刺");
      FULL.put("ViaUseFallback", "使用回退");
      FULL.put("Model", "模型");
      FULL.put("Sounds", "音效");
      FULL.put("HitSounds", "命中音效");
      FULL.put("UseSounds", "使用音效");
      FULL.put("SwingTicks", "挥动刻");
      FULL.put("ChargeDelay", "蓄力延迟");
      FULL.put("ForwardMove", "前进移动");
      MERGE.put("itemid", "物品ID");
      MERGE.put("itemname", "物品名称");
      MERGE.put("firstperson", "第一人称");
      MERGE.put("thirdperson", "第三人称");
      MERGE.put("suppressequip", "抑制装备");
      MERGE.put("useanimation", "使用动画");
      MERGE.put("itemtransform", "物品变换");
      MERGE.put("swingduration", "挥动时长");
      MERGE.put("grimsafe", "Grim安全");
      MERGE.put("allowsprint", "允许冲刺");
      MERGE.put("viausefallback", "使用回退");
      MERGE.put("model", "模型");
      MERGE.put("sounds", "音效");
      MERGE.put("hitsounds", "命中音效");
      MERGE.put("usesounds", "使用音效");
      MERGE.put("swingticks", "挥动刻");
      MERGE.put("chargedelay", "蓄力延迟");
      MERGE.put("forwardmove", "前进移动");
      t("spear", "长矛");
      t("spears", "长矛");
      t("lunge", "突刺");
      t("equip", "装备");
      t("suppress", "抑制");
      t("fallback", "回退");
      t("grim", "Grim");
      t("stab", "刺击");
      FULL.put("Control", "控制");
      FULL.put("Bounce", "弹跳");
      FULL.put("Freeze", "冻结");
      FULL.put("Rotation", "旋转");
      FULL.put("Pitch", "俯仰");
      FULL.put("InfiniteDura", "无限耐久");
      FULL.put("NoAnimation", "无动画");
      FULL.put("VisualSpoof", "视觉伪装");
      FULL.put("SetFlag", "设置标记");
      FULL.put("FireWorkBind", "烟花键绑定");
      FULL.put("PacketInteract", "数据包交互");
      FULL.put("InventorySwap", "背包交换");
      FULL.put("OnlyOne", "仅一个");
      FULL.put("AutoJump", "自动跳跃");
      FULL.put("UpPitch", "上升俯仰");
      FULL.put("UpFactor", "上升系数");
      FULL.put("FallSpeed", "下落速度");
      FULL.put("SpeedLimit", "速度上限");
      FULL.put("MaxSpeed", "最大速度");
      FULL.put("NoDrag", "无阻力");
      FULL.put("AutoStop", "自动停止");
      FULL.put("AutoFly", "自动飞行");
      FULL.put("AutoStart", "自动起飞");
      FULL.put("CheckSpeed", "检测速度");
      FULL.put("MinSpeed", "最小速度");
      FULL.put("DownSpeed", "下降速度");
      FULL.put("MotionStop", "动作停止");
      FULL.put("InfiniteMaxSpeed", "无限最大速度");
      FULL.put("InfiniteMinSpeed", "无限最小速度");
      FULL.put("InfiniteMaxHeight", "无限最大高度");
      FULL.put("ReleaseSneak", "释放潜行");
      MERGE.put("control", "控制");
      MERGE.put("bounce", "弹跳");
      MERGE.put("freeze", "冻结");
      MERGE.put("rotation", "旋转");
      MERGE.put("pitch", "俯仰");
      MERGE.put("infinitedura", "无限耐久");
      MERGE.put("noanimation", "无动画");
      MERGE.put("visualspoof", "视觉伪装");
      MERGE.put("setflag", "设置标记");
      MERGE.put("fireworkbind", "烟花键绑定");
      MERGE.put("packetinteract", "数据包交互");
      MERGE.put("inventoryswap", "背包交换");
      MERGE.put("onlyone", "仅一个");
      MERGE.put("autojump", "自动跳跃");
      MERGE.put("uppitch", "上升俯仰");
      MERGE.put("upfactor", "上升系数");
      MERGE.put("fallspeed", "下落速度");
      MERGE.put("speedlimit", "速度上限");
      MERGE.put("maxspeed", "最大速度");
      MERGE.put("nodrag", "无阻力");
      MERGE.put("autostop", "自动停止");
      MERGE.put("autofly", "自动飞行");
      MERGE.put("autostart", "自动起飞");
      MERGE.put("checkspeed", "检测速度");
      MERGE.put("minspeed", "最小速度");
      MERGE.put("downspeed", "下降速度");
      MERGE.put("motionstop", "动作停止");
      MERGE.put("infinitemaxspeed", "无限最大速度");
      MERGE.put("infiniteminspeed", "无限最小速度");
      MERGE.put("infinitemaxheight", "无限最大高度");
      MERGE.put("releasesneak", "释放潜行");
      t("drag", "阻力");
      FULL.put("CollarColor", "项圈颜色");
      MERGE.put("collarcolor", "项圈颜色");
      t("collar", "项圈");
      t("page", "页面");
      t("delay", "延迟");
      t("range", "范围");
      t("min", "最小");
      t("max", "最大");
      t("speed", "速度");
      t("strength", "强度");
      t("radius", "半径");
      t("size", "大小");
      t("width", "宽度");
      t("height", "高度");
      t("scale", "缩放");
      t("offset", "偏移");
      t("color", "颜色");
      t("mode", "模式");
      t("enable", "启用");
      t("disable", "禁用");
      t("auto", "自动");
      t("render", "渲染");
      t("rendering", "渲染");
      t("up", "向上");
      t("down", "向下");
      t("downward", "向下");
      t("in", "在");
      t("air", "空气");
      t("stop", "停止");
      t("time", "时间");
      t("jump", "跳跃");
      t("silent", "静默");
      t("close", "关闭");
      t("open", "打开");
      t("swap", "交换");
      t("switch", "切换");
      t("list", "列表");
      t("module", "模块");
      t("icon", "图标");
      t("glow", "光晕");
      t("rainbow", "彩色");
      t("pulse", "脉冲");
      t("saturation", "饱和度");
      t("factor", "系数");
      t("h", "水平");
      t("v", "垂直");
      t("vertical", "垂直");
      t("horizontal", "水平");
      t("explosion", "爆炸");
      t("explosions", "爆炸");
      t("boost", "增强");
      t("strafe", "侧移");
      t("block", "方块");
      t("blocks", "方块");
      t("slow", "缓慢");
      t("slowness", "缓慢");
      t("slot", "槽位");
      t("key", "按键");
      t("reset", "重置");
      t("default", "默认");
      t("background", "背景");
      t("border", "边框");
      t("grid", "网格");
      t("rotate", "旋转");
      t("shadow", "阴影");
      t("font", "字体");
      t("text", "文字");
      t("only", "仅");
      t("no", "不");
      t("on", "开");
      t("off", "关");
      t("self", "自身");
      t("main", "主");
      t("hand", "手");
      t("head", "头");
      t("feet", "脚");
      t("name", "名称");
      t("show", "显示");
      t("sound", "音效");
      t("message", "消息");
      t("bar", "进度条");
      t("place", "放置");
      t("break", "破坏");
      t("fill", "填充");
      t("move", "移动");
      t("step", "跨越");
      t("steps", "步数");
      t("fall", "掉落");
      t("ground", "地面");
      t("flying", "飞行");
      t("mine", "挖掘");
      t("mining", "挖掘");
      t("eating", "进食");
      t("check", "检测");
      t("detect", "检测");
      t("detection", "检测");
      t("ignore", "忽略");
      t("skip", "跳过");
      t("allow", "允许");
      t("keep", "保留");
      t("force", "强制");
      t("prefer", "优先");
      t("smart", "智能");
      t("custom", "自定义");
      t("random", "随机");
      t("sync", "同步");
      t("update", "更新");
      t("apply", "应用");
      t("start", "开始");
      t("end", "结束");
      t("wait", "等待");
      t("timeout", "超时");
      t("pause", "暂停");
      t("attack", "攻击");
      t("target", "目标");
      t("priority", "优先级");
      t("packet", "数据包");
      t("packets", "数据包");
      t("predict", "预测");
      t("position", "位置");
      t("pos", "位置");
      t("distance", "距离");
      t("fov", "视野");
      t("inventory", "背包");
      t("item", "物品");
      t("items", "物品");
      t("totem", "不死图腾");
      t("totems", "不死图腾");
      t("health", "血量");
      t("resistance", "抗性");
      t("outline", "描边");
      t("fade", "淡入淡出");
      t("alpha", "透明度");
      t("blur", "模糊");
      t("animation", "动画");
      t("anim", "动画");
      t("length", "时长");
      t("threshold", "阈值");
      t("weight", "权重");
      t("count", "计数");
      t("per", "每");
      t("friends", "好友");
      t("friend", "好友");
      t("players", "玩家");
      t("player", "玩家");
      t("person", "人");
      t("entity", "实体");
      t("surround", "围住");
      t("piston", "活塞");
      t("obsidian", "黑曜石");
      t("crystal", "水晶");
      t("elytra", "鞘翅");
      t("sneak", "潜行");
      t("sneaking", "潜行");
      t("web", "蜘蛛网");
      t("webs", "蜘蛛网");
      t("wall", "墙");
      t("walls", "墙");
      t("out", "外");
      t("inside", "内");
      t("face", "面向");
      t("hit", "命中");
      t("throw", "丢弃");
      t("use", "使用");
      t("using", "使用中");
      t("correct", "校正");
      t("spam", "刷屏");
      t("snow", "雪");
      t("banner", "幕布");
      t("fire", "火");
      t("box", "方框");
      t("full", "完整");
      t("line", "线条");
      t("stage", "阶段");
      t("anti", "防");
      t("back", "返回");
      t("timer", "计时器");
      t("motion", "动作");
      t("swing", "挥手");
      t("side", "侧");
      t("center", "中心");
      t("top", "顶部");
      t("first", "第一");
      t("second", "第二");
      t("after", "之后");
      t("safe", "安全");
      t("unsafe", "危险");
      t("bypass", "绕过");
      t("instant", "瞬间");
      t("fix", "修复");
      t("interact", "交互");
      t("looking", "看向");
      t("look", "视角");
      t("hud", "HUD");
      t("gui", "GUI");
      t("chat", "聊天");
      t("debug", "调试");
      t("progress", "进度");
      t("prefix", "前缀");
      t("teleport", "传送");
      t("hotbar", "快捷栏");
      t("damage", "伤害");
      t("hurt", "受伤");
      t("multiplier", "倍数");
      t("limit", "上限");
      t("infinite", "无限");
      t("cancel", "取消");
      t("overlay", "覆盖");
      t("fog", "雾");
      t("red", "红色");
      t("status", "状态");
      t("chunks", "区块");
      t("chunk", "区块");
      t("base", "基础");
      t("old", "旧");
      t("boat", "船");
      t("ghost", "幽灵");
      t("falling", "下落");
      t("crawling", "爬行");
      t("sweep", "清扫");
      t("shulker", "潜影盒");
      t("firework", "烟花");
      t("stone", "石");
      t("glowstone", "萤石");
      t("double", "双倍");
      t("whisper", "私聊");
      t("gap", "间隔");
      t("extend", "延伸");
      t("hover", "悬停");
      t("kit", "套装");
      t("enemy", "敌人");
      t("push", "推动");
      t("set", "设置");
      t("assist", "辅助");
      t("avoid", "避免");
      t("exploit", "利用");
      t("frame", "帧");
      t("lock", "锁定");
      t("hole", "洞");
      t("trash", "垃圾");
      t("potions", "药水");
      t("circle", "圆");
      t("esp", "ESP");
      t("bg", "背景");
      t("counter", "计数器");
      t("add", "添加");
      t("offhand", "副手");
      t("cast", "施放");
      t("points", "点");
      t("ice", "冰");
      t("leg", "腿");
      t("through", "穿透");
      t("log", "日志");
      t("release", "释放");
      t("xp", "经验");
      t("turtle", "海龟");
      t("hold", "持有");
      t("fast", "快速");
      t("when", "当");
      t("simulation", "模拟");
      t("active", "激活");
      t("all", "全部");
      t("mobs", "生物");
      t("indicator", "指示器");
      t("collide", "碰撞");
      t("liquid", "液体");
      t("window", "窗口");
      t("parkour", "跑酷");
      t("toggle", "切换");
      t("clear", "清空");
      t("multi", "多");
      t("low", "低");
      t("spawn", "生成");
      t("screen", "屏幕");
      t("slider", "滑条");
      t("one", "一");
      t("others", "其他");
      t("other", "其他");
      t("strict", "严格");
      t("portal", "传送门");
      t("fixed", "固定");
      t("zero", "零");
      t("charge", "充能");
      t("movement", "移动");
      t("shift", "Shift");
      t("fake", "伪装");
      t("rate", "比率");
      t("trapdoor", "陷阱门");
      t("remove", "移除");
      t("command", "命令");
      t("sprint", "冲刺");
      t("animals", "动物");
      t("bevel", "斜角");
      t("aim", "瞄准");
      t("bed", "床");
      t("balance", "平衡");
      t("spacing", "间距");
      t("pops", "爆图腾");
      t("pop", "爆");
      t("reminder", "提示");
      t("breaker", "破坏者");
      t("forward", "向前");
      t("sand", "沙子");
      t("sphere", "球");
      t("invisible", "隐形");
      t("slimes", "史莱姆");
      t("empty", "空");
      t("weather", "天气");
      t("tasks", "任务");
      t("legit", "合法");
      t("idle", "空闲");
      t("less", "更少");
      t("avatar", "头像");
      t("god", "神器");
      t("fluctuation", "波动");
      t("rect", "矩形");
      t("jitter", "抖动");
      t("space", "空间");
      t("stand", "站立");
      t("deceive", "欺骗");
      t("weapon", "武器");
      t("terrain", "地形");
      t("diagonal", "对角");
      t("apple", "苹果");
      t("respawn", "重生");
      t("send", "发送");
      t("click", "点击");
      t("suffix", "后缀");
      t("edit", "编辑");
      t("tag", "标签");
      t("tags", "标签");
      t("hard", "难");
      t("villagers", "村民");
      t("level", "等级");
      t("world", "世界");
      t("fish", "鱼");
      t("info", "信息");
      t("fps", "帧率");
      t("ping", "延迟");
      t("server", "服务器");
      t("brand", "品牌");
      t("coords", "坐标");
      t("model", "模型");
      t("watermark", "水印");
      t("title", "标题");
      t("sort", "排序");
      t("lower", "小写");
      t("armor", "护甲");
      t("sword", "剑");
      t("pickaxe", "镐");
      t("axe", "斧");
      t("bow", "弓");
      t("food", "食物");
      t("pearl", "末影珍珠");
      t("pearls", "末影珍珠");
      t("gapple", "金苹果");
      t("gapples", "金苹果");
      t("arrow", "箭");
      t("arrows", "箭");
      t("compass", "指南针");
      t("water", "水");
      t("bucket", "桶");
      t("ender", "末影");
      t("chest", "箱子");
      t("durability", "耐久");
      t("dur", "耐久");
      t("normal", "普通");
      t("vanilla", "原版");
      t("none", "无");
      t("always", "总是");
      t("never", "从不");
      t("both", "两者");
      t("duration", "持续");
      t("direction", "方向");
      t("thread", "线程");
      t("async", "异步");
      t("raycast", "射线");
      t("trace", "轨迹");
      t("test", "测试");
      t("single", "单人");
      t("moon", "月亮");
      t("earth", "地球");
      t("chinese", "中文");
      t("client", "客户端");
      t("suspend", "悬浮");
      t("blurarea", "模糊区域");
      t("outlineColor", "描边颜色");
      t("general", "通用");
      t("age", "存在时间");
      t("bind", "绑定");
      t("spectrum", "光谱");
      t("hold", "按住");
      t("b1", "B1");
   }
}
