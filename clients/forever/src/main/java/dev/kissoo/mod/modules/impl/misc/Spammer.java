package dev.kissoo.mod.modules.impl.misc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.client.MusicPlayer;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import dev.kissoo.mod.music.LrcLine;
import net.minecraft.client.network.PlayerListEntry;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("unused")
public class Spammer extends Module {

   private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

   public final EnumSetting<Type> type = this.add(new EnumSetting<>("Type", Type.Custom));

   public final StringSetting message = this.add(
           new StringSetting("Message", "不笑得是gay.骗你的.笑得也是",
                   () -> this.type.getValue() == Type.Custom)
   );

   // 歌词模式：要搜索的歌名
   public final StringSetting lyricSongName = this.add(
           new StringSetting("SongName", "Never Gonna Give You Up",
                   () -> this.type.getValue() == Type.Lyric)
   );

   public final EnumSetting<SendMode> sendMode = this.add(new EnumSetting<>("SendMode", SendMode.Public));

   public final StringSetting targetName = this.add(
           new StringSetting("Target", "PlayerName",
                   () -> this.sendMode.getValue() == SendMode.Single)
   );

   public final List<String> catgirl = List.of(
           "人家在00等你来操我哦～下面已经湿得黏糊糊的了♡ 快来00干烂人家吧～",
           "喵呜～%s……人家下面好痒……来00插人家嘛～猫猫的骚穴已经流水了～",
           "来00干我吧～人家没穿内裤，就穿了一条短裙在00等你哦♡",
           "人家好想要大肉棒……00见～把猫猫操到腿软好不好～",
           "呜喵～人家在00发情了……下面又湿又黏……快来00操我到喷水吧♡",
           "00等你～人家尾巴都翘起来了……快来后面干我～",
           "人家下面黏到不行了……来00把猫猫的骚穴灌满吧～求你了♡",
           "嘻嘻～%s……人家在00张开腿等你呢～快来操人家到失禁～",
           "来00玩我吧～猫猫现在又骚又空虚……需要你的大肉棒填满～",
           "人家在00发情到发抖了……快来干我～要把人家操坏也可以哦～",
           "尾巴摇得好厉害……%s快来00，人家下面已经湿透黏成一片了～",
           "人家想被你按在00狠狠地操……来吧～把猫猫操到哭出来～",
           "00见～人家下面又痒又黏……快用粗粗的肉棒帮人家止痒嘛～",
           "喵哈♡～人家在00准备好被轮了……谁先来都可以～快来干我～",
           "人家好骚……在00等你来操我到子宫都灌满精液～",
           "来00操烂人家的骚穴吧～猫猫已经忍不住要喷了～",
           "人家下面湿得能滴水了……%s快来00，把我操到走不动路～",
           "在00等大肉棒～人家现在只想被狠狠地干到高潮连连♡",
           "呜～人家好痒……来00把猫猫的黏糊糊骚穴干到翻白眼吧～",
           "人家在00穿的女仆装哦～来干我吧……可以很粗暴也没关系～",
           "人家在00已经被操到失禁了……尿和淫水喷了一地～快来继续干我吧♡",
           "喵呜～%s……人家要喷了！来00看人家潮吹给你看～喷得好厉害哦～",
           "下面完全控制不住了……%s快来00，人家一被操就会失禁喷水～",
           "人家在00喷了好多次了……地上全是猫猫的淫水和尿～还要更多吗～",
           "啊啊～要失禁了……来00把人家操到一边喷水一边尿出来吧♡",
           "人家现在只要被插就会潮吹……00等你来看我喷水高潮～",
           "呜喵♡～人家在00失禁喷了好大一滩……快来操我继续喷～",
           "下面好敏感……%s一来00人家就会喷水失禁给你看～",
           "人家已经喷到腿软了……00还有好多淫水没干～快来操我吧～",
           "来00干我～人家想被你操到失禁喷水，直到走不动路为止～",
           "人家在00被操到一边哭一边喷……淫水和尿混在一起好黏哦～",
           "喵哈～%s……人家下面要坏掉了……一直喷水停不下来～快来00～",
           "人家高潮的时候会失禁喷好远的～要不要来00接人家喷出的淫水呀～",
           "在00等你……人家已经喷了一地了……快来把我操到继续潮吹吧♡",
           "呜～要尿出来了……%s快来00，人家被操到失禁了～好丢人哦～",
           "人家现在一想到00被操就会喷……下面已经湿得一塌糊涂了～",
           "来00操我到失禁吧～猫猫想把淫水全部喷在你身上～",
           "人家在00已经被操喷好几次了……还是想要更多……快来干烂我～",
           "喷水停不下来了……%s……人家在00已经变成喷水母猫了～快来～",
           "喵呜♡～人家失禁了……尿着尿着就高潮喷水了……来00看我～",
           "人家下面又黏又敏感……被插一下就会潮吹失禁～00等你来玩～",
           "在00张开腿等你……人家已经准备好被操到喷水到天亮了～",
           "啊啊啊～要喷了要喷了！%s快来00接人家潮吹的淫水吧～",
           "人家在00被操到小腹鼓起来……全是喷出来的淫水和尿～",
           "失禁了好多次了……下面已经又湿又黏又痒……%s快来00继续操我～",
           "人家想被你按在00操到失禁喷水……把猫猫操成只会喷水的肉便器～",
           "呜喵～人家现在只要被干就会尿出来……来00体验一下吧♡",
           "喷了……又喷了……%s……人家在00已经喷得到处都是了～还要吗？",
           "人家在00高潮失禁的样子……你不想来看看吗～好淫荡的哦～",
           "下面已经喷到麻掉了……来00再把我操到潮吹吧～求求你了～",
           "人家发情到失禁了……00全是我的淫水……快来把我操得更脏吧♡",
           "喵哈～%s……人家潮吹喷得好远……来00看看我有多骚～",
           "人家已经分不清是尿还是淫水了……全都被操喷出来了～来00继续～",
           "在00等你把我操到失禁喷水……猫猫现在只想被你干到崩溃～",
           "人家下面彻底坏掉了……一直喷水失禁……快来00收拾我吧～"
   );

