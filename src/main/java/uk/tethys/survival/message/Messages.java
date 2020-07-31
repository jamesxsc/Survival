package uk.tethys.survival.message;

import org.bukkit.inventory.ItemStack;

public class Messages {

    public static final String PLAYER_ONLY = "no console";
    public static final String OPERATION_CANCELLED = "operation cancelled";

    public static final String CLAIM_TOOL_INFO = "claim tool info";
    public static final String SHOP_TOOL_INFO = "shop tool info";
    public static final String CLAIM_CREATE_SUCCESS = "claim create success";
    public static final String CLAIM_CREATE_FAIL = "claim create fail";
    public static final String BUY_PRICE_PROMPT = "buy price prompt";
    public static final String NFE_LOOP_OR_EXIT = "nfe loop or exit";
    public static final String SELL_PRICE_PROMPT = "sell price prompt";
    public static final String NO_BUY_OR_SELL = "no buy or sell";
    public static final String CREATING_SHOP = "creating shop";
    public static final String DENIED_DUE_TO_DB_FAIL = "denied due to db fail";

    public static String DRCL_WAIT() {
        return "";
        // todo add time remaining
    }

    public static String SELF_BALANCE(int balance) {
        return "";
    }

    public static String RETRIEVE_ERROR(String val, String error) {
        return "";
    }

    public static String UNLOCK_ITEM(ItemStack itemStack) {
        return "";
    }

    public static String CLAIM_AREA_SIZE(int min, int max) {
        return "";
    }
}
