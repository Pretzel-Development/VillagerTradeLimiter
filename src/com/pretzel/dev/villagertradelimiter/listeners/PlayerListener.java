package com.pretzel.dev.villagertradelimiter.listeners;

import com.pretzel.dev.villagertradelimiter.VillagerTradeLimiter;
import com.pretzel.dev.villagertradelimiter.data.Cooldown;
import com.pretzel.dev.villagertradelimiter.data.PlayerData;
import com.pretzel.dev.villagertradelimiter.lib.Util;
import com.pretzel.dev.villagertradelimiter.settings.Settings;
import com.pretzel.dev.villagertradelimiter.wrappers.IngredientWrapper;
import com.pretzel.dev.villagertradelimiter.wrappers.PlayerWrapper;
import com.pretzel.dev.villagertradelimiter.wrappers.RecipeWrapper;
import com.pretzel.dev.villagertradelimiter.wrappers.VillagerWrapper;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerListener implements Listener {
    private final VillagerTradeLimiter instance;
    private final Settings settings;

    /**
     * @param instance The instance of VillagerTradeLimiter.java
     * @param settings The settings instance
     */
    public PlayerListener(final VillagerTradeLimiter instance, final Settings settings) {
        this.instance = instance;
        this.settings = settings;
    }

    /** Handles when a player begins trading with a villager */
    @EventHandler
    public void onPlayerBeginTrading(final PlayerInteractEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getRightClicked() instanceof Villager villager)) return;

        final Player player = event.getPlayer();

        // Check if villager trading is disabled in the config
        if (instance.getCfg().getStringList("DisableWorlds").contains(villager.getWorld().getName())) return;

        ItemStack heldItem = player.getInventory().getItem(event.getHand());
        if (heldItem != null) {
            Material heldItemType = heldItem.getType();
            for (String ignoredType : instance.getCfg().getStringList("IgnoreHeldItems")) {
                if (heldItemType.equals(Material.matchMaterial(ignoredType))) return;
            }
        }

        if (settings.shouldSkipNPC(player) || settings.shouldSkipNPC(villager)) return;
        if (villager.getProfession() == Villager.Profession.NONE ||
                villager.getProfession() == Villager.Profession.NITWIT ||
                villager.getRecipeCount() == 0) return;

        // Handle job locking issue
        if (villager.getVillagerLevel() == 1 && villager.getProfession() != Villager.Profession.NONE) {
            // Reset job site if no trade is completed
            Bukkit.getScheduler().runTaskLater(instance, () -> {
                if (villager.getVillagerLevel() == 1 && villager.getRecipeCount() == 0) {
                    villager.setProfession(Villager.Profession.NONE);
                    villager.setVillagerLevel(0); // Reset to ensure no locking occurs
                }
            }, 20L); // Delay ensures the interaction completes first
        }

        if (instance.getCfg().isBoolean("DisableTrading")) {
            if (instance.getCfg().getBoolean("DisableTrading", false)) {
                event.setCancelled(true);
                return;
            }
        } else {
            final List<String> disabledWorlds = instance.getCfg().getStringList("DisableTrading");
            final String world = player.getWorld().getName();
            for (String disabledWorld : disabledWorlds) {
                if (world.equals(disabledWorld)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Your trade view logic here
        if (!instance.getPlayerData().containsKey(player.getUniqueId())) {
            instance.getPlayerData().put(player.getUniqueId(), new PlayerData());
        }
        if (!instance.getPlayerData().containsKey(villager.getUniqueId())) {
            instance.getPlayerData().put(villager.getUniqueId(), new PlayerData());
        }

        this.see(villager, player, player);
    }


    /**
     * Opens the villager's trading menu, with the adjusted trades of another player (or the same player)
     * @param villager The villager whose trades you want to see
     * @param player The player who calls the command, or the player that has begun trading
     * @param other The other player to view trades for, or the player that has just begun trading
     */
    public void see(final Villager villager, final Player player, final OfflinePlayer other) {
        //Skips when the villager is in a disabled world
        if(instance.getCfg().getStringList("DisableWorlds").contains(villager.getWorld().getName())) {
            Util.sendMsg(instance.getLang("see.noworld"), player);
            return;
        }

        //Wraps the villager and player into wrapper classes
        final VillagerWrapper villagerWrapper = new VillagerWrapper(villager);
        final PlayerWrapper otherWrapper = new PlayerWrapper(other);
        final Player otherPlayer = otherWrapper.getPlayer();
        if(settings.shouldSkipNPC(player) || settings.shouldSkipNPC(villager) || otherPlayer == null || settings.shouldSkipNPC(otherPlayer)) return; //Skips NPCs

        final PlayerData playerData = instance.getPlayerData().get(other.getUniqueId());
        if(playerData != null) playerData.setTradingVillager(villagerWrapper);

        //Checks if the version is old, before the 1.16 UUID changes
        String version = instance.getServer().getClass().getPackage().getName();
        boolean isOld = version.contains("1_13_") || version.contains("1_14_") || version.contains("1_15_");

        //Calculates the player's total reputation and Hero of the Village discount
        int totalReputation = villagerWrapper.getTotalReputation(villagerWrapper, otherWrapper, isOld);
        double hotvDiscount = getHotvDiscount(otherWrapper);

        //Adjusts the recipe prices, MaxUses, and ingredients
        final List<RecipeWrapper> recipes = villagerWrapper.getRecipes();
        for(RecipeWrapper recipe : recipes) {
            //Set the special price (discount)
            recipe.setSpecialPrice(getDiscount(recipe, totalReputation, hotvDiscount));

            //Set ingredient materials and amounts
            final ConfigurationSection override = settings.getOverride(recipe.getItemStack("buy"), recipe.getItemStack("sell"));
            if(override != null) {
                setIngredient(override.getConfigurationSection("Item1"), recipe.getIngredient1());
                setIngredient(override.getConfigurationSection("Item2"), recipe.getIngredient2());
                setIngredient(override.getConfigurationSection("Result"), recipe.getResult());
            }

            //Set the maximum number of uses (trades/restock)
            recipe.setMaxUses(getMaxUses(recipe, other));
        }

        //Open the villager's trading menu
        player.openMerchant(villager, false);
    }

    /**
     * @param recipe The recipe to get the base price for
     * @return The initial price of a recipe/trade, before any discounts are applied
     */
    private int getBasePrice(final RecipeWrapper recipe) {
        int basePrice = recipe.getIngredient1().getItemStack().getAmount();
        basePrice = settings.fetchInt(recipe, "Item1.Amount", basePrice);
        return Math.min(Math.max(basePrice, 1), 64);
    }

    /**
     * @param recipe The recipe to get the demand for
     * @return The current value of the demand for the given recipe
     */
    private int getDemand(final RecipeWrapper recipe) {
        int demand = recipe.getDemand();
        int maxDemand = settings.fetchInt(recipe, "MaxDemand", -1);
        if(maxDemand < 0) maxDemand = 1000000;
        if(demand > maxDemand) {
            recipe.setDemand(maxDemand);
            return maxDemand;
        }
        return demand;
    }

    /**
     * @param recipe The recipe to get the discount for
     * @param totalReputation The player's total reputation from a villager's gossips
     * @param hotvDiscount The total discount from the Hero of the Village effect
     * @return The total discount for the recipe, which is added to the base price to get the final price
     */
    private int getDiscount(final RecipeWrapper recipe, int totalReputation, double hotvDiscount) {
        //Calculates the total discount
        int basePrice = getBasePrice(recipe);
        int demand = getDemand(recipe);
        float priceMultiplier = recipe.getPriceMultiplier();
        int discount = -(int)(totalReputation * priceMultiplier) - (int)(hotvDiscount * basePrice) + Math.max(0, (int)(demand * priceMultiplier * basePrice));

        double maxDiscount = settings.fetchDouble(recipe, "MaxDiscount", 0.3); //0.1
        if(maxDiscount >= 0.0 && maxDiscount <= 1.0) {
            //Change the discount to the smaller MaxDiscount
            if(basePrice + discount < basePrice * (1.0 - maxDiscount)) {
                discount = -(int)(basePrice * maxDiscount);
            }
        } else if(maxDiscount > 1.0) {
            //Change the discount to the larger MaxDiscount
            //TODO: Allow for better fine-tuning
            discount = (int)(discount * maxDiscount);
        }
        return discount;
    }

    /**
     * @param recipe The recipe to get the MaxUses for
     * @return The current maximum number of times a player can make a trade before the villager restocks
     */
    private int getMaxUses(final RecipeWrapper recipe, final OfflinePlayer player) {
        int uses = recipe.getMaxUses();
        int maxUses = settings.fetchInt(recipe, "MaxUses", -1);
        boolean disabled = settings.fetchBoolean(recipe, "Disabled", false);

        //Disables the trade if the player has an active cooldown for the trade
        final PlayerData playerData = instance.getPlayerData().get(player.getUniqueId());
        if(playerData != null && playerData.getTradingVillager() != null) {
            final ConfigurationSection overrides = instance.getCfg().getConfigurationSection("Overrides");
            if(overrides != null) {
                final String type = settings.getType(recipe.getItemStack("sell"), recipe.getItemStack("buy"), recipe.getItemStack("buyB"));
                final String global = instance.getCfg().getString("Cooldown", "0");
                final String local = overrides.getString(type+".Cooldown", global);
                if(type != null && !local.equals("0")) {
                    if(playerData.getTradingCooldowns().containsKey(type)) {
                        final Date now = Date.from(Instant.now());
                        final Date lastTrade = Cooldown.parseTime(playerData.getTradingCooldowns().get(type));
                        long cooldown = Cooldown.parseCooldown(local);
                        if(lastTrade != null && (now.getTime()/1000L >= lastTrade.getTime()/1000L + cooldown)) {
                            playerData.getTradingCooldowns().remove(type);
                        } else {
                            maxUses = 0;
                        }
                    }
                }
            }
        }

        if(maxUses < 0) maxUses = uses;
        if(disabled) maxUses = 0;
        return maxUses;
    }

    /**
     * @param playerWrapper The wrapped player to check the hotv effect for
     * @return The Hero of the Village discount factor, adjusted by config
     */
    private double getHotvDiscount(final PlayerWrapper playerWrapper) {
        final Player player = playerWrapper.getPlayer();
        if(player == null) return 0.0;

        final PotionEffectType effectType = PotionEffectType.HERO_OF_THE_VILLAGE;
        if(!player.hasPotionEffect(effectType)) return 0.0;

        final PotionEffect effect = player.getPotionEffect(effectType);
        if(effect == null) return 0.0;

        //Calculates the discount factor from the player's current effect level or the defined maximum
        int heroLevel = effect.getAmplifier()+1;
        final int maxHeroLevel = instance.getCfg().getInt("MaxHeroLevel", -1);
        if(maxHeroLevel == 0 || heroLevel == 0) return 0.0;
        if(maxHeroLevel > 0 && heroLevel > maxHeroLevel) {
            heroLevel = maxHeroLevel;
        }
        return 0.0625*(heroLevel-1) + 0.3;
    }

    /**
     * @param item The config section that contains the settings for Item1, Item2, or Result items in the trade
     * @param ingredient The respective ingredient to change, based on config.yml
     */
    private void setIngredient(final ConfigurationSection item, final IngredientWrapper ingredient) {
        if(item == null) return;
        ItemStack previous = ingredient.getItemStack();
        Material material = Material.matchMaterial(item.getString("Material", previous.getType().getKey().getKey()));
        if (material != null) {
            ingredient.setItemStack(new ItemStack(
                material,
                item.getInt("Amount", previous.getAmount())
            ));
        }
    }
}
