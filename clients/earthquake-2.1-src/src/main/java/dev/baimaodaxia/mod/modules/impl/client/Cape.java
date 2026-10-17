package dev.baimaodaxia.mod.modules.impl.client;

import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.util.Identifier;

public class Cape extends Module {
   public static Cape INSTANCE;
   public static final Map<Capes, Identifier> CAPES_MAP = new EnumMap(Capes.class);
   public final EnumSetting<Capes> cape;

   public Cape() {
      super("Cape", Module.Category.Client);
      this.cape = this.add(new EnumSetting("Cape", Cape.Capes.FUTURE));
      this.setChinese("披风");
      INSTANCE = this;
   }

   static {
      CAPES_MAP.put(Cape.Capes.FUTURE, Identifier.of("minecraft", "textures/capes/future.png"));
      CAPES_MAP.put(Cape.Capes.RUSHERHACK, Identifier.of("minecraft", "textures/capes/rusherhack.png"));
      CAPES_MAP.put(Cape.Capes.HYPER, Identifier.of("minecraft", "textures/capes/hyper.png"));
      CAPES_MAP.put(Cape.Capes.Vape, Identifier.of("minecraft", "textures/capes/vape.png"));
      CAPES_MAP.put(Cape.Capes.Anime, Identifier.of("minecraft", "textures/capes/anime.png"));
      CAPES_MAP.put(Cape.Capes.OF, Identifier.of("minecraft", "textures/capes/of.png"));
      CAPES_MAP.put(Cape.Capes.Moon, Identifier.of("minecraft", "textures/capes/moon.png"));
      CAPES_MAP.put(Cape.Capes.Reimu, Identifier.of("minecraft", "textures/capes/reimu.png"));
      CAPES_MAP.put(Cape.Capes.Azureware, Identifier.of("minecraft", "textures/capes/azureware.png"));
      CAPES_MAP.put(Cape.Capes.Hanabi, Identifier.of("minecraft", "textures/capes/hanabi.png"));
      CAPES_MAP.put(Cape.Capes.Steam, Identifier.of("minecraft", "textures/capes/steam.png"));
      CAPES_MAP.put(Cape.Capes.FDP, Identifier.of("minecraft", "textures/capes/fdp.png"));
   }

   public static enum Capes {
      FUTURE,
      RUSHERHACK,
      HYPER,
      Vape,
      Anime,
      OF,
      Moon,
      Reimu,
      Azureware,
      Hanabi,
      Steam,
      FDP;

      // $FF: synthetic method
      private static Capes[] $values() {
         return new Capes[]{FUTURE, RUSHERHACK, HYPER, Vape, Anime, OF, Moon, Reimu, Azureware, Hanabi, Steam, FDP};
      }
   }
}
