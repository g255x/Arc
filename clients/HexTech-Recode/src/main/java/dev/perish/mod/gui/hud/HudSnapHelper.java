package dev.perish.mod.gui.hud;

import dev.perish.api.utils.Wrapper;
import java.awt.*;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public class HudSnapHelper implements Wrapper {
    public static final HudSnapHelper INSTANCE = new HudSnapHelper();

    private boolean gridSnap = true;
    private boolean edgeSnap = true;
    private boolean componentSnap = true;
    private int gridSize = 10;
    private float snapThreshold = 8f;
    private float smoothFactor = 0.35f;

    private SnapResult lastSnap;
    private boolean showGuides = true;

    public static class SnapResult {
        public float snappedX, snappedY;
        public boolean snappedXAxis, snappedYAxis;
        public GuideLine[] guideLines;

        public SnapResult() { guideLines = new GuideLine[0]; }
    }

    public static class GuideLine {
        public float x1, y1, x2, y2;
        public int color;
        public boolean horizontal;

        public GuideLine(float x1, float y1, float x2, float y2, int color, boolean h) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
            this.color = color; this.horizontal = h;
        }
    }

    public SnapResult calculateSnap(float rawX, float rawY, float w, float h,
                                      List<HudComponent> allComponents, HudComponent dragging) {
        SnapResult result = new SnapResult();
        result.snappedX = rawX;
        result.snappedY = rawY;

        java.util.List<GuideLine> guides = new java.util.ArrayList<>();
        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();

        if (edgeSnap) {
            float[] edgeX = {0, sw / 2 - w / 2, sw - w};
            float[] edgeY = {0, sh / 2 - h / 2, sh - h};
            for (float ex : edgeX) {
                if (Math.abs(rawX - ex) < snapThreshold) {
                    result.snappedX = ex;
                    result.snappedXAxis = true;
                    guides.add(new GuideLine(ex, 0, ex, sh, new Color(80, 180, 255, 180).getRGB(), true));
                }
            }
            for (float ey : edgeY) {
                if (Math.abs(rawY - ey) < snapThreshold) {
                    result.snappedY = ey;
                    result.snappedYAxis = true;
                    guides.add(new GuideLine(0, ey, sw, ey, new Color(80, 180, 255, 180).getRGB(), false));
                }
            }

            float rightEdge = rawX + w;
            if (Math.abs(rightEdge - sw) < snapThreshold) {
                result.snappedX = sw - w;
                result.snappedXAxis = true;
                guides.add(new GuideLine(sw, 0, sw, sh, new Color(80, 255, 150, 180).getRGB(), true));
            }
            float bottomEdge = rawY + h;
            if (Math.abs(bottomEdge - sh) < snapThreshold) {
                result.snappedY = sh - h;
                result.snappedYAxis = true;
                guides.add(new GuideLine(0, sh, sw, sh, new Color(80, 255, 150, 180).getRGB(), false));
            }
        }

        if (componentSnap && dragging != null) {
            for (HudComponent c : allComponents) {
                if (c == dragging || !c.isVisible()) continue;

                float cx = c.getX(), cy = c.getY();
                float cw = c.getWidth(), ch = c.getHeight();
                int alignColor = new Color(255, 200, 50, 160).getRGB();

                if (Math.abs(rawX - cx) < snapThreshold) {
                    result.snappedX = cx; result.snappedXAxis = true;
                    guides.add(new GuideLine(cx, Math.min(cy, rawY), cx, Math.max(cy + ch, rawY + h), alignColor, true));
                }
                if (Math.abs(rawX - (cx + cw)) < snapThreshold) {
                    result.snappedX = cx + cw; result.snappedXAxis = true;
                    guides.add(new GuideLine(cx + cw, Math.min(cy, rawY), cx + cw, Math.max(cy + ch, rawY + h), alignColor, true));
                }
                if (Math.abs((rawX + w) - cx) < snapThreshold) {
                    result.snappedX = cx - w; result.snappedXAxis = true;
                    guides.add(new GuideLine(cx, Math.min(cy, rawY), cx, Math.max(cy + ch, rawY + h), alignColor, true));
                }
                if (Math.abs((rawX + w) - (cx + cw)) < snapThreshold) {
                    result.snappedX = cx + cw - w; result.snappedXAxis = true;
                    guides.add(new GuideLine(cx + cw, Math.min(cy, rawY), cx + cw, Math.max(cy + ch, rawY + h), alignColor, true));
                }
                if (Math.abs(rawX - (cx + cw / 2 - w / 2)) < snapThreshold) {
                    result.snappedX = cx + cw / 2 - w / 2; result.snappedXAxis = true;
                    guides.add(new GuideLine(cx + cw / 2, Math.min(cy, rawY), cx + cw / 2, Math.max(cy + ch, rawY + h),
                            new Color(200, 100, 255, 140).getRGB(), true));
                }

                if (Math.abs(rawY - cy) < snapThreshold) {
                    result.snappedY = cy; result.snappedYAxis = true;
                    guides.add(new GuideLine(Math.min(cx, rawX), cy, Math.max(cx + cw, rawX + w), cy, alignColor, false));
                }
                if (Math.abs(rawY - (cy + ch)) < snapThreshold) {
                    result.snappedY = cy + ch; result.snappedYAxis = true;
                    guides.add(new GuideLine(Math.min(cx, rawX), cy + ch, Math.max(cx + cw, rawX + w), cy + ch, alignColor, false));
                }
                if (Math.abs((rawY + h) - cy) < snapThreshold) {
                    result.snappedY = cy - h; result.snappedYAxis = true;
                    guides.add(new GuideLine(Math.min(cx, rawX), cy, Math.max(cx + cw, rawX + w), cy, alignColor, false));
                }
                if (Math.abs((rawY + h) - (cy + ch)) < snapThreshold) {
                    result.snappedY = cy + ch - h; result.snappedYAxis = true;
                    guides.add(new GuideLine(Math.min(cx, rawX), cy + ch, Math.max(cx + cw, rawX + w), cy + ch, alignColor, false));
                }
                if (Math.abs(rawY - (cy + ch / 2 - h / 2)) < snapThreshold) {
                    result.snappedY = cy + ch / 2 - h / 2; result.snappedYAxis = true;
                    guides.add(new GuideLine(Math.min(cx, rawX), cy + ch / 2, Math.max(cx + cw, rawX + w), cy + ch / 2,
                            new Color(200, 100, 255, 140).getRGB(), false));
                }
            }
        }

        if (gridSnap && !result.snappedXAxis) {
            float gx = Math.round(rawX / gridSize) * gridSize;
            if (Math.abs(rawX - gx) < snapThreshold * 0.6f) {
                result.snappedX = gx; result.snappedXAxis = true;
            }
        }
        if (gridSnap && !result.snappedYAxis) {
            float gy = Math.round(rawY / gridSize) * gridSize;
            if (Math.abs(rawY - gy) < snapThreshold * 0.6f) {
                result.snappedY = gy; result.snappedYAxis = true;
            }
        }

        result.guideLines = guides.toArray(new GuideLine[0]);
        return result;
    }

    public void renderGuides(DrawContext context, SnapResult snap) {
        if (!showGuides || snap == null || snap.guideLines == null) return;
        for (GuideLine g : snap.guideLines) {
            dev.perish.api.utils.render.Render2DUtil.drawLine(
                    context.getMatrices(), g.x1, g.y1, g.x2, g.y2, g.color);
        }
    }

    public void applySmoothDrag(HudComponent comp, double mouseX, double mouseY) {
        float targetX = (float)(mouseX - comp.dragOffsetX);
        float targetY = (float)(mouseY - comp.dragOffsetY);

        if (HudEditorManager.INSTANCE.isEditMode()) {
            SnapResult snap = calculateSnap(targetX, targetY, comp.getWidth(), comp.getHeight(),
                    HudEditorManager.INSTANCE.getComponents(), comp);
            lastSnap = snap;
            targetX = snap.snappedX;
            targetY = snap.snappedY;
        }

        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();
        targetX = Math.max(0, Math.min(targetX, sw - comp.getWidth()));
        targetY = Math.max(0, Math.min(targetY, sh - comp.getHeight()));

        comp.setX(comp.getX() + (targetX - comp.getX()) * smoothFactor);
        comp.setY(comp.getY() + (targetY - comp.getY()) * smoothFactor);
    }

    public SnapResult getLastSnap() { return lastSnap; }

    public boolean isGridSnap() { return gridSnap; }
    public void setGridSnap(boolean v) { gridSnap = v; }
    public boolean isEdgeSnap() { return edgeSnap; }
    public void setEdgeSnap(boolean v) { edgeSnap = v; }
    public boolean isComponentSnap() { return componentSnap; }
    public void setComponentSnap(boolean v) { componentSnap = v; }
    public int getGridSize() { return gridSize; }
    public void setGridSize(int s) { gridSize = Math.max(5, Math.min(s, 50)); }
    public float getSnapThreshold() { return snapThreshold; }
    public void setSnapThreshold(float t) { snapThreshold = Math.max(3, Math.min(t, 20)); }
    public float getSmoothFactor() { return smoothFactor; }
    public void setSmoothFactor(float f) { smoothFactor = Math.max(0.1f, Math.min(f, 0.9f)); }
    public boolean showGuides() { return showGuides; }
    public void setShowGuides(boolean v) { showGuides = v; }
}
