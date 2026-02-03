package highLevelServer

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, StatusCodes, HttpEntity}
import akka.http.scaladsl.server.ContentNegotiator.Alternative.ContentType
import akka.stream.ActorMaterializer

object intro extends App {
  implicit val system: ActorSystem = ActorSystem("HighLevelIntro")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import akka.http.scaladsl.server.Directives._
  private val simplePath : Route =
    path("home") {
      complete(StatusCodes.OK)
    }

  private val chainRoute : Route =
    path("myEndpoint") {
      get {
        complete(StatusCodes.OK)
      } ~
      post {
        complete(StatusCodes.Forbidden)
      }
    } ~
    path("home") {
      get {
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            """
              |<html>
              | <body>
              |   Hello from high level server
              | </body>
              |</html>
              |""".stripMargin
          )
        )
      }
    }

  Http().bindAndHandle(chainRoute, "localhost", 8080)

}
