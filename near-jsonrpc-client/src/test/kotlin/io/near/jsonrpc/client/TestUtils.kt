package io.near.jsonrpc.client

import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import io.near.jsonrpc.types.JsonRpcRequest

/**
 * Test utilities and helper functions
 */
object TestUtils {
    
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }
    
    /**
     * Retry a suspending function with exponential backoff
     */
    suspend fun <T> retryWithBackoff(
        maxAttempts: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        var lastException: Exception? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: RpcException) {
                lastException = e
                
                // Check if it's a rate limit error
                if (e.message?.contains("429") == true || e.message?.contains("Rate limit") == true) {
                    if (attempt < maxAttempts - 1) {
                        println("Rate limited, retrying after ${currentDelay}ms (attempt ${attempt + 1}/$maxAttempts)")
                        delay(currentDelay)
                        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
                    }
                } else {
                    // Not a rate limit error, throw immediately
                    throw e
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxAttempts - 1) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
                }
            }
        }
        
        throw lastException ?: Exception("Retry failed")
    }
    
    /**
     * Create a test account ID
     */
    fun createTestAccountId(): String {
        return "test-${System.currentTimeMillis()}.near"
    }
    
    /**
     * Create a block reference for testing
     */
    fun createBlockReference(
        finality: String? = null,
        blockId: String? = null,
        blockHeight: Long? = null
    ): JsonObject {
        return buildJsonObject {
            finality?.let { put("finality", it) }
            blockId?.let { put("block_id", it) }
            blockHeight?.let { put("block_height", it) }
        }
    }
    
    /**
     * Create test transaction data
     */
    fun createTestTransaction(): JsonObject {
        return buildJsonObject {
            put("signer_id", "test.near")
            put("public_key", "ed25519:test_key")
            put("nonce", 1)
            put("receiver_id", "receiver.near")
            putJsonObject("actions") {
                put("type", "Transfer")
                put("amount", "1000000000000000000000000")
            }
        }
    }
    
    /**
     * Create mock RPC response
     */
    fun createMockRpcResponse(
        id: String = "1",
        result: JsonElement? = null,
        error: JsonElement? = null
    ): String {
        return buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", id)
            if (result != null) {
                put("result", result)
            }
            if (error != null) {
                put("error", error)
            }
        }.toString()
    }
    
    /**
     * Create mock network status response
     */
    fun createMockNetworkStatus(): String {
        return createMockRpcResponse(
            result = buildJsonObject {
                putJsonObject("version") {
                    put("version", "1.35.0")
                    put("build", "test")
                    put("rustc_version", "1.70.0")
                }
                put("chain_id", "testnet")
                put("protocol_version", 63)
                put("latest_protocol_version", 63)
                put("rpc_addr", "0.0.0.0:3030")
                put("validators", JsonArray(emptyList()))
                putJsonObject("sync_info") {
                    put("latest_block_hash", "test_hash")
                    put("latest_block_height", 100000)
                    put("latest_state_root", "test_state_root")
                    put("latest_block_time", "2024-01-01T00:00:00Z")
                    put("syncing", false)
                    put("earliest_block_hash", "earliest_hash")
                    put("earliest_block_height", 1)
                    put("earliest_block_time", "2023-01-01T00:00:00Z")
                    put("epoch_id", "test_epoch")
                    put("epoch_start_height", 99000)
                }
                put("validator_account_id", JsonNull)
                put("node_public_key", "test_key")
                put("uptime_sec", 3600)
            }
        )
    }
    
    /**
     * Create mock account view response
     */
    fun createMockAccountView(
        accountId: String = "test.near",
        amount: String = "1000000000000000000000000",
        locked: String = "0"
    ): String {
        return createMockRpcResponse(
            result = buildJsonObject {
                put("amount", amount)
                put("locked", locked)
                put("code_hash", "11111111111111111111111111111111")
                put("storage_usage", 500)
                put("storage_paid_at", 0)
                put("block_height", 100000)
                put("block_hash", "test_block_hash")
            }
        )
    }
    
    /**
     * Create mock block response
     */
    fun createMockBlock(height: Long = 100000): String {
        return createMockRpcResponse(
            result = buildJsonObject {
                put("author", "test_validator.near")
                putJsonObject("header") {
                    put("height", height)
                    put("hash", "test_hash_$height")
                    put("prev_hash", "test_prev_hash")
                    put("epoch_id", "test_epoch")
                    put("next_epoch_id", "next_epoch")
                    put("chunks_included", 1)
                    put("challenges_root", "test_challenges")
                    put("timestamp", System.currentTimeMillis())
                    put("timestamp_nanosec", "0")
                    put("random_value", "random")
                    putJsonObject("validator_proposals") {
                        // Empty proposals
                    }
                }
                put("chunks", JsonArray(emptyList()))
            }
        )
    }
    
    /**
     * Create mock error response
     */
    fun createMockErrorResponse(
        code: Int = -32603,
        message: String = "Internal error",
        data: String? = null
    ): String {
        return createMockRpcResponse(
            error = buildJsonObject {
                put("code", code)
                put("message", message)
                if (data != null) {
                    put("data", data)
                }
            }
        )
    }
    
    /**
     * Check if an RPC request matches expected format
     */
    fun isValidRpcRequest(
        request: String,
        expectedMethod: String,
        expectedParams: Any? = null
    ): Boolean {
        val parsed = json.decodeFromString<JsonRpcRequest>(request)
        
        return parsed.jsonrpc == "2.0" &&
               parsed.method == expectedMethod &&
               parsed.id != null &&
               (expectedParams == null || parsed.params == expectedParams)
    }
    
    /**
     * Create a client with custom timeout
     */
    fun createClientWithTimeout(
        url: String,
        connectTimeout: Long = 5,
        readTimeout: Long = 5,
        writeTimeout: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS
    ): NearRpcClient {
        val client = OkHttpClient.Builder()
            .connectTimeout(connectTimeout, unit)
            .readTimeout(readTimeout, unit)
            .writeTimeout(writeTimeout, unit)
            .build()
        return NearRpcClient(url, client)
    }
    
    /**
     * Alternative testnet RPC endpoints for testing
     */
    object AlternativeEndpoints {
        const val NEAR_TESTNET = "https://rpc.testnet.near.org"
        const val PAGODA_TESTNET = "https://near-testnet.api.pagoda.co/rpc/v1/"
        const val NEAR_MAINNET = "https://rpc.mainnet.near.org"
        const val PAGODA_MAINNET = "https://near-mainnet.api.pagoda.co/rpc/v1/"
    }
}