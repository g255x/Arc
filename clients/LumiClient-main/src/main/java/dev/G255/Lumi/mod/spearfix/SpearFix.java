package dev.G255.Lumi.mod.spearfix;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class SpearFix {
    public static final String MOD_ID = "spearfix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ThreadLocal<ItemStack> CURRENT_RENDER_STACK = new ThreadLocal<>();
    private static final int STAB_MOTION_TICKS = 6;
    private static final int STAB_RENDER_TICKS = 10;

    private static final Map<Item, String> SWORD_TO_SPEAR_TYPE = new HashMap<>();
    private static final String NOSLOW_KEY = "noslow";
    private static boolean noslowEnabled = true;
    private static boolean loaded;
    private static boolean modelsRegistered;
    private static int stabStartAge = Integer.MIN_VALUE;
    private static ItemStack stabStack = ItemStack.EMPTY;

    static {
        SWORD_TO_SPEAR_TYPE.put(Items.WOODEN_SWORD, "wooden");
        SWORD_TO_SPEAR_TYPE.put(Items.GOLDEN_SWORD, "golden");
        SWORD_TO_SPEAR_TYPE.put(Items.STONE_SWORD, "stone");
        SWORD_TO_SPEAR_TYPE.put(Items.IRON_SWORD, "iron");
        SWORD_TO_SPEAR_TYPE.put(Items.DIAMOND_SWORD, "diamond");
        SWORD_TO_SPEAR_TYPE.put(Items.NETHERITE_SWORD, "netherite");
    }

    private SpearFix() {
    }

    public static void initModelLoading() {
        registerModels();
    }

    public static void init() {
        load();
        registerModels();
        LOGGER.info("[SpearFix] Embedded spear compatibility loaded");
    }

    public static boolean isNoslowEnabled() {
        return noslowEnabled;
    }

    public static void setNoslowEnabled(boolean enabled) {
        noslowEnabled = enabled;
        save();
    }

    public static boolean isRemappedSpear(ItemStack stack) {
        return getSpearType(stack) != null;
    }

    public static String getSpearType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        String customName = stack.getName().getString().toLowerCase(Locale.ROOT);
        if (!isSpearName(customName)) return null;

        String type = SWORD_TO_SPEAR_TYPE.get(stack.getItem());
        if (type != null) return type;

        if (customName.contains("copper")) return "copper";
        if (customName.contains("wooden") || customName.contains("wood") || customName.contains("木")) return "wooden";
        if (customName.contains("golden") || customName.contains("gold") || customName.contains("金")) return "golden";
        if (customName.contains("stone") || customName.contains("石")) return "stone";
        if (customName.contains("iron") || customName.contains("铁")) return "iron";
        if (customName.contains("diamond") || customName.contains("钻石")) return "diamond";
        if (customName.contains("netherite") || customName.contains("下界合金")) return "netherite";

        return "diamond";
    }

    private static boolean isSpearName(String customName) {
        return customName.contains("spear")
                || customName.contains("长矛")
                || customName.contains("矛");
    }

    public static boolean shouldBypassUseSlowdown(ItemStack stack) {
        return noslowEnabled && isRemappedSpear(stack);
    }

    public static void startStabAnimation(AbstractClientPlayerEntity player, ItemStack stack) {
        if (player == null || !isRemappedSpear(stack)) return;

        stabStartAge = player.age;
        stabStack = stack.copy();
    }

    public static float getStabAnimationProgress(AbstractClientPlayerEntity player, ItemStack stack, float tickDelta) {
        if (player == null || !isRemappedSpear(stack)) return -1.0F;
        if (stabStack.isEmpty() || stabStack.getItem() != stack.getItem()
            || !stabStack.getName().getString().equals(stack.getName().getString())) {
            return -1.0F;
        }

        float ticks = player.age - stabStartAge + tickDelta;
        if (ticks < 0.0F || ticks > STAB_RENDER_TICKS) {
            if (ticks > STAB_RENDER_TICKS) { stabStartAge = Integer.MIN_VALUE; stabStack = ItemStack.EMPTY; }
            return -1.0F;
        }
        return MathHelper.clamp(ticks / STAB_MOTION_TICKS, 0.0F, 1.0F);
    }

    private static void registerModels() {
        if (modelsRegistered) return;
        modelsRegistered = true;
        ModelLoadingPlugin.register(new SpearModelLoadingPlugin());
    }

    private static void load() {
        if (loaded) return;
        loaded = true;

        Path path = configPath();
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException e) {
                LOGGER.warn("[SpearFix] Failed to load config, using defaults", e);
            }
        }

        noslowEnabled = Boolean.parseBoolean(properties.getProperty(NOSLOW_KEY, Boolean.toString(noslowEnabled)));
        save();
    }

    private static void save() {
        Path path = configPath();
        Properties properties = new Properties();
        properties.setProperty(NOSLOW_KEY, Boolean.toString(noslowEnabled));

        try {
            Files.createDirectories(path.getParent());
            try (OutputStream output = Files.newOutputStream(path)) {
                properties.store(output, "Lumi embedded SpearFix config");
            }
        } catch (IOException e) {
            LOGGER.warn("[SpearFix] Failed to save config", e);
        }
    }

    private static Path configPath() {
        return MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("alien")
                .resolve("cfg")
                .resolve("spearfix.properties");
    }
}
