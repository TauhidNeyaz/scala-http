package highLevelServer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object highLevelExercise extends App {
  implicit val system = ActorSystem("highLevelExercise")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher


}
