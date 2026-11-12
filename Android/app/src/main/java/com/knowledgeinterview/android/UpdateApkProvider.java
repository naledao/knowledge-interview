package com.knowledgeinterview.android;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.OpenableColumns;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;

/** 仅向系统安装器临时开放应用缓存中的已校验安装包。 */
public final class UpdateApkProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return "/app-update.apk".equals(uri.getPath())
                ? "application/vnd.android.package-archive" : null;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"/app-update.apk".equals(uri.getPath()) || !"r".equals(mode)) {
            throw new FileNotFoundException("不允许访问此路径或写入缓存文件");
        }
        File apk = new File(getContext().getCacheDir(), "app-update.apk");
        if (!apk.isFile()) throw new FileNotFoundException("安装包不存在");
        return ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (!"/app-update.apk".equals(uri.getPath())) return null;
        File apk = new File(getContext().getCacheDir(), "app-update.apk");
        if (!apk.isFile()) return null;
        String[] columns = projection == null
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
        MatrixCursor cursor = new MatrixCursor(columns);
        Object[] values = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) values[i] = "KnowledgeInterview-update.apk";
            else if (OpenableColumns.SIZE.equals(columns[i])) values[i] = apk.length();
        }
        cursor.addRow(values);
        return cursor;
    }
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("不允许写入安装包");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("不允许修改安装包");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("不允许删除安装包");
    }
}
