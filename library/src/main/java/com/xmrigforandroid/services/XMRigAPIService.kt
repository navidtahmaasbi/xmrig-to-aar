package com.xmrigforandroid.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.xmrigforandroid.workers.XMRigJsonRpcWorker
import com.xmrigforandroid.workers.XMRigSummaryUpdateWorker
import okhttp3.OkHttpClient
import android.os.CountDownTimer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.xmrigforandroid.IXMRigAPIService

class XMRigAPIService : Service() {

    private val client = OkHttpClient()
    private var isSummaryUpdate = false
    private lateinit var summaryUpdateTimer: CountDownTimer
    private lateinit var summaryUpdateWorkerRequest: OneTimeWorkRequest

    override fun onCreate() {
        super.onCreate()
        summaryUpdateWorkerRequest = OneTimeWorkRequestBuilder<XMRigSummaryUpdateWorker>().build()
        summaryUpdateTimer = object : CountDownTimer(10000, 10000) {
            override fun onTick(millisUntilFinished: Long) {
                // No action needed on tick
            }

            override fun onFinish() {
                Log.d(LOG_TAG, "summaryUpdateTimer::Finish")
                WorkManager.getInstance(applicationContext).enqueue(summaryUpdateWorkerRequest)
                if (isSummaryUpdate) {
                    this.start()
                }
            }
        }
    }

    fun sendJSONRpcCommand(method: String) {
        Log.d(LOG_TAG, "sendJSONRpcCommand: $method")
        val jsonRpcWorkerRequest = OneTimeWorkRequestBuilder<XMRigJsonRpcWorker>()
            .setInputData(workDataOf("METHOD" to method))
            .build()
        WorkManager.getInstance(applicationContext).enqueue(jsonRpcWorkerRequest)
    }

    private val binder = object : IXMRigAPIService.Stub() {
        override fun pauseMiner() {
            Log.d(LOG_TAG, "pauseMiner")
            sendJSONRpcCommand("pause")
        }

        override fun resumeMiner() {
            Log.d(LOG_TAG, "resumeMiner")
            sendJSONRpcCommand("resume")
        }

        override fun startSummaryUpdates() {
            Log.d(LOG_TAG, "startSummaryUpdates")
            isSummaryUpdate = true
            summaryUpdateTimer.start()
        }

        override fun stopSummaryUpdates() {
            Log.d(LOG_TAG, "stopSummaryUpdates")
            isSummaryUpdate = false
            summaryUpdateTimer.cancel()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        summaryUpdateTimer.cancel()
        super.onDestroy()
    }

    companion object {
        private val LOG_TAG = "XMRigAPIService"
        var IS_SERVICE_RUNNING = false
    }
}