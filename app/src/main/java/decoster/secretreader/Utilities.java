package decoster.secretreader;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.security.GeneralSecurityException;

import decoster.secretreader.AESCrypt;

/**
 * Created by kevin on 19.04.18.
 */

public class Utilities {

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    public  static File getPublicAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        boolean success=true;
        if (!file.exists()) {
            success = file.mkdir();
        }
        if (success) {
            Log.e("main", "Directory created");
        } else {
            Log.e("main", "Directory not created");
        }
        return file;
    }
    public static String decrypt(String secret, String password) throws GeneralSecurityException {
        return AESCrypt.decrypt(password, secret);
    }

    public static String encrypt(String secret, String password) throws GeneralSecurityException {
        return  AESCrypt.encrypt(password, secret);
    }

    public static void createPublicDir() {
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        try {
            path.mkdirs();
        }
        catch (Exception e) {
            Log.e("main", e.getMessage());
        }
    }
}