   public final List<String> provoke = List.of(
           "就你这垃圾也配玩MC？来00我教你怎么做人，废物",
           "穷逼废物连00都不敢来？哈哈哈笑死爹了，你妈在下面等你呢",
           "杂鱼出生还敢bb？来00我把你打到删号，垃圾东西",
           "你这种底边也配说话？来00挨打吧，我一只手都能锤爆你",
           "你妈死了还在玩MC？来00我送你全家团聚，孝子",
           "连00都不敢来的废物也配活着？快来00让我踩死你",
           "你这开挂的儿子敢来00吗？不敢来就是你妈的逼养的",
           "穷逼玩意儿一天天在这叫，敢来00我把你打到亲妈都不认识",
           "就你这智障水平也敢玩？来00我让你知道什么叫差距，杂种",
           "垃圾中的垃圾，来00让我吐口痰踩死你吧，恶心东西",
           "你全家都是废物，来00我一个打你们十个，敢吗？",
           "连00都不敢来的怂货，你爸当年射你的时候肯定后悔了",
           "你这种人活着就是浪费空气，来00我帮你妈清理垃圾",
           "弱智吧玩意儿还在这叫唤？来00我打得你妈都不认识你",
           "穷逼连根棒棒糖都买不起还玩MC？来00挨打吧废物",
           "你妈在下面喊你下去吃奶，来00我送你下去，孝子",
           "就你这脑瘫也敢跟我说话？来00我把你头打爆",
           "垃圾杂鱼连00的勇气都没有，笑死我了，废物点心",
           "你这种人出生就该被扔进厕所，来00我帮你完成使命",
           "敢来00吗？不敢来的都是你妈逼里爬出来的孬种",
           "我一只手就能把你这种垃圾锤成妈都不认识，来00试试？",
           "天天在这叫的废物，有种来00让我看看你有多硬",
           "你爸要是知道你这么废肯定会后悔把你射出来，来00吧",
           "连00都不敢来的东西也配叫自己是玩家？笑死",
           "垃圾中的战斗机，来00我让你知道什么叫被支配的恐惧",
           "你全家加起来都不够我打的，来00挨打吧，废物",
           "穷逼废物还在这呼吸空气？来00我帮你妈把你回收了",
           "就你这水平还敢玩MC？来00我打得你怀疑人生",
           "杂鱼连正眼看我的资格都没有，来00跪下叫爸爸",
           "你妈的逼估计都后悔把你生出来了，来00我送你下去道歉"
   );

