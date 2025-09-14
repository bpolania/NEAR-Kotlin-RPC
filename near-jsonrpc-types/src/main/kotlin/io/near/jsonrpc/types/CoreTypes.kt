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
    @SerialName("num_active_peers") val numActivePeers: Int = 0,
    @SerialName("peer_max_count") val peerMaxCount: Int = 0,
    @SerialName("active_peers") val activePeers: List<PeerInfo> = emptyList()
)

@Serializable
data class PeerInfo(
    val id: String = "",
    val addr: String = ""
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
    @SerialName("storage_usage") val storageUsage: Long = 0,
    @SerialName("block_height") val blockHeight: Long = 0,
    @SerialName("block_hash") val blockHash: String = ""
)

@Serializable
data class AccessKeyView(
    val nonce: Long = 0,
    val permission: String = "",
    @SerialName("block_height") val blockHeight: Long = 0,
    @SerialName("block_hash") val blockHash: String = ""
)

@Serializable
data class AccessKeyList(
    val keys: List<AccessKeyInfo> = emptyList(),
    @SerialName("block_height") val blockHeight: Long = 0,
    @SerialName("block_hash") val blockHash: String = ""
)

@Serializable
data class AccessKeyInfo(
    @SerialName("public_key") val publicKey: String = "",
    @SerialName("access_key") val accessKey: AccessKeyView = AccessKeyView()
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
    val hash: String = "",
    @SerialName("prev_hash") val prevHash: String = "",
    @SerialName("epoch_id") val epochId: String = "",
    @SerialName("next_epoch_id") val nextEpochId: String = "",
    @SerialName("chunks_included") val chunksIncluded: Int = 0,
    @SerialName("challenges_root") val challengesRoot: String = "",
    val timestamp: Long = 0,
    @SerialName("timestamp_nanosec") val timestampNanosec: String = "",
    @SerialName("random_value") val randomValue: String = "",
    @SerialName("validator_proposals") val validatorProposals: List<ValidatorProposal> = emptyList(),
    @SerialName("chunk_mask") val chunkMask: List<Boolean> = emptyList(),
    @SerialName("gas_price") val gasPrice: String = "",
    @SerialName("total_supply") val totalSupply: String = "",
    @SerialName("challenges_result") val challengesResult: List<String> = emptyList(),
    @SerialName("last_final_block") val lastFinalBlock: String = "",
    @SerialName("last_ds_final_block") val lastDsFinalBlock: String = "",
    @SerialName("next_bp_hash") val nextBpHash: String = "",
    @SerialName("block_merkle_root") val blockMerkleRoot: String = "",
    val approvals: List<String?> = emptyList(),
    val signature: String = "",
    @SerialName("latest_protocol_version") val latestProtocolVersion: Int = 0
)

@Serializable
data class ValidatorProposal(
    @SerialName("account_id") val accountId: String = "",
    @SerialName("public_key") val publicKey: String = "",
    val stake: String = ""
)

@Serializable
data class ChunkHeader(
    @SerialName("shard_id") val shardId: Int = 0,
    @SerialName("chunk_hash") val chunkHash: String = ""
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
    val logs: List<String> = emptyList(),
    @SerialName("block_height") val blockHeight: Long = 0,
    @SerialName("block_hash") val blockHash: String = ""
)

@Serializable
data class TransactionStatus(
    val status: String = "",
    val transaction: Transaction = Transaction()
)

@Serializable
data class Transaction(
    val hash: String = "",
    @SerialName("signer_id") val signerId: String = "",
    @SerialName("public_key") val publicKey: String = "",
    val nonce: Long = 0,
    @SerialName("receiver_id") val receiverId: String = "",
    val actions: List<Action> = emptyList(),
    val signature: String = ""
)

@Serializable
data class Action(
    val enum: ActionType = ActionType.TRANSFER,
    val transfer: TransferAction? = null
)

@Serializable
enum class ActionType {
    @SerialName("Transfer") TRANSFER,
    @SerialName("CreateAccount") CREATE_ACCOUNT,
    @SerialName("DeployContract") DEPLOY_CONTRACT,
    @SerialName("FunctionCall") FUNCTION_CALL,
    @SerialName("Stake") STAKE,
    @SerialName("AddKey") ADD_KEY,
    @SerialName("DeleteKey") DELETE_KEY,
    @SerialName("DeleteAccount") DELETE_ACCOUNT
}

@Serializable
data class TransferAction(
    val deposit: String = ""
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
    @SerialName("public_key") val publicKey: String = "",
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