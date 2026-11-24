package dev.g255.arc.api.utils.rotation;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 旋转数值（yaw/pitch）出现 NaN 时的兜底与定位工具。
 *
 * <p>原版 {@code Entity.setYaw/setPitch} 遇到非有限值会直接丢弃，并每帧打印
 * "Invalid entity rotation: NaN, discarding."。窗口最小化等场景下某些计算会产出 NaN，
 * 那条日志就会刷屏。这里在写入前拦掉非法值（与原版丢弃行为一致），
 * 同时把**调用来源**打出来，方便定位到底是哪段代码算出了 NaN。
 */
public final class RotationNaNGuard {
   private static final Logger LOGGER = LoggerFactory.getLogger("Arc/Rotation");
   /** 最多打印几次调用栈，避免自己变成新的刷屏源 */
   private static final int MAX_REPORTS = 6;
   private static final AtomicInteger REPORTS = new AtomicInteger();
   private static final AtomicLong LAST = new AtomicLong(0L);

   private RotationNaNGuard() {
   }

   public static boolean isValid(float value) {
      return Float.isFinite(value);
   }

   public static void report(String where, float value) {
      int count = REPORTS.incrementAndGet();
      if (count > MAX_REPORTS) {
         return;
      }

      long now = System.currentTimeMillis();
      long last = LAST.get();
      if (now - last < 1000L || !LAST.compareAndSet(last, now)) {
         return;
      }

      LOGGER.warn("Discarded non-finite rotation in {} (value={}), caller stack:{}", where, value, trace());
   }

   private static String trace() {
      StringBuilder sb = new StringBuilder();
      int printed = 0;
      for (StackTraceElement element : new Throwable().getStackTrace()) {
         String className = element.getClassName();
         if (className.startsWith("java.") || className.startsWith("jdk.")
               || className.equals(RotationNaNGuard.class.getName())) {
            continue;
         }

         sb.append("\n    at ").append(element);
         if (++printed >= 8) {
            break;
         }
      }

      return sb.toString();
   }
}
