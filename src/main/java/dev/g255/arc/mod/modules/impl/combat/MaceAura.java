package dev.g255.arc.mod.modules.impl.combat;

import dev.g255.arc.Arc;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.ClientTickEvent;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.Render3DEvent;
import dev.g255.arc.api.events.impl.TimerEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.asm.accessors.IPlayerMoveC2SPacket;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;

public class MaceAura extends Module {
   private static final double[] JUMP_HEIGHTS = new double[]{0.4199999868869781, 0.7531999805212017, 0.9999957640154541, 1.1661092609382138};
   public static MaceAura INSTANCE;
   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", (double)5.0F, (double)1.0F, (double)8.0F, 0.1));
   private final SliderSetting attackRange = this.add(new SliderSetting("AttackRange", (double)3.0F, (double)1.0F, (double)8.0F, 0.1));
   private final SliderSetting triggerDistance = this.add(new SliderSetting("TriggerDistance", (double)2.0F, 0.1, (double)20.0F, 0.1));
   private final SliderSetting lagbackDistance = this.add(new SliderSetting("LagbackDistance", (double)5.0F, (double)0.0F, (double)10.0F, 0.1));
   private final SliderSetting spoofHeight = this.add(new SliderSetting("SpoofHeight", (double)25.0F, (double)0.0F, (double)2000.0F, 0.1));
   private final SliderSetting cooldownTicks = this.add(new SliderSetting("CooldownTicks", (double)3.0F, (double)0.0F, (double)20.0F, (double)1.0F));
   private final BooleanSetting timer = this.add(new BooleanSetting("Timer", false));
   private final SliderSetting timerSpeed = this.add(new SliderSetting("TimerSpeed", (double)2.0F, (double)1.0F, (double)100.0F, 0.1, () -> this.timer.getValue()));
   private final BooleanSetting maceOnly = this.add(new BooleanSetting("MaceOnly", true));
   private final BooleanSetting silent = this.add(new BooleanSetting("Silent", false));
   private final EnumSetting<RenderMode> renderMode;
   private final ColorSetting silentPlayerBox;
   private final BooleanSetting inventorySwap;
   private final BooleanSetting onlyFalling;
   private final BooleanSetting onlyAir;
   private final BooleanSetting autoAttack;
   private final BooleanSetting set;
   private final BooleanSetting groundSpoof;
   private final BooleanSetting attackPull;
   private final EnumSetting<LagMode> lagMode;
   private final SliderSetting delay;
   private final SliderSetting delayUntilSwitch;
   private final SliderSetting lagDelay;
   private final SliderSetting delayUntilAttack;
   private final SliderSetting setRange;
   private final SliderSetting lagFly;
   private final SliderSetting smartXZ;
   private final SliderSetting smartUp;
   private final SliderSetting smartDown;
   private final SliderSetting smartDist;
   private final BooleanSetting noWait;
   private final EnumSetting<SwingMode> swing;
   private final Timer delayTimer;
   private final Timer lagTimer;
   private final Timer switchTimer;
   private final Timer attackTimer;
   private PlayerEntity target;
   private PlayerEntity setTarget;
   private Vec3d attackPos;
   private Vec3d renderPos;
   private Vec3d setAnchorPos;
   private int cooldown;
   private int airTicks;
   private boolean attacking;
   private boolean lagging;
   private boolean setArmed;
   private boolean maceSwapped;
   private boolean waitingLag;
   private float renderYaw;
   private float renderPitch;
   private float lastYaw;
   private float lastPitch;

