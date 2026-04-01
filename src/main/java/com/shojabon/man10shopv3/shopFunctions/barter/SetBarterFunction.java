package com.shojabon.man10shopv3.shopFunctions.barter;

import com.shojabon.man10shopv3.Man10ShopV3;
import com.shojabon.man10shopv3.Man10ShopV3API;
import com.shojabon.man10shopv3.annotations.ShopFunctionDefinition;
import com.shojabon.man10shopv3.dataClass.Man10Shop;
import com.shojabon.man10shopv3.dataClass.ShopFunction;
import com.shojabon.man10shopv3.menus.settings.SettingsMainMenu;
import com.shojabon.man10shopv3.menus.settings.innerSettings.BarterSettingMenu;
import com.shojabon.mcutils.Utils.SInventory.SInventoryItem;
import com.shojabon.mcutils.Utils.SItemStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@ShopFunctionDefinition(
        internalFunctionName = "setBarter",
        name = "トレード設定",
        explanation = {"トレード対象のアイテムなどを設定します"},
        enabledShopType = {"BARTER"},
        iconMaterial = Material.VILLAGER_SPAWN_EGG,
        category = "一般設定",
        allowedPermission = "MODERATOR",
        isAdminSetting = true
)
public class SetBarterFunction extends ShopFunction {
    public SetBarterFunction(Man10Shop shop, Man10ShopV3 plugin) {
        super(shop, plugin);
    }

    public List<ItemStack> getRequiredItems(){
        return loadBarterItems("requiredItems");
    }
    public List<ItemStack> getResultItems(){
        return loadBarterItems("resultItems");
    }

    private List<ItemStack> loadBarterItems(String key){
        List<ItemStack> result = new ArrayList<>();

        JSONArray itemsData = getFunctionData().optJSONArray(key);
        if(itemsData == null){
            plugin.getLogger().warning("[SetBarterFunction] トレード設定リストが見つかりません: shopId=" + shop.getShopId() + ", key=" + key);
            return result;
        }

        for(int i = 0; i < itemsData.length(); i++){
            if(itemsData.isNull(i)) {
                result.add(null);
                continue;
            }

            JSONObject itemData = itemsData.optJSONObject(i);
            if(itemData == null){
                logInvalidItem(key, i, "エントリがオブジェクト形式ではありません");
                result.add(null);
                continue;
            }

            String typeBase64 = itemData.optString("typeBase64", null);
            if(typeBase64 == null || typeBase64.isBlank()){
                logInvalidItem(key, i, "typeBase64 がありません");
                result.add(null);
                continue;
            }

            SItemStack item = SItemStack.fromBase64(typeBase64);
            if(item == null){
                logInvalidItem(key, i, "typeBase64 のデコードに失敗しました");
                result.add(null);
                continue;
            }

            int amount = itemData.optInt("amount", 1);
            item.setAmount(Math.max(amount, 1));
            result.add(item.build());
        }
        return result;
    }

    private void logInvalidItem(String key, int index, String reason){
        plugin.getLogger().warning(
                "[SetBarterFunction] トレードアイテムデータが不正です: shopId="
                        + shop.getShopId()
                        + ", key="
                        + key
                        + ", index="
                        + index
                        + ", 理由="
                        + reason
        );
    }

    public JSONArray itemStackListToJSONArray(List<ItemStack> items){
        JSONArray result = new JSONArray();

        for(ItemStack item: items){
            if(item == null) {
                result.put(JSONObject.NULL);
                continue;
            }
            result.put(Man10ShopV3API.itemStackToJSON(item));
        }
        return result;
    }

    @Override
    public SInventoryItem getSettingItem(Player player, SInventoryItem item) {
        item.setEvent(e -> {
            //required
            List<ItemStack> required = getRequiredItems();
            List<ItemStack> result = getResultItems();
            List<ItemStack> both = new ArrayList<>();
            both.addAll(required);
            both.addAll(result);

            //confirmation menu
            BarterSettingMenu menu = new BarterSettingMenu(player, shop, both, plugin);

            menu.setOnCloseEvent(ee -> new SettingsMainMenu(player, shop, getDefinition().category(), plugin).open(player));

            menu.setOnConfirm(items -> {
                Man10ShopV3.threadPool.submit(() -> {
                    if(!setVariable(player, "requiredItems", itemStackListToJSONArray(items.subList(0, 12)))){
                        return;
                    }
                    if(!setVariable(player, "resultItems", itemStackListToJSONArray(items.subList(12, 13)))){
                        warn(player, "ターゲットアイテム設定中に保存が失敗しました、部分的に保存されている可能性があるのでご確認ください。");
                        return;
                    }
                    new SettingsMainMenu(player, shop, getDefinition().category(), plugin).open(player);
                });
            });

            menu.open(player);

        });

        return item;
    }

}
