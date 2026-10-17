package dev.kissoo.mod.spearfix;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class SpearModelLoadingPlugin implements ModelLoadingPlugin {
    public static final String[] SPEAR_TYPES = {
            "wooden", "golden", "stone", "copper", "iron", "diamond", "netherite"
    };

    public static final Map<String, Identifier> SPEAR_MODEL_IDS = new HashMap<>();
    public static final Map<String, Identifier> SPEAR_IN_HAND_MODEL_IDS = new HashMap<>();

    static {
        for (String type : SPEAR_TYPES) {
            SPEAR_MODEL_IDS.put(type, Identifier.of("spearfix", type + "_spear_model"));
            SPEAR_IN_HAND_MODEL_IDS.put(type, Identifier.of("spearfix", type + "_spear_in_hand_model"));
        }
    }

    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        for (String type : SPEAR_TYPES) {
            pluginContext.addModels(
                    SPEAR_MODEL_IDS.get(type),
                    SPEAR_IN_HAND_MODEL_IDS.get(type)
            );
        }
    }
}
