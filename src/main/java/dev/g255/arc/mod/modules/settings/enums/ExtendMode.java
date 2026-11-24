package dev.g255.arc.mod.modules.settings.enums;

/**
 * 围点外侧要不要再摊一圈。
 *
 * <p>{@link #Off} 不扩展；{@link #Mining} 只有该点正在被人挖时才扩展；
 * {@link #Always} 无条件扩展；{@link #Body} 是 Alien 那一套 ——
 * 只有身体真的贴到围点上（骑在格缝、卡斜角）时才从那一格向外摊两层。</p>
 */
public enum ExtendMode {
   Off,
   Mining,
   Always,
   Body
}
