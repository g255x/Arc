package Open;

import dev.perish.Perish;
import net.fabricmc.api.ModInitializer;

public final class PerishEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        initialize();
    }

    public static void initialize() {
        new Perish().onInitialize();
    }
}
