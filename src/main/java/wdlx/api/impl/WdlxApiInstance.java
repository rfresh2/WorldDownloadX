package wdlx.api.impl;

import wdlx.WorldDownloadManager;
import wdlx.WorldDownloadX;
import wdlx.api.Session;
import wdlx.api.WdlXApi;

public class WdlxApiInstance implements WdlXApi {
    @Override
    public boolean isDownloading() {
        return WorldDownloadX.WDL_MANAGER.getStatus() == WorldDownloadManager.DownloadStatus.STARTED;
    }

    @Override
    public Session activeSession() {
        return WorldDownloadX.WDL_MANAGER.getCurrentSession();
    }
}
