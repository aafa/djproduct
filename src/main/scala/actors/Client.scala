package actors

import java.math.BigInteger

import akka.actor.{Actor, ActorLogging, ActorRef}
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.damgardJurikProduct.{SigmaDJProductCommonInput, SigmaDJProductVerifierComputation}
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.utility.SigmaProtocolMsg
import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.{DamgardJurikEnc, ScDamgardJurikEnc}
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.DamgardJurikPublicKey
import edu.biu.scapi.midLayer.ciphertext.BigIntegerCiphertext
import edu.biu.scapi.midLayer.plaintext.BigIntegerPlainText


class Client(number: BigInteger, broker: ActorRef)
  extends Actor
    with ActorLogging {
  var cA: Option[BigIntegerCiphertext] = None
  var cB: Option[BigIntegerCiphertext] = None
  var cC: Option[BigIntegerCiphertext] = None
  var djPublicKey: Option[DamgardJurikPublicKey] = None
  var protocolMsg1: Option[SigmaProtocolMsg] = None

  val djVerifierComputation: SigmaDJProductVerifierComputation =
    new SigmaDJProductVerifierComputation()

  def djCommonInput: Option[SigmaDJProductCommonInput] =
    for {
      pk <- djPublicKey
      _cA <- cA
      _cB <- cB
      _cC <- cC
    } yield new SigmaDJProductCommonInput(pk, _cA, _cB, _cC)

  def encryptNumber(n: BigInteger,
                    pk: DamgardJurikPublicKey): BigIntegerCiphertext = {
    val enc: DamgardJurikEnc = new ScDamgardJurikEnc()
    enc.setKey(pk)
    enc
      .encrypt(new BigIntegerPlainText(n))
      .asInstanceOf[BigIntegerCiphertext]
  }

  override def preStart(): Unit = broker ! Register

  override def receive: PartialFunction[Any, Unit] = {
    case Invitation(pk: DamgardJurikPublicKey) if djPublicKey.isEmpty =>
      djPublicKey = Some(pk)
      val cipherText: BigIntegerCiphertext = encryptNumber(number, pk)
      log.info(s"Invitation received; sending cipherText $cipherText")
      broker ! EncryptedNumber(cipherText)

    case EncryptedNumbers(n1, n2) =>
      cA = Some(n1)
      cB = Some(n2)

    case EncryptedResult(n: BigIntegerCiphertext) if cC.isEmpty =>
      cC = Some(n)
      broker ! RequestProve

    case Message1(msg1) if protocolMsg1.isEmpty =>
      protocolMsg1 = Some(msg1)
      djVerifierComputation.sampleChallenge()
      val challenge: Array[Byte] = djVerifierComputation.getChallenge
      log.info(s"Message1 received, sending challenge ${challenge.toList}")
      sender() ! Challenge(challenge)

    case Message2(msg2) if protocolMsg1.isDefined && djCommonInput.isDefined =>
      val result: Boolean =
        djVerifierComputation.verify(djCommonInput.get, protocolMsg1.get, msg2)
      log.info(ProverResult(result).toString)
      context.stop(self)
  }
}