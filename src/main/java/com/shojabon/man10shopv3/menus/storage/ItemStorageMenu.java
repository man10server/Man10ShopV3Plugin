package com.shojabon.man10shopv3.menus.storage;

import com.shojabon.man10shopv3.Man10ShopV3;
import com.shojabon.man10shopv3.dataClass.Man10Shop;
import com.shojabon.man10shopv3.menus.ShopMainMenu;
import com.shojabon.mcutils.Utils.SInventory.SInventory;
import com.shojabon.mcutils.Utils.SItemStack;
import com.shojabon.mcutils.Utils.SStringBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

public class ItemStorageMenu extends SInventory{
    
    Man10Shop shop;
    Man10ShopV3 plugin;
    Player player;
    int itemCount;
    boolean fillItem;

    public ItemStorageMenu(Player p, Man10Shop shop, int itemCount, Man10ShopV3 plugin){
        super(new SStringBuilder().darkGray().text(shop.getName() + "倉庫 ").green().text("上限").text(String.valueOf(shop.storageFunction.getStorageSize())).text("個").build(), 6, plugin);
        this.player = p;
        this.shop = shop;
        this.plugin = plugin;

        shop.updateData();

        this.itemCount = itemCount;
        ItemStack targetItemSingle = shop.targetItemFunction.getTargetItem().build().clone();
        int maxStackSize = targetItemSingle.getMaxStackSize();
        if(this.itemCount > maxStackSize*54){
            this.itemCount = maxStackSize*54;
        }
    }

    public void renderMenu(){
        if(itemCount == 0) return;
        int nextSlot = 0;

        ItemStack targetItemSingle = shop.targetItemFunction.getTargetItem().build().clone();
        int maxStackSize = targetItemSingle.getMaxStackSize();
        int stacks = this.itemCount/maxStackSize;
        if(stacks > 6*9) stacks = 6*9;
        SItemStack maxStackedItem = new SItemStack(targetItemSingle).setAmount(targetItemSingle.getMaxStackSize());
        for(int i = 0; i < stacks; i++){
            setItem(i, maxStackedItem.build());
            nextSlot++;
        }

        //add remaining non full stacked item
        if(stacks != 6*9){
            int remainingItemCount = this.itemCount - stacks*maxStackSize;
            setItem(nextSlot, new SItemStack(targetItemSingle.clone()).setAmount(remainingItemCount).build());
        }
        renderInventory();
    }

    public int countItems(boolean estimate){
        int result = 0;
        for(int i = 0; i < 6*9; i++){
            ItemStack item = activeInventory.getItem(i);
            if(item == null) continue;
            if(!isTargetItem(item)) {
                if(!estimate){
                    player.getInventory().addItem(item);
                }
                continue;
            }
            result += item.getAmount();
        }
        return result;
    }

    public void registerEvents(){
        setOnClickEvent(e -> {
            if(e.getAction() == InventoryAction.HOTBAR_SWAP){
                e.setCancelled(true);
                return;
            }
            if(e.getCurrentItem() == null) return;
            if(e.getClickedInventory() == null) return;
            SItemStack item = new SItemStack(e.getCurrentItem());
            if(!isTargetItem(e.getCurrentItem())){
                e.setCancelled(true);
                return;
            }
            //if bundle and right click
            if(item.getType() == Material.BUNDLE && e.getAction() == InventoryAction.PICKUP_HALF) {
                e.setCancelled(true);
                return;
            }
            //if item exceeds items storage size
            int selectedItemCount = new SItemStack(e.getCurrentItem()).getAmount();
            int diff = countItems(true) - itemCount;
            int estimatedNewStorageCount = diff + shop.storageFunction.getItemCount();
            if(estimatedNewStorageCount + selectedItemCount > shop.storageFunction.getStorageSize() && e.getClickedInventory().getType() != InventoryType.CHEST){
                player.sendMessage(Man10ShopV3.prefix + "§c§l倉庫のサイズ上限を越します");
                e.setCancelled(true);
                return;
            }
        });

        setAsyncOnForcedCloseEvent(e -> {
            int diff = countItems(false) - itemCount;

            JSONObject data = new JSONObject();
            data.put("amount", diff);
            shop.requestQueueTask(player, "storage.menu.close",data);
        });

        //reopen selector

        setOnCloseEvent(ee -> new ShopMainMenu(player, shop, plugin).open(player));
    }

    private boolean isTargetItem(ItemStack item){
        if(item == null) return false;
        return item.isSimilar(shop.targetItemFunction.getTargetTypeItem());
    }
}
