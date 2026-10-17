package dev.G255.Lumi.mod.modules.impl.client;

import dev.G255.Lumi.mod.modules.Module;
import dev.G255.Lumi.mod.modules.impl.client.skin.SkinCacheEntry;
import dev.G255.Lumi.mod.modules.impl.client.skin.SkinFetcher;
import dev.G255.Lumi.mod.modules.settings.impl.BooleanSetting;
import dev.G255.Lumi.mod.modules.settings.impl.EnumSetting;
import dev.G255.Lumi.mod.modules.settings.impl.StringSetting;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

public class Skin extends Module {
   public static Skin INSTANCE;
   private final StringSetting playerName = this.add(new StringSetting("Name", "Notch"));
   private final EnumSetting<ModelType> modelType = this.add(new EnumSetting<>("Model", ModelType.AUTO));
   private final BooleanSetting cape = this.add(new BooleanSetting("Cape", true));
   private final EnumSetting<CapeType> capeType = this.add(new EnumSetting<>("CapeType", CapeType.MELON, () -> this.cape.getValue()));
   private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
      Thread thread = new Thread(r, "Lumi-SkinFetcher");
      thread.setDaemon(true);
      return thread;
   });
   private Future<?> currentTask;
   private SkinCacheEntry currentEntry;
   private LoadState loadState = LoadState.IDLE;
   private String statusMessage = "";

   public Skin() {
      super("Skin", Module.Category.Client);
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.loadSkin();
   }

   @Override
   public void onDisable() {
      if (this.currentTask != null) {
         this.currentTask.cancel(true);
      }

      this.currentEntry = null;
      this.loadState = LoadState.IDLE;
      this.statusMessage = "";
   }

   @Override
   public void onLogin() {
      if (this.isOn()) {
         this.loadSkin();
      }
   }

   public Identifier getSkinTexture() {
      return this.isOn() && this.currentEntry != null ? this.currentEntry.identifier() : null;
   }

   public SkinTextures.Model getModel() {
      if (this.modelType.getValue() == ModelType.SLIM) {
         return SkinTextures.Model.SLIM;
      } else if (this.modelType.getValue() == ModelType.DEFAULT) {
         return SkinTextures.Model.WIDE;
      } else {
         return this.currentEntry != null && this.currentEntry.slim() ? SkinTextures.Model.SLIM : SkinTextures.Model.WIDE;
      }
   }

   public boolean hasSkin() {
      return this.isOn() && this.currentEntry != null;
   }

   public boolean hasCape() {
      return this.isOn() && this.cape.getValue();
   }

   public Identifier getCapeTexture() {
      return Identifier.of("forever", "cape/" + this.capeType.getValue().path);
   }

   public void refresh() {
      String name = this.playerName.getValue().trim();
      if (!name.isEmpty()) {
         SkinFetcher.clearCache(name);
         this.currentEntry = null;
         this.loadSkin();
      }
   }

   @Override
   public String getInfo() {
      return switch (this.loadState) {
         case IDLE -> "";
         case LOADING -> "...";
         case SUCCESS -> this.currentEntry != null ? this.currentEntry.playerName() : "";
         case FAILED -> "Failed";
      };
   }

   private void loadSkin() {
      String name = this.playerName.getValue().trim();
      if (name.isEmpty()) {
         this.loadState = LoadState.FAILED;
         this.statusMessage = "Empty player name";
         return;
      }

      SkinCacheEntry cached = SkinFetcher.getCached(name);
      if (cached != null) {
         this.currentEntry = cached;
         this.loadState = LoadState.SUCCESS;
         this.statusMessage = cached.playerName();
         return;
      }

      if (this.currentTask != null) {
         this.currentTask.cancel(true);
      }

      this.loadState = LoadState.LOADING;
      this.statusMessage = "Loading " + name;
      this.currentTask = this.executor.submit(() -> {
         try {
            SkinCacheEntry entry = SkinFetcher.fetchSkin(name);
            if (entry != null) {
               this.currentEntry = entry;
               this.loadState = LoadState.SUCCESS;
               this.statusMessage = entry.playerName();
            } else {
               this.loadState = LoadState.FAILED;
               this.statusMessage = "Load failed";
            }
         } catch (Exception e) {
            this.loadState = LoadState.FAILED;
            this.statusMessage = "Error: " + e.getMessage();
            e.printStackTrace();
         }
      });
   }

   public enum ModelType {
      AUTO,
      DEFAULT,
      SLIM
   }

   public enum LoadState {
      IDLE,
      LOADING,
      SUCCESS,
      FAILED
   }

   public enum CapeType {
      MELON("luna.png"),
      VAPE("vape_cape.png"),
      Chicken("chicken_cape.png"),
      BadLion("badlion_cape.png"),
      Creeper("creeper_cape.png"),
      Dragon("dragon_cape.png"),
      Elaina("elaina_cape.png"),
      FDP("fdp_cape.png"),
      LUNAR_LIGHT("lunar_light_cape.png"),
      LUNAR_DARK("lunar_dark_cape.png"),
      Mojang("mojang_cape.png"),
      NovoLine("novoline_cape.png"),
      Sagiri("sagiri_cape.png");

      private final String path;

      CapeType(String path) {
         this.path = path;
      }
   }
}
