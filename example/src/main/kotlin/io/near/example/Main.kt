package io.near.example

import io.near.jsonrpc.client.NearRpcClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.util.Base64

/**
 * Example usage of NEAR Kotlin RPC Client
 * Demonstrates various RPC methods available
 */
fun main() = runBlocking {
    println("==============================================")
    println("NEAR Kotlin RPC Client Example")
    println("==============================================\n")
    
    // Initialize client for mainnet
    val client = NearRpcClient("https://rpc.mainnet.near.org")
    
    try {
        // Example 1: Get network status
        println("1. Fetching network status...")
        val status = client.status()
        val statusObj = status.jsonObject
        println("   Chain ID: ${statusObj["chain_id"]?.jsonPrimitive?.content}")
        println("   Latest protocol version: ${statusObj["latest_protocol_version"]}")
        println("   Node version: ${statusObj["version"]?.jsonObject?.get("version")}")
        println()
        
        // Example 2: Get latest block
        println("2. Fetching latest block...")
        val blockId = "final"  // Can use block hash or height
        val block = client.block(blockId)
        val blockObj = block.jsonObject
        val header = blockObj["header"]?.jsonObject
        println("   Block height: ${header?.get("height")}")
        println("   Block hash: ${header?.get("hash")}")
        println("   Author: ${blockObj["author"]}")
        val chunks = blockObj["chunks"]?.jsonArray
        println("   Number of chunks: ${chunks?.size}")
        println()
        
        // Example 3: View account
        println("3. Viewing account 'near'...")
        val account = client.viewAccount("near")
        val accountObj = account.jsonObject
        println("   Balance: ${accountObj["amount"]} yoctoNEAR")
        println("   Locked: ${accountObj["locked"]} yoctoNEAR")
        println("   Storage used: ${accountObj["storage_usage"]} bytes")
        println("   Code hash: ${accountObj["code_hash"]}")
        println()
        
        // Example 4: Get gas price
        println("4. Fetching current gas price...")
        val gasPrice = client.gasPrice()
        val gasPriceObj = gasPrice.jsonObject
        println("   Gas price: ${gasPriceObj["gas_price"]} yoctoNEAR")
        println()
        
        // Example 5: Get validators
        println("5. Fetching validators...")
        val validators = client.validators()
        val validatorsObj = validators.jsonObject
        val currentValidators = validatorsObj["current_validators"]?.jsonArray
        val nextValidators = validatorsObj["next_validators"]?.jsonArray
        println("   Current validators: ${currentValidators?.size ?: 0}")
        println("   Next validators: ${nextValidators?.size ?: 0}")
        
        currentValidators?.firstOrNull()?.let { firstValidator ->
            val validator = firstValidator.jsonObject
            println("   Example validator: ${validator["account_id"]?.jsonPrimitive?.content}")
            println("   - Stake: ${validator["stake"]}")
            println("   - Expected blocks: ${validator["num_expected_blocks"]}")
            println("   - Produced blocks: ${validator["num_produced_blocks"]}")
        }
        println()
        
        // Example 6: Call view function on a contract
        println("6. Calling view function on a contract...")
        val contractId = "name.near"
        val methodName = "get"
        val args = "{}" // Empty JSON object
        val argsBase64 = Base64.getEncoder().encodeToString(args.toByteArray())
        
        try {
            val result = client.callFunction(
                accountId = contractId,
                methodName = methodName,
                argsBase64 = argsBase64
            )
            val resultObj = result.jsonObject
            println("   Contract: $contractId")
            println("   Method: $methodName")
            val resultBytes = resultObj["result"]?.jsonArray
            println("   Result bytes: ${resultBytes?.size ?: 0}")
            val logs = resultObj["logs"]?.jsonArray
            println("   Logs: ${logs?.size ?: 0} entries")
            
            // Decode result if it's a string
            resultBytes?.let { bytes ->
                val byteArray = bytes.map { it.jsonPrimitive.int.toByte() }.toByteArray()
                val resultString = String(byteArray)
                println("   Result (decoded): $resultString")
            }
        } catch (e: Exception) {
            println("   Note: Contract call example failed (expected if contract doesn't exist)")
            println("   Error: ${e.message}")
        }
        println()
        
        // Example 7: Get network info
        println("7. Fetching network info...")
        val networkInfo = client.networkInfo()
        val networkObj = networkInfo.jsonObject
        println("   Active peers: ${networkObj["active_peers"]?.jsonArray?.size ?: 0}")
        println("   Max peers: ${networkObj["peer_max_count"]}")
        println("   Sent bytes per sec: ${networkObj["sent_bytes_per_sec"]}")
        println("   Received bytes per sec: ${networkObj["received_bytes_per_sec"]}")
        println()
        
        // Example 8: Get protocol config (EXPERIMENTAL)
        println("8. Fetching protocol config (EXPERIMENTAL)...")
        val protocolConfig = client.protocolConfig()
        val protocolObj = protocolConfig.jsonObject
        println("   Protocol version: ${protocolObj["protocol_version"]}")
        println("   Chain ID: ${protocolObj["chain_id"]}")
        println("   Epoch length: ${protocolObj["epoch_length"]}")
        println()
        
        println("==============================================")
        println("Example completed successfully!")
        println("==============================================")
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}