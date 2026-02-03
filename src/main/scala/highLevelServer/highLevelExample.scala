package highLevelServer

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.ContentNegotiator.Alternative.ContentType
import akka.stream.ActorMaterializer
import lowLevelServer.GuitarDB.{CreateGuitar, FindAllGuitar}
import lowLevelServer.lowLevelRest.system
import lowLevelServer.{Guitar, GuitarDB, GuitarStoreJsonProtocol}
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import spray.json
import spray.json._
import GuitarDB._
import akka.http.scaladsl.Http

object highLevelExample extends App with GuitarStoreJsonProtocol{
  implicit val system: ActorSystem = ActorSystem("highLevelExample")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  /*
   GET /api/guitar fetches ALL the guitars in the store
   GET /api/guitar?id=x fetches the guitar with id X
   GET /api/guitar/X fetches guitar with id X
   GET /api/guitar/inventory?inStock=true
  */

  val guitarDB = system.actorOf(Props[GuitarDB], "LowLevelGuitarDB")
  val guitarList = List(
    Guitar("Finder", "Stratocaster"),
    Guitar("Gibson", "Les Paul"),
    Guitar("Martin", "LX1")
  )
  guitarList.foreach { guitar =>
    guitarDB ! CreateGuitar(guitar)
  }

  implicit val timeout: Timeout = Timeout(3 seconds)
  val guitarServerRoute =

    path("api" / "guitar") {
      parameter('id.as[Int]) { guitarId =>
        get {
          val guitarFuture : Future[Option[Guitar]] = (guitarDB ? FindGuitar(guitarId)).mapTo[Option[Guitar]]
          val entityFuture = guitarFuture.map { guitarOption =>
            HttpEntity (
              ContentTypes.`application/json`,
              guitarOption.toJson.prettyPrint
            )
          }
          complete(entityFuture)
        }
      } ~
      get {
        val guitarFuture : Future[List[Guitar]] = (guitarDB ? FindAllGuitar).mapTo[List[Guitar]]
        val entityFuture = guitarFuture.map { guitar =>
          HttpEntity (
            ContentTypes.`application/json`,
            guitar.toJson.prettyPrint
          )
        }
        complete(entityFuture)
      }
    } ~
    path("api" / "guitar" / IntNumber) { guitarId =>
      get {
        val guitarFuture: Future[Option[Guitar]] = (guitarDB ? FindGuitar(guitarId)).mapTo[Option[Guitar]]
        val entityFuture = guitarFuture.map { guitarOption =>
          HttpEntity(
            ContentTypes.`application/json`,
            guitarOption.toJson.prettyPrint
          )
        }
        complete(entityFuture)
      }
    }

  def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

  val simplifiedGuitarServerRoute =
    (pathPrefix("api" / "guitar") & get) {
      path("inventory") {
        parameter('inStock.as[Boolean]) { inStock =>
          complete(
            (guitarDb ? FindGuitarsInStock(inStock))
              .mapTo[List[Guitar]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      } ~
        (path(IntNumber) | parameter('id.as[Int])) { guitarId =>
          complete(
            (guitarDb ? FindGuitar(guitarId))
              .mapTo[Option[Guitar]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        } ~
        pathEndOrSingleSlash {
          complete(
            (guitarDb ? FindAllGuitars)
              .mapTo[List[Guitar]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
    }

  Http().bindAndHandle(simplifiedGuitarServerRoute, "localhost", 8080)

  Http().bindAndHandle(guitarServerRoute, "localhost", 8080)
}

