package dev.baimaodaxia.api.utils.render.panorama;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import org.apache.commons.io.IOUtils;
public class PanoramaResourceLoader {
   private static PanoramaShader panoramaShader;
   private static PanoramaRenderer panoramaRenderer;
   private static int currentMenu = 0;
   private static int pendingMenu = -1;
   private static final int MENU_COUNT = 12;

   public static void load(int menuIndex) {
      currentMenu = menuIndex % 12;
      String suffix = currentMenu == 0 ? "" : String.valueOf(currentMenu);
      Identifier vertex = Identifier.of("minecraft", "shaders/mainmenu/mainmenu" + suffix + ".vert");
      Identifier fragment = Identifier.of("minecraft", "shaders/mainmenu/mainmenu" + suffix + ".frag");
      Identifier texture = Identifier.of("minecraft", "shaders/mainmenu/mainmenu" + suffix + ".png");
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null) {
         pendingMenu = currentMenu;
      } else {
         ResourceManager manager = client.getResourceManager();
         if (manager == null) {
            pendingMenu = currentMenu;
         } else {
            String vertexSource = loadStringResource(manager, vertex);
            String fragmentSource = loadStringResource(manager, fragment);
            Identifier textureId = manager.getResource(texture).isPresent() ? texture : null;
            panoramaShader = new PanoramaShader(vertexSource, fragmentSource, textureId);
            if (panoramaRenderer != null) {
               panoramaRenderer.close();
            }

            panoramaRenderer = panoramaShader.compile();
            panoramaShader = null;
         }
      }
   }

   public static void loadRandom() {
      load((new Random()).nextInt(12));
   }

   private static String loadStringResource(ResourceManager manager, Identifier identifier) {
      Optional<Resource> resource = manager.getResource(identifier);
      if (resource.isPresent()) {
         try {
            return IOUtils.toString(((Resource)resource.get()).getInputStream(), StandardCharsets.UTF_8);
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      return "";
   }

   public static PanoramaRenderer getRenderer() {
      if (panoramaShader != null) {
         if (panoramaRenderer != null) {
            panoramaRenderer.close();
         }

         panoramaRenderer = panoramaShader.compile();
         panoramaShader = null;
      }

      if (panoramaRenderer == null) {
         if (pendingMenu >= 0) {
            load(pendingMenu);
            pendingMenu = -1;
         } else {
            load(0);
         }
      }

      return panoramaRenderer;
   }

   public static void setMenu(int index) {
      if (index != currentMenu) {
         load(index);
      }

   }

   public static int getCurrentMenu() {
      return currentMenu;
   }

   public static int getMenuCount() {
      return 12;
   }
}
