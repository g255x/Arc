package dev.g255.arc.mod.modules.settings;

import dev.g255.arc.api.utils.i18n.SettingDesc;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import java.util.function.BooleanSupplier;

public abstract class Setting {
   private final BooleanSupplier visibility;
   private final String name;
   private boolean hidden;
   /** 所属模块名，由 Module.addSetting(...) 回填；用于按「模块.配置项」查说明文案 */
   private String owner;
   /** 惰性缓存的类型名，见 getType() */
   private String type;

   public Setting(String name) {
      this.name = name;
      this.visibility = null;
   }

   public Setting(String name, BooleanSupplier visibilityIn) {
      this.name = name;
      this.visibility = visibilityIn;
   }

   public boolean isVisible() {
      if (this.hidden) {
         return false;
      } else {
         return this.visibility == null || this.visibility.getAsBoolean();
      }
   }

   public void setHidden() {
      this.hidden = true;
   }

   public String getName() {
      return this.name;
   }

   public String getDisplayName() {
      return ChineseTranslator.setting(this.name);
   }

   public String getOwner() {
      return this.owner;
   }

   public void setOwner(String owner) {
      this.owner = owner;
   }

   /**
    * 悬停说明。先查「模块.配置项」，再查「*配置项」这类通用条目（例如所有模块都有的 Key/Drawn），
    * 都没有就返回空串（此时不弹信息框），不去猜内容。
    */
   public String getDescription() {
      return this.getDescription(null);
   }

   /**
    * 带取值的说明：枚举/分页类配置项悬停在某个具体选项上时，优先用它自己的说明，
    * 没有该选项的文案就退回整个配置项的说明。
    */
   public String getDescription(String option) {
      boolean chinese = ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue();
      String text = SettingDesc.get(this.owner, this.name, option, this.getType(), chinese);
      return text == null ? "" : text;
   }

   /** 用于查「*.配置项|类型」这类通用条目：BooleanSetting → Boolean，SliderSetting → Slider…… */
   private String getType() {
      if (this.type == null) {
         Class<?> clazz = this.getClass();

         while (clazz != null && clazz != Setting.class) {
            String simple = clazz.getSimpleName();
            if (simple.endsWith("Setting")) {
               this.type = simple.substring(0, simple.length() - "Setting".length());
               break;
            }

            clazz = clazz.getSuperclass();
         }

         if (this.type == null) {
            this.type = "";
         }
      }

      return this.type;
   }
}
