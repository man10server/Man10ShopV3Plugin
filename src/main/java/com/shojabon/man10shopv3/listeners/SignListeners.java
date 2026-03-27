package com.shojabon.man10shopv3.listeners;

import ToolMenu.ConfirmationMenu;
import com.shojabon.man10shopv3.Man10ShopV3;
import com.shojabon.man10shopv3.dataClass.Man10Shop;
import com.shojabon.man10shopv3.menus.AdminShopSelectorMenu;
import com.shojabon.man10shopv3.menus.EditableShopSelectorMenu;
import com.shojabon.mcutils.Utils.BaseUtils;
import com.shojabon.mcutils.Utils.SInventory.SInventory;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SignListeners implements @NotNull Listener {

    Man10ShopV3 plugin;

    public SignListeners(Man10ShopV3 plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignUpdate(SignChangeEvent e){
        if(e.getLine(0) == null) return;
        if(!Objects.requireNonNull(e.getLine(0)).equalsIgnoreCase("man10shop") && !Objects.requireNonNull(e.getLine(0)).equalsIgnoreCase("man10adminshop")) return;

        //permission to use
        if(!e.getPlayer().hasPermission("man10shopv3.sign.create")){
            e.getPlayer().sendMessage(Man10ShopV3.prefix + "§c§lあなたには権限がありません");
            return;
        }

        //allowed world
        if(!Man10ShopV3.config.getStringList("enabledWorlds").contains(e.getBlock().getWorld().getName())) return;

        //if plugin disabled
        if(!Man10ShopV3.config.getBoolean("pluginEnabled")){
            e.getPlayer().sendMessage(Man10ShopV3.prefix + "§c§l現在このプラグインは停止中です");
            return;
        }

        if(Objects.requireNonNull(e.getLine(0)).equalsIgnoreCase("man10shop")){
            EditableShopSelectorMenu menu = new EditableShopSelectorMenu(e.getPlayer(),"その他", plugin);

            int signPrice = Man10ShopV3.config.getInt("sign.price");
            UUID uuid = e.getPlayer().getUniqueId();

            menu.setOnClick(shopId -> {
                Man10ShopV3.threadPool.submit(()->{
                    Man10Shop shop = Man10ShopV3.api.getShop(shopId, e.getPlayer());
                    if(shop == null){
                        e.getPlayer().sendMessage(Man10ShopV3.prefix + "§c§lショップが存在しません");
                        return;
                    }

                    if(Man10ShopV3.config.getInt("sign.price") != 0){
                        ConfirmationMenu confirmationMenu = new ConfirmationMenu(BaseUtils.priceString(signPrice)+ "円支払いますか？", plugin);

                        //confirm purchase
                        confirmationMenu.setOnConfirm(ee -> {
                            if(Man10ShopV3.vault.getBalance(uuid) < signPrice){
                                e.getPlayer().sendMessage(Man10ShopV3.prefix + "§c§l現金が不足しています");
                                confirmationMenu.close(e.getPlayer());
                            }
                            Man10ShopV3.vault.withdraw(uuid, signPrice );
                            buySign(shop, e);
                        });

                        confirmationMenu.setOnCancel(ee -> confirmationMenu.close(e.getPlayer()));

                        confirmationMenu.open(e.getPlayer());
                        return;
                    }
                    buySign(shop, e);
                });

            });
            menu.open(e.getPlayer());
        }else{

            //permission to use
            if(!e.getPlayer().hasPermission("man10shopv3.admin.sign.create")){
                e.getPlayer().sendMessage(Man10ShopV3.prefix + "§c§lあなたには権限がありません");
                return;
            }
            AdminShopSelectorMenu menu = new AdminShopSelectorMenu(e.getPlayer(), "その他", plugin);

            int signPrice = Man10ShopV3.config.getInt("sign.price");
            UUID uuid = e.getPlayer().getUniqueId();

            menu.setOnClick(shopId -> {
                Man10ShopV3.threadPool.submit(()->{
                    Man10Shop shop = Man10ShopV3.api.getShop(shopId, e.getPlayer());
                    if(shop == null){
                        e.getPlayer().sendMessage(Man10ShopV3.prefix + "§c§lショップが存在しません");
                        return;
                    }
                    if(Man10ShopV3.config.getInt("sign.price") != 0){
                        ConfirmationMenu confirmationMenu = new ConfirmationMenu(BaseUtils.priceString(signPrice)+ "円支払いますか？", plugin);

                        //confirm purchase
                        confirmationMenu.setOnConfirm(ee -> {
                            if(Man10ShopV3.vault.getBalance(uuid) < signPrice){
                                e.getPlayer().sendMessage(Man10ShopV3.prefix + "§c§l現金が不足しています");
                                confirmationMenu.close(e.getPlayer());
                            }
                            Man10ShopV3.vault.withdraw(uuid, signPrice );
                            buySign(shop, e);
                        });

                        confirmationMenu.setOnCancel(ee -> confirmationMenu.close(e.getPlayer()));

                        confirmationMenu.open(e.getPlayer());
                        return;
                    }
                    buySign(shop, e);
                });
            });
            menu.open(e.getPlayer());
        }
    }

    @EventHandler
    public void onSignDestroy(BlockBreakEvent e){
        if(!(e.getBlock().getState() instanceof Sign)){
            return;
        }
//        if(!e.getBlock().getState().hasMetadata("isMan10ShopV3Sign")){
//            return;
//        }
        Man10Shop shop = Man10ShopV3.api.getShopFromSign(null, e.getBlock().getLocation());
        if(shop == null) {
            return;
        }
        if(!shop.permissionFunction.hasPermission(e.getPlayer().getUniqueId(), "MODERATOR") && !e.getPlayer().hasPermission("man10shopv3.sign.break.bypass")){
            e.getPlayer().sendMessage(Man10ShopV3.prefix + "§c§l看板を破壊する権限を持っていません");
            e.setCancelled(true);
            return;
        }

//        e.getBlock().getState().removeMetadata("isMan10ShopV3Sign", plugin);
        SInventory.threadPool.execute(()-> {
            JSONObject result = shop.deleteSign(null, e.getBlock().getLocation());
            if(!result.getString("status").equals("success")){
                e.getPlayer().sendMessage(Man10ShopV3.prefix + "§c§l" + result.getString("message"));
            }
        });
    }

    private void breakBlockNaturally(Block block){
        Bukkit.getScheduler().runTask(plugin, ()->{
            block.breakNaturally(true);
        });
    }

    ConcurrentHashMap<UUID, Boolean> requestProcessed = new ConcurrentHashMap<>();

    @EventHandler
    public void onSignInteract(PlayerInteractEvent e){
        if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if(e.getClickedBlock() == null) return;
        if(!(e.getClickedBlock().getState() instanceof Sign)) return;
        if(!Man10ShopV3.config.getBoolean("pluginEnabled")){
//            e.getPlayer().sendMessage(Man10ShopV3.prefix + "§c§l現在このプラグインは停止中です");
            return;
        }
        //allowed world
        if(!Man10ShopV3.config.getStringList("enabledWorlds").contains(e.getClickedBlock().getWorld().getName())) return;

//        if(!e.getClickedBlock().getState().hasMetadata("isMan10ShopV3Sign")) return;
        Man10ShopV3.threadPool.execute(()->{
            if(requestProcessed.containsKey(e.getPlayer().getUniqueId())){
                return;
            }
            requestProcessed.put(e.getPlayer().getUniqueId(), true);
            try{
                Man10Shop shop = Man10ShopV3.api.getShopFromSign(e.getPlayer(), e.getClickedBlock().getLocation());
                if(shop == null){
                    requestProcessed.remove(e.getPlayer().getUniqueId());
                    return;
                }else{
                    shop.openMenu(e.getPlayer());
                    requestProcessed.remove(e.getPlayer().getUniqueId());
                }
            }catch (Exception ex){
                ex.printStackTrace();
                requestProcessed.remove(e.getPlayer().getUniqueId());
            }
        });
    }

    @EventHandler
    public void onSignBreak(BlockPhysicsEvent e){
        if(e.isCancelled())return;
        Block source = e.getSourceBlock();
        Block block = e.getBlock();
        if(source.getType() != Material.AIR){
            return;
        }
        if(!(block.getState() instanceof Sign)){
            return;
        }
        if(block.getLocation().equals(e.getSourceBlock().getLocation())){
            return;
        }
        if(block.getState().getBlockData() instanceof org.bukkit.block.data.type.WallSign){
            org.bukkit.block.data.type.WallSign signData = (org.bukkit.block.data.type.WallSign) block.getState().getBlockData();
            if(!block.getRelative(signData.getFacing().getOppositeFace()).equals(source)) return;
        }else if(block.getState().getBlockData() instanceof org.bukkit.block.data.type.Sign){
            if(!block.getRelative(BlockFace.DOWN).equals(source)) return;
        }


//        if(block.getState().hasMetadata("isMan10ShopV3Sign")) block.getState().removeMetadata("isMan10ShopV3Sign", plugin);


        Man10ShopV3.threadPool.execute(()->{
            Man10Shop shop = Man10ShopV3.api.getShopFromSign(null, block.getLocation());
            if(shop == null) return;
            JSONObject result = shop.deleteSign(null, block.getLocation());
        });
    }
//
    public void buySign(Man10Shop shop, SignChangeEvent e){
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Sign sign = ((Sign) e.getBlock().getState());

            ArrayList<String> lineData = shop.getSignInfo();

            sign.setLine(3, formatSignString(sign.getLine(2), shop));
            sign.setLine(2, formatSignString(sign.getLine(1), shop));

            for(int i = 0; i < lineData.size(); i++){
                if(lineData.get(i) == null || lineData.get(i).equalsIgnoreCase("")) continue;
                sign.setLine(i, lineData.get(i));
            }

            sign.update(true);

            Location l = e.getBlock().getLocation();
            Man10ShopV3.threadPool.execute(() -> {
                JSONObject result = shop.createSign(e.getPlayer(), l);
                if(!result.getString("status").equals("success")){
                    e.getPlayer().sendMessage(Man10ShopV3.prefix + "§c§l" + result.getString("message"));
                    return;
                }

                // set block meta
//                Bukkit.getServer().getScheduler().runTask(plugin, () ->{
//                    if(!(e.getBlock().getState() instanceof Sign)){
//                        return;
//                    }
//                    e.getBlock().setMetadata("isMan10ShopV3Sign", new FixedMetadataValue(plugin, true));
//                });


            });
            e.getPlayer().closeInventory();
        });
    }

    public String formatSignString(String original, Man10Shop shop){
        return original.replace("&", "§");
    }



}
