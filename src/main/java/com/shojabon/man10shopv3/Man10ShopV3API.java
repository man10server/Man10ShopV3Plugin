package com.shojabon.man10shopv3;

import com.shojabon.man10shopv3.dataClass.Man10Shop;
import com.shojabon.man10socket.Man10Socket;
import com.shojabon.mcutils.Utils.SItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Man10ShopV3API {

    Man10ShopV3 plugin;

    HashMap<String, Man10Shop> shopObjectCache = new HashMap<>();

    public Man10ShopV3API(Man10ShopV3 plugin){
        this.plugin = plugin;
    }

    // global methods
    public static JSONObject httpRequest(String endpoint, String method, JSONObject jsonInput) {
        JSONObject payload = new JSONObject();
        String pathHeading = Man10ShopV3.config.getString("api.endpoint");
        payload.put("target", "Man10ShopV3");
        payload.put("path", endpoint.replace(pathHeading, ""));
        payload.put("type", "request");
        payload.put("data", jsonInput);
        JSONObject a = Man10Socket.send(payload, true);
        // Keep "message" for existing callers and also expose it as "data" when absent.
        if(a.has("message") && !a.has("data")){
            a.put("data", a.get("message"));
        }
        return a;
//
//        try {
//            URL url = new URL(endpoint);
//            HttpURLConnection con = (HttpURLConnection) url.openConnection();
//
//            // optional default is GET
//            con.setRequestMethod(method);
//            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
//            con.setRequestProperty("Accept", "application/json; charset=UTF-8");
//            con.setRequestProperty("x-api-key", Man10ShopV3.config.getString("api.key"));
//            con.setConnectTimeout(5000);
//            con.setReadTimeout(5000);
//            con.setDoOutput(true);
//
//            try(OutputStream os = con.getOutputStream()) {
//                byte[] input = jsonInput.toString().getBytes(StandardCharsets.UTF_8);
//                os.write(input, 0, input.length);
//            }
//            int responseCode = con.getResponseCode();
//            BufferedReader in = null;
//            if(responseCode == 200){
//                in = new BufferedReader(
//                        new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
//            }else{
//                in = new BufferedReader(
//                        new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8));
//            }
//
//            String inputLine;
//            StringBuilder response = new StringBuilder();
//
//            while ((inputLine = in.readLine()) != null) {
//                response.append(inputLine);
//            }
//            in.close();
//
//            //print result
//            return new JSONObject(response.toString());
//        } catch (Exception e) {
//            e.printStackTrace();
//            JSONObject failResponse = new JSONObject();
//            failResponse.put("status", "endpoint_error");
//            failResponse.put("message", "エンドポイントに接続することがで来ませんでした");
//            return failResponse;
//        }
    }

    public static void warnMessage(Player p, String message){
        p.sendMessage(Man10ShopV3.prefix + "§c§l" + message);
    }

    public static void successMessage(Player p, String message){
        p.sendMessage(Man10ShopV3.prefix + "§a§l" + message);
    }

    public static JSONObject getPlayerJSON(Player p){
        Map<String, Object> result = new HashMap<>();
        result.put("name", p.getName());
        result.put("uuid", p.getUniqueId().toString());
        result.put("server", Man10ShopV3.config.getString("serverName"));
        if(p.getAddress() != null) result.put("ipAddress", p.getAddress().getAddress().getHostAddress());
        return new JSONObject(result);
    }

    public JSONObject getPlayerShops(Player p){
        return getPlayerShops(p, null);
    }

    public JSONObject getPlayerShops(Player p, String searchQuery){
        JSONObject payload = new JSONObject();
        payload.put("player", getPlayerJSON(p));
        if(searchQuery != null) payload.put("searchQuery", searchQuery);
        return httpRequest(this.plugin.getConfig().getString("api.endpoint") + "/shop/list", "POST", payload);
    }

    public JSONObject createShop(Player p,String name, boolean admin){
        JSONObject payload = new JSONObject();
        payload.put("player", getPlayerJSON(p));
        payload.put("admin", admin);
        payload.put("name", name);
        return httpRequest(this.plugin.getConfig().getString("api.endpoint") + "/shop/create", "POST", payload);
    }

    public JSONObject getAdminShops(Player p){
        return getAdminShops(p, null);
    }
    public JSONObject getAdminShops(Player p, String searchQuery){
        Map<String, Object> payload = new HashMap<>();
        payload.put("player", getPlayerJSON(p));
        payload.put("admin", true);
        if(searchQuery != null) payload.put("searchQuery", searchQuery);
        JSONObject result = httpRequest(this.plugin.getConfig().getString("api.endpoint") + "/shop/list", "POST", new JSONObject(payload));
        return result;
    }

    public JSONObject withdrawBuyShopMoney(Player p){
        JSONObject payload = new JSONObject();
        payload.put("player", getPlayerJSON(p));
        return httpRequest(this.plugin.getConfig().getString("api.endpoint") + "/shop/withdraw_buy_shop_money", "POST", payload);
    }

    public Man10Shop getShopFromSign(Player p, Location loc){
        JSONObject data = new JSONObject();
        data.put("server", Man10ShopV3.config.getString("serverName"));
        data.put("world", loc.getWorld().getName());
        data.put("x", loc.getBlockX());
        data.put("y", loc.getBlockY());
        data.put("z", loc.getBlockZ());

        JSONObject payload = new JSONObject();
        if(p != null){
            payload.put("player", getPlayerJSON(p));
        }
        payload.put("sign", data);
        JSONObject result = httpRequest(this.plugin.getConfig().getString("api.endpoint") + "/shop/info", "POST", payload);
        if(!result.getString("status").equals("success")){
//            if(p != null) p.sendMessage(Man10ShopV3.prefix + "§c§l" + result.getString("message"));
            return null;
        }
        return new Man10Shop(result.getJSONObject("data"));
    }

    public static JSONObject itemStackToJSON(ItemStack item){
        SItemStack sItemStack = new SItemStack(item);
        JSONObject itemData = new JSONObject();

        itemData.put("typeBase64", sItemStack.getItemTypeBase64(true));
        itemData.put("typeMd5", sItemStack.getItemTypeMD5(true));
        itemData.put("displayName", sItemStack.getDisplayName());
        itemData.put("amount", sItemStack.getAmount());
        itemData.put("lore", sItemStack.getLore());
        itemData.put("material", sItemStack.getType().name());
        itemData.put("customModelData", sItemStack.getCustomModelData());
        return itemData;
    }


    public static ItemStack JSONToItemStack(JSONObject data){
        SItemStack sItemStack = SItemStack.fromBase64(data.getString("typeBase64"));
        sItemStack.setAmount(data.getInt("amount"));
        return sItemStack.build();
    }

    public JSONObject getShopInformation(String shopId, Player requestingPlayer){
        Map<String, Object> payload = new HashMap<>();
        payload.put("shopId", shopId);
        if(requestingPlayer != null){
            payload.put("player", getPlayerJSON(requestingPlayer));
        }
        JSONObject result = httpRequest(this.plugin.getConfig().getString("api.endpoint") + "/shop/info", "POST", new JSONObject(payload));
        if(result == null || !result.getString("status").equals("success")) return null;
        return result;
    }
    public Man10Shop getShop(String shopId, Player requestingPlayer){
//        if(shopObjectCache.containsKey(shopId)){
//            Man10Shop shop =  shopObjectCache.get(shopId);
//            shop.updateData();
//            return shop;
//        }
        JSONObject shopInformationRequest = getShopInformation(shopId, requestingPlayer);
        if(shopInformationRequest == null) return null;
        shopObjectCache.put(shopId, new Man10Shop(shopInformationRequest.getJSONObject("data")));
        return shopObjectCache.get(shopId);
    }

}
