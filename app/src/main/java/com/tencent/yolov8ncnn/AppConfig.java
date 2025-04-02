package com.tencent.yolov8ncnn;

public class AppConfig {
    private static AppConfig instance;
    private boolean showHandCardOverlay;

    private AppConfig() {}

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public boolean isShowHandCardOverlay() {
        return showHandCardOverlay;
    }

    public void setShowHandCardOverlay(boolean showHandCardOverlay) {
        this.showHandCardOverlay = showHandCardOverlay;
    }
}