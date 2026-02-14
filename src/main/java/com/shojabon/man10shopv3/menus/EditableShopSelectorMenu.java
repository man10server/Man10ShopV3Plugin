package com.shojabon.man10shopv3.menus;

import ToolMenu.CategoricalSInventoryMenu;
import com.shojabon.man10shopv3.Man10ShopV3;
import com.shojabon.man10shopv3.shopFunctions.PermissionFunction;
import com.shojabon.mcutils.Utils.BaseUtils;
import com.shojabon.mcutils.Utils.SInventory.SInventoryItem;
import com.shojabon.mcutils.Utils.SItemStack;
import com.shojabon.mcutils.Utils.SStringBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.function.Consumer;

public class EditableShopSelectorMenu extends CategoricalSInventoryMenu {

    Man10ShopV3 plugin;
    Player player;
    Consumer<String> onClick = null;
    String searchQuery = null;

    JSONObject playerShopsRequest;

    public EditableShopSelectorMenu(Player p, String startingCategory, Man10ShopV3 plugin){
        this(p, startingCategory, null, plugin);
    }

    public EditableShopSelectorMenu(Player p, String startingCategory, String searchQuery, Man10ShopV3 plugin){
        super(new SStringBuilder().aqua().bold().text("管理可能ショップ一覧").build(), startingCategory, plugin);
        this.player = p;
        this.plugin = plugin;
        this.searchQuery = searchQuery;
        this.playerShopsRequest = Man10ShopV3.api.getPlayerShops(player, searchQuery);
        sort(true);

    }

    public void setOnClick(Consumer<String> event){
        this.onClick = event;
    }

    public void renderMenu(){
        String defaultCategory = "検索結果: " + searchQuery;
        if(searchQuery == null) defaultCategory = "その他";
        addInitializedCategory(defaultCategory);

        if(playerShopsRequest == null) return;

        JSONArray shops = playerShopsRequest.getJSONArray("data");
        for(int i = 0; i < shops.length(); i++){

            JSONObject shopInfo = shops.getJSONObject(i);

            JSONObject iconData = shopInfo.getJSONObject("icon");
            SItemStack icon = new SItemStack(Material.valueOf(iconData.getString("material")));
            icon.setCustomModelData(iconData.getInt("customModelData"));
            icon.setDisplayName(new SStringBuilder().green().bold().text(shopInfo.getString("name")).build());
            icon.addLore("§d§lショップタイプ: " + shopInfo.getString("shopType"));
            icon.addLore(new SStringBuilder().lightPurple().bold().text("権限: ").yellow().bold().text(PermissionFunction.getPermissionString(shopInfo.getString("permission"))).build());
            icon.addLore("");
            icon.addLore(new SStringBuilder().red().bold().text("在庫: ").yellow().bold().text(BaseUtils.priceString(shopInfo.getInt("itemCount"))).text("個").build());
            icon.addLore(new SStringBuilder().red().bold().text("残金: ").yellow().bold().text(BaseUtils.priceString(shopInfo.getInt("money"))).text("円").build());

            SInventoryItem item = new SInventoryItem(icon.build());
            item.clickable(false);
            item.setEvent(e -> {
                if(onClick != null) onClick.accept(shopInfo.getString("shopId"));
            });

            if(searchQuery != null){
                addItem(defaultCategory, item);
            }else{
                addItem(shopInfo.getString("category"), item);
            }
        }
        //setItems(items);
    }

    public void afterRenderMenu() {
        super.afterRenderMenu();
//        if(player.hasPermission("man10shopv2.admin.agent") && !player.hasPermission("man10shopv2.admin.debug")){
//            SInventoryItem debug = new SInventoryItem(new SItemStack(Material.COMMAND_BLOCK).setDisplayName("§c§lデバッグ").build()).clickable(false);
//            debug.setEvent(e -> {
//                new AgentActionMenu(player, plugin).open(player);
//            });
//            setItem(47, debug);
//            renderInventory();
//        }
    }

}
