package io.near.jsonrpc.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkStatus(
    @SerialName("chain_id")
    val chainId: String,
    @SerialName("rpc_addr")
    val rpcAddr: String? = null,
    @SerialName("sync_info")
    val syncInfo: SyncInfo,
    @SerialName("validator_account_id")
    val validatorAccountId: String? = null,
    val validators: List<ValidatorInfo>
)

@Serializable
data class SyncInfo(
    @SerialName("latest_block_hash")
    val latestBlockHash: String,
    @SerialName("latest_block_height")
    val latestBlockHeight: Long,
    @SerialName("latest_block_time")
    val latestBlockTime: String,
    @SerialName("latest_state_root")
    val latestStateRoot: String,
    val syncing: Boolean
)

@Serializable
data class ValidatorInfo(
    @SerialName("account_id")
    val accountId: String,
    @SerialName("is_slashed")
    val isSlashed: Boolean? = null
)

@Serializable
data class NetworkInfo(
    @SerialName("active_peers")
    val activePeers: List<PeerInfo>,
    @SerialName("num_active_peers")
    val numActivePeers: Int,
    @SerialName("peer_max_count")
    val peerMaxCount: Int,
    @SerialName("sent_bytes_per_sec")
    val sentBytesPerSec: Long,
    @SerialName("received_bytes_per_sec")
    val receivedBytesPerSec: Long,
    @SerialName("known_producers")
    val knownProducers: List<KnownProducer>
)

@Serializable
data class PeerInfo(
    val id: String,
    val addr: String,
    @SerialName("account_id")
    val accountId: String? = null
)

@Serializable
data class KnownProducer(
    @SerialName("account_id")
    val accountId: String,
    @SerialName("peer_id")
    val peerId: String
)

@Serializable
data class GasPrice(
    @SerialName("gas_price")
    val gasPrice: String
)

@Serializable
data class Chunk(
    val author: String,
    val header: ChunkHeader,
    val transactions: List<Transaction>,
    val receipts: List<Receipt>
)

@Serializable
data class Receipt(
    @SerialName("predecessor_id")
    val predecessorId: String,
    val receiver: String,
    @SerialName("receipt_id")
    val receiptId: String,
    val receipt: ReceiptEnum
)

@Serializable
data class ReceiptEnum(
    val Action: ActionReceipt? = null,
    val Data: DataReceipt? = null
)

@Serializable
data class ActionReceipt(
    @SerialName("signer_id")
    val signerId: String,
    @SerialName("signer_public_key")
    val signerPublicKey: String,
    @SerialName("gas_price")
    val gasPrice: String,
    @SerialName("output_data_receivers")
    val outputDataReceivers: List<DataReceiver>,
    @SerialName("input_data_ids")
    val inputDataIds: List<String>,
    val actions: List<Action>
)

@Serializable
data class DataReceipt(
    @SerialName("data_id")
    val dataId: String,
    val data: String? = null
)

@Serializable
data class DataReceiver(
    @SerialName("data_id")
    val dataId: String,
    @SerialName("receiver_id")
    val receiverId: String
)

@Serializable
data class ChunkId(
    @SerialName("chunk_hash")
    val chunkHash: String? = null,
    @SerialName("block_id")
    val blockId: String? = null,
    @SerialName("shard_id")
    val shardId: Int? = null
)

@Serializable
data class TransactionResult(
    val hash: String
)

@Serializable
data class ValidatorStatus(
    @SerialName("current_validators")
    val currentValidators: List<CurrentValidator>,
    @SerialName("next_validators")
    val nextValidators: List<NextValidator>,
    @SerialName("current_proposals")
    val currentProposals: List<ValidatorProposal>
)

@Serializable
data class CurrentValidator(
    @SerialName("account_id")
    val accountId: String,
    @SerialName("public_key")
    val publicKey: String,
    val stake: String,
    @SerialName("is_slashed")
    val isSlashed: Boolean,
    @SerialName("shards")
    val shards: List<Int>,
    @SerialName("num_expected_blocks")
    val numExpectedBlocks: Int,
    @SerialName("num_expected_chunks")
    val numExpectedChunks: Int,
    @SerialName("num_produced_blocks")
    val numProducedBlocks: Int,
    @SerialName("num_produced_chunks")
    val numProducedChunks: Int
)

@Serializable
data class NextValidator(
    @SerialName("account_id")
    val accountId: String,
    @SerialName("public_key")
    val publicKey: String,
    val stake: String,
    val shards: List<Int>
)

@Serializable
data class LightClientProofRequest(
    val type: String,
    @SerialName("light_client_head")
    val lightClientHead: String,
    @SerialName("transaction_hash")
    val transactionHash: String? = null,
    @SerialName("sender_id")
    val senderId: String? = null,
    @SerialName("receipt_id")
    val receiptId: String? = null,
    @SerialName("receiver_id")
    val receiverId: String? = null
)

@Serializable
data class LightClientProof(
    @SerialName("outcome_proof")
    val outcomeProof: ExecutionOutcome,
    @SerialName("outcome_root_proof")
    val outcomeRootProof: List<MerklePathItem>,
    @SerialName("block_header_lite")
    val blockHeaderLite: BlockHeaderLite,
    @SerialName("block_proof")
    val blockProof: List<MerklePathItem>
)

@Serializable
data class BlockHeaderLite(
    @SerialName("prev_block_hash")
    val prevBlockHash: String,
    @SerialName("inner_rest_hash")
    val innerRestHash: String,
    @SerialName("inner_lite")
    val innerLite: InnerLite
)

@Serializable
data class InnerLite(
    val height: Long,
    @SerialName("epoch_id")
    val epochId: String,
    @SerialName("next_epoch_id")
    val nextEpochId: String,
    @SerialName("prev_state_root")
    val prevStateRoot: String,
    @SerialName("outcome_root")
    val outcomeRoot: String,
    val timestamp: Long,
    @SerialName("timestamp_nanosec")
    val timestampNanosec: String,
    @SerialName("next_bp_hash")
    val nextBpHash: String,
    @SerialName("block_merkle_root")
    val blockMerkleRoot: String
)