package dev.g255.arc.core;

/**
 * 「一串物品 / 方块」的通用持有者。
 *
 * <p>客户端里原本 {@code XrayManager}（矿透）、{@code TradeManager}、{@code CleanerManager}
 * 各自实现了一份一模一样的 {@code add / remove / inList}，再由
 * {@code ItemSelectWindow}（物品选择窗口）用 instanceof 逐个分发。新增列表时只要实现本接口，
 * 物品选择窗口就能直接编辑它，不用再往那串 instanceof 里加分支。</p>
 */
public interface ItemListHolder {
   /** 列表里是否已有这一项。key 允许带 {@code block.minecraft.} / {@code item.minecraft.} 前缀。 */
   boolean inList(String key);

   void add(String key);

   void remove(String key);
}
