package dev.baimaodaxia.mod.modules.impl.client;

import dev.baimaodaxia.Alien;
import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.PacketEvent;
import dev.baimaodaxia.api.events.impl.TotemEvent;
import dev.baimaodaxia.core.impl.FontManager;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.ColorSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Formatting;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;

public class TextRadar extends Module {
   public static TextRadar INSTANCE;
   public static final String TURTLE_COLOR;
   public static final int TURTLE_COLOR_RGB;
   private final DecimalFormat df = new DecimalFormat("0.0");
   private final EnumSetting<SortMode> sortMode;
   private final BooleanSetting font;
   private final BooleanSetting shadow;
   private final SliderSetting x;
   private final SliderSetting y;
   private final ColorSetting color;
   private final ColorSetting friend;
   private final BooleanSetting doubleBlank;
   private final BooleanSetting health;
   private final BooleanSetting pops;
   public final BooleanSetting red;
   private final BooleanSetting getDistance;
   private final BooleanSetting effects;
   private final BooleanSetting strength;
   private final BooleanSetting turtle;
   private final BooleanSetting regen;
   private final BooleanSetting fireRes;
   private final BooleanSetting absorption;
   private final BooleanSetting potionTotem;
   private final BooleanSetting potionGapple;
   private final BooleanSetting potionIcons;
   private final BooleanSetting potionLevels;
   private final BooleanSetting potionDecimal;
   private final EnumSetting<PotionTimeStyle> potionTimeStyle;
   private final SliderSetting potionIconGap;
   private final SliderSetting potionBlankGap;
   private final SliderSetting potionTextGap;
   private final BooleanSetting potionModernDistance;
   private final Map<Integer, EffectTimer> strengthTimers;
   private final Map<Integer, EffectTimer> turtleTimers;
   private final Map<Integer, EffectTimer> regenTimers;
   private final Map<Integer, EffectTimer> fireResTimers;
   private final Map<Integer, EffectTimer> absorptionTimers;
   private final Map<Integer, String> lastEffects;

