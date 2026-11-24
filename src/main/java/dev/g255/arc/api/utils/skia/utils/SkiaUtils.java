package dev.g255.arc.api.utils.skia.utils;

import io.github.humbleui.skija.Data;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class SkiaUtils {
   public static Optional<byte[]> convertToBytes(String path) {
      try {
         InputStream inputStream = getResourceAsStream(path);

         Optional var8;
         label48: {
            try {
               if (inputStream == null) {
                  Optional<byte[]> optional2 = Optional.empty();
                  var8 = optional2;
                  break label48;
               }

               Optional<byte[]> optional = Optional.of(inputStream.readAllBytes());
               var8 = optional;
            } catch (Throwable var5) {
               if (inputStream != null) {
                  try {
                     inputStream.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (inputStream != null) {
               inputStream.close();
            }

            return var8;
         }

         if (inputStream != null) {
            inputStream.close();
         }

         return var8;
      } catch (IOException var6) {
         return Optional.empty();
      }
   }

   public static Optional<Data> convertToData(String path) {
      return convertToBytes(path).map(Data::makeFromBytes);
   }

   public static InputStream getResourceAsStream(String path) {
      if (path != null && !path.isBlank()) {
         InputStream stream = SkiaUtils.class.getResourceAsStream(path);
         if (stream != null) {
            return stream;
         } else {
            String normalized = path.startsWith("/") ? path.substring(1) : path;
            stream = SkiaUtils.class.getClassLoader().getResourceAsStream(normalized);
            if (stream != null) {
               return stream;
            } else {
               return !path.startsWith("/") ? SkiaUtils.class.getResourceAsStream("/" + path) : null;
            }
         }
      } else {
         return null;
      }
   }
}
