package blockchain

import java.security.PrivateKey
import java.security.PublicKey

class User(private val name: String, private val blockChain: BlockChain) : Thread() {
    private val messages: List<String>
        get() = listOf(
            "Hello!",
            "Where are you?",
            "Who are you?",
            "Today I went for a 10k run!",
            "I was done in 30 min.",
            "We are going hiking tomorrow. Does anyone want to join?",
            "What a beautiful day we have here today!",
            "The world is beautiful if we just let it.",
            "We have been given a paradise where we can all live in safety and peace, with everything in abundance - if we want to.",
            "Nicely said. That is very philosophical.",
            "This sentence is false!",
            "I am 6 years old and my preschool math teacher told us to solve this before tomorrow: \nThere is a circular area of grass for a goat to eat. The goat is tied with a rope from the edge of the circle. However, the goat is only allowed to eat half of the grass. How long should the rope be?\nCan someone help me?",
            "That's one small step for a man, one giant leap for mankind.",
            "We believe that when men reach beyond this planet, they should leave their national differences behind them.",
            "We do not realize what we have on Earth until we leave it.",
            "Never limit yourself because of others’ limited imagination; never limit others because of your own limited imagination.",
            "I know the sky is not the limit because there are footprints on the Moon — and I made some of them!",
            "Your time is limited, so don’t waste it.",
            "There is much pleasure to be gained from useless knowledge.",
            "Opinion is the medium between knowledge and ignorance.",
            "We are all born ignorant, but one must work hard to remain stupid.",
            "There is a big difference between those who find reasons to help others and those who search for reasons not to.",
            "Blessed is the one in whose spirit is no deceit.",
            "It is important to listen to others. But it is even more important not to do all the stupid things people tell you to do.",
            "To acquire knowledge, one must study. But to acquire wisdom, one must observe.",
            "There are 10 kinds of people in the world: Those who understand binary, and those who don't."
        )

    private val privateKey: PrivateKey
    private val publicKey: PublicKey

    init {
        val keyPair = Signature.createKeyPair()
        this.privateKey = keyPair.private
        this.publicKey = keyPair.public
    }

    override fun run() {
        while (blockChain.keepMining()) {
            sleep((1..10000).random().toLong())
            val message = this.messages.random()
            var messageId = this.blockChain.requestMessageId()
            var mySignature = Signature.signMessage(ChatMessage(messageId, this.name, message), this.privateKey)
            var chat = ChatMessage(messageId, this.name, message, mySignature, this.publicKey)
            while (!this.blockChain.addChat(chat)) {
                messageId = this.blockChain.requestMessageId()
                mySignature = Signature.signMessage(ChatMessage(messageId, this.name, message), this.privateKey)
                chat = ChatMessage(messageId, this.name, message, mySignature, this.publicKey)
            }
        }
    }
}