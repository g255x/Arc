package dev.baimaodaxia.mod.features;

import dev.baimaodaxia.api.utils.Wrapper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.TridentItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.world.World;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundCategory;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.Registries;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.item.Item;

public final class SpearPort {
   public static final int MAX_USE_TIME = 72000;
   private static final int VIA_WOODEN_SPEAR_ID = 1536;
   private static final int VIA_STONE_SPEAR_ID = 1537;
   private static final int VIA_COPPER_SPEAR_ID = 1538;
   private static final int VIA_IRON_SPEAR_ID = 1539;
   private static final int VIA_GOLDEN_SPEAR_ID = 1540;
   private static final int VIA_DIAMOND_SPEAR_ID = 1541;
   private static final int VIA_NETHERITE_SPEAR_ID = 1542;
   public static final Item WOODEN_SPEAR;
   public static final Item STONE_SPEAR;
   public static final Item COPPER_SPEAR;
   public static final Item IRON_SPEAR;
   public static final Item GOLDEN_SPEAR;
   public static final Item DIAMOND_SPEAR;
   public static final Item NETHERITE_SPEAR;
   public static final SoundEvent SPEAR_USE;
   public static final SoundEvent SPEAR_HIT;
   public static final SoundEvent SPEAR_ATTACK;
   public static final SoundEvent WOODEN_SPEAR_USE;
   public static final SoundEvent WOODEN_SPEAR_HIT;
   public static final SoundEvent WOODEN_SPEAR_ATTACK;
   public static final SoundEvent SPEAR_LUNGE_1;
   public static final SoundEvent SPEAR_LUNGE_2;
   public static final SoundEvent SPEAR_LUNGE_3;
   private static final List<Item> SPEARS;
   private static boolean initialized;
   private static final ThreadLocal<Boolean> IN_HAND_RENDER;
   private static long lastFallbackSpearAttackTick;
   private static boolean stoppingAfterFallbackAttack;
   private static final int MIN_FALLBACK_CHARGE_TICKS = 8;
   private static final int FALLBACK_CONTACT_COOLDOWN_TICKS = 10;
   private static final double FALLBACK_SWEEP_RANGE = (double)5.25F;
   private static final double FALLBACK_DIRECT_HIT_RANGE = 3.1;
   private static final double LUNGE_IMPULSE_PER_LEVEL = 0.458;
   private static final int DEFAULT_KINETIC_DELAY_TICKS = 8;

   private SpearPort() {
   }

   public static void init() {
      if (!initialized) {
         initialized = true;
      }

   }

   public static boolean isSpear(Item item) {
      return SPEARS.contains(item);
   }

   public static boolean isSpear(ItemStack stack) {
      return getVisualSpear(stack) != null;
   }

   public static ItemStack getVisualStack(ItemStack stack) {
      Item var1 = getVisualSpear(stack);
      return var1 != null && !stack.isOf(var1) ? stack.copyComponentsToNewStack(var1, stack.getCount()) : stack;
   }

   public static void setInHandRender(boolean inHandRender) {
      IN_HAND_RENDER.set(inHandRender);
   }

   public static boolean isInHandRender() {
      return (Boolean)IN_HAND_RENDER.get();
   }

   public static Identifier getSpearModelId(ItemStack stack) {
      Item var1 = getVisualSpear(stack);
      if (var1 == null) {
         return null;
      } else {
         Identifier var2 = Registries.ITEM.getId(var1);
         if (var2 == null) {
            return null;
         } else {
            String var3 = var2.getPath();
            if (isInHandRender()) {
               var3 = var3 + "_in_hand";
            }

            return Identifier.of(var2.getNamespace(), var3);
         }
      }
   }

