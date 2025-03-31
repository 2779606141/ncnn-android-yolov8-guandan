package com.tencent.yolov8ncnn;

public class User {
    private String username;
    private String userpassword;
    private byte isadmin;

    public User(String username, String userpassword, byte isAdmin) {
        this.username = username;
        this.userpassword = userpassword;
        this.isadmin = isAdmin;
    }
    public byte getIsAdmin() {
        return isadmin;
    }
    public void setIsAdmin(byte isAdmin) {
        this.isadmin = isAdmin;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserpassword() {
        return userpassword;
    }

    public void setUserpassword(String userpassword) {
        this.userpassword = userpassword;
    }
}