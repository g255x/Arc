package dev.g255.arc.api.utils.skia.image;

import io.github.humbleui.skija.Image;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ImageHelper {
   private final Map<String, Image> images = new HashMap();
   private final Map<Integer, Image> textures = new HashMap();

   public boolean load(int texture, float width, float height, Object origin) {
      return this.textures.containsKey(texture);
   }

   public boolean load(File file) {
      if (file != null && file.exists()) {
         String key = file.getAbsolutePath();
         if (this.images.containsKey(key)) {
            return true;
         } else {
            try {
               byte[] bytes = Files.readAllBytes(file.toPath());
               Image image = Image.makeDeferredFromEncodedBytes(bytes);
               if (image == null) {
                  return false;
               } else {
                  this.images.put(key, image);
                  return true;
               }
            } catch (IOException var5) {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean loadResource(String path) {
      if (path == null) {
         return false;
      } else if (path.isBlank()) {
         return false;
      } else if (this.images.containsKey(path)) {
         return true;
      } else {
         String normalized = path.startsWith("/") ? path.substring(1) : path;

         try {
            InputStream inputStream = this.openResource(path, normalized);

            boolean var12;
            label81: {
               boolean var14;
               label82: {
                  try {
                     if (inputStream == null) {
                        boolean bl = false;
                        var12 = bl;
                        break label81;
                     }

                     byte[] bytes = inputStream.readAllBytes();
                     Image image = Image.makeDeferredFromEncodedBytes(bytes);
                     if (image == null) {
                        boolean bl = false;
                        var14 = bl;
                        break label82;
                     }

                     this.images.put(path, image);
                     boolean bl = true;
                     var14 = bl;
                  } catch (Throwable var9) {
                     if (inputStream != null) {
                        try {
                           inputStream.close();
                        } catch (Throwable var8) {
                           var9.addSuppressed(var8);
                        }
                     }

                     throw var9;
                  }

                  if (inputStream != null) {
                     inputStream.close();
                  }

                  return var14;
               }

               if (inputStream != null) {
                  inputStream.close();
               }

               return var14;
            }

            if (inputStream != null) {
               inputStream.close();
            }

            return var12;
         } catch (IOException var10) {
            return false;
         }
      }
   }

   private InputStream openResource(String rawPath, String normalizedPath) {
      InputStream inputStream = ImageHelper.class.getClassLoader().getResourceAsStream(rawPath);
      if (inputStream != null) {
         return inputStream;
      } else {
         inputStream = ImageHelper.class.getClassLoader().getResourceAsStream(normalizedPath);
         return inputStream != null ? inputStream : ImageHelper.class.getResourceAsStream(rawPath.startsWith("/") ? rawPath : "/" + rawPath);
      }
   }

   public Image get(String key) {
      return (Image)this.images.get(key);
   }

   public Image get(int texture) {
      return (Image)this.textures.get(texture);
   }
}
