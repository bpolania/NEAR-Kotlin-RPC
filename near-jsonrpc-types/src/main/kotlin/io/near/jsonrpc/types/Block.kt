package io.near.jsonrpc.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BlockReference(
    @SerialName("block_id")
    val blockId: String? = null,
    @SerialName("block_height")
    val blockHeight: Long? = null,
    val finality: Finality? = null,
    @SerialName("sync_checkpoint")
    val syncCheckpoint: SyncCheckpoint? = null
)

@Serializable
enum class Finality {
    @SerialName("optimistic")
    OPTIMISTIC,
    @SerialName("near-final")
    NEAR_FINAL,
    @SerialName("final")
    FINAL
}

@Serializable
enum class SyncCheckpoint {
    @SerialName("genesis")
    GENESIS,
    @SerialName("earliest_available")
    EARLIEST_AVAILABLE
}

@Serializable
data class Block(
    val author: String,
    val header: BlockHeader,
    val chunks: List<ChunkHeader>
)

@Serializable
data class BlockHeader(
    val height: Long,
    val hash: String,
    @SerialName("prev_hash")
    val prevHash: String,
    @SerialName("epoch_id")
    val epochId: String,
    @SerialName("next_epoch_id")
    val nextEpochId: String,
    @SerialName("chunks_included")
    val chunksIncluded: Int,
    @SerialName("challenges_root")
    val challengesRoot: String,
    val timestamp: Long,
    @SerialName("timestamp_nanosec")
    val timestampNanosec: String,
    @SerialName("random_value")
    val randomValue: String,
    @SerialName("validator_proposals")
    val validatorProposals: List<ValidatorProposal>,
    @SerialName("chunk_mask")
    val chunkMask: List<Boolean>,
    @SerialName("gas_price")
    val gasPrice: String,
    @SerialName("block_ordinal")
    val blockOrdinal: Long? = null,
    @SerialName("total_supply")
    val totalSupply: String,
    @SerialName("challenges_result")
    val challengesResult: List<ChallengeResult>,
    @SerialName("last_final_block")
    val lastFinalBlock: String,
    @SerialName("last_ds_final_block")
    val lastDsFinalBlock: String,
    @SerialName("next_bp_hash")
    val nextBpHash: String,
    @SerialName("block_merkle_root")
    val blockMerkleRoot: String,
    @SerialName("epoch_sync_data_hash")
    val epochSyncDataHash: String? = null,
    val approvals: List<String?>,
    val signature: String,
    @SerialName("latest_protocol_version")
    val latestProtocolVersion: Int
)

@Serializable
data class ChunkHeader(
    @SerialName("chunk_hash")
    val chunkHash: String,
    @SerialName("prev_block_hash")
    val prevBlockHash: String,
    @SerialName("outcome_root")
    val outcomeRoot: String,
    @SerialName("prev_state_root")
    val prevStateRoot: String,
    @SerialName("encoded_merkle_root")
    val encodedMerkleRoot: String,
    @SerialName("encoded_length")
    val encodedLength: Long,
    @SerialName("height_created")
    val heightCreated: Long,
    @SerialName("height_included")
    val heightIncluded: Long,
    @SerialName("shard_id")
    val shardId: Int,
    @SerialName("gas_used")
    val gasUsed: Long,
    @SerialName("gas_limit")
    val gasLimit: Long,
    @SerialName("validator_reward")
    val validatorReward: String,
    @SerialName("balance_burnt")
    val balanceBurnt: String,
    @SerialName("outgoing_receipts_root")
    val outgoingReceiptsRoot: String,
    @SerialName("tx_root")
    val txRoot: String,
    @SerialName("validator_proposals")
    val validatorProposals: List<ValidatorProposal>,
    val signature: String
)

@Serializable
data class ValidatorProposal(
    @SerialName("account_id")
    val accountId: String,
    @SerialName("public_key")
    val publicKey: String,
    val stake: String
)

@Serializable
data class ChallengeResult(
    @SerialName("account_id")
    val accountId: String,
    @SerialName("is_double_sign")
    val isDoubleSign: Boolean
)