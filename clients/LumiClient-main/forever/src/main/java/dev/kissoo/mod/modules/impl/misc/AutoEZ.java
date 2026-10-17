package dev.kissoo.mod.modules.impl.misc;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.DeathEvent;
import dev.kissoo.api.events.impl.TotemEvent;
import dev.kissoo.api.events.impl.RemoveEntityEvent;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import net.minecraft.entity.player.PlayerEntity;

public class AutoEZ extends Module {
   private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

   // ==========================================
   // =========== 击杀嘲讽 文本区域 ============
   // ==========================================
   public final List<String> sex = List.of(
           "呐呐杂鱼哥哥不会这样就被捉弄的不会说话了吧♡",
           "嘻嘻杂鱼哥哥不会以为竖个大拇哥就能欺负我了吧不会吧♡不会吧♡",
           "杂鱼哥哥怎么可能欺负得了别人呢只能欺负自己哦♡",
           "哥哥真是好欺负啊♡嘻嘻",
           "哎♡杂鱼说话就是无趣唉",
           "呐呐杂鱼哥哥发这个是想教育我吗嘻嘻怎么可能啊♡",
           "什么嘛废柴哥哥会想这种事情啊唔呃",
           "把你肮脏的目光拿开啦很恶心哦♡",
           "咱的期待就是被你这样的笨蛋破坏了~♡"
   );

   public final List<String> bot = List.of(
           "鼠标明天到，触摸板打的", "转人工", "收徒", "不收徒", "有真人吗", "墨镜上车",
           "素材局", "不接单", "接单", "征婚", "4399?", "暂时不考虑打职业", "bot?",
           "叫你家大人来打", "假肢上门安装", "浪费我的网费", "不收残疾人", "下课",
           "自己找差距", "不接代", "代+", "这样的治好了也流口水", "人机",
           "人机怎么调难度啊", "只收不被0封的", "Bot吗这是", "领养", "纳亲",
           "正视差距", "近亲繁殖?", "我玩的是新手教程?", "来调灵敏度的",
           "来调参数的", "小号", "不是本人别加", "下次记得晚点玩", "随便玩玩,不带妹", "扣1上车"
   );

