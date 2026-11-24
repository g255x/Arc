package dev.g255.arc.api.utils.i18n;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/**
 * 配置项说明表（中 / 英），内容放在资源包里：
 * assets/arc/lang/settings_zh_cn.json / settings_en_us.json
 *
 * 键有四种：
 *   "模块名.配置项名=取值" —— 枚举/分页类配置项某一个选项的说明（最优先）
 *   "模块名.配置项名"       —— 针对某个模块的精确说明
 *   "*.配置项名|类型"       —— 同名但类型不同的配置项（例如 Fill 在有的模块是颜色、有的是开关）
 *   "*.配置项名"            —— 所有模块共用的说明（如 Key / Drawn 这类每个模块都有的项）
 * 文案里带 {default} {min} {max} 占位符不在这里替换：说明是纯文字，数值由 ClickGUI 的滑块/开关自己显示，
 * 避免文案与运行时数值不同步。
 */
public final class SettingDesc {
   private static final Map<String, String> ZH = new HashMap<>();
   private static final Map<String, String> EN = new HashMap<>();
   private static boolean loaded;

   private SettingDesc() {
   }

   public static String get(String module, String setting, boolean chinese) {
      return get(module, setting, null, null, chinese);
   }

   public static String get(String module, String setting, String option, boolean chinese) {
      return get(module, setting, option, null, chinese);
   }

   /**
    * @param option 枚举/分页类配置项当前悬停的那个取值，可为 null
    * @param type   配置项类型（Boolean / Slider / Enum / Color / String / Bind），可为 null
    *               查找顺序：模块.配置项=取值 → *.配置项=取值 → 模块.配置项 → *.配置项|类型 → *.配置项
    *               选项级文案排在模块整体说明之前：当鼠标停在展开后的某一项时，
    *               应当解释那一项本身，而不是整条配置；未展开时传的是当前选中项，同样适用。
    */
   public static String get(String module, String setting, String option, String type, boolean chinese) {
      if (setting == null || setting.isEmpty()) {
         return null;
      }

      ensureLoaded();
      Map<String, String> map = chinese ? ZH : EN;
      boolean hasModule = module != null && !module.isEmpty();
      boolean hasOption = option != null && !option.isEmpty();
      boolean hasType = type != null && !type.isEmpty();
      if (hasOption) {
         if (hasModule) {
            String exact = map.get(module + "." + setting + "=" + option);
            if (exact != null && !exact.isEmpty()) {
               return exact;
            }
         }

         String generic = map.get("*." + setting + "=" + option);
         if (generic != null && !generic.isEmpty()) {
            return generic;
         }
      }

      if (hasModule) {
         String exact = map.get(module + "." + setting);
         if (exact != null && !exact.isEmpty()) {
            return exact;
         }
      }

      if (hasType) {
         String typed = map.get("*." + setting + "|" + type);
         if (typed != null && !typed.isEmpty()) {
            return typed;
         }
      }

      String generic = map.get("*." + setting);
      return generic == null || generic.isEmpty() ? null : generic;
   }

   /** 资源重载后可以调用，强制下次重新读取（语言文件被热替换时用得上） */
   public static void invalidate() {
      loaded = false;
      ZH.clear();
      EN.clear();
   }

   /** 第一次打开 ClickGUI 时预热，避免第一帧悬停才去解析 JSON */
   public static void warmUp() {
      ensureLoaded();
   }

   private static void ensureLoaded() {
      if (loaded) {
         return;
      }

      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null || mc.getResourceManager() == null) {
         // 资源管理器还没就绪，等下一次调用再试
         return;
      }

      loaded = true;
      load(mc, "zh_cn", ZH);
      load(mc, "en_us", EN);
      System.out.println("[Arc] setting descriptions loaded: zh=" + ZH.size() + ", en=" + EN.size());
   }

   private static void load(MinecraftClient mc, String lang, Map<String, String> out) {
      Identifier id = Identifier.of("arc", "lang/settings_" + lang + ".json");
      try {
         net.minecraft.resource.Resource resource = mc.getResourceManager().getResource(id).orElse(null);
         if (resource == null) {
            return;
         }

         try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
               JsonElement value = entry.getValue();
               if (value != null && value.isJsonPrimitive()) {
                  out.put(entry.getKey(), value.getAsString());
               }
            }
         }
      } catch (Exception exception) {
         System.out.println("[Arc] failed to load setting descriptions (" + lang + "): " + exception);
      }
   }
}
