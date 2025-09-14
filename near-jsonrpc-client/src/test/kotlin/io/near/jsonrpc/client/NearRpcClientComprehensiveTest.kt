package io.near.jsonrpc.client

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.assertThrows
import java.util.Base64

class NearRpcClientComprehensiveTest : FunSpec({

    lateinit var mockWebServer: MockWebServer
    lateinit var client: NearRpcClient

    beforeEach {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = NearRpcClient(rpcUrl = mockWebServer.url("/").toString())
    }

    afterEach {
        mockWebServer.shutdown()
    }

    context("Status and Network Info") {
        test("should fetch network status") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "chain_id": "testnet",
                    "sync_info": {
                        "latest_block_hash": "test-hash",
                        "latest_block_height": 100000,
                        "syncing": false
                    }
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val status = client.status()
                val statusObj = status.jsonObject
                
                statusObj["chain_id"]?.jsonPrimitive?.content shouldBe "testnet"
                val syncInfo = statusObj["sync_info"]?.jsonObject
                syncInfo?.get("latest_block_height")?.jsonPrimitive?.int shouldBe 100000
                syncInfo?.get("syncing")?.jsonPrimitive?.boolean shouldBe false
            }
        }

        test("should fetch network info") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "active_peers": [],
                    "num_active_peers": 5,
                    "peer_max_count": 50,
                    "sent_bytes_per_sec": 1000,
                    "received_bytes_per_sec": 2000
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val networkInfo = client.networkInfo()
                val networkObj = networkInfo.jsonObject
                
                networkObj["num_active_peers"]?.jsonPrimitive?.int shouldBe 5
                networkObj["peer_max_count"]?.jsonPrimitive?.int shouldBe 50
                networkObj["sent_bytes_per_sec"]?.jsonPrimitive?.int shouldBe 1000
            }
        }
    }

    context("Gas Price") {
        test("should fetch gas price without block") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "gas_price": "100000000"
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val gasPrice = client.gasPrice()
                val gasPriceObj = gasPrice.jsonObject
                
                gasPriceObj["gas_price"]?.jsonPrimitive?.content shouldBe "100000000"
            }
            
            val request = mockWebServer.takeRequest()
            val body = request.body.readUtf8()
            body shouldContain "gas_price"
            body shouldContain "[null]"
        }

        test("should fetch gas price for specific block") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "gas_price": "200000000"
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val gasPrice = client.gasPrice("12345")
                val gasPriceObj = gasPrice.jsonObject
                
                gasPriceObj["gas_price"]?.jsonPrimitive?.content shouldBe "200000000"
            }
            
            val request = mockWebServer.takeRequest()
            val body = request.body.readUtf8()
            body shouldContain "12345"
        }
    }

    context("Block Operations") {
        test("should fetch block by finality") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "author": "validator.near",
                    "header": {
                        "height": 12345,
                        "hash": "block-hash"
                    },
                    "chunks": []
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val block = client.block("final")
                val blockObj = block.jsonObject
                
                blockObj["author"]?.jsonPrimitive?.content shouldBe "validator.near"
                val header = blockObj["header"]?.jsonObject
                header?.get("height")?.jsonPrimitive?.int shouldBe 12345
            }
            
            val request = mockWebServer.takeRequest()
            val body = request.body.readUtf8()
            body shouldContain """"finality":"final""""
        }

        test("should fetch block by height") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "author": "validator2.near",
                    "header": {
                        "height": 99999,
                        "hash": "height-block-hash"
                    }
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val block = client.block("99999")
                val blockObj = block.jsonObject
                
                val header = blockObj["header"]?.jsonObject
                header?.get("height")?.jsonPrimitive?.int shouldBe 99999
            }
            
            val request = mockWebServer.takeRequest()
            val body = request.body.readUtf8()
            body shouldContain """"block_id":99999"""
        }

        test("should fetch block by hash") {
            val blockHash = "9MzuZrRPW1BGpFnZJUJg6SzCrixPpJDfjsNeUobRXsLe"
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "header": {
                        "hash": "$blockHash"
                    }
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val block = client.block(blockHash)
                val blockObj = block.jsonObject
                
                val header = blockObj["header"]?.jsonObject
                header?.get("hash")?.jsonPrimitive?.content shouldBe blockHash
            }
            
            val request = mockWebServer.takeRequest()
            val body = request.body.readUtf8()
            body shouldContain """"block_id":"$blockHash""""
        }
    }

    context("Transaction Operations") {
        test("should fetch transaction status") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "status": {
                        "SuccessValue": ""
                    },
                    "transaction": {
                        "signer_id": "sender.near",
                        "receiver_id": "receiver.near"
                    }
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val tx = client.transaction("tx-hash", "sender.near")
                val txObj = tx.jsonObject
                
                txObj["status"] shouldNotBe null
                val transaction = txObj["transaction"]?.jsonObject
                transaction?.get("signer_id")?.jsonPrimitive?.content shouldBe "sender.near"
            }
        }

        test("should send transaction async") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": "tx-hash-12345"
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val result = client.sendTransactionAsync("base64-signed-tx")
                result.jsonPrimitive.content shouldBe "tx-hash-12345"
            }
        }

        test("should send transaction and wait") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "status": {
                        "SuccessValue": ""
                    },
                    "transaction_outcome": {
                        "id": "outcome-id"
                    }
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val result = client.sendTransactionCommit("base64-signed-tx")
                val resultObj = result.jsonObject
                
                resultObj["status"] shouldNotBe null
                val outcome = resultObj["transaction_outcome"]?.jsonObject
                outcome?.get("id")?.jsonPrimitive?.content shouldBe "outcome-id"
            }
        }
    }

    context("Validator Operations") {
        test("should fetch validators for latest block") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "current_validators": [
                        {
                            "account_id": "validator1.near",
                            "public_key": "ed25519:key1",
                            "stake": "1000000000000000000000000"
                        }
                    ],
                    "next_validators": []
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val validators = client.validators()
                val validatorsObj = validators.jsonObject
                
                val current = validatorsObj["current_validators"]?.jsonArray
                current?.size shouldBe 1
                
                val firstValidator = current?.first()?.jsonObject
                firstValidator?.get("account_id")?.jsonPrimitive?.content shouldBe "validator1.near"
            }
        }

        test("should fetch validators for specific block") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "current_validators": [],
                    "next_validators": []
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val validators = client.validators("12345")
                val validatorsObj = validators.jsonObject
                
                validatorsObj["current_validators"]?.jsonArray shouldNotBe null
                validatorsObj["next_validators"]?.jsonArray shouldNotBe null
            }
        }
    }

    context("View Operations") {
        test("should view account") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "amount": "1000000000000000000000000",
                    "locked": "0",
                    "code_hash": "11111111111111111111111111111111",
                    "storage_usage": 500
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val account = client.viewAccount("test.near")
                val accountObj = account.jsonObject
                
                accountObj["amount"]?.jsonPrimitive?.content shouldBe "1000000000000000000000000"
                accountObj["storage_usage"]?.jsonPrimitive?.int shouldBe 500
            }
            
            val request = mockWebServer.takeRequest()
            val body = request.body.readUtf8()
            body shouldContain """"request_type":"view_account""""
            body shouldContain """"account_id":"test.near""""
        }

        test("should view access key") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "nonce": 42,
                    "permission": "FullAccess"
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val key = client.viewAccessKey("test.near", "ed25519:key123")
                val keyObj = key.jsonObject
                
                keyObj["nonce"]?.jsonPrimitive?.int shouldBe 42
                keyObj["permission"]?.jsonPrimitive?.content shouldBe "FullAccess"
            }
        }

        test("should view access key list") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "keys": [
                        {
                            "public_key": "ed25519:key1",
                            "access_key": {
                                "nonce": 10,
                                "permission": "FullAccess"
                            }
                        }
                    ]
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val keyList = client.viewAccessKeyList("test.near")
                val keyListObj = keyList.jsonObject
                
                val keys = keyListObj["keys"]?.jsonArray
                keys?.size shouldBe 1
                
                val firstKey = keys?.first()?.jsonObject
                firstKey?.get("public_key")?.jsonPrimitive?.content shouldBe "ed25519:key1"
            }
        }

        test("should call contract function") {
            val args = """{"key": "value"}"""
            val argsBase64 = Base64.getEncoder().encodeToString(args.toByteArray())
            
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "result": [116, 101, 115, 116],
                    "logs": ["log1", "log2"],
                    "block_height": 12345
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val result = client.callFunction(
                    accountId = "contract.near",
                    methodName = "get_value",
                    argsBase64 = argsBase64
                )
                val resultObj = result.jsonObject
                
                val logs = resultObj["logs"]?.jsonArray
                logs?.size shouldBe 2
                logs?.get(0)?.jsonPrimitive?.content shouldBe "log1"
                
                resultObj["block_height"]?.jsonPrimitive?.int shouldBe 12345
            }
            
            val request = mockWebServer.takeRequest()
            val body = request.body.readUtf8()
            body shouldContain """"request_type":"call_function""""
            body shouldContain """"method_name":"get_value""""
            body shouldContain """"args_base64":"$argsBase64""""
        }

        test("should view contract state") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "values": [
                        {
                            "key": "a2V5",
                            "value": "dmFsdWU="
                        }
                    ],
                    "proof": []
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val state = client.viewState("contract.near", "prefix")
                val stateObj = state.jsonObject
                
                val values = stateObj["values"]?.jsonArray
                values?.size shouldBe 1
                
                val firstValue = values?.first()?.jsonObject
                firstValue?.get("key")?.jsonPrimitive?.content shouldBe "a2V5"
            }
        }
    }

    context("Experimental Methods") {
        test("should get protocol config") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "protocol_version": 54,
                    "chain_id": "testnet",
                    "epoch_length": 43200
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val config = client.protocolConfig()
                val configObj = config.jsonObject
                
                configObj["protocol_version"]?.jsonPrimitive?.int shouldBe 54
                configObj["chain_id"]?.jsonPrimitive?.content shouldBe "testnet"
                configObj["epoch_length"]?.jsonPrimitive?.int shouldBe 43200
            }
        }

        test("should get receipt") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "receipt_id": "receipt123",
                    "predecessor_id": "sender.near",
                    "receiver_id": "receiver.near"
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val receipt = client.receipt("receipt123")
                val receiptObj = receipt.jsonObject
                
                receiptObj["receipt_id"]?.jsonPrimitive?.content shouldBe "receipt123"
                receiptObj["predecessor_id"]?.jsonPrimitive?.content shouldBe "sender.near"
            }
        }
    }

    context("Error Handling") {
        test("should handle RPC errors") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "error": {
                    "code": -32601,
                    "message": "Method not found",
                    "data": "Additional info"
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                try {
                    client.status()
                    throw AssertionError("Should have thrown RpcException")
                } catch (e: RpcException) {
                    e.message shouldContain "Method not found"
                    e.error?.code shouldBe -32601
                    e.error?.message shouldBe "Method not found"
                }
            }
        }

        test("should handle HTTP errors") {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error")
            )

            runBlocking {
                try {
                    client.status()
                    throw AssertionError("Should have thrown RpcException")
                } catch (e: RpcException) {
                    e.message shouldContain "HTTP 500"
                }
            }
        }

        test("should handle malformed responses") {
            mockWebServer.enqueue(
                MockResponse()
                    .setBody("not json")
                    .setResponseCode(200)
            )

            runBlocking {
                try {
                    client.status()
                    throw AssertionError("Should have thrown exception")
                } catch (e: Exception) {
                    e shouldNotBe null
                }
            }
        }

        test("should handle empty responses") {
            mockWebServer.enqueue(
                MockResponse()
                    .setBody("")
                    .setResponseCode(200)
            )

            runBlocking {
                try {
                    client.status()
                    throw AssertionError("Should have thrown exception")
                } catch (e: Exception) {
                    e shouldNotBe null
                }
            }
        }
    }

    context("Query Operations") {
        test("should execute generic query") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "block_height": 12345,
                    "result": "query_result"
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val queryRequest = buildJsonObject {
                    put("request_type", "custom_query")
                    put("param1", "value1")
                }
                
                val result = client.query(queryRequest)
                val resultObj = result.jsonObject
                
                resultObj["block_height"]?.jsonPrimitive?.int shouldBe 12345
                resultObj["result"]?.jsonPrimitive?.content shouldBe "query_result"
            }
        }
    }

    context("Chunk Operations") {
        test("should fetch chunk by ID") {
            val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": "test-id",
                "result": {
                    "header": {
                        "chunk_hash": "chunk123",
                        "shard_id": 0
                    },
                    "transactions": []
                }
            }
            """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setBody(mockResponse)
                    .setResponseCode(200)
            )

            runBlocking {
                val chunkId = JsonPrimitive("chunk123")
                val chunk = client.chunk(chunkId)
                val chunkObj = chunk.jsonObject
                
                val header = chunkObj["header"]?.jsonObject
                header?.get("chunk_hash")?.jsonPrimitive?.content shouldBe "chunk123"
                header?.get("shard_id")?.jsonPrimitive?.int shouldBe 0
            }
        }
    }
})