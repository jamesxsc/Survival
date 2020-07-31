package uk.tethys.survival.listeners;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import uk.tethys.survival.Survival;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EconomyListener implements Listener {

    private final Survival plugin;
    private final Set<UUID> allowed;

    public EconomyListener(Survival plugin) {
        this.plugin = plugin;
        this.allowed = new HashSet<>();
    }

    public Set<UUID> getAllowed() {
        return allowed;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Date lastPlayed = new Date(player.getLastPlayed());
        if (!lastPlayed.toInstant().truncatedTo(ChronoUnit.DAYS).equals(new Date().toInstant().truncatedTo(ChronoUnit.DAYS))) {
            allowed.add(player.getUniqueId());
            // todo fancy message building
            player.sendMessage("");
            player.spigot().sendMessage(new ComponentBuilder(player.getDisplayName()).color(ChatColor.DARK_AQUA)
                    .append(", ").color(ChatColor.AQUA).append("you have not yet claimed your daily logon reward yet! ")
                    .color(ChatColor.GREEN).append("[Click here to claim now!]").color(ChatColor.BLUE).bold(true)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click!").create()))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/survival:drcl " + player.getUniqueId().toString())).create());
            player.sendMessage("");
        }
    }

}
