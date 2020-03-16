package uk.tethys.survival.managers;

import uk.tethys.survival.Survival;
import uk.tethys.survival.objects.Claim;

import java.util.HashSet;
import java.util.Set;

public class ClaimManager {

    private final Survival plugin;

    public ClaimManager(Survival plugin) {
        this.plugin = plugin;
        claims = new HashSet<>();
    }
    
    private final Set<Claim> claims;

    public Set<Claim> getClaims() {
        return claims;
    }

}