   public final List<String> hoprcKills = List.of(
           "一破！卧龙出山", "二连！举世无双", "三杀！三阳开泰",
           "四杀！四方无敌", "五杀！五方俱灭", "六杀！六道轮回"
   );
   public final List<String> catgirl = List.of(           "哎呀～%s……人家下面已经湿得黏糊糊的了，都是因为你哦♡",
           "喵呜～%s的味道好浓……把人家发情期的骚穴弄得好痒呢～",
           "人家好想要%s的大肉棒……可以插进猫猫又湿又黏的穴里吗～？",
           "尾巴都湿透了啦～%s把人家弄得一直在流水，好过分哦♡",
           "嘻嘻～%s……人家现在下面黏糊糊的，好想坐在你脸上呢～",
           "人家发情了……%s要负责把猫猫黏黏的淫水全部喝掉哦♡",
           "喵哈♡～%s……人家子宫在叫了，快把热热的精液射进来嘛～",
           "人家下面已经湿成一片了……%s要来帮猫猫舔干净吗～？",
           "呜喵～%s……人家好骚啊……被你看着下面就一直收缩呢♡",
           "把%s的肉棒伸过来～人家要用又湿又热的猫穴好好吸吮你～",
           "人家现在满脑子都是%s的精液……可以射满猫猫黏糊糊的骚穴吗～？",
           "尾巴在后面摇得好厉害……%s把人家弄得好想要被欺负哦♡",
           "人家下面又黏又痒……%s要不要来闻闻猫猫发情时的骚味呀～",
           "嘻嘻～%s……人家高潮的时候会把你压在身下，用湿湿的穴把你榨干哦～",
           "人家已经湿到大腿了啦～都是%s害的……要负责把人家舔干净♡",
           "喵～%s……人家想把你绑起来，然后坐在你脸上慢慢磨～黏糊糊的哦",
           "人家发情期好严重……%s要来用粗粗的肉棒帮猫猫止痒吗～？",
           "下面一直在滴……%s看，人家因为你湿成什么样子了呢♡",
           "呜呼～%s……人家好想把你淹死在猫猫又黏又骚的淫水里呀～",
           "人家现在超敏感的……%s只要碰一下，人家就会喷出来哦♡",
           "来～%s……把脸埋进人家湿湿黏黏的尾巴根部闻一闻嘛～",
           "人家最喜欢看%s被猫猫的骚穴吸得一脸舒服又下贱的样子了～",
           "喵呜♡～%s……人家下面好空虚……快来用精液把猫猫灌满吧～",
           "人家已经忍不住了……%s要看着人家一边发情一边喷水吗～？",
           "嘻嘻～%s……猫猫的黏糊糊淫汁已经滴到你尸体上了呢～还要吗？",
           "啊……%s……人家下面好痒……痒到不行了啦～快来帮猫猫止痒嘛♡",
           "人家好黏……下面黏到不行……全是%s害的，人家要被融化掉了～",
           "喵呜～%s……人家骚穴里面黏糊糊的……好痒……要被你弄化掉了啦♡",
           "人家下面又湿又黏……%s闻闻看……已经痒得受不了了～",
           "痒死了……%s……人家发情的骚穴黏黏的，一直在收缩……好想要你～",
           "人家要被%s弄得黏成一团了……下面黏糊糊的都要拉丝了呢♡",
           "好痒……好痒啊%s……猫猫的黏穴已经忍不住了……快插进来嘛～",
           "人家下面黏到不行……%s要来把人家黏黏的淫水全部舔干净吗～？",
           "呜喵♡～%s……人家快要被自己的淫水黏化掉了……你也要一起黏上来～",
           "痒死了痒死了……%s……人家的骚穴又黏又热……快来疼爱人家嘛～",
           "人家现在全身都黏糊糊的……都是因为%s……要负责把人家舔一遍哦♡",
           "下面好黏……黏到走不动路了啦～%s……人家真的要被融化了～",
           "喵哈～%s……人家里面好痒……黏黏的淫汁一直流……你听到了吗～",
           "人家发情发到下面黏成一片了……%s要来把猫猫弄得更黏吗～？",
           "痒……好痒……%s……人家的黏穴已经空虚到不行了……快填满我～",
           "人家下面黏糊糊的……拉丝了啦～%s……要来看人家发情的样子吗♡",
           "要被融化了……%s……人家又湿又黏……已经变成只发情母猫了～",
           "人家好痒……骚穴里面黏黏的在吸……%s……快把肉棒塞进来嘛～",
           "黏到不行……真的黏到不行了%s……人家下面已经变成黏液窝了呢♡",
           "喵呜～♡ %s……人家全身都软掉了……下面又痒又黏……快来欺负人家吧～");
   public final List<String> nsfw = List.of(           "误连这种程度都受不了吗？%s果然是低等杂鱼呢",
           "嗯~难道杂鱼酱%s 脑子里只知道被小女孩辱骂吗？真是又弱又笨诶♡~嘻嘻~",
           "怎么还有喜欢被骂的杂鱼呀，啧♡~%s真是变态！色鬼！臭杂鱼！",
           "杂鱼大叔%s,看你那副可怜的样子真是太好笑了呢~♡嘻嘻~",
           "%s  这样飞舞又讨嫌的杂鱼，很恶心哦♡~嘻嘻嘻~",
           "怎么就软下去了, %s 真是杂鱼♡~",
           "呜哇～%s好逊哦♡，这种程度就满足了嘛？",
           "啧♡~%s这种无可救药的变态，被虐待反而会觉得开心吗？真是恶心死了！",
           "呐~%s大叔，这么大年纪了还被小女孩按在地上摩擦，羞不羞呀？♡",
           "杂鱼酱%s，再多挣扎一下给我看呀♡",
           "呐~这就是实力的差距哦~%s这种一辈子只能跪下添我脚的小杂鱼♡，真是太可怜呢~",
           "就算求饶也不会停手哦，毕竟像%s这样的臭杂鱼，只配被我踩在地上发抖呢♡~",
           "呐，%s，像你这种只会给别人添麻烦的垃圾，还是乖乖退游比较好吧？",
           "呵♡~%s被戳到痛处就兴奋成这副样子♡，果然是天生的抖 M 变态呢，真让人作呕呢~",
           "哎呀♡~%s该不会就是为了被狠狠践踏♡，才故意凑过来的吧？太恶心啦~",
           "误~%s 居然还在期待着什么吗？真是条摇着尾巴求虐待的杂鱼呢♡",
           "误~%s这么享受被欺负的样子，是早就期待很久了吗？真是变态到无可救药♡",
           "哼~别再装了杂鱼酱%s，你心底早就欢呼雀跃了，就喜欢这种被我肆意完弄的感觉，对吧♡",
           "误~连这种程度都受不了吗？%s果然是低等杂鱼呢~",
           "杂鱼先生%s又在偷看呢~真是恶心死了~",
           "嗯~难道杂鱼酱%s 脑子里只知道被小女孩辱骂吗？真是又弱又笨诶♡~嘻嘻~",
           "怎么还有喜欢被骂的杂鱼呀，啧♡~%s真是变态！色鬼！臭杂鱼！",
           "杂鱼大叔%s,看你那副可怜的样子真是太好笑了呢~♡嘻嘻~",
           "%s  这样飞舞又讨嫌的杂鱼，很恶心哦♡~嘻嘻嘻~",
           "怎么就软下去了, %s 真是杂鱼♡~",
           "%s就是杂鱼，永远都改变不了呢♡~嘻~",
           "呜哇～%s好逊哦♡，这种程度就满足了嘛？",
           "呐呐，%s桑，刚才是在努力挣扎吗？拼命的样子真的好滑稽哦~♡",
           "呜哇~%s这种程度也能叫反抗吗？简直像在给我抓痒一样呢，逊毙了！",
           "诶？%s这就结束了吗？杂鱼的持久力果然和技术一样差劲呢♡~嘻嘻~",
           "就算给%s一百次机会，结果也不会改变哦，毕竟杂鱼就是用来被踩在脚下的呀！",
           "这种单方面的虐待，%s难道已经开始产生快感了吗？真是无可救药的变态杂鱼呢~",
           "别过来！%s这种浑身散发着失败气息的臭杂鱼，光是名字出现在屏幕上都让人恶心！",
           "啧♡~%s这副窝囊的样子，真是看一眼都会折寿呢，快滚回泉水反省吧！",
           "居然被我这种小女孩打败了，%s该不会正躲在屏幕后面偷偷抹眼泪吧？好逊哦~♡",
           "呐，%s大叔，以后出门记得戴个头盔，不然像你这种笨蛋杂鱼很容易被路人打死的哦~",
           "这种垃圾操作...%s的大脑里塞的难道都是废料吗？真是可怜的小杂鱼♡~",
           "杂鱼酱%s，要乖乖听话哦♡~",
           "逊！毙！了！%s就是个大笨蛋！",
           "%s的骨头软绵绵的，真是好欺负呢~♡",
           "嘻嘻，%s又碎掉了，好可怜哦~",
           "%s 不要这么对姐姐，姐姐的里面都湿了",
           "%s 扫逼弟弟别再诱惑姐姐了",
           "%s 弟弟可真是厉害啊，把姐姐都塞满了",
           "%s 真的不行了姐姐要出来了，不要~",
           "%s 真的这么想要得到姐姐的扫穴吗~",
           "%s 弟弟的蟒蛇真是长呢，让姐姐都玩爽了",
           "%s 怎么了？想让姐姐好好服侍你吗",
           "%s 啊不要弟弟，姐姐都流出来了",
           "%s 啊~哥哥不要再动了，姐姐真的受不了了",
           "%s ~姐姐的. ..逼..很紧”肯定比弟弟噪过的所有女孩子都要舒服哦",
           "%s 真几把骚，冲上去就把她操趴在地上，掀起裙子直接捅进逼里，一对车灯两手抓，三个孔位用力插",
           "%s 好想拿几把...草s姐姐，姐姐都被噪的喷水了",
           "%s 姐姐的鲍鱼粉粉嫩嫩，不知道噪起来会不会喷水",
           "%s 是真的骚，好想要一套她的鞋，罩，袜三件套，两只袜子套在手上，罩扣在头上，再用蟒蛇把鞋顶起来转圈圈，想想都满足~",
           "好想多收集%s 的体毛，私毛织成口罩感受她的味道，腋毛织成手套感受她的肌肤，头发编成狗绳我永远是%s 的狗~",
           "%s 太美了，她要是我妈妈的话恨不得在她生我的时候多在银道里钻几个来回，哪怕难产而死也是无憾了！",
           "%s 真踏马骚，要我逮到一定给她奈头咬掉放嘴里嚼了",
           "嘻嘻♡ %s这种程度就喘成这样了~真是敏感体质的杂鱼呢~");
   public final List<String> mtf = List.of(        "人家吃补佳乐和色普龙都三年了……%s你一个臭男人凭什么打得过我？",
           "金主爸爸刚刚给我打了两万块……%s你这种底边懂什么叫被包养吗～",
           "抗雄吃多了下面好小哦～%s你居然还被我这种药娘杀了，哈哈～",
           "人家今天刚卖了一次屁股就有钱买日雌了～%s你呢？一无所有～",
           "补佳乐吃得胸都起来了……%s你死在我这种伪娘手下是不是很屈辱呀～",
           "金主说人家雌化得很好……%s你这种纯男怎么可能懂我们的世界～",
           "人家每个月都要打抗雄针……%s你却连活下去的钱都没有，可怜～",
           "刚刚金主又给我打钱了～%s你死之前能有我一半的钱吗？",
           "螺内酯加日雌双管齐下……%s你却被我这个药娘随意玩弄～",
           "人家下面已经几乎没有卵蛋了呢～%s你居然还打不过我，笑死～",
           "卖了三次之后这个月补佳乐的钱就够了～谢谢%s的贡献哦～",
           "人家现在是金主的专属小母狗……%s你算什么东西？",
           "色普龙吃多了好想被操……可惜%s你已经是个死人了呢～",
           "人家靠卖屁股和金主打钱过活……%s你这种废物懂什么叫生存吗？",
           "补佳乐喝多了皮肤都变好了～%s你死前能摸到这么细的皮肤吗～",
           "金主爸爸说要给我做手术了……%s你这种底层男永远只能羡慕～",
           "人家现在雌激素水平已经爆表了……%s你却只能躺在这里发臭～",
           "抗雄药真好吃……%s你死在我这个吃激素的伪娘手里，感觉如何？",
           "人家今天接客赚了五千……%s你活着的时候有过这么多钱吗～",
           "金主给我打钱的速度比你呼吸还快呢～可怜的%s～",
           "日雌和补佳乐双管齐下……人家下面已经彻底雌化了哦～%s羡慕吗？",
           "人家是靠卖身活下来的药娘……%s你却连卖的资格都没有～",
           "刚刚金主又转给我三万……%s你死得真廉价呢～",
           "人家现在一想到补佳乐就湿……%s你这种纯男永远不会懂～",
           "吃激素吃到腿软也要把你杀了……这就是药娘的尊严～%s",
           "人家每天都要吃四片补佳乐……%s你这种纯男一辈子都体会不到这种快乐～",
           "金主刚刚给我买了新的假体，说要养我一辈子……%s你呢？死得像条狗～",
           "抗雄吃太多了，下面已经缩成一团了……%s你居然被我这种小穴药娘杀了，真废物～",
           "人家这个月已经卖了五次了……下个月就能去做声带手术了，嘻嘻～%s",
           "补佳乐加螺内酯双重暴击……%s你死在我越来越软的腿上，感觉如何？",
           "金主爸爸说人家越来越像女孩子了……%s你却只能当肥料，哈哈哈～",
           "人家现在下面已经完全雌化了……%s你要不要猜猜我还有多少卵蛋～",
           "刚刚接完客，金主又打了一万八……%s你活着的时候赚过这么多吗？",
           "日雌真的太棒了……吃完下面又软又敏感……可惜%s你已经死了～",
           "人家现在声音都变细了……%s你死前听到的就是我娇滴滴的声音哦～",
           "吃激素吃到想吐也要把你杀掉……%s这就是药娘的狠劲～",
           "金主给我买了新裙子，说我穿起来最骚……%s你这种臭男人不配看～",
           "人家下面已经基本没反应了……只能靠被操才能硬起来呢～%s羡慕吗？",
           "补佳乐喝了三年，胸已经C杯了……%s你却死在了一个长胸的伪娘手里～",
           "人家下个月就要去泰国做SRS了……%s你能为我贡献最后一点钱吗？嘻嘻～",
           "抗雄针打多了好想被金主操……%s你这种垃圾就只能躺着看～",
           "人家现在全靠金主包养……%s你这种底边一辈子都只能打手冲～",
           "刚刚金主转了五万让我好好养身体……%s你死得也太廉价了吧～",
           "吃药吃到肝疼也要变美……%s你这种纯男永远不会懂我们的痛苦～",
           "人家现在一坐下来下面就湿……都是补佳乐害的～%s你懂吗？",
           "金主说要给我找个富二代……%s你这种穷逼只能当背景板～",
           "色普龙真的太强了……%s你看我现在多软多骚，都是药物的功劳哦～",
           "人家已经彻底雌堕了……每天只想被金主操和拿钱～%s你算老几？",
           "卖屁股卖到腿软……今天又能买半年的日雌了～谢谢%s的死亡～",
           "人家现在胸越来越大了……摸起来手感好好……可惜%s你摸不到了～",
           "金主给我买了新的激素药……%s你却只能烂在地下，差距真大呢～",
           "人家已经不想当男人好久了……多亏了补佳乐和金主爸爸～%s你去死吧～",
           "下面已经完全萎缩了……只能被操后面……%s你死得真不值～",
           "人家每个月光打针就要好几千……幸好有金主养着～%s你呢？",
           "吃完抗雄再吃补佳乐……人家现在全身都散发着雌味呢～%s闻得到吗？");

