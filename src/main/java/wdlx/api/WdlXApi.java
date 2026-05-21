package wdlx.api;

import wdlx.WorldDownloadX;

public interface WdlXApi {
    static WdlXApi getInstance() {
        return WorldDownloadX.API;
    }

    boolean isDownloading();
    Session activeSession();
}
