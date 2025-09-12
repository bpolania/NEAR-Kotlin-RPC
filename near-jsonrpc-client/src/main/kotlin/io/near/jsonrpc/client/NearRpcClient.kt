package io.near.jsonrpc.client

import io.near.jsonrpc.types.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

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

    suspend fun status(): NetworkStatus = rpcCall("status", null)

    suspend fun networkInfo(): NetworkInfo = rpcCall("network_info", null)

    suspend fun gasPrice(blockReference: BlockReference? = null): GasPrice {
        val params = if (blockReference != null) {
            listOf(blockReference)
        } else {
            listOf(JsonNull)
        }
        return rpcCall("gas_price", params)
    }

    suspend fun block(blockReference: BlockReference): Block {
        val params = when {
            blockReference.finality != null -> buildJsonObject {
                val finalityStr = when(blockReference.finality) {
                    Finality.OPTIMISTIC -> "optimistic"
                    Finality.NEAR_FINAL -> "near-final"
                    Finality.FINAL -> "final"
                    null -> "final"
                }
                put("finality", finalityStr)
            }
            blockReference.blockId != null -> buildJsonObject {
                put("block_id", blockReference.blockId)
            }
            blockReference.blockHeight != null -> buildJsonObject {
                // NEAR RPC accepts block height as block_id
                put("block_id", blockReference.blockHeight)
            }
            else -> JsonNull
        }
        return rpcCall("block", params)
    }

    suspend fun chunk(chunkId: ChunkId): Chunk {
        val params = when {
            chunkId.chunkHash != null -> listOf(chunkId.chunkHash)
            chunkId.blockId != null && chunkId.shardId != null -> {
                listOf(chunkId.blockId, chunkId.shardId)
            }
            else -> throw IllegalArgumentException("ChunkId must have either chunkHash or both blockId and shardId")
        }
        return rpcCall("chunk", params)
    }

    suspend fun transaction(txHash: String, senderId: String): TransactionStatus {
        val params = listOf(txHash, senderId)
        return rpcCall("tx", params)
    }

    suspend fun viewAccount(
        accountId: String,
        blockReference: BlockReference = BlockReference(finality = Finality.FINAL)
    ): AccountView {
        val params = buildJsonObject {
            put("request_type", "view_account")
            put("account_id", accountId)
            when {
                blockReference.finality != null -> {
                    val finalityStr = when(blockReference.finality) {
                        Finality.OPTIMISTIC -> "optimistic"
                        Finality.NEAR_FINAL -> "near-final" 
                        Finality.FINAL -> "final"
                        null -> "final"
                    }
                    put("finality", finalityStr)
                }
                blockReference.blockId != null -> put("block_id", blockReference.blockId)
                blockReference.blockHeight != null -> put("block_height", blockReference.blockHeight)
            }
        }
        return rpcCall("query", params)
    }

    suspend fun viewAccessKey(
        accountId: String,
        publicKey: String,
        blockReference: BlockReference = BlockReference(finality = Finality.FINAL)
    ): AccessKeyView {
        val params = buildJsonObject {
            put("request_type", "view_access_key")
            put("account_id", accountId)
            put("public_key", publicKey)
            when {
                blockReference.finality != null -> {
                    val finalityStr = when(blockReference.finality) {
                        Finality.OPTIMISTIC -> "optimistic"
                        Finality.NEAR_FINAL -> "near-final"
                        Finality.FINAL -> "final"
                        null -> "final"
                    }
                    put("finality", finalityStr)
                }
                blockReference.blockId != null -> put("block_id", blockReference.blockId)
                blockReference.blockHeight != null -> put("block_height", blockReference.blockHeight)
            }
        }
        return rpcCall("query", params)
    }

    suspend fun viewAccessKeyList(
        accountId: String,
        blockReference: BlockReference = BlockReference(finality = Finality.FINAL)
    ): AccessKeyList {
        val params = buildJsonObject {
            put("request_type", "view_access_key_list")
            put("account_id", accountId)
            when {
                blockReference.finality != null -> {
                    val finalityStr = when(blockReference.finality) {
                        Finality.OPTIMISTIC -> "optimistic"
                        Finality.NEAR_FINAL -> "near-final"
                        Finality.FINAL -> "final"
                        null -> "final"
                    }
                    put("finality", finalityStr)
                }
                blockReference.blockId != null -> put("block_id", blockReference.blockId)
                blockReference.blockHeight != null -> put("block_height", blockReference.blockHeight)
            }
        }
        return rpcCall("query", params)
    }

    suspend fun callFunction(
        accountId: String,
        methodName: String,
        argsBase64: String,
        blockReference: BlockReference = BlockReference(finality = Finality.FINAL)
    ): CallFunctionResponse {
        val params = buildJsonObject {
            put("request_type", "call_function")
            put("account_id", accountId)
            put("method_name", methodName)
            put("args_base64", argsBase64)
            when {
                blockReference.finality != null -> {
                    val finalityStr = when(blockReference.finality) {
                        Finality.OPTIMISTIC -> "optimistic"
                        Finality.NEAR_FINAL -> "near-final"
                        Finality.FINAL -> "final"
                        null -> "final"
                    }
                    put("finality", finalityStr)
                }
                blockReference.blockId != null -> put("block_id", blockReference.blockId)
                blockReference.blockHeight != null -> put("block_height", blockReference.blockHeight)
            }
        }
        return rpcCall("query", params)
    }

    suspend fun sendTransaction(signedTx: String): TransactionResult {
        val params = listOf(signedTx)
        return rpcCall("broadcast_tx_async", params)
    }

    suspend fun sendTransactionAndWait(signedTx: String): TransactionStatus {
        val params = listOf(signedTx)
        return rpcCall("broadcast_tx_commit", params)
    }

    suspend fun validators(blockReference: BlockReference? = null): ValidatorStatus {
        val params = if (blockReference != null) {
            val blockParam = when {
                blockReference.finality != null -> {
                    val finalityStr = when(blockReference.finality) {
                        Finality.OPTIMISTIC -> "optimistic"
                        Finality.NEAR_FINAL -> "near-final"
                        Finality.FINAL -> "final"
                        null -> "final"
                    }
                    buildJsonObject { put("finality", finalityStr) }
                }
                blockReference.blockId != null -> buildJsonObject { put("block_id", blockReference.blockId) }
                blockReference.blockHeight != null -> blockReference.blockHeight
                else -> JsonNull
            }
            listOf(blockParam)
        } else {
            listOf(JsonNull)
        }
        return rpcCall("validators", params)
    }

    suspend fun lightClientProof(request: LightClientProofRequest): LightClientProof {
        return rpcCall("light_client_proof", request)
    }

    private suspend inline fun <reified T> rpcCall(
        method: String,
        params: Any?
    ): T = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString()
        
        val paramsElement = when (params) {
            null -> null
            is JsonElement -> params
            is BlockReference -> json.encodeToJsonElement(BlockReference.serializer(), params)
            is ChunkId -> json.encodeToJsonElement(ChunkId.serializer(), params)
            is LightClientProofRequest -> json.encodeToJsonElement(LightClientProofRequest.serializer(), params)
            is List<*> -> JsonArray(params.map { 
                when (it) {
                    is String -> JsonPrimitive(it)
                    is Number -> JsonPrimitive(it)
                    is Boolean -> JsonPrimitive(it)
                    is BlockReference -> json.encodeToJsonElement(BlockReference.serializer(), it)
                    else -> JsonNull
                }
            })
            else -> JsonNull
        }
        
        val requestBody = JsonRpcRequest(
            id = requestId,
            method = method,
            params = paramsElement
        )

        // Debug logging
        if (rpcUrl.contains("testnet")) {
            println("DEBUG: Making request to $rpcUrl with method $method")
        }
        
        val httpRequest = Request.Builder()
            .url(rpcUrl)
            .post(
                json.encodeToString(requestBody)
                    .toRequestBody("application/json".toMediaType())
            )
            .build()

        val response = httpClient.newCall(httpRequest).execute()
        val responseBody = response.body?.string() ?: throw RpcException("Empty response body")

        if (!response.isSuccessful) {
            throw RpcException("HTTP error: ${response.code} - $responseBody")
        }

        val jsonResponse = json.decodeFromString<JsonRpcResponse<JsonElement>>(responseBody)
        
        jsonResponse.error?.let { error ->
            throw RpcException("RPC error: ${error.code} - ${error.message}", error)
        }

        jsonResponse.result?.let { result ->
            return@withContext json.decodeFromJsonElement<T>(result)
        } ?: throw RpcException("No result in response")
    }
}

class RpcException(
    message: String,
    val error: JsonRpcError? = null
) : Exception(message)