   // 顺序模式的文本列表（可自行增删修改）
   public final List<String> sequentialList = List.of(
           "人之初",
           "性本善",
           "性相近",
           "习相远",
           "苟不教",
           "性乃迁",
           "教之道",
           "贵以专",
           "昔孟母",
           "择邻处",
           "子不学",
           "断机杼",
           "窦燕山",
           "有义方",
           "教五子",
           "名俱扬",
           "养不教",
           "父之过",
           "教不严",
           "师之惰",
           "子不学",
           "非所宜",
           "幼不学",
           "老何为",
           "玉不琢",
           "不成器",
           "人不学",
           "不知义",
           "为人子",
           "方少时",
           "亲师友",
           "习礼仪",
           "香九龄",
           "能温席",
           "孝于亲",
           "所当执",
           "融四岁",
           "能让梨",
           "悌于长",
           "宜先知",
           "首孝悌",
           "次见闻",
           "知某数",
           "识某文",
           "一而十",
           "十而百",
           "百而千",
           "千而万",
           "三才者",
           "天地人",
           "三光者",
           "日月星",
           "三纲者",
           "君臣义",
           "父子亲",
           "夫妇顺",
           "曰春夏",
           "曰秋冬",
           "此四时",
           "运不穷",
           "曰南北",
           "曰西东",
           "此四方",
           "应乎中",
           "曰水火",
           "木金土",
           "此五行",
           "本乎数",
           "十干者",
           "甲至癸",
           "十二支",
           "子至亥",
           "曰黄道",
           "日所躔",
           "曰赤道",
           "当中权",
           "赤道下",
           "温暖极",
           "我中华",
           "在东北",
           "寒燠均",
           "霜露改",
           "右高原",
           "左大海",
           "曰江河",
           "曰淮济",
           "此四渎",
           "水之纪",
           "曰岱华",
           "嵩恒衡",
           "此五岳",
           "山之名",
           "古九州",
           "今改制",
           "称行省",
           "三十五",
           "曰士农",
           "曰工商",
           "此四民",
           "国之良",
           "曰仁义",
           "礼智信",
           "此五常",
           "不容紊",
           "地所生",
           "有草木",
           "此植物",
           "遍水陆",
           "有虫鱼",
           "有鸟兽",
           "此动物",
           "能飞走",
           "稻粱菽",
           "麦黍稷",
           "此六谷",
           "人所食",
           "马牛羊",
           "鸡犬豕",
           "此六畜",
           "人所饲",
           "曰喜怒",
           "曰哀惧",
           "爱恶欲",
           "七情俱",
           "青赤黄",
           "及黑白",
           "此五色",
           "目所识",
           "酸苦甘",
           "及辛咸",
           "此五味",
           "口所含",
           "膻焦香",
           "及腥朽",
           "此五臭",
           "鼻所嗅",
           "匏土革",
           "木石金",
           "丝与竹",
           "乃八音",
           "曰平上",
           "曰去入",
           "此四声",
           "宜调协",
           "高曾祖",
           "父而身",
           "身而子",
           "子而孙",
           "自子孙",
           "至玄曾",
           "乃九族",
           "人之伦",
           "父子恩",
           "夫妇从",
           "兄则友",
           "弟则恭",
           "长幼序",
           "友与朋",
           "君则敬",
           "臣则忠",
           "此十义",
           "人所同",
           "当师叙",
           "勿违背",
           "斩齐衰",
           "大小功",
           "至缌麻",
           "五服终",
           "礼乐射",
           "御书数",
           "古六艺",
           "今不具",
           "惟书学",
           "人共遵",
           "既识字",
           "讲说文",
           "有古文",
           "大小篆",
           "隶草继",
           "不可乱",
           "若广学",
           "惧其繁",
           "但略说",
           "能知原",
           "凡训蒙",
           "须讲究",
           "详训诂",
           "明句读",
           "为学者",
           "必有初",
           "小学终",
           "至四书",
           "论语者",
           "二十篇",
           "群弟子",
           "记善言",
           "孟子者",
           "七篇止",
           "讲道德",
           "说仁义",
           "作中庸",
           "乃孔伋",
           "中不偏",
           "庸不易",
           "作大学",
           "乃曾子",
           "自修齐",
           "至平治",
           "孝经通",
           "四书熟",
           "如六经",
           "始可读",
           "诗书易",
           "礼春秋",
           "号六经",
           "当讲求",
           "有连山",
           "有归藏",
           "有周易",
           "三易详",
           "有典谟",
           "有训诰",
           "有誓命",
           "书之奥",
           "我周公",
           "作周礼",
           "著六官",
           "存治体",
           "大小戴",
           "注礼记",
           "述圣言",
           "礼乐备",
           "曰国风",
           "曰雅颂",
           "号四诗",
           "当讽咏",
           "诗既亡",
           "春秋作",
           "寓褒贬",
           "别善恶",
           "三传者",
           "有公羊",
           "有左氏",
           "有谷梁",
           "经既明",
           "方读子",
           "撮其要",
           "记其事",
           "五子者",
           "有荀扬",
           "文中子",
           "及老庄",
           "经子通",
           "读诸史",
           "考世系",
           "知终始",
           "自羲农",
           "至黄帝",
           "号三皇",
           "居上世",
           "唐有虞",
           "号二帝",
           "相揖逊",
           "称盛世",
           "夏有禹",
           "商有汤",
           "周文武",
           "称三王",
           "夏传子",
           "家天下",
           "四百载",
           "迁夏社",
           "汤伐夏",
           "国号商",
           "六百载",
           "至纣亡",
           "周武王",
           "始诛纣",
           "八百载",
           "最长久",
           "周辙东",
           "王纲坠",
           "逞干戈",
           "尚游说",
           "始春秋",
           "终战国",
           "五霸强",
           "七雄出",
           "嬴秦氏",
           "始兼并",
           "传二世",
           "楚汉争",
           "高祖兴",
           "汉业建",
           "至孝平",
           "王莽篡",
           "光武兴",
           "为东汉",
           "四百年",
           "终于献",
           "魏蜀吴",
           "争汉鼎",
           "号三国",
           "迄两晋",
           "宋齐继",
           "梁陈承",
           "为南朝",
           "都金陵",
           "北元魏",
           "分东西",
           "宇文周",
           "与高齐",
           "迨至隋",
           "一土宇",
           "不再传",
           "失统绪",
           "唐高祖",
           "起义师",
           "除隋乱",
           "创国基",
           "二十传",
           "三百载",
           "梁灭之",
           "国乃改",
           "梁唐晋",
           "及汉周",
           "称五代",
           "皆有由",
           "炎宋兴",
           "受周禅",
           "十八传",
           "南北混",
           "辽与金",
           "皆称帝",
           "元灭金",
           "绝宋世",
           "舆图广",
           "超前代",
           "九十年",
           "国祚废",
           "太祖兴",
           "国大明",
           "号洪武",
           "都金陵",
           "迨成祖",
           "迁燕京",
           "十六世",
           "至崇祯",
           "权阉肆",
           "寇如林",
           "李闯出",
           "神器焚",
           "清世祖",
           "膺景命",
           "靖四方",
           "克大定",
           "由康雍",
           "历乾嘉",
           "民安富",
           "治绩夸",
           "道咸间",
           "变乱起",
           "始英法",
           "扰都鄙",
           "同光后",
           "宣统弱",
           "传九帝",
           "满清殁",
           "革命兴",
           "废帝制",
           "立宪法",
           "建民国",
           "古今史",
           "全在兹",
           "载治乱",
           "知兴衰",
           "史虽繁",
           "读有次",
           "史记一",
           "汉书二",
           "后汉三",
           "国志四",
           "兼证经",
           "参通鉴",
           "读史者",
           "考实录",
           "通古今",
           "若亲目",
           "口而诵",
           "心而惟",
           "朝于斯",
           "夕于斯",
           "昔仲尼",
           "师项橐",
           "古圣贤",
           "尚勤学",
           "赵中令",
           "读鲁论",
           "彼既仕",
           "学且勤",
           "披蒲编",
           "削竹简",
           "彼无书",
           "且知勉",
           "头悬梁",
           "锥刺股",
           "彼不教",
           "自勤苦",
           "如囊萤",
           "如映雪",
           "家虽贫",
           "学不辍",
           "如负薪",
           "如挂角",
           "身虽劳",
           "犹苦卓",
           "苏老泉",
           "二十七",
           "始发奋",
           "读书籍",
           "彼既老",
           "犹悔迟",
           "尔小生",
           "宜早思",
           "若梁灏",
           "八十二",
           "对大廷",
           "魁多士",
           "彼既成",
           "众称异",
           "尔小生",
           "宜立志",
           "莹八岁",
           "能咏诗",
           "泌七岁",
           "能赋棋",
           "彼颖悟",
           "人称奇",
           "尔幼学",
           "当效之",
           "蔡文姬",
           "能辨琴",
           "谢道韫",
           "能咏吟",
           "彼女子",
           "且聪敏",
           "尔男子",
           "当自警",
           "唐刘晏",
           "方七岁",
           "举神童",
           "作正字",
           "彼虽幼",
           "身已仕",
           "有为者",
           "亦若是",
           "犬守夜",
           "鸡司晨",
           "苟不学",
           "曷为人",
           "蚕吐丝",
           "蜂酿蜜",
           "人不学",
           "不如物",
           "幼而学",
           "壮而行",
           "上致君",
           "下泽民",
           "扬名声",
           "显父母",
           "光于前",
           "裕于后",
           "人遗子",
           "金满赢",
           "我教子",
           "唯一经",
           "勤有功",
           "戏无益",
           "戒之哉",
           "宜勉力"
   );

