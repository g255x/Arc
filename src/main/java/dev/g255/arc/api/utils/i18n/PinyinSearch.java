package dev.g255.arc.api.utils.i18n;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 模块搜索用的拼音支持：中文、英文、拼音（全拼 / 首字母）三种写法都能命中同一个模块。
 *
 * <p>拼音表放在 {@code assets/arc/misc/pinyin.txt}，每行是「汉字 + 无声调全拼」
 * （GB2312 全集 6763 字，约 47KB）。第一次搜索时才读，之后常驻一个 HashMap。</p>
 *
 * <p>为什么不内置一张 Java 表：汉字表有 6000 多条，写进源码既难维护又拖慢编译；
 * 放资源文件还能在换字体/改翻译之后单独替换。</p>
 */
public final class PinyinSearch {
   private static final String RESOURCE = "/assets/arc/misc/pinyin.txt";
   private static volatile Map<Character, String> table;

   private PinyinSearch() {
   }

   private static Map<Character, String> table() {
      Map<Character, String> local = table;
      if (local != null) {
         return local;
      }

      synchronized (PinyinSearch.class) {
         if (table != null) {
            return table;
         }

         Map<Character, String> map = new HashMap<>(8192);
         try (InputStream in = PinyinSearch.class.getResourceAsStream(RESOURCE)) {
            if (in != null) {
               BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
               String line;
               while ((line = reader.readLine()) != null) {
                  if (line.length() < 2) {
                     continue;
                  }

                  map.put(line.charAt(0), line.substring(1).trim());
               }
            }
         } catch (Throwable ignored) {
            // 读不到就退化成「只按中英文匹配」，搜索本身仍然可用
         }

         table = map;
         return map;
      }
   }

   /** 单个字的无声调拼音；表里没有就返回空串 */
   public static String of(char c) {
      if (c < 128) {
         return String.valueOf(Character.toLowerCase(c));
      }

      String py = table().get(c);
      return py == null ? "" : py;
   }

   /** 整串的全拼：汉字转拼音，字母数字原样保留，其它字符丢掉 */
   public static String full(String text) {
      if (text == null || text.isEmpty()) {
         return "";
      }

      StringBuilder sb = new StringBuilder(text.length() * 3);
      for (int i = 0; i < text.length(); ++i) {
         char c = text.charAt(i);
         if (c < 128) {
            if (Character.isLetterOrDigit(c)) {
               sb.append(Character.toLowerCase(c));
            }
         } else {
            sb.append(of(c));
         }
      }

      return sb.toString();
   }

   /** 整串的首字母串：{@code 自围 → zw} */
   public static String initials(String text) {
      if (text == null || text.isEmpty()) {
         return "";
      }

      StringBuilder sb = new StringBuilder(text.length());
      for (int i = 0; i < text.length(); ++i) {
         char c = text.charAt(i);
         if (c < 128) {
            if (Character.isLetterOrDigit(c)) {
               sb.append(Character.toLowerCase(c));
            }
         } else {
            String py = of(c);
            if (!py.isEmpty()) {
               sb.append(py.charAt(0));
            }
         }
      }

      return sb.toString();
   }

   /** 查询串归一化：小写、去掉空格 */
   public static String normalize(String query) {
      if (query == null) {
         return "";
      }

      String lower = query.toLowerCase(Locale.ROOT);
      StringBuilder sb = new StringBuilder(lower.length());
      for (int i = 0; i < lower.length(); ++i) {
         char c = lower.charAt(i);
         if (!Character.isWhitespace(c)) {
            sb.append(c);
         }
      }

      return sb.toString();
   }

   /**
    * 一个模块是否命中查询：英文名子串、中文名子串、中文全拼、中文首字母，全都算命中。
    *
    * @param query    用户输入（已归一化或原始都行）
    * @param english  模块英文名，可为 null
    * @param chinese  模块中文名，可为 null
    */
   public static boolean matches(String query, String english, String chinese) {
      String q = normalize(query);
      if (q.isEmpty()) {
         return true;
      }

      if (english != null && english.toLowerCase(Locale.ROOT).contains(q)) {
         return true;
      }

      if (chinese != null && !chinese.isEmpty()) {
         if (chinese.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
         }

         if (full(chinese).contains(q)) {
            return true;
         }

         if (initials(chinese).contains(q)) {
            return true;
         }
      }

      return false;
   }
}
