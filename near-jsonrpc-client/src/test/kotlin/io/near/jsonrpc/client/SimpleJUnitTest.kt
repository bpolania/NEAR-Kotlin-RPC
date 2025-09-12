package io.near.jsonrpc.client

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.near.jsonrpc.types.*

class SimpleJUnitTest {
    
    private lateinit var mockServer: MockWebServer
    private lateinit var client: NearRpcClient
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    @BeforeEach
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        client = NearRpcClient(mockServer.url("/").toString())
    }
    
    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
    }
    
    @Test
    fun `test status endpoint`() = runBlocking {
        val mockStatus = """
        {
            "jsonrpc": "2.0",
            "id": 1,
            "result": {
                "version": {
                    "version": "1.35.0",
                    "build": "test",
                    "rustc_version": "1.70.0"
                },
                "chain_id": "testnet",
                "protocol_version": 63,
                "latest_protocol_version": 63,
                "rpc_addr": "0.0.0.0:3030",
                "validators": [],
                "sync_info": {
                    "latest_block_hash": "test_hash",
                    "latest_block_height": 100000,
                    "latest_state_root": "test_state_root",
                    "latest_block_time": "2024-01-01T00:00:00Z",
                    "syncing": false,
                    "earliest_block_hash": "earliest_hash",
                    "earliest_block_height": 1,
                    "earliest_block_time": "2023-01-01T00:00:00Z",
                    "epoch_id": "test_epoch",
                    "epoch_start_height": 99000
                },
                "validator_account_id": null,
                "node_public_key": "test_key",
                "uptime_sec": 3600
            }
        }
        """.trimIndent()
        
        mockServer.enqueue(MockResponse()
            .setBody(mockStatus)
            .setHeader("Content-Type", "application/json"))
        
        val status = client.status()
        
        assertNotNull(status)
        assertEquals("testnet", status.chainId)
        assertEquals(100000L, status.syncInfo.latestBlockHeight)
        assertFalse(status.syncInfo.syncing)
    }
    
    @Test
    fun `test view account`() = runBlocking {
        val mockAccount = """
        {
            "jsonrpc": "2.0",
            "id": 1,
            "result": {
                "amount": "1000000000000000000000000",
                "locked": "0",
                "code_hash": "11111111111111111111111111111111",
                "storage_usage": 500,
                "storage_paid_at": 0,
                "block_height": 100000,
                "block_hash": "test_block_hash"
            }
        }
        """.trimIndent()
        
        mockServer.enqueue(MockResponse()
            .setBody(mockAccount)
            .setHeader("Content-Type", "application/json"))
        
        val account = client.viewAccount("test.near")
        
        assertNotNull(account)
        assertEquals("1000000000000000000000000", account.amount)
        assertEquals("0", account.locked)
        assertEquals(500L, account.storageUsage)
    }
    
    @Test
    fun `test gas price`() = runBlocking {
        val mockGasPrice = """
        {
            "jsonrpc": "2.0",
            "id": 1,
            "result": {
                "gas_price": "100000000"
            }
        }
        """.trimIndent()
        
        mockServer.enqueue(MockResponse()
            .setBody(mockGasPrice)
            .setHeader("Content-Type", "application/json"))
        
        val gasPrice = client.gasPrice()
        
        assertNotNull(gasPrice)
        assertEquals("100000000", gasPrice.gasPrice)
    }
}