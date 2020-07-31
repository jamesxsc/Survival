package uk.tethys.survival.message.template;

import org.bukkit.ChatColor;

import java.util.function.Supplier;

// unneccessary
public class PlayerInfoMessageFactory extends BasicMessageFactory {

    private final boolean self;
    private final String name;
    private final String k;
    private final String v;

    private final ChatColor normal = ChatColor.AQUA;
    private final ChatColor kc = ChatColor.BLUE;
    private final ChatColor vc = ChatColor.DARK_BLUE;
    private final ChatColor namec = ChatColor.DARK_BLUE;

    public PlayerInfoMessageFactory(boolean self, String name, String k, String v) {
        this.self = self;
        this.name = name;
        this.k = k;
        this.v = v;
    }

    @Override
    public Supplier<String> getSupplier() {
        return () -> {
            if (self) {
                return normal + "Your " + kc + k + normal + " is currently " + vc + v;
            } else {
                return namec + name + normal + "'s " + kc + k + normal + " is currently " + vc + v;
            }
        };
    }

}
