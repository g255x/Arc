package dev.g255.arc.mod.modules.impl.client;

import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.Registries;

public class HoverSound extends Module {
   private static HoverSound INSTANCE;
   public static final SoundEvent MIO = register("arc", "mio");
   public static final SoundEvent MIO2 = register("arc", "mio2");
   public static final SoundEvent MIO3 = register("arc", "mio3");
   public final EnumSetting<Tone> tone;
   public final SliderSetting pitch;
   public final SliderSetting volume;
   public final BooleanSetting expandSound;
   public final EnumSetting<Tone> expandTone;
   public final SliderSetting expandPitch;
   public final SliderSetting expandVolume;
   public final BooleanSetting toggleSound;
   public final EnumSetting<Tone> toggleTone;
   public final SliderSetting togglePitch;
   public final SliderSetting toggleVolume;

   public HoverSound() {
      super("HoverSound", Module.Category.Client);
      this.tone = this.add(new EnumSetting("Sound", HoverSound.Tone.Mio));
      this.pitch = this.add(new SliderSetting("Pitch", 1.8, (double)0.5F, (double)2.0F, 0.1));
      this.volume = this.add(new SliderSetting("Volume", (double)0.25F, (double)0.0F, (double)1.0F, 0.05));
      this.expandSound = this.add(new BooleanSetting("ExpandSound", true));
      this.expandTone = this.add(new EnumSetting("ExpandTone", HoverSound.Tone.Mio2));
      this.expandPitch = this.add(new SliderSetting("ExpandPitch", 1.2, (double)0.5F, (double)2.0F, 0.1));
      this.expandVolume = this.add(new SliderSetting("ExpandVolume", 0.3, (double)0.0F, (double)1.0F, 0.05));
      this.toggleSound = this.add(new BooleanSetting("ToggleSound", true));
      this.toggleTone = this.add(new EnumSetting("ToggleTone", HoverSound.Tone.Mio3));
      this.togglePitch = this.add(new SliderSetting("TogglePitch", 1.2, (double)0.5F, (double)2.0F, 0.1));
      this.toggleVolume = this.add(new SliderSetting("ToggleVolume", 0.3, (double)0.0F, (double)1.0F, 0.05));
      this.setChinese("界面音效");
      INSTANCE = this;
   }

   private static SoundEvent register(String namespace, String path) {
      Identifier id = Identifier.of(namespace, path);
      return (SoundEvent)Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
   }

   private void play(Tone t, float p, float v) {
      if (t != null && !(v <= 0.0F) && mc.getSoundManager() != null) {
         mc.getSoundManager().play(PositionedSoundInstance.master(t.event(), p, v));
      }
   }

   public static boolean onHover() {
      HoverSound s = INSTANCE;
      if (s != null && s.isOn()) {
         s.play(s.tone.getValue(), s.pitch.getValueFloat(), s.volume.getValueFloat());
         return true;
      } else {
         return false;
      }
   }

   public static boolean onExpand() {
      HoverSound s = INSTANCE;
      if (s != null && s.isOn() && s.expandSound.getValue()) {
         s.play(s.expandTone.getValue(), s.expandPitch.getValueFloat(), s.expandVolume.getValueFloat());
         return true;
      } else {
         return false;
      }
   }

   public static boolean onModuleToggle() {
      HoverSound s = INSTANCE;
      if (s != null && s.isOn() && s.toggleSound.getValue()) {
         s.play(s.toggleTone.getValue(), s.togglePitch.getValueFloat(), s.toggleVolume.getValueFloat());
         return true;
      } else {
         return false;
      }
   }

   public static enum Tone {
      Mio(HoverSound.MIO),
      Mio2(HoverSound.MIO2),
      Mio3(HoverSound.MIO3),
      Click(HoverSound.SoundEventsHolder.UI_BUTTON_CLICK),
      Wood(HoverSound.SoundEventsHolder.BLOCK_WOODEN_BUTTON_CLICK_ON),
      Stone(HoverSound.SoundEventsHolder.BLOCK_STONE_BUTTON_CLICK_ON),
      Lever(HoverSound.SoundEventsHolder.BLOCK_LEVER_CLICK),
      Tripwire(HoverSound.SoundEventsHolder.BLOCK_TRIPWIRE_CLICK_ON),
      Chest(HoverSound.SoundEventsHolder.BLOCK_CHEST_OPEN),
      Pickup(HoverSound.SoundEventsHolder.ENTITY_ITEM_PICKUP),
      Xp(HoverSound.SoundEventsHolder.ENTITY_EXPERIENCE_ORB_PICKUP),
      LevelUp(HoverSound.SoundEventsHolder.ENTITY_PLAYER_LEVELUP),
      Page(HoverSound.SoundEventsHolder.ITEM_BOOK_PAGE_TURN),
      Toast(HoverSound.SoundEventsHolder.UI_TOAST_IN),
      Note(HoverSound.SoundEventsHolder.BLOCK_NOTE_BLOCK_HAT),
      Bell(HoverSound.SoundEventsHolder.BLOCK_NOTE_BLOCK_BELL),
      Pling(HoverSound.SoundEventsHolder.BLOCK_NOTE_BLOCK_PLING),
      Chime(HoverSound.SoundEventsHolder.BLOCK_NOTE_BLOCK_CHIME),
      Bass(HoverSound.SoundEventsHolder.BLOCK_NOTE_BLOCK_BASS),
      Snare(HoverSound.SoundEventsHolder.BLOCK_NOTE_BLOCK_SNARE),
      Bit(HoverSound.SoundEventsHolder.BLOCK_NOTE_BLOCK_BIT),
      Amethyst(HoverSound.SoundEventsHolder.BLOCK_AMETHYST_BLOCK_CHIME),
      Resonate(HoverSound.SoundEventsHolder.BLOCK_AMETHYST_BLOCK_RESONATE);

