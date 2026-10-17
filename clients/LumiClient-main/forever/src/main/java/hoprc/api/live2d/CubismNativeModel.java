package hoprc.api.live2d;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class CubismNativeModel implements Closeable {
   private static final int DYNAMIC_FLAG_IS_VISIBLE = 1;
   private static final int CONSTANT_FLAG_BLEND_ADDITIVE = 1;
   private static final int CONSTANT_FLAG_BLEND_MULTIPLICATIVE = 1 << 1;
   private static final int MOC_ALIGNMENT = 64;
   private static final int MODEL_ALIGNMENT = 16;

   private final CubismCoreLibrary core;
   private final Memory mocMemory;
   private final Memory modelMemory;
   private final Pointer model;
   private final Map<String, Integer> parameterIndices = new HashMap<>();
   private final Map<Integer, String> partIds = new HashMap<>();

   private final int drawableCount;
   private final int parameterCount;
   private final float canvasWidth;
   private final float canvasHeight;
   private final float originX;
   private final float originY;
   private final float pixelsPerUnit;
   private float boundsMinX;
   private float boundsMinY;
   private float boundsMaxX;
   private float boundsMaxY;

   public static CubismNativeModel load(MinecraftClient client, Identifier mocId, Path gameDir) throws IOException {
      try (InputStream input = client.getResourceManager().getResourceOrThrow(mocId).getInputStream()) {
         CubismCoreLibrary core = CubismCoreLibrary.load(gameDir);
         return new CubismNativeModel(input.readAllBytes(), core);
      }
   }

   private CubismNativeModel(byte[] mocBytes, CubismCoreLibrary core) {
      this.core = core;
      this.mocMemory = new Memory(mocBytes.length + MOC_ALIGNMENT);
      Pointer alignedMocMemory = alignedPointer(this.mocMemory, MOC_ALIGNMENT);
      alignedMocMemory.write(0, mocBytes, 0, mocBytes.length);

      int consistent = this.core.csmHasMocConsistency(alignedMocMemory, mocBytes.length);
      if (consistent == 0) {
         throw new IllegalStateException("Invalid Cubism moc3 data");
      }

      Pointer moc = this.core.csmReviveMocInPlace(alignedMocMemory, mocBytes.length);
      if (moc == null || Pointer.nativeValue(moc) == 0L) {
         throw new IllegalStateException("Cubism Core failed to revive moc");
      }

      int modelSize = this.core.csmGetSizeofModel(moc);
      this.modelMemory = new Memory(modelSize + MODEL_ALIGNMENT);
      Pointer alignedModelMemory = alignedPointer(this.modelMemory, MODEL_ALIGNMENT);
      this.model = this.core.csmInitializeModelInPlace(moc, alignedModelMemory, modelSize);
      if (this.model == null || Pointer.nativeValue(this.model) == 0L) {
         throw new IllegalStateException("Cubism Core failed to initialize model");
      }

      this.parameterCount = this.core.csmGetParameterCount(this.model);
      this.drawableCount = this.core.csmGetDrawableCount(this.model);
      cacheParameterIndices();
      cachePartIds();

      float[] size = new float[2];
      float[] origin = new float[2];
      float[] ppu = new float[1];
      this.core.csmReadCanvasInfo(this.model, size, origin, ppu);
      this.canvasWidth = size[0] / Math.max(1.0F, ppu[0]);
      this.canvasHeight = size[1] / Math.max(1.0F, ppu[0]);
      this.originX = origin[0] / Math.max(1.0F, ppu[0]);
      this.originY = origin[1] / Math.max(1.0F, ppu[0]);
      this.pixelsPerUnit = ppu[0];
      update();
      refreshBounds();
   }

   private static Pointer alignedPointer(Pointer pointer, int alignment) {
      long address = Pointer.nativeValue(pointer);
      long aligned = (address + alignment - 1L) & -alignment;
      return pointer.share(aligned - address);
   }

   public void update() {
      this.core.csmUpdateModel(this.model);
   }

   public void setParameter(String id, float value) {
      Integer index = this.parameterIndices.get(id);
      if (index == null) {
         return;
      }
      setParameter(index, value);
   }

   public void setParameter(int index, float value) {
      if (index < 0 || index >= this.parameterCount) {
         return;
      }

      float min = this.core.csmGetParameterMinimumValues(this.model).getFloat((long)index * Float.BYTES);
      float max = this.core.csmGetParameterMaximumValues(this.model).getFloat((long)index * Float.BYTES);
      float clamped = Math.max(min, Math.min(max, value));
      this.core.csmGetParameterValues(this.model).setFloat((long)index * Float.BYTES, clamped);
   }

   public int getDrawableCount() {
      return this.drawableCount;
   }

   public float getCanvasWidth() {
      return this.canvasWidth;
   }

   public float getCanvasHeight() {
      return this.canvasHeight;
   }

   public float getOriginX() {
      return this.originX;
   }

   public float getOriginY() {
      return this.originY;
   }

   public float getPixelsPerUnit() {
      return this.pixelsPerUnit;
   }

   public float getBoundsMinX() {
      return this.boundsMinX;
   }

   public float getBoundsMinY() {
      return this.boundsMinY;
   }

   public float getBoundsMaxX() {
      return this.boundsMaxX;
   }

   public float getBoundsMaxY() {
      return this.boundsMaxY;
   }

   public float getBoundsWidth() {
      return Math.max(0.001F, this.boundsMaxX - this.boundsMinX);
   }

   public float getBoundsHeight() {
      return Math.max(0.001F, this.boundsMaxY - this.boundsMinY);
   }

   public int getDrawableRenderOrder(int drawableIndex) {
      return this.core.csmGetRenderOrders(this.model).getInt((long)drawableIndex * Integer.BYTES);
   }

   public int getDrawableDrawOrder(int drawableIndex) {
      return this.core.csmGetDrawableDrawOrders(this.model).getInt((long)drawableIndex * Integer.BYTES);
   }

   public String getDrawableId(int drawableIndex) {
      Pointer idsPointer = this.core.csmGetDrawableIds(this.model);
      Pointer idPointer = idsPointer.getPointer((long)drawableIndex * Native.POINTER_SIZE);
      return idPointer == null ? "" : readCString(idPointer);
   }

   public int getDrawableParentPartIndex(int drawableIndex) {
      return this.core.csmGetDrawableParentPartIndices(this.model).getInt((long)drawableIndex * Integer.BYTES);
   }

   public String getDrawableParentPartId(int drawableIndex) {
      return this.partIds.getOrDefault(getDrawableParentPartIndex(drawableIndex), "");
   }

   public boolean isDrawableVisible(int drawableIndex) {
      byte flags = this.core.csmGetDrawableDynamicFlags(this.model).getByte(drawableIndex);
      return (flags & DYNAMIC_FLAG_IS_VISIBLE) != 0;
   }

   public boolean isDrawableAdditive(int drawableIndex) {
      byte flags = this.core.csmGetDrawableConstantFlags(this.model).getByte(drawableIndex);
      return (flags & CONSTANT_FLAG_BLEND_ADDITIVE) != 0;
   }

   public boolean isDrawableMultiplicative(int drawableIndex) {
      byte flags = this.core.csmGetDrawableConstantFlags(this.model).getByte(drawableIndex);
      return (flags & CONSTANT_FLAG_BLEND_MULTIPLICATIVE) != 0;
   }

   public int getDrawableTextureIndex(int drawableIndex) {
      return this.core.csmGetDrawableTextureIndices(this.model).getInt((long)drawableIndex * Integer.BYTES);
   }

   public float getDrawableOpacity(int drawableIndex) {
      return this.core.csmGetDrawableOpacities(this.model).getFloat((long)drawableIndex * Float.BYTES);
   }

   public int getDrawableVertexCount(int drawableIndex) {
      return this.core.csmGetDrawableVertexCounts(this.model).getInt((long)drawableIndex * Integer.BYTES);
   }

   public int getDrawableIndexCount(int drawableIndex) {
      return this.core.csmGetDrawableIndexCounts(this.model).getInt((long)drawableIndex * Integer.BYTES);
   }

   public int getDrawableMaskCount(int drawableIndex) {
      return this.core.csmGetDrawableMaskCounts(this.model).getInt((long)drawableIndex * Integer.BYTES);
   }

   public float[] getDrawableVertexPositions(int drawableIndex) {
      int count = getDrawableVertexCount(drawableIndex) * 2;
      Pointer pointer = this.core.csmGetDrawableVertexPositions(this.model).getPointer((long)drawableIndex * Native.POINTER_SIZE);
      return pointer == null ? new float[0] : pointer.getFloatArray(0, count);
   }

   public float[] getDrawableVertexUvs(int drawableIndex) {
      int count = getDrawableVertexCount(drawableIndex) * 2;
      Pointer pointer = this.core.csmGetDrawableVertexUvs(this.model).getPointer((long)drawableIndex * Native.POINTER_SIZE);
      return pointer == null ? new float[0] : pointer.getFloatArray(0, count);
   }

   public short[] getDrawableIndices(int drawableIndex) {
      int count = getDrawableIndexCount(drawableIndex);
      Pointer pointer = this.core.csmGetDrawableIndices(this.model).getPointer((long)drawableIndex * Native.POINTER_SIZE);
      return pointer == null ? new short[0] : pointer.getShortArray(0, count);
   }

   private void cacheParameterIndices() {
      Pointer idsPointer = this.core.csmGetParameterIds(this.model);
      for (int i = 0; i < this.parameterCount; i++) {
         Pointer idPointer = idsPointer.getPointer((long)i * Native.POINTER_SIZE);
         if (idPointer != null) {
            String id = readCString(idPointer);
            if (!id.isEmpty()) {
               this.parameterIndices.put(id, i);
            }
         }
      }
   }

   private void cachePartIds() {
      int partCount = this.core.csmGetPartCount(this.model);
      Pointer idsPointer = this.core.csmGetPartIds(this.model);
      for (int i = 0; i < partCount; i++) {
         Pointer idPointer = idsPointer.getPointer((long)i * Native.POINTER_SIZE);
         if (idPointer != null) {
            this.partIds.put(i, readCString(idPointer));
         }
      }
   }

   private void refreshBounds() {
      float minX = Float.POSITIVE_INFINITY;
      float minY = Float.POSITIVE_INFINITY;
      float maxX = Float.NEGATIVE_INFINITY;
      float maxY = Float.NEGATIVE_INFINITY;

      for (int drawable = 0; drawable < this.drawableCount; drawable++) {
         if (!isDrawableVisible(drawable)) {
            continue;
         }

         float[] vertices = getDrawableVertexPositions(drawable);
         for (int i = 0; i + 1 < vertices.length; i += 2) {
            minX = Math.min(minX, vertices[i]);
            maxX = Math.max(maxX, vertices[i]);
            minY = Math.min(minY, vertices[i + 1]);
            maxY = Math.max(maxY, vertices[i + 1]);
         }
      }

      if (minX == Float.POSITIVE_INFINITY) {
         minX = this.originX - this.canvasWidth * 0.5F;
         maxX = this.originX + this.canvasWidth * 0.5F;
         minY = this.originY - this.canvasHeight * 0.5F;
         maxY = this.originY + this.canvasHeight * 0.5F;
      }

      this.boundsMinX = minX;
      this.boundsMinY = minY;
      this.boundsMaxX = maxX;
      this.boundsMaxY = maxY;
   }

   private String readCString(Pointer pointer) {
      byte[] bytes = pointer.getByteArray(0, 128);
      int length = 0;
      while (length < bytes.length && bytes[length] != 0) {
         length++;
      }
      return new String(Arrays.copyOf(bytes, length), StandardCharsets.UTF_8);
   }

   @Override
   public void close() {
      this.parameterIndices.clear();
      this.partIds.clear();
      this.modelMemory.close();
      this.mocMemory.close();
   }
}
