package dev.kissoo.mod.modules.impl.movement;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.player.EntityUtil;
import dev.kissoo.api.utils.player.InventoryUtil;
import dev.kissoo.api.utils.player.MovementUtil;
import dev.kissoo.api.utils.world.BlockUtil;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.Module.Category;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import hoprc.api.events.eventbus.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;

public class WindLongJump extends Module {
    public static WindLongJump INSTANCE;
    private final SliderSetting throwDelay = this.add(new SliderSetting("ThrowDelay", 70.0, 0.0, 1000.0, 10.0).setSuffix(" ms"));
    private final SliderSetting upSpeed = this.add(new SliderSetting("UpSpeed", 5.0, 0.1, 10.0, 0.1));
    private final SliderSetting forwardSpeed = this.add(new SliderSetting("ForwardSpeed", 3.0, 0.5, 20.0, 0.1));
    private final SliderSetting maintainTime = this.add(new SliderSetting("MaintainTime", 50.0, 10.0, 500.0, 10.0).setSuffix(" ms"));
    private final BooleanSetting antiKick = this.add(new BooleanSetting("AntiKick", true));
    private final Timer stopTimer = new Timer();
    private long boostStartTime;
    private boolean thrown;
    private boolean jumped;
    private boolean stopping;

    public WindLongJump() {
        super("HighJump", "Vertical wind charge jump", Category.Movement);
        this.setChinese("高跳");
        INSTANCE = this;
    }

    public void onEnable() {
        if (!nullCheck()) {
            this.thrown = false;
            this.jumped = false;
            this.boostStartTime = 0L;
            this.stopping = false;
            if (!this.throwCharge()) {
                this.disable();
            } else {
                this.thrown = true;
                this.boostStartTime = System.currentTimeMillis();
            }
        }
    }

    public void onDisable() {
        this.thrown = false;
        this.boostStartTime = 0L;
        this.stopping = false;
        if (mc.player != null) {
            MovementUtil.setMotionX(0.0);
            MovementUtil.setMotionZ(0.0);
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent var1) {
        if (!nullCheck()) {
            if (this.stopping) {
                if (this.antiKick.getValue() && this.stopTimer.passed(500L)) {
                    BlockUtil.clickBlock(mc.player.getBlockPos().down(), Direction.UP, false, Hand.MAIN_HAND);
                    this.disable();
                } else if (!this.antiKick.getValue()) {
                    this.disable();
                }
            } else if (this.thrown && this.boostStartTime != 0L) {
                long var2 = System.currentTimeMillis() - this.boostStartTime;
                long var4 = (long)this.throwDelay.getValue();
                long var6 = var4 + (long)this.maintainTime.getValue();
                if (var2 >= var4 && var2 < var6) {
                    if (!this.jumped) {
                        if (mc.player.isOnGround()) {
                            mc.player.jump();
                        }

                        MovementUtil.setMotionY(this.upSpeed.getValue());
                        this.jumped = true;
                    }

                    double var8 = Math.toRadians((double)mc.player.getYaw());
                    double var10 = this.forwardSpeed.getValue();
                    MovementUtil.setMotionX(-Math.sin(var8) * var10);
                    MovementUtil.setMotionZ(Math.cos(var8) * var10);
                } else if (var2 >= var6) {
                    MovementUtil.setMotionX(0.0);
                    MovementUtil.setMotionZ(0.0);
                    this.stopping = true;
                    this.stopTimer.reset();
                }
            }
        }
    }

    private boolean throwCharge() {
        int var1 = -1;
        boolean var2 = false;
        if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
            var1 = mc.player.getInventory().selectedSlot;
        } else {
            var1 = InventoryUtil.findItem(Items.WIND_CHARGE);
            if (var1 == -1 && (var1 = InventoryUtil.findItemInventorySlotFromZero(Items.WIND_CHARGE)) != -1) {
                var2 = true;
            }
        }

        if (var1 == -1) {
            return false;
        } else {
            float var3 = mc.player.getYaw();
            Forever.ROTATION.snapBack();
            Forever.ROTATION.snapAt(var3, 90.0F);
            if (mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
                sendSequencedPacket(var1x -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var1x, var3, 90.0F));
                mc.player.swingHand(Hand.MAIN_HAND);
            } else if (var2) {
                InventoryUtil.inventorySwap(var1, mc.player.getInventory().selectedSlot);
                sendSequencedPacket(var1x -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var1x, var3, 90.0F));
                mc.player.swingHand(Hand.MAIN_HAND);
                InventoryUtil.inventorySwap(var1, mc.player.getInventory().selectedSlot);
                EntityUtil.syncInventory();
            } else {
                int var4 = mc.player.getInventory().selectedSlot;
                InventoryUtil.switchToSlot(var1);
                sendSequencedPacket(var1x -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, var1x, var3, 90.0F));
                mc.player.swingHand(Hand.MAIN_HAND);
                InventoryUtil.switchToSlot(var4);
            }

            Forever.ROTATION.snapBack();
            return true;
        }
    }
}
