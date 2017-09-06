package actors

import java.math.BigInteger

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern._
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, MustMatchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class BasicSpec
    extends TestKit(ActorSystem("dj-test"))
    with ImplicitSender
    with MustMatchers
    with FunSpecLike
    with BeforeAndAfterAll {

  val waitTime: FiniteDuration  = 300.millis
  implicit val timeout: Timeout = waitTime

  it("get all the trues") {
    val carroll: ActorRef = system.actorOf(Props(classOf[Broker]), "carroll")
    val alice: ActorRef =
      system.actorOf(Props(classOf[Client], BigInteger.valueOf(4), carroll), "alice")
    val bob: ActorRef =
      system.actorOf(Props(classOf[Client], BigInteger.valueOf(5), carroll), "bob")

    val eventualTuple: Future[(ProverResult, ProverResult)] = for {
      p2 <- (bob ? ShowProverResult).mapTo[ProverResult]
      p1 <- (alice ? ShowProverResult).mapTo[ProverResult]
    } yield (p1, p2)

    Await.result(eventualTuple, waitTime) == (ProverResult(true), ProverResult(true))
  }

  override def afterAll: Unit = {
    shutdown(system)
  }
}
