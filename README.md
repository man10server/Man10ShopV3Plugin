# Man10ShopV3Plugin

Man10ShopV3 の Maven ビルド用リポジトリです。

## 前提

- Java 21
- Maven 3.8+
- 依存 jar を `libs/` に配置済み

このリポジトリでは以下を `provided` 依存として参照します。

- `com.shojabon:McUtils:2.0`
- `com.shojabon:SCommandRouter:1.0-SNAPSHOT`

## McUtils / SCommandRouter の追加方法

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
```

## ビルド方法

```bash
mvn clean package
```

成果物は `target/` に出力されます。

## 補足

- ソケット通信実装 (`com.shojabon.man10socket.Man10Socket`) はこのリポジトリ内に同梱しています。
- ポートは `config.yml` の `api.socketPort` を参照します（未設定時は `api.endpoint` のポート、さらに未指定なら `6789`）。
