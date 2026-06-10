# Live Coverage プラグイン

**言語：** [English](README.md) · [简体中文](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

JaCoCo を利用した IntelliJ IDEA 向けリアルタイムコードカバレッジ可視化プラグインです。

## 機能

### 主な機能
- **リアルタイム可視化**：アプリ実行中に実行済みコードを自動ハイライト
- **累積カバレッジモード**：実行結果を継続的に累積し、"Clear Coverage" でリセット可能
- **JaCoCo Agent 内蔵**：agent JAR を同梱、追加ダウンロード不要
- **ツールウィンドウ**：カバレッジ統計とメソッド単位情報を表示
- **手動操作**：Refresh / Clear（リセット）/ Polling の一時停止・再開
- **マルチモジュール対応**：`Source output path` + `Classes output path` を複数ペア登録可能

### 解決する課題
- 保守が難しい複雑なレガシー業務コード
- 増分開発・要件変更時の影響範囲分析
- 回帰テスト前のテストデータ準備と不具合箇所の特定
- 「このリクエストで実際にどの業務コードが実行されたか」を迅速に把握

### 価値
- **リクエストからコード経路の可視化と理解コスト削減**：実行されたコードブロックを素早く特定
- **回帰テスト効率向上**：実行経路に基づいてコードと要件の影響を分析し、テストデータを準備
- **業務不具合の特定**：レガシー挙動をより速く把握し、問題のある業務コード断片を絞り込む

## インストール

[JetBrains Marketplace](https://plugins.jetbrains.com) からインストール（**Live Coverage** を検索）。30 日間トライアルと月額サブスクリプションに対応。

ローカル開発向けビルドは [ビルド](#ビルド) を参照。

## セットアップ

1. **設定**：
   - `Settings` -> `Tools` -> `Live Coverage`
   - TCP Address（既定：127.0.0.1）
   - TCP Port（既定：6300）
   - 1 つ以上のパスペアを追加：
     - `Source output path`（Java ソースルートの絶対パス）
     - `Classes output path`（コンパイル済み `.class` の絶対パス）

2. **アプリ実行**：
   - **自動注入**：JVM 引数は実行構成へ自動追加
   - Java アプリの実行/デバッグ時に JaCoCo JVM 引数を自動注入
   - 手動設定は不要

## 使い方

### 自動モード
プロジェクトを開くと自動でポーリングを開始し、実行に応じてカバレッジを累積します。

### 手動操作
`Tools` -> `Live Coverage` から利用：
- **Refresh Coverage**：カバレッジを手動更新
- **Clear Coverage**：全ハイライトをクリアし JaCoCo agent をリセット
- **Pause/Resume Coverage**：自動ポーリングの停止/再開

### ツールウィンドウ
下部の「Request Coverage」で確認できる内容：
- カバレッジ統計
- メソッド単位の実行情報
- 接続状態
- `Clear Coverage` と `Pause/Resume Coverage` ボタン

### 主なポイント
- ✅ **Agent 同梱**：JaCoCo agent JAR の手動取得不要
- ✅ **JVM 引数自動設定**：実行構成へ自動追加
- ✅ **累積モード**：実行履歴を自動蓄積
- ✅ **手動リセット**：いつでもリセット可能
- ✅ **リアルタイム更新**：実行経路を即時確認
- ✅ **実運用志向**：スレッドセーフでリソース管理された構成

## ビルド

```bash
./gradlew buildPlugin
```

成果物は `build/distributions/` に生成されます。

## 開発

### プロジェクト構成
- **ソースコード**：`src/main/java/`
- **リソース**：`src/main/resources/`
- **ビルド設定**：`build.gradle.kts`

### コードスタイル
- IntelliJ Platform のコーディング規約に従う
- Java 21 を使用
- すべてのサービスはスレッドセーフ
- `@NotNull` / `@Nullable` による null 安全性

## トラブルシューティング

### カバレッジが表示されない
1. JaCoCo agent が起動・到達可能か確認
2. TCP アドレス/ポート設定を確認
3. classes output path が正しいか確認
4. source output path がソース構成と一致するか確認

### 接続エラー
- JaCoCo agent の TCP 設定が正しいか確認
- ファイアウォール/ネットワーク設定を確認
- IDE ログを確認（Help -> Show Log in Finder/Explorer）

## ライセンス

Copyright (c) 2026 Showen。エンドユーザーライセンス：[EULA.md](EULA.md)。

## 謝辞

使用技術：
- IntelliJ Platform SDK
- JaCoCo Code Coverage Library
