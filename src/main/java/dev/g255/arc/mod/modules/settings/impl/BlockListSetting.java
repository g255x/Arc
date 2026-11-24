package dev.g255.arc.mod.modules.settings.impl;

import dev.g255.arc.mod.modules.settings.Setting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * 方块多选。对应 Hachimi 的 BlockListSetting（Whitelist / Blacklist 那类）。
 *
 * <p>存档写法：{@code minecraft:obsidian,minecraft:ancient_debris}。
 * 方块 id 本身带冒号不影响配置读写，因为 ConfigManager 的 key/value 切分带了 limit(2)。
 */
public class BlockListSetting extends Setting {
   private final List<Block> defaultValue;
   private final List<Block> value;

   public BlockListSetting(String name, Block... blocks) {
      this(name, null, blocks);
   }

   public BlockListSetting(String name, BooleanSupplier visibility, Block... blocks) {
      super(name, visibility);
      this.defaultValue = new ArrayList(Arrays.asList(blocks));
      this.value = new ArrayList(Arrays.asList(blocks));
   }

   public List<Block> getValue() {
      return this.value;
   }

   public List<Block> getDefaultValue() {
      return this.defaultValue;
   }

   public boolean contains(Block block) {
      return this.value.contains(block);
   }

   /** 点了就切换选中状态（Hachimi 的 afT / afU） */
   public void toggle(Block block) {
      if (!this.value.remove(block)) {
         this.value.add(block);
      }
   }

   public void add(Block block) {
      if (!this.value.contains(block)) {
         this.value.add(block);
      }
   }

   public void remove(Block block) {
      this.value.remove(block);
   }

   public void clear() {
      this.value.clear();
   }

   public void setValue(List<Block> blocks) {
      this.value.clear();
      this.value.addAll(blocks);
   }

   public void resetValue() {
      this.value.clear();
      this.value.addAll(this.defaultValue);
   }

   /** 逗号分隔的 registry id */
   public String serialize() {
      StringBuilder sb = new StringBuilder();

      for(Block block : this.value) {
         if (sb.length() > 0) {
            sb.append(',');
         }

         sb.append(Registries.BLOCK.getId(block));
      }

      return sb.toString();
   }

   /** 空串/读不到 → 回默认值；单个 id 认不出来就跳过，不整条丢弃 */
   public void load(String text) {
      this.value.clear();
      if (text == null || text.isBlank()) {
         this.value.addAll(this.defaultValue);
         return;
      }

      for(String part : text.split(",")) {
         String id = part.trim();
         if (id.isEmpty()) {
            continue;
         }

         Identifier identifier = Identifier.tryParse(id);
         if (identifier == null) {
            continue;
         }

         Block block = Registries.BLOCK.get(identifier);
         if (block != Blocks.AIR && !this.value.contains(block)) {
            this.value.add(block);
         }
      }
   }
}
