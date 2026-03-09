package com.shojabon.man10shopv3.commands.subCommands;

import com.shojabon.man10shopv3.Man10ShopV3;
import com.shojabon.man10socket.ClientHandler;
import com.shojabon.man10socket.Man10Socket;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class HealthCheckCommand implements CommandExecutor {

    private static final String SOCKET_TARGET_NAME = "Man10ShopV3";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Plugin man10SocketPlugin = Bukkit.getPluginManager().getPlugin("Man10Socket");
        boolean man10SocketEnabled = man10SocketPlugin != null && man10SocketPlugin.isEnabled();

        String serverName = Man10ShopV3.config.getString("serverName", "");
        String apiEndpoint = Man10ShopV3.config.getString("api.endpoint", "");
        boolean pluginEnabled = Man10ShopV3.config.getBoolean("pluginEnabled");
        int totalClients = Man10Socket.clients.size();
        int targetClients = countTargetClients();

        sender.sendMessage(Man10ShopV3.prefix + "§e§lHealth Check");
        sendStatus(sender, "pluginEnabled", pluginEnabled, pluginEnabled ? "有効" : "無効");
        sendStatus(sender, "serverName", !serverName.isEmpty(), serverName.isEmpty() ? "未設定" : serverName);
        sendStatus(sender, "api.endpoint", !apiEndpoint.isEmpty(), apiEndpoint.isEmpty() ? "未設定" : apiEndpoint);
        sendStatus(sender, "Man10Socket plugin", man10SocketEnabled, man10SocketEnabled ? "有効" : "無効");
        sendStatus(sender, "socket clients", totalClients > 0, String.valueOf(totalClients));
        sendStatus(sender, "API socket target", targetClients > 0, targetClients > 0 ? "接続中(" + targetClients + ")" : "未接続");

        if (!man10SocketEnabled || targetClients == 0) {
            sender.sendMessage(Man10ShopV3.prefix + "§c§lAPIサーバーとのSocket疎通が確立していません");
        } else if (!pluginEnabled || serverName.isEmpty() || apiEndpoint.isEmpty()) {
            sender.sendMessage(Man10ShopV3.prefix + "§e§l設定を見直してください");
        } else {
            sender.sendMessage(Man10ShopV3.prefix + "§a§l基本状態は正常です");
        }
        return true;
    }

    private void sendStatus(CommandSender sender, String label, boolean ok, String detail) {
        String color = ok ? "§a" : "§c";
        sender.sendMessage(Man10ShopV3.prefix + color + label + ": §f" + detail);
    }

    private int countTargetClients() {
        int count = 0;
        for (ClientHandler client : Man10Socket.clients.values()) {
            if (client == null || client.setNameFunction == null || client.setNameFunction.name == null) {
                continue;
            }
            if (client.setNameFunction.name.equalsIgnoreCase(SOCKET_TARGET_NAME)) {
                count++;
            }
        }
        return count;
    }
}
