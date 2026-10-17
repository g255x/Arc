package dev.kissoo.mod.modules.impl.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.core.Manager;
import dev.kissoo.core.impl.BlurManager;
import dev.kissoo.mod.modules.HudElement;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.impl.misc.Punctuation;
import dev.kissoo.mod.modules.settings.impl.BindSetting;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.ColorSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MiniMap extends Module implements HudElement {
    public static MiniMap INSTANCE;

    private int hudX = 10, hudY = 10;
    private AlignMode alignMode = AlignMode.LEFT;
    public final SliderSetting size = this.add(new SliderSetting("Size", 80, 50, 200, 1));
    public final SliderSetting bgAlpha = this.add(new SliderSetting("BgAlpha", 100, 0, 255, 1));
    public final ColorSetting outlineColor = this.add(new ColorSetting("OutlineColor", new Color(255, 255, 255)).allowClientColor(true));
    public final BooleanSetting blurBg = this.add(new BooleanSetting("BlurBg", true));
    public final SliderSetting blurRadius = this.add(new SliderSetting("BlurRadius", 5.0, 1.0, 20.0, 0.5));
    public final BooleanSetting crosshair = this.add(new BooleanSetting("Crosshair", true));
    public final SliderSetting crossLength = this.add(new SliderSetting("CrossLength", 0.17, 0.05, 1.0, 0.01, () -> crosshair.getValue()));
    public final SliderSetting range = this.add(new SliderSetting("Range", 50, 10, 200, 1));
    public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    public final SliderSetting dotSize = this.add(new SliderSetting("DotSize", 2.0, 0.5, 6.0, 0.5));
    public final BooleanSetting showWaypoints = this.add(new BooleanSetting("ShowWaypoints", true));
    public final BindSetting uiBind = this.add(new BindSetting("WaypointUI", -1));
    private boolean uiPressed = false;
    public static final List<Waypoint> WAYPOINTS = new CopyOnWriteArrayList<>();

    public static class Waypoint {
        public String name;
        public double x, y, z;
        public Color color;
        public Waypoint(String name, double x, double y, double z, Color color) {
            this.name = name; this.x = x; this.y = y; this.z = z; this.color = color;
        }
    }

    public MiniMap() {
        super("Map", Category.HUD);
        this.setChinese("小地图");
        INSTANCE = this;
        loadWaypoints();
    }

    public static void saveWaypoints() {
        try {
            File file = Manager.getFile("minimap_waypoints.txt");
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            for (Waypoint wp : WAYPOINTS) {
                pw.println(wp.name + ":" + wp.x + ":" + wp.y + ":" + wp.z + ":" + wp.color.getRGB());
            }
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadWaypoints() {
        WAYPOINTS.clear();
        try {
            File file = Manager.getFile("minimap_waypoints.txt");
            if (!file.exists()) return;

            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 5) {
                    String name = parts[0];
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    Color color = new Color(Integer.parseInt(parts[4]), true);
                    WAYPOINTS.add(new Waypoint(name, x, y, z, color));
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (uiBind.isPressed()) {
            if (!uiPressed && mc.currentScreen == null) {
                mc.setScreen(new WaypointScreen());
            }
            uiPressed = true;
        } else {
            uiPressed = false;
        }
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        renderElement(drawContext, tickDelta);
    }

    @Override
    public String getElementName() {
        return getChinese() != null ? getChinese() : getName();
    }

    @Override
    public int getElementX() {
        return alignMode == AlignMode.RIGHT ? hudX - getElementWidth() : hudX;
    }

    @Override
    public void setElementX(int x) {
        hudX = alignMode == AlignMode.RIGHT ? x + getElementWidth() : x;
    }

    @Override
    public int getElementY() {
        return hudY;
    }

    @Override
    public void setElementY(int y) {
        hudY = y;
    }

    @Override
    public int getElementWidth() {
        return size.getValueInt();
    }

    @Override
    public int getElementHeight() {
        return size.getValueInt();
    }

    @Override
    public boolean isElementVisible() {
        return isOn();
    }

    @Override
    public AlignMode getAlignMode() {
        return alignMode;
    }

    @Override
    public void setAlignMode(AlignMode mode) {
        if (mode == alignMode) return;
        if (mode == AlignMode.RIGHT && alignMode == AlignMode.LEFT) {
            hudX = hudX + getElementWidth();
        } else if (mode == AlignMode.LEFT && alignMode == AlignMode.RIGHT) {
            hudX = hudX - getElementWidth();
        }
        alignMode = mode;
    }

    @Override
    public int getRightAnchorX() {
        return alignMode == AlignMode.RIGHT ? hudX : hudX + getElementWidth();
    }

    @Override
    public void setRightAnchorX(int rightX) {
        hudX = alignMode == AlignMode.RIGHT ? rightX : rightX - getElementWidth();
    }

    @Override
    public void renderElement(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null) return;
        MatrixStack matrices = context.getMatrices();

        int x = getElementX();
        int y = getElementY();
        int mapSize = size.getValueInt();
        double maxRadius = mapSize / 2.0 - 2.0;

        if (blurBg.getValue()) {
            context.draw();
            new BlurManager().applyBlur(blurRadius.getValueFloat(), x, y, mapSize, mapSize);
        }

        Render2DUtil.drawRect(matrices, x, y, mapSize, mapSize, new Color(0, 0, 0, bgAlpha.getValueInt()));
        drawOutline(matrices, x, y, mapSize, mapSize, 0.5f, outlineColor.getValue());

        double centerX = x + mapSize / 2.0;
        double centerY = y + mapSize / 2.0;
        float currentDotSize = dotSize.getValueFloat();
        if (crosshair.getValue()) {
            drawFadedCrosshair(matrices, (float) centerX, (float) centerY, (float) mapSize / 2.0f);
        }
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            Color dotColor = isFriend(player) ? Color.GREEN : Color.RED;
            drawMapDot(matrices, player.getX(), player.getZ(), centerX, centerY, x, y, mapSize, maxRadius, dotColor, currentDotSize);
        }
        if (showWaypoints.getValue()) {
            for (Waypoint wp : WAYPOINTS) {
                drawMapDot(matrices, wp.x, wp.z, centerX, centerY, x, y, mapSize, maxRadius, wp.color, currentDotSize);
            }
            if (Punctuation.INSTANCE != null && Punctuation.INSTANCE.isOn()) {
                for (Punctuation.Spot spot : Punctuation.INSTANCE.waypoint.values()) {
                    drawMapDot(matrices, spot.pos().getX(), spot.pos().getZ(), centerX, centerY, x, y, mapSize, maxRadius, spot.color(), currentDotSize);
                }
            }
        }
    }

    private void drawFadedCrosshair(MatrixStack matrices, float cx, float cy, float radius) {
        float maxDist = radius * crossLength.getValueFloat();
        float thickness = 0.5f;
        float halfT = thickness / 2.0f;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int r = 255, g = 255, b = 255;
        int aCenter = 100, aEdge = 0;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, cx, cy - halfT, 0).color(r, g, b, aCenter);
        bufferBuilder.vertex(matrix, cx, cy + halfT, 0).color(r, g, b, aCenter);
        bufferBuilder.vertex(matrix, cx + maxDist, cy + halfT, 0).color(r, g, b, aEdge);
        bufferBuilder.vertex(matrix, cx + maxDist, cy - halfT, 0).color(r, g, b, aEdge);
        bufferBuilder.vertex(matrix, cx - maxDist, cy - halfT, 0).color(r, g, b, aEdge);
        bufferBuilder.vertex(matrix, cx - maxDist, cy + halfT, 0).color(r, g, b, aEdge);
        bufferBuilder.vertex(matrix, cx, cy + halfT, 0).color(r, g, b, aCenter);
        bufferBuilder.vertex(matrix, cx, cy - halfT, 0).color(r, g, b, aCenter);
        bufferBuilder.vertex(matrix, cx - halfT, cy, 0).color(r, g, b, aCenter);
        bufferBuilder.vertex(matrix, cx - halfT, cy + maxDist, 0).color(r, g, b, aEdge);
        bufferBuilder.vertex(matrix, cx + halfT, cy + maxDist, 0).color(r, g, b, aEdge);
        bufferBuilder.vertex(matrix, cx + halfT, cy, 0).color(r, g, b, aCenter);
        bufferBuilder.vertex(matrix, cx - halfT, cy - maxDist, 0).color(r, g, b, aEdge);
        bufferBuilder.vertex(matrix, cx - halfT, cy, 0).color(r, g, b, aCenter);
        bufferBuilder.vertex(matrix, cx + halfT, cy, 0).color(r, g, b, aCenter);
        bufferBuilder.vertex(matrix, cx + halfT, cy - maxDist, 0).color(r, g, b, aEdge);

        BuiltBuffer builtBuffer = bufferBuilder.end();
        if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }

        RenderSystem.disableBlend();
    }

    private void drawMapDot(MatrixStack matrices, double targetX, double targetZ,
                            double centerX, double centerY, int mapX, int mapY, int mapSize,
                            double maxRadius, Color color, float dSize) {
        double diffX = targetX - mc.player.getX();
        double diffZ = targetZ - mc.player.getZ();
        double screenOffsetX, screenOffsetZ;

        if (rotate.getValue()) {
            double yawRad = Math.toRadians(mc.player.getYaw());
            double sin = Math.sin(yawRad);
            double cos = Math.cos(yawRad);
            screenOffsetX = -diffX * cos - diffZ * sin;
            screenOffsetZ = diffX * sin - diffZ * cos;
        } else {
            screenOffsetX = diffX;
            screenOffsetZ = diffZ;
        }

        double mapRatio = maxRadius / range.getValue();
        double drawX = centerX + (screenOffsetX * mapRatio);
        double drawY = centerY + (screenOffsetZ * mapRatio);

        float radius = dSize / 2.0f;
        drawX = Math.max(mapX + radius + 1, Math.min(mapX + mapSize - radius - 1, drawX));
        drawY = Math.max(mapY + radius + 1, Math.min(mapY + mapSize - radius - 1, drawY));
        drawCircle(matrices, (float) drawX, (float) drawY, radius, color);
    }
    private void drawCircle(MatrixStack matrices, float x, float y, float radius, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x, y, 0).color(r, g, b, a);
        for (int i = 0; i <= 360; i += 10) {
            double angle = Math.toRadians(i);
            float vx = x + (float) (Math.sin(angle) * radius);
            float vy = y + (float) (Math.cos(angle) * radius);
            bufferBuilder.vertex(matrix, vx, vy, 0).color(r, g, b, a);
        }

        BuiltBuffer builtBuffer = bufferBuilder.end();
        if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }

        RenderSystem.disableBlend();
    }

    private boolean isFriend(PlayerEntity player) {
        try { return Forever.FRIEND.isFriend(player); } catch (Exception e) { return false; }
    }
    private void drawOutline(MatrixStack matrices, float x, float y, float w, float h, float t, Color c) {
        Render2DUtil.drawRect(matrices, x, y, w, t, c);
        Render2DUtil.drawRect(matrices, x, y + h - t, w, t, c);
        Render2DUtil.drawRect(matrices, x, y, t, h, c);
        Render2DUtil.drawRect(matrices, x + w - t, y, t, h, c);
    }
    public static class WaypointScreen extends Screen {
        public WaypointScreen() {
            super(Text.literal("Waypoint Manager"));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context, mouseX, mouseY, delta);

            String title = "=== MiniMap WayPointManager ===";
            context.drawText(this.textRenderer, title, this.width / 2 - this.textRenderer.getWidth(title) / 2, 20, -1, true);

            int yOffset = 50;
            if (WAYPOINTS.isEmpty()) {
                String emptyTip = "No WayPoint.Pls Input .minimap waypoint create <name> To Save。";
                context.drawText(this.textRenderer, emptyTip, this.width / 2 - this.textRenderer.getWidth(emptyTip) / 2, yOffset, Color.GRAY.getRGB(), true);
            } else {
                for (Waypoint wp : WAYPOINTS) {
                    String text = String.format("[%s] X:%.0f Y:%.0f Z:%.0f", wp.name, wp.x, wp.y, wp.z);
                    context.drawText(this.textRenderer, text, this.width / 2 - this.textRenderer.getWidth(text) / 2, yOffset, wp.color.getRGB(), true);
                    yOffset += 15;
                }
            }

            String escTip = "ESC To Close Screen";
            context.drawText(this.textRenderer, escTip, this.width / 2 - this.textRenderer.getWidth(escTip) / 2, this.height - 20, Color.LIGHT_GRAY.getRGB(), true);

            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}