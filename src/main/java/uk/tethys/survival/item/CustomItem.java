package uk.tethys.survival.item;

import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.function.Consumer;

public interface CustomItem {

    NamespacedKey getRecipeKey();

    Consumer<PrepareItemCraftEvent> checkRecipeMeta();

    Recipe getRecipe();

    String getId();

    ItemStack getItem() throws RuntimeException;

    public static boolean checkId(ItemStack itemStack, String id) {
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(CustomItems.KEY, PersistentDataType.STRING)) {
                String actualId = pdc.get(CustomItems.KEY, PersistentDataType.STRING);

                return id.equals(actualId);
            }
        }

        return false;
    }

}
