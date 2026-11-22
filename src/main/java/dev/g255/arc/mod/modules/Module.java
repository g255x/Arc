package dev.g255.arc.mod.modules;

import dev.g255.arc.Arc;
import dev.g255.arc.core.impl.CommandManager;
import dev.g255.arc.mod.Mod;
import dev.g255.arc.mod.modules.impl.client.AntiCheat;
import dev.g255.arc.mod.modules.impl.client.BaritoneModule;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import dev.g255.arc.mod.modules.impl.client.ColorsModule;
import dev.g255.arc.mod.modules.impl.client.HUD;
import dev.g255.arc.mod.modules.settings.Setting;
import dev.g255.arc.mod.modules.settings.impl.BindSetting;
import dev.g255.arc.mod.modules.settings.impl.BlockListSetting;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import dev.g255.arc.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;

public abstract class Module extends Mod {
   public final BooleanSetting drawn;
   private final List<Setting> settings;
   private final String description;
   private final Category category;
   private final BindSetting bindSetting;
   protected boolean state;
   private String chinese;

   public Module(String name, Category category) {
      this(name, "", category);
   }

   public Module(String name, String description, Category category) {
      super(name);
      this.settings = new ArrayList();
      this.category = category;
      this.description = description;
      this.bindSetting = this.add(new BindSetting("Key", this.isGuiModule() ? 344 : -1));
      this.drawn = this.add(new BooleanSetting("Drawn", !this.hideInModuleList()));
   }

   private boolean isGuiModule() {
      return this instanceof ClickGui;
   }

   private boolean hideInModuleList() {
      return this instanceof ColorsModule || this instanceof BaritoneModule || this instanceof AntiCheat || this instanceof ClientSetting || this instanceof HUD;
   }

   public void setChinese(String chinese) {
      this.chinese = chinese;
   }

   /** 模块中文名（没有设过就是 null）—— 搜索栏拿它做中文/拼音匹配 */
   public String getChineseName() {
      return this.chinese;
   }

   public String getArrayName() {
      String var10000 = this.getDisplayName();
      return var10000 + this.getArrayInfo();
   }

   public String getArrayInfo() {
      return this.getInfo() == null ? "" : " §7[§f" + this.getInfo() + "§7]";
   }

   public String getInfo() {
      return null;
   }

   public String getDisplayName() {
      // ClientSetting 在注册列表里排得较靠后，早期构造阶段 INSTANCE 仍为 null
      boolean chinese = ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue();
      return chinese && this.chinese != null ? this.chinese : this.getName();
   }

   public String getDescription() {
      boolean chinese = ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue();
      String localized = dev.g255.arc.api.utils.i18n.ModuleDesc.get(this.getName(), chinese);
      if (localized != null && !localized.isEmpty()) {
         return localized;
      }

      if (this.description != null && !this.description.isEmpty()) {
         return chinese && !dev.g255.arc.api.utils.i18n.LangUtil.containsCjk(this.description)
            ? dev.g255.arc.api.utils.i18n.LangUtil.ui(this.description)
            : this.description;
      }

      return "";
   }

   public Category getCategory() {
      return this.category;
   }

   public BindSetting getBindSetting() {
      return this.bindSetting;
   }

   public boolean isOn() {
      return this.state;
   }

   public boolean isOff() {
      return !this.isOn();
   }

   public void toggle() {
      if (this.isOn()) {
         this.disable();
      } else {
         this.enable();
      }

   }

   public void enable() {
      if (!this.state) {
         if (!nullCheck() && this.drawn.getValue() && ClientSetting.INSTANCE.toggle.getValue()) {
            int id = ClientSetting.INSTANCE.onlyOne.getValue() ? -1 : this.hashCode();
            switch ((ClientSetting.Style)ClientSetting.INSTANCE.messageStyle.getValue()) {
               case Mio -> CommandManager.sendMessageId("§2[+] §f" + this.getDisplayName(), id);
               case Debug -> CommandManager.sendMessageId(this.getCategory().name().toLowerCase() + "." + this.getDisplayName().toLowerCase() + ".§aenable", id);
               case Lowercase -> CommandManager.sendMessageId(this.getDisplayName().toLowerCase() + " §aenabled", id);
               case Melon -> CommandManager.sendMessageId("§b" + this.getDisplayName() + " §aEnabled.", id);
               case Normal -> CommandManager.sendMessageId("§f" + this.getDisplayName() + " §aEnabled", id);
               case Future -> CommandManager.sendMessageId("§7" + this.getDisplayName() + "§7 toggled §aon", id);
               case Chinese -> CommandManager.sendMessageId(this.getDisplayName() + " §a开启", id);
               case Moon -> CommandManager.sendChatMessageWidthIdNoSync("§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] [§3" + this.getDisplayName() + "§f] §7toggled §aon", id);
               case Earth -> CommandManager.sendChatMessageWidthIdNoSync("§l" + this.getDisplayName() + " §aenabled.", id);
            }
         }

         if (Arc.MODULE != null) {
            Arc.MODULE.showToggleBanner(this, true);
         }

         this.state = true;
         Arc.EVENT_BUS.subscribe(this);
         this.onToggle();
         this.onEnable();
      }
   }

