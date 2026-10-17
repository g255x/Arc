package dev.kissoo.mod.modules;

import dev.kissoo.mod.modules.impl.hud.NotificationHud;
import dev.kissoo.Forever;
import dev.kissoo.core.impl.CommandManager;
import dev.kissoo.mod.Mod;
import dev.kissoo.mod.modules.impl.client.AntiCheat;
import dev.kissoo.mod.modules.impl.client.BaritoneModule;
import dev.kissoo.mod.modules.impl.client.ClickGui;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import dev.kissoo.mod.modules.impl.client.ColorsModule;
import dev.kissoo.mod.modules.impl.misc.PlayerTell;
import dev.kissoo.mod.modules.settings.Setting;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import hoprc.obf.zkm.NoRenameObfuscate;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;

public abstract class Module extends Mod {
   public final BooleanSetting drawn;
   private final List<Setting> settings = new ArrayList();
   private final String description;
   private final Module.Category category;
   private final BindSetting bindSetting;
   protected boolean state;
   private String chinese;

   public Module(String name, Module.Category category) {
      this(name, "", category);
   }

   public Module(String name, String description, Module.Category category) {
      super(name);
      this.category = category;
      this.description = description;
      this.bindSetting = this.add(new BindSetting("Key", this.isGuiModule() ? 344 : -1));
      this.drawn = this.add(new BooleanSetting("Drawn", !this.hideInModuleList()));
   }

   private boolean isGuiModule() {
      return this instanceof ClickGui;
   }

   private boolean hideInModuleList() {
      return this instanceof ColorsModule
              || this instanceof BaritoneModule
              || this instanceof AntiCheat
              || this instanceof ClientSetting
              || this instanceof PlayerTell;
   }

   public boolean isHidden() {
      return this instanceof PlayerTell;
   }

   @NoRenameObfuscate
   public void onUpdate() {
   }

   public void sendPacket(Packet<?> packet) {
      if (mc.getNetworkHandler() == null) return;
      mc.getNetworkHandler().sendPacket(packet);
   }

   public void setChinese(String chinese) {
      this.chinese = chinese;
   }

   public String getChinese() {
      return this.chinese;
   }

   public String getArrayName() {
      return this.getDisplayName() + this.getArrayInfo();
   }

   public String getArrayInfo() {
      return this.getInfo() == null ? "" : " §7[§f" + this.getInfo() + "§7]";
   }

   public String getInfo() {
      return null;
   }

   public String getDisplayName() {
      return ClientSetting.INSTANCE.chinese.getValue() && this.chinese != null ? this.chinese : this.getName();
   }

   public String getDescription() {
      return this.description;
   }

