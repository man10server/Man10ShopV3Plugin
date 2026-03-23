# Man10ShopV3Plugin

Man10ShopV3 の Maven ビルド用リポジトリです。

## 前提

- Java 21
- Maven 3.8+
- 依存 jar を `libs/` に配置済み

このリポジトリでは以下を `provided` 依存として参照します。

- `com.shojabon:McUtils:2.0`
- `com.shojabon:SCommandRouter:1.0-SNAPSHOT`
- `com.shojabon:Man10Socket:1.0-SNAPSHOT`

## 依存 jar の追加方法

`libs/` に jar を置いた後、ローカル Maven リポジトリ (`~/.m2`) へ登録します。

```bash
mvn install:install-file \
  -Dfile=libs/McUtils-2.0.jar \
  -DgroupId=com.shojabon \
  -DartifactId=McUtils \
  -Dversion=2.0 \
  -Dpackaging=jar

mvn install:install-file \
  -Dfile=libs/SCommandRouter-1.0-SNAPSHOT.jar \
  -DgroupId=com.shojabon \
  -DartifactId=SCommandRouter \
  -Dversion=1.0-SNAPSHOT \
  -Dpackaging=jar

mvn install:install-file \
  -Dfile=libs/Man10Socket-1.0-SNAPSHOT.jar \
  -DgroupId=com.shojabon \
  -DartifactId=Man10Socket \
  -Dversion=1.0-SNAPSHOT \
  -Dpackaging=jar
```

`pom.xml` には以下依存が追加済みです。

```xml
<dependency>
    <groupId>com.shojabon</groupId>
    <artifactId>McUtils</artifactId>
    <version>2.0</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>com.shojabon</groupId>
    <artifactId>SCommandRouter</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>com.shojabon</groupId>
    <artifactId>Man10Socket</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

## ビルド方法

```bash
mvn clean package
```

成果物は `target/` に出力されます。

## 導入方法

1. 依存プラグインを `plugins/` に配置します。
- `SCommandRouter`
- `McUtils`
- `Man10Socket`

2. 本プラグイン jar を `plugins/` に配置します。
- このリポジトリでビルドした `target/` 配下の jar を配置

3. サーバーを一度起動し、`plugins/Man10Socket/config.yml` と `plugins/Man10ShopV3/config.yml` を生成します。

4. `plugins/Man10Socket/config.yml` を設定します。
- `listeningPort`: API サーバー側クライアントが接続する TCP ポート
- デフォルト値は `6789`
- API サーバーが別ホストにある場合は、このポートへ到達できるようにファイアウォールとセキュリティグループを開放してください

5. `plugins/Man10ShopV3/config.yml` を設定します。
- `serverName`: API サーバーへ渡すサーバー識別名。複数サーバーで使う場合は一意にする
- `api.endpoint`: ルーティング名の基準値。現在の実装では HTTP 通信には使わず、`/shop/...` のパス生成にだけ使う
- `api.key`: 旧 HTTP 実装の名残で、現在の `Man10Socket` 経由の実装では送信していません
- `pluginEnabled`: `true` のままで問題ありません
- `prefix`: ゲーム内メッセージの接頭辞

6. API サーバー側を `Man10Socket` に合わせて設定します。
- API サーバーは `Man10Socket` のソケットクライアントとして Minecraft サーバーの `listeningPort` へ接続します
- API サーバー側のクライアント名は `Man10ShopV3` にしてください
- 本プラグインは `Man10Socket.send("Man10ShopV3", payload, true)` で送っているため、名前が一致しないとリクエストが届きません
- リクエストの `path` は `/shop/...` 形式で送られます。現在使っている主なパスは以下です
- `/shop/list`
- `/shop/create`
- `/shop/info`
- `/shop/withdraw_buy_shop_money`
- `/shop/variable/set`
- `/shop/queue/add`
- レスポンスは最低でも `status` と `message` を返してください
- `shop/info` や一覧系では `data` も必要です。多くの処理が `status == "success"` と `data` を前提に動きます

7. 送受信データの合わせ方を確認します。
- リクエスト本体は概ね `type`, `path`, `data`, `replyId`, `target` を持つ JSON です
- `data.player` には `name`, `uuid`, `server`, `ipAddress` が入ります
- 看板参照系では `data.sign.world`, `x`, `y`, `z` を送ります
- キュー追加系では `shopId`, `key`, `data` を送ります
- API サーバー側は `replyId` に対する応答を返してください。応答が返らないとプラグイン側はタイムアウト扱いになります

8. サーバーを再起動し、起動ログにエラーがないことを確認します。
- コマンド `/mshop` が登録されていれば導入完了です。
- `Man10Socket` 側も起動しており、API サーバー接続後に通信エラーが出ないことを確認してください

9. `/mshop health` でヘルスチェックを実行します。
- 権限 `man10shopv3.health` が必要です
- `pluginEnabled`
- `serverName`
- `api.endpoint`
- `Man10Socket` プラグインの有効状態
- 接続中の socket client 数
- `Man10ShopV3` という名前の API サーバークライアントが接続しているか
- `API socket target: 未接続` と出る場合は、API サーバーが `Man10Socket` に接続できていないか、クライアント名が `Man10ShopV3` になっていません
- `基本状態は正常です` と出れば、少なくとも設定値と Socket 接続状態は問題ありません

## 注意事項

- Shop API サーバーは **PaperMC の起動が完了してから** 起動してください。PaperMC 起動前に Shop API サーバーを起動すると、Man10Socket のソケット接続が確立できず通信がタイムアウトします。
- `plugin.yml` では `SCommandRouter` と `Man10Socket` が必須依存 (`depend`) として宣言されています。
- `api.endpoint` のホスト名やポート番号へ直接 HTTP 送信しているわけではありません。`Man10Socket` を経由して API サーバーへ流しています。
- `api.endpoint` に `/api` のようなプレフィックスを入れても、現在の実装では `path` は `/shop/...` のまま送られます。
