package dev.baimaodaxia.mod.modules.impl.movement;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.combat.CombatUtil;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.api.utils.path.PathUtils;
import dev.baimaodaxia.api.utils.path.TPUtils;
import dev.baimaodaxia.api.utils.path.Vec3;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.BindSetting;
import dev.baimaodaxia.mod.modules.settings.impl.BooleanSetting;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TpAura extends Module {
   public static TpAura INSTANCE;
   private final EnumSetting<TpMode> tpMode;
   private final BooleanSetting keyMode;
   private final BindSetting key;
   private final SliderSetting range;
   private final SliderSetting delay;
   private final SliderSetting moveDistance;
   private final SliderSetting up;
   private final EnumSetting<TPUtils.TeleportType> tpType;
   private final BooleanSetting autoDisable;
   private final SliderSetting stayTime;
   private final Timer timer;
   private final Timer stayTimer;
   private Vec3d originalPos;
   private boolean isAtTarget;

   public TpAura() {
      super("TargetTP", Module.Category.Movement);
      this.tpMode = this.add(new EnumSetting("Mode", TpAura.TpMode.Normal));
      this.keyMode = this.add(new BooleanSetting("KeyMode", false));
      BooleanSetting var10006 = this.keyMode;
      Objects.requireNonNull(var10006);
      this.key = this.add(new BindSetting("TeleportKey", -1, var10006::getValue));
      this.range = this.add(new SliderSetting("Range", (double)114.0F, (double)1.0F, (double)1024.0F, (double)1.0F));
      this.delay = this.add(new SliderSetting("Delay", (double)0.0F, (double)0.0F, (double)1000.0F, (double)1.0F));
      this.moveDistance = this.add(new SliderSetting("MoveDistance", (double)10.0F, (double)1.0F, (double)50.0F, (double)1.0F));
      this.up = this.add(new SliderSetting("UP", (double)0.0F, (double)0.0F, (double)50.0F, (double)1.0F));
      this.tpType = this.add(new EnumSetting("TPType", TPUtils.TeleportType.New));
      this.autoDisable = this.add(new BooleanSetting("AutoDisable", true));
      this.stayTime = this.add((new SliderSetting("StayTime", (double)500.0F, (double)50.0F, (double)3000.0F, (double)50.0F)).setSuffix("ms"));
      this.timer = new Timer();
      this.stayTimer = new Timer();
      this.originalPos = null;
      this.isAtTarget = false;
      this.setChinese("目标传送");
      INSTANCE = this;
   }

   public String getInfo() {
      PlayerEntity target = CombatUtil.getClosestEnemy(this.range.getValue());
      return target == null ? null : target.getName().getString();
   }

   public void onDisable() {
      this.originalPos = null;
      this.isAtTarget = false;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         if (!this.keyMode.getValue() || this.key.isPressed()) {
            if (this.isAtTarget) {
               if (this.stayTimer.passed((long)this.stayTime.getValue())) {
                  this.teleportBack();
                  this.isAtTarget = false;
                  this.originalPos = null;
                  if (this.autoDisable.getValue()) {
                     this.disable();
                  }
               }

            } else {
               PlayerEntity target = CombatUtil.getClosestEnemy(this.range.getValue());
               if (target != null) {
                  if (this.timer.passed((long)this.delay.getValue())) {
                     this.timer.reset();
                     Vec3d targetPos = target.getPos().add((double)0.0F, this.up.getValue(), (double)0.0F);
                     if (this.tpMode.getValue() == TpAura.TpMode.FakeTP) {
                        this.originalPos = mc.player.getPos();
                        this.teleportTo(targetPos);
                        this.isAtTarget = true;
                        this.stayTimer.reset();
                     } else {
                        switch ((TPUtils.TeleportType)this.tpType.getValue()) {
                           case New -> this.teleportNew(targetPos);
                           case Legacy -> this.teleportLegacy(targetPos);
                        }

                        if (this.autoDisable.getValue()) {
                           this.disable();
                        }
                     }

                  }
               }
            }
         }
      }
   }

   private void teleportTo(Vec3d targetPos) {
      switch ((TPUtils.TeleportType)this.tpType.getValue()) {
         case New -> this.teleportNew(targetPos);
         case Legacy -> this.teleportLegacy(targetPos);
      }

   }

   private void teleportBack() {
      if (this.originalPos != null) {
         switch ((TPUtils.TeleportType)this.tpType.getValue()) {
            case New -> this.teleportNew(this.originalPos);
            case Legacy -> this.teleportLegacy(this.originalPos);
         }

      }
   }

   private void teleportNew(Vec3d targetPos) {
      Vec3d playerPos = mc.player.getPos();
      double distance = playerPos.distanceTo(targetPos);
      int packetsRequired = (int)Math.ceil(distance / this.moveDistance.getValue()) - 1;

      for(int i = 0; i < packetsRequired; ++i) {
         mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
      }

      mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(targetPos.x, targetPos.y, targetPos.z, true));
      mc.player.setPosition(targetPos);
   }

   private void teleportLegacy(Vec3d targetPos) {
      ArrayList<Vec3> path = PathUtils.computePath(targetPos);
      if (!path.isEmpty()) {
         path.removeFirst();

         for(Vec3 vec3 : path) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(vec3.x(), vec3.y(), vec3.z(), false));
         }

         mc.player.setPosition(targetPos.x, targetPos.y, targetPos.z);
      }
   }

   public static enum TpMode {
      Normal,
      FakeTP;

      // $FF: synthetic method
      private static TpMode[] $values() {
         return new TpMode[]{Normal, FakeTP};
      }
   }
}