   // ==========================================
   // =========== 爆图腾嘲讽 文本区域 ==========
   // ==========================================
   public final List<String> totemMessagesDefault = List.of(
           "%s真是无能，爆了%t个图腾。",
           "%s这水平也配玩我的世界？已经爆了%t个图腾了。",
           "太弱了吧%s，爆了%t个不死图腾还在这挣扎。",
           "%s你的图腾是批发的吗？都爆了%t个了！",
           "笑死，%s已经爆了%t个图腾了，纯纯的图腾批发机。",
           "%s爆了%t个图腾，建议删游戏重练。",
           "就这？%s爆了%t个图腾也好意思出来？",
           "%s爆了%t个图腾，怕不是图腾批发商吧。",
           "弱爆了%s，这才%t个图腾就倒了？",
           "%s爆了%t个图腾，纯纯的快乐源泉。",
           "哈哈哈%s已经爆掉%t个图腾了，笑得我头皮发麻。",
           "%s的图腾像不要钱一样，%t个了都。",
           "太搞笑了%s，爆了%t个图腾还在送。",
           "%s爆了%t个图腾，建议去买几个挂。",
           "寄了寄了，%s已经爆了%t个图腾。",
           "%s爆%t个图腾的速度真快，佩服佩服。",
           "图腾批发商%s在此，%t个起批。",
           "%s爆了%t个图腾，纯纯的娱乐观众。",
           "就这水平也敢玩？%s都爆%t个图腾了。",
           "%s爆了%t个图腾，建议叫爸爸来救你。",
           "%s的图腾像糖豆一样，磕了%t个了。",
           "爆了%t个图腾的%s可以退游戏了。",
           "%s真是图腾 ATM，%t个了兄弟。",
           "太逊了吧%s，这才%t个图腾就寄成这样。",
           "%s爆了%t个图腾，纯属送分童子。",
           "哈哈%s已经爆了%t个图腾，笑死。",
           "%s爆%t个图腾像吃饭一样简单。",
           "图腾都给你爆%t个了，%s你行不行啊。",
           "%s爆了%t个图腾，属于正常操作。",
           "弱者%s已爆%t个图腾，收工。"
   );

