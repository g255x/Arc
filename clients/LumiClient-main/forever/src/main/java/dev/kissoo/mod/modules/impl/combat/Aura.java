package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import dev.kissoo.chat.ChatClient;
import dev.kissoo.mod.modules.impl.misc.FakePlayer;
import dev.kissoo.mod.modules.impl.misc.IrcModule;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.PacketEvent;
import dev.kissoo.api.events.impl.RotationEvent;
import dev.kissoo.api.utils.combat.CombatUtil;
import dev.kissoo.api.utils.math.Animation;
import dev.kissoo.api.utils.math.Easing;
import dev.kissoo.api.utils.math.MathUtil;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.render.ColorUtil;
import dev.kissoo.api.utils.render.JelloUtil;
import dev.kissoo.api.utils.render.Render3DUtil;
import hoprc.asm.accessors.IEntity;
import hoprc.asm.accessors.ILivingEntity;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.exploit.Blink;
import dev.kissoo.mod.modules.impl.movement.ElytraFlight;
import dev.kissoo.mod.modules.impl.movement.Velocity;
import dev.kissoo.mod.modules.settings.enums.SwingSide;
import dev.kissoo.mod.modules.settings.enums.Timing;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;

import java.awt.Color;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.component.type.ItemEnchantmentsComponent;

public class Aura extends Module {
   public static Aura INSTANCE;
   public static Entity target;

