package com.tofuken.securekeyring;

/**
 * Created by Ken on 8/2/2016.
 */
public class ItemBean {
    boolean isEncrypted = false;
    int id = 0;
    String name = "";
    String key = "";
    String comment = "";

    public String toString() {
        return "Id:" + id + ", Name:" + name + ", Comment:" + comment + ", Encrypted:" + isEncrypted;
    }
}
