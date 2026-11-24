package dev.g255.arc.mod.modules.impl.render;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.TotemEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.client.util.math.MatrixStack;

public class PopEsp extends Module {
    public static PopEsp INSTANCE;
    private final SliderSetting duration = this.add(new SliderSetting("Duration", 1000, 100, 5000, 1));
    private final CopyOnWriteArrayList<Lightning> lightnings = new CopyOnWriteArrayList<>();

    public PopEsp() {
        super("PopEsp", Category.Render);
        this.setChinese("爆图腾雷击");
        INSTANCE = this;
    }

    @EventListener
    public void onTotem(TotemEvent event) {
        if (event.getPlayer() != mc.player) {
            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
            lightning.refreshPositionAfterTeleport(event.getPlayer().getX(), event.getPlayer().getY(), event.getPlayer().getZ());
            lightning.setCosmetic(true);
            mc.world.playSound(mc.player, event.getPlayer().getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 10000.0F, 0.8F + mc.world.random.nextFloat() * 0.2F);
            mc.world.playSound(mc.player, event.getPlayer().getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.WEATHER, 2.0F, 0.5F + mc.world.random.nextFloat() * 0.2F);
            lightnings.add(new Lightning(lightning));
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        lightnings.removeIf(Lightning::isExpired);
        for (Lightning lightning : lightnings) {
            // Keep lightning "alive" visually
            lightning.entity.age = 1;
            lightning.entity.setCosmetic(true);

            net.minecraft.client.render.VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
            mc.getEntityRenderDispatcher().render(lightning.entity,
                    lightning.entity.getX() - mc.getEntityRenderDispatcher().camera.getPos().x,
                    lightning.entity.getY() - mc.getEntityRenderDispatcher().camera.getPos().y,
                    lightning.entity.getZ() - mc.getEntityRenderDispatcher().camera.getPos().z,
                    0.0F, mc.getRenderTickCounter().getTickDelta(true), matrixStack, immediate, 0xF000F0);
            immediate.draw();
        }
    }

    private class Lightning {
        public final LightningEntity entity;
        private final Timer timer = new Timer();

        public Lightning(LightningEntity entity) {
            this.entity = entity;
            this.timer.reset();
        }

        public boolean isExpired() {
            return this.timer.passedMs((long) duration.getValue());
        }
    }
}