package actors

import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.utility.SigmaProtocolMsg
import edu.biu.scapi.midLayer.asymmetricCrypto.keys._
import edu.biu.scapi.midLayer.ciphertext.BigIntegerCiphertext

sealed trait Message
case object Register extends Message
case object Multiply extends Message
case class Invitation(pk: DamgardJurikPublicKey) extends Message

case class EncryptedNumber(n: BigIntegerCiphertext) extends Message
case class EncryptedNumbers(n1: BigIntegerCiphertext, n2: BigIntegerCiphertext)
    extends Message
case class EncryptedResult(n: BigIntegerCiphertext) extends Message

case object RequestProve extends Message
case object ProvideProve extends Message
case class ProverResult(success: Boolean) extends Message
case class Challenge(challenge: Array[Byte]) extends Message
case class Message1(msg: SigmaProtocolMsg) extends Message
case class Message2(msg: SigmaProtocolMsg) extends Message
case object ShowProverResult extends Message


