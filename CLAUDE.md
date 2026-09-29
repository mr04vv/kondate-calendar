# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

献立カレンダー: 料理を「日付 × 朝昼夜」の枠に割り当てる、個人用の Android / iPad アプリ（Kotlin Multiplatform + Compose Multiplatform + Room）。画面・DB・ロジックは `:shared` に置き、`:app` は Android の入口、`iosApp/` は iOS の SwiftUI ホストだけを持つ。仕様の正は `docs/spec.md`。採用デザインは案1「台所ノート」で、キャンバスは https://claude.ai/artifact/WGS4Z5aqU62sRefVJ8Kem4 。

## コマンド

この Mac にはシステムの JDK がないため、Android Studio 同梱の JBR を使う。

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew assembleDebug                 # app/build/outputs/apk/debug/app-debug.apk
./gradlew :shared:testAndroidHostTest   # JVM ユニットテスト（全件）
./gradlew :shared:testAndroidHostTest --tests "com.github.mr04vv.kondatecalendar.domain.ShoppingListTest"
./gradlew :shared:testAndroidHostTest --tests "*DishFilterTest.recommend*"
./gradlew lintDebug                     # AGP 標準の lint（独自設定なし）
./gradlew :shared:compileKotlinIosSimulatorArm64   # iOS 向けのコンパイル確認

# iPad シミュレーター向けビルド（Xcode のビルドフェーズが Gradle で Shared.framework を作る）
xcodebuild -project iosApp/iosApp.xcodeproj -target iosApp -configuration Debug -sdk iphonesimulator -arch arm64 build
```

iOS の実機へは `iosApp/iosApp.xcodeproj` を Xcode で開き、Signing の Team を選んで実行する。Xcode 26 は iOS プラットフォームが別途ダウンロード（Settings > Components）で、入っていないと `-scheme` 指定のビルドや実機の実行先が選べない。この Mac のシミュレーターのランタイムは iOS 18.6。

端末への反映は `~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk`。エミュレーターの AVD は `Medium_Phone_API_36.0`。実機は Xiaomi で、開発者向けオプションの「USB 経由でインストール」がオフだと `INSTALL_FAILED_USER_RESTRICTED` になる。

テストは `shared/src/androidHostTest` の JVM ユニットテストだけ（Compose の UI テストと Room のインストルメントテストは置かない方針）。テスト可能なロジックは TDD で、先に失敗するテストを書く。

## アーキテクチャ

- **バージョンの制約。** Kotlin 2.4.20 は Xcode 26 に対応するための選択。Compose Multiplatform は 1.10.3 で、1.11 以降は AGP 9.1 と compileSdk 37 が必要。AGP 8.x では KSP が Android 側の生成コードを登録しないので、`shared/build.gradle.kts` で生成ディレクトリを `androidMain` に足している（AGP 9 に上げたら外せるか確認する）。
- **ソースセット。** 共通コードは `shared/src/commonMain`、OS ごとの差分は `androidMain` / `iosMain` の `expect` / `actual`（DB の作成、写真、共有シート、端末名）。写真は Android だけで、iOS の `PhotoSection` は空。戻る操作は Android の `MainActivity` の `BackHandler` で扱い、iOS は各画面の戻る矢印だけ。
- **`domain/` は OS に依存しない純粋な Kotlin。** 日付は kotlinx-datetime。日付計算（月曜始まりの月格子は 4〜6 行、リストモードの週見出し付き行）、買い物リストの同名まとめと共有用テキスト、絞り込みとおすすめ、プリセット投入の差分計算がここにあり、ユニットテストの対象はすべてこの層と `feedback/Issue.kt`。ロジックを足すときはこの層に純粋関数で置き、DAO や Compose からは呼ぶだけにする。
- **Room のエンティティがそのままドメインモデル。** `data/Entities.kt` の `Dish` / `MealSlot` / `ShoppingItem` を UI まで直接使う。`Dish.presetKey` が null なら自作の料理（「由来」の列は別に持たない）。材料は JSON 文字列、タイプはカンマ区切り、`LocalDate` は epochDay で保存する（`Converters`）。ドライバーは両 OS とも `BundledSQLiteDriver` で、Android の DB ファイルは移行前と同じ `kondate.db`。
- **スキーマ変更の手順。** `KondateDatabase` の `version` を上げ、`Migration`（`migrate(connection: SQLiteConnection)` を実装）を `KondateDatabase.build` の `addMigrations` に追加する。スキーマは Room の Gradle プラグインで `shared/schemas/` に書き出されるので、生成された JSON もコミットする。実機に利用者のデータがあるため、破壊的な移行（`fallbackToDestructiveMigration`）は使わない。
- **ViewModel は `KondateViewModel` の 1 つだけ。** DAO を受け取り、Android は `MainActivity`、iOS は `MainViewController` で作る。料理・献立・買い物リストを `StateFlow` で持ち、献立は全件をメモリ上の `Map<SlotKey, Dish>` にしている。画面遷移はナビゲーションライブラリを使わず、`vm.stack`（`Screen` の積み重ね）と `BackHandler` で管理する。献立選択シートと枠のメニューは `vm.picking` / `vm.slotMenu` が null でないときに表示される。
- **積んだ画面はタブの上に重ねて表示する。** `KondateAppUi` はタブの `Scaffold` を常に残し、`Screen` を `Surface` で上に重ねる。タブごとの `rememberSaveable` 状態は `rememberSaveableStateHolder` で保持する。タブを差し替える書き方に戻すと、カレンダーの表示モードやスクロール位置が戻るたびに消える。
- **プリセットは `shared/src/commonMain/composeResources/files/presets.json`（約 290 品）。** Compose のリソースとして `Res.readBytes` で読む。起動のたびに `presetsToInsert` で未登録の `key` だけを追加するので、JSON に料理を足すと既存ユーザーにも反映される。`PresetsTest` が同梱 JSON そのものを検証する（200 品以上、key と name の一意性、name は 10 文字以内、丼と「ご飯もの」は併用しない、材料と手順が空でない）。材料名は買い物リストで完全一致でまとめるため、既存と同じ表記（例: 玉ねぎ、鶏もも肉、しょうゆ）に揃える。分量は 2 人分の文字列。
- **テーマは常に案1 のライト配色。** ダークテーマは持たない（仕様で決定済み）。ジャンル色は `Genre.color`、見出しは `FontFamily.Serif`（端末の明朝体に依存し、Xiaomi のシステムフォント設定によってはゴシックで表示される）。iOS は `UIUserInterfaceStyle = Light` で常にライト。
- **要望フォーム（`feedback/`）は GitHub API で直接 Issue を作る。** トークンは `local.properties` の `github.issueToken`（Git 管理外）から、`shared` の `generateBuildInfo` タスクが生成する `BuildInfo.GITHUB_ISSUE_TOKEN` としてビルド時に APK と iOS アプリの両方へ埋め込まれる。通信は Ktor。対象リポジトリは公開の `mr04vv/kondate-calendar` で、種類に応じて `enhancement` / `bug` ラベルを付ける。

## 慣習

- `ponytail:` で始まるコメントは、意図的に簡略化した箇所とその限界・拡張方法の目印。
- 意味のある数値は名前付き定数にする。コード内のコメントは英語、UI 文言と `docs/` は日本語。
- `docs/spec.md` に影響する変更は、更新してよいか依頼者に確認してから反映する。
