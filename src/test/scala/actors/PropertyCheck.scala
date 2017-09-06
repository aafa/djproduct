package actors

import java.math.BigInteger

import akka.actor.{ActorRef, _}
import akka.pattern._
import akka.util.Timeout
import org.scalacheck.Prop._
import org.scalacheck.Properties

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Await, Future}
import scala.util.Random

object PropertyCheck extends Properties("product")  {
  val system = ActorSystem("dj-property-test")
  val waitTime: FiniteDuration = 100.millis
  implicit val timeout: Timeout = waitTime

  property("is calculated") = forAll { (a: Int, b: Int) =>
    val take = Random.nextLong()
    val carroll = system.actorOf(Props(classOf[Broker]), s"carroll$a*$b-$take")
    val alice: ActorRef =
      system.actorOf(Props(classOf[Client], BigInteger.valueOf(Math.abs(a)), carroll), s"alice$a-$take")
    val bob: ActorRef =
      system.actorOf(Props(classOf[Client], BigInteger.valueOf(Math.abs(b)), carroll), s"bob$b-$take")

    val eventualTuple: Future[(ProverResult, ProverResult)] = for {
      p1 <- (alice ? ShowProverResult).mapTo[ProverResult]
      p2 <- (bob ? ShowProverResult).mapTo[ProverResult]
    } yield (p1, p2)

    Await.result(eventualTuple, waitTime) == (ProverResult(true), ProverResult(true))
  }

}
