# UpdateKit · Android 更新检测 + 公告 SDK

> 🤖 本项目由 AI 生成。

[中文](README.md) | [English](README_EN.md)

一个**纯客户端**的 Android SDK：安全地检查应用更新、拉取公告。后端用 [OpenHub](https://github.com/wanwu2026/OpenHub) 的 `update` / `announce` 插件（也可对接任何实现同一契约的服务）。

- 服务端对更新清单、公告**签名**，客户端**验签**——私钥永远只在服务器，不进包
- 更新做**双校验**：验清单签名 + 比对 APK 签名证书指纹（防中间人换 URL）
- 零第三方运行时依赖（只用 Android 系统 API + `org.json`）
- 核心验签逻辑是纯 Java 类，可单测；中文内容用 Python/OpenSSL 交叉验证字节级一致

> ⚠️ **诚实说明**：API Key 下发到客户端就能被逆向提取，它只用于识别请求，不是保密凭据。真正的安全靠**签名验签**——即使攻击者拿到 Key 并伪造响应，也伪造不了服务端签名。详见 [契约](docs/API_CONTRACT.md) §6。

---

## 它解决什么

前两个项目：**CardKeySDK**（卡密激活）+ **OpenHub**（通用后端）。OpenHub 的 `update` / `crash` 插件只有服务端，缺客户端。UpdateKit 补上这块：让 App 能安全地查更新、拉公告，和 CardKey 一起构成"客户端 SDK 家族"。

```
App 启动
 → UpdateKit 带 API Key 请求 OpenHub /update/check 和 /announce/list
 → 用内置公钥验签名（失败即丢弃，不信任）
 → 更新：下载 APK 后比对签名证书 SHA-256 与 manifest.signer_fingerprint
 → 都通过才把更新/公告交给宿主弹窗
```

---

## 快速开始

### 看演示（预编译 APK）

Releases 里的 `UpdateKitDemo-release.apk`（GitHub Actions 用临时密钥编译签名，每次构建密钥都变，重装需先卸载旧版）。Demo 内置一对演示密钥与已签名的清单/公告，**离线即可演示验签成功与篡改失败**：

- 「验签演示」：中文清单/公告验签 → 成功（证明 canonical 字节一致）
- 「篡改字段」：改掉 notes / 正文后验签 → 失败（证明防伪造）

> 本机无 Android SDK（aarch64），APK 由 GitHub Actions 在 x86_64 runner 上编译，CI 里跑真 Java 单测 + `:demo:assembleRelease`。

### 接入你的 App（3 步）

**1. 依赖**：

本地源码集成：`implementation project(':sdk')`。

远程依赖（JitPack，仓库打 tag 后可用）：

```gradle
// settings.gradle 的 repositories 加：maven { url 'https://jitpack.io' }
dependencies {
    implementation 'com.github.wanwu2026:UpdateKit:TAG'   // TAG = 仓库 tag，如 1.0
}
```

JitPack 首次构建需在 https://jitpack.io 用 GitHub 账号登录并触发（打 tag 后 JitPack 会自动发现）。

**2. 初始化**（`Application.onCreate`）：

```java
UpdateKit.init(new UpdateKit.Config(this)
        .baseUrl("https://your.openhub.example/api/v1")
        .apiKey("ohk_xxx")                    // 在 OpenHub 后台生成下发给 App
        .publicKeyBase64(PUBLIC_KEY_BASE64)); // OpenHub 签名公钥（只嵌公钥）
```

**3. 查更新 / 拉公告**：

```java
// 查更新（验签通过才返回）
UpdateInfo info = UpdateKit.get().checkUpdate(currentVersionCode);
if (info != null) {
    new UpdateDialog(this).info(info)
        .onConfirm(i -> downloadAndInstall(i.apkUrl))  // 下载后务必比对指纹
        .show();
}

// 拉公告（增量，逐条验签，失败的丢弃）
List<Notice> notices = UpdateKit.get().fetchNotices();
for (Notice n : notices) {
    new NoticeDialog(this).notice(n).show();
}
```

**下载后的第二道校验**（务必做）：

```java
String actual = Fingerprint.archiveApp(context, downloadedApkPath);
if (!UpdateVerifier.fingerprintMatches(actual, info.signerFingerprint)) {
    // 指纹不符 → 拒绝安装（APK 可能被替换）
}
```

---

## 签名与验签（最关键）

服务端（OpenHub）与客户端必须用**完全相同的字节**做签名/验签。规范 JSON 三要点：

1. key 按字典序排
2. 无空格分隔：`{"a":1,"b":2}`
3. 非 ASCII 转义成 `\uXXXX`（含 emoji 的 UTF-16 代理对，如 `😀` → `\ud83d\ude00`）

即复刻 Python `json.dumps(obj, sort_keys=True, separators=(",", ":"))`（`ensure_ascii=True`）。Java 侧由 `core/CanonicalJson.java` 实现，`tools/canonical_java.py` 是它的严格复刻，两者与 Python 原生输出做字节级交叉验证（含中文/emoji/控制字符/代理对）。

- **更新清单**签名体 = manifest 全部字段
- **公告**签名体 = `{product,title,content,level,force_popup,published_at}` 六字段；`id` 与 `signature` 不进签名体

算法：`SHA256withRSA`（RSASSA-PKCS1-v1_5 / SHA-256），签名用 RFC4648 Base32 无填充编码。详见 [契约](docs/API_CONTRACT.md) §4。

---

## 目录结构

```
UpdateKit/
├── sdk/src/main/java/com/example/updatekit/
│   ├── UpdateKit.java               # 单例入口 init/checkUpdate/fetchNotices
│   ├── core/
│   │   ├── CanonicalJson.java       # 规范 JSON（字节级对齐服务端）
│   │   ├── SignatureVerifier.java   # 公钥加载 + 验签
│   │   ├── UpdateVerifier.java      # 验签编排 + 指纹比对（纯逻辑，可单测）
│   │   ├── Fingerprint.java         # APK 签名证书 SHA-256
│   │   ├── ApiClient.java / Base32.java / Base64.java / Json.java
│   ├── model/UpdateInfo.java / Notice.java
│   └── ui/UpdateDialog.java / NoticeDialog.java
├── demo/                            # 演示 App（内置演示数据，离线演示验签）
├── tools/updatekit_tool.py          # 密钥生成 / 造签名数据 / 校验（不进 app）
├── tools/test_tool.py               # 交叉验证（本机 Python/OpenSSL）
└── docs/API_CONTRACT.md             # 对接契约
```

---

## 验证记录

| 项 | 状态 |
|---|---|
| CanonicalJson 复刻 vs Python json.dumps（中文/emoji/代理对/控制字符） | ✅ 字节级一致 |
| Base32 RFC4648 向量 | ✅ 全对 |
| 更新清单 / 公告 签名→验签端到端（含中文） | ✅ 通过 |
| 篡改 content/notes 后验签拒绝 | ✅ 通过 |
| 公告六字段（改 id 不影响验签） | ✅ 通过 |
| 嵌入 demo 的清单/公告用内置公钥验签 | ✅ 通过 |
| Java JUnit（CanonicalJson + UpdateVerifier） | ⏳ 由 GitHub Actions 在带 JDK 的 runner 上跑 |
| Android 编译出 APK | ⏳ 由 GitHub Actions 云端编译 |

本机环境：Python 3.13 / OpenSSL 3.5；无 JDK / Gradle / Android SDK（aarch64）。

---

## License

MIT —— 见 [LICENSE](LICENSE)。