   public MaceAura() {
      super("MaceAura", Module.Category.Combat);
      this.renderMode = this.add(new EnumSetting("RenderMode", MaceAura.RenderMode.Normal));
      this.silentPlayerBox = this.add(new ColorSetting("SilentPlayerBox", new Color(255, 80, 80, 180), () -> this.renderMode.is(MaceAura.RenderMode.Silent)));
      this.inventorySwap = this.add(new BooleanSetting("InventorySwap", false));
      this.onlyFalling = this.add(new BooleanSetting("OnlyFalling", true));
      this.onlyAir = this.add(new BooleanSetting("OnlyAir", true));
      this.autoAttack = this.add(new BooleanSetting("AutoAttack", true));
      this.set = this.add(new BooleanSetting("Set", false));
      this.groundSpoof = this.add(new BooleanSetting("GroundSpoof", true, () -> this.set.getValue()));
      this.attackPull = this.add(new BooleanSetting("AttackPull", false, () -> this.set.getValue()));
      this.lagMode = this.add(new EnumSetting("LagMode", MaceAura.LagMode.Fly, () -> this.set.getValue()));
      this.delay = this.add(new SliderSetting("Delay", (double)400.0F, (double)100.0F, (double)3000.0F, (double)10.0F, () -> this.set.getValue()));
      this.delayUntilSwitch = this.add(new SliderSetting("DelayUntilSwitch", (double)50.0F, (double)0.0F, (double)1000.0F, (double)10.0F, () -> this.set.getValue()));
      this.lagDelay = this.add(new SliderSetting("LagDelay", (double)80.0F, (double)0.0F, (double)1000.0F, (double)10.0F, () -> this.set.getValue()));
      this.delayUntilAttack = this.add(new SliderSetting("DelayUntilAttack", (double)80.0F, (double)0.0F, (double)1000.0F, (double)10.0F, () -> this.set.getValue()));
      this.setRange = this.add(new SliderSetting("SetRange", (double)5.0F, (double)1.0F, (double)10.0F, 0.1, () -> this.set.getValue()));
      this.lagFly = this.add(new SliderSetting("LagFly", (double)4.0F, 0.1, (double)10.0F, 0.1, () -> this.set.getValue() && this.lagMode.is(MaceAura.LagMode.Fly)));
      this.smartXZ = this.add(new SliderSetting("SmartXZ", (double)3.0F, (double)0.0F, (double)10.0F, 0.1, () -> this.set.getValue() && this.lagMode.is(MaceAura.LagMode.Smart)));
      this.smartUp = this.add(new SliderSetting("SmartUp", (double)3.0F, (double)0.0F, (double)10.0F, 0.1, () -> this.set.getValue() && this.lagMode.is(MaceAura.LagMode.Smart)));
      this.smartDown = this.add(new SliderSetting("SmartDown", (double)3.0F, (double)0.0F, (double)10.0F, 0.1, () -> this.set.getValue() && this.lagMode.is(MaceAura.LagMode.Smart)));
      this.smartDist = this.add(new SliderSetting("SmartDist", (double)2.0F, (double)0.0F, (double)10.0F, 0.1, () -> this.set.getValue() && this.lagMode.is(MaceAura.LagMode.Smart)));
      this.noWait = this.add(new BooleanSetting("NoWait", false));
      this.swing = this.add(new EnumSetting("Swing", MaceAura.SwingMode.All));
      this.delayTimer = new Timer();
      this.lagTimer = new Timer();
      this.switchTimer = new Timer();
      this.attackTimer = new Timer();
      this.setChinese("风锤光环");
      INSTANCE = this;
   }

   public void onEnable() {
      super.onEnable();
      this.resetState();
   }

   public void onDisable() {
      super.onDisable();
      this.restoreSlot();
      this.resetState();
   }

   private void resetState() {
      this.target = null;
      this.setTarget = null;
      this.attackPos = null;
      this.renderPos = null;
      this.setAnchorPos = null;
      this.cooldown = 0;
      this.airTicks = 0;
      this.attacking = false;
      this.lagging = false;
      this.setArmed = false;
      this.maceSwapped = false;
      this.waitingLag = false;
      this.syncRenderRot();
   }

   private void syncRenderRot() {
      if (mc.player != null) {
         this.renderYaw = mc.player.getYaw();
         this.renderPitch = mc.player.getPitch();
         this.lastYaw = this.renderYaw;
         this.lastPitch = this.renderPitch;
      }
   }

