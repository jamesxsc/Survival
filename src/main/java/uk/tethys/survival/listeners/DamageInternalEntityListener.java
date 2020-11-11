package uk.tethys.survival.listeners;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import uk.tethys.survival.Survival;

public class DamageInternalEntityListener implements Listener {

    public static NamespacedKey INTERNAL_ENTITY;

    public DamageInternalEntityListener(Survival survival) {
        INTERNAL_ENTITY = new NamespacedKey(survival, "DELETE_AFTER");
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (pdc.has(INTERNAL_ENTITY, PersistentDataType.LONG)) {
            event.setCancelled(true);
        }
    }

    public static class KillTask implements Runnable {
        @Override
        public void run() {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {

                    PersistentDataContainer pdc = entity.getPersistentDataContainer();
                    if (pdc.has(DamageInternalEntityListener.INTERNAL_ENTITY, PersistentDataType.LONG)) {

                        Long killAfterMillis = pdc.get(DamageInternalEntityListener.INTERNAL_ENTITY, PersistentDataType.LONG);
                        if (killAfterMillis == null || killAfterMillis == -1L) return;
                        if (System.currentTimeMillis() >= killAfterMillis)
                            entity.remove();
                    }
                }
            }
        }
    }
}
