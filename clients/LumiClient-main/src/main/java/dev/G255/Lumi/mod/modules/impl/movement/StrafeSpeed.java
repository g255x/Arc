package dev.G255.Lumi.mod.modules.impl.movement;

import dev.G255.Lumi.Lumi;
import dev.G255.Lumi.api.events.impl.MoveEvent;
import dev.G255.Lumi.api.events.impl.MovedEvent;
import dev.G255.Lumi.api.events.impl.UpdateEvent;
import dev.G255.Lumi.api.events.impl.PacketEvent.Receive;
import dev.G255.Lumi.api.utils.math.Timer;
import dev.G255.Lumi.api.utils.path.BaritoneUtil;
import dev.G255.Lumi.api.utils.player.EntityUtil;
import dev.G255.Lumi.api.utils.player.MovementUtil;
import dev.G255.Lumi.api.utils.world.BlockPosX;
import dev.G255.Lumi.api.utils.world.BlockUtil;
import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.Module.Category;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.SliderSetting;
import dev.G255.Lumi.api.events.eventbus.EventListener;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class StrafeSpeed extends Module {
    public static StrafeSpeed INSTANCE;
    private final BooleanSetting inWater = this.add(new BooleanSetting("InWater", false));
    private final BooleanSetting inBlock = this.add(new BooleanSetting("InBlock", false).setParent());
    private final SliderSetting inBlockSpeed = this.add(new SliderSetting("InBlockSpeed", 0.4, 0.1, 2.0, 0.01, this.inBlock::isOpen).setSuffix("b/t"));
    private final BooleanSetting airStop = this.add(new BooleanSetting("AirStop", false));
    private final BooleanSetting jump = this.add(new BooleanSetting("Jump", true));
    private final SliderSetting strafeSpeed = this.add(new SliderSetting("Speed", 0.2873, 0.0, 1.0, 1.0E-4));
    private final BooleanSetting explosions = this.add(new BooleanSetting("ExplosionsBoost", false));
    private final SliderSetting hFactor = this.add(new SliderSetting("H-Factor", 1.0, 0.0, 5.0, 0.01));
    private final SliderSetting coolDown = this.add(new SliderSetting("CoolDown", 1000.0, 0.0, 5000.0, 1.0));
    private final SliderSetting airThreshold = this.add(new SliderSetting("AirThreshold", 0.0, -0.5, 0.5, 0.01).setSuffix("b/t"));
    private final BooleanSetting airStrafe = this.add(new BooleanSetting("AirStrafe", false));
    private final SliderSetting airSpeed = this.add(new SliderSetting("AirSpeed", 0.5, 0.1, 2.0, 0.01, this.airStrafe::getValue));
    private final SliderSetting airCooldown = this.add(new SliderSetting("AirCooldown", 2.0, 0.0, 30.0, 0.5, this.airStrafe::getValue).setSuffix("s"));
    private final BooleanSetting airSlowFall = this.add(new BooleanSetting("AirSlowFall", false, this.airStrafe::getValue));
    private final SliderSetting airFallSpeed = this.add(
        new SliderSetting("AirFallSpeed", 0.05, 0.01, 0.5, 0.01, () -> this.airStrafe.getValue() && this.airSlowFall.getValue()).setSuffix("b/t")
    );
    private final SliderSetting airMinHeight = this.add(new SliderSetting("AirMinHeight", 5.0, 0.0, 20.0, 0.5, this.airStrafe::getValue).setSuffix("blocks"));
    private final BooleanSetting noLag = this.add(new BooleanSetting("NoLag", true).setParent());
    private final SliderSetting lagCount = this.add(new SliderSetting("LagCount", 2.0, 1.0, 10.0, 1.0, this.noLag::isOpen));
    private final SliderSetting lagDisableTime = this.add(new SliderSetting("LagDisable", 5.0, 1.0, 30.0, 1.0, this.noLag::isOpen).setSuffix("s"));
    private final Timer expTimer = new Timer();
    private final Timer lagDisableTimer = new Timer();
    private final Timer lagWindowTimer = new Timer();
    private final Timer airCooldownTimer = new Timer();
    private boolean wasOnGround = true;
    private boolean airStrafeActive = false;
    private boolean airStrafeApplied = false;
    private boolean stop;
    private double speed;
    private double getDistance;
    private int stage;
    private double lastExp;
    private boolean boost;
    private double targetSpeed;
    private double smoothFactor = 0.3;
    private int lagCounter;
    private boolean lagDisabled;
    private static final double STAGE1_MULTIPLIER = 1.35;
    private static final double STAGE1_OFFSET = 0.01;
    private static final double STAGE2_BOOST_HIGH = 1.6835;
    private static final double STAGE2_BOOST_LOW = 1.395;
    private static final double STAGE3_FRICTION = 0.66;
    private static final double DECAY_RATE = 159.0;
    private static final double HEIGHT_CHECK_START = 0.2;
    private static final double HEIGHT_CHECK_STEP = 0.05;
    private static final double HEIGHT_CHECK_MAX = 1.0;

    public StrafeSpeed() {
        super("StrafeSpeed", "Strafe speed with explosion boost", Category.Movement);
        this.setChinese("");
        this.setChineseDesc("");
        INSTANCE = this;

        try {
            if (Lumi.MODULE != null && Lumi.MODULE.getModuleByName("WindLongJump") == null) {
                Lumi.MODULE.addModule(new WindLongJump());
            }
        } catch (Exception var2) {
        }
    }

    public void onEnable() {
        if (mc.player != null) {
            this.speed = MovementUtil.getSpeed(false);
            this.getDistance = MovementUtil.getDistance2D();
            this.targetSpeed = this.speed;
        }

        this.stage = 4;
        this.lagCounter = 0;
        this.lagDisabled = false;
    }

    @EventListener(
        priority = 100
    )
    public void onPacketReceive(Receive var1) {
        if (!BaritoneUtil.isActive()) {
            if (var1.getPacket() instanceof ExplosionS2CPacket var3 && this.explosions.getValue()) {
                Vec3d var4 = new Vec3d(var3.getX(), var3.getY(), var3.getZ());
                if (mc.player.getEyePos().distanceTo(var4) < 15.0) {
                    double var5 = Math.sqrt((double)(var3.getPlayerVelocityX() * var3.getPlayerVelocityX() + var3.getPlayerVelocityZ() * var3.getPlayerVelocityZ()));
                    this.lastExp = this.expTimer.passed((long)this.coolDown.getValueInt()) ? var5 : var5 - this.lastExp;
                    double var7 = this.lastExp;
                    if (this.lastExp > 0.0) {
                        this.expTimer.reset();
                        this.speed = this.speed + this.lastExp * this.hFactor.getValue();
                        this.getDistance = this.getDistance + this.lastExp * this.hFactor.getValue();
                    }
                }
            }

            if (var1.getPacket() instanceof EntityVelocityUpdateS2CPacket) {
                this.resetStrafe();
                if (this.noLag.getValue()) {
                    this.handleLagDetection();
                }
            }
        }
    }

    private void handleLagDetection() {
        if (!this.lagDisabled) {
            if (!this.lagWindowTimer.passed(3000L)) {
                this.lagCounter++;
            } else {
                this.lagCounter = 1;
                this.lagWindowTimer.reset();
            }

            if (this.lagCounter >= this.lagCount.getValueInt()) {
                this.lagDisabled = true;
                this.lagDisableTimer.reset();
                this.lagCounter = 0;
            }
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent var1) {
        if (this.lagDisabled && this.lagDisableTimer.passed((long)this.lagDisableTime.getValueInt() * 1000L)) {
            this.lagDisabled = false;
        }
    }

    @EventListener
    public void onMove(MovedEvent var1) {
        if (!nullCheck()) {
            double var2 = mc.player.getX() - mc.player.prevX;
            double var4 = mc.player.getZ() - mc.player.prevZ;
            this.getDistance = Math.sqrt(var2 * var2 + var4 * var4);
        }
    }

    @EventListener
    public void onMove(MoveEvent var1) {
        if (!this.lagDisabled) {
            if (Fly.INSTANCE == null || !Fly.INSTANCE.isOn()) {
                if (WindLongJump.INSTANCE == null || !WindLongJump.INSTANCE.isOn()) {
                    if (!MovementUtil.isMoving()) {
                        if (this.airStop.getValue()) {
                            MovementUtil.setMotionX(0.0);
                            MovementUtil.setMotionZ(0.0);
                        }

                        var1.setX(0.0);
                        var1.setZ(0.0);
                        this.resetStrafe();
                        this.stop = true;
                    } else if ((this.inWater.getValue() || !mc.player.isInLava() && !mc.player.isTouchingWater() && !mc.player.isSubmergedInWater())
                        && !mc.player.isHoldingOntoLadder()
                        && !mc.player.isClimbing()
                        && (this.inBlock.getValue() || !EntityUtil.isInsideBlock())
                        && !mc.player.getAbilities().flying
                        && !mc.player.isFallFlying()) {
                        if (this.stop) {
                            this.stop = false;
                        } else {
                            boolean var6 = mc.player.isOnGround();
                            if (this.airStrafe.getValue()) {
                                if (!var6 && this.wasOnGround) {
                                    if (!(this.airCooldown.getValue() <= 0.0) && !this.airCooldownTimer.passed((long)(this.airCooldown.getValue() * 1000.0))) {
                                        this.airStrafeActive = false;
                                    } else {
                                        this.airStrafeActive = true;
                                    }

                                    this.airStrafeApplied = false;
                                }

                                if (var6) {
                                    if (!this.wasOnGround && this.airStrafeApplied) {
                                        this.airCooldownTimer.reset();
                                    }

                                    this.airStrafeActive = false;
                                    this.airStrafeApplied = false;
                                }

                                this.wasOnGround = var6;
                                if (!var6 && this.airStrafeActive) {
                                    if (this.airSlowFall.getValue()) {
                                        double var7 = MovementUtil.getMotionY();
                                        if (var7 < 0.0) {
                                            MovementUtil.setMotionY(Math.max(var7, -this.airFallSpeed.getValue()));
                                        }
                                    }

                                    boolean var16 = false;
                                    double var8 = this.airMinHeight.getValue();
                                    int var10 = (int)mc.player.getY();

                                    for (int var11 = 1; var11 <= (int)(var8 + 5.0); var11++) {
                                        BlockPosX var12 = new BlockPosX(mc.player.getX(), (double)(var10 - var11), mc.player.getZ());
                                        if (mc.world.getBlockState(var12).isAir()) {
                                            double var13 = mc.player.getY() - (double)(var10 - var11 + 1);
                                            var16 = var13 < var8;
                                            break;
                                        }
                                    }

                                    if (!var16) {
                                        float var19 = mc.player.input.movementForward;
                                        float var21 = mc.player.input.movementSideways;
                                        if (var19 != 0.0F || var21 != 0.0F) {
                                            double[] var22 = MovementUtil.directionSpeed(this.airSpeed.getValue());
                                            var1.setX(var22[0]);
                                            var1.setZ(var22[1]);
                                            this.airStrafeApplied = true;
                                        }
                                    }

                                    return;
                                }
                            } else {
                                this.wasOnGround = var6;
                            }

                            if (mc.player.isOnGround() || !(mc.player.getVelocity().y <= this.airThreshold.getValue())) {
                                double var4 = EntityUtil.isInsideBlock()
                                    ? this.inBlockSpeed.getValue()
                                    : MovementUtil.getSpeed(false, this.strafeSpeed.getValue());
                                if (this.stage == 1) {
                                    this.targetSpeed = 1.35 * var4 - 0.01;
                                } else if (this.stage != 2 || !mc.player.isOnGround() || !mc.options.forwardKey.isPressed() && !this.jump.getValue()
                                    )
                                 {
                                    if (this.stage == 3) {
                                        this.targetSpeed = 0.33999999999999997 * this.getDistance + 0.66 * var4;
                                        this.boost = !this.boost;
                                    } else {
                                        boolean var9 = false;

                                        for (double var18 = 0.2; var18 <= 1.0; var18 += 0.05) {
                                            if (BlockUtil.canCollide(null, mc.player.getBoundingBox().offset(0.0, var18, 0.0))) {
                                                var9 = true;
                                                break;
                                            }
                                        }

                                        if ((var9 || mc.player.horizontalCollision) && this.stage > 0) {
                                            this.stage = 1;
                                        }

                                        this.targetSpeed = this.getDistance * 0.9937106918238994;
                                    }
                                } else {
                                    double var2 = 0.3999 + MovementUtil.getJumpSpeed();
                                    MovementUtil.setMotionY(var2);
                                    var1.setY(var2);
                                    this.targetSpeed = this.targetSpeed * (this.boost ? 1.6835 : 1.395);
                                }

                                this.speed = this.speed + (this.targetSpeed - this.speed) * this.smoothFactor;
                                this.speed = Math.max(this.speed, var4);
                                this.speed = Math.min(this.speed, 10.0);
                                double var15 = (double)mc.player.input.movementForward;
                                double var17 = (double)mc.player.input.movementSideways;
                                double var20 = (double)mc.player.getYaw();
                                if (var15 == 0.0 && var17 == 0.0) {
                                    var1.setX(0.0);
                                    var1.setZ(0.0);
                                } else if (var15 != 0.0 && var17 != 0.0) {
                                    var15 *= Math.sin(Math.PI / 4);
                                    var17 *= Math.cos(Math.PI / 4);
                                }

                                var1.setX((var15 * this.speed * -Math.sin(Math.toRadians(var20)) + var17 * this.speed * Math.cos(Math.toRadians(var20))) * 0.99);
                                var1.setZ((var15 * this.speed * Math.cos(Math.toRadians(var20)) - var17 * this.speed * -Math.sin(Math.toRadians(var20))) * 0.99);
                                this.stage++;
                            }
                        }
                    } else {
                        this.resetStrafe();
                        this.stop = true;
                    }
                }
            }
        }
    }

    public void resetStrafe() {
        this.stage = 4;
        this.speed = 0.0;
        this.targetSpeed = 0.0;
        this.getDistance = 0.0;
    }

    public String getInfo() {
        return this.lagDisabled ? "LagPause" : null;
    }
}
