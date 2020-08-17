package uk.tethys.survival;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import uk.tethys.survival.commands.ClaimCommand;
import uk.tethys.survival.commands.CreateShopCommand;
import uk.tethys.survival.commands.ShopCommand;
import uk.tethys.survival.commands.internal.DRCLCommand;
import uk.tethys.survival.core.SQLManager;
import uk.tethys.survival.item.BuildersWandItem;
import uk.tethys.survival.item.CustomItems;
import uk.tethys.survival.listeners.*;
import uk.tethys.survival.managers.ClaimManager;
import uk.tethys.survival.managers.ShopManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class Survival extends JavaPlugin {

    private SQLManager sqlManager;

    //managers
    private ClaimManager claimManager;
    private ShopManager shopManager;

    private EconomyListener economyListener;

    public EconomyListener getEconomyListener() {
        return economyListener;
    }

    public static NamespacedKey IS_CUSTOM_ITEM;

    public static Survival INSTANCE;

    @Override
    public void onEnable() {
        INSTANCE = this;

        try {
            sqlManager = new SQLManager(this);
            Bukkit.getLogger().info("Successfully connected to MySQL DB");
        } catch (IOException | RuntimeException e) {
            // TODO NEEDS MORE VERBOSITY
            Bukkit.getLogger().severe("Connection to MySQL DB failed");
            e.printStackTrace();
            Bukkit.getLogger().severe("Disabling self");
            getPluginLoader().disablePlugin(this);
            return;
        }

        IS_CUSTOM_ITEM = new NamespacedKey(this, "is-custom-item");

        getCommand("claim").setExecutor(new ClaimCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("createshop").setExecutor(new CreateShopCommand(this));

        getCommand("drcl").setExecutor(new DRCLCommand(this));

        getServer().getPluginManager().registerEvents(new ClaimListener(this), this);
        getServer().getPluginManager().registerEvents(new CreateShopListener(this), this);
        economyListener = new EconomyListener(this);
        getServer().getPluginManager().registerEvents(this.economyListener, this);
        getServer().getPluginManager().registerEvents(new CentralShopListener(this), this);
        getServer().getPluginManager().registerEvents(new BuildersWandListener(), this);

        // managers
        claimManager = new ClaimManager(this);
        shopManager = new ShopManager(this);

        CustomItems.registerRecipes();

        // todo TEMP
        Bukkit.getScheduler().runTaskTimer(this, new BuildersWandItem.PreviewTask(), 50, 0);
    }

    @Override
    public void onDisable() {

    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public Connection getDBConnection() throws SQLException {
        return this.sqlManager.getConnection();
    }

}
