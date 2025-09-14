package io.near.jsonrpc.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Core JSON-RPC types (not in OpenAPI spec, always needed)
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

// Minimal types for client compilation (will be replaced by generated ones in CI)
// These are temporary stubs to allow local development

@Serializable
data class NetworkStatus(
    @SerialName("chain_id") val chainId: String = "",
    @SerialName("sync_info") val syncInfo: SyncInfo = SyncInfo()
)

@Serializable
data class SyncInfo(
    @SerialName("latest_block_height") val latestBlockHeight: Long = 0,
    val syncing: Boolean = false
)

@Serializable
data class NetworkInfo(
    @SerialName("num_active_peers") val numActivePeers: Int = 0
)

@Serializable
data class GasPrice(
    @SerialName("gas_price") val gasPrice: String = ""
)

@Serializable
data class AccountView(
    val amount: String = "",
    val locked: String = "",
    @SerialName("code_hash") val codeHash: String = "",
    @SerialName("storage_usage") val storageUsage: Long = 0
)

@Serializable
data class AccessKeyView(
    val nonce: Long = 0,
    val permission: String = ""
)

@Serializable
data class AccessKeyList(
    val keys: List<String> = emptyList()
)

@Serializable
data class Block(
    val author: String = "",
    val header: BlockHeader = BlockHeader(),
    val chunks: List<ChunkHeader> = emptyList()
)

@Serializable
data class BlockHeader(
    val height: Long = 0,
    val hash: String = ""
)

@Serializable
data class ChunkHeader(
    @SerialName("shard_id") val shardId: Int = 0
)

@Serializable
data class Chunk(
    val author: String = "",
    val header: ChunkHeader = ChunkHeader()
)

@Serializable
data class ChunkId(
    @SerialName("chunk_hash") val chunkHash: String? = null,
    @SerialName("block_id") val blockId: String? = null,
    @SerialName("shard_id") val shardId: Int? = null
)

@Serializable
data class BlockReference(
    val finality: Finality? = null,
    @SerialName("block_id") val blockId: String? = null,
    @SerialName("block_height") val blockHeight: Long? = null,
    @SerialName("sync_checkpoint") val syncCheckpoint: SyncCheckpoint? = null
)

@Serializable
enum class Finality {
    @SerialName("optimistic") OPTIMISTIC,
    @SerialName("near-final") NEAR_FINAL,
    @SerialName("final") FINAL
}

@Serializable
enum class SyncCheckpoint {
    @SerialName("genesis") GENESIS,
    @SerialName("earliest_available") EARLIEST_AVAILABLE
}

@Serializable
data class CallFunctionResponse(
    val result: List<Int> = emptyList(),
    val logs: List<String> = emptyList()
)

@Serializable
data class TransactionStatus(
    val status: String = "",
    val transaction: Transaction = Transaction()
)

@Serializable
data class Transaction(
    val hash: String = "",
    @SerialName("signer_id") val signerId: String = ""
)

@Serializable
data class TransactionResult(
    val hash: String = ""
)

@Serializable
data class ValidatorStatus(
    val validators: List<ValidatorInfo> = emptyList(),
    @SerialName("current_validators") val currentValidators: List<ValidatorInfo> = emptyList(),
    @SerialName("next_validators") val nextValidators: List<ValidatorInfo> = emptyList()
)

@Serializable
data class ValidatorInfo(
    @SerialName("account_id") val accountId: String = "",
    val stake: String = "",
    @SerialName("num_expected_blocks") val numExpectedBlocks: Long = 0,
    @SerialName("num_produced_blocks") val numProducedBlocks: Long = 0
)

@Serializable
data class LightClientProofRequest(
    @SerialName("type") val type: String,
    @SerialName("light_client_head") val lightClientHead: String,
    @SerialName("transaction_hash") val transactionHash: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("receipt_id") val receiptId: String? = null,
    @SerialName("receiver_id") val receiverId: String? = null,
    @SerialName("block_hash") val blockHash: String? = null
)

@Serializable
data class LightClientProof(
    @SerialName("outcome_proof") val outcomeProof: OutcomeProof = OutcomeProof(),
    @SerialName("block_header_lite") val blockHeaderLite: BlockHeaderLite = BlockHeaderLite()
)

@Serializable
data class OutcomeProof(
    val proof: List<MerklePathItem> = emptyList()
)

@Serializable
data class BlockHeaderLite(
    val hash: String = ""
)

@Serializable
data class MerklePathItem(
    val hash: String = "",
    val direction: String = ""
)

@Serializable
data class Permission(
    @SerialName("permission_type") val permissionType: String = ""
)