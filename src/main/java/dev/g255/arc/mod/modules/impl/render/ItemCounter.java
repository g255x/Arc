
package dev.g255.arc.mod.modules.impl.render;
import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.Render2DEvent;
import dev.g255.arc.api.utils.i18n.LangUtil;
import dev.g255.arc.api.utils.render.ColorUtil;
import dev.g255.arc.api.utils.render.TextUtil;
import dev.g255.arc.core.impl.FontManager;
import dev.g255.arc.mod.gui.HudElement;
import dev.g255.arc.mod.gui.clickgui.ClickGuiScreen;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.combat.AutoTrap;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.ColorSetting;
import dev.g255.arc.mod.modules.settings.impl.EnumSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
public class ItemCounter extends Module implements HudElement {
   public static ItemCounter INSTANCE;
   public final BooleanSetting showTotem = this.add(new BooleanSetting("Totem", true));
   public final BooleanSetting showCrystal = this.add(new BooleanSetting("Crystal", true));
   public final SliderSetting scale = this.add(new SliderSetting("Scale", (double)1.0F, (double)0.5F, (double)3.0F, (double)0.1F));
   public final SliderSetting yOffset = this.add(new SliderSetting("YOffset", (double)10.0F, (double)-500.0F, (double)500.0F, (double)1.0F));
   public final SliderSetting spacing = this.add(new SliderSetting("Spacing", (double)5.0F, (double)0.0F, (double)20.0F, (double)0.5F));
   public final BooleanSetting showIcon = this.add(new BooleanSetting("ShowIcon", false));
   private final EnumSetting<ColorMode> colorMode;
   public final ColorSetting totemColor;
   public final ColorSetting totemColor2;
   public final ColorSetting crystalColor;
   public final ColorSetting crystalColor2;
   public final ColorSetting textColor;
   public final SliderSetting pulseSpeed;
   public final SliderSetting pulseCounter;
   public final SliderSetting xOffset = this.add(new SliderSetting("XOffset", (double)0.0F, (double)-800.0F, (double)800.0F, (double)1.0F));
   private int cachedWidth = 60;
   private int cachedHeight = 20;
   public ItemCounter() {
      super("ItemCounter", Module.Category.HUD);
      this.colorMode = this.add(new EnumSetting("ColorMode", ItemCounter.ColorMode.Pulse));
      this.totemColor = this.add(new ColorSetting("TotemColor", new Color(5591891)));
      this.totemColor2 = this.add(new ColorSetting("TotemColor2", new Color(16777215), () -> this.colorMode.getValue() == ItemCounter.ColorMode.Pulse));
      this.crystalColor = this.add(new ColorSetting("CrystalColor", new Color(5921370)));
      this.crystalColor2 = this.add(new ColorSetting("CrystalColor2", new Color(16777215), () -> this.colorMode.getValue() == ItemCounter.ColorMode.Pulse));
      this.textColor = this.add(new ColorSetting("TextColor", new Color(16777215)));
      this.pulseSpeed = this.add(new SliderSetting("PulseSpeed", (double)1.0F, (double)0.0F, (double)5.0F, 0.1, () -> this.colorMode.getValue() == ItemCounter.ColorMode.Pulse));
      this.pulseCounter = this.add(new SliderSetting("PulseCounter", 10, 1, 50, () -> this.colorMode.getValue() == ItemCounter.ColorMode.Pulse));
      this.setChinese("图腾水晶计数");
      INSTANCE = this;
   }
   @EventListener
   public void onRender2D(Render2DEvent event) {
      if (mc.player != null && !mc.options.hudHidden && mc.currentScreen == null && !ClickGuiScreen.isEditingHud()) {
         this.renderAt(event.drawContext);
      }
   }
   private void renderAt(DrawContext context) {
      int screenWidth = mc.getWindow().getScaledWidth();
      int screenHeight = mc.getWindow().getScaledHeight();
      float textScale = (float)this.scale.getValue();
      float spacing = (float)this.spacing.getValue();
      float startY = (float)screenHeight / 2.0F + (float)this.yOffset.getValue();
      float centerX = (float)screenWidth / 2.0F + (float)this.xOffset.getValue();
      int widest = 0;
      int lines = 0;
      if (this.showTotem.getValue()) {
         int totemCount = mc.player.getInventory().count(Items.TOTEM_OF_UNDYING);
         widest = Math.max(widest, (int)this.drawItemWithCount(context, new ItemStack(Items.TOTEM_OF_UNDYING), "Totem", totemCount, centerX, startY, textScale, this.getColor(this.totemColor.getValue(), this.totemColor2.getValue(), (double)0.0F)));
         if (totemCount > 0) {
            startY += this.getLineHeight(textScale) + spacing;
            ++lines;
         }
      }
      if (this.showCrystal.getValue()) {
         int crystalCount = mc.player.getInventory().count(Items.END_CRYSTAL);
         widest = Math.max(widest, (int)this.drawItemWithCount(context, new ItemStack(Items.END_CRYSTAL), "Crystal", crystalCount, centerX, startY, textScale, this.getColor(this.crystalColor.getValue(), this.crystalColor2.getValue(), (double)20.0F)));
         if (crystalCount > 0) {
            startY += this.getLineHeight(textScale) + spacing;
            ++lines;
         }
      }
      if (this.isHeadBlocked()) {
         String headBlocked = LangUtil.isChinese() ? "你已被盖头!" : "Head blocked!";
         this.drawCenteredLine(context, headBlocked, centerX, startY, textScale, this.textColor.getValue().getRGB());
         widest = Math.max(widest, (int)this.getTextWidth(headBlocked, textScale, this.useItemFont()));
         startY += this.getLineHeight(textScale) + spacing;
         ++lines;
      }
      if (this.isEnemyUsingTrap()) {
         String usingTrap = LangUtil.isChinese() ? "敌人正在使用Trap!" : "Enemy is using Trap!";
         this.drawCenteredLine(context, usingTrap, centerX, startY, textScale, this.textColor.getValue().getRGB());
         widest = Math.max(widest, (int)this.getTextWidth(usingTrap, textScale, this.useItemFont()));
         ++lines;
      }
      this.cachedWidth = Math.max(1, widest);
      this.cachedHeight = Math.max(1, lines > 0 ? Math.round((float)lines * this.getLineHeight(textScale) + (float)(lines - 1) * spacing) : Math.round(this.getLineHeight(textScale)));
   }
   private float drawItemWithCount(DrawContext context, ItemStack stack, String name, int count, float x, float y, float scale, int color) {
      if (count <= 0) {
         return 0.0F;
      } else {
         boolean customFont = this.useItemFont();
         float textHeight = this.getLineHeight(scale);
         float nameWidth = this.getTextWidth(name, scale, customFont);
         float countWidth = this.getTextWidth(String.valueOf(count), scale, customFont);
         float totalWidth = nameWidth + countWidth + 4.0F * scale;
         float startX = x - totalWidth / 2.0F;
         if (this.showIcon.getValue()) {
            float itemSize = 16.0F * scale;
            context.getMatrices().push();
            context.getMatrices().translate(startX - itemSize - 2.0F, y + (textHeight - itemSize) / 2.0F, 0.0F);
            context.getMatrices().scale(scale, scale, 1.0F);
            context.drawItem(stack, 0, 0);
            context.getMatrices().pop();
            totalWidth += itemSize + 2.0F * scale;
         }

         this.drawScaledText(context, name, startX, y, scale, color, customFont);
         this.drawScaledText(context, String.valueOf(count), startX + nameWidth + 4.0F, y, scale, this.textColor.getValue().getRGB(), customFont);
         return totalWidth;
      }
   }