   public static Item getVisualSpear(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         Item var1 = getSpearByRegistryId(stack.getItem());
         if (var1 != null) {
            return var1;
         } else {
            String var2 = getMarkerText(stack);
            Item var3 = getSpearByMarker(var2);
            if (var3 != null) {
               return var3;
            } else if (!isViaSpearBaseItem(stack)) {
               return null;
            } else {
               Item var4 = getViaOriginalSpear(stack);
               if (var4 != null) {
                  return var4;
               } else {
                  Item var5 = getSpearByCustomModelData(stack);
                  return var5 != null ? var5 : null;
               }
            }
         }
      } else {
         return null;
      }
   }

   public static int getKineticDelayTicks(ItemStack stack) {
      return isSpear(stack) ? 8 : 0;
   }

   public static int getMaxKineticUseTicks(ItemStack stack) {
      Item var1 = getVisualSpear(stack);
      if (var1 == WOODEN_SPEAR) {
         return 300;
      } else if (var1 != STONE_SPEAR && var1 != GOLDEN_SPEAR) {
         if (var1 == COPPER_SPEAR) {
            return 240;
         } else if (var1 == IRON_SPEAR) {
            return 220;
         } else if (var1 == DIAMOND_SPEAR) {
            return 200;
         } else {
            return var1 == NETHERITE_SPEAR ? 160 : 0;
         }
      } else {
         return 260;
      }
   }

   public static int findSpearHotbarSlot() {
      if (Wrapper.mc.player == null) {
         return -1;
      } else {
         for(int var0 = 0; var0 < 9; ++var0) {
            ItemStack var1 = Wrapper.mc.player.getInventory().getStack(var0);
            if (isSpear(var1)) {
               return var0;
            }
         }

         return -1;
      }
   }

   public static void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
      if (!stoppingAfterFallbackAttack) {
         sendReleasePacket(user);
      }

   }

   public static void onUsageTick(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
      if (world != null && user != null && world.isClient && 72000 - remainingUseTicks >= 8 && user instanceof PlayerEntity var4) {
         if (var4 == MinecraftClient.getInstance().player) {
            tryFallbackSpearContact(var4, stack, 72000 - remainingUseTicks);
         }
      }

   }

   private static void sendReleasePacket(LivingEntity user) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1 != null && var1.player != null && var1.getNetworkHandler() != null && user == var1.player) {
         var1.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, user.getBlockPos(), user.getHorizontalFacing()));
      }

   }

   private static void tryFallbackSpearContact(PlayerEntity player, ItemStack stack, int usedTicks) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      if (var3.world != null && var3.interactionManager != null && var3.getNetworkHandler() != null) {
         long var4 = var3.world.getTime();
         double var6 = getFallbackSpearSpeed(player);
         if (var4 - lastFallbackSpearAttackTick >= 10L) {
            Entity var8 = findFallbackSpearTarget(var3, usedTicks, var6);
            if (var8 != null) {
               var3.interactionManager.attackEntity(player, var8);
               player.swingHand(Hand.MAIN_HAND);
               playHitSound(var3.world, var3.player, stack);
               applyLungeAfterHit(var3, player, stack);
               lastFallbackSpearAttackTick = var4;
               stopUsingAfterFallbackAttack(var3, player);
            }
         }
      }

   }

   private static Entity findFallbackSpearTarget(MinecraftClient client, int usedTicks, double speed) {
      if (client.world != null && client.player != null) {
         Vec3d var4 = getFallbackSpearDirection(client.player);
         double var5 = getFallbackSpearRange(usedTicks, speed);
         Vec3d var7 = client.player.getEyePos();
         Vec3d var8 = var7.add(var4.multiply(var5));
         Box var9 = client.player.getBoundingBox().stretch(var4.multiply(var5)).expand((double)1.25F, 0.8, (double)1.25F);
         Entity var10 = null;
         double var11 = Double.MAX_VALUE;

         for(Entity var14 : client.world.getOtherEntities(client.player, var9, SpearPort::canFallbackSpearHit)) {
            Box var15 = var14.getBoundingBox().expand(0.65);
            boolean var16 = var15.raycast(var7, var8).isPresent();
            boolean var17 = var14.squaredDistanceTo(client.player) <= 9.610000000000001;
            if (var16 || var17) {
               double var18 = var14.squaredDistanceTo(client.player);
               if (var18 < var11) {
                  var10 = var14;
                  var11 = var18;
               }
            }
         }

         return var10;
      } else {
         return null;
      }
   }

   private static double getFallbackSpearSpeed(PlayerEntity player) {
      Vec3d var1 = player.getVelocity();
      return (new Vec3d(var1.x, (double)0.0F, var1.z)).length() * (double)20.0F;
   }

   private static Vec3d getFallbackSpearDirection(PlayerEntity player) {
      Vec3d var1 = player.getVelocity();
      Vec3d var2 = new Vec3d(var1.x, (double)0.0F, var1.z);
      return var2.lengthSquared() > 0.0025 ? var2.normalize() : player.getRotationVec(1.0F).normalize();
   }

   private static double getFallbackSpearRange(int usedTicks, double speed) {
      double var3 = Math.min((double)5.25F, (double)2.75F + (double)Math.max(0, usedTicks - 8) * 0.08);
      return Math.max(3.1, Math.min(var3, (double)2.25F + speed * 0.45));
   }

   private static boolean canFallbackSpearHit(Entity entity) {
      return entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator() && entity.canHit();
   }

   private static void stopUsingAfterFallbackAttack(MinecraftClient client, PlayerEntity player) {
      stoppingAfterFallbackAttack = true;

      try {
         sendReleasePacket(player);
         player.stopUsingItem();
      } finally {
         stoppingAfterFallbackAttack = false;
      }

   }

   private static void applyLungeAfterHit(MinecraftClient client, PlayerEntity player, ItemStack stack) {
      int var3 = getLungeLevel(stack);
      if (var3 > 0 && !player.hasVehicle() && !player.isFallFlying() && !player.isTouchingWater()) {
         Vec3d var4 = player.getRotationVec(1.0F);
         Vec3d var5 = new Vec3d(var4.x, (double)0.0F, var4.z);
         if (!(var5.lengthSquared() < 1.0E-4)) {
            Vec3d var6 = var5.normalize().multiply(0.458 * (double)Math.min(var3, 3));
            player.addVelocity(var6.x, (double)0.0F, var6.z);
            player.velocityModified = true;
            playLungeSound(client.world, player);
         }
      }

   }

   private static int getLungeLevel(ItemStack stack) {
      int var1 = getLungeLevelFromEnchantments(stack.getEnchantments());
      if (var1 > 0) {
         return var1;
      } else {
         ItemEnchantmentsComponent var2 = (ItemEnchantmentsComponent)stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
         var1 = getLungeLevelFromEnchantments(var2);
         if (var1 > 0) {
            return var1;
         } else {
            String var3 = getMarkerText(stack);
            if (!var3.contains("lunge") && !var3.contains("突进")) {
               return var3.contains("1.21.11") && stack.hasEnchantments() ? 1 : 0;
            } else {
               return readLungeLevelFromMarker(var3);
            }
         }
      }
   }

   private static int getLungeLevelFromEnchantments(ItemEnchantmentsComponent enchantments) {
      if (enchantments != null && !enchantments.isEmpty()) {
         for(Object2IntMap.Entry<RegistryEntry<Enchantment>> var2 : enchantments.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> entry = (RegistryEntry)var2.getKey();
            if (entry.getKey().isPresent()) {
               String id = ((RegistryKey)entry.getKey().get()).getValue().toString().toLowerCase(Locale.ROOT);
               if (id.equals("minecraft:lunge") || id.endsWith(":lunge") || id.endsWith("/lunge")) {
                  return Math.max(1, var2.getIntValue());
               }
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   private static int readLungeLevelFromMarker(String marker) {
      if (!marker.contains("lunge:3") && !marker.contains("lunge 3") && !marker.contains("lvl:3") && !marker.contains("level:3")) {
         return !marker.contains("lunge:2") && !marker.contains("lunge 2") && !marker.contains("lvl:2") && !marker.contains("level:2") ? 1 : 2;
      } else {
         return 3;
      }
   }

   private static void playLungeSound(World world, PlayerEntity player) {
      if (world != null && player != null && world.isClient) {
         SoundEvent var10000;
         switch ((int)(Math.random() * (double)3.0F)) {
            case 0 -> var10000 = SPEAR_LUNGE_1;
            case 1 -> var10000 = SPEAR_LUNGE_2;
            default -> var10000 = SPEAR_LUNGE_3;
         }

         SoundEvent var2 = var10000;
         world.playSound(player, player.getX(), player.getY(), player.getZ(), var2, SoundCategory.PLAYERS, 1.0F, 1.0F);
      }

   }

   public static void playUseSound(World world, LivingEntity user, ItemStack stack) {
      playSound(world, user, getUseSound(stack), 0.85F, 1.0F);
   }

   public static void playAttackSound(World world, LivingEntity user, ItemStack stack) {
      playSound(world, user, getAttackSound(stack), 1.0F, 1.0F);
   }

   public static void playHitSound(World world, LivingEntity user, ItemStack stack) {
      playSound(world, user, getHitSound(stack), 1.0F, 1.0F);
   }

   private static void playSound(World world, LivingEntity user, SoundEvent sound, float volume, float pitch) {
      if (world != null && user != null && world.isClient) {
         PlayerEntity var10001;
         if (user instanceof PlayerEntity) {
            PlayerEntity var5 = (PlayerEntity)user;
            var10001 = var5;
         } else {
            var10001 = null;
         }

         world.playSound(var10001, user.getX(), user.getY(), user.getZ(), sound, SoundCategory.PLAYERS, volume, pitch);
      }

   }

   private static SoundEvent getUseSound(ItemStack stack) {
      return getVisualSpear(stack) == WOODEN_SPEAR ? WOODEN_SPEAR_USE : SPEAR_USE;
   }

   private static SoundEvent getAttackSound(ItemStack stack) {
      return getVisualSpear(stack) == WOODEN_SPEAR ? WOODEN_SPEAR_ATTACK : SPEAR_ATTACK;
   }

   private static SoundEvent getHitSound(ItemStack stack) {
      return getVisualSpear(stack) == WOODEN_SPEAR ? WOODEN_SPEAR_HIT : SPEAR_HIT;
   }

   public static Text getNameOverride(ItemStack stack) {
      Item var1 = getVisualSpear(stack);
      String var2 = getMarkerText(stack);
      if (var1 == null) {
         var1 = getSpearByMarker(var2);
      }

      if (var1 == null) {
         return null;
      } else {
         return !var2.contains("added_custom_name") && !var2.contains("1.21.11") && !var2.contains("netherie") && !var2.contains("spare") && !var2.contains("spaer") ? null : Text.translatable(var1.getTranslationKey());
      }
   }

   private static String getMarkerText(ItemStack stack) {
      StringBuilder var1 = new StringBuilder();
      appendTextComponent(var1, (Text)stack.get(DataComponentTypes.ITEM_NAME));
      appendTextComponent(var1, (Text)stack.get(DataComponentTypes.CUSTOM_NAME));
      NbtComponent var2 = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 != null && !var2.isEmpty()) {
         NbtCompound var3 = var2.copyNbt();
         var1.append(' ').append(var3);

         for(String var5 : var3.getKeys()) {
            var1.append(' ').append(var5);
            if (var3.contains(var5, 8)) {
               var1.append(' ').append(var3.getString(var5));
            }
         }
      }

      CustomModelDataComponent var7 = (CustomModelDataComponent)stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
      if (var7 != null) {
         var1.append(" custom_model_data=").append(var7.value());
      }

      LoreComponent var8 = (LoreComponent)stack.get(DataComponentTypes.LORE);
      if (var8 != null) {
         for(Text var6 : var8.lines()) {
            appendTextComponent(var1, var6);
         }
      }

      return var1.toString().toLowerCase(Locale.ROOT);
   }

   private static void appendTextComponent(StringBuilder marker, Text text) {
      if (text != null) {
         marker.append(' ').append(text.getString());
      }

   }

   private static Item getViaOriginalSpear(ItemStack stack) {
      NbtComponent var1 = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
      if (var1 != null && !var1.isEmpty()) {
         Integer var2 = findViaOriginalItemId(var1.copyNbt());
         return var2 == null ? null : getSpearByViaId(var2);
      } else {
         return null;
      }
   }

   private static Integer findViaOriginalItemId(NbtCompound nbt) {
      for(String var2 : nbt.getKeys()) {
         byte var3 = nbt.getType(var2);
         String var4 = var2.toLowerCase(Locale.ROOT);
         if (isViaIdKey(var4) && isNumericNbt(var3)) {
            int var10000;
            switch (var3) {
               case 1 -> var10000 = nbt.getByte(var2);
               case 2 -> var10000 = nbt.getShort(var2);
               case 3 -> var10000 = nbt.getInt(var2);
               case 4 -> var10000 = (int)nbt.getLong(var2);
               default -> var10000 = Integer.MIN_VALUE;
            }

            int var8 = var10000;
            if (getSpearByViaId(var8) != null) {
               return var8;
            }
         } else if (isViaIdKey(var4) && var3 == 8) {
            Item var7 = getSpearByIdentifier(nbt.getString(var2));
            if (var7 != null) {
               return getViaIdBySpear(var7);
            }
         } else if (var3 == 10) {
            Integer var5 = findViaOriginalItemId(nbt.getCompound(var2));
            if (var5 != null) {
               return var5;
            }
         } else if (var3 == 9) {
            Integer var6 = findViaOriginalItemId(nbt.getList(var2, 10));
            if (var6 != null) {
               return var6;
            }
         }
      }

      return null;
   }

   private static Integer findViaOriginalItemId(NbtList list) {
      for(int var1 = 0; var1 < list.size(); ++var1) {
         NbtElement var2 = list.get(var1);
         if (var2 != null && var2.getType() == 10) {
            Integer var3 = findViaOriginalItemId(list.getCompound(var1));
            if (var3 != null) {
               return var3;
            }
         }
      }

      return null;
   }

   private static boolean isViaSpearBaseItem(ItemStack stack) {
      return stack.isOf(Items.NETHERITE_SWORD) || stack.isOf(Items.DIAMOND_SWORD) || stack.isOf(Items.IRON_SWORD) || stack.isOf(Items.GOLDEN_SWORD) || stack.isOf(Items.STONE_SWORD) || stack.isOf(Items.WOODEN_SWORD) || stack.isOf(Items.TRIDENT) || stack.isOf(Items.STONE);
   }

   private static boolean isViaIdKey(String key) {
      return key.equals("id") || key.endsWith("|id") || key.endsWith(":id") || key.endsWith(".id") || key.contains("original") || key.contains("identifier");
   }

   private static boolean isNumericNbt(byte type) {
      return type == 1 || type == 2 || type == 3 || type == 4;
   }

   private static Item getSpearByCustomModelData(ItemStack stack) {
      CustomModelDataComponent var1 = (CustomModelDataComponent)stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
      return var1 == null ? null : getSpearByViaId(var1.value());
   }

   private static Item getSpearByMarker(String marker) {
      if (marker != null && (marker.contains("spear") || marker.contains("spare") || marker.contains("spaer"))) {
         if (!marker.contains("wooden_spear") && !marker.contains("wooden spear") && !marker.contains("wooden spare") && !marker.contains("wooden spaer")) {
            if (!marker.contains("stone_spear") && !marker.contains("stone spear") && !marker.contains("stone spare") && !marker.contains("stone spaer")) {
               if (!marker.contains("copper_spear") && !marker.contains("copper spear") && !marker.contains("copper spare") && !marker.contains("copper spaer")) {
                  if (!marker.contains("iron_spear") && !marker.contains("iron spear") && !marker.contains("iron spare") && !marker.contains("iron spaer")) {
                     if (!marker.contains("golden_spear") && !marker.contains("golden spear") && !marker.contains("golden spare") && !marker.contains("golden spaer")) {
                        if (!marker.contains("diamond_spear") && !marker.contains("diamond spear") && !marker.contains("diamond spare") && !marker.contains("diamond spaer")) {
                           if (!marker.contains("netherite_spear") && !marker.contains("netherite spear") && !marker.contains("netherite spare") && !marker.contains("netherite spaer") && !marker.contains("netherie spear") && !marker.contains("netherie spare") && !marker.contains("netherie spaer")) {
                              return marker.contains("1.21.11") ? NETHERITE_SPEAR : null;
                           } else {
                              return NETHERITE_SPEAR;
                           }
                        } else {
                           return DIAMOND_SPEAR;
                        }
                     } else {
                        return GOLDEN_SPEAR;
                     }
                  } else {
                     return IRON_SPEAR;
                  }
               } else {
                  return COPPER_SPEAR;
               }
            } else {
               return STONE_SPEAR;
            }
         } else {
            return WOODEN_SPEAR;
         }
      } else {
         return null;
      }
   }

   private static Item getSpearByIdentifier(String value) {
      if (value == null) {
         return null;
      } else {
         Item var10000;
         switch (value.toLowerCase(Locale.ROOT).replace("minecraft:", "")) {
            case "wooden_spear" -> var10000 = WOODEN_SPEAR;
            case "stone_spear" -> var10000 = STONE_SPEAR;
            case "copper_spear" -> var10000 = COPPER_SPEAR;
            case "iron_spear" -> var10000 = IRON_SPEAR;
            case "golden_spear" -> var10000 = GOLDEN_SPEAR;
            case "diamond_spear" -> var10000 = DIAMOND_SPEAR;
            case "netherite_spear" -> var10000 = NETHERITE_SPEAR;
            default -> var10000 = null;
         }

         return var10000;
      }
   }

   private static Item getSpearByRegistryId(Item item) {
      if (SPEARS.contains(item)) {
         return item;
      } else {
         Identifier var1 = Registries.ITEM.getId(item);
         return var1 == null ? null : getSpearByIdentifier(var1.toString());
      }
   }

   private static Item getSpearByViaId(int itemId) {
      Item var10000;
      switch (itemId) {
         case 1536 -> var10000 = WOODEN_SPEAR;
         case 1537 -> var10000 = STONE_SPEAR;
         case 1538 -> var10000 = COPPER_SPEAR;
         case 1539 -> var10000 = IRON_SPEAR;
         case 1540 -> var10000 = GOLDEN_SPEAR;
         case 1541 -> var10000 = DIAMOND_SPEAR;
         case 1542 -> var10000 = NETHERITE_SPEAR;
         default -> var10000 = null;
      }

      return var10000;
   }

   private static Integer getViaIdBySpear(Item spear) {
      if (spear == WOODEN_SPEAR) {
         return 1536;
      } else if (spear == STONE_SPEAR) {
         return 1537;
      } else if (spear == COPPER_SPEAR) {
         return 1538;
      } else if (spear == IRON_SPEAR) {
         return 1539;
      } else if (spear == GOLDEN_SPEAR) {
         return 1540;
      } else if (spear == DIAMOND_SPEAR) {
         return 1541;
      } else {
         return spear == NETHERITE_SPEAR ? 1542 : null;
      }
   }

   private static Item register(String name, ToolMaterials material, int attackDamage, float attackSpeed) {
      Identifier var4 = Identifier.of("minecraft", name);
      return Registries.ITEM.containsId(var4) ? (Item)Registries.ITEM.get(var4) : (Item)Registry.register(Registries.ITEM, var4, new Item(settings(material, attackDamage, attackSpeed)));
   }

   private static SoundEvent registerSound(String name) {
      Identifier var1 = Identifier.of("minecraft", name);
      return Registries.SOUND_EVENT.containsId(var1) ? (SoundEvent)Registries.SOUND_EVENT.get(var1) : (SoundEvent)Registry.register(Registries.SOUND_EVENT, var1, SoundEvent.of(var1));
   }

   private static Item.Settings settings(ToolMaterials material, int attackDamage, float attackSpeed) {
      Item.Settings var3 = (new Item.Settings()).maxDamage(material.getDurability()).attributeModifiers(SwordItem.createAttributeModifiers(material, attackDamage, attackSpeed)).component(DataComponentTypes.TOOL, TridentItem.createToolComponent());
      if (material == ToolMaterials.NETHERITE) {
         var3.fireproof();
      }

      return var3;
   }

   static {
      WOODEN_SPEAR = register("wooden_spear", ToolMaterials.WOOD, 3, -2.9F);
      STONE_SPEAR = register("stone_spear", ToolMaterials.STONE, 4, -2.9F);
      COPPER_SPEAR = register("copper_spear", ToolMaterials.STONE, 4, -2.9F);
      IRON_SPEAR = register("iron_spear", ToolMaterials.IRON, 5, -2.9F);
      GOLDEN_SPEAR = register("golden_spear", ToolMaterials.GOLD, 3, -2.9F);
      DIAMOND_SPEAR = register("diamond_spear", ToolMaterials.DIAMOND, 6, -2.9F);
      NETHERITE_SPEAR = register("netherite_spear", ToolMaterials.NETHERITE, 7, -2.9F);
      SPEAR_USE = registerSound("item.spear.use");
      SPEAR_HIT = registerSound("item.spear.hit");
      SPEAR_ATTACK = registerSound("item.spear.attack");
      WOODEN_SPEAR_USE = registerSound("item.spear_wood.use");
      WOODEN_SPEAR_HIT = registerSound("item.spear_wood.hit");
      WOODEN_SPEAR_ATTACK = registerSound("item.spear_wood.attack");
      SPEAR_LUNGE_1 = registerSound("item.spear.lunge_1");
      SPEAR_LUNGE_2 = registerSound("item.spear.lunge_2");
      SPEAR_LUNGE_3 = registerSound("item.spear.lunge_3");
      SPEARS = List.of(WOODEN_SPEAR, STONE_SPEAR, COPPER_SPEAR, IRON_SPEAR, GOLDEN_SPEAR, DIAMOND_SPEAR, NETHERITE_SPEAR);
      IN_HAND_RENDER = ThreadLocal.withInitial(() -> false);
      lastFallbackSpearAttackTick = -20L;
   }
}