   public final EnumSetting<Aura.Page> page = this.add(new EnumSetting("Page", Aura.Page.General));
   public final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 0.1F, 7.0, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 8.0, 0.1F, 14.0, () -> this.page.getValue() == Aura.Page.General));
   private final EnumSetting<Aura.Cooldown> cooldownMode = this.add(
           new EnumSetting("CooldownMode", Aura.Cooldown.Delay, () -> this.page.getValue() == Aura.Page.General)
   );
   private final BooleanSetting reset = this.add(
           new BooleanSetting("Reset", true, () -> this.page.getValue() == Aura.Page.General && this.cooldownMode.is(Aura.Cooldown.Delay))
   );
   private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting("Swing", SwingSide.All, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting hurtTime = this.add(new SliderSetting("HurtTime", 10.0, 0.0, 10.0, 1.0, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting cooldown = this.add(new SliderSetting("Cooldown", 1.1F, 0.0, 1.2F, 0.01, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting wallRange = this.add(new SliderSetting("WallRange", 6.0, 0.1F, 7.0, () -> this.page.getValue() == Aura.Page.General));
   private final BooleanSetting whileEating = this.add(new BooleanSetting("WhileUsing", true, () -> this.page.getValue() == Aura.Page.General));

   public final EnumSetting<SwitchMode> switchMode = this.add(
           new EnumSetting<>("SwitchMode", SwitchMode.None, () -> this.page.getValue() == Aura.Page.General)
   );

   private final BooleanSetting weaponOnly = this.add(
           new BooleanSetting("WeaponOnly", true,
                   () -> this.page.getValue() == Aura.Page.General && this.switchMode.getValue() != SwitchMode.Silent)
   );

   private final EnumSetting<SilentWeapon> silentWeapon = this.add(
           new EnumSetting<>("SilentWeapon", SilentWeapon.Sword,
                   () -> this.page.getValue() == Aura.Page.General && this.switchMode.getValue() == SwitchMode.Silent)
   );
   private final BooleanSetting silentEatPause = this.add(
           new BooleanSetting("SilentEatPause", true,
                   () -> this.page.getValue() == Aura.Page.General && this.switchMode.getValue() == SwitchMode.Silent)
   );

   private final BooleanSetting maceAura = this.add(new BooleanSetting("MaceAura", false, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting minBonusDamage = this.add(new SliderSetting("MinDamage", 4.0, 1.0, 36.0, 0.1, () -> this.page.getValue() == Aura.Page.General && this.maceAura.getValue()));
   private final BooleanSetting maceBreach = this.add(new BooleanSetting("BreachSwap", false, () -> this.page.getValue() == Aura.Page.General && this.maceAura.getValue()));

   private final EnumSetting<Timing> timing = this.add(new EnumSetting("Timing", Timing.All, () -> this.page.getValue() == Aura.Page.General));

   private final BooleanSetting Players = this.add(new BooleanSetting("Players", true, () -> this.page.getValue() == Aura.Page.Target).setParent());
   private final BooleanSetting armorLow = this.add(
           new BooleanSetting("ArmorLow", true, () -> this.page.getValue() == Aura.Page.Target && this.Players.isOpen())
   );
   private final BooleanSetting Mobs = this.add(new BooleanSetting("Mobs", true, () -> this.page.getValue() == Aura.Page.Target));
   private final BooleanSetting Animals = this.add(new BooleanSetting("Animals", true, () -> this.page.getValue() == Aura.Page.Target));
   private final BooleanSetting Villagers = this.add(new BooleanSetting("Villagers", true, () -> this.page.getValue() == Aura.Page.Target));
   private final BooleanSetting Slimes = this.add(new BooleanSetting("Slimes", true, () -> this.page.getValue() == Aura.Page.Target));
   private final EnumSetting<Aura.TargetMode> targetMode = this.add(
           new EnumSetting("Filter", Aura.TargetMode.DISTANCE, () -> this.page.getValue() == Aura.Page.Target)
   );

   private final EnumSetting<Aura.TargetESP> mode = this.add(new EnumSetting("TargetESP", Aura.TargetESP.Fill, () -> this.page.getValue() == Aura.Page.Render));
   private final SliderSetting animationTime = this.add(
           new SliderSetting("AnimationTime", 200.0, 0.0, 2000.0, 1.0, () -> this.page.getValue() == Aura.Page.Render)
   );
   private final EnumSetting<Easing> ease = this.add(new EnumSetting("Ease", Easing.CubicInOut, () -> this.page.getValue() == Aura.Page.Render));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 50), () -> this.page.getValue() == Aura.Page.Render));
   private final ColorSetting outlineColor = this.add(
           new ColorSetting("OutlineColor", new Color(255, 255, 255, 50), () -> this.page.getValue() == Aura.Page.Render)
   );
   private final ColorSetting hitColor = this.add(new ColorSetting("HitColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == Aura.Page.Render));
   private final ColorSetting hitOutlineColor = this.add(
           new ColorSetting("HitOutlineColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == Aura.Page.Render)
   );

   public final BooleanSetting fakeBlock = this.add(
           new BooleanSetting("FakeBlock", false, () -> this.page.getValue() == Aura.Page.Render)
   );
   public final EnumSetting<FakeBlockMode> fakeBlockMode = this.add(
           new EnumSetting<>("FakeBlockMode", FakeBlockMode.Style_1_7, () -> this.page.getValue() == Aura.Page.Render && this.fakeBlock.getValue())
   );
   public final SliderSetting fakeBlockSpeed = this.add(
           new SliderSetting("FakeBlockSpeed", 6, 1, 50, 1, () -> this.page.getValue() == Aura.Page.Render && this.fakeBlock.getValue())
   );

   private final Animation animation = new Animation();

   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == Aura.Page.Rotate));
   private final EnumSetting<RotateMode> rotateMode = this.add(
           new EnumSetting<>("RotateMode", RotateMode.Normal,
                   () -> this.rotate.getValue() && this.page.getValue() == Aura.Page.Rotate)
   );
   private final BooleanSetting yawStep = this.add(
           new BooleanSetting("YawStep", false,
                   () -> this.rotate.getValue()
                           && this.rotateMode.getValue() == RotateMode.Normal
                           && this.page.getValue() == Aura.Page.Rotate
           ).setParent()
   );
   private final BooleanSetting whenElytra = this.add(
           new BooleanSetting("FallFlying", true,
                   () -> this.rotate.getValue()
                           && this.rotateMode.getValue() == RotateMode.Normal
                           && this.yawStep.isOpen()
                           && this.page.getValue() == Aura.Page.Rotate)
   );
   private final SliderSetting steps = this.add(
           new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01,
                   () -> this.page.getValue() == Aura.Page.Rotate
                           && this.rotateMode.getValue() == RotateMode.Normal
                           && this.yawStep.isOpen())
   );
   private final BooleanSetting checkFov = this.add(
           new BooleanSetting("OnlyLooking", true,
                   () -> this.page.getValue() == Aura.Page.Rotate
                           && this.rotateMode.getValue() == RotateMode.Normal
                           && this.yawStep.isOpen())
   );
   private final SliderSetting fov = this.add(
           new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1,
                   () -> this.checkFov.getValue()
                           && this.page.getValue() == Aura.Page.Rotate
                           && this.rotateMode.getValue() == RotateMode.Normal
                           && this.yawStep.isOpen())
   );
   private final SliderSetting priority = this.add(
           new SliderSetting("Priority", 10, 0, 100,
                   () -> this.page.getValue() == Aura.Page.Rotate
                           && this.rotateMode.getValue() == RotateMode.Normal
                           && this.yawStep.isOpen())
   );

   private final Timer tick = new Timer();
   public Vec3d directionVec = null;

   private int cachedWeaponSlot = -1;

   public Aura() {
      super("Aura", Module.Category.Combat);
      this.setChinese("杀戮光环");
      INSTANCE = this;
   }

   public boolean shouldFakeBlock() {
      return this.isOn() && this.fakeBlock.getValue() && target != null;
   }

   public FakeBlockMode getFakeBlockMode() {
      return this.fakeBlockMode.getValue();
   }

   public int getFakeBlockSpeed() {
      return this.fakeBlockSpeed.getValueInt();
   }

   private int findWeaponSlot(Entity currentTarget) {
      int maceSlot = getMaceSlot(currentTarget);
      if (maceSlot != -1) {
         return maceSlot;
      }

      for (int i = 0; i < 9; i++) {
         Item item = mc.player.getInventory().getStack(i).getItem();
         switch (this.silentWeapon.getValue()) {
            case Sword:
               if (item instanceof SwordItem) return i;
               break;
            case Axe:
               if (item instanceof AxeItem) return i;
               break;
            case Mace:
               if (item instanceof MaceItem) return i;
               break;
            case Any:
               if (item instanceof SwordItem || item instanceof AxeItem || item instanceof MaceItem) return i;
               break;
         }
      }
      return -1;
   }

   private int getMaceSlot(Entity currentTarget) {
      if (!this.maceAura.getValue()) return -1;
      float fallDist = mc.player.fallDistance;

      if (fallDist > 1.5f) {
         int maceSlot = findItemSlot(MaceItem.class);
         if (maceSlot != -1) {
            float damage = getMaceBonusDamage(fallDist, mc.player.getInventory().getStack(maceSlot));
            if (damage >= this.minBonusDamage.getValueFloat()) {
               return maceSlot;
            }
         }
      }

      if (this.maceBreach.getValue() && currentTarget instanceof LivingEntity) {
         if (((LivingEntity) currentTarget).getArmor() > 0) {
            int maceSlot = findItemSlot(MaceItem.class);
            if (maceSlot != -1 && getEnchantmentLevel(mc.player.getInventory().getStack(maceSlot), "breach") > 0) {
               return maceSlot;
            }
         }
      }

      return -1;
   }

   private int findItemSlot(Class<? extends Item> itemClass) {
      for (int i = 0; i < 9; i++) {
         if (itemClass.isInstance(mc.player.getInventory().getStack(i).getItem())) {
            return i;
         }
      }
      return -1;
   }

   private int getEnchantmentLevel(ItemStack stack, String enchantName) {
      ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
      if (enchants != null) {
         for (var entry : enchants.getEnchantments()) {
            if (entry.getKey().isPresent() && entry.getKey().get().getValue().getPath().contains(enchantName)) {
               return enchants.getLevel(entry);
            }
         }
      }
      return 0;
   }

   private float getMaceBonusDamage(float fallDistance, ItemStack itemStack) {
      int densityLevel = getEnchantmentLevel(itemStack, "density");
      int h = (int) Math.floor(Math.max(0.0f, fallDistance));
      float i = h <= 3.0f ? 4.0f * h : (h <= 8.0f ? 12.0f + 2.0f * (h - 3.0f) : 22.0f + h - 8.0f);
      float damage = 6.0f + i;
      if (densityLevel > 0) {
         damage += 0.5f * densityLevel * h;
      }
      return damage;
   }

   private float getAttackSpeedForSlot(int slot) {
      ItemStack stack = mc.player.getInventory().getStack(slot);
      double baseSpeed = 4.0;

      if (stack.isEmpty()) return (float) baseSpeed;

      AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (modifiers != null) {
         for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() == EntityAttributes.GENERIC_ATTACK_SPEED.value()) {
               switch (entry.modifier().operation()) {
                  case ADD_VALUE:
                     baseSpeed += entry.modifier().value();
                     break;
                  case ADD_MULTIPLIED_BASE:
                     baseSpeed += baseSpeed * entry.modifier().value();
                     break;
                  case ADD_MULTIPLIED_TOTAL:
                     baseSpeed *= (1.0 + entry.modifier().value());
                     break;
               }
            }
         }
      }

      return (float) baseSpeed;
   }

   private float getCooldownProgressPerTickForSpeed(float attackSpeed) {
      return (float) (1.0 / attackSpeed * 20.0);
   }

   private float getEffectiveCooldownPerTick() {
      if (this.switchMode.getValue() == SwitchMode.Silent && this.cachedWeaponSlot != -1) {
         float weaponSpeed = getAttackSpeedForSlot(this.cachedWeaponSlot);
         return getCooldownProgressPerTickForSpeed(weaponSpeed);
      }
      return getAttackCooldownProgressPerTick();
   }

   private void silentSwitchAttack(Entity attackTarget) {
      int originalSlot = mc.player.getInventory().selectedSlot;
      int weaponSlot = this.cachedWeaponSlot;

      if (weaponSlot == -1) return;

      if (weaponSlot == originalSlot) {
         sendAttack(attackTarget);
         return;
      }

      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(weaponSlot));
      sendAttack(attackTarget);
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
   }

   private void sendAttack(Entity attackTarget) {
      mc.getNetworkHandler().sendPacket(
              PlayerInteractEntityC2SPacket.attack(attackTarget, mc.player.isSneaking())
      );
      mc.player.resetLastAttackedTicks();
      EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
   }

   public static void doRender(MatrixStack stack, float partialTicks, Entity entity, Color color, Color outlineColor, Aura.TargetESP mode) {
      switch (mode) {
         case Fill:
            Render3DUtil.draw3DBox(stack,
                    ((IEntity) entity).getDimensions().getBoxAt(new Vec3d(
                            MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double) partialTicks),
                            MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double) partialTicks),
                            MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double) partialTicks)
                    )).expand(0.0, 0.1, 0.0),
                    color, outlineColor, false, true);
            break;
         case Box:
            Render3DUtil.draw3DBox(stack,
                    ((IEntity) entity).getDimensions().getBoxAt(new Vec3d(
                            MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double) partialTicks),
                            MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double) partialTicks),
                            MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double) partialTicks)
                    )).expand(0.0, 0.1, 0.0),
                    color, outlineColor, true, true);
            break;
         case Jello:
            JelloUtil.drawJello(stack, entity, color);
            break;
         case ThunderHack:
            Render3DUtil.drawTargetEsp(stack, target, color);
      }
   }

   private static float[] getYawPitchTo(Vec3d fromEye, Vec3d to) {
      double dx = to.x - fromEye.x;
      double dy = to.y - fromEye.y;
      double dz = to.z - fromEye.z;
      float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
      float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
      return new float[]{yaw, Math.max(-90.0F, Math.min(90.0F, pitch))};
   }

   public static float getAttackCooldownProgressPerTick() {
      return (float) (1.0 / mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * 20.0);
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (target != null) {
         this.doRender(matrixStack, mc.getRenderTickCounter().getTickDelta(true), target, (Aura.TargetESP) this.mode.getValue());
      }
   }

   public void doRender(MatrixStack stack, float partialTicks, Entity entity, Aura.TargetESP mode) {
      switch (mode) {
         case Fill:
            Render3DUtil.draw3DBox(stack,
                    ((IEntity) entity).getDimensions().getBoxAt(new Vec3d(
                            MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double) partialTicks),
                            MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double) partialTicks),
                            MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double) partialTicks)
                    )).expand(0.0, 0.1, 0.0),
                    ColorUtil.fadeColor(this.color.getValue(), this.hitColor.getValue(),
                            this.animation.get(0.0, this.animationTime.getValueInt(), (Easing) this.ease.getValue())),
                    ColorUtil.fadeColor(this.outlineColor.getValue(), this.hitOutlineColor.getValue(),
                            this.animation.get(0.0, this.animationTime.getValueInt(), (Easing) this.ease.getValue())),
                    false, true);
            break;
         case Box:
            Render3DUtil.draw3DBox(stack,
                    ((IEntity) entity).getDimensions().getBoxAt(new Vec3d(
                            MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double) partialTicks),
                            MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double) partialTicks),
                            MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double) partialTicks)
                    )).expand(0.0, 0.1, 0.0),
                    ColorUtil.fadeColor(this.color.getValue(), this.hitColor.getValue(),
                            this.animation.get(0.0, this.animationTime.getValueInt(), (Easing) this.ease.getValue())),
                    ColorUtil.fadeColor(this.outlineColor.getValue(), this.hitOutlineColor.getValue(),
                            this.animation.get(0.0, this.animationTime.getValueInt(), (Easing) this.ease.getValue())),
                    true, true);
            break;
         case Jello:
            JelloUtil.drawJello(stack, entity, this.color.getValue());
            break;
         case ThunderHack:
            Render3DUtil.drawTargetEsp(stack, target, this.color.getValue());
      }
   }

   @Override
   public String getInfo() {
      return target == null ? null : target.getName().getString();
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {

            boolean isSilent = this.switchMode.getValue() == SwitchMode.Silent;

            if (!isSilent) {
               if (this.weaponOnly.getValue() && !EntityUtil.isHoldingWeapon(mc.player) && getMaceSlot(null) == -1) {
                  target = null;
                  return;
               }
            }

            if (isSilent && this.silentEatPause.getValue() && mc.player.isUsingItem()) {
               target = null;
               return;
            }

            target = this.getTarget(this.range.getValueFloat());
            boolean inRange = true;
            if (target == null) {
               target = this.getTarget(this.targetRange.getValueFloat());
               inRange = false;
            }

            if (target != null) {
               if (isSilent) {
                  this.cachedWeaponSlot = findWeaponSlot(target);
                  if (this.cachedWeaponSlot == -1) {
                     target = null;
                     return;
                  }
               }

               if (inRange) {
                  this.doAura();
               }
            }
         }
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (target != null && this.rotate.getValue()
              && this.rotateMode.getValue() == RotateMode.Normal
              && this.shouldYawStep()) {
         this.directionVec = this.getAttackVec(target);
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @EventListener
   public void onPacket(PacketEvent.Send event) {
      if (this.reset.getValue()) {
         Packet<?> packet = event.getPacket();
         if (packet instanceof HandSwingC2SPacket
                 || packet instanceof PlayerInteractEntityC2SPacket
                 && Criticals.getInteractType((PlayerInteractEntityC2SPacket) packet) == InteractType.ATTACK) {
            this.tick.reset();
         }
      }
   }

   private boolean check() {
      if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
         return false;
      }

      int at = (int) (this.tick.getMs() / 50L);

      at = (int) (at * Forever.SERVER.getTPSFactor());

      float cooldownPerTick = getEffectiveCooldownPerTick();
      if (!(Math.max(at / cooldownPerTick, 0.0F) >= this.cooldown.getValue())) {
         return false;
      }

      if (target instanceof LivingEntity entity && entity.hurtTime > this.hurtTime.getValue()) {
         return false;
      }

      if (this.switchMode.getValue() != SwitchMode.Silent) {
         if (!this.whileEating.getValue() && mc.player.isUsingItem()) {
            return false;
         }
      }

      return true;
   }

   private void doAura() {
      if (this.check()) {
         Vec3d hitVec = this.getAttackVec(target);

         if (this.rotate.getValue()) {
            if (this.rotateMode.getValue() == RotateMode.Normal) {
               if (!this.faceVector(hitVec)) {
                  return;
               }
            }
         }

         this.animation.to = 1.0;
         this.animation.from = 1.0;

         boolean isSilent = this.switchMode.getValue() == SwitchMode.Silent;

         if (!isSilent) {
            int maceSlot = getMaceSlot(target);
            if (maceSlot != -1 && mc.player.getInventory().selectedSlot != maceSlot) {
               mc.player.getInventory().selectedSlot = maceSlot;
            }
         }

         if (this.rotate.getValue() && this.rotateMode.getValue() == RotateMode.C06) {
            float[] rp = getYawPitchTo(mc.player.getEyePos(), hitVec);

            mc.getNetworkHandler().sendPacket(
                    new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
                            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            rp[0], rp[1], mc.player.isOnGround()
                    )
            );

            if (isSilent) {
               this.silentSwitchAttack(target);
            } else {
               sendAttack(target);
            }

            mc.getNetworkHandler().sendPacket(
                    new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
                            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()
                    )
            );
         } else {
            if (isSilent) {
               this.silentSwitchAttack(target);
            } else {
               sendAttack(target);
            }
         }

         this.tick.reset();
         if (this.rotate.getValue() && this.rotateMode.getValue() == RotateMode.Normal && !this.shouldYawStep()) {
            Forever.ROTATION.snapBack();
         }
      }
   }

   private Vec3d getAttackVec(Entity entity) {
      return MathUtil.getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
   }

   private boolean shouldYawStep() {
      return this.whenElytra.getValue()
              || !mc.player.isFallFlying()
              && (!ElytraFlight.INSTANCE.isOn() || !ElytraFlight.INSTANCE.isFallFlying())
              ? this.yawStep.getValue() && !Velocity.INSTANCE.noRotation()
              : false;
   }

   public boolean faceVector(Vec3d directionVec) {
      if (!this.shouldYawStep()) {
         Forever.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.directionVec = directionVec;
         return Forever.ROTATION.inFov(directionVec, this.fov.getValueFloat()) || !this.checkFov.getValue();
      }
   }

   public Entity getTarget(double range) {
      Entity target = null;
      double distance = range;
      double maxHealth = 36.0;

      for (Entity entity : Forever.THREAD.getEntities()) {
         if (this.isEnemy(entity)) {
            Vec3d hitVec = this.getAttackVec(entity);
            if (!(mc.player.getEyePos().distanceTo(hitVec) > range)
                    && (mc.player.canSee(entity) || !(mc.player.getEyePos().distanceTo(hitVec) > this.wallRange.getValue()))
                    && CombatUtil.isValid(entity)) {
               if (target == null) {
                  target = entity;
                  distance = mc.player.getEyePos().distanceTo(hitVec);
                  maxHealth = EntityUtil.getHealth(entity);
               } else {
                  if (this.armorLow.getValue() && entity instanceof PlayerEntity && EntityUtil.isArmorLow((PlayerEntity) entity, 10)) {
                     target = entity;
                     break;
                  }
                  if (this.targetMode.getValue() == Aura.TargetMode.HEALTH && EntityUtil.getHealth(entity) < maxHealth) {
                     target = entity;
                     maxHealth = EntityUtil.getHealth(entity);
                  } else if (this.targetMode.getValue() == Aura.TargetMode.DISTANCE && mc.player.getEyePos().distanceTo(hitVec) < distance) {
                     target = entity;
                     distance = mc.player.getEyePos().distanceTo(hitVec);
                  }
               }
            }
         }
      }
      return target;
   }

   private boolean isEnemy(Entity entity) {
      if (FakePlayer.isFakePlayer(entity)) return false;
      if (entity instanceof SlimeEntity) {
         return this.Slimes.getValue();
      } else if (entity instanceof PlayerEntity player) {
         if (!this.Players.getValue()) return false;
         return !IrcModule.shouldProtectIrcUser(player.getGameProfile().getName());
      } else if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) {
         return this.Villagers.getValue();
      } else if (entity instanceof AnimalEntity) {
         return this.Animals.getValue();
      } else {
         return entity instanceof MobEntity ? this.Mobs.getValue() : false;
      }
   }

   public enum SwitchMode {
      None,
      Silent
   }

   public enum SilentWeapon {
      Sword,
      Axe,
      Mace,
      Any
   }

   public static enum Cooldown {
      Vanilla,
      Delay;
   }

   public static enum Mode {
      Mace,
      Axe,
      Sword;
   }

   public static enum Page {
      General,
      Rotate,
      Target,
      Render;
   }

   public static enum TargetESP {
      Fill,
      Box,
      Jello,
      ThunderHack,
      None;
   }

   private static enum TargetMode {
      DISTANCE,
      HEALTH;
   }

   private enum RotateMode {
      Normal,
      C06
   }

   public enum FakeBlockMode {
      Style_1_7,
      SideDown,
      WindMill,
      Smooth,
      Naven,
      Push
   }
}
