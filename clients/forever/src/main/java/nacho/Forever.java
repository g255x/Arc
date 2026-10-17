package nacho;

import net.fabricmc.api.ModInitializer;

/**
 * Stable Fabric entrypoint.
 *
 * <p>Fabric resolves this class by the literal name in fabric.mod.json, so
 * obfuscation must preserve this small adapter. The actual startup logic lives
 * in {@link dev.kissoo.Forever} and can be obfuscated with the implementation.</p>
 */
public final class Forever implements ModInitializer {
    @Override
    public void onInitialize() {
        dev.kissoo.Forever.initialize();
    }
}
