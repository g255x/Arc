package dev.g255.arc.mod.modules.impl.client;

import dev.g255.arc.api.utils.player.InventoryUtil;
import dev.g255.arc.mod.gui.HudElement;
import dev.g255.arc.mod.gui.clickgui.ClickGuiScreen;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.PistonBlock;
import net.minecraft.client.gui.DrawContext;

public class ItemsCounter extends Module implements HudElement {
   public static ItemsCounter INSTANCE;
   private static final Item[] PREVIEW_ITEMS;
   private final BooleanSetting hideEmpty = this.add(new BooleanSetting("HideEmpty", true));
   private final BooleanSetting crystal = this.add(new BooleanSetting("Crystal", true));
   private final BooleanSetting xp = this.add(new BooleanSetting("XP", true));
   private final BooleanSetting pearl = this.add(new BooleanSetting("Pearl", true));
   private final BooleanSetting obsidian = this.add(new BooleanSetting("Obsidian", true));
   private final BooleanSetting egApple = this.add(new BooleanSetting("E-GApple", true));
   private final BooleanSetting gApple = this.add(new BooleanSetting("GApple", true));
   private final BooleanSetting totem = this.add(new BooleanSetting("Totem", true));
   private final BooleanSetting web = this.add(new BooleanSetting("Web", true));
   private final BooleanSetting anchor = this.add(new BooleanSetting("Anchor", true));
   private final BooleanSetting glowstone = this.add(new BooleanSetting("Glowstone", true));
   private final BooleanSetting piston = this.add(new BooleanSetting("Piston", true));
   private final BooleanSetting redstone = this.add(new BooleanSetting("RedStone", true));
   private final BooleanSetting enderChest = this.add(new BooleanSetting("EnderChest", true));
   private final BooleanSetting firework = this.add(new BooleanSetting("Firework", true));
   private final SliderSetting xOffset = this.add(new SliderSetting("X", 100, 0, 1500));
   private final SliderSetting yOffset = this.add(new SliderSetting("Y", 100, 0, 1000));
   private final SliderSetting offset = this.add(new SliderSetting("Offset", 18, 0, 30));
   private final ItemStack crystalStack;
   private final ItemStack xpStack;
   private final ItemStack pearlStack;
   private final ItemStack obsidianStack;
   private final ItemStack eGappleStack;
   private final ItemStack gappleStack;
   private final ItemStack totemStack;
   private final ItemStack webStack;
   private final ItemStack anchorStack;
   private final ItemStack glowstoneStack;
   private final ItemStack pistonStack;
   private final ItemStack redstoneStack;
   private final ItemStack enderChestStack;
   private final ItemStack fireworkStack;
   int x;
   int y;
   DrawContext drawContext;
   private int cachedWidth;
   private int cachedHeight;

