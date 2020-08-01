package uk.tethys.survival.message;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.inventory.ItemStack;

import java.awt.*;

public class Messages {

    public static final String PLAYER_ONLY = "no console";
    public static final String OPERATION_CANCELLED = "operation cancelled";

    public static final String CLAIM_TOOL_INFO = "claim tool info";
    public static final String SHOP_TOOL_INFO = color("shop tool info", 71, 53, 30);
    public static final String CLAIM_CREATE_SUCCESS = "claim create success";
    public static final String CLAIM_CREATE_FAIL = "claim overlaps another one or the db is broken";
    public static final String BUY_PRICE_PROMPT = "buy price prompt";
    public static final String NFE_LOOP_OR_EXIT = "nfe loop or exit";
    public static final String SELL_PRICE_PROMPT = "sell price prompt";
    public static final String NO_BUY_OR_SELL = "no buy or sell";
    public static final String CREATING_SHOP = "creating shop";
    public static final String DENIED_DUE_TO_DB_FAIL = "denied due to db fail";
    public static final String NOT_IN_CLAIM = "not in a claim dumbo";

    public static String DRCL_WAIT() {
        return "wait for your reward impatient man";
        // todo add time remaining
    }

    public static String SELF_BALANCE(int balance) {
        return "balance is " + balance;
    }

    public static String RETRIEVE_ERROR(String val, String error) {
        return "error retrieving";
    }

    public static String UNLOCK_ITEM(ItemStack itemStack) {
        return "item unlocked";
    }

    public static String CLAIM_AREA_SIZE(int min, int max) {
        return "wrong size claim...";
    }

    public static String CLAIM_TOOL_MODE(String newMode) {
        return "mode is now " + newMode;
    }

    private static String color(String text, int r, int g, int b) {
        return ChatColor.of(new Color(r, g, b)) + text + ChatColor.RESET;
    }
    
    

    
    
}
