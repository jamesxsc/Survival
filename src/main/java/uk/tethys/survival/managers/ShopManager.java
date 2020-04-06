package uk.tethys.survival.managers;

import uk.tethys.survival.Survival;
import uk.tethys.survival.objects.Shop;

import java.util.HashSet;
import java.util.Set;

public class ShopManager {

    private final Survival plugin;

    public ShopManager(Survival plugin) {
        this.plugin = plugin;
        this.shops = new HashSet<>();
    }

    private final Set<Shop> shops;

    public Set<Shop> getShops() {
        return shops;
    }

}
