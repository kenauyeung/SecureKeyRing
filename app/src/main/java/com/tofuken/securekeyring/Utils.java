package com.tofuken.securekeyring;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * Created by Ken on 8/2/2016.
 */
public class Utils {
    private static final int FILE_VERSION = 1;
    private static final String FILE_SUFFIX = ".bin";
    private String tmp;

    //  if not enough patch the key to 32 byte
    static byte[] fillKey(byte[] key) {

        if (key.length == 32) {
            return key;
        }
        byte[] keySize = new byte[32];
        if (key.length > 32) {
            System.arraycopy(key, 0, keySize, 0, 32);
        } else {
            int cur = 0;
            do {
                System.arraycopy(key, 0, keySize, cur, cur + key.length > 32 ? 32 - cur : key.length);
                cur += key.length;
            } while (cur < 32);
        }


        return keySize;
    }

    /**
     * Encrypt the given plaintext bytes using the given key
     *
     * @param data The plaintext to encrypt
     * @param key  The key to use for encryption
     * @return The encrypted bytes
     */
    static byte[] encrypt(byte[] data, byte[] key) throws InvalidCipherTextException {
        // 16 bytes is the IV size for AES256
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
        // Random iv
        SecureRandom rng = new SecureRandom();
        byte[] ivBytes = new byte[16];
        rng.nextBytes(ivBytes);

        cipher.init(true, new ParametersWithIV(new KeyParameter(key), ivBytes));
        byte[] outBuf = new byte[cipher.getOutputSize(data.length)];

        int processed = cipher.processBytes(data, 0, data.length, outBuf, 0);
        processed += cipher.doFinal(outBuf, processed);

        byte[] outBuf2 = new byte[processed + 16];        // Make room for iv
        System.arraycopy(ivBytes, 0, outBuf2, 0, 16);    // Add iv
        System.arraycopy(outBuf, 0, outBuf2, 16, processed);    // Then the encrypted data

        return outBuf2;
    }

    /**
     * Decrypt the given data with the given key
     *
     * @param data The data to decrypt
     * @param key  The key to decrypt with
     * @return The decrypted bytes
     */
    static byte[] decrypt(byte[] data, byte[] key) throws InvalidCipherTextException {
        // 16 bytes is the IV size for AES256
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
        byte[] ivBytes = new byte[16];
        System.arraycopy(data, 0, ivBytes, 0, ivBytes.length); // Get iv from data
        byte[] b = new byte[data.length - ivBytes.length];
        System.arraycopy(data, ivBytes.length, b, 0, data.length - ivBytes.length);

        cipher.init(false, new ParametersWithIV(new KeyParameter(key), ivBytes));
        byte[] decrypted = new byte[cipher.getOutputSize(b.length)];
        int len = cipher.processBytes(b, 0, b.length, decrypted, 0);
        len += cipher.doFinal(decrypted, len);

        return decrypted;
    }

    static void popAlert(Context context, int title, int message) {
        Resources res = context.getResources();
        popAlert(context, res.getString(title), res.getString(message));
    }

    static void popAlert(Context context, String title, String message) {
        String okButton = context.getResources().getString(R.string.button_ok);
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, okButton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }


    static String saveFile(Context context, String filename, byte[] key, byte[] data, boolean overwrite) throws IOException, InvalidCipherTextException {
        //filename = filename.toLowerCase() + FILE_SUFFIX;
        //File file = new File(context.getFilesDir(), filename);
        File file = getFileObject(context, filename);
        if (overwrite || !file.exists()) {
            key = fillKey(key);
            byte[] finalData = new byte[8 + (data == null ? 0 : data.length)];

            //Log.d("Data", "Write data len->"+data.length );

            System.arraycopy(intToByteArray(FILE_VERSION), 0, finalData, 0, 4); // file version
            System.arraycopy(intToByteArray(data.length), 0, finalData, 4, 4);  // # of data bytes
            if (data != null) {
                System.arraycopy(data, 0, finalData, 8, data.length);
            }
            finalData = encrypt(finalData, key);

            File tmpFile = getFileObject(context, filename, "tmp");
            FileOutputStream outputStream = new FileOutputStream(tmpFile, false);//  context.openFileOutput(file.getName(), Context.MODE_PRIVATE);
            //Log.d("Data", "# of Byte : " + finalData.length);

            outputStream.write(finalData);
            outputStream.close();

            if(verifySave(tmpFile,key,data)){
                if(tmpFile.renameTo(file)) {
                    Log.d("Data","Verification success:"+file.toString());
                    return file.toString();
                }else{
                    popAlert(context, R.string.error_title, R.string.msg_error_rename);
                }
            }else{
                popAlert(context, R.string.error_title, R.string.msg_error_verify);
            }
        } else {
            popAlert(context, R.string.error_title, R.string.file_exist);
        }
        return "";
    }

