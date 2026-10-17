package dev.perish.mod.gui.hud;

import dev.perish.api.utils.Wrapper;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

public class HudEditorScreen extends Screen implements Wrapper {
    private static HudEditorScreen INSTANCE;

    public HudEditorScreen() {
        super(Text.literal("HUD Editor"));
        INSTANCE = this;
    }

    public static HudEditorScreen getInstance() {
        return INSTANCE != null ? INSTANCE : new HudEditorScreen();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        assert mc != null;

        for (HudComponent component : HudEditorManager.INSTANCE.getComponents()) {
            if (!component.isVisible() || component.getWidth() <= 0 || component.getHeight() <= 0) continue;

            try { component.render(context, delta); } catch (Exception ignored) {}

            boolean selected = HudEditorManager.INSTANCE.isSelected(component);
            component.renderOverlay(context, selected);

            if (component.isDragging()) {
                component.onDrag(mouseX, mouseY);
            }
        }

        HudSnapHelper.INSTANCE.renderGuides(context, HudSnapHelper.INSTANCE.getLastSnap());

        renderStatusBar(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderStatusBar(DrawContext context) {
        String snapStatus;
        HudSnapHelper snap = HudSnapHelper.INSTANCE;
        HudComponent sel = HudEditorManager.INSTANCE.getSelected();

        if (sel != null && sel.isDragging()) {
            HudSnapHelper.SnapResult r = snap.getLastSnap();
            if (r != null && (r.snappedXAxis || r.snappedYAxis)) {
                snapStatus = "§aSNAPPED §7| Grid:" + snap.isGridSnap()
                        + " Edge:" + snap.isEdgeSnap() + " Align:" + snap.isComponentSnap()
                        + " §7[" + (int)sel.getX() + "," + (int)sel.getY() + "]";
            } else {
                snapStatus = "§6Dragging... §7[" + (int)sel.getX() + "," + (int)sel.getY() + "]";
            }
        } else {
            snapStatus = "Left: drag | Right: deselect | Scroll: resize | ESC: save & close";
        }

        float tw = textRenderer.getWidth(snapStatus.replace("§7", "").replace("§6", "").replace("§a", ""));
        float sw = mc.getWindow().getScaledWidth();

        context.getMatrices().push();
        context.getMatrices().translate(sw / 2 - tw / 2 - 4, 2, 500);
        dev.perish.api.utils.render.Render2DUtil.drawRoundedRect(
                context.getMatrices(), 0, 0, tw + 8, 11,
                3.0f, new java.awt.Color(15, 15, 22, 210)
        );
        context.drawText(textRenderer, snapStatus, 4, 1, 0xDDDDDDDD, false);
        context.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            HudComponent clicked = HudEditorManager.INSTANCE.getComponentAt(mx, my);
            if (clicked != null) {
                HudEditorManager.INSTANCE.setSelected(clicked);
                clicked.onMouseClicked(mx, my, button);
                return true;
            }
            HudEditorManager.INSTANCE.setSelected(null);
        } else if (button == 1) {
            HudEditorManager.INSTANCE.setSelected(null);
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) {
            for (HudComponent c : HudEditorManager.INSTANCE.getComponents()) c.onMouseReleased(button);
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) { return true; }

    @Override
    public boolean mouseScrolled(double mx, double my, double hx, double vy) {
        HudComponent sel = HudEditorManager.INSTANCE.getSelected();
        if (sel != null) sel.onScroll(vy);

        HudSnapHelper snap = HudSnapHelper.INSTANCE;
        if (vy > 0) snap.setGridSize(snap.getGridSize() + 5);
        else if (vy < 0) snap.setGridSize(snap.getGridSize() - 5);

        return super.mouseScrolled(mx, my, hx, vy);
    }

    @Override
    public void close() {
        HudEditorManager.INSTANCE.setEditMode(false);
        HudEditorManager.INSTANCE.savePositions();
        super.close();
    }

    @Override
    public boolean shouldPause() { return false; }
}
