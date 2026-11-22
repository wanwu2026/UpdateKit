package com.example.updatekit.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.example.updatekit.model.UpdateInfo;

/** 升级提示弹窗：显示版本与更新说明，确认回调交给宿主决定怎么下载/安装。 */
public final class UpdateDialog {
    public interface OnConfirm {
        void onConfirm(UpdateInfo info);
    }

    private final Context context;
    private UpdateInfo info;
    private OnConfirm onConfirm;

    public UpdateDialog(Context context) { this.context = context; }

    public UpdateDialog info(UpdateInfo info) { this.info = info; return this; }
    public UpdateDialog onConfirm(OnConfirm cb) { this.onConfirm = cb; return this; }

    public void show() {
        if (info == null) throw new IllegalStateException("未设置 UpdateInfo");
        String msg = "版本 " + info.version + "（" + info.versionCode + "）可用";
        if (info.notes != null && !info.notes.isEmpty()) msg += "\n\n" + info.notes;
        new AlertDialog.Builder(context)
                .setTitle("发现新版本")
                .setMessage(msg)
                .setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        if (onConfirm != null) onConfirm.onConfirm(info);
                    }
                })
                .setNegativeButton("稍后", null)
                .show();
    }
}
