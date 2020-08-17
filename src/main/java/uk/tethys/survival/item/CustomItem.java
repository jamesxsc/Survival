package uk.tethys.survival.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public interface CustomItem {

    NamespacedKey getRecipeKey();

    Recipe getRecipe();

    String getId();

    ItemStack getItem() throws RuntimeException;

}
