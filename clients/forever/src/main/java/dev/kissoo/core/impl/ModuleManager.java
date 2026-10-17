package dev.kissoo.core.impl;


import dev.kissoo.mod.modules.impl.render.skybox.Skybox;
import hoprc.api.core.impl.FontManager;
import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.Render2DEvent;
import dev.kissoo.api.events.impl.Render3DEvent;
import dev.kissoo.api.utils.Wrapper;
import dev.kissoo.api.utils.math.Easing;
import dev.kissoo.api.utils.path.BaritoneUtil;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.api.utils.render.TextUtil;
import dev.kissoo.mod.Mod;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.client.*;
import dev.kissoo.mod.modules.impl.combat.*;
import dev.kissoo.mod.modules.impl.exploit.*;
import dev.kissoo.mod.modules.impl.hud.*;
import dev.kissoo.mod.modules.impl.misc.*;
import dev.kissoo.mod.modules.impl.movement.*;
import dev.kissoo.mod.modules.impl.player.*;
import dev.kissoo.mod.modules.impl.render.*;
import dev.kissoo.mod.modules.impl.client.AntiCheat;
import dev.kissoo.mod.modules.impl.client.BaritoneModule;
import dev.kissoo.mod.modules.impl.client.ClickGui;
import dev.kissoo.mod.modules.impl.client.ClientSetting;
import dev.kissoo.mod.modules.impl.client.ColorsModule;
import dev.kissoo.mod.modules.impl.client.Fonts;
import dev.kissoo.mod.modules.impl.client.Skin;
import dev.kissoo.mod.modules.impl.combat.Aura;
import dev.kissoo.mod.modules.impl.combat.AuraPlus;
import dev.kissoo.mod.modules.impl.combat.AutoRegear;
import dev.kissoo.mod.modules.impl.combat.CrystalBot;
import dev.kissoo.mod.modules.impl.combat.CrystalBotPlus;
import dev.kissoo.mod.modules.impl.combat.SelfTrap;
import dev.kissoo.mod.modules.impl.exploit.AntiPacket;
import dev.kissoo.mod.modules.impl.exploit.Blink;
import dev.kissoo.mod.modules.impl.exploit.BowBomb;
import dev.kissoo.mod.modules.impl.player.AutoPearl;
import dev.kissoo.mod.modules.impl.hud.ChatPosition;
import dev.kissoo.mod.modules.impl.hud.CircularStatusHud;
import dev.kissoo.mod.modules.impl.hud.IrcChatHUD;
import dev.kissoo.mod.modules.impl.hud.MiniMap;
import dev.kissoo.mod.modules.impl.hud.NotificationHud;
import dev.kissoo.mod.modules.impl.hud.TextRadar;
import dev.kissoo.mod.modules.impl.misc.AutoLog;
import dev.kissoo.mod.modules.impl.misc.AutoReconnect;
import dev.kissoo.mod.modules.impl.misc.ExtraTab;
import dev.kissoo.mod.modules.impl.misc.FakePlayer;
import dev.kissoo.mod.modules.impl.misc.Friend;
import dev.kissoo.mod.modules.impl.misc.IrcModule;
import dev.kissoo.mod.modules.impl.misc.KillEffect;
import dev.kissoo.mod.modules.impl.misc.PearlRun;
import dev.kissoo.mod.modules.impl.misc.Punctuation;
import dev.kissoo.mod.modules.impl.misc.ShulkerViewer;
import dev.kissoo.mod.modules.impl.movement.AutoWalk;
import dev.kissoo.mod.modules.impl.movement.ElytraFlight;
import dev.kissoo.mod.modules.impl.movement.FastWeb;
import dev.kissoo.mod.modules.impl.movement.HoleSnap;
import dev.kissoo.mod.modules.impl.movement.MovementSync;
import dev.kissoo.mod.modules.impl.movement.NoSlow;
import dev.kissoo.mod.modules.impl.movement.Speed;
import dev.kissoo.mod.modules.impl.movement.Sprint;
import dev.kissoo.mod.modules.impl.movement.Velocity;
import dev.kissoo.mod.modules.impl.player.AntiEffects;
import dev.kissoo.mod.modules.impl.player.AutoArmor;
import dev.kissoo.mod.modules.impl.player.FreeLook;
import dev.kissoo.mod.modules.impl.player.Freecam;
import dev.kissoo.mod.modules.impl.player.InteractTweaks;
import dev.kissoo.mod.modules.impl.player.Sorter;
import dev.kissoo.mod.modules.impl.player.SpeedMine;
import dev.kissoo.mod.modules.impl.player.TimerModule;
import dev.kissoo.mod.modules.impl.render.Ambience;
import dev.kissoo.mod.modules.impl.render.BreakESP;
import dev.kissoo.mod.modules.impl.render.Camera;
import dev.kissoo.mod.modules.impl.render.Chams;
import dev.kissoo.mod.modules.impl.render.Crosshair;
import dev.kissoo.mod.modules.impl.render.FrameInterp;
import dev.kissoo.mod.modules.impl.render.HighLight;
import dev.kissoo.mod.modules.impl.movement.FastMoveatAi;
import dev.kissoo.mod.modules.impl.render.HoleESP;
import dev.kissoo.mod.modules.impl.render.ModelModify;
import dev.kissoo.mod.modules.impl.render.NameTags;
import dev.kissoo.mod.modules.impl.render.NoRender;
import dev.kissoo.mod.modules.impl.render.PlaceRender;
import dev.kissoo.mod.modules.impl.render.ShaderModule;
import dev.kissoo.mod.modules.impl.render.ViewModel;
import dev.kissoo.mod.modules.impl.render.Xray;
import dev.kissoo.mod.modules.impl.render.Zoom;

