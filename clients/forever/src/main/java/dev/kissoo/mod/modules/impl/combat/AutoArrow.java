package dev.kissoo.mod.modules.impl.combat;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.ClientTickEvent;
import dev.kissoo.api.events.impl.RotationEvent;
import dev.kissoo.api.events.impl.TickEvent;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Comparator;

public class AutoArrow extends Module {

    public static AutoArrow INSTANCE;

    private enum Page {
        AimBot, AutoBow, Release
    }

    private enum Priority {
        CROSSHAIR, DISTANCE
    }

    private final EnumSetting<Page> page = this.add(
            new EnumSetting<>("Page", Page.AimBot)
    );

    private final SliderSetting aimRange = this.add(
            new SliderSetting("AimRange", 60.0, 10.0, 120.0,
                    () -> page.is(Page.AimBot)).setSuffix("m")
    );
    private final BooleanSetting ignoreFriends = this.add(
            new BooleanSetting("IgnoreFriends", true,
                    () -> page.is(Page.AimBot))
    );
    private final SliderSetting rotateSpeed = this.add(
            new SliderSetting("RotateSpeed", 0.7, 0.1, 1.0, 0.01,
                    () -> page.is(Page.AimBot))
    );
    private final SliderSetting aimPredictTicks = this.add(
            new SliderSetting("PredictTicks", 3.0, 0.0, 10.0, 0.1,
                    () -> page.is(Page.AimBot)).setSuffix("t")
    );
    private final SliderSetting gravityComp = this.add(
            new SliderSetting("GravityComp", 0.03, 0.0, 0.2, 0.001,
                    () -> page.is(Page.AimBot))
    );
    private final EnumSetting<Priority> priority = this.add(
            new EnumSetting<>("Priority", Priority.CROSSHAIR,
                    () -> page.is(Page.AimBot))
    );

    private final SliderSetting bowRange = this.add(
            new SliderSetting("BowRange", 60.0, 10.0, 120.0,
                    () -> page.is(Page.AutoBow)).setSuffix("m")
    );

    private final BooleanSetting smart = this.add(
            new BooleanSetting("Smart", true,
                    () -> page.is(Page.Release))
    );
    private final SliderSetting releaseTicks = this.add(
            new SliderSetting("Ticks", 5, 0, 20,
                    () -> page.is(Page.Release) && !smart.getValue())
    );
    private final SliderSetting minTicks = this.add(
            new SliderSetting("MinTicks", 4, 0, 20,
                    () -> page.is(Page.Release) && smart.getValue())
    );
    private final SliderSetting maxTicks = this.add(
            new SliderSetting("MaxTicks", 20, 0, 20,
                    () -> page.is(Page.Release) && smart.getValue())
    );
    private final SliderSetting smartRange = this.add(
            new SliderSetting("SmartRange", 50, 5, 120,
                    () -> page.is(Page.Release) && smart.getValue())
    );
    private final BooleanSetting aimCheck = this.add(
            new BooleanSetting("AimCheck", true,
                    () -> page.is(Page.Release))
    );
    private final SliderSetting aimFov = this.add(
            new SliderSetting("AimFov", 3.0, 0.1, 20.0, 0.1,
                    () -> page.is(Page.Release) && aimCheck.getValue())
    );
    private final BooleanSetting tpsSync = this.add(
            new BooleanSetting("TPS-Sync", false,
                    () -> page.is(Page.Release))
    );
    private final BooleanSetting crossbow = this.add(
            new BooleanSetting("Crossbow", false,
                    () -> page.is(Page.Release))
    );
    private PlayerEntity currentTarget = null;
    private Vec3d aimVec = null;

    private int previousSlot = -1;
    private boolean switched = false;
    private boolean holding = false;

    public AutoArrow() {
        super("AutoArrow", Module.Category.Combat);
        this.setChinese("自动弓箭");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return currentTarget != null ? currentTarget.getName().getString() : null;
    }

    @Override
    public void onEnable() {
        currentTarget = null;
        aimVec = null;
        previousSlot = -1;
        switched = false;
        holding = false;
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        aimVec = null;
        if (mc.player != null) {
            if (holding) {
                mc.options.useKey.setPressed(false);
                holding = false;
            }
            if (switched && previousSlot != -1) {
                mc.player.getInventory().selectedSlot = previousSlot;
                previousSlot = -1;
                switched = false;
            }
        }
    }

    @EventListener
    public void onClientTick(ClientTickEvent event) {
        if (nullCheck()) return;
        if (event.isPost()) return;

        aimVec = null;

        if (!isPullingBow()) {
            currentTarget = null;
            return;
        }

        PlayerEntity target = findAimTarget();
        currentTarget = target;

        if (target != null) {
            Vec3d visible = getVisiblePoint(target);
            if (visible != null) {
                aimVec = calculateAimPosition(visible, target);
            }
        }
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (aimVec != null) {
            event.setTarget(aimVec, rotateSpeed.getValueFloat(), 10.0f);
        }
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (!event.isPost()) return;
        if (mc.player == null || mc.world == null) return;

        tickAutoBow();
        tickRelease();
    }

    private void tickAutoBow() {
        if (hasNearbyPlayer()) {
            if (mc.player.getMainHandStack().getItem() != Items.BOW) {
                int bowSlot = findBowSlot();
                if (bowSlot == -1) return;

                if (!switched) {
                    previousSlot = mc.player.getInventory().selectedSlot;
                }

                if (bowSlot < 9) {
                    mc.player.getInventory().selectedSlot = bowSlot;
                } else {
                    swapToHotbar(bowSlot, mc.player.getInventory().selectedSlot);
                }

                switched = true;
                return;
            }

            if (!holding) {
                if (mc.interactionManager != null) {
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                }
                mc.options.useKey.setPressed(true);
                holding = true;
            }
        } else {
            if (holding) {
                mc.options.useKey.setPressed(false);
                holding = false;
            }
        }
    }

