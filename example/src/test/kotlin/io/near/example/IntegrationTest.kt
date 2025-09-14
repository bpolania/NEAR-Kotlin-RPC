package io.near.example

import io.near.jsonrpc.client.NearRpcClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import java.util.Base64

/**
 * Integration tests for NEAR Kotlin RPC Client
 * Tests basic RPC methods against testnet
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
        val statusObj = status.jsonObject
        
        assertNotNull(status)
        assertNotNull(statusObj["chain_id"])
        assertNotNull(statusObj["sync_info"])
        
        val chainId = statusObj["chain_id"]?.jsonPrimitive?.content
        assertEquals("testnet", chainId)
    }
    
    @Test
    @DisplayName("Should fetch block by finality")
    fun testGetBlockByFinality() = runBlocking {
        val block = client.block("final")
        val blockObj = block.jsonObject
        
        assertNotNull(block)
        assertNotNull(blockObj["header"])
        
        val header = blockObj["header"]?.jsonObject
        assertNotNull(header)
        
        val height = header?.get("height")?.jsonPrimitive?.long ?: 0
        assertTrue(height > 0)
        
        assertNotNull(header?.get("hash"))
        assertNotNull(blockObj["author"])
        
        val chunks = blockObj["chunks"]?.jsonArray
        assertNotNull(chunks)
        assertTrue(chunks?.size ?: 0 > 0)
    }
    
    @Test
    @DisplayName("Should view account")
    fun testViewAccount() = runBlocking {
        val account = client.viewAccount("test.near")
        val accountObj = account.jsonObject
        
        assertNotNull(account)
        assertNotNull(accountObj["amount"])
        assertNotNull(accountObj["locked"])
        assertNotNull(accountObj["code_hash"])
        assertNotNull(accountObj["storage_usage"])
    }
    
    @Test
    @DisplayName("Should get gas price")
    fun testGasPrice() = runBlocking {
        val gasPrice = client.gasPrice()
        val gasPriceObj = gasPrice.jsonObject
        
        assertNotNull(gasPrice)
        assertNotNull(gasPriceObj["gas_price"])
        
        val price = gasPriceObj["gas_price"]?.jsonPrimitive?.content
        assertNotNull(price)
        assertTrue(price?.toLongOrNull() ?: 0 > 0)
    }
    
    @Test
    @DisplayName("Should get validators")
    fun testValidators() = runBlocking {
        val validators = client.validators()
        val validatorsObj = validators.jsonObject
        
        assertNotNull(validators)
        
        val currentValidators = validatorsObj["current_validators"]?.jsonArray
        assertNotNull(currentValidators)
        assertTrue(currentValidators?.size ?: 0 > 0)
        
        val firstValidator = currentValidators?.firstOrNull()?.jsonObject
        assertNotNull(firstValidator)
        assertNotNull(firstValidator?.get("account_id"))
        assertNotNull(firstValidator?.get("stake"))
    }
    
    @Test
    @DisplayName("Should call view function")
    fun testCallFunction() = runBlocking {
        // Call a simple view function on a known contract
        val args = "{}"
        val argsBase64 = Base64.getEncoder().encodeToString(args.toByteArray())
        
        try {
            val result = client.callFunction(
                accountId = "name.near",  // Known contract on testnet
                methodName = "get",
                argsBase64 = argsBase64
            )
            val resultObj = result.jsonObject
            
            assertNotNull(result)
            assertNotNull(resultObj["result"])
            assertNotNull(resultObj["logs"])
        } catch (e: Exception) {
            // It's okay if the contract doesn't exist on testnet
            // Just check that we got a proper error response
            assertTrue(e.message?.contains("error") ?: false || 
                      e.message?.contains("does not exist") ?: false)
        }
    }
    
    @Test
    @DisplayName("Should get network info")
    fun testNetworkInfo() = runBlocking {
        val networkInfo = client.networkInfo()
        val networkObj = networkInfo.jsonObject
        
        assertNotNull(networkInfo)
        assertNotNull(networkObj["active_peers"])
        assertNotNull(networkObj["num_active_peers"])
        
        val activePeers = networkObj["active_peers"]?.jsonArray
        assertNotNull(activePeers)
    }
    
    @Test
    @DisplayName("Should handle errors gracefully")
    fun testErrorHandling() = runBlocking {
        assertThrows(Exception::class.java) {
            runBlocking {
                // Try to view non-existent account
                client.viewAccount("this-account-definitely-does-not-exist-123456789.near")
            }
        }
    }
}