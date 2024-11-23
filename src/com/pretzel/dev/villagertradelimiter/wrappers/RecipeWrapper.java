package com.pretzel.dev.villagertradelimiter.wrappers;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class RecipeWrapper {
    //A list of all the items with a default MaxUses of 12 and 3, respectively
    private static final Material[] MAX_USES_12 = new Material[]{Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS, Material.IRON_INGOT, Material.BELL, Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS, Material.LAVA_BUCKET, Material.DIAMOND, Material.SHIELD, Material.RABBIT_STEW, Material.DRIED_KELP_BLOCK, Material.SWEET_BERRIES, Material.MAP, Material.FILLED_MAP, Material.COMPASS, Material.ITEM_FRAME, Material.GLOBE_BANNER_PATTERN, Material.WHITE_BANNER, Material.LIGHT_GRAY_BANNER, Material.GRAY_BANNER, Material.BLACK_BANNER, Material.BROWN_BANNER, Material.ORANGE_BANNER, Material.YELLOW_BANNER, Material.LIME_BANNER, Material.GREEN_BANNER, Material.CYAN_BANNER, Material.BLUE_BANNER, Material.LIGHT_BLUE_BANNER, Material.PURPLE_BANNER, Material.MAGENTA_BANNER, Material.PINK_BANNER, Material.RED_BANNER, Material.WHITE_BED, Material.LIGHT_GRAY_BED, Material.GRAY_BED, Material.BLACK_BED, Material.BROWN_BED, Material.ORANGE_BED, Material.YELLOW_BED, Material.LIME_BED, Material.GREEN_BED, Material.CYAN_BED, Material.BLUE_BED, Material.LIGHT_BLUE_BED, Material.PURPLE_BED, Material.MAGENTA_BED, Material.PINK_BED, Material.RED_BED, Material.REDSTONE, Material.GOLD_INGOT, Material.LAPIS_LAZULI, Material.RABBIT_FOOT, Material.GLOWSTONE, Material.TURTLE_SCUTE, Material.GLASS_BOTTLE, Material.ENDER_PEARL, Material.NETHER_WART, Material.EXPERIENCE_BOTTLE, Material.PUMPKIN, Material.PUMPKIN_PIE, Material.MELON, Material.COOKIE, Material.CAKE, Material.SUSPICIOUS_STEW, Material.GOLDEN_CARROT, Material.GLISTERING_MELON_SLICE, Material.CAMPFIRE, Material.TROPICAL_FISH, Material.PUFFERFISH, Material.BIRCH_BOAT, Material.ACACIA_BOAT, Material.OAK_BOAT, Material.DARK_OAK_BOAT, Material.SPRUCE_BOAT, Material.JUNGLE_BOAT, Material.ARROW, Material.FLINT, Material.STRING, Material.TRIPWIRE_HOOK, Material.TIPPED_ARROW, Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS, Material.LEATHER, Material.RABBIT_HIDE, Material.LEATHER_HORSE_ARMOR, Material.SADDLE, Material.BOOK, Material.ENCHANTED_BOOK, Material.BOOKSHELF, Material.INK_SAC, Material.GLASS, Material.WRITABLE_BOOK, Material.CLOCK, Material.NAME_TAG, Material.QUARTZ, Material.QUARTZ_PILLAR, Material.QUARTZ_BLOCK, Material.TERRACOTTA, Material.WHITE_TERRACOTTA, Material.LIGHT_GRAY_TERRACOTTA, Material.GRAY_TERRACOTTA, Material.BLACK_TERRACOTTA, Material.BROWN_TERRACOTTA, Material.ORANGE_TERRACOTTA, Material.YELLOW_TERRACOTTA, Material.LIME_TERRACOTTA, Material.GREEN_TERRACOTTA, Material.CYAN_TERRACOTTA, Material.BLUE_TERRACOTTA, Material.LIGHT_BLUE_TERRACOTTA, Material.PURPLE_TERRACOTTA, Material.MAGENTA_TERRACOTTA, Material.PINK_TERRACOTTA, Material.RED_TERRACOTTA, Material.WHITE_GLAZED_TERRACOTTA, Material.LIGHT_GRAY_GLAZED_TERRACOTTA, Material.GRAY_GLAZED_TERRACOTTA, Material.BLACK_GLAZED_TERRACOTTA, Material.BROWN_GLAZED_TERRACOTTA, Material.ORANGE_GLAZED_TERRACOTTA, Material.YELLOW_GLAZED_TERRACOTTA, Material.LIME_GLAZED_TERRACOTTA, Material.GREEN_GLAZED_TERRACOTTA, Material.CYAN_GLAZED_TERRACOTTA, Material.BLUE_GLAZED_TERRACOTTA, Material.LIGHT_BLUE_GLAZED_TERRACOTTA, Material.PURPLE_GLAZED_TERRACOTTA, Material.MAGENTA_GLAZED_TERRACOTTA, Material.PINK_GLAZED_TERRACOTTA, Material.RED_GLAZED_TERRACOTTA, Material.SHEARS, Material.PAINTING, Material.STONE_AXE, Material.STONE_SHOVEL, Material.STONE_PICKAXE, Material.STONE_HOE};
    private static final Material[] MAX_USES_3 = new Material[]{Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, Material.DIAMOND_SWORD, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_PICKAXE, Material.DIAMOND_HOE, Material.IRON_SWORD, Material.IRON_AXE, Material.IRON_SHOVEL, Material.IRON_PICKAXE, Material.FISHING_ROD, Material.BOW, Material.CROSSBOW};

    private final NBTCompound recipe;
    private final IngredientWrapper ingredient1;
    private final IngredientWrapper ingredient2;
    private final IngredientWrapper result;
    private final int specialPrice;

    /** @param recipe The NBTCompound that contains the villager's NBT data of the recipe */
    public RecipeWrapper(final NBTCompound recipe) {
        this.recipe = recipe;
        this.ingredient1 = new IngredientWrapper(recipe, "buy");
        this.ingredient2 = new IngredientWrapper(recipe, "buyB");
        this.result = new IngredientWrapper(recipe, "sell");
        this.specialPrice = getSpecialPrice();
    }

    /** @param demand The demand, which increases prices if you buy too often. Negative values are ignored. */
    public void setDemand(int demand) { recipe.setInteger("demand", demand); }

    /** @param specialPrice The discount, which is added to the base price. A negative value will decrease the price, and a positive value will increase the price. */
    public void setSpecialPrice(int specialPrice) { recipe.setInteger("specialPrice", specialPrice); }

    /** @param maxUses The maximum number of times a player can make a trade before the villager restocks */
    public void setMaxUses(int maxUses) { recipe.setInteger("maxUses", maxUses); }

    /** Resets the recipe back to its default state */
    public void reset() {
        this.setSpecialPrice(this.specialPrice);
        this.ingredient1.reset();
        this.ingredient2.reset();
        this.result.reset();

        int maxUses = 16;
        Material buyMaterial = recipe.getItemStack("buy").getType();
        Material sellMaterial = recipe.getItemStack("sell").getType();
        if(Arrays.asList(MAX_USES_12).contains(buyMaterial) || Arrays.asList(MAX_USES_12).contains(sellMaterial)) {
            maxUses = 12;
        } else if(Arrays.asList(MAX_USES_3).contains(buyMaterial) || Arrays.asList(MAX_USES_3).contains(sellMaterial)) {
            maxUses = 3;
        }
        setMaxUses(maxUses);
    }

    /** @return The wrapper for the first ingredient */
    public IngredientWrapper getIngredient1() { return ingredient1; }

    /** @return The wrapper for the second ingredient */
    public IngredientWrapper getIngredient2() { return ingredient2; }

    /** @return The wrapper for the result */
    public IngredientWrapper getResult() { return result; }

    /** @return The demand for this recipe (increases the price when above 0) */
    public int getDemand() { return recipe.getInteger("demand"); }

    /** @return The price multiplier for this recipe (controls how strongly gossips, demand, etc. affect the price) */
    public float getPriceMultiplier() { return recipe.getFloat("priceMultiplier"); }

    /** @return The discount, which is added to the base price. A negative value will decrease the price, and a positive value will increase the price. */
    public int getSpecialPrice() { return recipe.getInteger("specialPrice"); }

    /** @return The maximum number of times a player can make a trade before the villager restocks */
    public int getMaxUses() { return recipe.getInteger("maxUses"); }

    /** @return The ItemStack representation of an ingredient or the result */
    public ItemStack getItemStack(final String key) { return recipe.getItemStack(key); }
}
