package dev.perish.mod.modules.impl.combat;

import dev.perish.api.events.eventbus.EventListener;
import dev.perish.api.events.impl.PacketEvent;
import dev.perish.api.events.impl.UpdateEvent;
import dev.perish.Perish;
import dev.perish.api.utils.player.EntityUtil;
import dev.perish.api.utils.player.MovementUtil;
import dev.perish.api.utils.world.BlockUtil;
import dev.perish.asm.accessors.IPlayerMoveC2SPacket;
import dev.perish.mod.modules.Module;
import dev.perish.mod.modules.impl.exploit.Blink;
import dev.perish.mod.modules.impl.exploit.BowBomb;
import dev.perish.mod.modules.impl.exploit.Phase;
import dev.perish.mod.modules.impl.player.AutoPearl;
import dev.perish.mod.modules.settings.impl.BooleanSetting;
import dev.perish.mod.modules.settings.impl.EnumSetting;
import dev.perish.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.Box;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.item.Items;

public class Criticals extends Module {
    public static Criticals INSTANCE;
    public final EnumSetting<Criticals.Mode> mode = this.add(new EnumSetting("Mode", Criticals.Mode.OldNCP));
    public final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", true, () -> !this.mode.is(Criticals.Mode.Ground)));
    public final BooleanSetting onlyStatic = this.add(new BooleanSetting("OnlyStatic", true, () -> this.isGrimMode()));
    public final BooleanSetting onlyInside = this.add(new BooleanSetting("OnlyInside", true, () -> this.isGrimMode()));
    public final BooleanSetting pauseMace = this.add(new BooleanSetting("PauseMace", true, () -> this.isGrimMode()));
    public final BooleanSetting fullPacket = this.add(new BooleanSetting("FullPacket", false, () -> this.isGrimMode()));
    public final BooleanSetting packet1 = this.add(new BooleanSetting("Packet1", false, () -> this.mode.is(Criticals.Mode.GrimOption)).setParent());
    public final SliderSetting spoofY1 = this.add(new SliderSetting("SpoofY1", 0.0, 0.0, 0.2, 1.0E-4, () -> this.mode.is(Criticals.Mode.GrimOption) && this.packet1.isOpen()));
    public final BooleanSetting onGround1 = this.add(new BooleanSetting("onGround1", false, () -> this.mode.is(Criticals.Mode.GrimOption) && this.packet1.isOpen()));
    public final BooleanSetting packet2 = this.add(new BooleanSetting("Packet2", false, () -> this.mode.is(Criticals.Mode.GrimOption)).setParent());
    public final SliderSetting spoofY2 = this.add(new SliderSetting("SpoofY2", 0.0, 0.0, 0.2, 1.0E-4, () -> this.mode.is(Criticals.Mode.GrimOption) && this.packet2.isOpen()));
    public final BooleanSetting onGround2 = this.add(new BooleanSetting("onGround2", false, () -> this.mode.is(Criticals.Mode.GrimOption) && this.packet2.isOpen()));
    public final BooleanSetting packet3 = this.add(new BooleanSetting("Packet3", false, () -> this.mode.is(Criticals.Mode.GrimOption)).setParent());
    public final SliderSetting spoofY3 = this.add(new SliderSetting("SpoofY3", 0.0, 0.0, 0.2, 1.0E-4, () -> this.mode.is(Criticals.Mode.GrimOption) && this.packet3.isOpen()));
    public final BooleanSetting onGround3 = this.add(new BooleanSetting("onGround3", false, () -> this.mode.is(Criticals.Mode.GrimOption) && this.packet3.isOpen()));
    public final BooleanSetting packet4 = this.add(new BooleanSetting("Packet4", false, () -> this.mode.is(Criticals.Mode.GrimOption)).setParent());
    public final SliderSetting spoofY4 = this.add(new SliderSetting("SpoofY4", 0.0, 0.0, 0.2, 1.0E-4, () -> this.mode.is(Criticals.Mode.GrimOption) && this.packet4.isOpen()));
    public final BooleanSetting onGround4 = this.add(new BooleanSetting("onGround4", false, () -> this.mode.is(Criticals.Mode.GrimOption) && this.packet4.isOpen()));
    public final BooleanSetting packet5 = this.add(new BooleanSetting("Packet5", false, () -> this.mode.is(Criticals.Mode.GrimOption)).setParent());
    public final SliderSetting spoofY5 = this.add(new SliderSetting("SpoofY5", 0.0, 0.0, 0.2, 1.0E-4, () -> this.mode.is(Criticals.Mode.GrimOption) && this.packet5.isOpen()));
    public final BooleanSetting onGround5 = this.add(new BooleanSetting("onGround5", false, () -> this.mode.is(Criticals.Mode.GrimOption) && this.packet5.isOpen()));
    private final BooleanSetting setOnGround = this.add(new BooleanSetting("SetNoGround", false, () -> this.mode.is(Criticals.Mode.Ground)));
    private final BooleanSetting blockCheck = this.add(new BooleanSetting("BlockCheck", true, () -> this.mode.is(Criticals.Mode.Ground)));
    private final BooleanSetting autoJump = this.add(new BooleanSetting("AutoJump", true, () -> this.mode.is(Criticals.Mode.Ground)).setParent());
    private final BooleanSetting mini = this.add(new BooleanSetting("Mini", true, () -> this.mode.is(Criticals.Mode.Ground) && this.autoJump.isOpen()));
    private final SliderSetting y = this.add(
            new SliderSetting("MotionY", 0.05, 0.0, 1.0, 1.0E-10, () -> this.mode.is(Criticals.Mode.Ground) && this.autoJump.isOpen())
    );
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true, () -> this.mode.is(Criticals.Mode.Ground)));
    private final BooleanSetting crawlingDisable = this.add(new BooleanSetting("CrawlingDisable", true, () -> this.mode.is(Criticals.Mode.Ground)));
    private final BooleanSetting flight = this.add(new BooleanSetting("Flight", false, () -> this.mode.is(Criticals.Mode.Ground)));
    boolean requireJump = false;

    public Criticals() {
        super("Criticals", Module.Category.Combat);
        this.setChinese("刀刀暴击");
        INSTANCE = this;
    }

    public static Entity getEntity(PlayerInteractEntityC2SPacket packet) {
        return mc.world == null ? null : mc.world.getEntityById(packet.entityId);
    }

    public static InteractType getInteractType(PlayerInteractEntityC2SPacket packet) {
        return packet.type.getType();
    }

    @Override
    public String getInfo() {
        return this.mode.getValue().name();
    }

    private boolean isGrimMode() {
        return this.mode.is(Criticals.Mode.GrimV2) || this.mode.is(Criticals.Mode.GrimV3) || this.mode.is(Criticals.Mode.GrimOption);
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        if (!event.isCancelled()) {
            if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                if (this.mode.is(Criticals.Mode.Ground)) {
                    if (!BowBomb.send) {
                        if (!AutoPearl.throwing && !Phase.INSTANCE.isOn()) {
                            if (this.setOnGround.getValue()) {
                                if (event.getPacket() instanceof PlayerMoveC2SPacket) {
                                    ((IPlayerMoveC2SPacket) event.getPacket()).setOnGround(false);
                                }
                            }
                        }
                    }
                } else {
                    if (mc.player != null && this.pauseMace.getValue() && mc.player.getMainHandStack().getItem() == Items.MACE) {
                        return;
                    }
                    Entity entity;
                    if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet
                            && getInteractType(packet) == InteractType.ATTACK
                            && !((entity = getEntity(packet)) instanceof EndCrystalEntity)
                            && (!this.onlyGround.getValue() || mc.player.isOnGround() || mc.player.getAbilities().flying)
                            && !mc.player.isInLava()
                            && !mc.player.isTouchingWater()
                            && entity != null) {
                        this.doCrit(entity);
                    }
                }
            }
        }
    }

    @Override
    public void onLogout() {
        if (this.mode.is(Criticals.Mode.Ground) && this.autoDisable.getValue()) {
            this.disable();
        }
    }

    @Override
    public void onEnable() {
        if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
            if (mc.player != null) {
                this.requireJump = true;
                if (this.mode.is(Criticals.Mode.Ground)) {
                    if (nullCheck()) {
                        if (this.autoDisable.getValue()) {
                            this.disable();
                        }
                    } else if (MovementUtil.isMoving() && this.autoDisable.getValue()) {
                        this.disable();
                    } else if (this.crawlingDisable.getValue() && mc.player.isCrawling()) {
                        this.disable();
                    } else if (mc.player.isOnGround()
                            && this.autoJump.getValue()
                            && (!this.blockCheck.getValue() || BlockUtil.canCollide(mc.player, new Box(EntityUtil.getPlayerPos(true).up(2))))) {
                        this.jump();
                    }
                }
            }
        }
    }

    public void jump() {
        if (this.mini.getValue()) {
            MovementUtil.setMotionY(this.y.getValue());
        } else {
            mc.player.jump();
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
            if (mc.player != null) {
                if (this.mode.is(Criticals.Mode.Ground)) {
                    if (this.crawlingDisable.getValue() && mc.player.isCrawling()) {
                        this.disable();
                    } else if (MovementUtil.isMoving() && this.autoDisable.getValue()) {
                        this.disable();
                    } else if (this.flight.getValue() && mc.player.fallDistance > 0.0F) {
                        MovementUtil.setMotionY(0.0);
                        MovementUtil.setMotionX(0.0);
                        MovementUtil.setMotionZ(0.0);
                        this.requireJump = false;
                    } else if (this.blockCheck.getValue() && !BlockUtil.canCollide(mc.player, new Box(EntityUtil.getPlayerPos(true).up(2)))) {
                        this.requireJump = true;
                    } else if (mc.player != null && mc.player.isOnGround() && this.autoJump.getValue() && (this.flight.getValue() || this.requireJump)) {
                        this.jump();
                        this.requireJump = false;
                    }
                }
            }
        }
    }

    public void doCrit(Entity entity) {
        if (this.isGrimMode()) {
            if ((MovementUtil.isStatic() || !this.onlyStatic.getValue()) && (Perish.ROTATION.directionVec != null || !this.onlyInside.getValue())) {
                switch (this.mode.getValue()) {
                    case GrimV2:
                        if (this.fullPacket.getValue()) {
                            float yaw = Perish.ROTATION.rotationYaw;
                            float pitch = Perish.ROTATION.rotationPitch;
                            if (mc.player != null) {
                                mc.getNetworkHandler().sendPacket(new Full(mc.player.getX(), mc.player.getY() + 1.058293536E-5, mc.player.getZ(), yaw, pitch, false));

                                mc.getNetworkHandler().sendPacket(new Full(mc.player.getX(), mc.player.getY() + 9.16580235E-6, mc.player.getZ(), yaw, pitch, false));
                                mc.getNetworkHandler().sendPacket(new Full(mc.player.getX(), mc.player.getY() + 1.0371854E-7, mc.player.getZ(), yaw, pitch, false));
                            } else {
                                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.058293536E-5, mc.player.getZ(), false));
                                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 9.16580235E-6, mc.player.getZ(), false));
                                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0371854E-7, mc.player.getZ(), false));
                            }
                        }
                        break;
                    case GrimV3:
                        if (this.fullPacket.getValue()) {
                            float yaw = Perish.ROTATION.rotationYaw;
                            float pitch = Perish.ROTATION.rotationPitch;
                            if (mc.player != null) {
                                mc.getNetworkHandler().sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, true));
                                mc.getNetworkHandler().sendPacket(new Full(mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), yaw, pitch, false));
                                mc.getNetworkHandler().sendPacket(new Full(mc.player.getX(), mc.player.getY() + 0.04535F, mc.player.getZ(), yaw, pitch, false));
                            }
                        } else {
                            if (mc.player != null) {
                                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));

                                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), false));
                                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.04535F, mc.player.getZ(), false));
                            }
                        }
                        break;
                    case GrimOption:
                        if (this.fullPacket.getValue()) {
                            float yaw = Perish.ROTATION.rotationYaw;
                            float pitch = Perish.ROTATION.rotationPitch;
                            if (this.packet1.getValue()) {
                                if (mc.player != null) {
                                    mc.getNetworkHandler().sendPacket(new Full(mc.player.getX(), mc.player.getY() + this.spoofY1.getValue(), mc.player.getZ(), yaw, pitch, this.onGround1.getValue()));
                                }
                            }
                            if (this.packet2.getValue()) {
                                if (mc.player != null) {
                                    mc.getNetworkHandler().sendPacket(new Full(mc.player.getX(), mc.player.getY() + this.spoofY2.getValue(), mc.player.getZ(), yaw, pitch, this.onGround2.getValue()));
                                }
                            }
                            if (this.packet3.getValue()) {
                                if (mc.player != null) {
                                    mc.getNetworkHandler().sendPacket(new Full(mc.player.getX(), mc.player.getY() + this.spoofY3.getValue(), mc.player.getZ(), yaw, pitch, this.onGround3.getValue()));
                                }
                            }
                            if (this.packet4.getValue()) {
                                if (mc.player != null) {
                                    mc.getNetworkHandler().sendPacket(new Full(mc.player.getX(), mc.player.getY() + this.spoofY4.getValue(), mc.player.getZ(), yaw, pitch, this.onGround4.getValue()));
                                }
                            }
                            if (this.packet5.getValue()) {
                                if (mc.player != null) {
                                    mc.getNetworkHandler().sendPacket(new Full(mc.player.getX(), mc.player.getY() + this.spoofY5.getValue(), mc.player.getZ(), yaw, pitch, this.onGround5.getValue()));
                                }
                            }
                        } else {
                            if (this.packet1.getValue()) {
                                if (mc.player != null) {
                                    mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + this.spoofY1.getValue(), mc.player.getZ(), this.onGround1.getValue()));
                                }
                            }
                            if (this.packet2.getValue()) {
                                if (mc.player != null) {
                                    mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + this.spoofY2.getValue(), mc.player.getZ(), this.onGround2.getValue()));
                                }
                            }
                            if (this.packet3.getValue()) {
                                if (mc.player != null) {
                                    mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + this.spoofY3.getValue(), mc.player.getZ(), this.onGround3.getValue()));
                                }
                            }
                            if (this.packet4.getValue()) {
                                if (mc.player != null) {
                                    mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + this.spoofY4.getValue(), mc.player.getZ(), this.onGround4.getValue()));
                                }
                            }
                            if (this.packet5.getValue()) {
                                if (mc.player != null) {
                                    mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + this.spoofY5.getValue(), mc.player.getZ(), this.onGround5.getValue()));
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
            return;
        }
        switch (this.mode.getValue()) {
            case UpdatedNCP:
                if (mc.player != null) {
                    mc.player.addCritParticles(entity);
                }
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.71875E-7, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
                break;
            case Strict:
                if (mc.player != null) {
                    mc.player.addCritParticles(entity);
                }
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.062600301692775, mc.player.getZ(), false));
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.07260029960661, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
                break;
            case NCP:
                if (mc.player != null) {
                    mc.player.addCritParticles(entity);
                }
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
                break;
            case OldNCP:
                if (mc.player != null) {
                    mc.player.addCritParticles(entity);
                }
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.058293536E-5, mc.player.getZ(), false));
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 9.16580235E-6, mc.player.getZ(), false));
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0371854E-7, mc.player.getZ(), false));
                break;
            case Hypixel2K22:
                if (mc.player != null) {
                    mc.player.addCritParticles(entity);
                }
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0045, mc.player.getZ(), true));
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.52121E-4, mc.player.getZ(), false));
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.3, mc.player.getZ(), false));
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.025, mc.player.getZ(), false));
                break;
            case Packet:
                if (mc.player != null) {
                    mc.player.addCritParticles(entity);
                }
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 5.0E-4, mc.player.getZ(), false));
                mc.getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0E-4, mc.player.getZ(), false));
            case Ground:
            default:
                break;
            case BBTT:
                if (MovementUtil.isMoving() || !MovementUtil.isStatic()) {
                    return;
                }

                if (mc.player != null) {
                    mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));

                    mc.getNetworkHandler()
                            .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), false));
                    mc.getNetworkHandler()
                            .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.045, mc.player.getZ(), false));
                }
        }
    }

    public enum Mode {
        UpdatedNCP,
        Strict,
        NCP,
        OldNCP,
        Hypixel2K22,
        Packet,
        Ground,
        BBTT,
        GrimV2,
        GrimV3,
        GrimOption
    }
}