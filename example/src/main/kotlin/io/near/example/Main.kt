package io.near.example

import io.near.jsonrpc.client.NearRpcClient
import io.near.jsonrpc.types.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Example usage of NEAR Kotlin RPC Client
 * Similar to the example in near-openapi-client (Rust implementation)
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
        println("   Chain ID: ${status.chainId}")
        println("   Latest block height: ${status.syncInfo.latestBlockHeight}")
        println("   Syncing: ${status.syncInfo.syncing}")
        println()
        
        // Example 2: Get latest block
        println("2. Fetching latest block...")
        val block = client.block(BlockReference(finality = Finality.FINAL))
        println("   Block height: ${block.header.height}")
        println("   Block hash: ${block.header.hash}")
        println("   Author: ${block.author}")
        println("   Number of chunks: ${block.chunks.size}")
        println()
        
        // Example 3: View account
        println("3. Viewing account 'near'...")
        val account = client.viewAccount("near")
        println("   Balance: ${account.amount} yoctoNEAR")
        println("   Locked: ${account.locked} yoctoNEAR")
        println("   Storage used: ${account.storageUsage} bytes")
        println("   Code hash: ${account.codeHash}")
        println()
        
        // Example 4: Get gas price
        println("4. Fetching current gas price...")
        val gasPrice = client.gasPrice()
        println("   Gas price: ${gasPrice.gasPrice} yoctoNEAR")
        println()
        
        // Example 5: Get validators
        println("5. Fetching validators...")
        val validators = client.validators()
        println("   Current validators: ${validators.currentValidators.size}")
        println("   Next validators: ${validators.nextValidators.size}")
        if (validators.currentValidators.isNotEmpty()) {
            val firstValidator = validators.currentValidators.first()
            println("   Example validator: ${firstValidator.accountId}")
            println("   - Stake: ${firstValidator.stake}")
            println("   - Expected blocks: ${firstValidator.numExpectedBlocks}")
            println("   - Produced blocks: ${firstValidator.numProducedBlocks}")
        }
        println()
        
        // Example 6: Call view function on a contract
        println("6. Calling view function on a contract...")
        val contractId = "name.near"
        val methodName = "get"
        val args = "{}" // Empty JSON object
        val argsBase64 = args.toByteArray().encodeBase64()
        
        try {
            val result = client.callFunction(
                accountId = contractId,
                methodName = methodName,
                argsBase64 = argsBase64
            )
            println("   Contract: $contractId")
            println("   Method: $methodName")
            println("   Result bytes: ${result.result.size}")
            println("   Logs: ${result.logs}")
        } catch (e: Exception) {
            println("   Note: Contract call example failed (expected if contract doesn't exist)")
            println("   Error: ${e.message}")
        }
        
        println("\n==============================================")
        println("Example completed successfully!")
        println("==============================================")
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}

fun ByteArray.encodeBase64(): String {
    return java.util.Base64.getEncoder().encodeToString(this)
}