   public TextRadar() {
      super("TextRadar", Module.Category.Client);
      this.sortMode = this.add(new EnumSetting("SortMode", TextRadar.SortMode.Distance));
      this.font = this.add(new BooleanSetting("Font", true));
      this.shadow = this.add(new BooleanSetting("Shadow", true));
      this.x = this.add(new SliderSetting("X", 0, 0, 1500));
      this.y = this.add(new SliderSetting("Y", 100, 0, 1000));
      this.color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
      this.friend = this.add((new ColorSetting("Friend")).injectBoolean(true));
      this.doubleBlank = this.add(new BooleanSetting("Double", false));
      this.health = this.add(new BooleanSetting("Health", true));
      this.pops = this.add(new BooleanSetting("Pops", true));
      this.red = this.add(new BooleanSetting("Red", false));
      this.getDistance = this.add(new BooleanSetting("Distance", true));
      this.effects = this.add((new BooleanSetting("Effects", true)).setParent());
      BooleanSetting var10006 = this.effects;
      Objects.requireNonNull(var10006);
      this.strength = this.add(new BooleanSetting("Potion_Strength", true, var10006::isOpen));
      var10006 = this.effects;
      Objects.requireNonNull(var10006);
      this.turtle = this.add(new BooleanSetting("Potion_Turtle", true, var10006::isOpen));
      var10006 = this.effects;
      Objects.requireNonNull(var10006);
      this.regen = this.add(new BooleanSetting("Potion_Regen", true, var10006::isOpen));
      var10006 = this.effects;
      Objects.requireNonNull(var10006);
      this.fireRes = this.add(new BooleanSetting("Potion_FireRes", true, var10006::isOpen));
      var10006 = this.effects;
      Objects.requireNonNull(var10006);
      this.absorption = this.add(new BooleanSetting("Potion_Absorption", true, var10006::isOpen));
      var10006 = this.effects;
      Objects.requireNonNull(var10006);
      this.potionTotem = this.add(new BooleanSetting("Potion_Totem", true, var10006::isOpen));
      var10006 = this.effects;
      Objects.requireNonNull(var10006);
      this.potionGapple = this.add(new BooleanSetting("Potion_Gapple", true, var10006::isOpen));
      var10006 = this.effects;
      Objects.requireNonNull(var10006);
      this.potionIcons = this.add(new BooleanSetting("PotionIcons", true, var10006::isOpen));
      var10006 = this.effects;
      Objects.requireNonNull(var10006);
      this.potionLevels = this.add(new BooleanSetting("PotionLevels", true, var10006::isOpen));
      var10006 = this.effects;
      Objects.requireNonNull(var10006);
      this.potionDecimal = this.add(new BooleanSetting("PotionDecimal", false, var10006::isOpen));
      PotionTimeStyle var10005 = TextRadar.PotionTimeStyle.Mine;
      var10006 = this.effects;
      Objects.requireNonNull(var10006);
      this.potionTimeStyle = this.add(new EnumSetting("PotionStyle", var10005, var10006::isOpen));
      this.potionIconGap = this.add(new SliderSetting("PotionIconGap", (double)2.0F, (double)0.0F, (double)16.0F, (double)0.5F, () -> this.effects.isOpen() && this.potionIcons.getValue() && this.potionLevels.getValue()));
      this.potionBlankGap = this.add(new SliderSetting("PotionBlankGap", (double)25.0F, (double)8.0F, (double)50.0F, (double)0.5F, () -> this.effects.isOpen() && this.potionIcons.getValue()));
      this.potionTextGap = this.add(new SliderSetting("PotionTextGap", (double)4.0F, (double)0.0F, (double)16.0F, (double)0.5F, () -> this.effects.isOpen() && this.potionIcons.getValue()));
      var10006 = this.effects;
      Objects.requireNonNull(var10006);
      this.potionModernDistance = this.add(new BooleanSetting("1.21.6+", false, var10006::isOpen));
      this.strengthTimers = new ConcurrentHashMap();
      this.turtleTimers = new ConcurrentHashMap();
      this.regenTimers = new ConcurrentHashMap();
      this.fireResTimers = new ConcurrentHashMap();
      this.absorptionTimers = new ConcurrentHashMap();
      this.lastEffects = new ConcurrentHashMap();
      this.setChinese("文字雷达");
      INSTANCE = this;
   }

   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (mc.player != null && mc.world != null) {
         ArrayList<AbstractClientPlayerEntity> players = new ArrayList(mc.world.getPlayers());
         switch (((SortMode)this.sortMode.getValue()).ordinal()) {
            case 0 -> players.sort(Comparator.comparingDouble((player) -> (double)mc.player.distanceTo(player)));
            case 1 -> players.sort(Comparator.comparingDouble((player) -> (double)(player.getHealth() + player.getAbsorptionAmount())));
         }

         ArrayList<RadarRow> radarRows = new ArrayList();
         int maxWidth = 0;
         int totalHeight = 0;
         long now = System.currentTimeMillis();

         for(PlayerEntity playerEntity : players) {
            if (playerEntity != mc.player) {
               this.syncPotionState(playerEntity, now);
               if (this.turtle.getValue() && this.hasTurtleEffect(playerEntity)) {
                  boolean var23 = true;
               } else {
                  boolean var10000 = false;
               }

               String blank = this.doubleBlank.getValue() ? "  " : " ";
               StringBuilder sb = new StringBuilder();
               if (this.health.getValue()) {
                  sb.append(getHealthColor(playerEntity));
                  sb.append(this.df.format((double)(playerEntity.getHealth() + playerEntity.getAbsorptionAmount())));
                  sb.append(blank);
               }

               sb.append(Formatting.RESET.toString());
               sb.append(playerEntity.getName().getString());
               if (this.getDistance.getValue()) {
                  sb.append(blank);
                  sb.append(Formatting.WHITE);
                  sb.append(this.df.format((double)mc.player.distanceTo(playerEntity)));
                  sb.append("m");
               }

               if (this.effects.getValue()) {
                  sb.append(this.getPotionDisplayString(playerEntity, blank, this.strength.getValue(), this.turtle.getValue(), this.regen.getValue(), this.fireRes.getValue(), this.absorption.getValue(), this.potionIcons.getValue(), this.potionLevels.getValue(), this.potionBlankGap.getValueFloat(), this.potionTextGap.getValueFloat()));
               }

               if (this.pops.getValue()) {
                  int totemPopped = Alien.POP.getPop(playerEntity);
                  if (totemPopped > 0) {
                     sb.append(blank);
                     sb.append(getPopColor(totemPopped));
                     sb.append("-");
                     sb.append(totemPopped);
                  }
               }

               boolean isFriend = Alien.FRIEND.isFriend(playerEntity);
               if (!isFriend || this.friend.booleanValue) {
                  int textColor;
                  if (isFriend && this.friend.booleanValue) {
                     textColor = this.friend.getValue().getRGB();
                  } else {
                     textColor = this.color.getValue().getRGB();
                  }

                  String text = sb.toString();
                  int textWidth = this.getTextWidth(text);
                  int lineHeight = this.font.getValue() ? (int)FontManager.ui.getFontHeight() : 9;
                  radarRows.add(new RadarRow(playerEntity, text, textColor, textWidth, lineHeight));
                  maxWidth = Math.max(maxWidth, textWidth);
                  totalHeight += lineHeight;
               }
            }
         }

         if (!radarRows.isEmpty()) {
            int currentX = this.x.getValueInt();
            int currentY = this.y.getValueInt();

            for(RadarRow row : radarRows) {
               if (this.font.getValue()) {
                  FontManager.ui.drawString(drawContext.getMatrices(), row.text, (double)currentX, (double)currentY, row.color, this.shadow.getValue());
               } else {
                  drawContext.drawText(mc.textRenderer, row.text, currentX, currentY, row.color, this.shadow.getValue());
               }

               currentY += row.lineHeight;
            }

         }
      }
   }

   public void syncPotionState(PlayerEntity player, long now) {
      if (player != null) {
         int playerId = player.getId();
         if (player.isDead()) {
            this.clearPotionState(playerId);
         } else {
            this.pruneExpired(now);
            if (this.potionGapple.getValue()) {
               this.detectGapple(player, playerId, now);
            }
         }

      }
   }

   public boolean hasTurtleEffect(PlayerEntity player) {
      return player == null ? false : this.hasEffect(player, StatusEffects.RESISTANCE, this.turtleTimers, player.getId(), System.currentTimeMillis(), this.turtle.getValue());
   }

   private boolean hasEffect(PlayerEntity player, RegistryEntry<StatusEffect> effect, Map<Integer, EffectTimer> timers, int playerId, long now, boolean enabled) {
      if (!enabled) {
         return false;
      } else if (player == mc.player && mc.player != null) {
         StatusEffectInstance instance = mc.player.getStatusEffect(effect);
         return instance != null && instance.getDuration() > 0;
      } else {
         EffectTimer timer = (EffectTimer)timers.get(playerId);
         if (timer != null && timer.expiry > now) {
            return true;
         } else {
            if (timer != null) {
               timers.remove(playerId);
            }

            return false;
         }
      }
   }

   private ArrayList<PotionDisplay> getPotionDisplays(PlayerEntity player, boolean showStrength, boolean showTurtle, boolean showRegen, boolean showFireRes, boolean showAbsorption, boolean showIcons, boolean showLevels, float blankGap, float textGap) {
      ArrayList<PotionDisplay> displays = new ArrayList();
      if (player == null) {
         return displays;
      } else {
         this.syncPotionState(player, System.currentTimeMillis());
         this.addPotionDisplay(displays, player, StatusEffects.STRENGTH, this.strengthTimers, showStrength, Formatting.YELLOW.toString(), Formatting.YELLOW.getColorValue(), "strength", "Strength", TextRadar.EffectLimit.STRENGTH, showIcons, showLevels, blankGap, textGap);
         this.addPotionDisplay(displays, player, StatusEffects.RESISTANCE, this.turtleTimers, showTurtle, TURTLE_COLOR, TURTLE_COLOR_RGB, "resistance", "Resistance", TextRadar.EffectLimit.RESISTANCE, showIcons, showLevels, blankGap, textGap);
         this.addPotionDisplay(displays, player, StatusEffects.REGENERATION, this.regenTimers, showRegen, Formatting.LIGHT_PURPLE.toString(), Formatting.LIGHT_PURPLE.getColorValue(), "regeneration", "Regen", TextRadar.EffectLimit.NONE, showIcons, showLevels, blankGap, textGap);
         this.addPotionDisplay(displays, player, StatusEffects.FIRE_RESISTANCE, this.fireResTimers, showFireRes, Formatting.GOLD.toString(), Formatting.GOLD.getColorValue(), "fire_resistance", "FireRes", TextRadar.EffectLimit.NONE, showIcons, showLevels, blankGap, textGap);
         this.addPotionDisplay(displays, player, StatusEffects.ABSORPTION, this.absorptionTimers, showAbsorption, Formatting.YELLOW.toString(), Formatting.YELLOW.getColorValue(), "absorption", "Absorption", TextRadar.EffectLimit.NONE, showIcons, showLevels, blankGap, textGap);
         return displays;
      }
   }

   private void addPotionDisplay(ArrayList<PotionDisplay> displays, PlayerEntity player, RegistryEntry<StatusEffect> effect, Map<Integer, EffectTimer> timers, boolean enabled, String colorCode, int color, String effectId, String effectName, EffectLimit limit, boolean showIcons, boolean showLevels, float blankGap, float textGap) {
      if (enabled) {
         EffectSnapshot snapshot = this.getEffectSnapshot(player, effect, timers, limit);
         if (snapshot != null && !(snapshot.seconds <= (double)0.0F)) {
            String level = showLevels ? toRoman(snapshot.amplifier + 1) : "";
            String text = " " + colorCode + effectName + (level.isEmpty() ? "" : " " + level) + " " + this.formatPotionTime(snapshot.seconds);
            displays.add(new PotionDisplay(text, level, color, effectId, effectName));
         }
      }
   }

   private EffectSnapshot getEffectSnapshot(PlayerEntity player, RegistryEntry<StatusEffect> effect, Map<Integer, EffectTimer> timers, EffectLimit limit) {
      if (player == mc.player && mc.player != null) {
         StatusEffectInstance instance = mc.player.getStatusEffect(effect);
         if (instance != null && instance.getDuration() > 0) {
            double seconds = Math.min((double)instance.getDuration() / (double)20.0F, (double)maxEffectSeconds(limit, instance.getAmplifier()));
            return new EffectSnapshot(Math.max((double)1.0F, seconds), instance.getAmplifier());
         } else {
            return null;
         }
      } else {
         long now = System.currentTimeMillis();
         EffectTimer timer = (EffectTimer)timers.get(player.getId());
         if (timer == null) {
            return null;
         } else if (timer.expiry <= now) {
            timers.remove(player.getId());
            return null;
         } else {
            double seconds = Math.min((double)(timer.expiry - now) / (double)1000.0F, (double)maxEffectSeconds(limit, timer.amplifier));
            return new EffectSnapshot(Math.max((double)1.0F, seconds), timer.amplifier);
         }
      }
   }

   private String potionTextPrefix(boolean showIcons, float blankGap, float textGap) {
      if (!showIcons) {
         return " ";
      } else {
         int spaces = Math.max(1, Math.round((blankGap + textGap) / 3.5F));
         return " ".repeat(spaces);
      }
   }

   public String getPotionDisplayString(PlayerEntity player, String separator, boolean showStrength, boolean showTurtle, boolean showRegen, boolean showFireRes, boolean showAbsorption, boolean showIcons, boolean showLevels, float blankGap, float textGap) {
      StringBuilder sb = new StringBuilder();

      for(PotionDisplay display : this.getPotionDisplays(player, showStrength, showTurtle, showRegen, showFireRes, showAbsorption, showIcons, showLevels, blankGap, textGap)) {
         String text = display.text;
         if (!showIcons && text.startsWith(" ")) {
            text = separator + text.substring(1);
         }

         sb.append(text);
      }

      return sb.toString();
   }

   private void drawPotionIcons(DrawContext context, PlayerEntity player, String renderedText, float textX, float textY, boolean fancyFont) {
      float fontHeight = fancyFont && FontManager.ui != null ? FontManager.ui.getFontHeight() : 9.0F;
      float iconSize = 10.0F;
      float iconY = textY + (fontHeight - iconSize) / 2.0F;

      for(PotionDisplay display : this.getPotionDisplays(player, this.strength.getValue(), this.turtle.getValue(), this.regen.getValue(), this.fireRes.getValue(), this.absorption.getValue(), true, this.potionLevels.getValue(), this.potionBlankGap.getValueFloat(), this.potionTextGap.getValueFloat())) {
         int index = renderedText.indexOf(display.text);
         if (index >= 0) {
            String prefix = renderedText.substring(0, index);
            float prefixWidth = fancyFont && FontManager.ui != null ? FontManager.ui.getWidth(prefix) : (float)mc.textRenderer.getWidth(prefix);
            float iconX = textX + prefixWidth;
            float levelX = iconX + iconSize + this.potionIconGap.getValueFloat();
            if (!display.level.isEmpty()) {
               if (fancyFont && FontManager.ui != null) {
                  FontManager.ui.drawString(context.getMatrices(), display.level, (double)levelX, (double)textY, display.color, false);
               } else {
                  context.drawText(mc.textRenderer, display.level, Math.round(levelX), Math.round(textY), display.color, false);
               }
            }

            this.drawEffectIcon(context, display.effectId, Math.round(iconX), Math.round(iconY), Math.round(iconSize));
         }
      }

   }

   private void drawEffectIcon(DrawContext context, String effectId, int x, int y, int size) {
      try {
         context.drawTexture(Identifier.of("minecraft", "textures/mob_effect/" + effectId + ".png"), x, y, 0.0F, 0.0F, size, size, size, size);
      } catch (Exception var7) {
      }

   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (this.effects.getValue()) {
         if (this.strength.getValue() || this.turtle.getValue() || this.regen.getValue() || this.fireRes.getValue() || this.absorption.getValue()) {
            if (!Module.nullCheck()) {
               Packet var3 = event.getPacket();
               if (var3 instanceof WorldEventS2CPacket) {
                  WorldEventS2CPacket packet = (WorldEventS2CPacket)var3;
                  if (packet.getEventId() == 2002) {
                     this.trackSplashPotion(packet);
                  }
               }

            }
         }
      }
   }

   private void trackSplashPotion(WorldEventS2CPacket packet) {
      Vec3d splashPos = Vec3d.ofCenter(packet.getPos());
      double radius = packet.getData() == 1 ? (double)6.0F : (double)4.0F;
      PotionEntity potionEntity = this.findSplashPotionEntity(splashPos);
      if (potionEntity != null) {
         ItemStack stack = potionEntity.getStack();
         PotionContentsComponent potionContents = (PotionContentsComponent)stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
         if (potionContents != PotionContentsComponent.DEFAULT) {
            ArrayList<PotionInfo> potionInfos = new ArrayList();

            for(StatusEffectInstance effectInstance : potionContents.getEffects()) {
               RegistryEntry<StatusEffect> effectType = effectInstance.getEffectType();
               Map<Integer, EffectTimer> timerMap = null;
               int maxDuration = 0;
               if (effectType == StatusEffects.STRENGTH) {
                  if (effectInstance.getAmplifier() != 0 && effectInstance.getAmplifier() != 1) {
                     continue;
                  }

                  timerMap = this.strengthTimers;
                  maxDuration = maxStrengthTicks(effectInstance.getAmplifier());
               } else if (effectType == StatusEffects.RESISTANCE) {
                  if (effectInstance.getAmplifier() != 2 && effectInstance.getAmplifier() != 3) {
                     continue;
                  }

                  timerMap = this.turtleTimers;
                  maxDuration = maxResistanceTicks(effectInstance.getAmplifier());
               } else if (effectType == StatusEffects.REGENERATION) {
                  timerMap = this.regenTimers;
                  maxDuration = 900;
               } else if (effectType == StatusEffects.FIRE_RESISTANCE) {
                  timerMap = this.fireResTimers;
                  maxDuration = 800;
               } else if (effectType == StatusEffects.ABSORPTION) {
                  timerMap = this.absorptionTimers;
                  maxDuration = 100;
               }

               if (timerMap != null) {
                  potionInfos.add(new PotionInfo(timerMap, effectInstance.getDuration(), effectInstance.getAmplifier(), maxDuration));
               }
            }

            if (!potionInfos.isEmpty()) {
               long now = System.currentTimeMillis();

               for(PlayerEntity player : mc.world.getPlayers()) {
                  if (player != null && !player.isSpectator() && !player.isDead()) {
                     double distance = this.potionModernDistance.getValue() ? this.getClosestDistanceToBox(splashPos, player) : splashPos.distanceTo(player.getPos());
                     if (!(distance > radius)) {
                        double factor = (double)1.0F - distance / radius;
                        int playerId = player.getId();

                        for(PotionInfo potionInfo : potionInfos) {
                           int duration = (int)(factor * (double)potionInfo.baseDuration + (double)0.5F);
                           if (duration >= 20) {
                              duration = Math.min(duration, potionInfo.maxDuration);
                              this.mergePotionTimer(potionInfo.map, playerId, now + (long)duration * 50L, potionInfo.amplifier);
                           }
                        }
                     }
                  }
               }
            }

         }
      }
   }

   private PotionEntity findSplashPotionEntity(Vec3d splashPos) {
      PotionEntity closest = null;
      double closestDist = (double)4.0F;

      for(Entity entity : mc.world.getEntities()) {
         if (entity instanceof PotionEntity potion) {
            double dist = entity.getPos().squaredDistanceTo(splashPos);
            if (dist < closestDist) {
               closestDist = dist;
               closest = potion;
            }
         }
      }

      return closest;
   }

   private double getClosestDistanceToBox(Vec3d splashPos, PlayerEntity player) {
      Box box = player.getBoundingBox();
      double dx = Math.max((double)0.0F, Math.max(box.minX - splashPos.x, splashPos.x - box.maxX));
      double dy = Math.max((double)0.0F, Math.max(box.minY - splashPos.y, splashPos.y - box.maxY));
      double dz = Math.max((double)0.0F, Math.max(box.minZ - splashPos.z, splashPos.z - box.maxZ));
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private void mergePotionTimer(Map<Integer, EffectTimer> timers, int playerId, long expiry, int amplifier) {
      timers.merge(playerId, new EffectTimer(expiry, amplifier), (oldTimer, newTimer) -> {
         if (newTimer.amplifier > oldTimer.amplifier) {
            return newTimer;
         } else {
            return newTimer.amplifier == oldTimer.amplifier && newTimer.expiry > oldTimer.expiry ? newTimer : oldTimer;
         }
      });
   }

   private void detectGapple(PlayerEntity player, int playerId, long now) {
      StringBuilder effectKey = new StringBuilder();
      StatusEffectInstance absorptionEffect = null;

      for(StatusEffectInstance effect : player.getStatusEffects()) {
         effectKey.append(effect.getTranslationKey()).append(effect.getAmplifier()).append(';');
         if (effect.getEffectType() == StatusEffects.ABSORPTION) {
            absorptionEffect = effect;
         }
      }

      String lastKey = (String)this.lastEffects.getOrDefault(playerId, "");
      this.lastEffects.put(playerId, effectKey.toString());
      if (absorptionEffect != null) {
         String var10000 = ((StatusEffect)StatusEffects.ABSORPTION.value()).getTranslationKey();
         String absorptionKey = var10000 + absorptionEffect.getAmplifier();
         if (!lastKey.contains(absorptionKey)) {
            if (absorptionEffect.getAmplifier() >= 3) {
               this.mergePotionTimer(this.regenTimers, playerId, now + 300000L, 0);
               this.mergePotionTimer(this.fireResTimers, playerId, now + 300000L, 0);
               this.mergePotionTimer(this.regenTimers, playerId, now + 20000L, 1);
               this.mergePotionTimer(this.absorptionTimers, playerId, now + 120000L, 3);
            } else {
               this.mergePotionTimer(this.absorptionTimers, playerId, now + 120000L, 0);
               this.mergePotionTimer(this.regenTimers, playerId, now + 5000L, 1);
            }
         }
      }

   }

   @EventListener
   public void onTotemPop(TotemEvent event) {
      PlayerEntity player = event.getPlayer();
      if (player != null) {
         if (this.effects.getValue() && this.potionTotem.getValue()) {
            int playerId = player.getId();
            long now = System.currentTimeMillis();
            this.strengthTimers.remove(playerId);
            this.turtleTimers.remove(playerId);
            this.regenTimers.remove(playerId);
            this.fireResTimers.remove(playerId);
            this.absorptionTimers.remove(playerId);
            this.mergePotionTimer(this.regenTimers, playerId, now + 45000L, 1);
            this.mergePotionTimer(this.fireResTimers, playerId, now + 40000L, 0);
            this.mergePotionTimer(this.absorptionTimers, playerId, now + 5000L, 1);
         }
      }
   }

   private void clearPotionState(int playerId) {
      this.strengthTimers.remove(playerId);
      this.turtleTimers.remove(playerId);
      this.regenTimers.remove(playerId);
      this.fireResTimers.remove(playerId);
      this.absorptionTimers.remove(playerId);
      this.lastEffects.remove(playerId);
   }

   private void pruneExpired(long now) {
      this.strengthTimers.entrySet().removeIf((entry) -> ((EffectTimer)entry.getValue()).expiry <= now);
      this.turtleTimers.entrySet().removeIf((entry) -> ((EffectTimer)entry.getValue()).expiry <= now);
      this.regenTimers.entrySet().removeIf((entry) -> ((EffectTimer)entry.getValue()).expiry <= now);
      this.fireResTimers.entrySet().removeIf((entry) -> ((EffectTimer)entry.getValue()).expiry <= now);
      this.absorptionTimers.entrySet().removeIf((entry) -> ((EffectTimer)entry.getValue()).expiry <= now);
   }

   private int getTextWidth(String text) {
      return this.font.getValue() ? (int)Math.ceil((double)FontManager.ui.getWidth(text)) : mc.textRenderer.getWidth(text);
   }

   private static String toRoman(int value) {
      String var10000;
      switch (value) {
         case 1 -> var10000 = "I";
         case 2 -> var10000 = "II";
         case 3 -> var10000 = "III";
         case 4 -> var10000 = "IV";
         case 5 -> var10000 = "V";
         default -> var10000 = Integer.toString(value);
      }

      return var10000;
   }

   private String formatPotionTime(double seconds) {
      seconds = Math.max(this.potionDecimal.getValue() ? 0.1 : (double)1.0F, seconds);
      if (this.potionTimeStyle.getValue() == TextRadar.PotionTimeStyle.Noxis) {
         int minutes = (int)(seconds / (double)60.0F);
         double remaining = seconds - (double)minutes * (double)60.0F;
         return this.potionDecimal.getValue() ? String.format(Locale.ROOT, "[%d:%04.1f]", minutes, remaining) : String.format(Locale.ROOT, "[%d:%02d]", minutes, (int)remaining);
      } else {
         return this.potionDecimal.getValue() ? String.format(Locale.ROOT, "%.1fs", seconds) : (int)seconds + "s";
      }
   }

   private static long maxResistanceSeconds(int amplifier) {
      if (amplifier >= 3) {
         return 20L;
      } else {
         return amplifier >= 2 ? 40L : Long.MAX_VALUE;
      }
   }

   private static long maxStrengthSeconds(int amplifier) {
      return amplifier >= 1 ? 90L : 480L;
   }

   private static long maxEffectSeconds(EffectLimit limit, int amplifier) {
      long var10000;
      switch (limit.ordinal()) {
         case 0 -> var10000 = Long.MAX_VALUE;
         case 1 -> var10000 = maxResistanceSeconds(amplifier);
         case 2 -> var10000 = maxStrengthSeconds(amplifier);
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private static int maxResistanceTicks(int amplifier) {
      long seconds = maxResistanceSeconds(amplifier);
      return seconds == Long.MAX_VALUE ? Integer.MAX_VALUE : (int)seconds * 20;
   }

   private static int maxStrengthTicks(int amplifier) {
      return (int)maxStrengthSeconds(amplifier) * 20;
   }

   public static Formatting getHealthColor(PlayerEntity player) {
      double health = (double)(player.getHealth() + player.getAbsorptionAmount());
      if (health > (double)18.0F) {
         return Formatting.GREEN;
      } else if (health > (double)16.0F) {
         return Formatting.DARK_GREEN;
      } else if (health > (double)12.0F) {
         return Formatting.YELLOW;
      } else if (health > (double)8.0F) {
         return Formatting.GOLD;
      } else {
         return health > (double)4.0F ? Formatting.RED : Formatting.DARK_RED;
      }
   }

   public static Formatting getPopColor(int totems) {
      if (INSTANCE != null && INSTANCE.red.getValue()) {
         return Formatting.RED;
      } else if (totems > 10) {
         return Formatting.DARK_RED;
      } else if (totems > 8) {
         return Formatting.RED;
      } else if (totems > 6) {
         return Formatting.GOLD;
      } else if (totems > 4) {
         return Formatting.YELLOW;
      } else {
         return totems > 2 ? Formatting.DARK_GREEN : Formatting.GREEN;
      }
   }

   static {
      TURTLE_COLOR = Formatting.BLUE.toString();
      TURTLE_COLOR_RGB = (new Color(85, 85, 255)).getRGB();
   }

   private static enum EffectLimit {
      NONE,
      RESISTANCE,
      STRENGTH;

      // $FF: synthetic method
      private static EffectLimit[] $values() {
         return new EffectLimit[]{NONE, RESISTANCE, STRENGTH};
      }
   }

   private static record EffectSnapshot(double seconds, int amplifier) {
   }

   private static record EffectTimer(long expiry, int amplifier) {
   }

   private static record PotionDisplay(String text, String level, int color, String effectId, String effectName) {
   }

   private static record PotionInfo(Map<Integer, EffectTimer> map, int baseDuration, int amplifier, int maxDuration) {
   }

   private class RadarRow {
      private final PlayerEntity player;
      private final String text;
      private final int color;
      private final int textWidth;
      private final int lineHeight;

      private RadarRow(PlayerEntity player, String text, int color, int textWidth, int lineHeight) {
         this.player = player;
         this.text = text;
         this.color = color;
         this.textWidth = textWidth;
         this.lineHeight = lineHeight;
      }
   }

   public static enum SortMode {
      Distance,
      Health;

      // $FF: synthetic method
      private static SortMode[] $values() {
         return new SortMode[]{Distance, Health};
      }
   }

   public static enum PotionTimeStyle {
      Mine,
      Noxis;

      // $FF: synthetic method
      private static PotionTimeStyle[] $values() {
         return new PotionTimeStyle[]{Mine, Noxis};
      }
   }
}
