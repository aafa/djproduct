package actors

import java.math.BigInteger
import java.security.KeyPair

import akka.actor._
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.damgardJurikProduct._
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.utility.SigmaProtocolMsg
import edu.biu.scapi.midLayer.asymmetricCrypto.encryption._
import edu.biu.scapi.midLayer.asymmetricCrypto.keys._
import edu.biu.scapi.midLayer.ciphertext.BigIntegerCiphertext
import edu.biu.scapi.midLayer.plaintext.BigIntegerPlainText

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

class Broker extends Actor with ActorLogging {
  val damgardJurikEnc: DamgardJurikEnc = new ScDamgardJurikEnc()
  val dJKeyGenParameterSpec = new DJKeyGenParameterSpec(256, 40)
  val keyPair: KeyPair = damgardJurikEnc.generateKey(dJKeyGenParameterSpec)
  val (publicKey, privateKey): (DamgardJurikPublicKey, DamgardJurikPrivateKey) =
    (keyPair.getPublic.asInstanceOf[DamgardJurikPublicKey],
     keyPair.getPrivate.asInstanceOf[DamgardJurikPrivateKey])
  damgardJurikEnc.setKey(publicKey, privateKey)

  var actor1: Option[ActorRef] = None
  var actor2: Option[ActorRef] = None

  var cA: Option[BigIntegerCiphertext] = None
  var cB: Option[BigIntegerCiphertext] = None
  var cC: Option[BigIntegerCiphertext] = None

  var a: Option[BigIntegerPlainText] = None
  var b: Option[BigIntegerPlainText] = None

  def proverInput: Option[SigmaDJProductProverInput] =
    for {
      _cA <- cA
      _cB <- cB
      _cC <- cC
      _a <- a
      _b <- b
    } yield
      new SigmaDJProductProverInput(publicKey,
                                    _cA,
                                    _cB,
                                    _cC,
                                    privateKey,
                                    _a,
                                    _b)

  override def receive: PartialFunction[Any, Unit] = {
    case Register if actor1.isEmpty =>
      actor1 = Some(sender())
      sender() ! Invitation(publicKey)

    case Register if actor2.isEmpty =>
      actor2 = Some(sender())
      sender() ! Invitation(publicKey)

    // D-J operations

    case EncryptedNumber(ciphertext)
        if cA.isEmpty && actor1.contains(sender()) =>
      cA = Some(ciphertext)
      self ! Multiply

    case EncryptedNumber(ciphertext)
        if cB.isEmpty && actor2.contains(sender()) =>
      cB = Some(ciphertext)
      self ! Multiply

    case Multiply if cA.isDefined && cB.isDefined && cC.isEmpty =>
      for {
        _cA <- cA
        _cB <- cB
        act1 <- actor1
        act2 <- actor2
      } {
        act1 ! EncryptedNumbers(_cA, _cB)
        act2 ! EncryptedNumbers(_cA, _cB)

        val _a = damgardJurikEnc.decrypt(_cA).asInstanceOf[BigIntegerPlainText]
        val _b = damgardJurikEnc.decrypt(_cB).asInstanceOf[BigIntegerPlainText]

        log.info(s"Proceeding with Multiply ${_a}*${_b}")

        val product: BigIntegerPlainText = new BigIntegerPlainText(
          _a.getX multiply _b.getX)
        val _cC: BigIntegerCiphertext =
          damgardJurikEnc.encrypt(product).asInstanceOf[BigIntegerCiphertext]

        log.info(s"product $product")
        a = Some(_a)
        b = Some(_b)
        cC = Some(_cC)

        act1 ! EncryptedResult(_cC)
        act2 ! EncryptedResult(_cC)
      }

    case RequestProve if proverInput.isDefined =>
      log.info(s"Proceeding with Prove")
      context.actorOf(Props(classOf[Prover], sender(), proverInput.get)) ! ProvideProve

  }
}

class Prover(verifier: ActorRef, proverInput: SigmaDJProductProverInput)
    extends Actor
    with ActorLogging {

  val proverComputation: SigmaDJProductProverComputation =
    new SigmaDJProductProverComputation()

  override def receive: PartialFunction[Any, Unit] = {

    case ProvideProve =>
      val msg1: SigmaProtocolMsg =
        proverComputation.computeFirstMsg(proverInput)
      verifier ! Message1(msg1)

    case Challenge(challenge) =>
      val msg2 = proverComputation.computeSecondMsg(challenge)
      log.info(s"Resolving challenge $msg2")
      verifier ! Message2(msg2)
  }
}
