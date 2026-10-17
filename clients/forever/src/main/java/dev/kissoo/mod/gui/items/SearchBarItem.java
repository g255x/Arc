package dev.kissoo.mod.gui.items;

import dev.kissoo.api.utils.math.Timer;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.mod.gui.items.buttons.Button;
import dev.kissoo.mod.modules.impl.client.ClickGui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class SearchBarItem extends Item {
    public static String searchText = "";

    private boolean isTyping = false;
    private final Timer cursorTimer = new Timer();
    private boolean showCursor = true;
    private long lastBackspaceTime = 0;
    public SearchBarItem() {
        super("Search");
        this.setHeight(15); // 搜索条高度
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        Render2DUtil.drawRect(context.getMatrices(), x, y, width, height, new Color(0, 0, 0, 80));

        if (isTyping) {
            Render2DUtil.drawRect(
                    context.getMatrices(),
                    x, y + height - 1.0f,
                    width, 1.0f,
                    ClickGui.getInstance().enableBGColor.getValue().getRGB()
            );
        } else {
            Render2DUtil.drawRect(
                    context.getMatrices(),
                    x, y + height - 1.0f,
                    width, 1.0f,
                    new Color(80, 80, 80, 100)
            );
        }

        float textY = y + (height - 6) / 2.0F - 1.0F;

        String displayText = isTyping
                ? searchText
                : (searchText.isEmpty() ? "Search..." : searchText);

        int textColor = isTyping
                ? Button.enableTextColor
                : (searchText.isEmpty()
                ? new Color(150, 150, 150, 200).getRGB()
                : Button.enableTextColor);

        String drawStr = displayText;
        int maxW = width - 10;
        if (getWidth(drawStr) > maxW) {
            while (getWidth(drawStr) > maxW && !drawStr.isEmpty()) {
                drawStr = drawStr.substring(1);
            }
        }

        this.drawString(drawStr, x + 4.0F, textY, textColor);

        if (isTyping) {
            if (cursorTimer.passed(500L)) {
                showCursor = !showCursor;
                cursorTimer.reset();
            }
            if (showCursor) {
                float cursorX = x + 4.0F + getWidth(drawStr);
                if (cursorX < x + width - 2) {
                    Render2DUtil.drawRect(
                            context.getMatrices(),
                            cursorX, textY,
                            1.0F, 8.0F,
                            Button.enableTextColor
                    );
                }
            }
        }
    }


    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean hovering = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        if (mouseButton == 0) {
            if (hovering) {
                isTyping = !isTyping;
                cursorTimer.reset();
                showCursor = true;
                Item.sound();
            } else {
                isTyping = false;
            }
        } else if (mouseButton == 1 && hovering) {
            searchText = "";
            Item.sound();
        }
    }

    @Override
    public void onKeyTyped(char typedChar, int keyCode) {
        if (isTyping) {
            if (typedChar >= 32 && typedChar <= 126) {
                searchText += typedChar;
                cursorTimer.reset();
            }
        }
    }


    @Override
    public void onKeyPressed(int key) {
        if (isTyping) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (System.currentTimeMillis() - lastBackspaceTime > 100) {
                    this.handleBackspace();
                    lastBackspaceTime = System.currentTimeMillis();
                }
            } else if (key == GLFW.GLFW_KEY_V && Screen.hasControlDown()) {
                String clipboard = mc.keyboard.getClipboard();
                if (clipboard != null) searchText += clipboard;
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
                isTyping = false;
            }
        }
    }

    private void handleBackspace() {
        if (!searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.length() - 1);
            cursorTimer.reset();
        }
    }
    @Override
    public void update() {
    }
}