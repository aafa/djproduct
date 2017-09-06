package actors

import java.math.BigInteger
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor._
import akka.util.Timeout
import akka.pattern._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.FiniteDuration

object Main extends App {
  val waitTime: FiniteDuration  = 300.millis
  implicit val timeout: Timeout = waitTime

  val system = ActorSystem("dj")

  val carroll = system.actorOf(Props(classOf[Broker]), "carroll")
  val alice   = system.actorOf(Props(classOf[Client], BigInteger.valueOf(4), carroll), "alice")
  val bob     = system.actorOf(Props(classOf[Client], BigInteger.valueOf(5), carroll), "bob")

  val eventualTuple: Future[(ProverResult, ProverResult)] = for {
    p2 <- (bob ? ShowProverResult).mapTo[ProverResult]
    p1 <- (alice ? ShowProverResult).mapTo[ProverResult]
    _  <- system.terminate()
  } yield (p1, p2)

  println(Await.result(eventualTuple, waitTime))
  Await.result(system.whenTerminated, waitTime)
}
