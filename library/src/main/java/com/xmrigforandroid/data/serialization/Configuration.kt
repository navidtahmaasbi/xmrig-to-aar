package com.xmrigforandroid.data.serialization

import androidx.annotation.Keep
import kotlinx.serialization.*

@Keep
@Serializable
enum class ConfigurationMode {
    @SerialName("simple") SIMPLE,
    @SerialName("advanced") ADVANCE
}

@Keep
@Serializable
enum class XMRigFork {
    @SerialName("original") ORIGINAL,
    @SerialName("moneroocean") MONEROOCEAN
}

@Keep
@Serializable
enum class RandomXMode {
    @SerialName("auto") AUTO,
    @SerialName("fast") FAST,
    @SerialName("light") LIGHT
}

@Keep
@Serializable
data class PoolConfig(
    val algo: String? = null,
    val coin: String? = null,
    val url: String? = null,
    val user: String? = null,
    val pass: String? = null,
    val rigId: String? = null,
    val nicehash: Boolean = false,
    val keepalive: Boolean = true,
    val enabled: Boolean = true,
    val tls: Boolean = false,
    val tlsFingerprint: String? = null,
    val daemon: Boolean = false,
    val socks5: String? = null,
    val selfSelect: String? = null,
    val submitToOrigin: Boolean = false
)

@Keep
@Serializable
data class RandomXConfig(
    val init: Int = -1,
    val initAvx2: Int = 0,
    val mode: String? = "auto",
    val gbPages: Boolean = false,
    val rdmsr: Boolean = true,
    val wrmsr: Boolean = true,
    val cacheQos: Boolean = false,
    val numa: Boolean = true,
    val scratchpadPrefetchMode: Int = 1
)

@Keep
@Serializable
data class CPUConfig(
    val enabled: Boolean = true,
    val hugePages: Boolean = true,
    val hugePagesJit: Boolean = false,
    val hwAes: String? = null,
    val priority: String? = "2",
    val memoryPool: Boolean = true,
    val yield: String? = "true",
    val maxThreadsHint: String? = "75",
    val asm: Boolean = true,
    val argon2Impl: String? = null,
    val astrobwtMaxSize: Int = 100,
    val astrobwtAvx2: Boolean = false,
    val algos: String? = ""
)

@Keep
@Serializable
data class HTTPConfig(
    val enabled: Boolean = true,
    val host: String = "127.0.0.1",
    val port: Int = 50080,
    val accessToken: String? = null,
    val restricted: Boolean = true
)

@Keep
@Serializable
data class TLSConfig(
    val enabled: Boolean = false,
    val protocols: String? = null,
    val cert: String? = null,
    val certKey: String? = null,
    val ciphers: String? = null,
    val ciphersuites: String? = null,
    val dhparam: String? = null
)

@Keep
@Serializable
data class Configuration(
    val api: APIConfig? = APIConfig(),
    val http: HTTPConfig? = HTTPConfig(),
    val autosave: Boolean = true,
    val background: Boolean = false,
    val colors: Boolean = true,
    val title: Boolean = true,
    val randomx: RandomXConfig? = RandomXConfig(),
    val cpu: CPUConfig? = CPUConfig(),
    val donateLevel: Int = 0,
    val donateOverProxy: Int = 0,
    val logFile: String? = null,
    val pools: List<PoolConfig>? = listOf(PoolConfig()),
    val printTime: Int = 60,
    val healthPrintTime: Int = 60,
    val dmi: Boolean = true,
    val retries: Int = 5,
    val retryPause: Int = 5,
    val syslog: Boolean = false,
    val tls: TLSConfig? = TLSConfig(),
    val userAgent: String? = null,
    val verbose: Int = 1,
    val watch: Boolean = true,
    val rebenchAlgo: Boolean = false,
    val benchAlgoTime: Int = 20,
    val pauseOnBattery: Boolean = false,
    val pauseOnActive: Boolean = false
)

@Keep
@Serializable
data class APIConfig(
    val id: String? = null,
    val workerId: String? = null
)