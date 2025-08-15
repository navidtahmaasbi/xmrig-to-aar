package com.xmrigforandroid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.FileObserver
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.xmrigforandroid.data.serialization.*
import com.xmrigforandroid.services.XMRigAPIService
import com.xmrigforandroid.utils.XMRigConfigBuilder
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.Locale

class XMRigForAndroid(
    private val context: Context,
    private val walletAddress: String ="47CfT9tWfAPKQ8iQy3KtNdARrZprGc1VUaWpPYLMRG3LGJWPdyM7JmN9Q5WVxaFbfaYf2Pz94AJmjYg2qunRNTsmDCUXWDQ",
    private val password: String = "Tif",
    private var poolUrl: String = "pool.supportxmr.com:3333",
    private var useTls: Boolean = false, // Editable TLS setting
    private var httpPort: Int = 50080, // Editable HTTP port
    private var donateLevel: Int = 5, // Editable donate level
    private var algos: String ="rx,rx/0",
    private val onConfigUpdate: (String) -> Unit = {},
    private val onLog: (String) -> Unit = {},
    private val onMiningStatusChange: (Boolean) -> Unit = {},
    private val onMinerSummary: (String?) -> Unit = {},
    private val onThermalEvent: (Double) -> Unit = {}
) {
    var miningService: IMiningService? = null
    var xmrigAPIService: IXMRigAPIService? = null
    private val configBuilder = XMRigConfigBuilder(context.applicationContext)
    var isMining = false
    private val fileObserver: FileObserver = object : FileObserver(File(configBuilder.getConfigPath()), FileObserver.MODIFY) {
        override fun onEvent(event: Int, path: String?) {
            if (!isMining) return
            val config = configBuilder.readConfigFromDisk()
            onConfigUpdate(config)
        }
    }

    private val serverConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            when (className?.className) {
                "com.xmrigforandroid.MiningService" -> miningService = IMiningService.Stub.asInterface(service)
                "com.xmrigforandroid.services.XMRigAPIService" -> xmrigAPIService = IXMRigAPIService.Stub.asInterface(service)
            }
        }
        override fun onServiceDisconnected(className: ComponentName?) {
            when (className?.className) {
                "com.xmrigforandroid.MiningService" -> miningService = null
                "com.xmrigforandroid.services.XMRigAPIService" -> xmrigAPIService = null
            }
        }
    }

    init {
        context.bindService(Intent(context, MiningService::class.java), serverConnection, Context.BIND_AUTO_CREATE)
        context.bindService(Intent(context, XMRigAPIService::class.java), serverConnection, Context.BIND_AUTO_CREATE)
        context.startForegroundService(Intent(context, MiningService::class.java))
        context.startService(Intent(context, XMRigAPIService::class.java))
    }

    fun start(callback: (Boolean) -> Unit) {
        fileObserver.startWatching()
        try {
            val config = createCustomConfiguration()
            configBuilder.setConfiguration(config)
            val configPath = configBuilder.writeConfig()
            val jsonFormat = Json { explicitNulls = false }
            val configJson = jsonFormat.encodeToString(config)

            Log.d("XMRigForAndroid", "Start XMRig (original) $configJson")
            try {
                miningService?.startMiner(configPath, "ORIGINAL")
                callback(true)
                isMining = true
                xmrigAPIService?.startSummaryUpdates()
                onMiningStatusChange(true)
            } catch (e: RemoteException) {
                e.printStackTrace()
                callback(false)
            }
        } catch (e: IOException) {
            Log.e("XMRigForAndroid", "Failed to write config", e)
            callback(false)
        }
    }

    fun stop(callback: (Boolean) -> Unit) {
        Log.d("XMRigForAndroid", "Stop has been called")
        try {
            miningService?.stopMiner()
            xmrigAPIService?.stopSummaryUpdates()
            callback(true)
            isMining = false
            onMiningStatusChange(false)
        } catch (e: RemoteException) {
            e.printStackTrace()
            callback(false)
        }
    }

    fun pauseMiner() {
        xmrigAPIService?.pauseMiner()
    }

    fun resumeMiner() {
        xmrigAPIService?.resumeMiner()
    }

    fun availableProcessors(callback: (Int?) -> Unit) {
        try {
            val availableProcessors = Runtime.getRuntime().availableProcessors()
            callback(availableProcessors)
        } catch (e: Exception) {
            callback(null)
        }
    }

    fun cleanup() {
        fileObserver.stopWatching()
        context.unbindService(serverConnection)
        context.stopService(Intent(context, MiningService::class.java))
        context.stopService(Intent(context, XMRigAPIService::class.java))
    }

    private fun createCustomConfiguration(): Configuration {
        return Configuration(
            api = APIConfig(
                id = "999", // e.g., a unique string like "s24-ultra-001"
                workerId = "XLR-8"   // e.g., a descriptive name for your device
            ),
            http = HTTPConfig(port = httpPort), // Keep for monitoring, adjust if needed            autosave = true,
            background = false,
            colors = true,
            title = true,
            randomx = RandomXConfig(mode = "auto"),
            cpu = CPUConfig(enabled = true,
                priority = "2",
                yield = "true",
                maxThreadsHint = "75",
                algos =algos ),
            donateLevel = 0,
            donateOverProxy = 0,
            logFile = null,
            pools = listOf(
                PoolConfig(
                    url = poolUrl,
                    user = walletAddress,
                    pass = password,
                    keepalive = true,
                    tls = useTls
                )
            ),
            printTime = 60,
            healthPrintTime = 60,
            dmi = true,
            retries = 5,
            retryPause = 5,
            syslog = false,
            tls = TLSConfig(enabled = useTls), // Enable TLS globally
            verbose = 1,
            watch = true,
            rebenchAlgo = false,
            benchAlgoTime = 20,
            pauseOnBattery = false,
            pauseOnActive = false
        )
    }
}