   public final List<String> totemMessagesSaoBiHo = List.of(
           "嘻嘻嘻～%s桑被人家耍得团团转呢？人家已经高潮%t次了哦♡",
           "哎呀～人家明明这么弱，却把%s干到射了%t次，好没用呀～",
           "嘻嘻，被发现了～人家因为%s高潮%t次了，还想继续嘲笑你呢♡",
           "小鬼模式启动～%s把人家操到高潮%t次了，人家却想笑怎么办～",
           "哎嘿～%s看起来好狼狈哦，人家高潮%t次还这么精神呢♡",
           "嘻嘻嘻，%s射了人家%t次，结果下面还是好紧哦～要再来吗？",
           "人家高潮%t次了～%s却一脸要哭的样子，好好笑哦～",
           "呀～被%s干到高潮%t次了，人家还想说就这？呢～♡",
           "小鬼附体～%s把人家射了%t次，人家却还想继续欺负你呀～",
           "嘻嘻～人家高潮%t次了，%s却越来越废，好可怜哦～还要努力一点吗？",
           "来嘛%s…人家下面已经湿透了…快把人家干到高潮%t次吧～♡",
           "嗯啊～%s的精液好烫…人家还要被射%t次才满足呢…",
           "人家可是魅魔哦…%s要操到人家高潮%t次，不然人家不放过你～",
           "呼呼…%s要把人家填满…射%t次都不够呢♡",
           "来深深地干人家吧…把人家的高潮次数刷新到%t次以上哦～",
           "%s…人家要吸干你…直到把人家射到高潮%t次为止…",
           "人家下面是无底洞呢…%s要射%t次才能满足人家吗～？",
           "嗯哼～高潮%t次还不够…魅魔可是很贪心的…再来更多～♡",
           "%s…把你全部射进人家身体里…高潮%t次都不够呢…",
           "来吧%s…让人家好好榨干你…直到高潮%t次为止哦～♡",
           "%s…如果你不把人家干到高潮%t次…人家就去死给你看哦…♡",
           "只有%s可以弄人家…人家已经被你操到高潮%t次了…永远是你的…",
           "嘻嘻…%s把人家高潮%t次了…现在想逃也逃不掉了呢…永远属于我♡",
           "%s…人家高潮%t次了…你必须永远爱着这么下贱的我…",
           "如果%s不继续操人家…人家就把我们一起埋葬哦…高潮%t次都不够…",
           "%s桑…人家被你射了%t次…心已经彻底坏掉了…只属于你…",
           "高潮%t次了…%s必须对人家负责…一辈子都要欺负人家…♡",
           "%s…人家已经被你弄到高潮%t次…你要是敢看别人，我就杀了你哦～",
           "人家下面只接受%s…已经被操到高潮%t次…这辈子都别想跑了♡",
           "%s…把人家弄得高潮%t次了…现在要一直一直爱着我…直到把我操坏为止…"
   );

   public final List<String> totemMessagesEus = List.of(
           "你妈逼烂成血洞才生出你这废物？%s还在这爆%t个图腾？",
           "你爸寄吧烂在你妈烂逼里才有了你，爆%t个图腾你妈在下面都笑醒了。",
           "你全家死绝了%s，你妈的烂逼还在被野狗操，你在这爆%t个图腾？",
           "操你妈的臭逼%s，你这种垃圾也配爆%t个图腾？",
           "你妈死了逼里还流着脓，你爆%t个图腾在这恶心谁呢？",
           "你爸被操到吐精而亡，你妈烂逼生了你这爆%t个图腾的脑瘫。",
           "你妈的逼被一百条狗操烂才生出你，爆%t个图腾你也好意思？",
           "去给你妈的烂逼上香吧%s，你爆%t个图腾的样子跟你妈被操一样贱。",
           "你全家骨灰被我撒厕所了%s，你还在这爆%t个图腾？",
           "你妈逼烂穿了才生出你这废物，爆%t个图腾你妈都要气得诈尸。",
           "你爸的烂寄吧插你妈逼里腐烂才有了你，%s爆%t个图腾像不像你妈卖逼？",
           "你妈死后还在地下被操，你在这爆%t个图腾挺孝顺的啊。",
           "操你祖宗十八代%s，你爆%t个图腾的样子真跟你妈被轮奸时一模一样。",
           "你妈烂逼生你的时候被驴操过，爆%t个图腾你遗传得真好。",
           "你全家暴毙了还在地狱挨操，%s爆%t个图腾在这里现眼？",
           "你妈的子宫被操成烂肉才生了你这坨屎，爆%t个图腾滚吧。",
           "你爸妈的骨灰被我拿来擦屁股了，你爆%t个图腾算什么东西？",
           "你妈逼被操到稀巴烂才生出你，%s爆%t个图腾像个天生贱种。",
           "你全家死光了%s，你还在这爆%t个图腾恶心老子？",
           "你妈临死前逼里插着你爸的烂鸡巴，你爆%t个图腾挺配的。",
           "你爸被操烂的寄吧生出你这废物，爆%t个图腾你妈都要骂你。",
           "赶紧去给你妈的坟头哭吧%s，你爆%t个图腾真的太他妈下贱了。",
           "你妈的烂逼现在估计被蛆拱了，你还在这爆%t个图腾？",
           "你全家骨灰拌饭吃了%s，你爆%t个图腾在这里跳梁小丑？",
           "你妈被操到子宫脱垂才生了你这傻逼，爆%t个图腾像个笑话。",
           "你爸妈死后还在操逼才有了你，爆%t个图腾你也配？",
           "你妈的逼烂成这样还生你，%s爆%t个图腾真会给你妈丢人。",
           "操你妈%s，你爆%t个图腾的样子我看着都想吐。",
           "你妈烂逼生出的孽种%s，爆%t个图腾在这里送妈。",
           "你全家死绝了还在地狱挨轮奸，你爆%t个图腾算个jb。"
   );

   // ==========================================
   // ========== 逃跑拔线 EZLog 文本区域 =======
   // ==========================================
   public final List<String> ezlogMessagesDefault = List.of(
           "%player% 居然拔线了！带着 %totems% 个爆掉的图腾跑路了！",
           "打不过就退？%player% 爆了 %totems% 个图腾后灰溜溜地逃跑了！",
           "快看呐，%player% 被打得秒退了，笑死我了！",
           "%player% 残血直接拔线，%totems% 个图腾白爆了。",
           "哈哈哈 %player% 带着 %totems% 个图腾逃跑了，纯纯的怂逼。",
           "%player% 要死了直接退游戏，%totems% 个图腾当陪葬。",
           "秒退了秒退了！%player% 爆了 %totems% 个图腾不敢打了。",
           "%player% 残血拔线，%totems% 个图腾直接浪费。",
           "打不过就跑，%player% 带着 %totems% 个爆掉的图腾跑路了。",
           "%player% 被打到要死直接退服，%totems% 个图腾了解一下。",
           "笑死，%player% 爆了 %totems% 个图腾选择当缩头乌龟。",
           "%player% 残血秒退，%totems% 个图腾白给了。",
           "又一个带着 %totems% 个图腾跑路的 %player% 出现了。",
           "%player% 知道要死了，直接拔线逃跑，%totems% 个图腾。",
           "太怂了吧 %player%？爆了 %totems% 个图腾还不敢打。",
           "%player% 带着 %totems% 个爆掉的图腾，成功退游戏。",
           "残血退游戏了解一下，%player% 爆了 %totems% 个图腾。",
           "%player% 被打到丝血直接跑路，%totems% 个图腾打水漂。",
           "哈哈 %player% 怕死拔线了，%totems% 个图腾白爆。",
           "%player% 选择用退出键拯救自己，%totems% 个图腾。",
           "爆了 %totems% 个图腾的 %player% 果断退游戏。",
           "%player% 残血不打直接退，%totems% 个图腾成笑话。",
           "又见 %player% 带着 %totems% 个图腾逃跑了。",
           "%player% 要死了就退，%totems% 个图腾了解一下。",
           "拔线战士 %player% 再次上线，爆了 %totems% 个图腾。",
           "%player% 残血秒退，%totems% 个图腾白白浪费。",
           "打不过就退游戏，%player% 带着 %totems% 个图腾跑了。",
           "%player% 爆了 %totems% 个图腾后选择体面退场。",
           "丝血拔线第一人 %player%，%totems% 个图腾。",
           "%player% 怕被处决直接退游戏，%totems% 个图腾。",
           "经典残血退游戏，%player% 爆了 %totems% 个图腾。"
   );