      private final SoundEvent sound;

      private Tone(SoundEvent sound) {
         this.sound = sound;
      }

      public SoundEvent event() {
         return this.sound;
      }

      // $FF: synthetic method
      private static Tone[] $values() {
         return new Tone[]{Mio, Mio2, Mio3, Click, Wood, Stone, Lever, Tripwire, Chest, Pickup, Xp, LevelUp, Page, Toast, Note, Bell, Pling, Chime, Bass, Snare, Bit, Amethyst, Resonate};
      }
   }

   private static final class SoundEventsHolder {
      static final SoundEvent UI_BUTTON_CLICK;
      static final SoundEvent BLOCK_WOODEN_BUTTON_CLICK_ON;
      static final SoundEvent BLOCK_STONE_BUTTON_CLICK_ON;
      static final SoundEvent BLOCK_LEVER_CLICK;
      static final SoundEvent BLOCK_TRIPWIRE_CLICK_ON;
      static final SoundEvent BLOCK_CHEST_OPEN;
      static final SoundEvent ENTITY_ITEM_PICKUP;
      static final SoundEvent ENTITY_EXPERIENCE_ORB_PICKUP;
      static final SoundEvent ENTITY_PLAYER_LEVELUP;
      static final SoundEvent ITEM_BOOK_PAGE_TURN;
      static final SoundEvent UI_TOAST_IN;
      static final SoundEvent BLOCK_NOTE_BLOCK_HAT;
      static final SoundEvent BLOCK_NOTE_BLOCK_BELL;
      static final SoundEvent BLOCK_NOTE_BLOCK_PLING;
      static final SoundEvent BLOCK_NOTE_BLOCK_CHIME;
      static final SoundEvent BLOCK_NOTE_BLOCK_BASS;
      static final SoundEvent BLOCK_NOTE_BLOCK_SNARE;
      static final SoundEvent BLOCK_NOTE_BLOCK_BIT;
      static final SoundEvent BLOCK_AMETHYST_BLOCK_CHIME;
      static final SoundEvent BLOCK_AMETHYST_BLOCK_RESONATE;

      static {
         UI_BUTTON_CLICK = (SoundEvent)SoundEvents.UI_BUTTON_CLICK.value();
         BLOCK_WOODEN_BUTTON_CLICK_ON = SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON;
         BLOCK_STONE_BUTTON_CLICK_ON = SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON;
         BLOCK_LEVER_CLICK = SoundEvents.BLOCK_LEVER_CLICK;
         BLOCK_TRIPWIRE_CLICK_ON = SoundEvents.BLOCK_TRIPWIRE_CLICK_ON;
         BLOCK_CHEST_OPEN = SoundEvents.BLOCK_CHEST_OPEN;
         ENTITY_ITEM_PICKUP = SoundEvents.ENTITY_ITEM_PICKUP;
         ENTITY_EXPERIENCE_ORB_PICKUP = SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
         ENTITY_PLAYER_LEVELUP = SoundEvents.ENTITY_PLAYER_LEVELUP;
         ITEM_BOOK_PAGE_TURN = SoundEvents.ITEM_BOOK_PAGE_TURN;
         UI_TOAST_IN = SoundEvents.UI_TOAST_IN;
         BLOCK_NOTE_BLOCK_HAT = (SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_HAT.value();
         BLOCK_NOTE_BLOCK_BELL = (SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
         BLOCK_NOTE_BLOCK_PLING = (SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
         BLOCK_NOTE_BLOCK_CHIME = (SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value();
         BLOCK_NOTE_BLOCK_BASS = (SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_BASS.value();
         BLOCK_NOTE_BLOCK_SNARE = (SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value();
         BLOCK_NOTE_BLOCK_BIT = (SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_BIT.value();
         BLOCK_AMETHYST_BLOCK_CHIME = SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
         BLOCK_AMETHYST_BLOCK_RESONATE = SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE;
      }
   }
}
