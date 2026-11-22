# UpdateKit 对接契约

UpdateKit 是 [OpenHub](https://github.com/wanwu2026/OpenHub) 的 Android 客户端 SDK，只做两件事：**查更新** 和 **拉公告**。服务端发清单/公告时会签名，客户端负责验签——私钥永远只在服务器。

## 1. 服务发现

客户端只需配置两个地址（拼在 OpenHub 根地址后）：

| 用途 | 路径 | 鉴权 |
|---|---|---|
| 查更新 | `GET /api/v1/update/check?product=&current_version_code=&channel=` | API Key |
| 拉公告 | `GET /api/v1/announce/list?product=&since=` | API Key |

鉴权头统一：`Authorization: Bearer <api-key>`。API Key 由开发者在 OpenHub 管理后台生成后下发到 App（**不要写死在客户端作为唯一凭据**，见 §5）。

## 2. 查更新（update/check）

响应是 OpenHub 统一包络 `{ok, data, meta}`。有更新时 `data`：

```json
{
  "has_update": true,
  "manifest": {
    "product": "my_app",
    "version": "1.2.0",
    "version_code": 12,
    "channel": "stable",
    "signer_fingerprint": "<64 位 hex, APK 签名证书 SHA-256>",
    "apk_url": "https://cdn/app.apk",
    "sha256": "<apk 文件 sha256>",
    "notes": "修复若干问题"
  },
  "manifest_signature": "<Base32, 对 manifest 规范 JSON 的 RSA-SHA256 签名>",
  "signature_algo": "RSA-SHA256"
}
```

无更新时 `has_update=false` 且 `manifest=null`、`manifest_signature=null`。

## 3. 拉公告（announce/list）

```json
{
  "ok": true,
  "data": {
    "notices": [
      {
        "id": 1,
        "product": "my_app",
        "title": "版本公告",
        "content": "中文内容",
        "level": "info|warn|urgent",
        "force_popup": false,
        "published_at": 1730000000000,
        "signature": "<Base32, 对公告规范 JSON 的 RSA-SHA256 签名>"
      }
    ]
  }
}
```

`since` 参数用于增量拉取：只返回 `published_at > since` 的公告。客户端把已读的最大 `published_at` 存本地，下次带上。

## 4. 签名与验签（最关键）

**算法**：`RSASSA-PKCS1-v1_5 / SHA-256`（Java `SHA256withRSA`），签名结果用 RFC4648 Base32（A-Z2-7，无填充）编码。客户端只嵌公钥（X.509 SPKI DER 的 Base64）。

**规范 JSON（canonical）必须用完全相同的参数重建字节**，否则含中文时验签必然失败：

```
json.dumps(obj, sort_keys=True, separators=(",", ":"))   # Python 默认 ensure_ascii=True
```

对应 Java 侧必须：**按 key 排序**、**无空格分隔**、**非 ASCII 字符转义为 \uXXXX**（`ensure_ascii=True` 的效果）。本 SDK 的 `CanonicalJson` 就是照这个语义实现的，并有中文用例单测锁死。

- 更新清单签名体 = manifest **全部字段**的规范 JSON（服务端签的就是响应里那个 manifest 对象）。
- 公告签名体 = `{product,title,content,level,force_popup,published_at}` 六个字段的规范 JSON；**`id` 与 `signature` 不进签名体**（id 只做已读去重）。

## 5. 客户端必须做的双校验（更新）

只验清单签名不够——攻击者可以替换 `apk_url` 指向恶意 APK。正确顺序：

1. 用内置公钥验 `manifest_signature`（防清单被篡改）；
2. 下载 APK 后，用 `PackageManager.getPackageArchiveInfo()` + `signingInfo` 取签名证书，算其 SHA-256，与 `manifest.signer_fingerprint` 比对（防 URL 被换）；
3. 可选：比对 APK 文件 sha256 与 `manifest.sha256`。

任一步失败即拒绝更新。服务端只提供可验的清单与指纹，**不替客户端做下载后校验**。

## 6. 安全边界（诚实说明）

- API Key 下发到客户端就能被逆向提取，它只用于**识别请求**，不是保密凭据。真正的安全靠签名验签——即使攻击者拿到 API Key 并伪造响应，也伪造不了服务端签名。
- 公钥可被替换（二次打包）：缓解靠代码混淆 + 证书锁定，无银弹。
- 公告验签同样重要：不验签的公告可被中间人替换成钓鱼内容。

## 7. 与 CardKeySDK 的关系

复用同一套原语（RSA 验签、Base32、SPKI 公钥加载、设备码），但**独立仓库**，不互相依赖。三者关系：CardKey（激活）+ UpdateKit（更新/公告）都是客户端，OpenHub 是它们的后端。
