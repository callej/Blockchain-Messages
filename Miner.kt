package blockchain

import kotlin.random.Random

class Miner(private val minerId: Int, private val blockChain: BlockChain) : Thread() {
    override fun run() {
         nextSpec@ while (this.blockChain.keepMining()) {
             if (this.blockChain.newBlockAvailableForMining()) {
                 val timestamp: Long = System.currentTimeMillis()
                 val blockSpecs = this.blockChain.getBlockSpecs()
                 var newBlock = Block(blockSpecs, Random.nextLong(), System.currentTimeMillis() - timestamp, this.minerId)
                 while (newBlock.calculateHash().take(blockSpecs.powZeros) != "0".repeat(blockSpecs.powZeros)) {
                     newBlock = Block(blockSpecs, Random.nextLong(), System.currentTimeMillis() - timestamp, this.minerId)
                     if (blockSpecs != this.blockChain.getBlockSpecs() || !this.blockChain.keepMining()) {
                         continue@nextSpec
                     }
                 }
                 this.blockChain.addBlock(newBlock)
             }
        }
    }
}