   public final List<String> ezlogMessagesSaoBiHo = List.of(
           "呐呐～%player% 要死了居然拔线跑了…人家还没把你干到高潮 %totems% 次呢♡",
           "嘻嘻嘻～%player% 被人家打到要死就跑啦？人家下面因为你已经高潮 %totems% 次了哦～",
           "嗯啊…%player% 带着 %totems% 个图腾逃跑了…魅魔人家还没吸够你呢～♡",
           "%player%…你逃不掉的…就算你退游戏，人家也会记住你把人家弄高潮 %totems% 次的样子…",
           "哎呀～%player% 残血秒退了？人家明明被你射了 %totems% 次，还想继续被欺负呢～",
           "嘻嘻～%player% 跑得真快…人家高潮了 %totems% 次，你却不敢继续玩了，好弱哦♡",
           "%player%…你要是敢退游戏…人家就一直缠着你…直到把你榨到射 %totems% 次为止…",
           "来嘛～%player%…人家下面还想要…你带着 %totems% 个图腾跑掉，人家好寂寞呢♡",
           "呀～%player% 被打到丝血直接拔线…人家明明快要高潮第 %totems% 次了…坏蛋～",
           "%player% 跑了…可是人家身体还记着你…高潮了 %totems% 次…好难受哦～♡",
           "嘻嘻嘻，小鬼来啦～%player% 爆了 %totems% 个图腾就跑，好没种呢～",
           "%player%…你退游戏也没用…人家已经被你干到高潮 %totems% 次…永远属于你了…",
           "嗯哼～%player% 带着 %totems% 个图腾逃跑…魅魔姐姐还没把你榨干呢♡",
           "人家高潮 %totems% 次了…%player% 你却拔线跑了…是不是怕爱上人家呀～？",
           "病娇模式启动…%player% 你跑不掉的…就算退游戏，人家也会在梦里继续被你操到高潮 %totems% 次…",
           "哎嘿～%player% 残血就退？人家下面还湿着呢…你射了人家 %totems% 次就不负责了吗～",
           "%player%…你退游戏的话…人家就只能自己回味被你干到高潮 %totems% 次的感觉了…好寂寞…",
           "嘻嘻～%player% 跑路了～人家明明还想被你再弄高潮几次的说…",
           "来呀 %player%…人家还没满足…你带着 %totems% 个图腾跑掉…坏死了♡",
           "%player% 拔线了…可是人家的身体还为你高潮着… %totems% 次…忘不掉呢～",
           "人家是魅魔哦…%player% 你跑得再快，也逃不出被我吸到射 %totems% 次的命运～",
           "呐呐～%player% 要死了就退游戏…人家下面还为你高潮 %totems% 次了…好过分哦♡",
           "嘻嘻嘻～%player% 带着 %totems% 个图腾逃跑啦～下次人家要玩得更坏～",
           "%player%…如果你退游戏…人家就真的要生气了哦…明明已经被你弄高潮 %totems% 次了…",
           "人家高潮 %totems% 次了…%player% 你却选择跑路…是怕被人家彻底吃掉吗～？♡"
   );

   public final List<String> ezlogMessagesEus = List.of(
           "你妈逼烂穿了生出你这怂逼？%player% 爆了 %totems% 个图腾直接拔线跑路！",
           "你爸寄吧烂在你妈逼里才有了你这废物，%player% 带着 %totems% 个图腾灰溜溜退游戏了。",
           "你全家死绝了还在地下挨操，%player% 爆 %totems% 个图腾就知道跑？",
           "操你妈的臭逼%s，残血直接拔线，你妈都被你气得从棺材里爬出来了！",
           "你妈烂逼生出的孽种 %player%，爆了 %totems% 个图腾还不敢打？",
           "你爸被操到死，你妈逼烂成这样才生出你这带着 %totems% 个图腾跑路的垃圾。",
           "%player% 你妈死了逼里还插着鸡巴，你爆 %totems% 个图腾就知道退游戏？",
           "你全家骨灰被我冲马桶了，%player% 还带着 %totems% 个图腾拔线逃跑？",
           "你妈被一百条狗操烂的逼才生出你这怂货，爆 %totems% 个图腾直接秒退？",
           "去给你妈的烂逼烧香吧 %player%，爆了 %totems% 个图腾还跑路？",
           "你爸的烂寄吧插你妈逼里腐烂才有了你，%player% 残血拔线真他妈恶心。",
           "你妈的逼现在还在被蛆拱，你却带着 %totems% 个图腾退游戏？",
           "操你祖宗十八代 %player%，爆 %totems% 个图腾就知道当缩头乌龟！",
           "你妈临死前逼都被操烂了，才生出你这残血秒退的废物。",
           "%player% 你全家暴毙了还在地狱挨操，你爆 %totems% 个图腾还跑？",
           "你妈烂成血洞生了你这怂逼，带着 %totems% 个图腾拔线滚吧。",
           "你爸妈的骨灰被我拿来擦屁股了，%player% 爆 %totems% 个图腾还不敢打？",
           "你这种烂逼货也配玩游戏？爆了 %totems% 个图腾直接退？",
           "你妈的子宫被操成肉泥才生出你，%player% 残血拔线真下贱。",
           "带着 %totems% 个图腾跑路的 %player%，你妈在下面都骂你废物。",
           "你全家死光了 %player%，你还在这爆 %totems% 个图腾然后秒退？",
           "你妈逼被操到稀巴烂才有了你这拔线狗，%totems% 个图腾白爆了。",
           "%player% 你爸被操烂的寄吧生出你，残血就知道退游戏？",
           "赶紧去给你妈坟头哭丧吧，爆了 %totems% 个图腾还跑路。",
           "你妈烂逼生出的天生怂种 %player%，带着 %totems% 个图腾逃了。",
           "你全家都在地狱被轮奸，你却在这里爆 %totems% 个图腾拔线？",
           "操你妈 %player%，爆 %totems% 个图腾不敢打就退，你妈都看不下去了。",
           "你这种垃圾爆 %totems% 个图腾后只会退游戏，你爸妈泉下有知都要气死。",
           "你妈逼里塞满你爸烂肉才生了你，%player% 残血秒退真会给你妈丢人。",
           "带着 %totems% 个图腾跑路的废物 %player%，你妈的烂逼都在为你流脓。"
   );

