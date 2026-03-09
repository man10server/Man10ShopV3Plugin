# Man10ShopV3 導入手順書

Man10ShopV3 は Minecraft サーバー上のショップ機能を提供するシステムです。
Minecraft プラグイン（Man10ShopV3Plugin）と API サーバー（Man10ShopV3）の2つのコンポーネントで構成されます。

## システム構成

```
┌─────────────────────┐     Man10Socket (TCP:6789)     ┌──────────────────────┐
│  Minecraft Server   │◄──────────────────────────────►│  Man10ShopV3 API     │
│  (Paper 1.21.8)     │                                │  (FastAPI, port 8000)│
│                     │                                │                      │
│  プラグイン:         │                                │  接続先:              │
│  - Man10ShopV3      │                                │  - MongoDB (27017)   │
│  - Man10Socket      │                                │  - Minecraft (6789)  │
│  - McUtils          │                                └──────────────────────┘
│  - SCommandRouter   │                                          │
└─────────────────────┘                                          │
                                                       ┌──────────────────────┐
                                                       │  MongoDB 7           │
                                                       │  (port 27017)        │
                                                       └──────────────────────┘
```

## 前提条件

- Docker / Docker Compose がインストール済み
- `man10shop:local` Docker イメージがビルド済み

## 手順

### 1. プラグインの配置

以下の jar ファイルを `plugins/` ディレクトリに配置します。

| ファイル | 役割 |
|---------|------|
| `Man10ShopV3-1.0-SNAPSHOT.jar` | ショップ本体プラグイン |
| `Man10Socket-1.0-SNAPSHOT.jar` | API サーバーとの TCP 通信プラグイン |
| `McUtils-2.0.jar` | ユーティリティライブラリ |
| `SCommandRouter-1.0-SNAPSHOT.jar` | コマンド処理ライブラリ |

### 2. Minecraft サーバーを一度起動して設定ファイルを生成

```bash
docker compose up -d minecraft
```

初回起動時に以下のファイルが自動生成されます。

- `plugins/Man10Socket/config.yml`
- `plugins/Man10ShopV3/config.yml`

生成されたら一度停止しても構いません。

### 3. Man10Socket プラグインの設定

`plugins/Man10Socket/config.yml` を編集します。

```yaml
listeningPort: 6789
```

- `listeningPort`: API サーバーが接続してくる TCP ポート（デフォルト: 6789）

### 4. Man10ShopV3 プラグインの設定

`plugins/Man10ShopV3/config.yml` を編集します。

```yaml
pluginEnabled: true
serverName: "man10"
api:
  endpoint: "http://man10shopv3:8000"
  key: "test"
prefix: "§6[§eMan10Shop§dV3§6]"
```

| 項目 | 説明 |
|-----|------|
| `serverName` | API サーバーへ渡すサーバー識別名。複数サーバーの場合は一意にする |
| `api.endpoint` | パス生成の基準値（HTTP 直接通信には使わない） |
| `api.key` | 旧 HTTP 実装の名残。現在の Man10Socket 経由では未使用 |
| `pluginEnabled` | `true` のままで OK |
| `prefix` | ゲーム内メッセージの接頭辞 |

### 5. API サーバーの設定

`man10shop/config/config.json` を作成・編集します。

```json
{
  "hostPort": 8000,
  "mongodbConnectionString": "mongodb://mongodb:27017",
  "communicationMode": "socket",
  "man10socket": {
    "hosts": [
      {
        "name": "Man10ShopV3",
        "host": "minecraft",
        "port": 6789
      }
    ]
  },
  "queue": {
    "size": 8,
    "rateLimit": 0
  },
  "batching": {
    "setVariableBatchSeconds": 1
  },
  "api": {
    "endpoint": "https://{endpoint}",
    "key": "test"
  },
  "defaultVariables": {
    "storage": {
      "storageSizeMax": 3456,
      "storageSlotPrice": 100
    }
  }
}
```

**重要**: `man10socket.hosts[].name` は必ず `"Man10ShopV3"` にしてください。
プラグイン側は `Man10Socket.send("Man10ShopV3", payload, true)` で送信するため、この名前が一致しないとリクエストが届きません。

| 項目 | 説明 |
|-----|------|
| `hostPort` | API サーバーの HTTP ポート |
| `mongodbConnectionString` | MongoDB 接続文字列。Docker 内では `mongodb://mongodb:27017` |
| `communicationMode` | `"socket"` を指定 |
| `man10socket.hosts[].name` | **`Man10ShopV3`** 固定（プラグインの送信先名と一致させる） |
| `man10socket.hosts[].host` | Minecraft サーバーのホスト名。Docker Compose のサービス名 `minecraft` を指定 |
| `man10socket.hosts[].port` | Man10Socket のリスニングポート（デフォルト: 6789） |

### 6. Docker Compose の設定

`docker-compose.yml` に以下のサービスを追加します。

```yaml
services:
  minecraft:
    image: itzg/minecraft-server:java21-graalvm
    environment:
      MEMORYSIZE: "2G"
      TYPE: "PAPER"
      EULA: "TRUE"
      VERSION: "1.21.8"
      RCON_PASSWORD: "password"
    volumes:
      - ../Paper_1_21_8:/data:rw
    ports:
      - 25565:25565/tcp

  man10shopv3:
    image: man10shop:local
    volumes:
      - ./man10shop/config:/man10shopv3/config:ro
    ports:
      - "8000:8000"
    depends_on:
      mongodb:
        condition: service_healthy

  mongodb:
    image: mongo:7
    volumes:
      - mongodb_data:/data/db
    healthcheck:
      test: ["CMD-SHELL", "echo 'db.runCommand({ ping: 1 }).ok' | mongosh --quiet mongodb://localhost:27017/test | grep 1"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 20s

volumes:
  mongodb_data:
```

