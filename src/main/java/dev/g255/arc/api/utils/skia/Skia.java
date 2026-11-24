package dev.g255.arc.api.utils.skia;

import dev.g255.arc.Arc;
import dev.g255.arc.api.utils.skia.context.SkiaContext;
import dev.g255.arc.api.utils.skia.font.Fonts;
import dev.g255.arc.api.utils.skia.image.ImageHelper;
import dev.g255.arc.asm.accessors.IAbstractTexture;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.lang.Character.UnicodeScript;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class Skia {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private static final ImageHelper IMAGE_HELPER = new ImageHelper();
   private static final ImageFilter SHADOW_FILTER;
   private static final Map<Identifier, Image> TEXTURE_CACHE;
   private static final Map<String, Image> URL_CACHE;
   private static final Pattern STRIP_PATTERN;
   private static Paint TEXT_PAINT;
   private static Font ASCENT_KEY;
   private static float ASCENT_VAL;

   public static Canvas getCanvas() {
      return SkiaContext.getCanvas();
   }

   public static void save() {
      if (getCanvas() != null) {
         getCanvas().save();
      }

   }

   public static void restore() {
      if (getCanvas() != null) {
         getCanvas().restore();
      }

   }

   public static void clip(float x, float y, float width, float height, float radius) {
      if (getCanvas() != null) {
         getCanvas().clipRRect(RRect.makeXYWH(x, y, width, height, radius), ClipMode.INTERSECT, true);
      }
   }

   public static void drawRect(float x, float y, float width, float height, Color color) {
      if (getCanvas() != null) {
         Paint paint = getPaint(color);

         try {
            getCanvas().drawRect(Rect.makeXYWH(x, y, width, height), paint);
         } catch (Throwable var9) {
            if (paint != null) {
               try {
                  paint.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (paint != null) {
            paint.close();
         }

      }
   }

   public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
      if (getCanvas() != null) {
         Paint paint = getPaint(color);

         try {
            getCanvas().drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
         } catch (Throwable var10) {
            if (paint != null) {
               try {
                  paint.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (paint != null) {
            paint.close();
         }

      }
   }

   public static void drawRoundedRectOutline(float x, float y, float width, float height, float radius, float strokeWidth, Color color) {
      if (getCanvas() != null) {
         Paint paint = getPaint(color);

         try {
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(strokeWidth);
            getCanvas().drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
         } catch (Throwable var11) {
            if (paint != null) {
               try {
                  paint.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (paint != null) {
            paint.close();
         }

      }
   }

   public static void drawCircle(float x, float y, float radius, Color color) {
      if (getCanvas() != null) {
         Paint paint = getPaint(color);

         try {
            getCanvas().drawCircle(x, y, radius, paint);
         } catch (Throwable var8) {
            if (paint != null) {
               try {
                  paint.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (paint != null) {
            paint.close();
         }

      }
   }

   public static void drawLine(float x, float y, float endX, float endY, float width, Color color) {
      if (getCanvas() != null) {
         Paint paint = getPaint(color);

         try {
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(width);
            paint.setAntiAlias(true);
            getCanvas().drawLine(x, y, endX, endY, paint);
         } catch (Throwable var10) {
            if (paint != null) {
               try {
                  paint.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (paint != null) {
            paint.close();
         }

      }
   }

   public static void drawRoundedGuiBlur(float x, float y, float width, float height, float radius, float alpha) {
      if (Arc.BLUR != null) {
         Arc.BLUR.applyBlur(14.0F * alpha, x, y, width, height);
      }

   }

   public static void drawRoundedGuiBlur(float x, float y, float width, float height, float radius) {
      drawRoundedGuiBlur(x, y, width, height, radius, 1.0F);
   }

   public static void drawShadow(float x, float y, float width, float height, float radius) {
      drawShadow(x, y, width, height, radius, new Color(0, 0, 0, 90));
   }

   public static void drawShadow(float x, float y, float width, float height, float radius, Color color) {
      if (getCanvas() != null) {
         Paint paint = getPaint(color);

         try {
            paint.setImageFilter(SHADOW_FILTER);
            getCanvas().drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
         } catch (Throwable var10) {
            if (paint != null) {
               try {
                  paint.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (paint != null) {
            paint.close();
         }

      }
   }

   public static void drawImage(File file, float x, float y, float width, float height) {
      if (getCanvas() != null && file != null && IMAGE_HELPER.load(file)) {
         getCanvas().drawImageRect(IMAGE_HELPER.get(file.getAbsolutePath()), Rect.makeXYWH(x, y, width, height));
      }
   }

   public static void drawResourceImage(String resourcePath, float x, float y, float width, float height) {
      if (getCanvas() != null && resourcePath != null && IMAGE_HELPER.loadResource(resourcePath)) {
         getCanvas().drawImageRect(IMAGE_HELPER.get(resourcePath), Rect.makeXYWH(x, y, width, height));
      }
   }

   public static boolean drawTextureRegion(Identifier textureId, float x, float y, float width, float height, float u, float v, float texWidth, float texHeight) {
      if (getCanvas() != null && textureId != null) {
         Image image = loadMinecraftTexture(textureId);
         if (image == null) {
            return false;
         } else {
            getCanvas().drawImageRect(image, Rect.makeXYWH(u, v, texWidth, texHeight), Rect.makeXYWH(x, y, width, height));
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean drawUrlTextureRegion(String url, float x, float y, float width, float height, float u, float v, float texWidth, float texHeight) {
      if (getCanvas() != null && url != null && !url.isBlank()) {
         Image image = loadUrlImage(url);
         if (image == null) {
            return false;
         } else {
            getCanvas().drawImageRect(image, Rect.makeXYWH(u, v, texWidth, texHeight), Rect.makeXYWH(x, y, width, height));
            return true;
         }
      } else {
         return false;
      }
   }

   public static void invalidateTexture(Identifier textureId) {
      if (textureId != null) {
         TEXTURE_CACHE.remove(textureId);
      }

   }

   public static void invalidateUrlTexture(String url) {
      if (url != null) {
         URL_CACHE.remove(url);
      }

   }

   public static void drawRoundedImage(File file, float x, float y, float width, float height, float radius) {
      if (getCanvas() != null) {
         save();
         clip(x, y, width, height, radius);
         drawImage(file, x, y, width, height);
         restore();
      }
   }

   /**
    * 画一行文字。
    *
    * <p><b>y 是文字外框的顶边，不是基线。</b>调用方（EnemyInfo / DeathFx）都是按
    * {@code y + (框高 - 字高) / 2} 来垂直居中排版的，所以这里必须把顶边换算成
    * {@code canvas.drawString} 需要的基线 —— 即顶边减去（负的）ascent。</p>
    *
    * <p>方法体原来是反编译失败留下的空注释块（{@code $FF: Couldn't be decompiled}），
    * 表现出来就是「敌人信息卡片、头像、血条都在，唯独一个字都没有」。</p>
    */
   public static void drawText(String text, float x, float y, Color color, Font font) {
      Canvas canvas = getCanvas();
      if (canvas == null || font == null || color == null) {
         return;
      }

      String value = stripFormatting(text);
      if (value.isEmpty()) {
         return;
      }

      float baseline = y - cachedAscent(font);
      Paint paint = getTextPaint(color);
      paint.setAntiAlias(true);
      canvas.drawString(value, x, baseline, font, paint);
   }

   public static float getStringWidth(String text, Font font) {
      return text != null && font != null ? font.measureTextWidth(stripFormatting(text)) : 0.0F;
   }

   public static float getFontHeight(Font font) {
      if (font == null) {
         return 9.0F;
      } else {
         FontMetrics metrics = font.getMetrics();
         return metrics.getDescent() - metrics.getAscent();
      }
   }

   public static String getLimitText(String text, Font font, float width) {
      if (text != null && !text.isEmpty() && !(getStringWidth(text, font) <= width)) {
         String value = text;

         for(float dots = getStringWidth("...", font); !value.isEmpty() && getStringWidth(value, font) + dots > width; value = value.substring(0, value.length() - 1)) {
         }

         return value + "...";
      } else {
         return text;
      }
   }

   public static Font getDefaultFont(float size, boolean bold) {
      return bold ? Fonts.getProductSansMedium(size) : Fonts.getMiSans(size);
   }

   public static Font getDefaultFont(String text, float size, boolean bold) {
      if (containsCjk(text)) {
         return bold ? Fonts.getCjkBold(size) : Fonts.getCjkRegular(size);
      } else {
         return getDefaultFont(size, bold);
      }
   }

   public static Paint getPaint(Color color) {
      Paint paint = new Paint();
      paint.setARGB(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
      paint.setAntiAlias(true);
      return paint;
   }

   private static Image loadMinecraftTexture(Identifier textureId) {
      try {
         Image cached = (Image)TEXTURE_CACHE.get(textureId);
         if (cached != null) {
            return cached;
         } else {
            Image resourceImage = tryLoadResourceTexture(textureId);
            if (resourceImage != null) {
               TEXTURE_CACHE.put(textureId, resourceImage);
               return resourceImage;
            } else {
               MC.getTextureManager().bindTexture(textureId);
               AbstractTexture texture = MC.getTextureManager().getTexture(textureId);
               Image image = tryLoadNativeTexture(texture);
               if (image != null) {
                  TEXTURE_CACHE.put(textureId, image);
                  return image;
               } else {
                  int glTextureId = GL11.glGetInteger(32873);
                  if (glTextureId <= 0) {
                     glTextureId = getGlTextureId(texture);
                  }

                  if (glTextureId <= 0) {
                     return null;
                  } else {
                     int previousBinding = GL11.glGetInteger(32873);
                     GL11.glBindTexture(3553, glTextureId);

                     Image var10;
                     try {
                        int width = GL11.glGetTexLevelParameteri(3553, 0, 4096);
                        int height = GL11.glGetTexLevelParameteri(3553, 0, 4097);
                        if (width > 0 && height > 0) {
                           int rowBytes = width * 4;
                           ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
                           GL11.glGetTexImage(3553, 0, 6408, 5121, buffer);
                           byte[] flipped = flipTextureRows(buffer, width, height);
                           if (isAllZero(flipped)) {
                              Image image3 = null;
                              Image var13 = image3;
                              return var13;
                           }

                           Image rasterImage = Image.makeRasterFromBytes(new ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL), flipped, (long)rowBytes);
                           if (rasterImage != null) {
                              TEXTURE_CACHE.put(textureId, rasterImage);
                           }

                           Image var14 = rasterImage;
                           return var14;
                        }

                        Image image2 = null;
                        var10 = image2;
                     } finally {
                        GL11.glBindTexture(3553, previousBinding);
                     }

                     return var10;
                  }
               }
            }
         }
      } catch (Throwable var19) {
         return null;
      }
   }

   private static Image loadUrlImage(String url) {
      Image cached = (Image)URL_CACHE.get(url);
      if (cached != null) {
         return cached;
      } else {
         try {
            URL u = new URL(url);
            URLConnection conn = u.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "Kimi-Skia");
            InputStream in = conn.getInputStream();

            Image var7;
            try {
               byte[] bytes = in.readAllBytes();
               Image image2 = Image.makeDeferredFromEncodedBytes(bytes);
               if (image2 != null) {
                  URL_CACHE.put(url, image2);
               }

               var7 = image2;
            } catch (Throwable var9) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (in != null) {
               in.close();
            }

            return var7;
         } catch (Throwable var10) {
            return null;
         }
      }
   }

   private static Image tryLoadResourceTexture(Identifier textureId) {
      try {
         Optional resource = MC.getResourceManager().getResource(textureId);
         if (resource.isEmpty()) {
            return null;
         } else {
            InputStream inputStream = ((Resource)resource.get()).getInputStream();

            Object var5;
            label54: {
               Image var6;
               try {
                  byte[] bytes = inputStream.readAllBytes();
                  Image image2 = Image.makeDeferredFromEncodedBytes(bytes);
                  if (image2 == null) {
                     var5 = null;
                     break label54;
                  }

                  var6 = image2;
               } catch (Throwable var8) {
                  if (inputStream != null) {
                     try {
                        inputStream.close();
                     } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                     }
                  }

                  throw var8;
               }

               if (inputStream != null) {
                  inputStream.close();
               }

               return var6;
            }

            if (inputStream != null) {
               inputStream.close();
            }

            return (Image)var5;
         }
      } catch (Throwable var9) {
         return null;
      }
   }

   private static Image tryLoadNativeTexture(AbstractTexture texture) {
      if (texture == null) {
         return null;
      } else {
         try {
            Image image = extractNativeImage(texture, Collections.newSetFromMap(new IdentityHashMap()), 0);
            if (image != null) {
               return image;
            }
         } catch (Throwable var2) {
         }

         return null;
      }
   }

   private static Image extractNativeImage(Object value, Set<Object> visited, int depth) {
      if (value != null && depth <= 7 && !visited.contains(value)) {
         visited.add(value);
         if (value instanceof NativeImage) {
            NativeImage nativeImage2 = (NativeImage)value;
            return nativeImageToSkiaImage(nativeImage2);
         } else {
            NativeImageBackedTexture nativeTexture;
            NativeImage nativeImage;
            if (value instanceof NativeImageBackedTexture && (nativeImage = (nativeTexture = (NativeImageBackedTexture)value).getImage()) != null) {
               return nativeImageToSkiaImage(nativeImage);
            } else {
               for(Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
                  for(Field field : type.getDeclaredFields()) {
                     try {
                        field.setAccessible(true);
                        Object child = field.get(value);
                        Image image = extractNativeImage(child, visited, depth + 1);
                        if (image != null) {
                           return image;
                        }
                     } catch (Throwable var13) {
                     }
                  }

                  for(AccessibleObject accessibleObject : type.getDeclaredMethods()) {
                     String methodName;
                     if (((Method)accessibleObject).getParameterCount() == 0 && ((Method)accessibleObject).getReturnType() != Void.TYPE && ((Method)accessibleObject).getDeclaringClass() != Object.class && ((methodName = ((Method)accessibleObject).getName().toLowerCase()).contains("image") || methodName.contains("native") || methodName.contains("pixel") || methodName.contains("buffer") || methodName.contains("content") || methodName.contains("texture") || methodName.contains("data"))) {
                        try {
                           ((Method)accessibleObject).setAccessible(true);
                           Object child = ((Method)accessibleObject).invoke(value);
                           Image image = extractNativeImage(child, visited, depth + 1);
                           if (image != null) {
                              return image;
                           }
                        } catch (Throwable var14) {
                        }
                     }
                  }
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static Image nativeImageToSkiaImage(NativeImage nativeImage) {
      int width = nativeImage.getWidth();
      int height = nativeImage.getHeight();
      int rowBytes = width * 4;
      byte[] pixels = new byte[rowBytes * height];
      int index = 0;

      for(int y = 0; y < height; ++y) {
         for(int x = 0; x < width; ++x) {
            int color = nativeImage.getColor(x, y);
            pixels[index++] = (byte)(color & 255);
            pixels[index++] = (byte)(color >> 8 & 255);
            pixels[index++] = (byte)(color >> 16 & 255);
            pixels[index++] = (byte)(color >> 24 & 255);
         }
      }

      return Image.makeRasterFromBytes(new ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL), pixels, (long)rowBytes);
   }

   private static byte[] flipTextureRows(ByteBuffer buffer, int width, int height) {
      int rowBytes = width * 4;
      byte[] source = new byte[rowBytes * height];
      buffer.rewind();
      buffer.get(source);
      byte[] flipped = new byte[source.length];

      for(int row = 0; row < height; ++row) {
         int srcPos = row * rowBytes;
         int dstPos = (height - 1 - row) * rowBytes;
         System.arraycopy(source, srcPos, flipped, dstPos, rowBytes);
      }

      return flipped;
   }

   private static boolean isAllZero(byte[] pixels) {
      if (pixels != null && pixels.length != 0) {
         for(byte pixel : pixels) {
            if (pixel != 0) {
               return false;
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private static int getGlTextureId(AbstractTexture texture) {
      try {
         return ((IAbstractTexture)texture).arc$getGlIdRaw();
      } catch (Throwable var2) {
         return -1;
      }
   }

   private static boolean containsCjk(String text) {
      if (text != null && !text.isEmpty()) {
         for(int i = 0; i < text.length(); ++i) {
            Character.UnicodeScript script = UnicodeScript.of(text.charAt(i));
            if (script == UnicodeScript.HAN || script == UnicodeScript.HIRAGANA || script == UnicodeScript.KATAKANA || script == UnicodeScript.HANGUL) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static String stripFormatting(String string) {
      if (string == null) {
         return "";
      } else {
         return string.indexOf(167) < 0 ? string : STRIP_PATTERN.matcher(string).replaceAll("");
      }
   }

   private static Paint getTextPaint(Color color) {
      Paint paint = TEXT_PAINT;
      if (paint == null) {
         TEXT_PAINT = paint = new Paint();
      }

      paint.setARGB(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
      return paint;
   }

   private static float cachedAscent(Font font) {
      if (font == null) {
         return 0.0F;
      } else if (ASCENT_KEY == font) {
         return ASCENT_VAL;
      } else {
         ASCENT_KEY = font;
         ASCENT_VAL = font.getMetrics().getAscent();
         return ASCENT_VAL;
      }
   }

   static {
      SHADOW_FILTER = ImageFilter.makeBlur(3.0F, 3.0F, FilterTileMode.DECAL);
      TEXTURE_CACHE = new ConcurrentHashMap();
      URL_CACHE = new ConcurrentHashMap();
      STRIP_PATTERN = Pattern.compile("§.");
   }
}
