package dev.g255.arc.mod.modules.impl.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.g255.arc.mod.gui.Alignment;
import dev.g255.arc.mod.gui.HudElement;
import dev.g255.arc.mod.gui.clickgui.ClickGuiScreen;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.resource.Resource;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage.Format;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AnimatedGif extends Module implements HudElement {
   public static AnimatedGif INSTANCE;
   public final EnumSetting<GifMode> mode;
   public final SliderSetting widthSize;
   private final SliderSetting x;
   private final SliderSetting y;
   private final List<Frame> frames;
   private boolean loaded;
   private GifMode loadedMode;
   private int gifWidth;
   private int gifHeight;
   private int totalDuration;

   public AnimatedGif() {
      super("AnimatedGif", "Screen animated GIF overlay", Module.Category.HUD);
      this.mode = this.add(new EnumSetting("Mode", AnimatedGif.GifMode.cat));
      this.widthSize = this.add(new SliderSetting("WidthSize", (double)100.0F, (double)50.0F, (double)500.0F, (double)1.0F));
      this.x = this.add(new SliderSetting("X", 20, 0, 1920));
      this.y = this.add(new SliderSetting("Y", 20, 0, 1080));
      this.frames = new ArrayList();
      this.loaded = false;
      this.loadedMode = null;
      this.gifWidth = 100;
      this.gifHeight = 100;
      this.totalDuration = 0;
      this.setChinese("屏幕动图");
      INSTANCE = this;
   }

   public void onEnable() {
      this.loaded = false;
      this.loadGif();
   }

   public void onDisable() {
      this.clearFrames();
      this.loaded = false;
   }

   private void clearFrames() {
      if (mc != null && mc.getTextureManager() != null) {
         for(Frame frame : this.frames) {
            try {
               mc.getTextureManager().destroyTexture(frame.identifier);
            } catch (Exception var4) {
            }
         }
      }

      this.frames.clear();
      this.totalDuration = 0;
   }

   private void loadGif() {
      if (mc != null && mc.getResourceManager() != null && mc.getTextureManager() != null) {
         if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(this::loadGif);
         } else {
            GifMode gifMode = this.mode.getValue();
            this.loadedMode = gifMode;
            this.loaded = true;
            this.clearFrames();

            try {
               String fileName = gifMode.getFileName();
               Identifier resId = Identifier.of("arc", "gif/" + fileName);
               Optional<Resource> resource = mc.getResourceManager().getResource(resId);
               if (resource.isEmpty()) {
                  System.out.println("[GifHud] 未找到 GIF: " + fileName);
               } else {
                  InputStream inputStream = ((Resource)resource.get()).getInputStream();

                  label192: {
                     try {
                        ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream);

                        label170: {
                           try {
                              Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
                              if (readers.hasNext()) {
                                 ImageReader reader = (ImageReader)readers.next();
                                 reader.setInput(imageInputStream, false);
                                 int frameCount = reader.getNumImages(true);
                                 BufferedImage canvas = null;
                                 Graphics graphics = null;
                                 BufferedImage previous = null;

                                 for(int i = 0; i < frameCount; ++i) {
                                    BufferedImage frameImage = reader.read(i);
                                    IIOMetadata metadata = reader.getImageMetadata(i);
                                    Node tree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
                                    int left = 0;
                                    int top = 0;
                                    int delay = 10;
                                    String disposal = "none";
                                    NodeList nodes = tree.getChildNodes();

                                    for(int j = 0; j < nodes.getLength(); ++j) {
                                       Node node = nodes.item(j);
                                       if (node.getNodeName().equalsIgnoreCase("ImageDescriptor")) {
                                          NamedNodeMap attrs = node.getAttributes();
                                          left = Integer.parseInt(attrs.getNamedItem("imageLeftPosition").getNodeValue());
                                          top = Integer.parseInt(attrs.getNamedItem("imageTopPosition").getNodeValue());
                                          if (canvas == null) {
                                             this.gifWidth = Integer.parseInt(attrs.getNamedItem("imageWidth").getNodeValue()) + left;
                                             this.gifHeight = Integer.parseInt(attrs.getNamedItem("imageHeight").getNodeValue()) + top;
                                          }
                                       } else if (node.getNodeName().equalsIgnoreCase("GraphicControlExtension")) {
                                          NamedNodeMap attrs = node.getAttributes();
                                          disposal = attrs.getNamedItem("disposalMethod").getNodeValue();
                                          delay = Integer.parseInt(attrs.getNamedItem("delayTime").getNodeValue());
                                       }
                                    }

                                    int duration = delay == 0 ? 100 : delay * 10;
                                    if (canvas == null) {
                                       this.gifWidth = Math.max(this.gifWidth, frameImage.getWidth());
                                       this.gifHeight = Math.max(this.gifHeight, frameImage.getHeight());
                                       canvas = new BufferedImage(this.gifWidth, this.gifHeight, 2);
                                       graphics = canvas.createGraphics();
                                       ((Graphics2D)graphics).setBackground(new Color(0, 0, 0, 0));
                                    }

                                    if (disposal.equals("restoreToPrevious")) {
                                       previous = new BufferedImage(this.gifWidth, this.gifHeight, 2);
                                       Graphics prevG = previous.createGraphics();
                                       prevG.drawImage(canvas, 0, 0, (ImageObserver)null);
                                       prevG.dispose();
                                    }

                                    graphics.drawImage(frameImage, left, top, (ImageObserver)null);
                                    NativeImage nativeImage = new NativeImage(Format.RGBA, this.gifWidth, this.gifHeight, false);

                                    for(int px = 0; px < this.gifWidth; ++px) {
                                       for(int py = 0; py < this.gifHeight; ++py) {
                                          int argb = canvas.getRGB(px, py);
                                          int a = argb >> 24 & 255;
                                          int r = argb >> 16 & 255;
                                          int g = argb >> 8 & 255;
                                          int b = argb & 255;
                                          int abgr = a << 24 | b << 16 | g << 8 | r;
                                          nativeImage.setColor(px, py, abgr);
                                       }
                                    }

                                    long now = System.currentTimeMillis();
                                    String baseName = fileName.replace(".gif", "");
                                    Identifier texId = Identifier.of("arc", "dyn_gif_" + baseName + "_" + i + "_" + now);
                                    mc.getTextureManager().registerTexture(texId, new NativeImageBackedTexture(nativeImage));
                                    this.frames.add(new Frame(texId, duration));
                                    this.totalDuration += duration;
                                    if (disposal.equals("restoreToBackgroundColor")) {
                                       ((Graphics2D)graphics).setComposite(AlphaComposite.Clear);
                                       graphics.fillRect(left, top, frameImage.getWidth(), frameImage.getHeight());
                                       ((Graphics2D)graphics).setComposite(AlphaComposite.SrcOver);
                                    } else if (disposal.equals("restoreToPrevious") && previous != null) {
                                       ((Graphics2D)graphics).setComposite(AlphaComposite.Clear);
                                       graphics.fillRect(0, 0, this.gifWidth, this.gifHeight);
                                       ((Graphics2D)graphics).setComposite(AlphaComposite.SrcOver);
                                       graphics.drawImage(previous, 0, 0, (ImageObserver)null);
                                    }
                                 }

                                 if (graphics != null) {
                                    graphics.dispose();
                                 }

                                 System.out.println("[GifHud] 解析完毕！共 " + frameCount + " 帧。");
                                 break label170;
                              }
                           } catch (Throwable var34) {
                              if (imageInputStream != null) {
                                 try {
                                    imageInputStream.close();
                                 } catch (Throwable var33) {
                                    var34.addSuppressed(var33);
                                 }
                              }

                              throw var34;
                           }

                           if (imageInputStream != null) {
                              imageInputStream.close();
                           }
                           break label192;
                        }

                        if (imageInputStream != null) {
                           imageInputStream.close();
                        }
                     } catch (Throwable var35) {
                        if (inputStream != null) {
                           try {
                              inputStream.close();
                           } catch (Throwable var32) {
                              var35.addSuppressed(var32);
                           }
                        }

                        throw var35;
                     }

                     if (inputStream != null) {
                        inputStream.close();
                     }

                     return;
                  }

                  if (inputStream != null) {
                     inputStream.close();
                  }

               }
            } catch (Exception exception) {
               exception.printStackTrace();
               System.out.println("[GifHud] GIF解析严重失败！");
               this.loaded = false;
            }
         }
      }
   }

   public void onRender2D(DrawContext context, float tickDelta) {
      if (!ClickGuiScreen.isEditingHud()) {
         this.render(context, tickDelta);
      }
   }

   public void render(DrawContext context, float tickDelta) {
      if (this.loadedMode != this.mode.getValue()) {
         this.loaded = false;
      }

      if (!this.loaded) {
         this.loadGif();
      }

      if (!this.frames.isEmpty() && this.totalDuration != 0 && this.gifWidth > 0 && this.gifHeight > 0) {
         int width = this.widthSize.getValueInt();
         int height = (int)((float)width * ((float)this.gifHeight / (float)this.gifWidth));
         int px = this.getX();
         int py = this.getY();
         long time = System.currentTimeMillis() % (long)this.totalDuration;
         long acc = 0L;
         Identifier current = ((Frame)this.frames.get(0)).identifier;

         for(Frame frame : this.frames) {
            acc += (long)frame.duration;
            if (time < acc) {
               current = frame.identifier;
               break;
            }
         }

         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         context.drawTexture(current, px, py, 0.0F, 0.0F, width, height, width, height);
         RenderSystem.disableBlend();
      }
   }

   public String getDisplayName() {
      return super.getDisplayName();
   }

   public int getX() {
      return this.x.getValueInt();
   }

   public void setX(int nx) {
      this.x.setValue((double)nx);
   }

   public int getY() {
      return this.y.getValueInt();
   }

   public void setY(int ny) {
      this.y.setValue((double)ny);
   }

   public int getWidth() {
      return this.widthSize.getValueInt();
   }

   public int getHeight() {
      return this.gifWidth > 0 && this.gifHeight > 0 ? (int)((float)this.widthSize.getValueInt() * ((float)this.gifHeight / (float)this.gifWidth)) : this.widthSize.getValueInt();
   }

   public boolean isVisible() {
      return this.isOn();
   }

   public Alignment getSide() {
      return Alignment.LEFT;
   }

   public static enum GifMode {
      cat("cat.gif"),
      kou("kou.gif"),
      kiss_cum("kiss_cum.gif"),
      shazi("shazi.gif");

      private final String fileName;

      private GifMode(String fileName) {
         this.fileName = fileName;
      }

      public String getFileName() {
         return this.fileName;
      }

      // $FF: synthetic method
      private static GifMode[] $values() {
         return new GifMode[]{cat, kou, kiss_cum, shazi};
      }
   }

   private static class Frame {
      final Identifier identifier;
      final int duration;

      Frame(Identifier identifier, int duration) {
         this.identifier = identifier;
         this.duration = duration;
      }
   }
}
