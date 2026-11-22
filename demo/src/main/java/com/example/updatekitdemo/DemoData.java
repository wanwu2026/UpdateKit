package com.example.updatekitdemo;

import org.json.JSONObject;

/** 演示数据：由 tools/updatekit_tool.py 用演示公钥的私钥签出，含中文，用于离线演示验签。 */
public final class DemoData {
    private DemoData() {}

    /** 服务端 manifest（含签名字段 _signature）。 */
    public static final String MANIFEST_JSON =
            "{\"product\":\"demo_app\",\"version\":\"2.0.0\",\"version_code\":20,\"channel\":\"stable\","
            + "\"signer_fingerprint\":\"cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc\","
            + "\"apk_url\":\"https://example.com/app-v2.apk\","
            + "\"sha256\":\"dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd\","
            + "\"notes\":\"演示更新：修复中文乱码，提升稳定性\","
            + "\"_signature\":\"JYXL7LOYOEKJ5O7K34RRINRXTETOAU5UZPMK272OOYIYDTUR644SB52IAHQLOCOUEXBNCMHPS5UE472VXSW6ZXX23DVJI4LACOLBMECRRHZ2RMCZLPFPE3EKHGAQVP4GNO7BP7FZDG4XKUBVIIDAWFJMGQ4XFFRKYWF2XI7VR2TX662GUBMWAE7C43NLN2XYNHVH3SMI3FYUYCYGXYNNXEWHDBTWA7P7O7KCHD4TSFE4BBVCBKYYB3UQSKO2BWATPPP32NG5Z5NGYOHOBPEC7OL3IHA4Y6XIG4LXYT22R3HHOHZKSS2SBBNGYONCIJYJIPUBUAX6CS5SQWH24HKAWNU6AIP7DJDMSNPJNYPM3YQ3CFEBI6EHWCWG7MLGONTSSBMAC3ELZ642Y4K3VGDFXRBRE4\"}";

    /** 服务端公告（含 signature / id）。 */
    public static final String NOTICE_JSON =
            "{\"product\":\"demo_app\",\"title\":\"版本公告\",\"content\":\"中文内容不乱码 😀\","
            + "\"level\":\"warn\",\"force_popup\":true,\"published_at\":1730000000000,"
            + "\"signature\":\"NCT4YVNGHVVWO6AMI26W6M2ZLCS5U7ZRHRF5LJZBKAZ5YZEIQ74FDPD3YRQHNTGSIT4KEDBDO3GDINOOPCZA7M6ZPX75RTRZ7PEERYX5M5GOO2WOAM63ICIR3TVXW4ISRQZMYF4A2LLRNNBBN275PDDW3GUVSOM7P2VN3FL567FBWH5M6ZZP7PXB5MD3XPAR5GDS3SE7Q24BGQA25FNOXJ67ZEYUJCDMOSKRNSFQGDABTIXHIZAE46B2JBAYTT6S42HOHAAAZTXYYOXMMKWQ2BL43DV7N6COX6JP355NYLTGHEUQQELGXBQU3YIPPZTOHZ5XEHCLSZDN5A6RAELFPS5OO3JRGK2MIU3CRLZKIE36QW3OMHDNILETZ4OF64PVWY4EKY63SZD7ZXOCTP6OSRYZ3I\","
            + "\"id\":1}";

    public static JSONObject manifest() {
        try { return new JSONObject(MANIFEST_JSON); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public static JSONObject notice() {
        try { return new JSONObject(NOTICE_JSON); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
