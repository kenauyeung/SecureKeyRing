package com.tofuken.securekeyring;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.spongycastle.util.encoders.Base64;

/**
 * Created by Ken on 8/2/2016.
 */
public class KeyContainer {

    byte[] keyByte = null;
    String keyFilename = "";
    // final String ENCODE = "UTF-8";
    int idCounter = 1;
    ArrayList<ItemBean> list = new ArrayList();

    public void addItem(String name, String key, String comment) {
        addItem(idCounter++, false, name, key, comment);
    }

    public void addItem(ItemBean bean) {
        Log.d("Data", "Add new Item:" + bean);
        bean.id = idCounter++;
        bean.isEncrypted = false;
        list.add(bean);
    }

    private void addItem(int id, boolean isEncrypt, String name, String key, String comment) {
        ItemBean item = new ItemBean();
        item.id = id;
        item.isEncrypted = isEncrypt;
        item.name = name;
        item.key = key;
        item.comment = comment;
        list.add(item);
    }

    public void loadList(byte[] b) throws UnsupportedEncodingException {
        String str = new String(b);
       // Log.d("Data", "Read->" + str);
        String[] arr = str.split("\n");
        list.clear();
        if (arr != null) {
            idCounter = Integer.parseInt(arr[0].trim());
            for (int i = 1; i < arr.length; i++) {
                String[] tmp = arr[i].split(",");
               // Log.d("Data", "Read-> Item->" + tmp.length);
                if (tmp.length > 3) {
                    addItem(Integer.parseInt(tmp[0]),
                            tmp[1].equals("1"),
                            getDecode(tmp[2]),
                            getDecode(tmp[3]),
                            tmp.length > 4 ? getDecode(tmp[4]) : ""); // when comment empty, the split will not generate this item
                }
            }
        }
    }

    public byte[] getBytes() throws UnsupportedEncodingException {
        return getBytes(false);
    }

    public byte[] getBytes(boolean isNew) throws UnsupportedEncodingException {


        String str = String.valueOf(isNew ? 1 : idCounter);
        if (!isNew && list != null && !list.isEmpty()) {
            for (ItemBean item : list) {
                str += "\n" + item.id
                        + "," + (item.isEncrypted ? "1" : "0")
                        + "," + getEncode(item.name)
                        + "," + getEncode(item.key)
                        + "," + getEncode(item.comment); // ensure even comment is empty the array split will work accordingly
            }
        }
        Log.d("Data", "Get Byte: " + isNew + "->" + str);
        return str.getBytes();
    }

    private String getEncode(String s) throws UnsupportedEncodingException {
        if (s != null && !s.isEmpty()) {
            return new String(Base64.encode(s.getBytes()));
        }
        return "";
    }

    private String getDecode(String s) throws UnsupportedEncodingException {
        if (s != null && !s.isEmpty()) {
            return new String(Base64.decode(s.getBytes()));
        }
        return "";
    }

    public void reset() {
        list.clear();
        idCounter = 1;
        keyByte = null;
        keyFilename = "";
    }

    public String toString() {
        return "Key File:" + keyFilename + ",Counter:" + idCounter + ", Item:" + list;
    }
}
