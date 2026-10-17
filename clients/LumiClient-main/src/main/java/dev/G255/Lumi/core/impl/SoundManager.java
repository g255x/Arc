package dev.G255.Lumi.core.impl;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SoundManager {
    private static final Identifier GUI_CLICK_ID = Identifier.of("lumi", "gui_click");
    private static final Identifier ENABLE_ID = Identifier.of("lumi", "enable");
    private static final Identifier DISABLE_ID = Identifier.of("lumi", "disable");
    private static final Identifier ADD_ENTITY_ID = Identifier.of("lumi", "add_entity");

    public static RegistryEntry<SoundEvent> GUI_CLICK;
    public static RegistryEntry<SoundEvent> ENABLE;
    public static RegistryEntry<SoundEvent> DISABLE;
    public static RegistryEntry<SoundEvent> ADD_ENTITY;

    private static boolean registered = false;

    public static void registerSounds() {
        if (registered) return;
        try {
            Registry.register(Registries.SOUND_EVENT, GUI_CLICK_ID, SoundEvent.of(GUI_CLICK_ID));
            Registry.register(Registries.SOUND_EVENT, ENABLE_ID, SoundEvent.of(ENABLE_ID));
            Registry.register(Registries.SOUND_EVENT, DISABLE_ID, SoundEvent.of(DISABLE_ID));
            Registry.register(Registries.SOUND_EVENT, ADD_ENTITY_ID, SoundEvent.of(ADD_ENTITY_ID));
            GUI_CLICK = Registries.SOUND_EVENT.getEntry(GUI_CLICK_ID).orElseThrow();
            ENABLE = Registries.SOUND_EVENT.getEntry(ENABLE_ID).orElseThrow();
            DISABLE = Registries.SOUND_EVENT.getEntry(DISABLE_ID).orElseThrow();
            ADD_ENTITY = Registries.SOUND_EVENT.getEntry(ADD_ENTITY_ID).orElseThrow();
        } catch (IllegalStateException e) {
            System.err.println("[Lumi] Sound registry already frozen, sounds disabled.");
        }
        registered = true;
    }
}