   private void drawCenteredLine(DrawContext context, String text, float centerX, float y, float scale, int color) {
      boolean customFont = this.useItemFont();
      float width = this.getTextWidth(text, scale, customFont);
      this.drawScaledText(context, text, centerX - width / 2.0F, y, scale, color, customFont);
   }

   private void drawScaledText(DrawContext context, String text, float x, float y, float scale, int color, boolean customFont) {
      context.getMatrices().push();
      context.getMatrices().translate(x, y, 0.0F);
      context.getMatrices().scale(scale, scale, 1.0F);
      TextUtil.drawString(context, text, (double)0.0F, (double)0.0F, color, customFont, true);
      context.getMatrices().pop();
   }

   private boolean useItemFont() {
      return FontManager.ui != null;
   }

   private float getTextWidth(String text, float scale, boolean customFont) {
      return customFont ? FontManager.ui.getWidth(text) * scale : (float)mc.textRenderer.getWidth(text) * scale;
   }

   private float getLineHeight(float scale) {
      boolean customFont = this.useItemFont();
      float f;
       if (customFont) {
         f = FontManager.ui.getFontHeight();
      } else {
         Objects.requireNonNull(mc.textRenderer);
         f = 9.0F;
      }
      return f * scale;
   }
   private boolean isHeadBlocked() {
      BlockPos base = mc.player.getBlockPos();
      return this.isBlocked(base.up(1)) || this.isBlocked(base.up(2));
   }
   private boolean isBlocked(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      return !state.isAir() && state.getCollisionShape(mc.world, pos) != null && !state.getCollisionShape(mc.world, pos).isEmpty();
   }
   private boolean isEnemyUsingTrap() {
      return AutoTrap.INSTANCE != null && AutoTrap.INSTANCE.isOn() && AutoTrap.INSTANCE.target != null;
   }
   private int getColor(Color color1, Color color2, double delay) {
      return this.colorMode.getValue() == ItemCounter.ColorMode.Pulse ? ColorUtil.pulseColor(color1, color2, delay, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue()).getRGB() : color1.getRGB();
   }
   public int getX() {
      return mc.getWindow().getScaledWidth() / 2 + this.xOffset.getValueInt();
   }
   public void setX(int nx) {
      this.xOffset.setValue((double)(nx - mc.getWindow().getScaledWidth() / 2));
   }
   public int getY() {
      return mc.getWindow().getScaledHeight() / 2 + this.yOffset.getValueInt();
   }
   public void setY(int ny) {
      this.yOffset.setValue((double)(ny - mc.getWindow().getScaledHeight() / 2));
   }
   public int getWidth() {
      return this.cachedWidth;
   }
   public int getHeight() {
      return this.cachedHeight;
   }
   public boolean isVisible() {
      return this.isOn();
   }
   public void render(DrawContext context, float tickDelta) {
      if (mc.player != null) {
         this.renderAt(context);
      }
   }
   private static enum ColorMode {
      Custom,
      Pulse;
      // $FF: synthetic method
      private static ColorMode[] $values() {
         return new ColorMode[]{Custom, Pulse};
      }
   }
}
