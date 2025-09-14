package io.near.jsonrpc.client

import io.near.jsonrpc.types.JsonRpcRequest
import io.near.jsonrpc.types.JsonRpcResponse
import io.near.jsonrpc.types.JsonRpcError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * NEAR JSON-RPC client for interacting with NEAR Protocol nodes.
 * 
 * This client provides type-safe methods for all NEAR RPC endpoints.
 * All methods are suspend functions for coroutine-based async operations.
 * 
 * @param rpcUrl The URL of the NEAR RPC endpoint (default: mainnet)
 * @param httpClient Optional custom OkHttpClient for advanced configuration
 */
class NearRpcClient(
    private val rpcUrl: String = "https://rpc.mainnet.near.org",
    private val httpClient: OkHttpClient = defaultHttpClient()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    companion object {
        private fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }

    /**
     * Returns general status of the node and information about the current chain.
     */
    suspend fun status(): JsonElement = rpcCall("status", null)

    /**
     * Returns network information.
     */
    suspend fun networkInfo(): JsonElement = rpcCall("network_info", null)

    /**
     * Returns gas price for a specific block.
     * @param blockId Optional block ID (hash or height). Uses latest block if null.
     */
    suspend fun gasPrice(blockId: String? = null): JsonElement {
        val params = if (blockId != null) {
            listOf(blockId)
        } else {
            listOf(JsonNull)
        }
        return rpcCall("gas_price", params)
    }

    /**
     * Returns block details by ID.
     * @param blockId Block hash, block height, or finality ("final", "optimistic")
     */
    suspend fun block(blockId: String): JsonElement {
        val params = when (blockId) {
            "final", "optimistic" -> buildJsonObject {
                put("finality", blockId)
            }
            else -> {
                // Check if it's a number (block height) or hash
                blockId.toLongOrNull()?.let {
                    buildJsonObject {
                        put("block_id", it)
                    }
                } ?: buildJsonObject {
                    put("block_id", blockId)
                }
            }
        }
        return rpcCall("block", params)
    }

    /**
     * Returns chunk details by ID.
     * @param chunkId Chunk hash or [blockId, shardId]
     */
    suspend fun chunk(chunkId: JsonElement): JsonElement {
        return rpcCall("chunk", chunkId)
    }

    /**
     * Returns transaction status and details.
     * @param txHash Transaction hash
     * @param senderId Sender account ID
     */
    suspend fun transaction(txHash: String, senderId: String): JsonElement {
        val params = listOf(txHash, senderId)
        return rpcCall("tx", params)
    }

    /**
     * Sends a transaction asynchronously (doesn't wait for result).
     * @param signedTx Base64 encoded signed transaction
     * @return Transaction hash
     */
    suspend fun sendTransactionAsync(signedTx: String): JsonElement {
        val params = listOf(signedTx)
        return rpcCall("broadcast_tx_async", params)
    }

    /**
     * Sends a transaction and waits for execution.
     * @param signedTx Base64 encoded signed transaction
     */
    suspend fun sendTransactionCommit(signedTx: String): JsonElement {
        val params = listOf(signedTx)
        return rpcCall("broadcast_tx_commit", params)
    }

    /**
     * Returns validator information for a specific block.
     * @param blockId Optional block ID. Uses latest block if null.
     */
    suspend fun validators(blockId: String? = null): JsonElement {
        val params = if (blockId != null) {
            listOf(blockId)
        } else {
            listOf(JsonNull)
        }
        return rpcCall("validators", params)
    }

    /**
     * Queries information from the NEAR network.
     * Supports various query types like view_account, view_access_key, call_function, etc.
     * @param request Query request parameters
     */
    suspend fun query(request: JsonObject): JsonElement {
        return rpcCall("query", request)
    }

    /**
     * View account details.
     * @param accountId NEAR account ID
     * @param blockId Optional block ID for historical queries
     */
    suspend fun viewAccount(
        accountId: String,
        blockId: String? = null,
        finality: String? = "final"
    ): JsonElement {
        val request = buildJsonObject {
            put("request_type", "view_account")
            put("account_id", accountId)
            blockId?.let { put("block_id", it) }
            finality?.let { if (blockId == null) put("finality", it) }
        }
        return query(request)
    }

    /**
     * View access key details.
     * @param accountId NEAR account ID
     * @param publicKey Public key in base58 format
     * @param blockId Optional block ID for historical queries
     */
    suspend fun viewAccessKey(
        accountId: String,
        publicKey: String,
        blockId: String? = null,
        finality: String? = "final"
    ): JsonElement {
        val request = buildJsonObject {
            put("request_type", "view_access_key")
            put("account_id", accountId)
            put("public_key", publicKey)
            blockId?.let { put("block_id", it) }
            finality?.let { if (blockId == null) put("finality", it) }
        }
        return query(request)
    }

    /**
     * View all access keys for an account.
     * @param accountId NEAR account ID
     * @param blockId Optional block ID for historical queries
     */
    suspend fun viewAccessKeyList(
        accountId: String,
        blockId: String? = null,
        finality: String? = "final"
    ): JsonElement {
        val request = buildJsonObject {
            put("request_type", "view_access_key_list")
            put("account_id", accountId)
            blockId?.let { put("block_id", it) }
            finality?.let { if (blockId == null) put("finality", it) }
        }
        return query(request)
    }

    /**
     * Call a view function on a contract.
     * @param accountId Contract account ID
     * @param methodName Method name to call
     * @param argsBase64 Base64 encoded arguments
     * @param blockId Optional block ID for historical queries
     */
    suspend fun callFunction(
        accountId: String,
        methodName: String,
        argsBase64: String,
        blockId: String? = null,
        finality: String? = "final"
    ): JsonElement {
        val request = buildJsonObject {
            put("request_type", "call_function")
            put("account_id", accountId)
            put("method_name", methodName)
            put("args_base64", argsBase64)
            blockId?.let { put("block_id", it) }
            finality?.let { if (blockId == null) put("finality", it) }
        }
        return query(request)
    }

    /**
     * View contract state.
     * @param accountId Contract account ID
     * @param prefixBase64 Base64 encoded prefix for keys to return
     * @param blockId Optional block ID for historical queries
     */
    suspend fun viewState(
        accountId: String,
        prefixBase64: String = "",
        blockId: String? = null,
        finality: String? = "final"
    ): JsonElement {
        val request = buildJsonObject {
            put("request_type", "view_state")
            put("account_id", accountId)
            put("prefix_base64", prefixBase64)
            blockId?.let { put("block_id", it) }
            finality?.let { if (blockId == null) put("finality", it) }
        }
        return query(request)
    }

    /**
     * Get light client proof for execution outcomes.
     * EXPERIMENTAL: This method may change or be removed in future versions.
     */
    suspend fun lightClientProof(request: JsonObject): JsonElement {
        return rpcCall("EXPERIMENTAL_light_client_proof", request)
    }

    /**
     * Get protocol configuration.
     * EXPERIMENTAL: This method may change or be removed in future versions.
     */
    suspend fun protocolConfig(blockId: String? = null): JsonElement {
        val params = if (blockId != null) {
            buildJsonObject { put("block_id", blockId) }
        } else {
            buildJsonObject { put("finality", "final") }
        }
        return rpcCall("EXPERIMENTAL_protocol_config", params)
    }

    /**
     * Get changes in a block.
     * EXPERIMENTAL: This method may change or be removed in future versions.
     */
    suspend fun changes(blockId: String): JsonElement {
        val params = buildJsonObject {
            put("block_id", blockId)
        }
        return rpcCall("EXPERIMENTAL_changes", params)
    }

    /**
     * Get state changes in a block.
     * EXPERIMENTAL: This method may change or be removed in future versions.
     */
    suspend fun changesInBlock(blockId: String): JsonElement {
        val params = buildJsonObject {
            put("block_id", blockId)
        }
        return rpcCall("EXPERIMENTAL_changes_in_block", params)
    }

    /**
     * Get receipt by ID.
     * EXPERIMENTAL: This method may change or be removed in future versions.
     */
    suspend fun receipt(receiptId: String): JsonElement {
        val params = buildJsonObject {
            put("receipt_id", receiptId)
        }
        return rpcCall("EXPERIMENTAL_receipt", params)
    }

    /**
     * Makes a raw JSON-RPC call to the NEAR node.
     * @param method The RPC method name
     * @param params The parameters for the method (can be null, JsonElement, or List)
     * @return The result as a JsonElement
     */
    private suspend fun rpcCall(
        method: String,
        params: Any?
    ): JsonElement = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString()
        
        val paramsJson = when (params) {
            null -> JsonNull
            is JsonElement -> params
            is List<*> -> JsonArray(params.map { 
                when (it) {
                    null -> JsonNull
                    is String -> JsonPrimitive(it)
                    is Number -> JsonPrimitive(it)
                    is Boolean -> JsonPrimitive(it)
                    is JsonElement -> it
                    else -> json.encodeToJsonElement(it)
                }
            })
            else -> json.encodeToJsonElement(params)
        }

        val requestBody = JsonRpcRequest(
            id = requestId,
            method = method,
            params = paramsJson
        )

        val requestJson = json.encodeToString(JsonRpcRequest.serializer(), requestBody)
        val httpRequest = Request.Builder()
            .url(rpcUrl)
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(httpRequest).execute()
        val responseBody = response.body?.string()
            ?: throw RpcException("Empty response from server")

        if (!response.isSuccessful) {
            throw RpcException("HTTP ${response.code}: $responseBody")
        }

        val jsonResponse = json.decodeFromString<JsonRpcResponse<JsonElement>>(responseBody)

        jsonResponse.error?.let { error ->
            throw RpcException(
                message = "RPC Error: ${error.message}",
                error = error
            )
        }

        jsonResponse.result?.let { result ->
            return@withContext result
        } ?: throw RpcException("Missing result in response")
    }
}

/**
 * Exception thrown when an RPC call fails.
 */
class RpcException(
    message: String,
    val error: JsonRpcError? = null
) : Exception(message)