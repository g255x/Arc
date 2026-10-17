package hoprc.asm.mixins;

import net.minecraft.util.math.random.GaussianGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.atomic.AtomicLong;

@Mixin(targets = "net.minecraft.class_5820", remap = false)
public class MixinLegacyRandomThreadCheck {
    @Shadow
    @Final
    private AtomicLong field_28766;

    @Shadow
    @Final
    private GaussianGenerator field_35124;

    /**
     * @author Forever
     * @reason Baritone can touch CheckedRandom from multiple threads while chunks are loading.
     */
    @Overwrite(remap = false)
    public void method_43052(long seed) {
        this.field_28766.set((seed ^ 25214903917L) & 281474976710655L);
        this.field_35124.reset();
    }

    /**
     * @author Forever
     * @reason Prevent CheckedRandom from crashing on concurrent access by retrying the seed update.
     */
    @Overwrite(remap = false)
    public int method_43156(int bits) {
        long oldSeed;
        long nextSeed;
        do {
            oldSeed = this.field_28766.get();
            nextSeed = oldSeed * 25214903917L + 11L & 281474976710655L;
        } while (!this.field_28766.compareAndSet(oldSeed, nextSeed));

        return (int)(nextSeed >> 48 - bits);
    }
}
