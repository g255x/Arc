package dev.g255.arc.core.impl;

import dev.g255.arc.core.ItemListHolder;
import dev.g255.arc.core.Manager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

/**
 * 自困 / 围脚放置用的方块表（{@code arc/placeblocks.txt}）。
 *
 * <p>和矿透（{@link XrayManager}）走同一套机制：列表就是一行一个方块名，
 * 由 {@code ItemSelectWindow}（ClickGui 里的「物品」窗口）点选维护，
 * 模块面板里只留一个 {@code Edit} 开关去打开那个窗口，不再在模块里塞一份内嵌列表控件。</p>
 *
 * <p>顺序即优先级：列表里排在前面的方块优先被拿在手上。</p>
 */
public class PlaceBlockManager extends Manager implements ItemListHolder {
   private final ArrayList<String> list = new ArrayList<>();

   public PlaceBlockManager() {
      this.read();
   }

   public ArrayList<String> getList() {
      return this.list;
   }

   @Override
   public boolean inList(String name) {
      if (name == null) {
         return false;
      }

      String key = strip(name);
      return this.list.contains(key);
   }

   public void clear() {
      this.list.clear();
   }

   @Override
   public void remove(String name) {
      if (name != null && this.list.remove(strip(name))) {
         this.save();
      }
   }

   @Override
   public void add(String name) {
      if (name == null) {
         return;
      }

      String key = strip(name);
      if (!key.isEmpty() && !this.list.contains(key)) {
         this.list.add(key);
         this.save();
      }
   }

   /** 把列表按顺序解析成 {@link Block}，交给 {@code InventoryUtil.findBlockInList} 按优先级找。 */
   public List<Block> blocks() {
      ArrayList<Block> blocks = new ArrayList<>();

      for(String name : this.list) {
         Block block = resolve(name);
         if (block != null && block != Blocks.AIR && !blocks.contains(block)) {
            blocks.add(block);
         }
      }

      return blocks;
   }

   private static Block resolve(String name) {
      try {
         String id = name.indexOf(58) >= 0 ? name : "minecraft:" + name;
         return Registries.BLOCK.get(Identifier.tryParse(id));
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static String strip(String name) {
      return name.replace("block.minecraft.", "").replace("item.minecraft.", "");
   }

   public void read() {
      try {
         File file = getFile("placeblocks.txt");
         if (!file.exists()) {
            this.list.add(strip(Blocks.OBSIDIAN.getTranslationKey()));
            this.list.add(strip(Blocks.ENDER_CHEST.getTranslationKey()));
            this.save();
            return;
         }

         for(String s : IOUtils.readLines(new FileInputStream(file), StandardCharsets.UTF_8)) {
            String key = strip(s.trim());
            if (!key.isEmpty() && !this.list.contains(key)) {
               this.list.add(key);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public void save() {
      try {
         File file = getFile("placeblocks.txt");
         PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

         for(String s : this.list) {
            writer.println(s);
         }

         writer.close();
      } catch (Exception var4) {
         System.out.println("[Arc] by G255 - Failed to save placeblocks");
      }
   }
}
