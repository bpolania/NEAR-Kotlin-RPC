package io.near.jsonrpc.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountView(
    val amount: String,
    val locked: String,
    @SerialName("code_hash")
    val codeHash: String,
    @SerialName("storage_usage")
    val storageUsage: Long,
    @SerialName("storage_paid_at")
    val storagePaidAt: Long,
    @SerialName("block_height")
    val blockHeight: Long,
    @SerialName("block_hash")
    val blockHash: String
)

@Serializable
data class AccessKeyView(
    val nonce: Long,
    val permission: Permission,
    @SerialName("block_height")
    val blockHeight: Long,
    @SerialName("block_hash")
    val blockHash: String
)

@Serializable
data class AccessKeyList(
    val keys: List<AccessKeyInfo>,
    @SerialName("block_height")
    val blockHeight: Long,
    @SerialName("block_hash")
    val blockHash: String
)

@Serializable
data class AccessKeyInfo(
    @SerialName("public_key")
    val publicKey: String,
    @SerialName("access_key")
    val accessKey: AccessKey
)

@Serializable
data class ContractCode(
    @SerialName("code_base64")
    val codeBase64: String,
    val hash: String,
    @SerialName("block_height")
    val blockHeight: Long,
    @SerialName("block_hash")
    val blockHash: String
)

@Serializable
data class ContractState(
    val values: List<StateItem>,
    @SerialName("block_height")
    val blockHeight: Long,
    @SerialName("block_hash")
    val blockHash: String
)

@Serializable
data class StateItem(
    val key: String,
    val value: String
)

@Serializable
data class CallFunctionRequest(
    @SerialName("account_id")
    val accountId: String,
    @SerialName("method_name")
    val methodName: String,
    @SerialName("args_base64")
    val argsBase64: String
)

@Serializable
data class CallFunctionResponse(
    val result: List<Int> = emptyList(),
    val logs: List<String> = emptyList(),
    @SerialName("block_height")
    val blockHeight: Long? = null,
    @SerialName("block_hash")
    val blockHash: String? = null
)