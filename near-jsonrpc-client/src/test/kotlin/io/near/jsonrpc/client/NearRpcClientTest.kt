package io.near.jsonrpc.client

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.near.jsonrpc.types.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class NearRpcClientTest : FunSpec({

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

    test("should fetch network status") {
        val mockResponse = """
        {
            "jsonrpc": "2.0",
            "id": "test-id",
            "result": {
                "chain_id": "mainnet",
                "sync_info": {
                    "latest_block_hash": "test-hash",
                    "latest_block_height": 100,
                    "latest_block_time": "2024-01-01T00:00:00Z",
                    "latest_state_root": "state-root",
                    "syncing": false
                },
                "validators": []
            }
        }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
        )

        runBlocking {
            val status = client.status()
            status.chainId shouldBe "mainnet"
            status.syncInfo.latestBlockHeight shouldBe 100
            status.syncInfo.syncing shouldBe false
        }
    }

    test("should view account") {
        val mockResponse = """
        {
            "jsonrpc": "2.0",
            "id": "test-id",
            "result": {
                "amount": "1000000000000000000000000",
                "locked": "0",
                "code_hash": "11111111111111111111111111111111",
                "storage_usage": 1000,
                "storage_paid_at": 0,
                "block_height": 50000,
                "block_hash": "block-hash"
            }
        }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
        )

        runBlocking {
            val account = client.viewAccount("test.near")
            account.amount shouldBe "1000000000000000000000000"
            account.storageUsage shouldBe 1000
            account.blockHeight shouldBe 50000
        }
    }

    test("should handle RPC errors") {
        val mockResponse = """
        {
            "jsonrpc": "2.0",
            "id": "test-id",
            "error": {
                "code": -32601,
                "message": "Method not found",
                "data": null
            }
        }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
        )

        runBlocking {
            try {
                client.status()
                throw AssertionError("Should have thrown RpcException")
            } catch (e: RpcException) {
                e.message shouldNotBe null
                e.error?.code shouldBe -32601
                e.error?.message shouldBe "Method not found"
            }
        }
    }

    test("should call contract function") {
        val mockResponse = """
        {
            "jsonrpc": "2.0",
            "id": "test-id",
            "result": {
                "result": [123, 34, 116, 101, 115, 116, 34, 58, 34, 118, 97, 108, 117, 101, 34, 125],
                "logs": ["Log message 1", "Log message 2"],
                "block_height": 60000,
                "block_hash": "function-block-hash"
            }
        }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
        )

        runBlocking {
            val response = client.callFunction(
                accountId = "contract.near",
                methodName = "get_status",
                argsBase64 = "e30="
            )
            response.logs.size shouldBe 2
            response.logs[0] shouldBe "Log message 1"
            response.blockHeight shouldBe 60000
        }
    }

    test("should fetch block by height") {
        val mockResponse = """
        {
            "jsonrpc": "2.0",
            "id": "test-id",
            "result": {
                "author": "validator.near",
                "header": {
                    "height": 12345,
                    "hash": "block-hash",
                    "prev_hash": "prev-hash",
                    "epoch_id": "epoch-id",
                    "next_epoch_id": "next-epoch-id",
                    "chunks_included": 4,
                    "challenges_root": "challenges-root",
                    "timestamp": 1640000000000,
                    "timestamp_nanosec": "1640000000000000000",
                    "random_value": "random",
                    "validator_proposals": [],
                    "chunk_mask": [true, true, true, true],
                    "gas_price": "100000000",
                    "total_supply": "1000000000000000000000000000",
                    "challenges_result": [],
                    "last_final_block": "final-block",
                    "last_ds_final_block": "ds-final-block",
                    "next_bp_hash": "bp-hash",
                    "block_merkle_root": "merkle-root",
                    "approvals": [],
                    "signature": "signature",
                    "latest_protocol_version": 50
                },
                "chunks": []
            }
        }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
        )

        runBlocking {
            val block = client.block(BlockReference(blockHeight = 12345))
            block.author shouldBe "validator.near"
            block.header.height shouldBe 12345
            block.header.gasPrice shouldBe "100000000"
        }
    }
})