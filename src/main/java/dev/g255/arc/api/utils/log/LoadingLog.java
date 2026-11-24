package dev.g255.arc.api.utils.log;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

/**
 * 加载界面（SplashOverlay）底部那几行实时日志的来源。
 *
 * <p>做法是往 root logger 上挂一个 appender，把 INFO 及以上的日志塞进一个固定长度的环形缓冲。
 * 加载界面每帧从 {@link #snapshot(int)} 取最后 N 条画在进度条下面，
 * 越靠上的行透明度越低，形成「向上淡出」。</p>
 *
 * <p>为什么不直接用 {@code Arc} 自己的 logger：加载界面出现的时机比大部分模块初始化还早，
 * 而且用户想看的是「游戏在干什么」，包括 Minecraft 自身的资源加载、网络、声音系统的输出，
 * 所以挂 root logger。DEBUG/TRACE 量太大，在 {@link #append} 里就滤掉了。</p>
 */
public final class LoadingLog {
   /** 缓冲多少条。加载界面最多也就能画十几行，留 160 条够滚很久，又不至于占内存 */
   private static final int CAPACITY = 160;
   /** 单行最长字符数，超了截断 —— 长堆栈会把整屏刷满，而且渲染也要算宽度 */
   private static final int MAX_LENGTH = 180;

   private static final Deque<Line> LINES = new ArrayDeque<>();
   private static volatile boolean attached;

   private LoadingLog() {
   }

   /**
    * 挂上 appender。重复调用无副作用；环境不支持（logger 不是 log4j2 的 core 实现）时静默失败，
    * 加载界面上就是没有日志区，不影响游戏。
    */
   public static void attach() {
      if (attached) {
         return;
      }

      attached = true;
      try {
         org.apache.logging.log4j.Logger logger = LogManager.getRootLogger();
         if (!(logger instanceof Logger core)) {
            return;
         }

         Appender appender = new Appender();
         appender.start();
         core.addAppender(appender);
      } catch (Throwable ignored) {
         // 加载界面的日志只是锦上添花，装不上就算了
      }
   }

   /**
    * 取最后 {@code max} 条日志，按「旧 → 新」返回。
    */
   public static List<Line> snapshot(int max) {
      if (max <= 0) {
         return List.of();
      }

      synchronized (LINES) {
         int skip = Math.max(0, LINES.size() - max);
         ArrayList<Line> out = new ArrayList<>(Math.min(max, LINES.size()));
         int index = 0;

         for (Line line : LINES) {
            if (index++ < skip) {
               continue;
            }

            out.add(line);
         }

         return out;
      }
   }

   private static void push(Line line) {
      synchronized (LINES) {
         LINES.addLast(line);
         while (LINES.size() > CAPACITY) {
            LINES.removeFirst();
         }
      }
   }

   /** 一条日志：文本 + log4j 的 level 数值（越小越严重）+ 时间戳 */
   public static final class Line {
      public final String text;
      public final int level;
      public final long time;

      Line(String text, int level, long time) {
         this.text = text;
         this.level = level;
         this.time = time;
      }
   }

   private static final class Appender extends AbstractAppender {
      Appender() {
         // ignoreExceptions = true：这个 appender 只是给加载界面看的，绝不能因为它把日志链搞断
         super("arc-loading-log", null, null, true, Property.EMPTY_ARRAY);
      }

      @Override
      public void append(LogEvent event) {
         try {
            // intLevel：ERROR=200 / WARN=300 / INFO=400 / DEBUG=500，比 INFO 大的直接丢
            if (event.getLevel().intLevel() > Level.INFO.intLevel()) {
               return;
            }

            String text = event.getMessage().getFormattedMessage();
            if (text == null || text.isEmpty()) {
               return;
            }

            // 多行（堆栈）压成一行：加载界面是单行滚动的，换行会把布局撑乱
            text = text.replace('\n', ' ').replace('\r', ' ').trim();
            if (text.isEmpty()) {
               return;
            }

            if (text.length() > MAX_LENGTH) {
               text = text.substring(0, MAX_LENGTH) + "...";
            }

            push(new Line(text, event.getLevel().intLevel(), event.getTimeMillis()));
         } catch (Throwable ignored) {
            // appender 里抛异常会把整条日志链打断，吞掉
         }
      }
   }
}
