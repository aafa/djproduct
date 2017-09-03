package actors

import java.math.BigInteger

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, MustMatchers}

import scala.concurrent.duration._

class BasicSpec
    extends TestKit(ActorSystem("dj-test"))
    with ImplicitSender
    with MustMatchers
    with FunSpecLike
    with BeforeAndAfterAll {
  val waitTime: FiniteDuration = 500.millis

  val carroll: ActorRef = system.actorOf(Props(classOf[Broker]), "carroll")
  val alice: ActorRef =
    system.actorOf(Props(classOf[Client], BigInteger.valueOf(4), carroll), "alice")
  val bob: ActorRef =
    system.actorOf(Props(classOf[Client], BigInteger.valueOf(5), carroll), "bob")

  it("should register") {
    carroll ! Register
    expectMsgType[Invitation](waitTime)
  }

  override def afterAll: Unit = {
    shutdown(system)
  }
}
