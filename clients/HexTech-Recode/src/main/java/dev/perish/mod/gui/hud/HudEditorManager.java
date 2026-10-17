package dev.perish.mod.gui.hud;

import dev.perish.core.Manager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class HudEditorManager extends Manager {
    public static HudEditorManager INSTANCE;

    private final List<HudComponent> components = new CopyOnWriteArrayList<>();
    private HudComponent selectedComponent;
    private boolean editMode;

    public HudEditorManager() {
        INSTANCE = this;
    }

    public void register(HudComponent component) {
        components.add(component);
    }

    public void unregister(HudComponent component) {
        components.remove(component);
    }

    public List<HudComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public boolean isEditMode() { return editMode; }
    public void setEditMode(boolean mode) { this.editMode = mode; if (!mode) selectedComponent = null; }
    public void toggleEditMode() { setEditMode(!editMode); if (!editMode) savePositions(); }

    public HudComponent getSelected() { return selectedComponent; }
    public void setSelected(HudComponent c) { if (editMode) selectedComponent = c; }
    public boolean isSelected(HudComponent c) { return editMode && selectedComponent == c; }

    public HudComponent getComponentAt(double mx, double my) {
        for (int i = components.size() - 1; i >= 0; i--) {
            HudComponent c = components.get(i);
            if (c.isVisible() && c.isHovering(mx, my)) return c;
        }
        return null;
    }

    public void savePositions() {
        File file = getFile("hud_positions.txt");
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (HudComponent c : components) {
                w.println(c.getId() + ":" + c.getX() + ":" + c.getY());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadPositions() {
        File file = getFile("hud_positions.txt");
        if (!file.exists()) return;
        Map<String, float[]> pos = new HashMap<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                try {
                    String[] p = line.split(":");
                    if (p.length >= 3) pos.put(p[0], new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2])});
                } catch (Exception ignored) {}
            }
        } catch (Exception e) { e.printStackTrace(); }
        for (HudComponent c : components) {
            float[] p = pos.get(c.getId());
            if (p != null) { c.setX(p[0]); c.setY(p[1]); }
        }
    }
}
