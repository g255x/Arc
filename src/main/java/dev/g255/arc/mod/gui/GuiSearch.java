package dev.g255.arc.mod.gui;

import dev.g255.arc.api.utils.i18n.PinyinSearch;
import dev.g255.arc.mod.modules.Module;

/**
 * ClickGUI 的唯一一个搜索框状态。
 *
 * <p>搜索框画在「模块」页顶部 Tab 下方（{@code ClickGuiScreen#drawSearchBar} 负责绘制与命中），
 * 但过滤的是**所有分类**的模块列表 —— 打一个字，战斗/移动/渲染/客户端同时收窄，
 * 等于在整个客户端里搜，而不是每个分类各挂一个输入框。</p>
 *
 * <p>状态是静态的：整个客户端只有一个 ClickGuiScreen 实例，搜索词跨栏共享。
 * 关闭界面时由 {@link #reset()} 清空，下次打开看到的一定是完整列表。</p>
 */
public final class GuiSearch {
   private static final int MAX_LENGTH = 32;

   private static String text = "";
   private static boolean focused;

   private GuiSearch() {
   }

   public static String getText() {
      return text;
   }

   public static boolean isEmpty() {
      return text.isEmpty();
   }

   public static boolean isFocused() {
      return focused;
   }

   public static void setFocused(boolean value) {
      focused = value;
   }

   public static void blur() {
      focused = false;
   }

   public static void reset() {
      text = "";
      focused = false;
   }

   /** 输入一个字符；控制字符（退格/回车等）由 {@link #keyPressed(int)} 处理 */
   public static void append(char chr) {
      if (!Character.isISOControl(chr) && text.length() < MAX_LENGTH) {
         text += chr;
      }
   }

   /**
    * 搜索框聚焦时的按键处理。
    *
    * @return true 表示这一键已经被搜索框吃掉，调用方不要再往下传
    *         （否则 ESC 会直接关整个 ClickGUI，退格会被当成模块绑定）
    */
   public static boolean keyPressed(int key) {
      if (!focused) {
         return false;
      }

      if (key == 259) {
         // 退格
         if (!text.isEmpty()) {
            text = text.substring(0, text.length() - 1);
         }
      } else if (key == 256) {
         // ESC：第一次清词并交出焦点，再按一次才关界面
         text = "";
         focused = false;
      } else if (key == 257) {
         // 回车：收起输入焦点，保留搜索词
         focused = false;
      }

      return true;
   }

   /** 模块是否命中当前搜索词：英文子串 / 中文子串 / 全拼 / 首字母 */
   public static boolean matches(Module module) {
      if (text.isEmpty() || module == null) {
         return true;
      }

      return PinyinSearch.matches(text, module.getName(), module.getChineseName());
   }
}
