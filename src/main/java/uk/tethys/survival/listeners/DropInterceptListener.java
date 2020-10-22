package uk.tethys.survival.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DropInterceptListener implements Listener {

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        List<ItemStack> drops = event.getDrops();
        drops.clear();

        //todo gen and input custom drops (supplier network)
    }

}
