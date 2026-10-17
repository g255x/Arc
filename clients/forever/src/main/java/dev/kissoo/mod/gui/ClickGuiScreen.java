package dev.kissoo.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.Forever;
import hoprc.api.core.impl.ShaderManager;
import dev.kissoo.core.impl.ShaderParams;
import dev.kissoo.api.utils.Wrapper;
import dev.kissoo.api.utils.math.SmoothAnimationUtil;
import dev.kissoo.mod.gui.items.Component;
import dev.kissoo.mod.gui.items.Item;
import dev.kissoo.mod.gui.items.SearchBarItem;
import dev.kissoo.mod.gui.items.buttons.BindButton;
import dev.kissoo.mod.gui.items.buttons.ModuleButton;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.client.ClickGui;
import dev.kissoo.mod.modules.impl.client.HudEditor;
import dev.kissoo.mod.modules.impl.hud.IrcChatHUD;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGuiScreen extends Screen {
   private static ClickGuiScreen INSTANCE = new ClickGuiScreen();
   private final ArrayList<Component> components = new ArrayList<>();

   private final ArrayList<HudElement> hudElements = new ArrayList<>();
   private final Map<HudElement, SmoothAnimationUtil> hudAnimX = new HashMap<>();
   private final Map<HudElement, SmoothAnimationUtil> hudAnimY = new HashMap<>();

   private HudElement draggingHud = null;
   private int dragOffsetX = 0;
   private int dragOffsetY = 0;
   private boolean showHudGuides = true;
   private static final int HUD_SNAP = 2;

   public ClickGuiScreen() {
      super(Text.literal("Alien"));
      this.setInstance();
      this.load();
   }

   public static ClickGuiScreen getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ClickGuiScreen();
      }
      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   private void load() {
      this.components.clear();
      this.hudElements.clear();
      this.hudAnimX.clear();
      this.hudAnimY.clear();

      int x = -84;
      for (final Module.Category category : Module.Category.values()) {
         String categoryName = category.toString();
         x += 94;
         this.components.add(new Component(categoryName, category, x, 4, true) {
            @Override
            public void setupItems() {
               if (category == Module.Category.Client) {
                  this.addItem(new SearchBarItem());
               }
               for (Module module : Forever.MODULE.getModules()) {
                  if (module.isHidden()) continue;
                  if (module.getCategory().equals(category)) {
                     this.addButton(new ModuleButton(module));
                  }
                  if (module instanceof HudElement hudElement
                          && category == Module.Category.HUD) {
                     if (!hudElements.contains(hudElement)) {
                        hudElements.add(hudElement);
                        hudAnimX.put(hudElement,
                                new SmoothAnimationUtil(hudElement.getElementX(), 0.28, 0.2));
                        hudAnimY.put(hudElement,
                                new SmoothAnimationUtil(hudElement.getElementY(), 0.28, 0.2));
                     }
                  }
               }
            }
         });
      }

      this.components.forEach(component -> {
         component.getItems().sort((item1, item2) -> {
            if (item1 instanceof SearchBarItem) return -1;
            if (item2 instanceof SearchBarItem) return 1;
            return item1.getName().compareToIgnoreCase(item2.getName());
         });
      });
   }

   private boolean isHudEditorMode() {
      return HudEditor.INSTANCE != null && HudEditor.INSTANCE.isOn();
   }

   private SmoothAnimationUtil getAnimX(HudElement e) {
      return hudAnimX.computeIfAbsent(e,
              k -> new SmoothAnimationUtil(e.getElementX(), 0.28, 0.2));
   }

   private SmoothAnimationUtil getAnimY(HudElement e) {
      return hudAnimY.computeIfAbsent(e,
              k -> new SmoothAnimationUtil(e.getElementY(), 0.28, 0.2));
   }

   private float getRenderHudX(HudElement e) {
      SmoothAnimationUtil anim = getAnimX(e);
      anim.setTarget(e.getElementX());
      return (float) anim.update();
   }

   private float getRenderHudY(HudElement e) {
      SmoothAnimationUtil anim = getAnimY(e);
      anim.setTarget(e.getElementY());
      return (float) anim.update();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      float a      = (float) ClickGui.getInstance().alphaValue;
      float scale  = 0.92F + 0.08F * a;
      float slideY = (1.0F - a) * 20.0F;

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
      Item.context = context;
      this.renderBackground(context, mouseX, mouseY, delta);

      if (isHudEditorMode()) {
         renderHudOverlay(context, mouseX, mouseY, delta);
      }

      context.getMatrices().push();
      context.getMatrices().translate(0.0F, slideY, 0.0F);
      context.getMatrices().scale(scale, scale, 1.0F);

      ClickGui gui = ClickGui.getInstance();
      boolean isFullyOpen = gui.alphaValue > 0.95;

      if (gui.toggleShader.getValue()
              && Forever.SHADER != null
              && !Forever.SHADER.fullNullCheck()
              && isFullyOpen) {

         List<float[]> rects = new ArrayList<>();
         for (Component comp : this.components) {
            rects.addAll(comp.getClippedShaderRects());
         }

         if (!rects.isEmpty()) {
            ShaderManager.Shader sMode =
                    (ShaderManager.Shader) gui.shaderMode.getValue();
            ShaderParams guiParams = gui.buildGuiShaderParams();
            Forever.SHADER.applyShaderRects(
                    rects,
                    sMode,
                    context.getMatrices(),
                    guiParams
            );
         }
      }

      this.components.forEach(component ->
              component.drawScreen(context, mouseX, mouseY, delta));

      context.getMatrices().pop();

      if (gui.image.getValue()) {
         renderCornerImage(context, a);
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderCornerImage(DrawContext context, float alpha) {
      ClickGui gui = ClickGui.getInstance();
      ClickGui.ImageMode mode = (ClickGui.ImageMode) gui.imageMode.getValue();
      net.minecraft.util.Identifier id =
              net.minecraft.util.Identifier.of(mode.getPath());

      int size = gui.imageScale.getValueInt();
      int offX = gui.imageOffsetX.getValueInt();
      int offY = gui.imageOffsetY.getValueInt();

      int x = Math.max(0, this.width - size - offX);
      int y = Math.max(0, this.height - size - offY);

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      context.drawTexture(id, x, y, 0, 0, size, size, size, size);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
   }

   private void renderHudOverlay(DrawContext context, int mouseX, int mouseY, float delta) {
      if (showHudGuides && draggingHud != null) {
         context.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x15FFFFFF);
         context.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x15FFFFFF);
      }
      for (HudElement elem : this.hudElements) {
         renderHudElement(context, elem, mouseX, mouseY, delta);
      }
      if (showHudGuides && draggingHud != null) {
         renderHudAlignGuides(context);
      }
      context.fill(0, this.height - 13, this.width, this.height, 0x90000000);
      String alignInfo = draggingHud != null
              ? (draggingHud.getAlignMode() == HudElement.AlignMode.RIGHT ? " [右对齐]" : " [左对齐]")
              : "";
      String info = draggingHud != null
              ? String.format("§e拖拽HUD §7(%d, %d)%s",
              draggingHud.getElementX(), draggingHud.getElementY(), alignInfo)
              : "§7HudEditor | 左键拖拽 | 右键切换 | G辅助线 | R居中";
      context.drawCenteredTextWithShadow(
              textRenderer, info, this.width / 2, this.height - 10, 0xFFFFFF);
   }

   private void renderHudElement(
           DrawContext context, HudElement elem,
           int mouseX, int mouseY, float delta
   ) {
      int actualX = elem.getElementX();
      int actualY = elem.getElementY();
      int w = elem.getElementWidth();
      int h = elem.getElementHeight();
      if (w <= 0 || h <= 0) return;

      float renderX = getRenderHudX(elem);
      float renderY = getRenderHudY(elem);

      boolean vis = elem.isElementVisible();
      if (vis) {
         try {
            context.getMatrices().push();
            context.getMatrices().translate(
                    renderX - actualX, renderY - actualY, 0.0F);
            elem.renderElement(context, delta);
            context.getMatrices().pop();
         } catch (Exception ignored) {}
      }

      if (draggingHud == elem) {
         int rx = Math.round(renderX);
         int ry = Math.round(renderY);
         drawDashedRect(context, rx, ry, w, h, 0xFFFFFFFF);

         String label = elem.getElementName();
         int lw = textRenderer.getWidth(label);
         int lx = rx + w + 4;
         int ly = ry - 2;
         if (lx + lw + 6 > this.width) lx = rx - lw - 6;
         if (ly < 2) ly = ry + h + 2;

         context.fill(lx - 2, ly - 1, lx + lw + 2, ly + 9, 0xB0000000);
         context.drawTextWithShadow(textRenderer, label, lx, ly, 0xFFFFFFFF);
      }
   }

   private void drawDashedRect(DrawContext context, int x, int y, int w, int h, int color) {
      int dash = 4, gap = 2;
      for (int i = 0; i < w; i += dash + gap) {
         int end = Math.min(i + dash, w);
         context.fill(x + i, y,         x + end, y + 1,     color);
         context.fill(x + i, y + h - 1, x + end, y + h,     color);
      }
      for (int i = 0; i < h; i += dash + gap) {
         int end = Math.min(i + dash, h);
         context.fill(x,         y + i, x + 1,     y + end, color);
         context.fill(x + w - 1, y + i, x + w,     y + end, color);
      }
   }

   private void renderHudAlignGuides(DrawContext context) {
      int dx = draggingHud.getElementX(), dy = draggingHud.getElementY();
      int dw = draggingHud.getElementWidth(), dh = draggingHud.getElementHeight();

      if (Math.abs(dx + dw / 2 - width / 2) <= HUD_SNAP)
         context.fill(width / 2, 0, width / 2 + 1, height, 0x8000FFFF);
      if (Math.abs(dy + dh / 2 - height / 2) <= HUD_SNAP)
         context.fill(0, height / 2, width, height / 2 + 1, 0x8000FFFF);

      for (HudElement o : hudElements) {
         if (o == draggingHud || o.getElementWidth() <= 0) continue;
         int ox = o.getElementX(), oy = o.getElementY();
         int ow = o.getElementWidth(), oh = o.getElementHeight();
         int minY = Math.min(dy, oy), maxY = Math.max(dy + dh, oy + oh);
         int minX = Math.min(dx, ox), maxX = Math.max(dx + dw, ox + ow);

         if (Math.abs(dx - ox) <= HUD_SNAP)
            context.fill(ox, minY, ox + 1, maxY, 0x8000FFFF);
         if (Math.abs(dx + dw - ox - ow) <= HUD_SNAP)
            context.fill(ox + ow - 1, minY, ox + ow, maxY, 0x80FF8800);
         if (Math.abs(dy - oy) <= HUD_SNAP)
            context.fill(minX, oy, maxX, oy + 1, 0x8000FFFF);
         if (Math.abs(dy + dh - oy - oh) <= HUD_SNAP)
            context.fill(minX, oy + oh - 1, maxX, oy + oh, 0x8000FFFF);
         if (Math.abs(dx - ox - ow) <= HUD_SNAP)
            context.fill(ox + ow, minY, ox + ow + 1, maxY, 0x8000FF00);
         if (Math.abs(dx + dw - ox) <= HUD_SNAP)
            context.fill(ox - 1, minY, ox, maxY, 0x8000FF00);
         if (Math.abs(dy - oy - oh) <= HUD_SNAP)
            context.fill(minX, oy + oh, maxX, oy + oh + 1, 0x8000FF00);
         if (Math.abs(dy + dh - oy) <= HUD_SNAP)
            context.fill(minX, oy - 1, maxX, oy, 0x8000FF00);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int clickedButton) {
      int mx = (int) mouseX, my = (int) mouseY;
      if (isHudEditorMode()) {
         HudElement clicked = getHoveredHud(mx, my);
         if (clickedButton == 0 && clicked != null && clicked.isDraggable()) {
            draggingHud  = clicked;
            dragOffsetX  = mx - clicked.getElementX();
            dragOffsetY  = my - clicked.getElementY();
            return true;
         }
         if (clickedButton == 1 && clicked instanceof Module module) {
            module.toggle();
            return true;
         }
      }
      this.components.forEach(component -> component.mouseClicked(mx, my, clickedButton));
      return super.mouseClicked(mouseX, mouseY, clickedButton);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int releaseButton) {
      if (isHudEditorMode() && releaseButton == 0 && draggingHud != null) {
         clampHud(draggingHud);
         updateAlignMode(draggingHud);
         draggingHud = null;
         Forever.CONFIG.save();
         return true;
      }
      this.components.forEach(component ->
              component.mouseReleased((int) mouseX, (int) mouseY, releaseButton));
      return super.mouseReleased(mouseX, mouseY, releaseButton);
   }

   private void updateAlignMode(HudElement hud) {
      int hx = hud.getElementX();
      int hw = hud.getElementWidth();
      int rightEdge = hx + hw;
      boolean hasRightAlignPartner = false;

      for (HudElement o : hudElements) {
         if (o == hud || o.getElementWidth() <= 0) continue;
         if (Math.abs(rightEdge - (o.getElementX() + o.getElementWidth())) <= 1) {
            hasRightAlignPartner = true;
            break;
         }
      }

      if (hasRightAlignPartner || (hx + hw / 2 > width / 2)) {
         if (hud.getAlignMode() != HudElement.AlignMode.RIGHT)
            hud.setAlignMode(HudElement.AlignMode.RIGHT);
      } else {
         if (hud.getAlignMode() != HudElement.AlignMode.LEFT)
            hud.setAlignMode(HudElement.AlignMode.LEFT);
      }
   }

   @Override
   public boolean mouseDragged(
           double mouseX, double mouseY, int button,
           double deltaX, double deltaY
   ) {
      if (isHudEditorMode() && button == 0 && draggingHud != null) {
         int nx = (int) mouseX - dragOffsetX;
         int ny = (int) mouseY - dragOffsetY;
         int dw = draggingHud.getElementWidth();
         int dh = draggingHud.getElementHeight();
         int dragDh = draggingHud.getDragBoundsHeight();

         if (Math.abs(nx) < HUD_SNAP) nx = 0;
         if (Math.abs(ny) < HUD_SNAP) ny = 0;
         if (Math.abs(nx + dw - width)  < HUD_SNAP) nx = width  - dw;
         if (Math.abs(ny + dragDh - height) < HUD_SNAP) ny = height - dragDh;

         int cx = (width - dw) / 2, cy = (height - dh) / 2;
         if (Math.abs(nx - cx) < HUD_SNAP) nx = cx;
         if (Math.abs(ny - cy) < HUD_SNAP) ny = cy;

         boolean snappedX = false, snappedY = false;

         for (HudElement o : hudElements) {
            if (o == draggingHud || o.getElementWidth() <= 0) continue;
            int ox = o.getElementX(), oy = o.getElementY();
            int ow = o.getElementWidth(), oh = o.getElementHeight();
            boolean vertAdj = Math.abs(ny - oy - oh) < HUD_SNAP
                    || Math.abs(ny + dh - oy) < HUD_SNAP;
            if (vertAdj && !snappedX) {
               int rsr = HUD_SNAP * 3;
               if (Math.abs((nx + dw) - (ox + ow)) < rsr) { nx = ox + ow - dw; snappedX = true; }
               if (!snappedX && Math.abs(nx - ox) < rsr)  { nx = ox;           snappedX = true; }
            }
            if (!snappedY) {
               if (Math.abs(ny - oy - oh) < HUD_SNAP) { ny = oy + oh; snappedY = true; }
               if (!snappedY && Math.abs(ny + dh - oy) < HUD_SNAP) { ny = oy - dh; snappedY = true; }
            }
         }

         for (HudElement o : hudElements) {
            if (o == draggingHud || o.getElementWidth() <= 0) continue;
            int ox = o.getElementX(), oy = o.getElementY();
            int ow = o.getElementWidth(), oh = o.getElementHeight();
            if (!snappedX) {
               if (Math.abs(nx - ox) < HUD_SNAP)             { nx = ox;           snappedX = true; }
               if (!snappedX && Math.abs(nx+dw-ox-ow)<HUD_SNAP){ nx = ox+ow-dw;  snappedX = true; }
               if (!snappedX && Math.abs(nx-ox-ow)<HUD_SNAP)  { nx = ox+ow;       snappedX = true; }
               if (!snappedX && Math.abs(nx+dw-ox)<HUD_SNAP)  { nx = ox-dw;       snappedX = true; }
            }
            if (!snappedY) {
               if (Math.abs(ny-oy)<HUD_SNAP)                  { ny = oy;           snappedY = true; }
               if (!snappedY && Math.abs(ny+dh-oy-oh)<HUD_SNAP){ ny = oy+oh-dh;   snappedY = true; }
               if (!snappedY && Math.abs(ny-oy-oh)<HUD_SNAP)  { ny = oy+oh;        snappedY = true; }
               if (!snappedY && Math.abs(ny+dh-oy)<HUD_SNAP)  { ny = oy-dh;        snappedY = true; }
            }
         }

         if (draggingHud.getAlignMode() == HudElement.AlignMode.RIGHT) {
            forceSetLeftAlignX(draggingHud, nx);
         } else {
            draggingHud.setElementX(nx);
         }
         draggingHud.setElementY(ny);
         return true;
      }
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   private void forceSetLeftAlignX(HudElement hud, int leftX) {
      try {
         if (hud.getAlignMode() == HudElement.AlignMode.RIGHT) {
            hud.setRightAnchorX(leftX + hud.getElementWidth());
         } else {
            hud.setElementX(leftX);
         }
      } catch (Exception e) {
         hud.setElementX(leftX);
      }
   }

   @Override
   public boolean mouseScrolled(
           double mouseX, double mouseY,
           double horizontalAmount, double verticalAmount
   ) {
      if (isHudEditorMode()) {
         HudElement hoveredHud = getHoveredHud((int) mouseX, (int) mouseY);
         if (hoveredHud instanceof IrcChatHUD ircHud) {
            ircHud.scrollBy(verticalAmount);
            return true;
         }
      }

      boolean hoveringAnyPanel = false;
      for (Component component : this.components) {
         if (component.isOpen() && component.isMouseOver(mouseX, mouseY)) {
            component.onMouseScroll(mouseX, mouseY, verticalAmount);
            hoveringAnyPanel = true;
         }
      }
      if (!hoveringAnyPanel) {
         if (InputUtil.isKeyPressed(Wrapper.mc.getWindow().getHandle(), 340)) {
            if (verticalAmount < 0.0)
               this.components.forEach(c -> c.setX(c.getX() - 15));
            else if (verticalAmount > 0.0)
               this.components.forEach(c -> c.setX(c.getX() + 15));
         } else {
            if (verticalAmount < 0.0)
               this.components.forEach(c -> c.setY(c.getY() - 15));
            else if (verticalAmount > 0.0)
               this.components.forEach(c -> c.setY(c.getY() + 15));
         }
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      BindButton listeningButton = findListeningBind();
      if (listeningButton != null) {
         if (keyCode == 256) {
            listeningButton.setting.setValue(-1);
            listeningButton.isListening = false;
         } else {
            listeningButton.onKeyPressed(keyCode);
         }
         return true;
      }

      for (Component c : this.components) c.onKeyPressed(keyCode);

      if (isHudEditorMode() && draggingHud != null && draggingHud.isDraggable()) {
         int step = hasShiftDown() ? 10 : 1;
         switch (keyCode) {
            case 263 -> { moveHudLeft(draggingHud,  step); clampHud(draggingHud); return true; }
            case 262 -> { moveHudLeft(draggingHud, -step); clampHud(draggingHud); return true; }
            case 265 -> { draggingHud.setElementY(draggingHud.getElementY() - step); clampHud(draggingHud); return true; }
            case 264 -> { draggingHud.setElementY(draggingHud.getElementY() + step); clampHud(draggingHud); return true; }
            case 71  -> { showHudGuides = !showHudGuides; return true; }
            case 82  -> {
               forceSetLeftAlignX(draggingHud, (width - draggingHud.getElementWidth()) / 2);
               draggingHud.setElementY((height - draggingHud.getElementHeight()) / 2);
               clampHud(draggingHud);
               return true;
            }
         }
      }

      if (keyCode == 259) return true;
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private void moveHudLeft(HudElement hud, int delta) {
      try {
         if (hud.getAlignMode() == HudElement.AlignMode.RIGHT) {
            hud.setRightAnchorX(hud.getRightAnchorX() - delta);
         } else {
            hud.setElementX(hud.getElementX() - delta);
         }
      } catch (Exception e) {
         hud.setElementX(hud.getElementX() - delta);
      }
   }

   private BindButton findListeningBind() {
      for (Component c : this.components) {
         for (Item i : c.getItems()) {
            if (i instanceof BindButton b && b.isListening) return b;
            if (i instanceof ModuleButton mb) {
               for (Item sub : mb.getItems()) {
                  if (sub instanceof BindButton b && b.isListening) return b;
               }
            }
         }
      }
      return null;
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      this.components.forEach(c -> c.onKeyTyped(chr, modifiers));
      return super.charTyped(chr, modifiers);
   }

   private HudElement getHoveredHud(int mx, int my) {
      HudElement r = null;
      int min = Integer.MAX_VALUE;
      for (HudElement e : hudElements) {
         if (isOverHud(e, mx, my)) {
            int a = e.getElementWidth() * e.getElementHeight();
            if (a > 0 && a < min) { min = a; r = e; }
         }
      }
      return r;
   }

   private boolean isOverHud(HudElement e, int mx, int my) {
      int m = 4;
      int x = e.getElementX(), y = e.getElementY();
      int w = e.getElementWidth(), h = e.getElementHeight();
      return w > 0 && h > 0
              && mx >= x - m && mx <= x + w + m
              && my >= y - m && my <= y + h + m;
   }

   private void clampHud(HudElement e) {
      int clampedX = Math.max(0, Math.min(e.getElementX(), width  - e.getElementWidth()));
      int clampedY = Math.max(0, Math.min(e.getElementY(), height - e.getDragBoundsHeight()));
      forceSetLeftAlignX(e, clampedX);
      e.setElementY(clampedY);
   }

   @Override
   public boolean shouldPause() { return false; }

   public final ArrayList<Component> getComponents() { return this.components; }

   public int getTextOffset() {
      return -ClickGui.getInstance().textOffset.getValueInt() - 6;
   }
}
