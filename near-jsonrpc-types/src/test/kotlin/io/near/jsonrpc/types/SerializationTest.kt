package io.near.jsonrpc.types

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class SerializationTest : FunSpec({
    
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    context("Snake_case to camelCase conversion") {
        test("should serialize camelCase properties to snake_case") {
            val blockRef = BlockReference(
                blockId = "test-block-id",
                blockHeight = 12345
            )
            
            val serialized = json.encodeToString(blockRef)
            
            // Check that JSON contains snake_case
            serialized.contains("\"block_id\"") shouldBe true
            serialized.contains("\"block_height\"") shouldBe true
            
            // Should NOT contain camelCase
            serialized.contains("\"blockId\"") shouldBe false
            serialized.contains("\"blockHeight\"") shouldBe false
        }
        
        test("should deserialize snake_case JSON to camelCase properties") {
            val jsonString = """
                {
                    "block_id": "test-id",
                    "block_height": 99999,
                    "sync_checkpoint": "genesis"
                }
            """.trimIndent()
            
            val blockRef = json.decodeFromString<BlockReference>(jsonString)
            
            blockRef.blockId shouldBe "test-id"
            blockRef.blockHeight shouldBe 99999
            blockRef.syncCheckpoint shouldBe SyncCheckpoint.GENESIS
        }
    }
    
    context("Complex object serialization") {
        test("should serialize/deserialize Transaction correctly") {
            val original = Transaction(
                signerId = "alice.near",
                publicKey = "ed25519:pubkey123",
                nonce = 42,
                receiverId = "bob.near",
                actions = listOf(
                    Action(
                        enum = ActionType.TRANSFER,
                        transfer = TransferAction(deposit = "1000000000000000000000000")
                    )
                ),
                signature = "sig123",
                hash = "hash123"
            )
            
            val jsonString = json.encodeToString(original)
            val deserialized = json.decodeFromString<Transaction>(jsonString)
            
            deserialized.signerId shouldBe original.signerId
            deserialized.publicKey shouldBe original.publicKey
            deserialized.nonce shouldBe original.nonce
            deserialized.receiverId shouldBe original.receiverId
            deserialized.actions.size shouldBe 1
            deserialized.actions[0].enum shouldBe ActionType.TRANSFER
            deserialized.actions[0].transfer?.deposit shouldBe "1000000000000000000000000"
        }
        
        test("should handle nested objects in BlockHeader") {
            val jsonString = """
                {
                    "height": 1000,
                    "hash": "block-hash",
                    "prev_hash": "prev-hash",
                    "epoch_id": "epoch-123",
                    "next_epoch_id": "epoch-124",
                    "chunks_included": 4,
                    "challenges_root": "challenges",
                    "timestamp": 1640000000000,
                    "timestamp_nanosec": "1640000000000000000",
                    "random_value": "random",
                    "validator_proposals": [
                        {
                            "account_id": "validator.near",
                            "public_key": "ed25519:key",
                            "stake": "50000000000000000000000000"
                        }
                    ],
                    "chunk_mask": [true, false, true, true],
                    "gas_price": "100000000",
                    "total_supply": "1000000000000000000000000000",
                    "challenges_result": [],
                    "last_final_block": "final-hash",
                    "last_ds_final_block": "ds-final-hash",
                    "next_bp_hash": "bp-hash",
                    "block_merkle_root": "merkle-root",
                    "approvals": ["approval1", null, "approval3"],
                    "signature": "block-signature",
                    "latest_protocol_version": 55
                }
            """.trimIndent()
            
            val header = json.decodeFromString<BlockHeader>(jsonString)
            
            header.height shouldBe 1000
            header.validatorProposals.size shouldBe 1
            header.validatorProposals[0].accountId shouldBe "validator.near"
            header.chunkMask shouldBe listOf(true, false, true, true)
            header.approvals.size shouldBe 3
            header.approvals[1] shouldBe null
        }
    }
    
    context("Enum serialization") {
        test("should serialize enums to correct string values") {
            val finalityJson = json.encodeToString(Finality.FINAL)
            finalityJson shouldBe "\"final\""
            
            val optimisticJson = json.encodeToString(Finality.OPTIMISTIC)
            optimisticJson shouldBe "\"optimistic\""
            
            val nearFinalJson = json.encodeToString(Finality.NEAR_FINAL)
            nearFinalJson shouldBe "\"near-final\""
        }
        
        test("should deserialize string values to enums") {
            val final = json.decodeFromString<Finality>("\"final\"")
            final shouldBe Finality.FINAL
            
            val optimistic = json.decodeFromString<Finality>("\"optimistic\"")
            optimistic shouldBe Finality.OPTIMISTIC
            
            val nearFinal = json.decodeFromString<Finality>("\"near-final\"")
            nearFinal shouldBe Finality.NEAR_FINAL
        }
        
        test("should handle ActionType enum serialization") {
            val action = Action(
                enum = ActionType.FUNCTION_CALL,
                functionCall = FunctionCallAction(
                    methodName = "test",
                    args = "e30=",
                    gas = 30000000000000,
                    deposit = "0"
                )
            )
            
            val jsonString = json.encodeToString(action)
            jsonString.contains("\"FunctionCall\"") shouldBe true
            
            val deserialized = json.decodeFromString<Action>(jsonString)
            deserialized.enum shouldBe ActionType.FUNCTION_CALL
        }
    }
    
    context("Error handling") {
        test("should handle malformed JSON") {
            val malformed = """
                {
                    "block_id": "test",
                    "block_height": "not-a-number"
                }
            """
            
            shouldThrow<SerializationException> {
                json.decodeFromString<BlockReference>(malformed)
            }
        }
        
        test("should handle missing required fields") {
            val incomplete = """
                {
                    "signer_id": "alice.near"
                }
            """
            
            shouldThrow<SerializationException> {
                json.decodeFromString<Transaction>(incomplete)
            }
        }
        
        test("should ignore unknown fields when configured") {
            val withExtra = """
                {
                    "block_id": "test",
                    "block_height": 12345,
                    "unknown_field": "ignored",
                    "another_unknown": 999
                }
            """
            
            val blockRef = json.decodeFromString<BlockReference>(withExtra)
            blockRef.blockId shouldBe "test"
            blockRef.blockHeight shouldBe 12345
        }
    }
    
    context("JsonRpc serialization") {
        test("should serialize JsonRpcRequest correctly") {
            val request = JsonRpcRequest(
                jsonrpc = "2.0",
                id = "unique-123",
                method = "query",
                params = json.parseToJsonElement("""{"request_type": "view_account"}""")
            )
            
            val serialized = json.encodeToString(request)
            
            serialized.contains("\"jsonrpc\":\"2.0\"") shouldBe true
            serialized.contains("\"id\":\"unique-123\"") shouldBe true
            serialized.contains("\"method\":\"query\"") shouldBe true
            serialized.contains("\"request_type\":\"view_account\"") shouldBe true
        }
        
        test("should handle JsonRpcResponse with result") {
            val jsonString = """
                {
                    "jsonrpc": "2.0",
                    "id": "test-123",
                    "result": {
                        "amount": "1000000000000000000000000",
                        "locked": "0",
                        "code_hash": "11111111111111111111111111111111",
                        "storage_usage": 500,
                        "storage_paid_at": 0,
                        "block_height": 12345,
                        "block_hash": "hash"
                    }
                }
            """.trimIndent()
            
            val response = json.decodeFromString<JsonRpcResponse<AccountView>>(jsonString)
            
            response.jsonrpc shouldBe "2.0"
            response.id shouldBe "test-123"
            response.result shouldNotBe null
            response.result?.amount shouldBe "1000000000000000000000000"
            response.error shouldBe null
        }
        
        test("should handle JsonRpcResponse with error") {
            val jsonString = """
                {
                    "jsonrpc": "2.0",
                    "id": "test-456",
                    "error": {
                        "code": -32601,
                        "message": "Method not found",
                        "data": {
                            "details": "The method 'invalid_method' does not exist"
                        }
                    }
                }
            """.trimIndent()
            
            val response = json.decodeFromString<JsonRpcResponse<AccountView>>(jsonString)
            
            response.jsonrpc shouldBe "2.0"
            response.id shouldBe "test-456"
            response.result shouldBe null
            response.error shouldNotBe null
            response.error?.code shouldBe -32601
            response.error?.message shouldBe "Method not found"
            response.error?.data shouldNotBe null
        }
    }
    
    context("Large number handling") {
        test("should handle yoctoNEAR amounts as strings") {
            val account = AccountView(
                amount = "999999999999999999999999999999999",
                locked = "0",
                codeHash = "11111111111111111111111111111111",
                storageUsage = 9999999999,
                storagePaidAt = 0,
                blockHeight = 99999999,
                blockHash = "hash"
            )
            
            val jsonString = json.encodeToString(account)
            val deserialized = json.decodeFromString<AccountView>(jsonString)
            
            // Large numbers are preserved as strings
            deserialized.amount shouldBe "999999999999999999999999999999999"
            
            // Regular numbers work normally
            deserialized.storageUsage shouldBe 9999999999
            deserialized.blockHeight shouldBe 99999999
        }
    }
})