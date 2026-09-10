package com.workaround.spectv;

public class Item {

    private int androidVersionImage;
    private String androidVersionName;
    private String androidVersionNumber;

    // Constructor
    public Item(int androidVersionImage, String androidVersionName, String androidVersionNumber) {
        this.androidVersionImage = androidVersionImage;
        this.androidVersionName = androidVersionName;
        this.androidVersionNumber = androidVersionNumber;

    }

    // Getters and Setters method
    public int getAndroidVersionImage() {
        return androidVersionImage;
    }

    public void setAndroidVersionImage(int androidVersionImage) {
        this.androidVersionImage = androidVersionImage;
    }

    public String getAndroidVersionName() {
        return androidVersionName;
    }

    public void setAndroidVersionName(String androidVersionName) {
        this.androidVersionName = androidVersionName;
    }

    public String getAndroidVersionNumber() {
        return androidVersionNumber;
    }

    public void setAndroidVersionNumber(String androidVersionNumber) {
        this.androidVersionNumber = androidVersionNumber;
    }
}
