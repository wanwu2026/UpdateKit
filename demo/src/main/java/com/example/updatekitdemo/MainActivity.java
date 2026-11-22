package com.example.updatekitdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.updatekit.UpdateKit;
import com.example.updatekit.core.Json;
import com.example.updatekit.core.UpdateVerifier;
import com.example.updatekit.model.Notice;
import com.example.updatekit.model.UpdateInfo;
import com.example.updatekit.ui.NoticeDialog;
import com.example.updatekit.ui.UpdateDialog;

import java.util.Map;

/**
 * 纯代码 UI（无需 layout XML）。演示核心能力：
 * - 内置演示清单/公告用内置公钥验签 → 成功（含中文，证明 canonical 字节一致）
 * - 篡改字段后验签 → 失败（证明防伪造）
 * - 真实联网查更新/拉公告只需解开注释里的两行（需配置自己的 OpenHub 地址与 API Key）
 */
public class MainActivity extends Activity {
    private TextView log;
    private StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = 24;
        box.setPadding(pad, pad, pad, pad);

        log = new TextView(this);
        log.setTextSize(15);
        box.addView(log);

        box.addView(button("1. 公钥加载 + 验签演示（离线）", new View.OnClickListener() {
            @Override public void onClick(View v) { demoVerify(); }
        }));
        box.addView(button("2. 篡改字段 → 验签应失败", new View.OnClickListener() {
            @Override public void onClick(View v) { demoTamper(); }
        }));
        box.addView(button("3. 弹出升级对话框（已验签清单）", new View.OnClickListener() {
            @Override public void onClick(View v) { showUpdateDialog(); }
        }));
        box.addView(button("4. 弹出公告对话框（已验签公告）", new View.OnClickListener() {
            @Override public void onClick(View v) { showNoticeDialog(); }
        }));
        TextView tip = new TextView(this);
        tip.setTextSize(13);
        tip.setText("\n真实联网：UpdateKit.get().checkUpdate(当前版本号) / fetchNotices()，"
                + "需在 DemoApp 配置你的 OpenHub 地址与 API Key。");
        box.addView(tip);

        scroll.addView(box);
        setContentView(scroll);
        append("UpdateKit Demo 就绪。公钥加载: " + DemoApp.publicKeyLoadable());
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(l);
        return b;
    }

    private final UpdateVerifier verifier() {
        return UpdateKit.get().verifier();
    }

    private void demoVerify() {
        sb.setLength(0);
        Map<String, Object> m = Json.toMap(DemoData.manifest());
        String sig = DemoData.manifest().optString("_signature", "");
        append("清单验签（含中文 notes）: " + verifier().verifyManifest(m, sig));

        Map<String, Object> n = Json.toMap(DemoData.notice());
        String nsig = DemoData.notice().optString("signature", "");
        append("公告验签（六字段签名体）: " + verifier().verifyNotice(n, nsig));
    }

    private void demoTamper() {
        sb.setLength(0);
        Map<String, Object> m = Json.toMap(DemoData.manifest());
        m.put("notes", "【被篡改】换成一个假的说明");
        String sig = DemoData.manifest().optString("_signature", "");
        append("篡改 notes 后验签（应为 false）: " + verifier().verifyManifest(m, sig));

        Map<String, Object> n = Json.toMap(DemoData.notice());
        n.put("content", "【被篡改】钓鱼内容");
        String nsig = DemoData.notice().optString("signature", "");
        append("篡改公告正文后验签（应为 false）: " + verifier().verifyNotice(n, nsig));
    }

    private void showUpdateDialog() {
        UpdateInfo info = new UpdateInfo(Json.toMap(DemoData.manifest()));
        new UpdateDialog(this).info(info).onConfirm(new UpdateDialog.OnConfirm() {
            @Override public void onConfirm(UpdateInfo i) {
                append("用户点了立即更新 → 宿主自行下载安装：" + i.apkUrl);
            }
        }).show();
    }

    private void showNoticeDialog() {
        Notice notice = new Notice(Json.toMap(DemoData.notice()));
        new NoticeDialog(this).notice(notice).onClose(new NoticeDialog.OnClose() {
            @Override public void onClose(Notice n) { append("公告已关闭/已读"); }
        }).show();
    }

    private void append(String line) {
        sb.append(line).append('\n');
        log.setText(sb.toString());
    }
}
