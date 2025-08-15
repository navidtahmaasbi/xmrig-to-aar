package com.xmrigforandroid.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class XMRigSummaryUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val onMinerSummary: (String?) -> Unit
    ):
        CoroutineWorker(appContext, workerParams) {

    private val client = OkHttpClient()

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            Log.d(XMRigSummaryUpdateWorker.LOG_TAG, "doSummaryUpdate")
            val request = Request.Builder()
                    .url("http://127.0.0.1:50080/2/summary")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer XMRigForAndroid")
                    .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code $response")
                    }
                    val summary = response.body!!.string()
                    onMinerSummary(summary) // Notify via callback
                    Result.success()
                }
            } catch (e: IOException) {
                Log.e(XMRigSummaryUpdateWorker.LOG_TAG, "Error fetching summary", e)
                Result.failure()
            }
        }
    }

    companion object {
        val LOG_TAG = "XMRigSummaryUpdateWorker"
    }
}
