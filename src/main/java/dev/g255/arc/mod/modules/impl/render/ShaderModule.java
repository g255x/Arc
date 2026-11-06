package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.Arc;
import dev.g255.arc.asm.accessors.IGameRenderer;
import dev.g255.arc.core.impl.ShaderManager;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;

public class ShaderModule extends Module {
   public static ShaderModule INSTANCE;
   private final EnumSetting<Page> page;
   public final EnumSetting<ShaderManager.Shader> mode;
   public final SliderSetting speed;
   public final ColorSetting fill;
   public final SliderSetting maxSample;
   public final SliderSetting divider;
   public final SliderSetting radius;
   public final SliderSetting smoothness;
   public final SliderSetting alpha;
   public final SliderSetting maxRange;
   public final SliderSetting pulseSpeed;
   public final ColorSetting pulse;
   public final SliderSetting step;
   public final SliderSetting octaves;
   public final ColorSetting smoke1;
   public final ColorSetting smoke2;
   public final ColorSetting smoke3;
   public final ColorSetting smoke4;
   private final BooleanSetting hands;
   private final BooleanSetting self;
   private final BooleanSetting players;
   private final BooleanSetting friends;
   private final BooleanSetting crystals;
   private final BooleanSetting creatures;
   private final BooleanSetting monsters;
   private final BooleanSetting ambients;
   private final BooleanSetting items;
   private final BooleanSetting others;

   public ShaderModule() {
      super("Shader", Module.Category.Render);
      this.page = this.add(new EnumSetting("Page", ShaderModule.Page.Shader));
      this.mode = this.add(new EnumSetting("Mode", ShaderManager.Shader.Solid, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.speed = this.add(new SliderSetting("Speed", (double)4.0F, (double)0.0F, (double)20.0F, 0.1, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.fill = this.add(new ColorSetting("Color", () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.maxSample = this.add(new SliderSetting("MaxSample", (double)10.0F, (double)0.0F, (double)20.0F, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.divider = this.add(new SliderSetting("Divider", (double)150.0F, (double)0.0F, (double)300.0F, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.radius = this.add(new SliderSetting("Radius", (double)2.0F, (double)0.0F, (double)6.0F, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.smoothness = this.add(new SliderSetting("Smoothness", (double)1.0F, (double)0.0F, (double)1.0F, 0.01, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.alpha = this.add(new SliderSetting("GlowAlpha", 255, 0, 255, () -> this.page.getValue() == ShaderModule.Page.Shader));
      this.maxRange = this.add(new SliderSetting("MaxRange", 64, 16, 512, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.pulseSpeed = this.add(new SliderSetting("PulseSize", (double)10.0F, (double)0.0F, (double)200.0F, 0.1, () -> this.page.getValue() == ShaderModule.Page.Color));
      this.pulse = this.add(new ColorSetting("Pulse", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color));
      this.step = this.add(new SliderSetting("Step", (double)0.5F, (double)0.0F, (double)4.0F, (double)0.01F, () -> this.page.getValue() == ShaderModule.Page.Color));
      this.octaves = this.add(new SliderSetting("Octaves", 10, 5, 30, () -> this.page.getValue() == ShaderModule.Page.Color));
      this.smoke1 = this.add(new ColorSetting("Color1", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color));
      this.smoke2 = this.add(new ColorSetting("Color2", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color));
      this.smoke3 = this.add(new ColorSetting("Color3", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color));
      this.smoke4 = this.add(new ColorSetting("Color4", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color));
      this.hands = this.add(new BooleanSetting("Hands", true, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.self = this.add(new BooleanSetting("Self", true, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.players = this.add(new BooleanSetting("Players", true, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.friends = this.add(new BooleanSetting("Friends", true, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.crystals = this.add(new BooleanSetting("Crystals", true, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.creatures = this.add(new BooleanSetting("Creatures", false, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.monsters = this.add(new BooleanSetting("Monsters", false, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.ambients = this.add(new BooleanSetting("Ambients", false, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.items = this.add(new BooleanSetting("Items", true, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.others = this.add(new BooleanSetting("Others", false, () -> this.page.getValue() == ShaderModule.Page.Target));
      this.setChinese("着色器");
      INSTANCE = this;
   }

   public String getInfo() {
      return ((ShaderManager.Shader)this.mode.getValue()).name();
   }

   public boolean shouldRender(Entity entity) {
      if (entity == null) {
         return false;
      } else if (mc.player == null) {
         return false;
      } else if ((double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(entity.getPos())) > this.maxRange.getValue()) {
         return false;
      } else {
         Objects.requireNonNull(entity);
         if (entity instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity)entity;
            if (entity == mc.player) {
               return this.self.getValue();
            } else {
               return Arc.FRIEND.isFriend(playerEntity) ? this.friends.getValue() : this.players.getValue();
            }
         } else if (entity instanceof EndCrystalEntity) {
            return this.crystals.getValue();
         } else if (entity instanceof ItemEntity) {
            return this.items.getValue();
         } else {
            boolean var10000;
            switch (entity.getType().getSpawnGroup()) {
               case CREATURE:
               case WATER_CREATURE:
                  var10000 = this.creatures.getValue();
                  break;
               case MONSTER:
                  var10000 = this.monsters.getValue();
                  break;
               case AMBIENT:
               case WATER_AMBIENT:
                  var10000 = this.ambients.getValue();
                  break;
               default:
                  var10000 = this.others.getValue();
            }

            return var10000;
         }
      }
   }

   public void onRender3D(MatrixStack matrixStack) {
      if (this.hands.getValue()) {
         Arc.SHADER.renderShader(() -> ((IGameRenderer)mc.gameRenderer).IRenderHand(mc.gameRenderer.getCamera(), mc.getRenderTickCounter().getTickDelta(true), matrixStack.peek().getPositionMatrix()), this.mode.getValue());
      }

   }

   public void onToggle() {
      Arc.SHADER.reloadShaders();
   }

   public void onLogin() {
      Arc.SHADER.reloadShaders();
   }

   private static enum Page {
      Shader,
      Target,
      Color;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{Shader, Target, Color};
      }
   }
}
