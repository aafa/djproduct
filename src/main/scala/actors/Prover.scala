package actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill}
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.damgardJurikProduct.{SigmaDJProductProverComputation, SigmaDJProductProverInput}
import edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.utility.SigmaProtocolMsg

class Prover(verifier: ActorRef, proverInput: SigmaDJProductProverInput, broker: ActorRef)
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
      broker ! PoisonPill
      self ! PoisonPill
  }
}
