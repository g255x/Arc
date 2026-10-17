package dev.baimaodaxia.mod.gui.windows;

import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.api.utils.render.Render2DUtil;
import dev.baimaodaxia.mod.gui.ClickGuiScreen;
import dev.baimaodaxia.mod.modules.Module;
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
      if (Module.nullCheck()) {
         this.renderBackground(context, mouseX, mouseY, delta);
      }

      this.windows.stream().filter((v0) -> v0.isVisible()).forEach((w) -> {
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
      this.windows.stream().filter((v0) -> v0.isVisible()).forEach((wx) -> wx.mouseClicked(mouseX, mouseY, button));
      int i = Wrapper.mc.getWindow().getScaledWidth() / 2;
      float offset = (float)this.windows.size() * 20.0F / -2.0F - 23.0F;
      if (Render2DUtil.isHovered(mouseX, mouseY, (double)((float)i + offset + 1.0F), (double)(Wrapper.mc.getWindow().getScaledHeight() - 23), (double)15.0F, (double)15.0F)) {
         Wrapper.mc.setScreen(ClickGuiScreen.getInstance());
      }

      float offset2 = offset + 23.0F;

      for(WindowBase w : this.windows) {
         if (Render2DUtil.isHovered(mouseX, mouseY, (double)((float)i + offset2), (double)(Wrapper.mc.getWindow().getScaledHeight() - 24), (double)17.0F, (double)17.0F)) {
            w.setVisible(!w.isVisible());
         }

         offset2 += 20.0F;
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.windows.stream().filter((v0) -> v0.isVisible()).forEach((w) -> w.keyPressed(keyCode, scanCode, modifiers));
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public boolean charTyped(char key, int keyCode) {
      this.windows.stream().filter((v0) -> v0.isVisible()).forEach((w) -> w.charTyped(key, keyCode));
      return super.charTyped(key, keyCode);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.windows.stream().filter((v0) -> v0.isVisible()).forEach((w) -> w.mouseScrolled((int)(verticalAmount * (double)5.0F)));
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }
}