   public ItemsCounter() {
      super("Items", Module.Category.HUD);
      this.crystalStack = new ItemStack(Items.END_CRYSTAL);
      this.xpStack = new ItemStack(Items.EXPERIENCE_BOTTLE);
      this.pearlStack = new ItemStack(Items.ENDER_PEARL);
      this.obsidianStack = new ItemStack(Items.OBSIDIAN);
      this.eGappleStack = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
      this.gappleStack = new ItemStack(Items.GOLDEN_APPLE);
      this.totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
      this.webStack = new ItemStack(Items.COBWEB);
      this.anchorStack = new ItemStack(Items.RESPAWN_ANCHOR);
      this.glowstoneStack = new ItemStack(Items.GLOWSTONE);
      this.pistonStack = new ItemStack(Items.PISTON);
      this.redstoneStack = new ItemStack(Items.REDSTONE_BLOCK);
      this.enderChestStack = new ItemStack(Items.ENDER_CHEST);
      this.fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
      this.cachedWidth = 100;
      this.cachedHeight = 16;
      this.setChinese("物品数量");
      INSTANCE = this;
   }

   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (!nullCheck() && !ClickGuiScreen.isEditingHud()) {
         this.renderAt(drawContext);
      }
   }

   private void renderAt(DrawContext drawContext) {
      this.drawContext = drawContext;
      this.x = this.xOffset.getValueInt() - this.offset.getValueInt();
      this.y = this.yOffset.getValueInt();
      int drawn = 0;
      if (this.crystal.getValue()) {
         this.crystalStack.setCount(this.getItemCount(Items.END_CRYSTAL));
         this.drawItem(this.crystalStack);
         ++drawn;
      }

      if (this.xp.getValue()) {
         this.xpStack.setCount(this.getItemCount(Items.EXPERIENCE_BOTTLE));
         this.drawItem(this.xpStack);
         ++drawn;
      }

      if (this.pearl.getValue()) {
         this.pearlStack.setCount(this.getItemCount(Items.ENDER_PEARL));
         this.drawItem(this.pearlStack);
         ++drawn;
      }

      if (this.obsidian.getValue()) {
         this.obsidianStack.setCount(this.getItemCount(Items.OBSIDIAN));
         this.drawItem(this.obsidianStack);
         ++drawn;
      }

      if (this.egApple.getValue()) {
         this.eGappleStack.setCount(this.getItemCount(Items.ENCHANTED_GOLDEN_APPLE));
         this.drawItem(this.eGappleStack);
         ++drawn;
      }

      if (this.gApple.getValue()) {
         this.gappleStack.setCount(this.getItemCount(Items.GOLDEN_APPLE));
         this.drawItem(this.gappleStack);
         ++drawn;
      }

      if (this.totem.getValue()) {
         this.totemStack.setCount(this.getItemCount(Items.TOTEM_OF_UNDYING));
         this.drawItem(this.totemStack);
         ++drawn;
      }

      if (this.web.getValue()) {
         this.webStack.setCount(this.getItemCount(Items.COBWEB));
         this.drawItem(this.webStack);
         ++drawn;
      }

      if (this.anchor.getValue()) {
         this.anchorStack.setCount(this.getItemCount(Items.RESPAWN_ANCHOR));
         this.drawItem(this.anchorStack);
         ++drawn;
      }

      if (this.glowstone.getValue()) {
         this.glowstoneStack.setCount(this.getItemCount(Items.GLOWSTONE));
         this.drawItem(this.glowstoneStack);
         ++drawn;
      }

      int pistonCount;
      if (this.piston.getValue() && ((pistonCount = InventoryUtil.getItemCount(PistonBlock.class)) > 0 || !this.hideEmpty.getValue())) {
         this.x += this.offset.getValueInt();
         this.pistonStack.setCount(Math.max(1, pistonCount));
         this.drawItem(this.pistonStack);
         ++drawn;
      }

      if (this.redstone.getValue()) {
         this.redstoneStack.setCount(this.getItemCount(Items.REDSTONE_BLOCK));
         this.drawItem(this.redstoneStack);
         ++drawn;
      }

      if (this.enderChest.getValue()) {
         this.enderChestStack.setCount(this.getItemCount(Items.ENDER_CHEST));
         this.drawItem(this.enderChestStack);
         ++drawn;
      }

      if (this.firework.getValue()) {
         this.fireworkStack.setCount(this.getItemCount(Items.FIREWORK_ROCKET));
         this.drawItem(this.fireworkStack);
         ++drawn;
      }

      this.cachedWidth = Math.max(1, (drawn > 0 ? drawn - 1 : 0) * this.offset.getValueInt() + 16);
      this.cachedHeight = 16;
   }

   private int getItemCount(Item item) {
      int i = InventoryUtil.getItemCount(item);
      if (this.hideEmpty.getValue() && i == 0) {
         return 0;
      } else {
         this.x += this.offset.getValueInt();
         return Math.max(i, 1);
      }
   }

   private void drawItem(ItemStack itemStack) {
      this.drawContext.drawItem(itemStack, this.x, this.y);
      this.drawContext.drawItemInSlot(mc.textRenderer, itemStack, this.x, this.y);
   }

   public int getX() {
      return this.xOffset.getValueInt() - this.offset.getValueInt();
   }

   public void setX(int nx) {
      this.xOffset.setValue((double)nx + (double)this.offset.getValueInt());
   }

   public int getY() {
      return this.yOffset.getValueInt();
   }

   public void setY(int ny) {
      this.yOffset.setValue((double)ny);
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
      if (nullCheck()) {
         this.renderPreview(context);
      } else {
         this.renderAt(context);
      }
   }

   private void renderPreview(DrawContext drawContext) {
      int startX = this.xOffset.getValueInt() - this.offset.getValueInt();
      int step = this.offset.getValueInt();

      for(int i = 0; i < PREVIEW_ITEMS.length; ++i) {
         ItemStack stack = new ItemStack(PREVIEW_ITEMS[i], i + 1);
         drawContext.drawItem(stack, startX + i * step, this.yOffset.getValueInt());
         drawContext.drawItemInSlot(mc.textRenderer, stack, startX + i * step, this.yOffset.getValueInt());
      }

      this.cachedWidth = Math.max(1, (PREVIEW_ITEMS.length - 1) * step + 16);
      this.cachedHeight = 16;
   }

   static {
      PREVIEW_ITEMS = new Item[]{Items.END_CRYSTAL, Items.EXPERIENCE_BOTTLE, Items.ENDER_PEARL, Items.OBSIDIAN, Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE, Items.TOTEM_OF_UNDYING, Items.COBWEB, Items.RESPAWN_ANCHOR, Items.GLOWSTONE, Items.PISTON, Items.REDSTONE_BLOCK, Items.ENDER_CHEST, Items.FIREWORK_ROCKET};
   }
}
