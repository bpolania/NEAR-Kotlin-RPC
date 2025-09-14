package io.near.jsonrpc.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Core JSON-RPC types used by the client for generic request/response handling.
 * These are not in the OpenAPI spec and are needed for the RPC transport layer.
 */

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String,
    val params: JsonElement? = null
)

@Serializable
data class JsonRpcResponse<T>(
    val jsonrpc: String = "2.0",
    val id: String? = null,
    val result: T? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)