package lowLevelServer

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethod, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.ContentNegotiator.Alternative.ContentType
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import lowLevelServer.GuitarDB.{CreateGuitar, FindAllGuitar, GuitarCreated}
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

case class Guitar(make : String, model : String)

object GuitarDB {
  case class CreateGuitar(guitar : Guitar)
  case class GuitarCreated(id : Int)
  case class FindGuitar(id : Int)
  object FindAllGuitar
}

class GuitarDB extends Actor with ActorLogging {
  import GuitarDB._

  private var guitars: Map[Int, Guitar] = Map()
  private var currentGuitarId : Int = 0

  override def receive: Receive = onMessage(guitars)


  def onMessage(guitars: Map[Int, Guitar]): Receive = {
    case FindAllGuitar =>
      log.info("Finding all guitars ...")
      sender() ! guitars.values.toList

    case FindGuitar(id) =>
      log.info(s"Finding guitar by $id")
      sender() ! guitars.get(id)

    case CreateGuitar(guitar) =>
      log.info(s"Adding guitar $guitar with id $currentGuitarId")
      context.become(onMessage(guitars + (currentGuitarId -> guitar)))
      sender() ! GuitarCreated(currentGuitarId)
      currentGuitarId += 1
  }
}

trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {
  implicit val guitarFormat: RootJsonFormat[Guitar] = jsonFormat2(Guitar)
}

object lowLevelRest extends App with GuitarStoreJsonProtocol {
  implicit val system: ActorSystem = ActorSystem("GuitarStore")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  // JSON -> marshalling
  private val simpleGuitar = Guitar("Finder", "Stratocaster")
  println(simpleGuitar.toJson.prettyPrint)

  // un-marshalling
  private val simpleGuitarJsonString =
    """
      |{
      |  "make": "Finder",
      |  "model": "Stratocaster"
      |}
      |""".stripMargin
  println(simpleGuitarJsonString.parseJson.convertTo[Guitar])

  val guitarDB = system.actorOf(Props[GuitarDB], "LowLevelGuitarDB")
  val guitarList = List(
    Guitar("Finder", "Stratocaster"),
    Guitar("Gibson", "Les Paul"),
    Guitar("Martin", "LX1")
  )
  guitarList.foreach { guitar =>
    guitarDB ! CreateGuitar(guitar)
  }

  /**
   * Server Code
   */

  implicit val defaultTimeout: Timeout = Timeout(2 seconds)

  val requestHandler : HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/api/guitar"), _, _, _) =>
      val guitarsFuture : Future[List[Guitar]] = (guitarDB ? FindAllGuitar).mapTo[List[Guitar]]
      guitarsFuture.map{ guitars =>
        HttpResponse(
          entity = HttpEntity(
            ContentTypes.`application/json`,
            guitars.toJson.prettyPrint
          )
        )
      }

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"), _, entity, _) =>
      val strictEntityFuture = entity.toStrict(3 seconds)
      strictEntityFuture.flatMap { strictEntity =>
        val guitarJsonString = strictEntity.data.utf8String
        val guitar = guitarJsonString.parseJson.convertTo[Guitar]

        val guitarCreatedFuture : Future[GuitarCreated] = (guitarDB ? CreateGuitar(guitar)).mapTo[GuitarCreated]
        guitarCreatedFuture.map { _ =>
          HttpResponse(StatusCodes.OK)
        }

      }

    case request: HttpRequest =>
      request.discardEntityBytes()
      Future{
        HttpResponse(status = StatusCodes.NotFound)
      }
  }

  Http().bindAndHandleAsync(requestHandler, "localhost", 8080)

}
