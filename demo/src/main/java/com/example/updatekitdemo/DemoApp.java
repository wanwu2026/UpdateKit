package com.example.updatekitdemo;

import android.app.Application;

import com.example.updatekit.UpdateKit;
import com.example.updatekit.core.SignatureVerifier;

/** 初始化 SDK。演示数据内置在本地（无服务器时也能看到验签效果），见 MainActivity。 */
public class DemoApp extends Application {
    public static final String PRODUCT = "demo_app";

    // 演示公钥（配套 tools/updatekit_tool.py keygen 生成的临时密钥；仅演示用）
    public static final String PUBLIC_KEY_BASE64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvVYZKfJE8oVUfzJDqPLuXea0xOd+33x02HoCSukO3PrfVv0GSgTEE5AbX1DayMTN59pIJe67gbiv3FkjyUU7enzsBytSncYCCbBZTtSUHwIid2i5Ed9CUYAmI9eGb/3G8vB1PphmB05a2ANOrAaYJPKNDoUFWlWQU7MXxEkt0+22+T3UIMFMfQjRW3Wqq373Mr473YLUutFC7BGLtEPOyBuy9laLIY1YRI9ENGpmLTGrcrTFePpfmD/08pPF+0/JSfiTstqiAvYhw0zztuwnp8P5p7lf/QtfYKcVYnRKR0DcnZDkJfTgS16vFQ9znktLTpcQzkcw2klD0bWlTGVgOQIDAQAB";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            UpdateKit.init(new UpdateKit.Config(this)
                    .baseUrl("https://your.openhub.example/api/v1") // 自行替换成你的 OpenHub 地址
                    .apiKey("ohk_demo_replace_me")
                    .publicKeyBase64(PUBLIC_KEY_BASE64)
                    .product(PRODUCT));
        } catch (Exception e) {
            // 公钥错误会在 init 抛 SignatureException；演示数据保证正确，这里只兜底
            throw new RuntimeException("UpdateKit 初始化失败", e);
        }
    }

    /** 便于 demo 判断是否已成功初始化（不依赖网络）。 */
    public static boolean publicKeyLoadable() {
        try {
            SignatureVerifier.publicKeyFromBase64(PUBLIC_KEY_BASE64);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