    private void tickRelease() {
        if (mc.player == null) return;
        ItemStack mainhand = mc.player.getMainHandStack();

        if (mainhand.getItem() == Items.BOW) {
            if (!mc.player.isUsingItem()) return;
            float tpsOffset = tpsSync.getValue() ? getTPSOffset() : 0.0f;
            float needTicks = getRequiredTicks();

            if ((float) mc.player.getItemUseTime() + tpsOffset >= needTicks) {
                if (aimCheck.getValue() && aimVec != null) {
                    if (!Forever.ROTATION.inFov(aimVec, aimFov.getValueFloat())) {
                        return;
                    }
                }

                sendReleasePacket();
                mc.player.stopUsingItem();
            }

        } else if (crossbow.getValue()
                && mainhand.getItem() == Items.CROSSBOW
                && (float) mc.player.getItemUseTime()
                / (float) CrossbowItem.getPullTime(mainhand, mc.player) > 1.0f) {

            sendReleasePacket();
            mc.player.stopUsingItem();

            if (mc.interactionManager != null) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
        }
    }

    private void sendReleasePacket() {
        mc.getNetworkHandler().sendPacket(
                new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                        BlockPos.ORIGIN,
                        Direction.DOWN
                )
        );
    }

    private float getRequiredTicks() {
        if (!smart.getValue()) {
            return releaseTicks.getValueFloat();
        }
        if (aimVec == null) {
            return maxTicks.getValueFloat();
        }
        return getRecommendedBowChargeTicks(
                minTicks.getValueFloat(),
                maxTicks.getValueFloat(),
                smartRange.getValueFloat()
        );
    }

    public float getRecommendedBowChargeTicks(float minT, float maxT, float sRange) {
        if (mc.player == null || aimVec == null) return maxT;
        double distance = mc.player.getEyePos().distanceTo(aimVec);
        float ratio = (float) MathHelper.clamp(distance / sRange, 0.0, 1.0);
        return MathHelper.lerp(ratio, minT, maxT);
    }

    private float getTPSOffset() {
        try {
            return 20.0f - Forever.SERVER.getTPS();
        } catch (Exception e) {
            return 0.0f;
        }
    }

    private PlayerEntity findAimTarget() {
        if (mc.world == null) return null;
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player)
                .filter(PlayerEntity::isAlive)
                .filter(p -> !ignoreFriends.getValue() || !isFriend(p))
                .filter(p -> p.distanceTo(mc.player) <= aimRange.getValue())
                .filter(p -> getVisiblePoint(p) != null)
                .min(Comparator.comparingDouble(p -> {
                    if (priority.getValue() == Priority.DISTANCE) {
                        return p.distanceTo(mc.player);
                    }
                    Vec3d visible = getVisiblePoint(p);
                    if (visible == null) return Double.MAX_VALUE;
                    Vec3d dir = visible.subtract(mc.player.getEyePos()).normalize();
                    Vec3d look = mc.player.getRotationVec(1.0f);
                    return -look.dotProduct(dir);
                }))
                .orElse(null);
    }

    private Vec3d getVisiblePoint(PlayerEntity target) {
        Vec3d head = new Vec3d(target.getX(), target.getEyeY(), target.getZ());
        if (isVisible(head)) return head;

        Vec3d chest = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ());
        if (isVisible(chest)) return chest;

        Vec3d feet = target.getPos();
        if (isVisible(feet)) return feet;

        return null;
    }

    private boolean isVisible(Vec3d pos) {
        Vec3d eyePos = mc.player.getEyePos();
        RaycastContext ctx = new RaycastContext(
                eyePos, pos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        );
        HitResult result = mc.world.raycast(ctx);
        return result.getType() == HitResult.Type.MISS;
    }

    private Vec3d calculateAimPosition(Vec3d targetPos, PlayerEntity target) {
        Vec3d result = targetPos;
        Vec3d velocity = target.getVelocity();
        Vec3d hVel = new Vec3d(velocity.x, 0.0, velocity.z);

        if (hVel.lengthSquared() > 0.0001 && aimPredictTicks.getValue() > 0.0) {
            result = result.add(hVel.multiply(aimPredictTicks.getValue()));
        }

        double distance = mc.player.getEyePos().distanceTo(result);
        double yComp = distance * gravityComp.getValue();
        result = result.add(0.0, yComp, 0.0);

        return result;
    }

    private boolean isPullingBow() {
        return mc.player.isUsingItem()
                && mc.player.getActiveItem().getItem() == Items.BOW;
    }

    private boolean hasNearbyPlayer() {
        if (mc.world == null) return false;
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player)
                .filter(PlayerEntity::isAlive)
                .anyMatch(p -> p.distanceTo(mc.player) <= bowRange.getValue());
    }

    private int findBowSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.BOW) return i;
        }
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.BOW) return i;
        }
        return -1;
    }

    private void swapToHotbar(int from, int hotbarSlot) {
        if (mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                from,
                hotbarSlot,
                SlotActionType.SWAP,
                mc.player
        );
    }

    private boolean isFriend(PlayerEntity player) {
        try {
            return Forever.FRIEND.isFriend(player);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isActive() {
        return this.isOn() && (holding || switched);
    }

    public PlayerEntity getCurrentTarget() {
        return currentTarget;
    }

    public Vec3d getAimVec() {
        return aimVec;
    }
}