package actors

import java.security.KeyPair

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.damgardJurikProduct.SigmaDJProductProverInput
import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.{
  DJKeyGenParameterSpec,
  DamgardJurikEnc,
  ScDamgardJurikEnc
}
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.{DamgardJurikPrivateKey, DamgardJurikPublicKey}
import edu.biu.scapi.midLayer.ciphertext.BigIntegerCiphertext
import edu.biu.scapi.midLayer.plaintext.BigIntegerPlainText

class Broker extends Actor with ActorLogging {
  val damgardJurikEnc: DamgardJurikEnc = new ScDamgardJurikEnc()
  val dJKeyGenParameterSpec            = new DJKeyGenParameterSpec(256, 40)
  val keyPair: KeyPair                 = damgardJurikEnc.generateKey(dJKeyGenParameterSpec)

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
      _a  <- a
      _b  <- b
    } yield new SigmaDJProductProverInput(publicKey, _cA, _cB, _cC, privateKey, _a, _b)

  override def receive: PartialFunction[Any, Unit] = {
    case Register if actor1.isEmpty =>
      actor1 = Some(sender())
      sender() ! Invitation(publicKey)

    case Register if actor2.isEmpty =>
      actor2 = Some(sender())
      sender() ! Invitation(publicKey)

    // D-J operations

    case EncryptedNumber(ciphertext) if cA.isEmpty && actor1.contains(sender()) =>
      cA = Some(ciphertext)
      self ! Multiply

    case EncryptedNumber(ciphertext) if cB.isEmpty && actor2.contains(sender()) =>
      cB = Some(ciphertext)
      self ! Multiply

    case Multiply if cA.isDefined && cB.isDefined && cC.isEmpty =>
      for {
        _cA  <- cA
        _cB  <- cB
        act1 <- actor1
        act2 <- actor2
      } {
        act1 ! EncryptedNumbers(_cA, _cB)
        act2 ! EncryptedNumbers(_cA, _cB)

        val _a = damgardJurikEnc.decrypt(_cA).asInstanceOf[BigIntegerPlainText]
        val _b = damgardJurikEnc.decrypt(_cB).asInstanceOf[BigIntegerPlainText]

        log.info(s"Proceeding with Multiply ${_a}*${_b}")

        val product: BigIntegerPlainText = new BigIntegerPlainText(_a.getX multiply _b.getX)
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
