package actors

import java.math.BigInteger

import actors.Main.system
import akka.actor.{ActorRef, ActorSystem, Props}
import org.scalacheck.Properties
import org.scalacheck.Prop._

object PropertyCheck extends Properties("product"){
  val system = ActorSystem("dj-property-test")

  property("product is correct") = forAll { (a: BigInt, b: BigInt) =>
    val carroll = system.actorOf(Props(classOf[Broker]), "carroll")
    val alice: ActorRef =
      system.actorOf(Props(classOf[Client], a.bigInteger, carroll), "alice")
    val bob: ActorRef =
      system.actorOf(Props(classOf[Client], b.bigInteger, carroll), "bob")


    (a * b) == (a * b)
  }

}
