package io.near.jsonrpc.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.KSerializer

@Serializable
data class Transaction(
    @SerialName("signer_id")
    val signerId: String,
    @SerialName("public_key")
    val publicKey: String,
    val nonce: Long,
    @SerialName("receiver_id")
    val receiverId: String,
    val actions: List<Action>,
    val signature: String,
    val hash: String
)

@Serializable
data class Action(
    val enum: ActionType,
    @SerialName("function_call")
    val functionCall: FunctionCallAction? = null,
    val transfer: TransferAction? = null,
    val stake: StakeAction? = null,
    @SerialName("add_key")
    val addKey: AddKeyAction? = null,
    @SerialName("delete_key")
    val deleteKey: DeleteKeyAction? = null,
    @SerialName("create_account")
    val createAccount: CreateAccountAction? = null,
    @SerialName("delete_account")
    val deleteAccount: DeleteAccountAction? = null,
    @SerialName("deploy_contract")
    val deployContract: DeployContractAction? = null
)

@Serializable
enum class ActionType {
    @SerialName("FunctionCall")
    FUNCTION_CALL,
    @SerialName("Transfer")
    TRANSFER,
    @SerialName("Stake")
    STAKE,
    @SerialName("AddKey")
    ADD_KEY,
    @SerialName("DeleteKey")
    DELETE_KEY,
    @SerialName("CreateAccount")
    CREATE_ACCOUNT,
    @SerialName("DeleteAccount")
    DELETE_ACCOUNT,
    @SerialName("DeployContract")
    DEPLOY_CONTRACT
}

@Serializable
data class FunctionCallAction(
    @SerialName("method_name")
    val methodName: String,
    val args: String,
    val gas: Long,
    val deposit: String
)

@Serializable
data class TransferAction(
    val deposit: String
)

@Serializable
data class StakeAction(
    val stake: String,
    @SerialName("public_key")
    val publicKey: String
)

@Serializable
data class AddKeyAction(
    @SerialName("public_key")
    val publicKey: String,
    @SerialName("access_key")
    val accessKey: AccessKey
)

@Serializable
data class DeleteKeyAction(
    @SerialName("public_key")
    val publicKey: String
)

@Serializable
class CreateAccountAction

@Serializable
data class DeleteAccountAction(
    @SerialName("beneficiary_id")
    val beneficiaryId: String
)

@Serializable
data class DeployContractAction(
    val code: String
)

@Serializable
data class AccessKey(
    val nonce: Long,
    @Serializable(with = PermissionSerializer::class)
    val permission: Permission
)

@Serializable(with = PermissionSerializer::class)
sealed class Permission {
    @Serializable
    object FullAccess : Permission()
    
    @Serializable
    data class FunctionCall(
        @SerialName("FunctionCall")
        val functionCall: FunctionCallPermission
    ) : Permission()
}

object PermissionSerializer : KSerializer<Permission> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Permission", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: Permission) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is Permission.FullAccess -> {
                jsonEncoder.encodeJsonElement(JsonPrimitive("FullAccess"))
            }
            is Permission.FunctionCall -> {
                jsonEncoder.encodeJsonElement(JsonObject(mapOf(
                    "FunctionCall" to jsonEncoder.json.encodeToJsonElement(
                        FunctionCallPermission.serializer(), 
                        value.functionCall
                    )
                )))
            }
        }
    }
    
    override fun deserialize(decoder: Decoder): Permission {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        
        return when {
            element is JsonPrimitive && element.contentOrNull == "FullAccess" -> {
                Permission.FullAccess
            }
            element is JsonObject && "FunctionCall" in element -> {
                val functionCallElement = element["FunctionCall"]!!
                val functionCall = jsonDecoder.json.decodeFromJsonElement(
                    FunctionCallPermission.serializer(),
                    functionCallElement
                )
                Permission.FunctionCall(functionCall)
            }
            else -> throw IllegalArgumentException("Unknown permission type: $element")
        }
    }
}

@Serializable
data class FunctionCallPermission(
    val allowance: String? = null,
    @SerialName("receiver_id")
    val receiverId: String,
    @SerialName("method_names")
    val methodNames: List<String>
)

@Serializable
data class TransactionStatus(
    val status: ExecutionStatus,
    val transaction: Transaction,
    @SerialName("transaction_outcome")
    val transactionOutcome: ExecutionOutcome,
    @SerialName("receipts_outcome")
    val receiptsOutcome: List<ExecutionOutcome>
)

@Serializable
data class ExecutionStatus(
    val enum: ExecutionStatusType,
    @SerialName("success_value")
    val successValue: String? = null,
    @SerialName("success_receipt_id")
    val successReceiptId: String? = null,
    val failure: JsonElement? = null
)

@Serializable
enum class ExecutionStatusType {
    @SerialName("Unknown")
    UNKNOWN,
    @SerialName("Failure")
    FAILURE,
    @SerialName("SuccessValue")
    SUCCESS_VALUE,
    @SerialName("SuccessReceiptId")
    SUCCESS_RECEIPT_ID
}

@Serializable
data class ExecutionOutcome(
    @SerialName("block_hash")
    val blockHash: String,
    val id: String,
    val outcome: Outcome,
    val proof: List<MerklePathItem>
)

@Serializable
data class Outcome(
    val logs: List<String>,
    @SerialName("receipt_ids")
    val receiptIds: List<String>,
    @SerialName("gas_burnt")
    val gasBurnt: Long,
    @SerialName("tokens_burnt")
    val tokensBurnt: String,
    @SerialName("executor_id")
    val executorId: String,
    val status: ExecutionStatus,
    val metadata: OutcomeMetadata? = null
)

@Serializable
data class OutcomeMetadata(
    val version: Int,
    @SerialName("gas_profile")
    val gasProfile: List<GasProfile>? = null
)

@Serializable
data class GasProfile(
    val cost: String,
    @SerialName("cost_category")
    val costCategory: String,
    @SerialName("gas_used")
    val gasUsed: Long
)

@Serializable
data class MerklePathItem(
    val hash: String,
    val direction: Direction
)

@Serializable
enum class Direction {
    @SerialName("Left")
    LEFT,
    @SerialName("Right")
    RIGHT
}