import dev.kissoo.mod.modules.settings.Setting;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class ModuleManager implements Wrapper {
   private final ArrayList<Module> modules = new ArrayList();
   private final ArrayList<ModuleManager.ToggleBanner> toggleBanners = new ArrayList();
   private boolean notificationMouseWasDown = false;
   private boolean interactiveToggleInBanner = false;
   private long lastAuthEnforce = 0L;

   public ArrayList<Module> getModules() {
      return this.modules;
   }

   public ModuleManager() {
      this.init();
   }


   public void init() {
      if (BaritoneUtil.loaded) {
         this.addModule(new BaritoneModule());
      }

      // ── Local-only modules (Mixin-referenced or core-system-referenced) ──
      this.addModule(new Fonts());
      this.addModule(new Ambience());
      this.addModule(new CrystalBot());
      this.addModule(new CrystalBotPlus());
      this.addModule(new AutoWalk());
      this.addModule(new ChatPosition());
      this.addModule(new ExtraTab());
      this.addModule(new Friend());
      this.addModule(new AntiHCQ());
      this.addModule(new ElytraFlight());
      this.addModule(new Aura());
      this.addModule(new AuraPlus());
      this.addModule(new Skybox());
      this.addModule(new SelfTrap());
      this.addModule(new NotificationHud());
      this.addModule(new IrcChatHUD());
      this.addModule(new Blink());
      this.addModule(new Punctuation());
      this.addModule(new Camera());
      this.addModule(new IrcModule());
      this.addModule(new ClickGui());
      this.addModule(new HudEditor());
      this.addModule(new BowSpam());
      this.addModule(new ColorsModule());
      this.addModule(new AutoRegear());
      this.addModule(new AntiCheat());
      this.addModule(new AimPearl());
      this.addModule(new AutoAnchor());
      this.addModule(new AutoWeb());
      this.addModule(new Breaker());
      this.addModule(new Criticals());
      this.addModule(new Crosshair());
      this.addModule(new Chams());
      this.addModule(new AntiPacket());
      this.addModule(new BowBomb());
      this.addModule(new AutoReconnect());
      this.addModule(new HoleESP());
      this.addModule(new MovementSync());
      this.addModule(new NameTags());
      this.addModule(new ShulkerViewer());
      this.addModule(new FakePlayer());
      this.addModule(new HighLight());
      this.addModule(new FastMoveatAi());
      this.addModule(new FastWeb());
      this.addModule(new Freecam());
      this.addModule(new FreeLook());
      this.addModule(new TimerModule());
      this.addModule(new ClientSetting());
      this.addModule(new Skin());
      this.addModule(new KillEffect());
      this.addModule(new ModelModify());
      this.addModule(new AntiEffects());
      this.addModule(new NoRender());
      this.addModule(new NoSlow());
      this.addModule(new Xray());
      this.addModule(new SpeedMine());
      this.addModule(new Phase());
      this.addModule(new PlaceRender());
      this.addModule(new AutoPearl());
      this.addModule(new InteractTweaks());
      this.addModule(new ShaderModule());
      this.addModule(new FrameInterp());
      this.addModule(new Speed());
      this.addModule(new Sprint());
      this.addModule(new Velocity());
      this.addModule(new ViewModel());
      this.addModule(new Zoom());
      this.addModule(new CircularStatusHud());
      // hud
      this.addModule(new MiniMap());
      this.addModule(new TextRadar());
      // misc
      this.addModule(new AutoLog());
      this.addModule(new PearlRun());
      this.addModule(new AntiMonkey());
      // movement
      this.addModule(new HoleSnap());
      // player
      this.addModule(new AutoArmor());
      this.addModule(new Sorter());
      // render
      this.addModule(new BreakESP());

      // Restored local modules
      this.addModule(new CNInputFix());
      this.addModule(new ItemsCounter());
      this.addModule(new MusicPlayer());
      this.addModule(new AntiRegear());
      this.addModule(new AutoArrow());
      this.addModule(new AutoMend());
      this.addModule(new AutoPush());
      this.addModule(new Follower());
      this.addModule(new CrossbowAim());
      this.addModule(new Burrow());
      this.addModule(new HoleFiller());
      this.addModule(new Offhand());
      this.addModule(new PearlBlocker());
      this.addModule(new PistonCrystal());
      this.addModule(new Quiver());
      this.addModule(new Surround());
      this.addModule(new AntiHunger());
      this.addModule(new ChunkESP());
      this.addModule(new AutoTrap());
      this.addModule(new Clip());
      this.addModule(new InfiniteTrident());
      this.addModule(new MaceSpoof());
      this.addModule(new MaceSpoofPlus());
      this.addModule(new MaceAura());
      this.addModule(new PingSpoof());
      this.addModule(new SkinFlicker());
      this.addModule(new TeleportLogger());
      this.addModule(new ThirtyTwoKMace());
      this.addModule(new XCarry());
      this.addModule(new ArmorHud());
      this.addModule(new ArrayListHud());
      this.addModule(new BackpackContents());
      this.addModule(new CoordsHud());
      this.addModule(new DFHealth());
      this.addModule(new DFHungerBar());
      this.addModule(new DFTitle());
      this.addModule(new DFWeapon());
      this.addModule(new DirectionHud());
      this.addModule(new FPSHud());
      this.addModule(new GifHud());
      this.addModule(new NoirLive2D());
      this.addModule(new PacketHud());
      this.addModule(new PingHud());
      this.addModule(new PlayerModel());
      this.addModule(new PotionHud());
      this.addModule(new ServerInfoHud());
      this.addModule(new SpeedHud());
      this.addModule(new TPSHud());
      this.addModule(new TimeHud());
      this.addModule(new Totem());
      this.addModule(new WaterMarkHud());
      this.addModule(new WelcomerHud());
      this.addModule(new AutoEZ());
      this.addModule(new AutoExplain());
      this.addModule(new BetterHurt());
      this.addModule(new Bot());
      this.addModule(new ChatAppend());
      this.addModule(new NoSound());
      this.addModule(new Nuker());
      this.addModule(new PacketLogger());
      this.addModule(new Spammer());
      this.addModule(new PlayerTell());
      this.addModule(new Tips());
      this.addModule(new AntiVoid());
      this.addModule(new BlockStrafe());
      this.addModule(new BoatFly());
      this.addModule(new FastFall());
      this.addModule(new Flatten());
      this.addModule(new Fly());
      this.addModule(new WindTP());
      this.addModule(new LieDownMove());
      this.addModule(new NoJumpDelay());
      this.addModule(new Scaffold());
      this.addModule(new Step());
      this.addModule(new Strafe());
      this.addModule(new VClip());
      this.addModule(new AntiBaseKill());
      this.addModule(new AutoKouZi());
      this.addModule(new AutoPot());
      this.addModule(new Driller());
      this.addModule(new NoFall());
      this.addModule(new PacketEat());
      this.addModule(new Replenish());
      this.addModule(new Yaw());
      this.addModule(new ESP());
      this.addModule(new LogoutSpots());
      this.addModule(new PhaseESP());
      this.addModule(new PlayerTrail());
      this.addModule(new PopChams());
      this.addModule(new TotemParticle());
      this.addModule(new Tracers());
      this.addModule(new Trajectories());
      // ── Spear-backed modules (from decompiled additions) ──
      this.addModule(new SpearAura());
      this.addModule(new PistonCrystalPlus());
      this.addModule(new MaceGodModule());
      this.addModule(new MaceGodPlus());
      this.addModule(new TimerMace());
      this.addModule(new BurstStrafe());
      this.addModule(new SuperLongJump());
      this.addModule(new StrafeSpeed());
      this.addModule(new WindLongJump());
      this.addModule(new FlatMace());
      this.addModule(new AntiCobweb());
      this.addModule(new MissAlert());
      this.addModule(new ItemAlert());
      this.addModule(new Notebot());
      this.modules.sort(Comparator.comparing(Mod::getName));
   }

   public void onKeyReleased(int eventKey) {
      if (eventKey != -1 && eventKey != 0) {
         this.handleKeyEvent(eventKey, false);
      }
   }

   public void onKeyPressed(int eventKey) {
      if (eventKey != -1 && eventKey != 0) {
         this.handleKeyEvent(eventKey, true);
      }
   }

   private void handleKeyEvent(int key, boolean isPressed) {
      for (Module module : this.modules) {
         BindSetting bindSetting = module.getBindSetting();
         if (bindSetting.getValue() == key) {
            if (isPressed && mc.currentScreen == null) {
               module.toggle();
               bindSetting.holding = true;
            } else if (!isPressed && bindSetting.isHoldEnable() && bindSetting.holding) {
               module.toggle();
               bindSetting.holding = false;
            }
         }

         for (Setting setting : module.getSettings()) {
            if (setting instanceof BindSetting bind && bind.getValue() == key) {
               bind.setPressed(isPressed);
            }
         }
      }
   }

   public void onLogin() {
      for (Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.onLogin();
            } catch (Throwable t) {
               this.logModuleError(module, "onLogin", t);
            }
         }
      }
   }

   public void onLogout() {
      for (Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.onLogout();
            } catch (Throwable t) {
               this.logModuleError(module, "onLogout", t);
            }
         }
      }
   }

   private void enforceAuthGate() {
      long now = System.currentTimeMillis();
      if (now - this.lastAuthEnforce < 1000L) {
         return;
      }

      this.lastAuthEnforce = now;
      if (dev.kissoo.auth.AuthGate.allowModuleActivation()) {
         return;
      }

      // 授权失效（token 被吊销/过期）：禁用仍在运行的模块，落实「启动成功≠永久授权」。
      for (Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.disable();
            } catch (Throwable ignored) {
            }
         }
      }
   }

   public void onRender2D(DrawContext drawContext) {
      this.enforceAuthGate();
      for (Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.onRender2D(drawContext, mc.getRenderTickCounter().getTickDelta(true));
            } catch (Throwable var6) {
               var6.printStackTrace();
               if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender2D]) Message: [" + var6.getMessage() + "]");
               }
            }
         }
      }

      try {
         Forever.EVENT_BUS.post(Render2DEvent.get(drawContext, mc.getRenderTickCounter().getTickDelta(true)));
      } catch (Throwable var5) {
         var5.printStackTrace();
         if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + var5.getMessage() + "]");
         }
      }

      this.renderToggleBanners(drawContext);
   }

   public void render3D(MatrixStack matrices) {
      GL11.glEnable(2848);

      for (Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.onRender3D(matrices);
            } catch (Throwable var6) {
               var6.printStackTrace();
               if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender3D]) Message: [" + var6.getMessage() + "]");
               }
            }
         }
      }

      try {
         Forever.EVENT_BUS.post(Render3DEvent.get(matrices, mc.getRenderTickCounter().getTickDelta(true)));
      } catch (Throwable var5) {
         var5.printStackTrace();
         if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + var5.getMessage() + "]");
         }
      }

      GL11.glDisable(2848);
   }

   private void logModuleError(Module module, String stage, Throwable throwable) {
      throwable.printStackTrace();
      if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
         CommandManager.sendMessage("搂4An error has occurred (" + module.getName() + " [" + stage + "]) Message: [" + throwable.getMessage() + "]");
      }
   }

   private void renderToggleBanners(DrawContext ctx) {
      if (!this.toggleBanners.isEmpty() && mc != null) {
         if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.banner.getValue()) {
            long now = System.currentTimeMillis();
            int sw = mc.getWindow().getScaledWidth();
            int sh = mc.getWindow().getScaledHeight();
            int rawW = mc.getWindow().getWidth();
            int rawH = mc.getWindow().getHeight();
            int mx = (int)(mc.mouse.getX() * sw / Math.max(1, rawW));
            int my = (int)(mc.mouse.getY() * sh / Math.max(1, rawH));
            boolean mouseDown = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1;
            ArrayList<ModuleManager.ToggleBanner> removeList = new ArrayList();
            long fi = (long)ClientSetting.INSTANCE.bannerFade.getValue();
            long hold = (long)ClientSetting.INSTANCE.bannerHold.getValue();
            if (ClientSetting.INSTANCE.onlyOne.getValue() && ClientSetting.INSTANCE.bannerStyle.getValue() == ClientSetting.BannerStyle.iOS) {
               long start = ((ModuleManager.ToggleBanner)this.toggleBanners.get(0)).start;
               long elapsed = now - start;
               long total = fi + hold + fi;
               if (elapsed > total) {
                  removeList.add((ModuleManager.ToggleBanner)this.toggleBanners.get(0));
               }

               double alpha;
               double slide;
               if (elapsed < fi) {
                  double p = Math.max(0.0, Math.min(1.0, (double)elapsed / fi));
                  double e = Easing.SineOut.ease(p);
                  alpha = e;
                  slide = 10.0 * e;
               } else if (elapsed < fi + hold) {
                  alpha = 1.0;
                  slide = 10.0;
               } else {
                  double p = Math.max(0.0, Math.min(1.0, (double)(elapsed - fi - hold) / fi));
                  double e = Easing.SineInOut.ease(p);
                  alpha = 1.0 - e;
                  slide = 10.0 - 10.0 * e;
               }

               int a = (int)Math.max(0L, Math.min(255L, Math.round(alpha * 255.0)));
               int maxRows = Math.min(3, this.toggleBanners.size());
               int rowH = 14;
               int pad = 10;
               int height = rowH * maxRows + pad * 2;
               int width = Math.max(180, (int)(sw * 0.36));
               int x = 14;
               int y = (int)slide;
               Color bg = new Color(255, 255, 255, (int)Math.round(a * 0.85));
               float radius = height / 2.0F;
               Forever.BLUR.applyBlur(30.0F, x, y, width, height);
               Render2DUtil.drawPill(ctx.getMatrices(), x, y, width, height, bg);
               Render2DUtil.drawRoundedStroke(ctx.getMatrices(), x, y, width, height, radius, new Color(220, 224, 230, Math.min(180, a)), 48);
               Render2DUtil.drawGlow(ctx.getMatrices(), x - 3.0F, y - 3.0F, width + 6.0F, height + 6.0F, new Color(255, 255, 255, Math.min(54, a)).getRGB());
               Render2DUtil.drawLine(ctx.getMatrices(), x + radius, y + 2.0F, x + width - radius, y + 2.0F, new Color(255, 255, 255, Math.min(90, a)).getRGB());
               Render2DUtil.drawLine(
                       ctx.getMatrices(), x + radius, y + height - 2.2F, x + width - radius, y + height - 2.2F, new Color(120, 130, 140, Math.min(70, a)).getRGB()
               );
               int gx = x + pad;
               int gy = y + pad;

               for (int i = 0; i < maxRows; i++) {
                  ModuleManager.ToggleBanner b = (ModuleManager.ToggleBanner)this.toggleBanners.get(this.toggleBanners.size() - 1 - i);
                  Module modCur = this.getModuleByDisplayName(b.name);
                  boolean curEnabled = modCur != null && modCur.isOn();
                  String text = (curEnabled ? "已开启 " : "已关闭 ") + b.name;
                  Color textColor = new Color(30, 30, 30, a);
                  int tw = ClickGui.getInstance().font.getValue() ? (int)FontManager.ui.getWidth(text) : (int)TextUtil.getWidth(text);
                  int tx = x + (width - tw) / 2;
                  double ty = gy + i * rowH;
                  TextUtil.drawString(ctx, text, tx, ty, textColor.getRGB(), ClickGui.getInstance().font.getValue());
                  int toggleW = 28;
                  int toggleH = 14;
                  float swx = x + width - pad - toggleW;
                  float swy = y + pad + i * rowH + (rowH - toggleH) / 2.0F;
                  Color on = new Color(0, 120, 212, a);
                  Color track = curEnabled ? new Color(on.getRed(), on.getGreen(), on.getBlue(), Math.min(180, a)) : new Color(238, 238, 238, Math.min(200, a));
                  Render2DUtil.drawRoundedRect(ctx.getMatrices(), swx, swy, toggleW, toggleH, 6.0F, track);
                  float dotR = toggleH / 2.0F - 2.0F;
                  float dotCx = curEnabled ? swx + toggleW - toggleH / 2.0F : swx + toggleH / 2.0F;
                  float dotCy = swy + toggleH / 2.0F;
                  Render2DUtil.drawCircle(ctx.getMatrices(), dotCx, dotCy, dotR, new Color(255, 255, 255, Math.min(230, a)), 80);
                  if (mx >= swx && mx <= swx + toggleW && my >= swy && my <= swy + toggleH && mouseDown && !this.notificationMouseWasDown && modCur != null) {
                     this.interactiveToggleInBanner = true;
                     modCur.toggle();
                  }
               }

               this.notificationMouseWasDown = mouseDown;
            } else {
               int baseY = 8;
               int height = 16;
               int gap = 4;
               int index = 0;

               for (ModuleManager.ToggleBanner b : this.toggleBanners) {
                  long elapsedx = now - b.start;
                  long totalx = fi + hold + fi;
                  if (elapsedx > totalx) {
                     removeList.add(b);
                  } else {
                     double alpha;
                     double slide;
                     if (elapsedx < fi) {
                        double p = Math.max(0.0, Math.min(1.0, (double)elapsedx / fi));
                        double e = Easing.SineOut.ease(p);
                        alpha = e;
                        slide = -height - 8 + 8.0 * e;
                     } else if (elapsedx < fi + hold) {
                        alpha = 1.0;
                        slide = 8.0;
                     } else {
                        double p = Math.max(0.0, Math.min(1.0, (double)(elapsedx - fi - hold) / fi));
                        double e = Easing.SineInOut.ease(p);
                        alpha = 1.0 - e;
                        slide = 8.0 + (-height - 8) * e;
                     }

                     int pad = 10;
                     Module modCur = this.getModuleByDisplayName(b.name);
                     boolean curEnabled = modCur != null && modCur.isOn();
                     String text = (curEnabled ? "已开启 " : "已关闭 ") + b.name;
                     int tw = ClickGui.getInstance().font.getValue() ? (int)FontManager.ui.getWidth(text) : (int)TextUtil.getWidth(text);
                     int width = Math.max(tw + pad * 2 + 8 + 30, 160);
                     int margin = 14;
                     int ay = (int)(
                             margin
                                     + (
                                     ClientSetting.INSTANCE.bannerStack.getValue() == ClientSetting.StackDir.Down
                                             ? slide + index * (height + gap)
                                             : slide - index * (height + gap)
                             )
                     );
                     int a = (int)Math.max(0L, Math.min(255L, Math.round(alpha * 255.0)));
                     int bgAlpha = (int)Math.round(a * 0.32);
                     Color bg = new Color(255, 255, 255, Math.min(220, a));
                     Color on = new Color(0, 120, 212, a);
                     new Color(204, 204, 204, a);
                     float radius = 3.5F;
                     Forever.BLUR.applyBlur(30.0F, margin, ay, width, height);
                     Render2DUtil.drawRoundedRect(ctx.getMatrices(), margin, ay, width, height, radius, bg);
                     Render2DUtil.drawRoundedStroke(ctx.getMatrices(), margin, ay, width, height, radius, new Color(220, 224, 230, Math.min(180, a)), 48);
                     Render2DUtil.drawGlow(
                             ctx.getMatrices(), margin - 3.0F, ay - 3.0F, width + 6.0F, height + 6.0F, new Color(255, 255, 255, Math.min(54, a)).getRGB()
                     );
                     Render2DUtil.drawLine(
                             ctx.getMatrices(), margin + 2.0F, ay + 2.0F, margin + width - 2.0F, ay + 2.0F, new Color(255, 255, 255, Math.min(90, a)).getRGB()
                     );
                     Render2DUtil.drawLine(
                             ctx.getMatrices(),
                             margin + 2.0F,
                             ay + height - 2.2F,
                             margin + width - 2.0F,
                             ay + height - 2.2F,
                             new Color(120, 130, 140, Math.min(70, a)).getRGB()
                     );
                     int toggleW = 28;
                     int toggleH = 14;
                     float tx = margin + width - pad - toggleW;
                     float ty = ay + (height - toggleH) / 2.0F;
                     Color track = curEnabled
                             ? new Color(on.getRed(), on.getGreen(), on.getBlue(), Math.min(180, a))
                             : new Color(238, 238, 238, Math.min(200, a));
                     Render2DUtil.drawRoundedRect(ctx.getMatrices(), tx, ty, toggleW, toggleH, 6.0F, track);
                     float dotR = toggleH / 2.0F - 2.0F;
                     float dotCx = curEnabled ? tx + toggleW - toggleH / 2.0F : tx + toggleH / 2.0F;
                     float dotCy = ty + toggleH / 2.0F;
                     Render2DUtil.drawCircle(ctx.getMatrices(), dotCx, dotCy, dotR, new Color(255, 255, 255, Math.min(230, a)), 80);
                     double textY = ay + (height - (ClickGui.getInstance().font.getValue() ? FontManager.ui.getFontHeight() : TextUtil.getHeight())) / 2.0;
                     int textAreaW = width - pad * 2 - toggleW;
                     int textX = margin + pad + (textAreaW - tw) / 2;
                     Color textColor = new Color(30, 30, 30, a);
                     TextUtil.drawString(ctx, text, textX, textY, textColor.getRGB(), ClickGui.getInstance().font.getValue());
                     if (mx >= tx && mx <= tx + toggleW && my >= ty && my <= ty + toggleH && mouseDown && !this.notificationMouseWasDown && modCur != null) {
                        this.interactiveToggleInBanner = true;
                        modCur.toggle();
                     }

                     index++;
                  }
               }
            }

            if (!removeList.isEmpty()) {
               this.toggleBanners.removeAll(removeList);
            }

            this.notificationMouseWasDown = mouseDown;
         }
      }
   }

   private void renderPotionListLegacy(DrawContext ctx) {
      if (mc.player != null) {
         int margin = 14;
         int startX = margin + 2;
         int startY = margin + 92;
         int pillH = 14;
         int pillPad = 6;
         int idx = 0;

         for (StatusEffectInstance se : mc.player.getStatusEffects()) {
            String name = ((StatusEffect)se.getEffectType().value()).getName().getString();
            int ticks = se.getDuration();
            int totalSec = Math.max(0, ticks / 20);
            int mm = totalSec / 60;
            int ss = totalSec % 60;
            String time = String.format("%d:%02d", mm, ss);
            String text = name + " " + time;
            int tw = ClickGui.getInstance().font.getValue() ? (int)FontManager.ui.getWidth(text) : (int)TextUtil.getWidth(text);
            int pillW = tw + pillPad * 2;
            int y = startY + idx * (pillH + 4);
            int a = 180;
            Render2DUtil.drawRoundedRect(ctx.getMatrices(), startX, y, pillW, pillH, 4.0F, new Color(255, 255, 255, a));
            Render2DUtil.drawRoundedStroke(ctx.getMatrices(), startX, y, pillW, pillH, 4.0F, new Color(220, 224, 230, 160), 48);
            int tx = startX + pillPad;
            double ty = y + (pillH - (ClickGui.getInstance().font.getValue() ? FontManager.ui.getFontHeight() : TextUtil.getHeight())) / 2.0;
            TextUtil.drawString(ctx, text, tx, ty, new Color(30, 30, 30).getRGB(), ClickGui.getInstance().font.getValue());
            if (++idx >= 5) {
               break;
            }
         }
      }
   }

   private void renderPotionBanners(DrawContext ctx) {
   }

   private String formatDuration(int ticks) {
      int totalSec = Math.max(0, ticks / 20);
      int mm = totalSec / 60;
      int ss = totalSec % 60;
      return String.format("%d:%02d", mm, ss);
   }

   private Module getModuleByDisplayName(String name) {
      for (Module m : this.modules) {
         if (m.getDisplayName().equalsIgnoreCase(name) || m.getName().equalsIgnoreCase(name)) {
            return m;
         }
      }

      return null;
   }

   public void addModule(Module module) {
      this.modules.add(module);
   }

   public Module getModuleByName(String string) {
      for (Module module : this.modules) {
         if (module.getName().equalsIgnoreCase(string)) {
            return module;
         }
      }

      return null;
   }

   private static class ToggleBanner {
      public final String name;
      public final boolean enabled;
      public final long start;

      public ToggleBanner(String name, boolean enabled, long start) {
         this.name = name;
         this.enabled = enabled;
         this.start = start;
      }
   }
}
