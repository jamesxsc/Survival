package uk.tethys.survival;

import org.bukkit.plugin.java.JavaPlugin;
import uk.tethys.survival.commands.ClaimCommand;
import uk.tethys.survival.commands.ShopCommand;
import uk.tethys.survival.listeners.ClaimListener;
import uk.tethys.survival.managers.ClaimManager;

public class Survival extends JavaPlugin {

    //managers
    private ClaimManager claimManager;

    @Override
    public void onEnable() {
        getCommand("claim").setExecutor(new ClaimCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getServer().getPluginManager().registerEvents(new ClaimListener(this), this);

        // managers
        claimManager = new ClaimManager(this);
    }

    @Override
    public void onDisable() {

    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }


}
