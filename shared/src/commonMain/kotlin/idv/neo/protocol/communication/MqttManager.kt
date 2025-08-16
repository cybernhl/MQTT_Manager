package idv.neo.protocol.communication

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import idv.neo.utils.log.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await

public class MqttManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var client: Mqtt3AsyncClient? = null

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val defaultTestConfig by lazy {
        MqttConfig(
            host = "a4773b5e72ae4f9fa90ae9b8fe8cf34e.s1.eu.hivemq.cloud",
            port = 8883,
            username = "SDK_DEMO_TH_1",
            password = "m7W\$4530@uVy".toByteArray(),
            clientIdPrefix = "sdk-test-client"
        )
    }
    private val _totalBytesSent = MutableStateFlow(0L)
    val totalBytesSent: StateFlow<Long> = _totalBytesSent.asStateFlow()

    private val _totalBytesReceived = MutableStateFlow(0L)
    val totalBytesReceived: StateFlow<Long> = _totalBytesReceived.asStateFlow()
    private val _totalBytesSentFormatted = MutableStateFlow(formatBytes(0L))
    val totalBytesSentFormatted: StateFlow<String> = _totalBytesSentFormatted.asStateFlow()

    private val _totalBytesReceivedFormatted = MutableStateFlow(formatBytes(0L))
    val totalBytesReceivedFormatted: StateFlow<String> = _totalBytesReceivedFormatted.asStateFlow()

    private var currentConfig: MqttConfig? = null

    suspend fun connect(config: MqttConfig? = null): Boolean {
        currentConfig = config ?: defaultTestConfig
        val activeConfig = currentConfig ?: return false
        if (_connectionState.value && client != null) {
            // 可以加入邏輯：如果 activeConfig 和上次的不同，則先 disconnect 再 connect
            // println("Already connected.")
            // return true // 或者根據需求決定是否需要重新連線
        }

        client?.let {
            if (it.state.isConnectedOrReconnect) {
                try {
                    it.disconnect().await()
                } catch (e: Exception) {
                    // Log or handle disconnect error if necessary
                    Log.e("Error disconnecting previous client: ${e.message}")
                }
            }
        }
        client = null

        _connectionState.value = false

        try {
            val clientBuilder = MqttClient.builder()
                .useMqttVersion3()
                .identifier("${activeConfig.clientIdPrefix}-${System.currentTimeMillis()}")
                .serverHost(activeConfig.host)
                .serverPort(activeConfig.port)

            if (activeConfig.useSsl) {
                clientBuilder.sslWithDefaultConfig()
            }

            clientBuilder.simpleAuth()
                .username(activeConfig.username)
                .password(activeConfig.password)
                .applySimpleAuth()


            clientBuilder
                .addConnectedListener { context ->
                    Log.i("MQTT Connected: ${context.clientConfig.serverHost}")
                    _connectionState.value = true
                }
                .addDisconnectedListener { context ->
                    Log.w("MQTT Disconnected: ${context.clientConfig.serverHost}, Cause: ${context.cause}")
                    _connectionState.value = false
                    // 可選：根據斷線原因決定是否嘗試重連或通知上層
                }

            val newClient = clientBuilder.buildAsync()
            val connAck: Mqtt3ConnAck = newClient.connect().await()

            if (connAck.returnCode.isError) {
                _connectionState.value = false
                Log.w("Connection failed: ${connAck.returnCode}")
                client = null // 連線失敗，清除 client
                return false
            }
            // 連線成功後才賦值給成員變數 client
            this.client = newClient
            // _connectionState.value 已由 ConnectedListener 設定為 true
            Log.i("Successfully connected to ${activeConfig.host}")
            return true

        } catch (e: Exception) {
            _connectionState.value = false
            Log.e("Connection exception: ${e.message}")
            e.printStackTrace()
            client = null // 異常時清除 client
            return false
        }
    }

    suspend fun publish(
        topic: String,
        message: String,
        qos: MqttQos = MqttQos.AT_LEAST_ONCE,
        retain: Boolean = false
    ) {
        // 確保 client 已初始化且已連線
        val currentClient = client
        if (currentClient == null || !_connectionState.value) {
            Log.e("Cannot publish: Client not connected.")
            return
        }

        try {
            val payloadBytes = message.toByteArray(Charsets.UTF_8)
            val payloadSize = payloadBytes.size.toLong()
            Log.d(
                "Publishing to topic '$topic'. Message size: $payloadSize bytes. Message: ${
                    message.take(
                        50
                    )
                }..."
            )
            currentClient.publishWith()
                .topic(topic)
                .payload(payloadBytes)
                .qos(qos)
                .retain(retain)
                .send()
                .await()
            val newTotalSent = _totalBytesSent.value + payloadSize
            _totalBytesSent.value = newTotalSent
            _totalBytesSentFormatted.value = formatBytes(newTotalSent)

            Log.i("Message published to '$topic'. Total bytes sent: ${getTotalBytesSentFormattedInternal()}") // 使用內部方法
        } catch (e: Exception) {
            Log.i("Publish failed: ${e.message}")
            e.printStackTrace()
        }
    }

    fun subscribe(topic: String, qos: MqttQos = MqttQos.AT_LEAST_ONCE): Flow<String> =
        callbackFlow {
            val currentClient = client
            if (currentClient == null || !_connectionState.value) {
                Log.i("Cannot subscribe: Client not connected.")
                close(IllegalStateException("Client not connected or not initialized")) // 關閉 Flow 並帶上原因
                return@callbackFlow
            }

            val individualMessageCallback: (com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish) -> Unit =
                { mqtt3Publish ->
                    val receivedPayloadBytes = mqtt3Publish.payloadAsBytes
                    val receivedPayloadSize = receivedPayloadBytes?.size?.toLong() ?: 0L
                    val receivedTopic = mqtt3Publish.topic.toString()
                    // 更新 StateFlow
                    val newTotalReceived = _totalBytesReceived.value + receivedPayloadSize
                    _totalBytesReceived.value = newTotalReceived
                    _totalBytesReceivedFormatted.value = formatBytes(newTotalReceived)

                    Log.d("Message received on data channel '${receivedTopic}'. Size: $receivedPayloadSize bytes. Total received: ${getTotalBytesReceivedFormattedInternal()}") // 使用內部方法

                    if (receivedTopic == topic) {
                        val receivedMessage = String(mqtt3Publish.payloadAsBytes, Charsets.UTF_8)
                        Log.i("Message received on topic '$receivedTopic': $receivedMessage")
                        trySend(receivedMessage).isSuccess
                    } else {
                        Log.w("DEBUG: Received message on topic '$receivedTopic' but expecting '$topic'. This might indicate a logic issue or shared callback.")
                    }
                }

            val subAckFuture = currentClient.subscribeWith()
                .topicFilter(topic)
                .qos(qos)
                .callback(individualMessageCallback)
                .send()


            scope.launch {
                try {
                    val subAck = subAckFuture.await()
                    if (subAck.returnCodes.any { it.isError }) {
                        val errorCodes = subAck.returnCodes.joinToString()
                        Log.e("Subscription to '$topic' failed: $errorCodes")
                        close(RuntimeException("Subscription failed for $topic: $errorCodes"))
                    } else {
                        Log.i("Successfully subscribed to topic '$topic'")
                    }
                } catch (e: Exception) {
                    Log.e("Subscription to '$topic' exception: ${e.message}")
                    close(e) // 關閉 Flow
                }
            }

            awaitClose {
                Log.i("Closing subscription for topic '$topic'. Unsubscribing...")
                client?.let { c ->
                    if (c.state.isConnectedOrReconnect) {
                        c.unsubscribeWith().topicFilter(topic).send()
                            .whenComplete { unsubAck, throwable ->
                                if (throwable != null) {
                                    Log.e("Error unsubscribing from topic '$topic': ${throwable.message}")
                                } else {
                                    Log.i("Successfully sent unsubscribe request for topic '$topic'. unsubAck: $unsubAck")
                                }
                            }
                    } else {
                        Log.e("Client not connected, cannot unsubscribe from topic '$topic'")
                    }
                }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun disconnect() {
        val currentClient = client
        if (currentClient != null && currentClient.state.isConnectedOrReconnect) {
            try {
                currentClient.disconnect().await()
                Log.i("Disconnected successfully.")
            } catch (e: Exception) {
                Log.e("Disconnect exception: ${e.message}")
                e.printStackTrace()
            }
        }
        _connectionState.value = false
        client = null // 清理 client
        currentConfig = null
    }

    fun getTotalBytesSent(): Long = _totalBytesSent.value
    fun getTotalBytesReceived(): Long = _totalBytesReceived.value

    fun resetTrafficStats() {
        _totalBytesSent.value = 0L
        _totalBytesReceived.value = 0L
        _totalBytesSentFormatted.value = formatBytes(0L)
        _totalBytesReceivedFormatted.value = formatBytes(0L)
        Log.i("Core: Traffic statistics have been reset.")
    }

    fun getTotalMegabytesSent(): Double = _totalBytesSent.value / (1024.0 * 1024.0)

    private fun getTotalBytesSentFormattedInternal(): String {
        return formatBytes(_totalBytesSent.value)
    }

    private fun getTotalBytesReceivedFormattedInternal(): String {
        return formatBytes(_totalBytesReceived.value)
    }

    fun cleanup() {
        scope.cancel() // 取消所有由這個 scope 啟動的協程
        // client?.disconnect() // 確保 client 斷開，如果 disconnect() 沒被呼叫
        Log.i("MqttManager cleaned up.")
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "Invalid size"
    if (bytes < 1024) return "$bytes Bytes"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.2f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.2f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}