   @EventListener
   public void onTimer(TimerEvent event) {
      if (this.timer.getValue() && !nullCheck()) {
         event.set((float)this.timerSpeed.getValue());
      }

   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (event.isPost() && !nullCheck()) {
         if (this.cooldown > 0) {
            --this.cooldown;
            this.handleSilentCamera();
            this.trackAirState();
         } else {
            if (this.set.getValue()) {
               this.setTick();
            }

         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         if (this.cooldown <= 0) {
            if (this.autoAttack.getValue()) {
               if (!this.set.getValue()) {
                  this.target = this.findPlayer(this.targetRange.getValue());
                  if (this.target == null) {
                     this.attackPos = null;
                  } else if (!this.maceOnly.getValue() || this.hasMace()) {
                     if (!this.onlyAir.getValue() || !mc.player.isOnGround()) {
                        if (!this.onlyFalling.getValue() || !(mc.player.fallDistance <= 0.0F)) {
                           double dist = (double)mc.player.distanceTo(this.target);
                           if (!(dist > this.attackRange.getValue())) {
                              this.attackPos = this.target.getPos();
                              this.renderPos = this.target.getPos();
                              if (this.noWait.getValue() || !(mc.player.getAttackCooldownProgress(0.5F) < 1.0F)) {
                                 if (!(dist > this.triggerDistance.getValue())) {
                                    this.doAttack(this.attackPos);
                                    this.cooldown = (int)this.cooldownTicks.getValue();
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void setTick() {
      if (mc.player.isOnGround() && this.setAnchorPos == null) {
         this.setArmed = false;
         this.setAnchorPos = mc.player.getPos();
      }

      if (this.setAnchorPos != null) {
         if (this.setAnchorPos.distanceTo(mc.player.getPos()) > this.setRange.getValue()) {
            this.setAnchorPos = null;
            this.setArmed = false;
            this.lagging = false;
         } else {
            this.setTarget = this.findPlayer(this.targetRange.getValue());
            if (this.setTarget == null) {
               this.attackPos = null;
            } else if (!this.maceOnly.getValue() || this.hasMace()) {
               if (!this.setArmed && this.delayTimer.passedMs(this.delay.getValue())) {
                  this.setArmed = true;
                  this.switchTimer.reset();
                  this.lagTimer.reset();
                  this.attackTimer.reset();
               }

               if (this.setArmed) {
                  if (this.lagging) {
                     if (this.attackTimer.passedMs(this.delayUntilAttack.getValue())) {
                        Vec3d pos = this.setTarget.getPos();
                        if (this.attackPull.getValue()) {
                           pos = pos.add((double)0.0F, (double)0.5F, (double)0.0F);
                        }

                        this.doAttack(pos);
                        this.cooldown = (int)this.cooldownTicks.getValue();
                        this.lagging = false;
                     }

                  } else {
                     if (this.lagTimer.passedMs(this.lagDelay.getValue())) {
                        this.doLagSequence(this.setTarget.getPos());
                        this.lagging = true;
                        this.attackTimer.reset();
                     }

                  }
               }
            }
         }
      }
   }

   private void doAttack(Vec3d pos) {
      this.attacking = true;
      this.sendAttack(pos);
      this.lagTo(pos);
      mc.player.setPosition(pos.x, pos.y, pos.z);
      mc.player.setVelocity(mc.player.getVelocity().x, (double)0.0F, mc.player.getVelocity().z);
      mc.player.fallDistance = 0.0F;
      this.delayTimer.reset();
      if (this.set.getValue()) {
         this.setAnchorPos = mc.player.getPos();
      }

      this.attacking = false;
   }

   private void sendAttack(Vec3d pos) {
      mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(this.target, mc.player.isSneaking()));
      mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
   }

   private void lagTo(Vec3d targetPos) {
      double x = mc.player.getX();
      double y = mc.player.getY();
      double z = mc.player.getZ();

      for(double h : JUMP_HEIGHTS) {
         this.sendPos(x, y + h, z, false);
      }

      Vec3d top = new Vec3d(x, y + JUMP_HEIGHTS[JUMP_HEIGHTS.length - 1], z);
      this.sendSteps(top, targetPos);
      Vec3d high = new Vec3d(targetPos.x, targetPos.y + this.spoofHeight.getValue(), targetPos.z);
      this.sendOnGroundChain(top, high);
      this.sendFull(high.x, high.y, high.z, false);
      this.sendOnGroundChain(high, targetPos);
      this.sendFull(targetPos.x, targetPos.y, targetPos.z, true);
   }

   private void sendOnGroundChain(Vec3d from, Vec3d to) {
      int count = (int)Math.ceil(from.distanceTo(to) / (double)10.0F) - 1;

      for(int i = 0; i < count; ++i) {
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
      }

   }

   private void sendFull(double x, double y, double z, boolean onGround) {
      mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, mc.player.getYaw(), mc.player.getPitch(), onGround));
   }

   private void doLagSequence(Vec3d targetPos) {
      if (this.lagMode.is(MaceAura.LagMode.Fly)) {
         double x = mc.player.getX();
         double y = mc.player.getY();
         double z = mc.player.getZ();

         for(double h : JUMP_HEIGHTS) {
            this.sendPos(x, y + h, z, false);
         }

         this.sendPos(x, y + this.lagFly.getValue(), z, false);
         this.sendPos(x, y, z, true);
      } else {
         double x = mc.player.getX();
         double y = mc.player.getY();
         double z = mc.player.getZ();
         double dx = targetPos.x - x;
         double dz = targetPos.z - z;
         double len = Math.sqrt(dx * dx + dz * dz);
         if (len > this.smartDist.getValue()) {
            dx *= this.smartDist.getValue() / len;
            dz *= this.smartDist.getValue() / len;
         }

         for(double h : JUMP_HEIGHTS) {
            this.sendPos(x, y + h, z, false);
         }

         this.sendPos(x + dx, y + this.smartUp.getValue(), z + dz, false);
         this.sendPos(x + dx, y - this.smartDown.getValue(), z + dz, true);
      }

   }

   private void sendSteps(Vec3d from, Vec3d to) {
      double x = from.x;
      double z = from.z;
      double y = from.y;
      if (Math.abs(x - to.x) > (double)0.25F) {
         x += x > to.x ? (double)-0.25F : (double)0.25F;
         this.sendPos(x, y, z, false);
      }

      if (Math.abs(z - to.z) > (double)0.25F) {
         z += z > to.z ? (double)-0.25F : (double)0.25F;
         this.sendPos(x, y, z, false);
      }

      if (x != to.x || z != to.z) {
         this.sendPos(to.x, y, to.z, false);
      }

   }

   private void sendPos(double x, double y, double z, boolean onGround) {
      mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround));
   }

   private void handleSilentCamera() {
      boolean silentMode = this.renderMode.is(MaceAura.RenderMode.Silent);
      if (silentMode) {
         if (this.target != null) {
            if (Arc.MODULE != null) {
            }

            this.renderPos = this.target.getPos().add((double)0.0F, (double)mc.player.getStandingEyeHeight(), (double)0.0F);
            this.syncRenderRot();
         }
      }
   }

   private void trackAirState() {
      if (!mc.player.isFallFlying() && !mc.player.isClimbing() && !mc.player.isTouchingWater()) {
         if (!mc.player.isOnGround()) {
            ++this.airTicks;
         } else if (mc.player.getVelocity().y >= (double)0.0F) {
            this.airTicks = 0;
            this.lagging = false;
         }

      } else {
         this.airTicks = 0;
         this.syncRenderRot();
      }
   }

   private PlayerEntity findPlayer(double range) {
      PlayerEntity best = null;
      double bestDist = range;

      for(PlayerEntity player : mc.world.getPlayers()) {
         if (player != mc.player && player.isAlive() && !Arc.FRIEND.isFriend(player)) {
            double dist = (double)mc.player.distanceTo(player);
            if (dist <= bestDist) {
               bestDist = dist;
               best = player;
            }
         }
      }

      return best;
   }

   private boolean hasMace() {
      if (mc.player.getMainHandStack().getItem() == Items.MACE) {
         return true;
      } else {
         return this.inventorySwap.getValue() && this.findMaceSlot() != -1;
      }
   }

   private int findMaceSlot() {
      for(int i = 0; i < 9; ++i) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
            return i;
         }
      }

      return -1;
   }

   private void restoreSlot() {
   }

   private boolean isEnemyVisible(Entity entity) {
      return entity != null && mc.player.canSee(entity);
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (!this.attacking) {
            if (this.set.getValue() && this.groundSpoof.getValue()) {
               if (event.getPacket() instanceof PlayerMoveC2SPacket) {
                  PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket)event.getPacket();
                  if (packet.changesPosition()) {
                     ((IPlayerMoveC2SPacket)packet).setOnGround(false);
                  }
               }

            }
         }
      }
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (!nullCheck()) {
         if (this.renderMode.is(MaceAura.RenderMode.Normal)) {
            PlayerEntity renderTarget = this.set.getValue() ? this.setTarget : this.target;
            if (renderTarget != null && renderTarget.isAlive()) {
               Box box = renderTarget.getBoundingBox();
               event.drawBox(box, new Color(255, 80, 80, 120));
            }
         } else if (this.renderMode.is(MaceAura.RenderMode.Silent) && this.renderPos != null && this.setTarget != null) {
            Box box = new Box(this.renderPos.x - 0.3, this.renderPos.y, this.renderPos.z - 0.3, this.renderPos.x + 0.3, this.renderPos.y + 1.8, this.renderPos.z + 0.3);
            event.drawBox(box, this.silentPlayerBox.getValue());
         }

      }
   }

   public boolean isSilentRendering() {
      return this.isOn() && this.renderMode.is(MaceAura.RenderMode.Silent) && this.target != null && this.attacking;
   }

   public boolean shouldHideRotation() {
      return this.isOn() && this.renderMode.is(MaceAura.RenderMode.Silent) && this.target != null;
   }

   public Vec3d getSilentPos() {
      return this.renderPos;
   }

   public static enum LagMode {
      Fly,
      Smart;

      // $FF: synthetic method
      private static LagMode[] $values() {
         return new LagMode[]{Fly, Smart};
      }
   }

   public static enum RenderMode {
      Normal,
      Silent;

      // $FF: synthetic method
      private static RenderMode[] $values() {
         return new RenderMode[]{Normal, Silent};
      }
   }

   public static enum SwingMode {
      All,
      MainHand;

      // $FF: synthetic method
      private static SwingMode[] $values() {
         return new SwingMode[]{All, MainHand};
      }
   }
}
