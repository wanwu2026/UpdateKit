package com.example.updatekit.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.example.updatekit.model.Notice;

/** 公告弹窗：按 level 显示不同标题，forcePopup 时不可取消。 */
public final class NoticeDialog {
    public interface OnClose {
        void onClose(Notice notice);
    }

    private final Context context;
    private Notice notice;
    private OnClose onClose;

    public NoticeDialog(Context context) { this.context = context; }

    public NoticeDialog notice(Notice notice) { this.notice = notice; return this; }
    public NoticeDialog onClose(OnClose cb) { this.onClose = cb; return this; }

    public void show() {
        if (notice == null) throw new IllegalStateException("未设置 Notice");
        String title = titleFor(notice.level);
        AlertDialog.Builder b = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(notice.content)
                .setPositiveButton("知道了", null); // 回调统一放 onDismiss，避免重复触发
        if (notice.forcePopup) {
            b.setCancelable(false);
        }
        final AlertDialog dialog = b.create();
        // 关闭（点按钮或点外部）只回调一次，宿主在这里推进已读水位
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface d) {
                if (onClose != null) onClose.onClose(notice);
            }
        });
        dialog.show();
    }

    private static String titleFor(String level) {
        if ("urgent".equals(level)) return "紧急公告";
        if ("warn".equals(level)) return "重要公告";
        return "公告";
    }
}