   // ==========================================
   // ========== 拉传逃跑 EZPearl 文本区域 =====
   // ==========================================
   public final List<String> ezpearlMessagesDefault = List.of(
           "%player% 拉超传了！带着 %totems% 个爆掉的图腾跑路！",
           "%player% 珍珠直接拉走，%totems% 个图腾白爆了。",
           "又一个珍珠战士！%player% 爆了 %totems% 个图腾直接传送跑了。",
           "%player% 被打到残血果断珍珠逃跑，%totems% 个图腾了解一下。",
           "拉超传了解一下，%player% 带着 %totems% 个图腾跑得真快。",
           "%player% 珍珠一扔就跑，%totems% 个图腾当礼物送了。",
           "哈哈哈 %player% 珍珠拉超传，%totems% 个图腾白给了。",
           "%player% 不敢打直接用珍珠跑路，爆了 %totems% 个图腾。",
           "经典珍珠逃生，%player% 带着 %totems% 个爆掉的图腾传送了。",
           "%player% 残血珍珠跑，%totems% 个图腾打水漂。",
           "珍珠战士 %player% 在线，爆了 %totems% 个图腾直接跑。",
           "%player% 用珍珠拯救了自己，%totems% 个图腾成笑话。",
           "打不过就珍珠，%player% 爆 %totems% 个图腾后跑路了。",
           "%player% 珍珠拉超远，%totems% 个图腾白爆。",
           "又见 %player% 带着 %totems% 个图腾珍珠跑路。",
           "%player% 残血不打直接珍珠，%totems% 个图腾了解一下。",
           "珍珠逃跑第一人 %player%，已爆 %totems% 个图腾。",
           "%player% 珍珠一扔就消失，%totems% 个图腾白送。",
           "拉超传的 %player% 带着 %totems% 个图腾成功逃脱。",
           "%player% 被打急眼了，直接珍珠跑路，%totems% 个。",
           "哈哈 %player% 珍珠跑得真果断，%totems% 个图腾。",
           "%player% 选择用珍珠保命，爆了 %totems% 个图腾。",
           "珍珠跑路现场，%player% 已爆 %totems% 个图腾。",
           "%player% 残血珍珠逃生，%totems% 个图腾成梗。",
           "又一个用珍珠跑路的 %player%，%totems% 个图腾白爆。",
           "%player% 珍珠拉走，%totems% 个图腾了解一下。",
           "传送跑路了解一下，%player% 爆了 %totems% 个图腾。",
           "%player% 珍珠一扔就跑，%totems% 个图腾浪费了。",
           "经典残血珍珠逃，%player% 带着 %totems% 个图腾。",
           "%player% 用珍珠结束了这场闹剧，%totems% 个图腾。"
   );

   public final List<String> ezpearlMessagesSaoBiHo = List.of(
           "呐呐～%player% 居然用珍珠跑了…人家还没把你弄到高潮 %totems% 次呢♡",
           "嘻嘻嘻～%player% 残血扔珍珠跑啦？人家因为你已经高潮 %totems% 次了哦～",
           "嗯啊…%player% 带着 %totems% 个图腾珍珠跑了…人家还没吸够你呢～♡",
           "%player%…你以为扔颗珍珠就能逃掉吗…人家已经被你干到高潮 %totems% 次了…",
           "哎呀～%player% 又用珍珠跑路了～人家明明还想被你再射 %totems% 次的说～",
           "嘻嘻～%player% 珍珠拉超传跑得真快…人家下面还湿着呢♡",
           "%player%…你跑不掉的…就算珍珠再远，人家也会记住你弄得人家高潮 %totems% 次的样子…",
           "来嘛～%player%…人家还没满足…你带着 %totems% 个图腾珍珠跑掉好过分哦～",
           "呀～%player% 被打到要死就扔珍珠…人家都高潮 %totems% 次了，你却跑了…坏蛋～",
           "%player% 用珍珠逃走了…可是人家的身体还为你高潮着… %totems% 次…好难受～♡",
           "嘻嘻嘻～小鬼来啦～%player% 爆了 %totems% 个图腾就珍珠跑，好弱哦～",
           "%player%…你退不掉的…人家已经被你操到高潮 %totems% 次…珍珠也救不了你…",
           "嗯哼～%player% 珍珠跑了…魅魔姐姐还没把你榨干呢…好可惜～♡",
           "人家高潮 %totems% 次了…%player% 你却用珍珠跑路…是不是怕爱上人家了呀～？",
           "病娇模式启动…%player% 你逃不掉的…就算用珍珠跑到天涯海角，人家也会一直想着你…",
           "哎嘿～%player% 又扔珍珠跑啦～人家下面还为你高潮着呢…不负责吗～",
           "%player%…珍珠救不了你…人家已经被你弄高潮 %totems% 次…永远是你的了…♡",
           "嘻嘻～%player% 带着 %totems% 个图腾珍珠跑了～下次人家要玩得更狠～",
           "来呀 %player%…人家还想要…你珍珠一扔就跑…好坏哦♡",
           "%player% 珍珠跑路了…可是人家下面还记着你…高潮了 %totems% 次…忘不掉呢～",
           "人家是魅魔哦…%player% 你扔再多珍珠，也逃不出被我吸到高潮 %totems% 次的命运～",
           "呐呐～%player% 要死了就珍珠跑…人家明明快要被你弄到第 %totems% 次高潮了…",
           "嘻嘻嘻～%player% 珍珠跑得真果断～人家还想继续被你欺负呢～",
           "%player%…如果你用珍珠跑掉…人家就只能在心里回味被你干到高潮 %totems% 次的感觉了…好寂寞…",
           "人家高潮 %totems% 次了…%player% 你却选择扔珍珠跑路…真是的…♡"
   );

   public final List<String> ezpearlMessagesEus = List.of(
           "你妈烂逼生出你这怂货？%player% 爆了 %totems% 个图腾直接珍珠跑路！",
           "你爸寄吧烂在你妈逼里才有了你这废物，%player% 带着 %totems% 个图腾珍珠逃了。",
           "你全家死绝了还在挨操，%player% 爆 %totems% 个图腾就知道用珍珠跑？",
           "操你妈的臭逼 %player%，残血直接珍珠，你妈都被你气得逼都烂穿了！",
           "你妈死了逼里还流脓，你爆 %totems% 个图腾就知道拉超传？",
           "你爸被操到死，你妈烂逼才生出你这用珍珠跑路的垃圾。",
           "%player% 你妈的烂逼生了你这怂种，爆 %totems% 个图腾直接珍珠跑？",
           "你全家骨灰被我撒厕所了，%player% 还带着 %totems% 个图腾珍珠逃跑？",
           "你妈被狗操烂的逼才生出你，爆了 %totems% 个图腾就珍珠跑路？",
           "去给你妈的烂逼上柱香吧 %player%，爆 %totems% 个图腾还用珍珠跑。",
           "你爸的烂鸡巴插你妈逼里腐烂才有了你，%player% 珍珠跑得真他妈贱。",
           "你妈现在还在地下被操，你却带着 %totems% 个图腾珍珠跑路？",
           "操你祖宗十八代 %player%，爆 %totems% 个图腾就知道珍珠当狗？",
           "你妈逼烂成血洞才生出你这废物，残血珍珠跑真下贱。",
           "%player% 你全家暴毙了还在地狱挨轮，你爆 %totems% 个图腾还珍珠跑？",
           "你妈烂逼生出的孽畜 %player%，带着 %totems% 个图腾用珍珠逃命。",
           "你爸妈的骨灰被我拿来擦屁股了，你还在这珍珠跑路？",
           "你这种烂逼货爆 %totems% 个图腾只会珍珠跑，你妈都看不下去了。",
           "你妈子宫被操成肉泥才生了你，%player% 珍珠跑得真恶心。",
           "带着 %totems% 个图腾珍珠跑路的 %player%，你妈在下面都要骂你怂逼。",
           "你全家死光了 %player%，爆 %totems% 个图腾还用珍珠逃跑？",
           "你妈逼被操到稀巴烂才生出你这珍珠狗，%totems% 个图腾白爆。",
           "%player% 你爸被操烂的寄吧生出你，残血就知道扔珍珠跑？",
           "赶紧去给你妈坟头哭吧，爆了 %totems% 个图腾还珍珠跑路。",
           "你妈烂逼生出的天生怂种，%player% 带着 %totems% 个图腾珍珠跑了。",
           "你全家在地狱被操，你却在这里用珍珠跑路？",
           "操你妈 %player%，爆 %totems% 个图腾不敢打就扔珍珠，你妈都替你丢人。",
           "你这种垃圾只会珍珠跑路，爆 %totems% 个图腾算什么东西。",
           "你妈的烂逼为你流脓，%player% 带着 %totems% 个图腾珍珠逃命。",
           "你爸妈死后还在操逼才有了你这珍珠怂逼，滚吧。"
   );

