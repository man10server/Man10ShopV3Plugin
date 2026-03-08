package com.shojabon.man10shopv3.commands;

import com.shojabon.man10shopv3.Man10ShopV3;
import com.shojabon.man10shopv3.commands.subCommands.*;
import com.shojabon.man10shopv3.commands.subCommands.internals.*;
import com.shojabon.scommandrouter.SCommandRouter.SCommandArgumentType;
import com.shojabon.scommandrouter.SCommandRouter.SCommandObject;
import com.shojabon.scommandrouter.SCommandRouter.SCommandRouter;
import org.bukkit.Bukkit;


public class Man10ShopV3Command extends SCommandRouter {

    Man10ShopV3 plugin;

    public Man10ShopV3Command(Man10ShopV3 plugin){
        super(plugin, "mshop");
        this.plugin = plugin;
        registerCommands();
        registerEvents();
        pluginPrefix = Man10ShopV3.prefix;
    }

    public void registerEvents(){
        setNoPermissionEvent(e -> e.sender.sendMessage(Man10ShopV3.prefix + "§c§lあなたは権限がありません"));
        setOnNoCommandFoundEvent(e -> e.sender.sendMessage(Man10ShopV3.prefix + "§c§lコマンドが存在しません"));
    }

    public void registerCommands(){
        //shops command
        addCommand(
                new SCommandObject()
                        .prefix("test")
                        .permission("man10shopv3.test")
                        .explanation("テスト")
                        .executor(new TestCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("health")
                        .permission("man10shopv3.health")
                        .explanation("プラグインとSocket接続の状態を確認")
                        .executor(new HealthCheckCommand())
        );

        addCommand(
                new SCommandObject()
                        .prefix("withdrawBuyShop")
                        .permission("man10shopv3.withdrawBuyShopMoney")
                        .explanation("販売系ショップのお金を引き出す")
                        .executor(new WithdrawBuyShopMoneyCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("shops")

                        .permission("man10shopv3.shops")
                        .explanation("編集可能なショップを開く")
                        .executor(new ShopsCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("shops")
                        .argument("検索キーワード")

                        .permission("man10shopv3.shops")
                        .explanation("編集可能なショップを開く")
                        .executor(new ShopsCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("adminShops")
                        .permission("man10shopv3.admin.shops")
                        .explanation("アドミンショップをショップを開く")
                        .executor(new AdminShopsCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("adminShops")
                        .argument("検索キーワード")
                        .permission("man10shopv3.admin.shops")
                        .explanation("アドミンショップをショップを開く")
                        .executor(new AdminShopsCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("toggleWorld")
                        .permission("man10shopv3.toggleWorld")
                        .explanation("看板が機能するワールド一覧を表示する")
                        .executor(new ToggleWorldCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("toggleWorld")
                        .argument("ワールド名", SCommandArgumentType.WORLD)

                        .permission("man10shopv3.toggleWorld")
                        .explanation("看板が機能するワールドの有効/無効を設定")
                        .executor(new ToggleWorldCommand(plugin))
        );


        addCommand(
                new SCommandObject()
                        .prefix("create")
                        .argument("ショップ名")

                        .permission("man10shopv3.shop.create")
                        .explanation("ショップを作成")
                        .executor(new CreateShopCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("createAdmin")
                        .argument("ショップ名")

                        .permission("man10shopv3.admin.shop.create")
                        .explanation("管理者ショップを作成")
                        .executor(new CreateAdminShopCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("reload")

                        .permission("man10shopv3.reload")
                        .explanation("プラグインをリロードする")
                        .explanation("")
                        .explanation("設定を変更したときに使用する")
                        .explanation("コマンドを使用するとサーバー起動時状態に戻る")
                        .executor(new ReloadConfigCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("togglePlugin")

                        .permission("man10shopv3.togglePlugin")
                        .explanation("プラグインが有効かどうかを表示する")
                        .executor(new TogglePluginCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("togglePlugin")
                        .argument("有効/無効", SCommandArgumentType.BOOLEAN)
                        .permission("man10shopv3.togglePlugin")
                        .explanation("プラグインの有効/無効を設定")
                        .executor(new TogglePluginCommand(plugin))
        );

        // internals
        addCommand(
                new SCommandObject()
                        .prefix("message")
                        .argument("uuid")
                        .infinity()
                        .permission("man10shopv3.message").explanation("send message to player").
                        executor(new MessageCommand(plugin))
        );
        addCommand(
                new SCommandObject()
                        .prefix("itemGive")
                        .argument("uuid")
                        .argument("itemBase64")
                        .argument("amount")
                        .permission("man10shopv3.item.give").explanation("アイテムを付与する(内部用)").
                        executor(new ItemGiveCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("itemTake")
                        .argument("uuid")
                        .argument("itemBase64")
                        .argument("amount")
                        .permission("man10shopv3.item.take").explanation("アイテムを取る(内部用)").
                        executor(new ItemTakeCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("itemCheck")
                        .argument("uuid")
                        .argument("itemBase64")
                        .argument("amount")
                        .permission("man10shopv3.item.check").explanation("アイテムを取る(内部用)").
                        executor(new ItemCheckCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("inventoryHasSpace")
                        .argument("uuid")
                        .permission("man10shopv3.inventory.space").explanation("イベントリのスペースがあるかを確認(内部用)").
                        executor(new InventoryHasSpace(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("moneyTake")
                        .argument("uuid")
                        .argument("amount")
                        .permission("man10shopv3.money.take").explanation("お金を取る(内部用)").
                        executor(new MoneyTakeCommand(plugin))
        );
        addCommand(
                new SCommandObject()
                        .prefix("moneyGet")
                        .argument("uuid")
                        .permission("man10shopv3.money.get")
                        .explanation("お金を見る(内部用)").
                        executor(new MoneyGetCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("moneyGive")
                        .argument("uuid")
                        .argument("amount")
                        .permission("man10shopv3.money.give").explanation("お金をあげる(内部用)").
                        executor(new MoneyGiveCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("signUpdate")
                        .argument("shopId")
                        .permission("man10shopv3.sign.update")
                        .explanation("看板アップデート(内部用)").
                        executor(new SignUpdateCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("storageOpen")
                        .argument("shopId")
                        .argument("uuid")
                        .argument("displayAmount")
                        .permission("man10shopv3.storage.menu").explanation("倉庫メニュー表示(内部用)").
                        executor(new StorageMenuOpenCommand(plugin))
        );

        addCommand(
                new SCommandObject()
                        .prefix("lootBoxPlay")
                        .argument("shopId")
                        .argument("uuid")
                        .argument("logId")
                        .permission("man10shopv3.lootbox.play").explanation("ガチャをプレーする(内部用)").
                        executor(new LootBoxPlayCommand(plugin))
        );
    }

}
