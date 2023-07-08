package blockchain

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature

const val KEYPAIR_ALGORITHM = "RSA"
const val KEY_SIZE = 2048
const val SIGNATURE_ALGORITHM = "SHA256withRSA"

class Signature {

    companion object {
        fun createKeyPair(): KeyPair {
            val kpGen = KeyPairGenerator.getInstance(KEYPAIR_ALGORITHM)
            kpGen.initialize(KEY_SIZE)
            return kpGen.genKeyPair()
        }

        fun signMessage(chat: ChatMessage, privateKey: PrivateKey): ByteArray {
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initSign(privateKey)
            signature.update(chat.toByteArray())
            return signature.sign()
        }

        fun verifySignature(chat: ChatMessage): Boolean {
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initVerify(chat.publicKey)
            signature.update(chat.toByteArray())
            return signature.verify(chat.sign)
        }
    }
}