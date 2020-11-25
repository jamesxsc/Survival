package uk.tethys.survival.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.*;
import uk.tethys.survival.Survival;

import java.util.EventListener;

public class BreakInternalBlockListener implements EventListener {

    public static String INTERNAL_BLOCK; // metadata key for deletion time etc...
    private final Survival plugin;

    public BreakInternalBlockListener(Survival plugin) {
        INTERNAL_BLOCK = "DELETE_AFTER";
        this.plugin = plugin;
    }

    // todo if internal then stop it!
    @EventHandler public void onBreak(BlockBreakEvent event) { }
    @EventHandler public void onBreak(BlockBurnEvent event) { }
    @EventHandler public void onBreak(BlockPistonEvent event) { }
    @EventHandler public void onBreak(BlockPhysicsEvent event) { }

    public static class DestroyTask implements Runnable {
        @Override
        public void run() {

        }
    }

}
