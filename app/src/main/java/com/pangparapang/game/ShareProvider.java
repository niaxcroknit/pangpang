package com.pangparapang.game;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * 자랑하기 그림을 카카오톡 같은 다른 앱에 건네주는 아주 작은 통로예요.
 * 앱 캐시의 share 폴더에 있는 그림 파일만, 읽기 전용으로, 공유를 허락받은 앱에게만 열어 줘요.
 */
public class ShareProvider extends ContentProvider {
    public static final String AUTHORITY = "com.pangparapang.game.share";

    private File fileFor(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("/") || name.contains("..")) throw new FileNotFoundException();
        File f = new File(new File(getContext().getCacheDir(), "share"), name);
        if (!f.exists()) throw new FileNotFoundException();
        return f;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return "image/png";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(fileFor(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        try {
            File f = fileFor(uri);
            c.addRow(new Object[]{f.getName(), f.length()});
        } catch (FileNotFoundException ignored) {
            // 파일이 없으면 빈 결과
        }
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
