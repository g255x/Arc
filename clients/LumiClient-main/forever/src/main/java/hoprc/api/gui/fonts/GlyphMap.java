package hoprc.api.gui.fonts;

import com.mojang.blaze3d.systems.RenderSystem;
import hoprc.asm.accessors.INativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImage.Format;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class GlyphMap {
   final int fromIncl;
   final int toExcl;
   final Font font;
   Font secondFont;
   final Identifier bindToTexture;
   final int pixelPadding;
   private final Int2ObjectMap<Glyph> glyphs = new Int2ObjectOpenHashMap<>();
   int width;
   int height;
   boolean generated = false;

   public GlyphMap(int from, int to, Font font, Identifier identifier, int padding) {
      this.fromIncl = from;
      this.toExcl = to;
      this.font = font;
      this.bindToTexture = identifier;
      this.pixelPadding = padding;
   }

   public GlyphMap(int from, int to, Font font, Font secondFont, Identifier identifier, int padding) {
      this(from, to, font, identifier, padding);
      this.secondFont = secondFont;
   }

   public static void registerBufferedImageTexture(Identifier i, BufferedImage bi) {
      try {
         int ow = bi.getWidth();
         int oh = bi.getHeight();
         NativeImage image = new NativeImage(Format.RGBA, ow, oh, false);
         long ptr = ((INativeImage)(Object)image).getPointer();
         IntBuffer backingBuffer = MemoryUtil.memIntBuffer(ptr, image.getWidth() * image.getHeight());
         int off = 0;
         WritableRaster _ra = bi.getRaster();
         ColorModel _cm = bi.getColorModel();
         int nbands = _ra.getNumBands();
         int dataType = _ra.getDataBuffer().getDataType();

         Object _d = switch (dataType) {
            case 0 -> new byte[nbands];
            case 1 -> new short[nbands];
            default -> throw new IllegalArgumentException("Unknown data buffer type: " + dataType);
            case 3 -> new int[nbands];
            case 4 -> new float[nbands];
            case 5 -> new double[nbands];
         };

         for (int y = 0; y < oh; y++) {
            for (int x = 0; x < ow; x++) {
               _ra.getDataElements(x, y, _d);
               int a = _cm.getAlpha(_d);
               int r = _cm.getRed(_d);
               int g = _cm.getGreen(_d);
               int b = _cm.getBlue(_d);
               int abgr = a << 24 | b << 16 | g << 8 | r;
               backingBuffer.put(abgr);
            }
         }

         NativeImageBackedTexture tex = new NativeImageBackedTexture(image);
         tex.upload();
         if (RenderSystem.isOnRenderThread()) {
            MinecraftClient.getInstance().getTextureManager().registerTexture(i, tex);
         } else {
            RenderSystem.recordRenderCall(() -> MinecraftClient.getInstance().getTextureManager().registerTexture(i, tex));
         }
      } catch (Throwable var21) {
         var21.printStackTrace();
      }
   }

   public Glyph getGlyph(int c) {
      if (!this.generated) {
         this.generate();
      }

      return (Glyph)this.glyphs.get(c);
   }

   public void destroy() {
      MinecraftClient.getInstance().getTextureManager().destroyTexture(this.bindToTexture);
      this.glyphs.clear();
      this.width = -1;
      this.height = -1;
      this.generated = false;
   }

   public boolean contains(int c) {
      return c >= this.fromIncl && c < this.toExcl;
   }

   private Font getFontForGlyph(int c) {
      return this.font.canDisplay(c) ? this.font : (Font)Objects.requireNonNullElse(this.secondFont, this.font);
   }

   public void generate() {
      if (!this.generated) {
         int range = this.toExcl - this.fromIncl - 1;
         int charsVert = (int)(Math.ceil(Math.sqrt(range)) * 1.5);
         this.glyphs.clear();
         int generatedChars = 0;
         int charNX = 0;
         int maxX = 0;
         int maxY = 0;
         int currentX = 0;
         int currentY = 0;
         int currentRowMaxY = 0;
         List<Glyph> glyphs1 = new ArrayList<>();
         AffineTransform af = new AffineTransform();

         for (FontRenderContext frc = new FontRenderContext(af, true, false); generatedChars <= range; charNX++) {
            int currentChar = this.fromIncl + generatedChars;
            Font font = this.getFontForGlyph(currentChar);

            String charStr = new String(Character.toChars(currentChar));
            Rectangle2D stringBounds = font.getStringBounds(charStr, frc);

            int width;
            int height;

            if (currentChar == 0xF0521 || currentChar == 0xF0A19) {
               java.awt.font.GlyphVector gv = font.createGlyphVector(frc, charStr);
               Rectangle2D visualBounds = gv.getVisualBounds();
               width = (int) Math.ceil(Math.max(stringBounds.getWidth(), visualBounds.getWidth())) + 6;
               height = (int) Math.ceil(Math.max(stringBounds.getHeight(), visualBounds.getHeight())) + 6;
            } else {
               width = (int) Math.ceil(stringBounds.getWidth());
               height = (int) Math.ceil(stringBounds.getHeight());
            }

            generatedChars++;
            maxX = Math.max(maxX, currentX + width);
            maxY = Math.max(maxY, currentY + height);
            if (charNX >= charsVert) {
               currentX = 0;
               currentY += currentRowMaxY + this.pixelPadding;
               charNX = 0;
               currentRowMaxY = 0;
            }

            currentRowMaxY = Math.max(currentRowMaxY, height);

            int cellStartX = currentX;
            if (currentChar == 0xF0521 || currentChar == 0xF0A19) {
               cellStartX += 6;
            }

            glyphs1.add(new Glyph(cellStartX, currentY, width, height, currentChar, this));
            int extraPad = (currentChar == 0xF0521 || currentChar == 0xF0A19) ? 8 : 0;
            currentX += width + this.pixelPadding + extraPad;
         }

         BufferedImage bi = new BufferedImage(Math.max(maxX + this.pixelPadding, 1), Math.max(maxY + this.pixelPadding, 1), 2);
         this.width = bi.getWidth();
         this.height = bi.getHeight();
         Graphics2D g2d = bi.createGraphics();
         g2d.setColor(new Color(255, 255, 255, 0));
         g2d.fillRect(0, 0, this.width, this.height);
         g2d.setColor(Color.WHITE);
         g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
         g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         for (Glyph glyph : glyphs1) {
            Font glyphFont = this.getFontForGlyph(glyph.value());
            g2d.setFont(glyphFont);
            FontMetrics fontMetrics = g2d.getFontMetrics();

            String drawStr = new String(Character.toChars(glyph.value()));
            int drawY = glyph.v() + fontMetrics.getAscent();

            int drawX = glyph.u();
            if (glyph.value() == 0xF0521 || glyph.value() == 0xF0A19) {
               FontRenderContext frc2 = g2d.getFontRenderContext();
               java.awt.font.GlyphVector gv = glyphFont.createGlyphVector(frc2, drawStr);
               Rectangle2D visualBounds = gv.getVisualBounds();
               drawX = glyph.u() - (int) Math.floor(visualBounds.getX()) + 3;
            }

            g2d.drawString(drawStr, drawX, drawY);
            this.glyphs.put(glyph.value(), glyph);
         }

         registerBufferedImageTexture(this.bindToTexture, bi);
         this.generated = true;
      }
   }
}