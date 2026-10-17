package hoprc.api.live2d;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import hoprc.api.utils.win.CpuFeatures;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

interface CubismCoreLibrary extends Library {
   int csmGetVersion();

   int csmGetLatestMocVersion();

   int csmGetMocVersion(Pointer moc, int size);

   int csmHasMocConsistency(Pointer moc, int size);

   Pointer csmReviveMocInPlace(Pointer address, int size);

   int csmGetSizeofModel(Pointer moc);

   Pointer csmInitializeModelInPlace(Pointer moc, Pointer address, int size);

   void csmUpdateModel(Pointer model);

   void csmReadCanvasInfo(Pointer model, float[] outSizeInPixels, float[] outOriginInPixels, float[] outPixelsPerUnit);

   int csmGetParameterCount(Pointer model);

   Pointer csmGetParameterIds(Pointer model);

   Pointer csmGetParameterValues(Pointer model);

   Pointer csmGetParameterMinimumValues(Pointer model);

   Pointer csmGetParameterMaximumValues(Pointer model);

   Pointer csmGetParameterDefaultValues(Pointer model);

   int csmGetPartCount(Pointer model);

   Pointer csmGetPartIds(Pointer model);

   Pointer csmGetPartOpacities(Pointer model);

   int csmGetDrawableCount(Pointer model);

   Pointer csmGetDrawableIds(Pointer model);

   Pointer csmGetDrawableConstantFlags(Pointer model);

   Pointer csmGetDrawableDynamicFlags(Pointer model);

   Pointer csmGetDrawableTextureIndices(Pointer model);

   Pointer csmGetDrawableDrawOrders(Pointer model);

   Pointer csmGetRenderOrders(Pointer model);

   Pointer csmGetDrawableOpacities(Pointer model);

   Pointer csmGetDrawableMaskCounts(Pointer model);

   Pointer csmGetDrawableMasks(Pointer model);

   Pointer csmGetDrawableVertexCounts(Pointer model);

   Pointer csmGetDrawableVertexPositions(Pointer model);

   Pointer csmGetDrawableVertexUvs(Pointer model);

   Pointer csmGetDrawableIndexCounts(Pointer model);

   Pointer csmGetDrawableIndices(Pointer model);

   Pointer csmGetDrawableParentPartIndices(Pointer model);

   static CubismCoreLibrary load(Path gameDir) throws IOException {
      CubismCoreLibrary cached = Holder.core;
      if (cached != null) {
         return cached;
      }

      if (!CpuFeatures.isAvx2Supported()) {
         throw new UnsatisfiedLinkError("Live2DCubismCore.dll requires AVX2, which this CPU does not support");
      }

      synchronized (Holder.class) {
         if (Holder.core == null) {
            Holder.core = loadFrom(extractBundledDll(gameDir));
         }
         return Holder.core;
      }
   }

   static CubismCoreLibrary loadFrom(Path extractedDll) {
      if (extractedDll != null && Files.isRegularFile(extractedDll)) {
         try {
            return Native.load(extractedDll.toAbsolutePath().toString(), CubismCoreLibrary.class);
         } catch (UnsatisfiedLinkError ignored) {
         }
      }

      return loadFromSearchPath();
   }

   static CubismCoreLibrary loadFromSearchPath() {
      UnsatisfiedLinkError last = null;
      for (Path dir : searchDirs()) {
         if (Files.isDirectory(dir)) {
            NativeLibrary.addSearchPath("Live2DCubismCore", dir.toAbsolutePath().toString());
            NativeLibrary.addSearchPath("Live2DCubismCore_MD", dir.toAbsolutePath().toString());
         }
      }

      for (String name : List.of("Live2DCubismCore", "Live2DCubismCore_MD")) {
         try {
            return Native.load(name, CubismCoreLibrary.class);
         } catch (UnsatisfiedLinkError error) {
            last = error;
         }
      }

      throw last == null ? new UnsatisfiedLinkError("Live2DCubismCore.dll not found") : last;
   }

   static Path extractBundledDll(Path gameDir) throws IOException {
      String resourcePath = "/assets/forever/live2d/native/windows/x86_64/Live2DCubismCore.dll";
      try (InputStream input = CubismCoreLibrary.class.getResourceAsStream(resourcePath)) {
         if (input == null) {
            throw new IOException("Bundled Live2DCubismCore.dll not found: " + resourcePath);
         }

         byte[] dllBytes = input.readAllBytes();
         Path outputDir = gameDir.resolve("forever-live2d");
         Files.createDirectories(outputDir);
         Path output = outputDir.resolve("Live2DCubismCore-" + sha256Prefix(dllBytes) + ".dll");
         if (!sameContent(output, dllBytes)) {
            Files.write(output, dllBytes);
         }
         return output;
      }
   }

   static boolean sameContent(Path path, byte[] expected) throws IOException {
      if (!Files.isRegularFile(path) || Files.size(path) != expected.length) {
         return false;
      }
      byte[] current = Files.readAllBytes(path);
      if (current.length != expected.length) {
         return false;
      }
      for (int i = 0; i < expected.length; i++) {
         if (current[i] != expected[i]) {
            return false;
         }
      }
      return true;
   }

   static String sha256Prefix(byte[] bytes) {
      try {
         byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
         StringBuilder builder = new StringBuilder(16);
         for (int i = 0; i < 8; i++) {
            builder.append(String.format("%02x", digest[i]));
         }
         return builder.toString();
      } catch (NoSuchAlgorithmException exception) {
         throw new IllegalStateException("SHA-256 unavailable", exception);
      }
   }

   static List<Path> searchDirs() {
      Path cwd = Path.of("").toAbsolutePath();
      List<Path> dirs = new ArrayList<>();
      dirs.add(cwd.resolve("run").resolve("live2d"));
      dirs.add(cwd.resolve("live2d"));
      dirs.add(cwd.resolve("lib"));
      dirs.add(cwd);
      return dirs;
   }

   final class Holder {
      private static CubismCoreLibrary core;

      private Holder() {
      }
   }
}
