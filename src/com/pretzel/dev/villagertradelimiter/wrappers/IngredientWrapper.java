package com.pretzel.dev.villagertradelimiter.wrappers;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class IngredientWrapper {
    private final NBTCompound recipe;
    private final String key;
    private final ItemStack itemStack;

    /**
     * @param recipe The NBTCompound that contains the recipe's NBT data of the ingredient
     * @param key The key under which the recipe is located
     */
    public IngredientWrapper(final NBTCompound recipe, final String key) {
        this.recipe = recipe;
        this.key = key;
        this.itemStack = getItemStack();
    }

    /** @return The {@link ItemStack} representing the data in the recipe */
    public ItemStack getItemStack() {
        return recipe.getItemStack(key);
    }

    /** @param itemStack The {@link ItemStack} which will replace the item in the recipe */
    public void setItemStack(final ItemStack itemStack) {
        // Ensure itemStack is not null or empty before set
        if(itemStack != null && itemStack.getType() != Material.AIR) {
            NBTCompound itemCompound = recipe.getOrCreateCompound(key);
            itemCompound.setItemStack(key, itemStack);
            // Validate the itemStack has all necessary keys
            validateItemStack(itemCompound);

        }
    }

    private void validateItemStack(NBTCompound itemCompound) {
        if (!itemCompound.hasKey("id")) {
            itemCompound.setString("id", itemStack.getType().toString().toLowerCase());
        }
        if (!itemCompound.hasKey("count")) {
            itemCompound.setInteger("count", itemStack.getAmount());
        }
        // Add any other necessary checks here to ensure all required NBT fields are present
    }

    /** Resets the material ID and the amount of this ingredient to default values */
    public void reset() {
        setItemStack(itemStack);
    }
}