   public Module.Category getCategory() {
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

   public boolean isEnabled() {
      return this.state;
   }

   public void toggle() {
      if (this.isOn()) {
         this.disable();
      } else {
         this.enable();
      }
   }

   public void enable() {
      // 边界重复校验：模块激活前分别验证服务端签名的 capability（#5，避免共读单一布尔）。
      if (!dev.kissoo.auth.AuthGate.allowModuleActivation()) {
         return;
      }
      if (!this.state) {

         // ===== 原有聊天栏提示逻辑（保持不动）=====
         if (!nullCheck() && this.drawn.getValue() && ClientSetting.INSTANCE.toggle.getValue()) {
            int id = ClientSetting.INSTANCE.onlyOne.getValue() ? -1 : this.hashCode();
            String msg = "";
            boolean noSync = false;

            switch ((ClientSetting.Style) ClientSetting.INSTANCE.messageStyle.getValue()) {
               case Mio:      msg = "§2[+] §f" + this.getDisplayName(); break;
               case Debug:    msg = this.getCategory().name().toLowerCase() + "." + this.getDisplayName().toLowerCase() + ".§aenable"; break;
               case Lowercase:msg = this.getDisplayName().toLowerCase() + " §aenabled"; break;
               case Melon:    msg = "§b" + this.getDisplayName() + " §aEnabled."; break;
               case Normal:   msg = "§f" + this.getDisplayName() + " §aEnabled"; break;
               case Future:   msg = "§7" + this.getDisplayName() + " toggled §aon"; break;
               case Chinese:  msg = this.getDisplayName() + " §a开启"; break;
               case Moon:
                  msg = "§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] [§3" + this.getDisplayName() + "§f] §7toggled §aon";
                  noSync = true;
                  break;
               case Earth:
                  msg = "§l" + this.getDisplayName() + " §aenabled.";
                  noSync = true;
                  break;
               case None:     msg = this.getDisplayName() + " enabled"; break;
            }

            if (!msg.isEmpty()) {
               if (noSync) {
                  CommandManager.sendChatMessageWidthIdNoSync(msg, id);
               } else {
                  CommandManager.sendMessageId(msg, id);
               }
            }
         }

         // ===== 通知卡片：独立于聊天栏开关，单独判断 =====
         if (!nullCheck()
                 && NotificationHud.INSTANCE != null
                 && NotificationHud.INSTANCE.isOn()) {
            NotificationHud.send(
                    this.getCategory().name(),
                    this.getDisplayName() + " Enabled",
                    NotificationHud.NotificationType.ENABLE
            );
         }

         this.state = true;
         try {
            Forever.EVENT_BUS.subscribe(this);
            this.onToggle();
            this.onEnable();
         } catch (Throwable t) {
            this.state = false;
            try {
               Forever.EVENT_BUS.unsubscribe(this);
            } catch (Throwable ignored) {
            }
            this.handleLifecycleError("enable", t);
         }
      }
   }

   public void disable() {
      if (this.state) {
         // ===== 原有聊天栏提示逻辑（保持不动）=====
         if (!nullCheck() && this.drawn.getValue() && ClientSetting.INSTANCE.toggle.getValue()) {
            int id = ClientSetting.INSTANCE.onlyOne.getValue() ? -1 : this.hashCode();
            String msg = "";
            boolean noSync = false;

            switch ((ClientSetting.Style) ClientSetting.INSTANCE.messageStyle.getValue()) {
               case Mio:      msg = "§4[-] §f" + this.getDisplayName(); break;
               case Debug:    msg = this.getCategory().name().toLowerCase() + "." + this.getDisplayName().toLowerCase() + ".§cdisable"; break;
               case Lowercase:msg = this.getDisplayName().toLowerCase() + " §cdisabled"; break;
               case Melon:    msg = "§b" + this.getDisplayName() + " §cDisabled."; break;
               case Normal:   msg = "§f" + this.getDisplayName() + " §cDisabled"; break;
               case Future:   msg = "§7" + this.getDisplayName() + " toggled §coff"; break;
               case Chinese:  msg = this.getDisplayName() + " §c关闭"; break;
               case Moon:
                  msg = "§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] [§3" + this.getDisplayName() + "§f] §7toggled §coff";
                  noSync = true;
                  break;
               case Earth:
                  msg = "§l" + this.getDisplayName() + " §cdisabled.";
                  noSync = true;
                  break;
               case None:     msg = this.getDisplayName() + " disabled"; break;
            }

            if (!msg.isEmpty()) {
               if (noSync) {
                  CommandManager.sendChatMessageWidthIdNoSync(msg, id);
               } else {
                  CommandManager.sendMessageId(msg, id);
               }
            }
         }

         if (!nullCheck()
                 && NotificationHud.INSTANCE != null
                 && NotificationHud.INSTANCE.isOn()) {
            NotificationHud.send(
                    this.getCategory().name(),
                    this.getDisplayName() + " Disabled",
                    NotificationHud.NotificationType.DISABLE
            );
         }

         this.state = false;
         try {
            Forever.EVENT_BUS.unsubscribe(this);
            this.onToggle();
            this.onDisable();
         } catch (Throwable t) {
            try {
               Forever.EVENT_BUS.unsubscribe(this);
            } catch (Throwable ignored) {
            }
            this.handleLifecycleError("disable", t);
         }
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

   private void handleLifecycleError(String stage, Throwable throwable) {
      System.err.println("[Forever/Module] " + this.getName() + " failed during " + stage + ": " + throwable);
      throwable.printStackTrace();
   }

   @NoRenameObfuscate
   public void onDisable() {
   }

   @NoRenameObfuscate
   public void onEnable() {
   }

   @NoRenameObfuscate
   public void onToggle() {
   }

   @NoRenameObfuscate
   public void onLogin() {
   }

   @NoRenameObfuscate
   public void onLogout() {
   }

   @NoRenameObfuscate
   public void onRender2D(DrawContext drawContext, float tickDelta) {
   }

   @NoRenameObfuscate
   public void onRender3D(MatrixStack matrixStack) {
   }

   public void addSetting(Setting setting) {
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

         pendingUpdateManager.close();
      }
   }

   public static enum Category {
      Combat {
         @Override
         public String getIcon() { return "b"; }
      },
      Misc {
         @Override
         public String getIcon() { return "["; }
      },
      Render {
         @Override
         public String getIcon() { return "a"; }
      },
      Movement {
         @Override
         public String getIcon() { return "8"; }
      },
      Player {
         @Override
         public String getIcon() { return "5"; }
      },
      Exploit {
         @Override
         public String getIcon() { return "6"; }
      },
      Client {
         @Override
         public String getIcon() { return "7"; }
      },
      HUD {
         @Override
         public String getIcon() { return "H"; }
      },
      Mace {
         @Override
         public String getIcon() { return "M"; }
      };

      public abstract String getIcon();
   }
}
