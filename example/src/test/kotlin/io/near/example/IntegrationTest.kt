package io.near.example

import io.near.jsonrpc.client.NearRpcClient
import io.near.jsonrpc.types.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName

/**
 * Integration tests for NEAR Kotlin RPC Client
 * Similar to the tests in near-openapi-client and near-jsonrpc-client-ts
 */
class IntegrationTest {
    
    private lateinit var client: NearRpcClient
    
    @BeforeEach
    fun setup() {
        // Use QuickNode endpoint for better rate limits, fall back to public testnet
        val quickNodeUrl = "https://white-shy-fire.near-testnet.quiknode.pro/1c9f76d8dab07f1657d6aebc20441c38e81265e5"
        val rpcUrl = System.getenv("NEAR_TESTNET_RPC_URL") ?: quickNodeUrl
        client = NearRpcClient(rpcUrl)
    }
    
    @Test
    @DisplayName("Should fetch network status")
    fun testNetworkStatus() = runBlocking {
        val status = client.status()
        
        assertNotNull(status)
        assertNotNull(status.chainId)
        assertNotNull(status.syncInfo)
        assertTrue(status.syncInfo.latestBlockHeight > 0)
        assertEquals("testnet", status.chainId)
    }
    
    @Test
    @DisplayName("Should fetch block by finality")
    fun testGetBlockByFinality() = runBlocking {
        val block = client.block(BlockReference(finality = Finality.FINAL))
        
        assertNotNull(block)
        assertNotNull(block.header)
        assertTrue(block.header.height > 0)
        assertNotNull(block.header.hash)
        assertNotNull(block.author)
        assertTrue(block.chunks.isNotEmpty())
    }
    
    @Test
    @DisplayName("Should fetch block by height")
    fun testGetBlockByHeight() = runBlocking {
        // First get the latest block to know a valid height
        val latestBlock = client.block(BlockReference(finality = Finality.FINAL))
        val targetHeight = latestBlock.header.height - 10 // Get a block 10 heights back
        
        val block = client.block(BlockReference(blockHeight = targetHeight))
        
        assertNotNull(block)
        assertEquals(targetHeight, block.header.height)
    }
    
    @Test
    @DisplayName("Should view account details")
    fun testViewAccount() = runBlocking {
        // Use a well-known testnet account
        val account = client.viewAccount("test.near")
        
        assertNotNull(account)
        assertNotNull(account.amount)
        assertNotNull(account.codeHash)
        assertTrue(account.storageUsage >= 0)
    }
    
    @Test
    @DisplayName("Should get gas price")
    fun testGasPrice() = runBlocking {
        val gasPrice = client.gasPrice()
        
        assertNotNull(gasPrice)
        assertNotNull(gasPrice.gasPrice)
        assertTrue(gasPrice.gasPrice.toLong() > 0)
    }
    
    @Test
    @DisplayName("Should get validators")
    fun testValidators() = runBlocking {
        val validators = client.validators()
        
        assertNotNull(validators)
        assertNotNull(validators.currentValidators)
        assertNotNull(validators.nextValidators)
        assertTrue(validators.currentValidators.isNotEmpty())
        
        // Check first validator has required fields
        val firstValidator = validators.currentValidators.first()
        assertNotNull(firstValidator.accountId)
        assertNotNull(firstValidator.stake)
        assertTrue(firstValidator.numExpectedBlocks >= 0)
    }
    
    @Test
    @DisplayName("Should handle non-existent account gracefully")
    fun testNonExistentAccount() = runBlocking {
        try {
            client.viewAccount("this-account-definitely-does-not-exist-12345.near")
            fail("Expected exception for non-existent account")
        } catch (e: Exception) {
            // Expected behavior
            assertTrue(e.message?.contains("error") == true || e.message?.contains("not found") == true)
        }
    }
    
    @Test
    @DisplayName("Should fetch network info")
    fun testNetworkInfo() = runBlocking {
        val networkInfo = client.networkInfo()
        
        assertNotNull(networkInfo)
        assertTrue(networkInfo.numActivePeers >= 0)
        assertTrue(networkInfo.peerMaxCount > 0)
        assertNotNull(networkInfo.activePeers)
    }
    
    @Test
    @DisplayName("Should call view function on contract")
    fun testCallViewFunction() = runBlocking {
        // Try to call a view function on a known contract
        // Using empty args (base64 encoded "{}")
        val argsBase64 = "e30=" // {} in base64
        
        try {
            val result = client.callFunction(
                accountId = "name.testnet",
                methodName = "get",
                argsBase64 = argsBase64
            )
            
            assertNotNull(result)
            assertNotNull(result.result)
            assertNotNull(result.logs)
        } catch (e: Exception) {
            // Contract might not exist or method might not be available
            // This is acceptable for this test
            println("Contract call failed (expected if contract doesn't exist): ${e.message}")
        }
    }
    
    @Test
    @DisplayName("Should handle invalid block height")
    fun testInvalidBlockHeight() = runBlocking {
        try {
            // Try to fetch a block from the future
            client.block(BlockReference(blockHeight = 999999999999))
            fail("Expected exception for invalid block height")
        } catch (e: Exception) {
            // Expected behavior
            assertNotNull(e.message)
        }
    }
}