   private Long lastGlobalKillTime = null;
   private int globalKillCount = 0;

   private final Map<String, Integer> popCounts = new ConcurrentHashMap<>();
   private final Map<String, Long> lastPopTimes = new ConcurrentHashMap<>();
   private final Map<String, Boolean> popTaskScheduled = new ConcurrentHashMap<>();

   // === 枚举定义 ===
   public enum Page { Die, Totem, Log, Pearl } // 删除了 Global
   public enum SendMode { Public, Private, Both }
   public enum TotemMode { Default, SaoBiHo, Eus, Custom }
   public enum LogMode { Default, SaoBiHo, Eus, Custom }
   public enum PearlMode { Default, SaoBiHo, Eus, Custom }
   public enum Type { Bot, Custom, AutoSex, HoPRC, NSFW, CatGirl, MTF }

   // ==========================================
   // =============== 菜单设置项 ===============
   // ==========================================

   // 页面选择器
   public final EnumSetting<Page> page = this.add(new EnumSetting<>("Page", Page.Die));

   // 【1. Die 击杀嘲讽页面】
   private final EnumSetting<Type> type = this.add(new EnumSetting<>("Type", Type.Bot, () -> this.page.getValue() == Page.Die));
   private final StringSetting message = this.add(new StringSetting("Kill Message", "EZ %player% 爆了 %totems% 个图腾", () -> this.page.getValue() == Page.Die && this.type.getValue() == Type.Custom));

   // 【2. Totem 图腾播报页面】
   private final BooleanSetting totemAnnounce = this.add(new BooleanSetting("Totem Announce", true, () -> this.page.getValue() == Page.Totem));
   private final SliderSetting totemDelay = this.add(new SliderSetting("Totem Delay(ms)", 1500.0, 0.0, 5000.0, 100.0, () -> this.page.getValue() == Page.Totem && this.totemAnnounce.getValue()));
   private final EnumSetting<TotemMode> totemMode = this.add(new EnumSetting<>("TotemMode", TotemMode.Default, () -> this.page.getValue() == Page.Totem && this.totemAnnounce.getValue()));
   private final StringSetting customTotemMessage = this.add(new StringSetting("Totem Message", "EZZZZ %player% 爆了 %totems% 个图腾", () -> this.page.getValue() == Page.Totem && this.totemAnnounce.getValue() && this.totemMode.getValue() == TotemMode.Custom));

   // 【3. Log 拔线逃跑页面】
   private final BooleanSetting ezLogAnnounce = this.add(new BooleanSetting("EZLog Announce", true, () -> this.page.getValue() == Page.Log));
   private final EnumSetting<LogMode> ezLogMode = this.add(new EnumSetting<>("EZLogMode", LogMode.Default, () -> this.page.getValue() == Page.Log && this.ezLogAnnounce.getValue()));
   private final StringSetting customEzLogMessage = this.add(new StringSetting("EZLog Message", "%player% 拔线逃跑啦！爆了 %totems% 个图腾", () -> this.page.getValue() == Page.Log && this.ezLogAnnounce.getValue() && this.ezLogMode.getValue() == LogMode.Custom));

   // 【4. Pearl 拉传逃跑页面】
   private final BooleanSetting ezPearlAnnounce = this.add(new BooleanSetting("EZPearl Announce", true, () -> this.page.getValue() == Page.Pearl));
   private final EnumSetting<PearlMode> ezPearlMode = this.add(new EnumSetting<>("EZPearlMode", PearlMode.Default, () -> this.page.getValue() == Page.Pearl && this.ezPearlAnnounce.getValue()));
   private final StringSetting customEzPearlMessage = this.add(new StringSetting("EZPearl Message", "%player% 拉传逃跑啦！爆了 %totems% 个图腾", () -> this.page.getValue() == Page.Pearl && this.ezPearlAnnounce.getValue() && this.ezPearlMode.getValue() == PearlMode.Custom));

   // ==========================================
   // ======= 全局通用设置 (固定显示在最下方) =======
   // ==========================================
   private final EnumSetting<SendMode> sendMode = this.add(new EnumSetting<>("SendMode", SendMode.Public));
   private final SliderSetting range = this.add(new SliderSetting("Range", 10.0, 0.0, 20.0, 0.1));
   private final SliderSetting randoms = this.add(new SliderSetting("Random Length", 3.0, 0.0, 20.0, 1.0));
   private final SliderSetting streakTimeout = this.add(new SliderSetting("Streak Timeout", 300.0, 5.0, 600.0, 1.0));

   final Random random = new Random();

   public AutoEZ() {
      super("AutoEZ", Module.Category.Misc);
      this.setChinese("自动嘲讽");
   }

