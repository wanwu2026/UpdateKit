package com.example.updatekit.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import java.security.MessageDigest;

/** APK 签名证书指纹（SHA-256），用于"下载后比对指纹"这第二道校验。 */
public final class Fingerprint {
    private Fingerprint() {}

    /** 取本机已安装包的签名证书 SHA-256（小写 hex）。 */
    public static String installedApp(Context context, String packageName) throws Exception {
        PackageManager pm = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
            SigningInfo si = info.signingInfo;
            if (si != null) {
                Signature[] sigs = si.getApkContentsSigners();
                if (sigs != null && sigs.length > 0) return sha256(sigs[0].toByteArray());
            }
        }
        PackageInfo legacy = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        if (legacy.signatures != null && legacy.signatures.length > 0) {
            return sha256(legacy.signatures[0].toByteArray());
        }
        throw new Exception("无法读取签名证书: " + packageName);
    }

    /** 取一个 APK 文件（下载后、安装前）的签名证书 SHA-256。 */
    public static String archiveApp(Context context, String apkPath) throws Exception {
        PackageManager pm = context.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo info = pm.getPackageArchiveInfo(apkPath, flags);
        if (info == null) throw new Exception("无法解析 APK: " + apkPath);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.signingInfo != null) {
            Signature[] sigs = info.signingInfo.getApkContentsSigners();
            if (sigs != null && sigs.length > 0) return sha256(sigs[0].toByteArray());
        }
        if (info.signatures != null && info.signatures.length > 0) {
            return sha256(info.signatures[0].toByteArray());
        }
        throw new Exception("APK 无签名证书: " + apkPath);
    }

    /** 计算字节的 SHA-256 hex（小写），与签名体/服务端指纹比对用。 */
    public static String sha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(data);
        StringBuilder sb = new StringBuilder(d.length * 2);
        for (byte b : d) {
            int v = b & 0xFF;
            sb.append(Character.forDigit(v >>> 4, 16)).append(Character.forDigit(v & 15, 16));
        }
        return sb.toString();
    }
}
