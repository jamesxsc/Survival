package uk.tethys.survival.tasks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ClaimTask {

    public static class SustainParticle extends BukkitRunnable {

        private final Player player;
        private final double color;
        private final Location[] locs;

        public SustainParticle(Player player, double color, Location... locs) {
            if (color > 1 || color < 0)
                throw new IllegalArgumentException("Double must be below 1 (/24D)");
            this.player = player;
            this.color = color;
            this.locs = locs;
        }

        @Override
        public void run() {
            for (Location location : locs) {
                player.spawnParticle(Particle.NOTE, location.getBlockX() + .5, location.getBlockY() + 1.5,
                        location.getBlockZ() + .5, 0, color, 0, 0, 1);
            }
        }

    }

}
