package com.xmrigforandroid;

interface IXMRigAPIService {
    void startSummaryUpdates();
    void stopSummaryUpdates();
    void pauseMiner();
    void resumeMiner();
}