package blockchain

import java.math.BigInteger
import java.security.MessageDigest
import java.security.PublicKey

const val HASH_FUNCTION = "SHA-256"
const val POW_LOW_LIMIT_SEC = 10
const val POW_HIGH_LIMIT_SEC = 60

data class ChatMessage(val id: Int, val user: String, val message: String, val sign: ByteArray = ByteArray(0), val publicKey: PublicKey = Signature.createKeyPair().public) {
    fun toByteArray() = (this.id.toString() + user + message).toByteArray()
}

data class BlockSpec(val id: Int, val previousHash: String, val powZeros: Int, val chats: MutableList<ChatMessage>)

data class Block(private val blockSpec: BlockSpec, private val magic: Long, private val creationTime: Long, private val minerId: Int) {
    val id = blockSpec.id
    val previousHash = blockSpec.previousHash
    private val powZeros = blockSpec.powZeros
    private val chatList = blockSpec.chats
    private val timestamp: Long = System.currentTimeMillis()
    private val hash = calculateHash()
    private val maxMessageId = blockSpec.chats.maxOfOrNull { it.id } ?: 0
    private val minMessageId = blockSpec.chats.minOfOrNull { it.id } ?: 0

    private fun dataString(): String {
        var dataStr = "Block data:"
        if (this.chatList.isEmpty()) {
            return "$dataStr no messages"
        } else {
            for (chat in this.chatList) {
                dataStr += "\n${chat.user}: ${chat.message}"
            }
            return dataStr
        }
    }

    fun calculateHash(): String {
        return (this.id.toString() +
                this.timestamp.toString() +
                this.creationTime.toString() +
                this.magic.toString() +
                this.powZeros.toString() +
                this.previousHash +
                this.minerId.toString() +
                this.chatList.toString()).hash()
    }

    fun getStoredHash() = this.hash

    fun getPowZeros() = this.powZeros

    fun getCreationTime() = this.creationTime

    fun verifyChats(): Boolean {
        for (chat in this.chatList) {
            if (!Signature.verifySignature(chat)) {
                return false
            }
        }
        return true
    }

    fun getMaxMessageId() = this.maxMessageId

    fun getMinMessageId() = this.minMessageId

    private fun String.hash(): String {
        val md = MessageDigest.getInstance(HASH_FUNCTION)
        return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(64, '0')
    }

    override fun toString(): String {
        return "Block:\n" +
                "Created by miner # ${this.minerId}\n" +
                "Id: ${this.id}\n" +
                "Timestamp: ${this.timestamp}\n" +
                "Magic number: ${this.magic}\n" +
                "Hash of the previous block:\n${this.previousHash}\n" +
                "Hash of the block:\n${this.hash}\n" +
                "${dataString()}\n" +
                "Block was generating for ${this.creationTime / 1000} seconds"
    }
}

class BlockChain(private val maxLength: Int) {
    private val blockChain = emptyList<Block>().toMutableList()
    @Volatile private var powZeros = 0
    @Volatile private var doneMining = false
    @Volatile private var miningAvailable = true
    private val chatList = emptyList<ChatMessage>().toMutableList()
    private var blockSpecs = BlockSpec(1, "0", powZeros, ArrayList(chatList))
    @Volatile private var messageId = 1
    private val miningLock = Any()
    private val chatLock = Any()
    private val idLock = Any()

    private fun setupNextBlock() {
        synchronized(this.chatLock) {
            if (this.chatList.isEmpty()) {
                this.miningAvailable = false
            } else {
                this.blockSpecs = BlockSpec(blockChain.size + 1, blockChain.last().getStoredHash(), powZeros, ArrayList(chatList))
                this.chatList.clear()
                this.miningAvailable = true
            }
        }
    }

    fun getBlockSpecs() = this.blockSpecs

    fun requestMessageId(): Int {
        synchronized(this.idLock) {
            return this.messageId++
        }
    }

    fun addChat(chat: ChatMessage): Boolean {
        synchronized(this.chatLock) {
            if (chat.id > (this.blockSpecs.chats.maxOfOrNull { it.id } ?: 0)) {
                this.chatList.add(chat)
                if (!this.miningAvailable) {
                    this.setupNextBlock()
                }
                return true
            } else {
                return false
            }
        }
    }

    fun addBlock(block: Block) {
        synchronized(this.miningLock) {
            if (this.validNewBlock(block)) {
                this.blockChain.add(block)
                println(block)
                when {
                    (block.getCreationTime() / 1000) < POW_LOW_LIMIT_SEC.toLong() -> {
                        powZeros++
                        println("N was increased to $powZeros\n")
                    }

                    ((block.getCreationTime() / 1000) > POW_HIGH_LIMIT_SEC.toLong()) && (this.powZeros > 0) -> {
                        this.powZeros--
                        println("N was decreased by 1\n")
                    }

                    else -> println("N stays the same\n")
                }
                this.setupNextBlock()
                this.doneMining = this.blockChain.size >= this.maxLength
            }
        }
    }

    private fun validNewBlock(block: Block): Boolean {
        if (this.blockChain.isEmpty()) {
            return ((block.id == 1) &&
                    (block.previousHash == "0") &&
                    (block.getStoredHash() == block.calculateHash()) &&
                    (block.getStoredHash().take(this.powZeros) == "0".repeat(this.powZeros)) &&
                    (block.getPowZeros() == this.powZeros) &&
                    (block.getMaxMessageId() == 0) &&
                    (block.getMinMessageId() == 0))
        } else {
            return ((block.id == (this.blockChain.last().id + 1)) &&
                    (block.previousHash == this.blockChain.last().getStoredHash()) &&
                    (block.getStoredHash() == block.calculateHash()) &&
                    (block.getStoredHash().take(this.powZeros) == "0".repeat(this.powZeros)) &&
                    (block.getPowZeros() == this.powZeros) &&
                    (block.verifyChats()) &&
                    (block.getMinMessageId() > this.blockChain.last().getMaxMessageId()) &&
                    (this.validChain()))
        }
    }

    private fun validBlock(block: Block): Boolean {
        if (block.id < 1) return false
        if (block.id == 1) {
            return ((block.previousHash == "0") &&
                    (block.getStoredHash() == block.calculateHash()) &&
                    (block.getStoredHash().take(block.getPowZeros()) == "0".repeat(block.getPowZeros())) &&
                    (block.getMaxMessageId() == 0) &&
                    (block.getMinMessageId() == 0))
        } else {
            return ((block.id == (this.blockChain[block.id - 2].id + 1)) &&
                    (block.previousHash == this.blockChain[block.id - 2].getStoredHash()) &&
                    (block.getStoredHash() == block.calculateHash()) &&
                    (block.getStoredHash().take(block.getPowZeros()) == "0".repeat(block.getPowZeros())) &&
                    block.verifyChats())
        }
    }

    private fun validChain(): Boolean {
        if (this.blockChain[0].id != 1) return false
        for (index in this.blockChain.indices) {
            if (!this.validBlock(this.blockChain[index])) return false
            if ((index > 0) && (this.blockChain[index].getMinMessageId() <= this.blockChain[index - 1].getMaxMessageId())) return false
        }
        return true
    }

    fun keepMining() = !this.doneMining

    fun newBlockAvailableForMining() = this.miningAvailable

    override fun toString(): String {
        var str = ""
        for (block in this.blockChain) {
            str += "$block\n"
        }
        return str
    }
}