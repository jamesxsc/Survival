package uk.tethys.survival.item;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import uk.tethys.survival.Survival;

public class CustomItems {

    public static final NamespacedKey KEY = new NamespacedKey(Survival.INSTANCE, "custom_item_id");

    public static BuildersWandItem BUILDERS_WAND = new BuildersWandItem();

    public static void registerRecipes() {
//        Bukkit.addRecipe(BUILDERS_WAND.getRecipe());
    }

}
