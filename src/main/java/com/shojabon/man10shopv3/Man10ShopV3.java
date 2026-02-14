package com.shojabon.man10shopv3;

import com.shojabon.man10shopv3.commands.Man10ShopV3Command;
import com.shojabon.man10shopv3.dataClass.QueueRequestObject;
import com.shojabon.man10shopv3.listeners.SignListeners;
import com.shojabon.man10socket.Man10Socket;
import com.shojabon.mcutils.Utils.VaultAPI;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.shojabon.man10shopv3.Man10ShopV3API.getPlayerJSON;
import static com.shojabon.man10shopv3.Man10ShopV3API.httpRequest;

public final class Man10ShopV3 extends JavaPlugin implements @NotNull Listener {
    public static String prefix;
    public static FileConfiguration config;
    public static ExecutorService threadPool = Executors.newCachedThreadPool();
    public static VaultAPI vault;

    public static Man10ShopV3API api = null;
    public static ArrayList<UUID> ved = new ArrayList<>();
    public static Queue<QueueRequestObject> transactionRequestQueue = new ConcurrentLinkedQueue<>();
    public static ConcurrentHashMap<UUID, Boolean> transactionLock = new ConcurrentHashMap<>();
    public static boolean pluginEnabled = true;

    private int resolveSocketPort() {
        int configPort = this.getConfig().getInt("api.socketPort", -1);
        if (configPort > 0) {
            return configPort;
        }

        String endpoint = this.getConfig().getString("api.endpoint", "");
        try {
            java.net.URI uri = new java.net.URI(endpoint);
            if (uri.getPort() > 0) {
                return uri.getPort();
            }
        } catch (Exception ignored) {
        }
        return 6789;
    }

    public void locallyQueuedRequestProcessThread(){
        while (pluginEnabled){
            try{
                if(transactionRequestQueue.isEmpty()){
                    Thread.sleep(100);
                    continue;
                }
                QueueRequestObject request = transactionRequestQueue.poll();
                if(request == null) continue;
                transactionLock.remove(request.player.getUniqueId());
                if(!request.player.isOnline()) continue;

                Map<String, Object> payload = new HashMap<>();
                payload.put("shopId", request.shopId);
                if(request.player != null){
                    payload.put("player", getPlayerJSON(request.player));
                }
                payload.put("key", request.key);
                payload.put("data", request.data);
                httpRequest(this.getConfig().getString("api.endpoint") + "/shop/queue/add", "POST", new JSONObject(payload));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        config = getConfig();
        prefix = getConfig().getString("prefix");
        api = new Man10ShopV3API(this);
        Man10Socket.initialize(this, resolveSocketPort());

        vault = new VaultAPI();

        getServer().getPluginManager().registerEvents(new SignListeners(this), this);
        getServer().getPluginManager().registerEvents(this, this);
        new Man10ShopV3Command(this);

        Bukkit.getServer().getScheduler().runTaskAsynchronously(this, this::locallyQueuedRequestProcessThread);
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        for(UUID uuid: ved){
            Player vp = Bukkit.getPlayer(uuid);
            if(vp == null){
                ved.remove(uuid);
                continue;
            }
            e.getPlayer().hidePlayer(this, vp);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e){
        if(ved.contains(e.getPlayer().getUniqueId())) e.getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
        ved.remove(e.getPlayer().getUniqueId());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        pluginEnabled = false;
        Man10Socket.shutdown();
    }
}
