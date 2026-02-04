package highLevelServer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.stream.ActorMaterializer
import spray.json._
import akka.http.scaladsl.server.Directives.{reject, _}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

case class Person(pin : Int, name : String)

trait PersonJsonProtocol extends DefaultJsonProtocol {
  implicit val personJson: RootJsonFormat[Person] = jsonFormat2(Person)
}

object highLevelExercise extends App with PersonJsonProtocol {
  implicit val system: ActorSystem = ActorSystem("highLevelExercise")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  private var people = List (
    Person(1, "Alice"),
    Person(2, "Bob"),
    Person(3, "James"),
    Person(4, "Michael")
  )

//  println(people.toJson.prettyPrint)

  private val personServerRoute =
    pathPrefix("api" / "people") {
      get {
        (path(IntNumber) | parameter('pin.as[Int])) { pin =>
          // TODO : fetch the person with PIN
          complete(
            HttpEntity(
              ContentTypes.`application/json`,
              people.find(_.pin == pin).toJson.prettyPrint
            )
          )
        } ~
        pathEndOrSingleSlash {
          // TODO : fetch all the user
          complete(
            HttpEntity(
              ContentTypes.`application/json`,
              people.toJson.prettyPrint
            )
          )
        }
      } ~
      (post & pathEndOrSingleSlash & extractRequest & extractLog) { (request, log) =>
        // TODO : insert a person
        val entity = request.entity
        val strictEntityFuture = entity.toStrict(2 seconds)
        val personFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[Person])

        personFuture.onComplete {
          case Success(person) =>
            log.info(s"Got person $person")
            people = people :+ person
          case Failure(ex) =>
            log.info(s"Failed to get the person : $ex")
        }
        complete( personFuture
          .map(_ => StatusCodes.OK)
          .recover({
            case _ => StatusCodes.InternalServerError
          })
        )
      }
    }

  Http().bindAndHandle(personServerRoute, "localhost", 8080)
}