   private final Random random = new Random();
   private final SliderSetting randoms = this.add(new SliderSetting("Random", 4.0, 0.0, 20.0, 1.0));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 4.0, 0.5, 30.0, 0.1).setSuffix("s"));

   private final BooleanSetting checkSelf = this.add(new BooleanSetting("CheckSelf", false));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));

   private final Timer timer = new Timer();
   private final Queue<String> friendQueue = new ConcurrentLinkedQueue<>();

   // 运行状态变量
   private int sequentialIndex = 0;
   private int lyricIndex = 0;
   private final List<String> loadedLyrics = new CopyOnWriteArrayList<>();
   private boolean isFetchingLyrics = false;
   private String lastSearchedSong = "";

   public Spammer() {
      super("Spammer", Module.Category.Misc);
      this.setChinese("自动刷屏");
   }

   @Override
   public void onEnable() {
      this.sequentialIndex = 0;
      this.lyricIndex = 0;
   }

   @Override
   public void onLogout() {
      if (this.autoDisable.getValue()) {
         this.disable();
      }
   }

   @Override
   public void onDisable() {
      this.friendQueue.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      // 防止空指针报错
      if (mc.player == null || mc.getNetworkHandler() == null) return;

      // 如果开启了歌词模式，检测是否需要网络请求歌词
      if (this.type.getValue() == Type.Lyric) {
         String targetSong = this.lyricSongName.getValue().trim();
         if (!targetSong.isEmpty() && !targetSong.equals(lastSearchedSong)) {
            lastSearchedSong = targetSong;
            fetchLyricsAsync(targetSong);
         }
         if (isFetchingLyrics || loadedLyrics.isEmpty()) {
            return;
         }
      }

      if (!this.timer.passedS(this.delay.getValue())) return;
      this.timer.reset();

      String randomString = this.generateRandomString(this.randoms.getValueInt());
      if (!randomString.isEmpty()) randomString = " " + randomString;

      String finalMessage = "";

      // 根据模式处理文本
      switch (this.type.getValue()) {
         case Custom -> finalMessage = this.message.getValue() + randomString;
         case CatGirl -> finalMessage = this.catgirl.get(this.random.nextInt(this.catgirl.size())) + randomString;
         case Provoke -> finalMessage = this.provoke.get(this.random.nextInt(this.provoke.size())) + randomString;

         case Sequential -> {
            if (this.sequentialList.isEmpty()) return;
            finalMessage = this.sequentialList.get(this.sequentialIndex) + randomString;
            this.sequentialIndex++;
            if (this.sequentialIndex >= this.sequentialList.size()) {
               this.sequentialIndex = 0; // 发完一轮从头开始
            }
         }

         case Lyric -> {
            if (this.loadedLyrics.isEmpty()) return;
            finalMessage = this.loadedLyrics.get(this.lyricIndex) + randomString;
            this.lyricIndex++;
            if (this.lyricIndex >= this.loadedLyrics.size()) {
               this.lyricIndex = 0; // 发完一轮从头开始
            }
         }
      }

      // 发送模式
      switch (this.sendMode.getValue()) {
         case Random -> sendRandomTell(finalMessage);
         case Single -> sendSingleTell(finalMessage);
         case Friend -> sendFriendTell(finalMessage);
         case Public -> {
            if (finalMessage.startsWith("/")) {
               mc.getNetworkHandler().sendCommand(finalMessage.replaceFirst("/", ""));
            } else {
               mc.getNetworkHandler().sendChatMessage(finalMessage);
            }
         }
      }
   }

   // --- 新增：独立网络线程请求歌词，不依赖未知的 searchSongs ---
   private void fetchLyricsAsync(String songName) {
      this.isFetchingLyrics = true;
      this.loadedLyrics.clear();
      this.lyricIndex = 0;

      new Thread(() -> {
         try {
            // 1. 调用网易云基础搜索接口获取歌曲ID
            String encodedName = URLEncoder.encode(songName, StandardCharsets.UTF_8);
            String searchUrl = "http://music.163.com/api/search/get/web?s=" + encodedName + "&type=1&limit=1";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(searchUrl)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            if (jsonResponse.has("result") && jsonResponse.getAsJsonObject("result").has("songs")) {
               // 获取第一首歌的 ID
               long songId = jsonResponse.getAsJsonObject("result")
                       .getAsJsonArray("songs").get(0).getAsJsonObject()
                       .get("id").getAsLong();

               // 2. 使用你现有的 API 类和解析类去抓取歌词
               String lrcText = MusicPlayer.SERVICE.api().lyric(songId);
               List<LrcLine> parsedLines = dev.kissoo.mod.music.LrcParser.parse(lrcText);

               for (LrcLine line : parsedLines) {
                  String text = line.text().trim();
                  if (!text.isEmpty()) {
                     this.loadedLyrics.add(text);
                  }
               }
            }

            if (this.loadedLyrics.isEmpty()) {
               this.loadedLyrics.add("[系统] 未找到纯文本歌词或伴奏无词: " + songName);
            }
         } catch (Exception e) {
            this.loadedLyrics.add("[系统] 获取歌词网络异常: " + e.getMessage());
         } finally {
            this.isFetchingLyrics = false;
         }
      }, "AlienV4-LyricFetcher").start();
   }

   // --- 下方为你原本写好，但我上一轮不小心漏掉的方法 ---

   private void sendSingleTell(String content) {
      String target = this.targetName.getValue();
      if (target == null || target.trim().isEmpty() || mc.getNetworkHandler() == null) return;
      mc.getNetworkHandler().sendChatCommand("tell " + target.trim() + " " + content);
   }

   private void sendRandomTell(String content) {
      if (mc.getNetworkHandler() == null) return;
      Collection<PlayerListEntry> players = mc.getNetworkHandler().getPlayerList();
      List<PlayerListEntry> list = new ArrayList<>(players);
      if (list.isEmpty()) return;

      PlayerListEntry target = list.get(this.random.nextInt(list.size()));
      int attempts = 0;
      while (checkSelf.getValue() && mc.player != null &&
              Objects.equals(target.getProfile().getName(), mc.player.getGameProfile().getName()) &&
              attempts < 50) {
         target = list.get(this.random.nextInt(list.size()));
         attempts++;
      }
      mc.getNetworkHandler().sendChatCommand("tell " + target.getProfile().getName() + " " + content);
   }

   private void sendFriendTell(String content) {
      if (mc.getNetworkHandler() == null || mc.player == null) return;

      if (this.friendQueue.isEmpty()) {
         for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            String playerName = entry.getProfile().getName();
            if (!playerName.equals(mc.player.getName().getString()) && isFriend(playerName)) {
               this.friendQueue.add(playerName);
            }
         }
      }

      if (this.friendQueue.isEmpty()) return;

      String targetFriend = this.friendQueue.poll();
      if (targetFriend != null) {
         mc.getNetworkHandler().sendChatCommand("tell " + targetFriend + " " + content);
      }
   }

   private boolean isFriend(String name) {
      try {
         return Forever.FRIEND.isFriend(name);
      } catch (Exception e) {
         return false;
      }
   }

   private String generateRandomString(int length) {
      if (length <= 0) return "";
      StringBuilder sb = new StringBuilder(length);
      for (int i = 0; i < length; i++) {
         sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
      }
      return sb.toString();
   }

   // --- 枚举 ---
   public enum Type {
      Custom,
      CatGirl,
      Provoke,
      Sequential,
      Lyric
   }

   public enum SendMode {
      Public,
      Random,
      Single,
      Friend
   }
}