    static boolean verifySave(File tmpFile, byte[] key, byte[] originalData)throws IOException, InvalidCipherTextException {
        byte[] data = readFile(tmpFile, key);
        int fileVersion = byteArrayToInt(data, 0);
        int dataLen = byteArrayToInt(data, 4);

        if(fileVersion==FILE_VERSION && dataLen==originalData.length){
            // check each decoded data
            for(int i=0; i < originalData.length;i++){
                if(originalData[i] != data[i+8]){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    static byte[] readFile(Context context, String filename, byte[] key) throws IOException, InvalidCipherTextException {
        File file = getFileObject(context, filename);
        if (file.exists()) {
            byte[] data= readFile(file, key);

            int fileVersion = byteArrayToInt(data, 0);
            int dataLen = byteArrayToInt(data, 4);

            Log.d("Data", "File version " + fileVersion + ", Data Len:" + dataLen + ", Raw D:" + data.length);
            if (data.length > 4 && dataLen > 0) {

                byte[] finalData = new byte[dataLen];
                System.arraycopy(data, 8, finalData, 0, finalData.length);
                //Log.d("Data", "Final read byte->"+finalData.length );
                return finalData;
            }
            return null;
        } else {
            popAlert(context, R.string.error_title, R.string.file_not_exist);
        }

        return null;
    }

    private static byte[] readFile(File file, byte[] key) throws IOException, InvalidCipherTextException {
        byte[] data = new byte[(int) file.length()];
        FileInputStream inputStream = new FileInputStream(file);// context.openFileInput(file.getName());
        int curRead = 0;

        do {
            curRead += inputStream.read(data, curRead, data.length - curRead);
        } while (curRead < data.length);
        inputStream.close();
        return  decrypt(data, fillKey(key));
    }

    static File getFileObject(Context context, String filename) {
        return getFileObject(context, filename, FILE_SUFFIX);
    }

    static File getFileObject(Context context, String filename, String fileSuffix) {
        // incase the filename contains full path
        if (filename.lastIndexOf('/') > -1) {
            filename = filename.substring(filename.lastIndexOf('/') + 1);
            Log.d("Data", "File name:" + filename);
        }

        filename = filename.toLowerCase();
        if (!filename.endsWith(fileSuffix)) {
            filename += fileSuffix;
        }
        //return new File(context.getFilesDir(), filename);
        File f = new File(getParentDir(context), filename);
        Log.d("Data", "Get Folder:" + f.toString());
        return f;
    }

    static String[] getFileList(Context context) {
        Log.d("Data", "File Dir:" + android.os.Environment.getDataDirectory());
        File file = new File(getParentDir(context));//context.getFilesDir();
        Log.d("Data", "List Folder:" + file.toString());

        return file.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(FILE_SUFFIX);
            }
        });
    }

    static String getParentDir(Context context) {

        String s = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File tmp = context.getExternalFilesDir(null);
            if (tmp != null) s = tmp.toString();
        }

        if (s == null) s = context.getFilesDir().toString();

        Log.d("Data", "Parent Path:" + s);
        File f = new File(s);
        if (!f.exists()) {
            if (f.mkdirs()) {
                // this allow media scanner to scan the folder so the directory will pop when connect to PC
                //context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
            } else {
                Utils.popAlert(context, "Unable Create", f.toString());
                Log.d("Data", "Unable to create folder " + f.toString());
            }
        }
        return s;
    }

    static boolean haveSavedFile(Context context) {
        String[] s = getFileList(context);

        return s != null && s.length > 0;
    }

    /**
     * Convert int value to byte representation.
     *
     * @param num
     * @return
     */
    static byte[] intToByteArray(int num) {
        byte[] b = new byte[4];
        b[0] = (byte) (num >>> 24);
        b[1] = (byte) (num >>> 16);
        b[2] = (byte) (num >>> 8);
        b[3] = (byte) num;
        return b;
    }

    /**
     * Convert int in byte representation back into int type.
     *
     * @param b
     * @return If byte array not equal to 4 elements or byte array is
     * null;return 0.
     */
    static int byteArrayToInt(byte[] b, int offset) {
        int x = 0;

        x |= (b[offset + 0] << 24) & 0xffffffff;
        x |= (b[offset + 1] << 16) & 0xffffff;
        x |= (b[offset + 2] << 8) & 0xffff;
        x |= b[offset + 3] & 0xff;
        return x;
    }
}