### 7. 全サービスの起動

```bash
docker compose up -d
```

> **注意: サービスの起動順序について**
>
> API サーバー（man10shopv3）は起動時に Minecraft サーバーの Man10Socket（TCP:6789）へ接続を試みます。
> Minecraft サーバーが先に起動していないと接続に失敗します。
> API サーバーにはリトライ処理があるため `docker compose up -d` で同時起動すれば通常は問題ありませんが、
> **Minecraft サーバーを再起動した場合は API サーバーも再起動が必要**です。
>
> ```bash
> docker compose restart minecraft && sleep 30 && docker compose restart man10shopv3
> ```

### 8. 動作確認

1. Minecraft サーバーのログでプラグインが有効化されていることを確認:

```bash
docker logs <minecraft_container> 2>&1 | grep -E "Man10Socket|Man10ShopV3"
```

期待される出力:
```
Enabling Man10Socket v1.0-SNAPSHOT
Enabling Man10ShopV3 v1.0-SNAPSHOT
```

2. API サーバーのログでソケット接続が確立されていることを確認:

```bash
docker logs <man10shopv3_container> 2>&1 | tail -10
```

期待される出力:
```
Socket opened Man10ShopV3
Uvicorn running on http://0.0.0.0:8000
```

3. ゲーム内でコマンド `/mshop` が登録されていれば導入完了です。

4. ゲーム内で `/mshop health` を実行し、設定と Socket 接続状態を確認します。

期待される出力:
```
pluginEnabled: 有効
serverName: man10
api.endpoint: http://man10shopv3:8000
Man10Socket plugin: 有効
socket clients: 1
API socket target: 接続中(1)
基本状態は正常です
```

`API socket target: 未接続` と出る場合は、API サーバーが Man10Socket に接続できていないか、
`man10socket.hosts[].name` が `"Man10ShopV3"` になっていません。

---

## 設定項目リファレンス

### Man10Socket プラグイン (`plugins/Man10Socket/config.yml`)

| 項目 | 説明 | デフォルト |
|-----|------|----------|
| `listeningPort` | API サーバーが接続する TCP ポート | `6789` |

### Man10ShopV3 プラグイン (`plugins/Man10ShopV3/config.yml`)

| 項目 | 説明 | デフォルト |
|-----|------|----------|
| `pluginEnabled` | プラグインの有効/無効 | `true` |
| `serverName` | サーバー識別名（複数サーバー時は一意にする） | - |
| `api.endpoint` | パス生成の基準値（直接 HTTP 通信には使わない） | - |
| `api.key` | 旧 HTTP 実装の名残（Man10Socket 経由では未使用） | - |
| `prefix` | ゲーム内メッセージの接頭辞 | - |

### API サーバー (`config/config.json`)

| 項目 | 説明 | 値 |
|-----|------|---|
| `hostPort` | HTTP ポート | `8000` |
| `mongodbConnectionString` | MongoDB 接続先 | `mongodb://mongodb:27017` |
| `communicationMode` | 通信方式 | `"socket"` |
| `man10socket.hosts[].name` | クライアント登録名 | `"Man10ShopV3"` (固定) |
| `man10socket.hosts[].host` | MC サーバーのホスト | `minecraft` (Docker Compose サービス名) |
| `man10socket.hosts[].port` | Man10Socket ポート | `6789` |

---

## Docker イメージのビルド

`man10shop:local` イメージが無い場合、Man10ShopV3 リポジトリで以下を実行します。

```bash
# ローカル用ビルド（単一アーキテクチャ）
./scripts/docker-buildx.sh --load -p linux/amd64 -i man10shop -t local

# マルチアーキビルド（レジストリへプッシュ）
./scripts/docker-buildx.sh --push -i shojabon/man10shopv3 -t latest
```

---

## トラブルシューティング

### API サーバーが `Connection refused` で接続できない

- Minecraft サーバーが起動しているか確認
- Man10Socket プラグインが有効化されているか確認
- Man10Socket の `listeningPort` と API サーバーの `man10socket.hosts[].port` が一致しているか確認
- 同一 Docker ネットワークに所属しているか確認

### ヘルスチェックで `socket clients: 0` / `API socket target: 未接続`

- Minecraft サーバーを再起動した後、API サーバー側のソケット接続が切断されたまま復旧しないことがある
- API サーバーを再起動すれば再接続される:
  ```bash
  docker compose restart man10shopv3
  ```

### プラグインからのリクエストが API サーバーに届かない

- API サーバーの `man10socket.hosts[].name` が `"Man10ShopV3"` になっているか確認
- プラグインは `Man10Socket.send("Man10ShopV3", ...)` で送信するため、名前が一致しないと通信できない

### MongoDB 接続エラー

- `docker compose ps mongodb` でコンテナが healthy か確認

### `.venv/bin/python: no such file or directory`

古いイメージを使っている可能性があります。再ビルドしてください:

```bash
docker compose down
docker compose build --no-cache man10shopv3
docker compose up -d
```

---

## ファイル構成

```
Paper_1_21_8/
├── docker-compose.yml
├── man10shop/
│   └── config/
│       └── config.json              # API サーバー設定
├── plugins/
│   ├── Man10ShopV3-1.0-SNAPSHOT.jar
│   ├── Man10Socket-1.0-SNAPSHOT.jar
│   ├── McUtils-2.0.jar
│   ├── SCommandRouter-1.0-SNAPSHOT.jar
│   ├── Man10ShopV3/
│   │   └── config.yml               # ショッププラグイン設定
│   └── Man10Socket/
│       └── config.yml               # ソケット通信プラグイン設定
└── Man10ShopV3_導入手順書.md         # この文書
```
