package dev.g255.arc.mod.gui.windows;

import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.api.utils.render.Render2DUtil;
import dev.g255.arc.mod.gui.ClickGuiScreen;
import dev.g255.arc.mod.modules.impl.client.ClickGui;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

public class WindowsScreen extends Screen {
   public static WindowBase lastClickedWindow;
   public static WindowBase draggingWindow;
   private List<WindowBase> windows = new ArrayList();

   public WindowsScreen(WindowBase... windows) {
      super(Text.of("CustomWindows"));
      this.windows.clear();
      lastClickedWindow = null;
      this.windows = Arrays.stream(windows).toList();
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      // 始终画一次背景：以前只在 nullCheck 成立时才画，游戏内打开就是完全透明的，
      // 看起来像界面没关掉。现在和 ClickGUI 一样有压暗/着色底板。
      this.renderBackground(context, mouseX, mouseY, delta);

      this.windows.stream().filter(WindowBase::isVisible).forEach((w) -> {
         if (w != lastClickedWindow) {
            w.render(context, mouseX, mouseY);
         }

      });
      if (lastClickedWindow != null && lastClickedWindow.isVisible()) {
         lastClickedWindow.render(context, mouseX, mouseY);
      }

   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.windows.forEach((w) -> w.mouseReleased(mouseX, mouseY, button));
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.windows.stream().filter(WindowBase::isVisible).forEach((w) -> w.mouseClicked(mouseX, mouseY, button));
      // 窗口右上角的 X 只是把窗口隐藏掉，最后一个窗口关掉后要退出本界面，
      // 否则会停留在一个只有背景的空界面里，鼠标还被界面抓着。
      if (this.windows.stream().noneMatch(WindowBase::isVisible)) {
         this.exitToClickGui();
         return true;
      }

      int i = Wrapper.mc.getWindow().getScaledWidth() / 2;
      float offset = (float)this.windows.size() * 20.0F / -2.0F - 23.0F;
      if (Render2DUtil.isHovered(mouseX, mouseY, (double)((float)i + offset + 1.0F), (double)(Wrapper.mc.getWindow().getScaledHeight() - 23), (double)15.0F, (double)15.0F)) {
         this.exitToClickGui();
         return true;
      }

      offset += 23.0F;

      for(WindowBase w2 : this.windows) {
         if (Render2DUtil.isHovered(mouseX, mouseY, (double)((float)i + offset), (double)(Wrapper.mc.getWindow().getScaledHeight() - 24), (double)17.0F, (double)17.0F)) {
            w2.setVisible(!w2.isVisible());
         }

         offset += 20.0F;
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      for(WindowBase window : this.windows) {
         if (window.isVisible() && window.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
         }
      }

      if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
         this.exitToClickGui();
         return true;
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   /**
    * 本界面是从 ClickGUI 打开的子界面：ClickGui 模块还开着就退回 ClickGUI，否则正常关闭回到游戏。
    */
   private void exitToClickGui() {
      ClickGui gui = ClickGui.getInstance();
      if (gui != null && gui.isOn()) {
         Wrapper.mc.setScreen(ClickGuiScreen.getInstance());
      } else {
         this.close();
      }
   }

   public boolean charTyped(char key, int keyCode) {
      this.windows.stream().filter(WindowBase::isVisible).forEach((w) -> w.charTyped(key, keyCode));
      return super.charTyped(key, keyCode);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.windows.stream().filter(WindowBase::isVisible).forEach((w) -> w.mouseScrolled((int)(verticalAmount * (double)5.0F)));
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }
}
