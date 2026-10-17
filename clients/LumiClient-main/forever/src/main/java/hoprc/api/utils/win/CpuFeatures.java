package hoprc.api.utils.win;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class CpuFeatures {

   private interface Kernel32Lib extends Library {
      Kernel32Lib INSTANCE = Native.load("kernel32", Kernel32Lib.class);

      boolean IsProcessorFeaturePresent(int processorFeature);
   }

   private static final int PF_AVX2_INSTRUCTIONS_AVAILABLE = 40;

   private static volatile Boolean avx2Supported;

   public static boolean isAvx2Supported() {
      Boolean cached = avx2Supported;
      if (cached != null) {
         return cached;
      }

      synchronized (CpuFeatures.class) {
         if (avx2Supported == null) {
            avx2Supported = detectAvx2();
         }
         return avx2Supported;
      }
   }

   private static boolean detectAvx2() {
      String osName = System.getProperty("os.name", "").toLowerCase();
      if (!osName.contains("win")) {
         return true;
      }

      Boolean viaHotspot = detectViaHotspotUseAvx();
      if (viaHotspot != null) {
         return viaHotspot;
      }

      try {
         // PF_AVX2_INSTRUCTIONS_AVAILABLE is only recognized on Windows 11 /
         // Server 2022+; older Windows returns FALSE for unknown constants,
         // so this path is a last resort and may under-report support.
         return Kernel32Lib.INSTANCE.IsProcessorFeaturePresent(PF_AVX2_INSTRUCTIONS_AVAILABLE);
      } catch (Throwable throwable) {
         return true;
      }
   }

   private static Boolean detectViaHotspotUseAvx() {
      try {
         com.sun.management.HotSpotDiagnosticMXBean bean =
               java.lang.management.ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);
         if (bean == null) {
            return null;
         }
         // HotSpot derives UseAVX from CPUID at startup: 0 = none/SSE only,
         // 1 = AVX, 2 = AVX2, 3 = AVX-512.
         String value = bean.getVMOption("UseAVX").getValue();
         return Integer.parseInt(value.trim()) >= 2;
      } catch (Throwable throwable) {
         return null;
      }
   }

   private CpuFeatures() {
   }
}
