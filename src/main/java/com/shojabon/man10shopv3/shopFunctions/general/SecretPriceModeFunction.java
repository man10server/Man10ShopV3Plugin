package com.shojabon.man10shopv3.shopFunctions.general;

import ToolMenu.BooleanInputMenu;
import com.shojabon.man10shopv3.Man10ShopV3;
import com.shojabon.man10shopv3.annotations.ShopFunctionDefinition;
import com.shojabon.man10shopv3.dataClass.Man10Shop;
import com.shojabon.man10shopv3.dataClass.ShopFunction;
import com.shojabon.man10shopv3.menus.settings.SettingsMainMenu;
import com.shojabon.mcutils.Utils.SInventory.SInventoryItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.json.JSONObject;

@ShopFunctionDefinition(
        internalFunctionName = "secretPriceMode",
        name = "値段非表示モード",
        explanation = {"設定した分間毎に値段を設定する", "どちらかが0の場合設定は無効化"},
        enabledShopType = {"BUY", "SELL"},
        iconMaterial = Material.POISONOUS_POTATO,
        category = "一般設定",
        allowedPermission = "MODERATOR",
        isAdminSetting = true
)
public class SecretPriceModeFunction extends ShopFunction {
    public SecretPriceModeFunction(Man10Shop shop, Man10ShopV3 plugin) {
        super(shop, plugin);
    }
    public boolean isEnabled(){
        return getFunctionData().getBoolean("enabled");
    }

    @Override
    public SInventoryItem getSettingItem(Player player, SInventoryItem item) {
        item.setEvent(e -> {
            //confirmation menu
            BooleanInputMenu menu = new BooleanInputMenu(isEnabled(), "値段非表示モード", plugin);
            menu.setOnClose(ee -> new SettingsMainMenu(player, shop, getDefinition().category(), plugin).open(player));
            menu.setOnCancel(ee -> new SettingsMainMenu(player, shop, getDefinition().category(), plugin).open(player));
            menu.setOnConfirm(bool -> {
                Man10ShopV3.threadPool.submit(() -> {
                    if(!setVariable(player, "enabled", bool)){
                        return;
                    }
                    success(player, "値段モードを設定しました");
                    new SettingsMainMenu(player, shop, getDefinition().category(), plugin).open(player);
                });
            });
            menu.open(player);
        });
        return item;
    }
}