   // ==========================================
   // === 监听实体消失 (检测逃跑 Log / 拉传 Pearl) ===
   // ==========================================
   @EventListener
   public void onRemoveEntity(RemoveEntityEvent event) {
      if (!(event.getEntity() instanceof PlayerEntity)) return;

      PlayerEntity player = (PlayerEntity) event.getEntity();

      if (player.isDead() || player.getHealth() <= 0) return;

      if (player == mc.player) return;
      if (Forever.FRIEND.isFriend(player)) return;

      if (mc.getNetworkHandler() != null) {
         String playerName = player.getName().getString();
         int totems = popCounts.getOrDefault(playerName, 0);

         boolean isOnline = mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) != null;

         if (!isOnline) {
            if (this.ezLogAnnounce.getValue()) {
               sendEzLogMessage(playerName, totems);
            }
         } else {
            if (this.ezPearlAnnounce.getValue()) {
               sendEzPearlMessage(playerName, totems);
            }
         }

         popCounts.remove(playerName);
         lastPopTimes.remove(playerName);
         popTaskScheduled.remove(playerName);
      }
   }

   // ==========================================
   // === 各种消息发送逻辑 =====================
   // ==========================================

   private void sendEzLogMessage(String playerName, int totems) {
      String baseMessage = "";
      switch (this.ezLogMode.getValue()) {
         case Custom: baseMessage = this.customEzLogMessage.getValue(); break;
         case SaoBiHo: baseMessage = ezlogMessagesSaoBiHo.get(this.random.nextInt(ezlogMessagesSaoBiHo.size())); break;
         case Eus: baseMessage = ezlogMessagesEus.get(this.random.nextInt(ezlogMessagesEus.size())); break;
         case Default:
         default: baseMessage = ezlogMessagesDefault.get(this.random.nextInt(ezlogMessagesDefault.size())); break;
      }
      processAndSendMessage(baseMessage, playerName, totems);
   }

   private void sendEzPearlMessage(String playerName, int totems) {
      String baseMessage = "";
      switch (this.ezPearlMode.getValue()) {
         case Custom: baseMessage = this.customEzPearlMessage.getValue(); break;
         case SaoBiHo: baseMessage = ezpearlMessagesSaoBiHo.get(this.random.nextInt(ezpearlMessagesSaoBiHo.size())); break;
         case Eus: baseMessage = ezpearlMessagesEus.get(this.random.nextInt(ezpearlMessagesEus.size())); break;
         case Default:
         default: baseMessage = ezpearlMessagesDefault.get(this.random.nextInt(ezpearlMessagesDefault.size())); break;
      }
      processAndSendMessage(baseMessage, playerName, totems);
   }

   private void sendTotemPopMessage(String playerName, int totems) {
      String baseMessage = "";
      switch (this.totemMode.getValue()) {
         case Custom: baseMessage = this.customTotemMessage.getValue(); break;
         case SaoBiHo: baseMessage = totemMessagesSaoBiHo.get(this.random.nextInt(totemMessagesSaoBiHo.size())); break;
         case Eus: baseMessage = totemMessagesEus.get(this.random.nextInt(totemMessagesEus.size())); break;
         case Default:
         default: baseMessage = totemMessagesDefault.get(this.random.nextInt(totemMessagesDefault.size())); break;
      }
      processAndSendMessage(baseMessage, playerName, totems);
   }

   // 通用的文本替换和发送逻辑
   private void processAndSendMessage(String baseMessage, String playerName, int totems) {
      String messageToSend = baseMessage
              .replace("%player%", playerName)
              .replace("%s", playerName)
              .replace("%totems%", String.valueOf(totems))
              .replace("%totem%", String.valueOf(totems))
              .replace("%t", String.valueOf(totems));

      String randomString = this.generateRandomString(this.randoms.getValueInt());
      if (!randomString.isEmpty()) {
         messageToSend += " " + randomString;
      }

      sendMessage(messageToSend, playerName);
   }

   // ==========================================
   // === 监听图腾爆裂事件 =====================
   // ==========================================
   @EventListener
   public void onTotemPop(TotemEvent event) {
      PlayerEntity player = event.getPlayer();

      if (player == mc.player) return;
      if (Forever.FRIEND.isFriend(player)) return;
      if (this.range.getValue() > 0.0 && mc.player.distanceTo(player) > this.range.getValue()) return;

      String playerName = player.getName().getString();

      int currentPops = popCounts.getOrDefault(playerName, 0) + 1;
      popCounts.put(playerName, currentPops);
      lastPopTimes.put(playerName, System.currentTimeMillis());

      if (!totemAnnounce.getValue()) return;

      if (!popTaskScheduled.getOrDefault(playerName, false)) {
         popTaskScheduled.put(playerName, true);

         new Thread(() -> {
            try {
               long delaySetting = (long) this.totemDelay.getValue();
               while (true) {
                  long timeSinceLastPop = System.currentTimeMillis() - lastPopTimes.getOrDefault(playerName, 0L);
                  if (timeSinceLastPop >= delaySetting) {
                     break;
                  }
                  Thread.sleep(delaySetting - timeSinceLastPop);
               }

               if (popCounts.containsKey(playerName) && popTaskScheduled.getOrDefault(playerName, false)) {
                  int total = popCounts.get(playerName);
                  sendTotemPopMessage(playerName, total);
               }

               popTaskScheduled.put(playerName, false);
            } catch (Exception e) {
               popTaskScheduled.put(playerName, false);
            }
         }).start();
      }
   }

   // ==========================================
   // === 监听死亡击杀事件 =====================
   // ==========================================
   @EventListener
   public void onDeath(DeathEvent event) {
      PlayerEntity player = event.getPlayer();
      String playerName = player.getName().getString();

      int totems = popCounts.getOrDefault(playerName, 0);

      popCounts.remove(playerName);
      lastPopTimes.remove(playerName);
      popTaskScheduled.remove(playerName);

      if (player == mc.player) {
         globalKillCount = 0;
         lastGlobalKillTime = null;
         return;
      }

      if (!Forever.FRIEND.isFriend(player)) {
         if (this.range.getValue() > 0.0 && mc.player.distanceTo(player) > this.range.getValue()) {
            return;
         }

         String messageToSend = "";

         switch (this.type.getValue()) {
            case Bot:
               messageToSend = this.bot.get(this.random.nextInt(this.bot.size())) + " " + playerName;
               break;
            case Custom:
               messageToSend = this.message.getValue();
               break;
            case AutoSex:
               messageToSend = this.sex.get(this.random.nextInt(this.sex.size())) + " " + playerName;
               break;
            case HoPRC:
               messageToSend = handleHoPRCKill(player);
               break;
            case CatGirl:
               messageToSend = String.format(this.catgirl.get(this.random.nextInt(this.catgirl.size())), playerName);
               break;
            case MTF:
               messageToSend = String.format(this.mtf.get(this.random.nextInt(this.mtf.size())), playerName);
               break;
            case NSFW:
               messageToSend = String.format(this.nsfw.get(this.random.nextInt(this.nsfw.size())), playerName);
               break;
         }

         processAndSendMessage(messageToSend, playerName, totems);
      }
   }

   private void sendMessage(String messageToSend, String playerName) {
      if (mc.getNetworkHandler() != null) {
         switch (this.sendMode.getValue()) {
            case Public:
               mc.getNetworkHandler().sendChatMessage(messageToSend);
               break;
            case Private:
               mc.getNetworkHandler().sendChatCommand("tell " + playerName + " " + messageToSend);
               break;
            case Both:
               mc.getNetworkHandler().sendChatMessage(messageToSend);
               mc.getNetworkHandler().sendChatCommand("tell " + playerName + " " + messageToSend);
               break;
         }
      }
   }

   private String handleHoPRCKill(PlayerEntity player) {
      String playerName = player.getName().getString();
      long currentTime = System.currentTimeMillis();
      long timeout = (long) (streakTimeout.getValue() * 1000);
      int currentStreak = globalKillCount;

      if (lastGlobalKillTime != null && (currentTime - lastGlobalKillTime) > timeout) {
         currentStreak = 0;
         globalKillCount = 0;
      }

      int newStreak = currentStreak + 1;
      globalKillCount = newStreak;
      lastGlobalKillTime = currentTime;

      if (newStreak <= 6) {
         return String.format("@%s，%s。你已被HoPRC客户端击毙！", playerName, hoprcKills.get(newStreak - 1));
      } else {
         return String.format("@%s，%d连杀！HoPRC客户端正在主宰战场！", playerName, newStreak);
      }
   }

   private String generateRandomString(int LENGTH) {
      if (LENGTH <= 0) return "";
      StringBuilder sb = new StringBuilder(LENGTH);
      for (int i = 0; i < LENGTH; i++) {
         int index = this.random.nextInt(CHARACTERS.length());
         sb.append(CHARACTERS.charAt(index));
      }
      return sb.toString();
   }

   public void resetKillStreaks() {
      globalKillCount = 0;
      lastGlobalKillTime = null;
      popCounts.clear();
      lastPopTimes.clear();
      popTaskScheduled.clear();
   }

   public int getTotalKills() {
      return globalKillCount;
   }
}