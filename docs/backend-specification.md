# EnCounter バックエンド・ロジック処理仕様書

> **最終更新**: 2026年2月6日  
> **バージョン**: 1.0.0

---

## 📑 目次

- [📑 目次](#-目次)
- [はじめに](#はじめに)
- [システム概要](#システム概要)
- [モジュール構成](#モジュール構成)
- [BLE通信システム](#ble通信システム)
- [データ管理](#データ管理)
- [マッチング・フィルタリングロジック](#マッチングフィルタリングロジック)
- [通知システム](#通知システム)
- [永続化](#永続化)
- [依存性注入（DI）](#依存性注入di)
- [データフロー](#データフロー)
- [パフォーマンス最適化](#パフォーマンス最適化)
- [トラブルシューティング](#トラブルシューティング)
- [更新履歴](#更新履歴)

---

## はじめに

本仕様書は、EnCounterアプリの**バックエンド・ロジック処理**について記述します。UIコンポーネントの詳細は別途フロントエンド仕様書に記載予定です。

### 全体シーケンス図（概要）

アプリ起動からすれちがい検知・チャットまでの全体フローを示します。

```mermaid
sequenceDiagram
    autonumber
    actor User as ユーザー
    participant App as EnCounter App
    participant Auth as Firebase Auth
    participant DB as Firestore
    participant BLE as BLE通信
    participant Other as 他ユーザー端末

    Note over User,DB: 🚀 アプリ起動・初期化
    User->>App: アプリ起動
    App->>Auth: 匿名認証
    Auth-->>App: UID発行
    App->>DB: プロフィール取得/作成
    DB-->>App: User情報

    Note over User,Other: 📡 すれちがい通信開始
    User->>App: 「すれちがい開始」タップ
    App->>BLE: startEncounter(uidPrefix)
    
    par 発信（Advertise）
        BLE->>Other: Service UUID + uidPrefix
    and 受信（Scan）
        Other->>BLE: BLE信号
    end

    Note over App,DB: 🔍 検知・フィルタリング
    BLE-->>App: 検知イベント(uidPrefix)
    App->>DB: getUserByUidPrefix()
    DB-->>App: 相手のUser情報
    App->>App: フィルタリング処理
    App->>App: 通知（バイブ・サウンド）
    App->>App: 履歴保存

    Note over User,DB: 💬 チャット開始
    User->>App: ユーザーカードタップ
    App->>DB: getOrCreateChatRoom()
    DB-->>App: ChatRoom
    User->>App: メッセージ送信
    App->>DB: sendMessage()
```

### 技術スタック

| カテゴリ | 技術 |
|:--|:--|
| **言語** | Kotlin 2.0+ |
| **UI** | Jetpack Compose |
| **DI** | Hilt (Dagger) |
| **非同期** | Kotlin Coroutines + Flow |
| **データベース** | Firebase Firestore |
| **認証** | Firebase Auth (匿名認証) |
| **BLE** | Android BLE API (BluetoothLeScanner, BluetoothLeAdvertiser) |
| **ローカル永続化** | SharedPreferences |
| **ビルド** | Gradle Kotlin DSL |

### 対応Android

| 項目 | 値 |
|:--|:--|
| minSdk | 26 (Android 8.0) |
| targetSdk | 36 |
| compileSdk | 36 |

---

## システム概要

### アーキテクチャ

```mermaid
flowchart TB
    subgraph UI["UI Layer"]
        RadarScreen["RadarScreen"]
        MatchListScreen["MatchListScreen"]
        ChatScreen["ChatScreen"]
        RadarVM["RadarViewModel"]
        MatchListVM["MatchListViewModel"]
        ChatVM["ChatViewModel"]
        
        RadarScreen --> RadarVM
        MatchListScreen --> MatchListVM
        ChatScreen --> ChatVM
    end
    
    subgraph Domain["Domain Layer"]
        User["User<br/>UserStatus"]
        EncounterRec["EncounterRecord"]
        ChatRoom["ChatRoom<br/>Message"]
    end
    
    subgraph Data["Data Layer"]
        UserRepo["UserRepository"]
        HistoryRepo["EncounterHistoryRepository"]
        ChatRepo["ChatRepository"]
        
        Firestore1[("Firestore")]
        SharedPrefs[("SharedPreferences")]
        Firestore2[("Firestore")]
        
        UserRepo --> Firestore1
        HistoryRepo --> SharedPrefs
        ChatRepo --> Firestore2
    end
    
    subgraph Infra["Infrastructure Layer"]
        BleManager["BleManager"]
        SettingsRepo["SettingsRepository"]
        NotifManager["NotificationManager"]
    end
    
    RadarVM --> User
    RadarVM --> EncounterRec
    MatchListVM --> User
    MatchListVM --> EncounterRec
    ChatVM --> ChatRoom
    
    User --> UserRepo
    EncounterRec --> HistoryRepo
    ChatRoom --> ChatRepo
    
    Data --> Infra
```

### 主要機能

| 機能 | 説明 |
|:--|:--|
| **BLEすれちがい検知** | 周囲のEnCounterユーザーをBLEでリアルタイム検知 |
| **マッチング・フィルタリング** | ステータスと興味タグに基づく条件付きマッチング |
| **すれちがい履歴** | 検知したユーザーの永続化・履歴管理 |
| **チャット** | マッチしたユーザーとのリアルタイムチャット |
| **ステルスモード** | 自分を見せずに他者を検知するモード |
| **通知** | 検知時のバイブレーション・サウンド通知 |

### 機能間の関係図

```mermaid
flowchart LR
    subgraph Core["コア機能"]
        BLE["📡 BLE通信"]
        Filter["🔍 フィルタリング"]
        Match["🤝 マッチング"]
    end
    
    subgraph Data["データ管理"]
        Profile["👤 プロフィール"]
        History["📚 履歴"]
        Chat["💬 チャット"]
    end
    
    subgraph Support["サポート機能"]
        Notif["🔔 通知"]
        Stealth["👻 ステルス"]
        Settings["⚙️ 設定"]
    end
    
    BLE --> Filter
    Filter --> Match
    Match --> History
    Match --> Chat
    Match --> Notif
    
    Profile --> Filter
    Settings --> BLE
    Stealth --> BLE
```

---

## モジュール構成

### ディレクトリ構造

```
app/src/main/java/com/encounter/app/
├── EnCounterApplication.kt    # Applicationクラス（Hilt初期化）
├── MainActivity.kt            # エントリーポイント
├── ble/                       # BLE通信モジュール
│   └── BleManager.kt          # BLE制御（Advertise/Scan）
├── data/                      # データ層
│   └── repository/
│       ├── UserRepository.kt           # ユーザー情報（Firebase）
│       ├── ChatRepository.kt           # チャット（Firebase）
│       ├── EncounterHistoryRepository.kt  # すれちがい履歴（Local）
│       └── SettingsRepository.kt       # 設定（Local）
├── debug/                     # デバッグ機能
├── di/                        # 依存性注入
│   └── AppModule.kt           # Hiltモジュール定義
├── domain/                    # ドメイン層
│   └── model/
│       ├── User.kt            # ユーザーモデル・ステータス
│       ├── EncounterRecord.kt # すれちがい記録モデル
│       └── ChatRoom.kt        # チャットルーム・メッセージモデル
├── navigation/                # 画面遷移
│   ├── Screen.kt              # ルート定義
│   └── AppNavGraph.kt         # ナビゲーショングラフ
├── notification/              # 通知機能
│   └── EncounterNotificationManager.kt  # バイブ・サウンド
└── ui/                        # UI層（フロントエンド）
    ├── screens/
    │   ├── radar/             # レーダー画面
    │   ├── matchlist/         # すれちがい図鑑
    │   ├── chat/              # チャット画面
    │   ├── profile/           # プロフィール設定
    │   ├── settings/          # 設定画面
    │   └── ...
    └── theme/                 # テーマ定義
```

---

## BLE通信システム

### 概要

BLE（Bluetooth Low Energy）を使用して、周囲のEnCounterユーザーを検知します。

- **Advertise（発信）**: 自分のuidPrefixをBLEで周囲にブロードキャスト
- **Scan（受信）**: 周囲のEnCounterユーザーからのBLE信号を受信

### BleManager クラス

**ファイル**: `ble/BleManager.kt`

#### 定数

| 定数 | 値 | 説明 |
|:--|:--|:--|
| `SERVICE_UUID` | `0000EC00-0000-1000-8000-00805F9B34FB` | EnCounter専用Service UUID |
| `DEFAULT_RSSI_THRESHOLD` | -70 dBm | デフォルト受信感度閾値 |

#### 受信感度プリセット（ScanSensitivity）

| プリセット | RSSI閾値 | 検知距離の目安 |
|:--|:--|:--|
| `HIGH` | -85 dBm | 約10m以上 |
| `MEDIUM` | -70 dBm | 約5m |
| `LOW` | -50 dBm | 約1-2m |

#### 送信電力プリセット（TxPowerLevel）

| プリセット | 到達距離の目安 |
|:--|:--|
| `HIGH` | 約10m以上 |
| `MEDIUM` | 約5-8m |
| `LOW` | 約1-3m |
| `ULTRA_LOW` | 約0.5-1m |

### BLE通信フロー

```mermaid
sequenceDiagram
    participant User as ユーザー
    participant App as EnCounter App
    participant BLE as BleManager
    participant Firebase as Firestore

    User->>App: すれちがい開始
    App->>BLE: startEncounter(uidPrefix)
    
    par Advertise（発信）
        BLE->>BLE: startAdvertising(uidPrefix)
        Note over BLE: Service UUID + uidPrefix をブロードキャスト
    and Scan（受信）
        BLE->>BLE: startScanning()
        Note over BLE: Service UUIDでフィルタリング
    end
    
    loop 周囲のデバイスを検知
        BLE->>BLE: processScanResult()
        BLE->>BLE: RSSI閾値チェック
        BLE-->>App: rawDetectionEvent(uidPrefix, rssi)
        App->>Firebase: getUserByUidPrefix(uidPrefix)
        Firebase-->>App: User情報
        App->>App: フィルタリング処理
        alt フィルター通過
            App->>BLE: addDetectedDevice(uidPrefix)
            BLE-->>App: newDetectionEvent
            App->>App: 通知（バイブ・サウンド）
            App->>App: 履歴に追加
        else フィルター不通過
            Note over App: スキップ（表示しない）
        end
    end
```

### ステルスモード

ステルスモードを有効にすると、**発信（Advertise）を停止**し、**受信（Scan）のみ**になります。

```kotlin
fun setStealthMode(enabled: Boolean) {
    _isStealthMode.value = enabled
    if (enabled && _isAdvertising.value) {
        stopAdvertising()  // 発信停止
    }
}
```

---

## データ管理

### Firestore データ構造

#### ER図（エンティティ関係図）

```mermaid
erDiagram
    USER ||--o{ CHATROOM : "participates"
    USER ||--o{ ENCOUNTER_RECORD : "detected"
    CHATROOM ||--|{ MESSAGE : "contains"
    
    USER {
        string uid PK
        string uidPrefix UK "BLE用短縮UID"
        string displayName
        string comment
        string status "UserStatus"
        array tags "string[]"
        string fcmToken
    }
    
    CHATROOM {
        string roomId PK
        array participants "uid[]"
        number createdAt
        string lastMessage
    }
    
    MESSAGE {
        string messageId PK
        string roomId FK
        string senderId FK
        string text
        number createdAt
    }
    
    ENCOUNTER_RECORD {
        string uidPrefix PK "Local保存"
        number timestamp
        string displayName "nullable"
    }
```

#### users コレクション

```
users/
└── {uid}/
    ├── uid: string              # Firebase Auth UID
    ├── uidPrefix: string        # BLE用短縮UID（16文字）
    ├── displayName: string      # 表示名
    ├── comment: string          # 一言コメント
    ├── status: string           # UserStatus（OPEN, BORED, etc.）
    ├── tags: array<string>      # 興味タグ
    └── fcmToken: string         # プッシュ通知用トークン
```

#### chatRooms コレクション

```
chatRooms/
└── {roomId}/
    ├── roomId: string
    ├── participants: array<string>  # 参加者UIDリスト（ソート済み）
    ├── createdAt: number
    ├── lastMessage: string
    └── messages/                    # サブコレクション
        └── {messageId}/
            ├── messageId: string
            ├── senderId: string
            ├── text: string
            └── createdAt: number
```

### UserRepository

**ファイル**: `data/repository/UserRepository.kt`

#### 主要メソッド

| メソッド | 説明 |
|:--|:--|
| `signInAnonymously()` | Firebase匿名認証でログイン |
| `saveUserProfile(user)` | プロフィール保存（uidPrefixを自動設定） |
| `getUser(uid)` | UID指定でユーザー取得 |
| `getUsersByIds(uidPrefixes)` | 複数uidPrefixでバッチ取得（30件ずつチャンク） |
| `getUserByUidPrefix(uidPrefix)` | 単一uidPrefixで取得（**キャッシュ付き**） |
| `observeCurrentUser()` | 現在ユーザーをリアルタイム監視 |
| `clearUserCache()` | キャッシュクリア |

#### キャッシュシステム

BLEでは同じユーザーが何度も検知されるため、**uidPrefixベースのキャッシュ**でFirebase照会数を削減します。

```kotlin
private val userCache = mutableMapOf<String, User?>()

suspend fun getUserByUidPrefix(uidPrefix: String): User? {
    // キャッシュ確認
    if (userCache.containsKey(uidPrefix)) {
        return userCache[uidPrefix]  // キャッシュヒット
    }
    
    // Firestore照会
    val user = firestore.collection("users")
        .whereEqualTo("uidPrefix", uidPrefix)
        .limit(1)
        .get()
        .await()
        .documents.firstOrNull()?.toObject(User::class.java)
    
    // キャッシュに保存（nullもキャッシュ）
    userCache[uidPrefix] = user
    return user
}
```

### EncounterHistoryRepository

**ファイル**: `data/repository/EncounterHistoryRepository.kt`

すれちがい履歴を**SharedPreferences**で永続化します。

| 項目 | 値 |
|:--|:--|
| 最大保存件数 | 1,000件 |
| 保存形式 | JSON配列 |
| 重複時の挙動 | タイムスタンプ・名前を更新 |

```kotlin
data class EncounterRecord(
    val uidPrefix: String,     // BLEで検知したUID（16文字）
    val timestamp: Long,       // すれちがった日時
    val displayName: String?   // ユーザー名（キャッシュ用）
)
```

**設計意図**:
- Firebase Firestoreではなくローカル保存を採用
- 理由: データ量が多くなる可能性、オフライン動作、コスト最適化
- SharedPreferencesで十分な規模（最大1,000件程度を想定）

---

## マッチング・フィルタリングロジック

### UserStatus（気分ステータス）

プロダクトの核心機能。多彩な気分表現を実現します。

| ステータス | 表示名 | 絵文字 | 分類 |
|:--|:--|:--|:--|
| `OPEN` | 話しかけてOK | 👋 | 積極的 |
| `BORED` | 暇してます | 😴 | 積極的 |
| `LOOKING_FOR_HELP` | 誰か助けて | 🆘 | 積極的 |
| `GAME_PARTNER` | ゲーム仲間募集 | 🎮 | 積極的 |
| `COFFEE` | カフェ行きたい | ☕ | 積極的 |
| `COMMUTING` | 移動中 | 🚃 | 状況系 |
| `WORKING` | 作業中 | 💻 | 状況系 |
| `BUSY` | 忙しい | 🔴 | 非アクティブ系 |
| `OFFLINE` | オフライン | ⚫ | 非表示 |

#### ステータス状態遷移図

```mermaid
stateDiagram-v2
    [*] --> OFFLINE : アプリ起動
    
    state "アクティブ" as Active {
        state "積極的" as Positive {
            OPEN : 👋 話しかけてOK
            BORED : 😴 暇してます
            LOOKING_FOR_HELP : 🆘 誰か助けて
            GAME_PARTNER : 🎮 ゲーム仲間募集
            COFFEE : ☕ カフェ行きたい
        }
        
        state "状況系" as Situational {
            COMMUTING : 🚃 移動中
            WORKING : 💻 作業中
        }
        
        state "非アクティブ" as Inactive {
            BUSY : 🔴 忙しい
        }
    }
    
    OFFLINE --> Active : ステータス変更
    Active --> OFFLINE : オフライン切替
    
    note right of Active
        アクティブなステータス同士で
        マッチング判定が行われる
    end note
```

### フィルタリング条件

検知時に以下の**すべての条件**を満たした場合のみ、ユーザーに表示されます。

1. **自分がアクティブ**: 自分のステータスがOFFLINE以外
2. **相手がアクティブ**: 相手のステータスがOFFLINE以外
3. **ステータス一致**: 自分と相手のステータスが同じ
4. **タグ一致**: 興味タグが1つ以上一致

```kotlin
// RadarViewModel.processNewDetection()

// 1. 自分がOFFLINEの場合はスキップ
if (!myUser.status.isActive()) return

// 2. Firebase照会（相手の情報取得）
val detectedUser = userRepository.getUserByUidPrefix(uidPrefix)
if (detectedUser == null) return

// 3. 相手がOFFLINEの場合はスキップ
if (!detectedUser.status.isActive()) return

// 4. ステータスが一致するか確認
if (myUser.status != detectedUser.status) return

// 5. 興味タグが1つ以上一致するか確認
val commonTags = myUser.tags.intersect(detectedUser.tags.toSet())
if (commonTags.isEmpty()) return

// フィルター通過 → 検知リストに追加
bleManager.addDetectedDevice(uidPrefix)
```

### フィルタリング・フロー図

```mermaid
flowchart TD
    A[BLE検知イベント] --> B{自分がOFFLINE?}
    B -->|Yes| Z[スキップ]
    B -->|No| C[Firebase照会]
    C --> D{ユーザー存在?}
    D -->|No| Z
    D -->|Yes| E{相手がOFFLINE?}
    E -->|Yes| Z
    E -->|No| F{ステータス一致?}
    F -->|No| Z
    F -->|Yes| G{タグ1つ以上一致?}
    G -->|No| Z
    G -->|Yes| H[検知リストに追加]
    H --> I[通知・履歴保存]
```

---

## 通知システム

### 通知フロー図

```mermaid
sequenceDiagram
    participant VM as RadarViewModel
    participant NM as NotificationManager
    participant Settings as SettingsRepository
    participant Vibrator as Vibrator
    participant Sound as SoundPool

    VM->>NM: notifyDetection()
    
    NM->>Settings: vibrationEnabled.first()
    Settings-->>NM: true/false
    
    NM->>Settings: soundEnabled.first()
    Settings-->>NM: true/false
    
    NM->>Settings: soundVolume.first()
    Settings-->>NM: 0.0~1.0
    
    alt バイブレーション有効
        NM->>Vibrator: vibrateHeartbeat()
        Note over Vibrator: 鼓動風パターン<br/>弱→強→弱
    end
    
    alt サウンド有効
        NM->>Sound: playMatchSound(volume)
        Note over Sound: カスタム音 or<br/>システム音
    end
```

### EncounterNotificationManager

**ファイル**: `notification/EncounterNotificationManager.kt`

検知時のバイブレーションと音声通知を管理します。

#### バイブレーションパターン

**鼓動風パターン（Heartbeat）**:
```kotlin
// タイミング: 0ms開始, 100ms振動, 50ms休止, 300ms振動, 50ms休止, 100ms振動
val HEARTBEAT_PATTERN = longArrayOf(0, 100, 50, 300, 50, 100)
// 振幅（0=休止, 255=最大強度）
val HEARTBEAT_AMPLITUDES = intArrayOf(0, 120, 0, 255, 0, 120)
```

#### 音声通知

- **SoundPool**を使用（低遅延・軽量）
- カスタムサウンド: `res/raw/notification_match.mp3`
- サウンドファイルがない場合はシステムデフォルト音を使用

#### 設定連携

```kotlin
fun notifyDetection() {
    val vibrationEnabled = settingsRepository.vibrationEnabled.first()
    val soundEnabled = settingsRepository.soundEnabled.first()
    val volume = settingsRepository.soundVolume.first()
    
    if (vibrationEnabled) vibrateHeartbeat()
    if (soundEnabled) playMatchSound(volume)
}
```

---

## 永続化

### SettingsRepository

**ファイル**: `data/repository/SettingsRepository.kt`

設定値を**SharedPreferences**で永続化します。

#### 設定適用フロー

```mermaid
flowchart TD
    subgraph Init["初期化時"]
        A[SettingsRepository.init] --> B[設定値読み込み]
        B --> C[BleManagerに適用]
    end
    
    subgraph Runtime["実行時"]
        D[ユーザーが設定変更] --> E[SharedPreferencesに保存]
        E --> F[StateFlow更新]
        F --> G{BLE設定?}
        G -->|受信感度| H[BleManager.setScanSensitivity]
        G -->|送信電力| I[BleManager.setTxPowerLevel]
        G -->|ステルス| J[BleManager.setStealthMode]
        G -->|通知設定| K[NotificationManagerで参照]
    end
    
    C --> Runtime
```

#### 保存される設定

| 設定 | キー | デフォルト値 |
|:--|:--|:--|
| 受信感度 | `scan_sensitivity` | `MEDIUM` |
| 送信電力 | `tx_power_level` | `MEDIUM` |
| バイブレーション | `vibration_enabled` | `true` |
| 音声通知 | `sound_enabled` | `true` |
| 音量 | `sound_volume` | `0.7f` |
| ステルスモード | `stealth_mode` | `false` |

#### 初期化時の設定適用

```kotlin
init {
    // 初回起動時にBleManagerに設定を適用
    applyBleSensitivity(_scanSensitivity.value)
    applyTxPowerLevel(_txPowerLevel.value)
    applyStealthMode(_stealthMode.value)
}
```

---

## 依存性注入（DI）

### DIコンポーネント関係図

```mermaid
classDiagram
    class AppModule {
        <<Hilt Module>>
        +provideFirebaseAuth() FirebaseAuth
        +provideFirebaseFirestore() FirebaseFirestore
    }
    
    class BleManager {
        <<Singleton>>
        -context: Context
        +startAdvertising(uidPrefix)
        +startScanning()
        +setStealthMode(enabled)
    }
    
    class UserRepository {
        <<Singleton>>
        -auth: FirebaseAuth
        -firestore: FirebaseFirestore
        -userCache: Map
        +getUserByUidPrefix()
        +observeCurrentUser()
    }
    
    class SettingsRepository {
        <<Singleton>>
        -context: Context
        -bleManager: BleManager
        +scanSensitivity: StateFlow
        +stealthMode: StateFlow
    }
    
    class EncounterHistoryRepository {
        <<Singleton>>
        -context: Context
        +history: StateFlow
        +addEncounter()
    }
    
    class EncounterNotificationManager {
        <<Singleton>>
        -context: Context
        -settingsRepository: SettingsRepository
        +notifyDetection()
    }
    
    class RadarViewModel {
        <<HiltViewModel>>
        +uiState: StateFlow
        +toggleEncounter()
    }
    
    AppModule ..> UserRepository : provides
    BleManager <-- SettingsRepository : injects
    SettingsRepository <-- EncounterNotificationManager : injects
    BleManager <-- RadarViewModel : injects
    UserRepository <-- RadarViewModel : injects
    SettingsRepository <-- RadarViewModel : injects
    EncounterHistoryRepository <-- RadarViewModel : injects
    EncounterNotificationManager <-- RadarViewModel : injects
```

### Hilt構成

**ファイル**: `di/AppModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
```

### Singletonクラス

以下のクラスは`@Singleton`スコープで提供されます：

| クラス | 役割 |
|:--|:--|
| `BleManager` | BLE通信の全体管理 |
| `UserRepository` | ユーザー情報のFirebase連携 |
| `ChatRepository` | チャット機能のFirebase連携 |
| `EncounterHistoryRepository` | すれちがい履歴のローカル保存 |
| `SettingsRepository` | 設定のローカル保存 |
| `EncounterNotificationManager` | 通知（バイブ・サウンド） |

---

## データフロー

### すれちがい検知からUI表示までの流れ

```mermaid
sequenceDiagram
    participant BLE as BleManager
    participant VM as RadarViewModel
    participant Repo as UserRepository
    participant History as EncounterHistoryRepository
    participant Notif as NotificationManager
    participant UI as RadarScreen

    BLE->>BLE: processScanResult()
    BLE->>VM: rawDetectionEvent(uidPrefix, rssi)
    VM->>Repo: getUserByUidPrefix(uidPrefix)
    
    alt キャッシュヒット
        Repo-->>VM: User（キャッシュから）
    else キャッシュミス
        Repo->>Repo: Firestore照会
        Repo-->>VM: User
    end
    
    VM->>VM: フィルタリング処理
    
    alt フィルター通過
        VM->>BLE: addDetectedDevice(uidPrefix)
        BLE->>VM: newDetectionEvent
        VM->>Notif: notifyDetection()
        VM->>History: addEncounter(uidPrefix, displayName)
        VM->>VM: UIState更新
        VM-->>UI: detectedUsers更新
    end
```

### StateFlow によるUI更新

```kotlin
data class RadarUiState(
    val isScanning: Boolean = false,
    val isAdvertising: Boolean = false,
    val isStealthMode: Boolean = false,
    val detectedDeviceCount: Int = 0,
    val detectedDevices: Set<String> = emptySet(),
    val detectedUsers: Map<String, User> = emptyMap(),
    val detectedTimestamps: Map<String, Long> = emptyMap(),
    // ...
)
```

---

## パフォーマンス最適化

### キャッシュシステムのフロー

```mermaid
flowchart TD
    A[BLE検知: uidPrefix] --> B{userCacheに存在?}
    
    B -->|Yes| C[キャッシュヒット]
    C --> D[User返却]
    
    B -->|No| E[キャッシュミス]
    E --> F[Firestore照会]
    F --> G{User存在?}
    
    G -->|Yes| H[Userをキャッシュに保存]
    G -->|No| I[nullをキャッシュに保存]
    
    H --> D
    I --> J[null返却]
```

### 1. uidPrefixキャッシュ

BLE検知では同じユーザーが繰り返し検知されるため、`UserRepository`にキャッシュを実装。

- **キャッシュヒット時**: Firebase照会なし
- **キャッシュミス時**: 1回だけ照会してキャッシュに保存
- **nullもキャッシュ**: 未登録ユーザーの再照会を防止

### 2. Firestoreバッチクエリ

`getUsersByIds()`では、Firestoreの`whereIn`制限（30件）に対応してチャンク分割。

```kotlin
uidPrefixes.chunked(30).forEach { chunk ->
    firestore.collection("users")
        .whereIn("uidPrefix", chunk)
        .get()
    // ...
}
```

### 3. 非同期処理の並列化

`rawDetectionEvent`の処理は`launch`で非同期実行し、BLE検知をブロックしない。

```kotlin
bleManager.rawDetectionEvent.collect { event ->
    launch {  // 非同期処理（ブロックしない）
        processNewDetection(event.uidPrefix, event.rssi)
    }
}
```

### 4. SharedFlowのバッファ

BLE検知は短時間に複数発生する可能性があるため、バッファを設定。

```kotlin
private val _rawDetectionEvent = MutableSharedFlow<NewDetectionEvent>(
    extraBufferCapacity = 20
)
```

### パフォーマンス最適化の全体像

```mermaid
flowchart LR
    subgraph BLE["📡 BLE検知"]
        Scan[Scanイベント] --> Buffer[SharedFlow<br/>Buffer=20]
    end
    
    subgraph Process["⚙️ 処理"]
        Buffer --> Async[launchで<br/>非同期処理]
        Async --> Cache{キャッシュ<br/>確認}
    end
    
    subgraph Firebase["🔥 Firebase"]
        Cache -->|Miss| Query[Firestore照会]
        Query --> Chunk[30件ずつ<br/>チャンク分割]
    end
    
    subgraph Result["✅ 結果"]
        Cache -->|Hit| Return[即座に返却]
        Chunk --> Return
    end
```

---

## トラブルシューティング

### 問題切り分けフロー

```mermaid
flowchart TD
    A["検知されない"] --> B{Bluetooth<br/>ON?}
    B -->|No| B1["端末設定でONに"]
    B -->|Yes| C{BLE権限<br/>許可?}
    
    C -->|No| C1["アプリ設定で権限許可"]
    C -->|Yes| D{Android<br/>バージョン?}
    
    D -->|11以下| E{Location<br/>ON?}
    E -->|No| E1["位置情報をONに"]
    E -->|Yes| F
    D -->|12以上| F
    
    F{Service UUID<br/>一致?} -->|No| F1["アプリ側の問題"]
    F -->|Yes| G{RSSI閾値<br/>範囲内?}
    
    G -->|No| G1["距離が遠い<br/>受信感度を上げる"]
    G -->|Yes| H{ステータス<br/>一致?}
    
    H -->|No| H1["同じステータスに設定"]
    H -->|Yes| I{タグ<br/>一致?}
    
    I -->|No| I1["共通タグを設定"]
    I -->|Yes| J["✅ 検知成功"]
```

### BLEスキャンが動作しない

| 原因 | 解決策 |
|:--|:--|
| Bluetooth無効 | 端末設定でBluetoothをONに |
| 権限未許可 | アプリに`BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`権限を許可 |
| 位置情報無効（Android 11以下） | 端末設定で位置情報をONに |

### ユーザーが検知されない

| 原因 | 解決策 |
|:--|:--|
| ステータスがOFFLINE | アクティブなステータスに変更 |
| ステータス不一致 | 相手と同じステータスに設定 |
| タグ不一致 | 共通のタグを設定 |
| 距離が遠い | 受信感度を「高感度」に変更 |
| ステルスモード中 | 設定画面でステルスモードをOFFに |

### キャッシュの問題

アプリ起動時に`UserRepository.clearUserCache()`が呼ばれます。手動でクリアする場合は「検知リストクリア」機能を使用してください。

---

## 更新履歴

| 日付 | バージョン | 変更内容 |
|:--|:--|:--|
| 2026-02-06 | 1.0.0 | 初版作成 |
