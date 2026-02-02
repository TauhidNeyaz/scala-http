package lowLevelServer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object lowLevelAPI extends App {

  implicit val system: ActorSystem = ActorSystem("LowLevelServerAPI")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import system.dispatcher

  private val serverSource = Http().bind("localhost", 8000)
  private val connectionSink = Sink.foreach[IncomingConnection] { connection =>
    println(s"Accepted incoming connection from: ${connection.remoteAddress}")
  }

  private val serverBindingFuture = serverSource.to(connectionSink).run()
  serverBindingFuture.onComplete {
    case Success(_) =>
      println("Server binding successful.")
    case Failure(ex) => println(s"Server binding failed: $ex")
  }

}