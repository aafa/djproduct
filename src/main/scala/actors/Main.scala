package actors

import java.math.BigInteger

import akka.actor._

object Main extends App{
  val system = ActorSystem("dj")

  val carroll = system.actorOf(Props(classOf[Broker]), "carroll")

  val alice = system.actorOf(Props(classOf[Client], BigInteger.valueOf(4), carroll), "alice")
  val bob = system.actorOf(Props(classOf[Client], BigInteger.valueOf(5), carroll), "bob")


}