   public void disable() {
      if (this.state) {
         if (!nullCheck() && this.drawn.getValue() && ClientSetting.INSTANCE.toggle.getValue()) {
            int id = ClientSetting.INSTANCE.onlyOne.getValue() ? -1 : this.hashCode();
            switch ((ClientSetting.Style)ClientSetting.INSTANCE.messageStyle.getValue()) {
               case Mio -> CommandManager.sendMessageId("§4[-] §f" + this.getDisplayName(), id);
               case Debug -> CommandManager.sendMessageId(this.getCategory().name().toLowerCase() + "." + this.getDisplayName().toLowerCase() + ".§cdisable", id);
               case Lowercase -> CommandManager.sendMessageId(this.getDisplayName().toLowerCase() + " §cdisabled", id);
               case Melon -> CommandManager.sendMessageId("§b" + this.getDisplayName() + " §cDisabled.", id);
               case Normal -> CommandManager.sendMessageId("§f" + this.getDisplayName() + " §cDisabled", id);
               case Future -> CommandManager.sendMessageId("§7" + this.getDisplayName() + " toggled §coff", id);
               case Chinese -> CommandManager.sendMessageId(this.getDisplayName() + " §c关闭", id);
               case Moon -> CommandManager.sendChatMessageWidthIdNoSync("§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] [§3" + this.getDisplayName() + "§f] §7toggled §coff", id);
               case Earth -> CommandManager.sendChatMessageWidthIdNoSync("§l" + this.getDisplayName() + " §cdisabled.", id);
            }
         }

         if (Arc.MODULE != null) {
            Arc.MODULE.showToggleBanner(this, false);
         }

         this.state = false;
         Arc.EVENT_BUS.unsubscribe(this);
         this.onToggle();
         this.onDisable();
      }
   }

   public void sendMessage(String message) {
      CommandManager.sendMessage(message);
   }

   public void setState(boolean state) {
      if (this.state != state) {
         if (state) {
            this.enable();
         } else {
            this.disable();
         }

      }
   }

   public boolean setBind(String rkey) {
      if (rkey.equalsIgnoreCase("none")) {
         this.bindSetting.setValue(-1);
         return true;
      } else {
         int key;
         try {
            key = InputUtil.fromTranslationKey("key.keyboard." + rkey.toLowerCase()).getCode();
         } catch (NumberFormatException var4) {
            if (!nullCheck()) {
               this.sendMessage("§4Bad bind!");
            }

            return false;
         }

         if (rkey.equalsIgnoreCase("none")) {
            key = -1;
         }

         if (key == 0) {
            return false;
         } else {
            this.bindSetting.setValue(key);
            return true;
         }
      }
   }

   public void onDisable() {
   }

   public void onEnable() {
   }

   public void onToggle() {
   }

   /** SHIFT+左键把本模块设置重置为默认值后调用，用于让模块自己重新应用内部状态。 */
   public void onSettingsReset() {
   }

   public void onLogin() {
   }

   public void onLogout() {
   }

   public void onRender2D(DrawContext drawContext, float tickDelta) {
   }

   public void onRender3D(MatrixStack matrixStack) {
   }

   public void addSetting(Setting setting) {
      setting.setOwner(this.getName());
      this.settings.add(setting);
   }

   public StringSetting add(StringSetting setting) {
      this.addSetting(setting);
      return setting;
   }

   public ColorSetting add(ColorSetting setting) {
      this.addSetting(setting);
      return setting;
   }

   public SliderSetting add(SliderSetting setting) {
      this.addSetting(setting);
      return setting;
   }

   public BooleanSetting add(BooleanSetting setting) {
      this.addSetting(setting);
      return setting;
   }

   public <T extends Enum<T>> EnumSetting<T> add(EnumSetting<T> setting) {
      this.addSetting(setting);
      return setting;
   }

   public BindSetting add(BindSetting setting) {
      this.addSetting(setting);
      return setting;
   }

   public BlockListSetting add(BlockListSetting setting) {
      this.addSetting(setting);
      return setting;
   }

   public List<Setting> getSettings() {
      return this.settings;
   }

   public static boolean nullCheck() {
      return mc.player == null || mc.player.input == null || mc.world == null;
   }

   public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      if (mc.getNetworkHandler() != null && mc.world != null) {
         PendingUpdateManager pendingUpdateManager = mc.world.getPendingUpdateManager().incrementSequence();

         try {
            int i = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
         } catch (Throwable var5) {
            if (pendingUpdateManager != null) {
               try {
                  pendingUpdateManager.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (pendingUpdateManager != null) {
            pendingUpdateManager.close();
         }

      }
   }

   public static enum Category {
      Combat {
         public String getIcon() {
            return "b";
         }
      },
      Misc {
         public String getIcon() {
            return "[";
         }
      },
      Render {
         public String getIcon() {
            return "a";
         }
      },
      Movement {
         public String getIcon() {
            return "8";
         }
      },
      Player {
         public String getIcon() {
            return "5";
         }
      },
      Exploit {
         public String getIcon() {
            return "6";
         }
      },
      Client {
         public String getIcon() {
            return "7";
         }
      },
      HUD {
         public String getIcon() {
            return "H";
         }
      };

      public abstract String getIcon();

      // $FF: synthetic method
      private static Category[] $values() {
         return new Category[]{Combat, Misc, Render, Movement, Player, Exploit, Client, HUD};
      }
   }
}
