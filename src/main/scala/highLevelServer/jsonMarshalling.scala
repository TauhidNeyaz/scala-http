package highLevelServer

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps

case class Player(nickname : String, characterClass : String, level : Int)

object GameAreaMap {
  case object GetAllPlayer
  case class GetPlayer(nickname : String)
  case class GetPlayerByClass(characterClass : String)
  case class AddPlayer(player : Player)
  case class RemovePlayer(player : Player)
  private case object OperationSuccess
}

class GameAreaMap extends Actor with ActorLogging {
  var players: Map[String, Player] = Map[String, Player]()
  import GameAreaMap._
  override def receive: Receive = {
    case GetAllPlayer =>
      log.info("Getting all players ...")
      sender() ! players.values.toList

    case GetPlayer(nickname) =>
      log.info(s"Getting player with nickname $nickname")
      sender() ! players.get(nickname)

    case GetPlayerByClass(characterClass) =>
      log.info(s"Getting player with characterClass $characterClass")
      sender() ! players.values.toList.filter(_.characterClass == characterClass)

    case AddPlayer(player) =>
      log.info(s"Adding a new player $player")
      players = players + (player.nickname -> player)
      sender() ! OperationSuccess

    case RemovePlayer(player) =>
      log.info(s"Removing a player $player")
      players = players - player.nickname
      sender() ! OperationSuccess
  }
}

trait PlayerJsonProtocol extends DefaultJsonProtocol {
  implicit val playerFormatter: RootJsonFormat[Player] = jsonFormat3(Player)
}

object jsonMarshalling extends App with PlayerJsonProtocol with SprayJsonSupport {
  implicit val system: ActorSystem = ActorSystem("JsonMarshalling")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher
  import GameAreaMap._

  var players = List(
    Player("liQUid", "Legend", 100),
    Player("Psycho", "Master", 90),
    Player("Dr.Damage", "Expert", 70),
    Player("Nothing", "Newbie", 30)
  )

  val gameAdmin = system.actorOf(Props[GameAreaMap], "GameAdmin")
  players.foreach { player =>
    gameAdmin ! AddPlayer(player)
  }

  /*
   - GET /api/player, returns all the players in the map, as JSON
   - GET /api/player/(nickname), returns the player with the given nickname (as JSON)
   - GET /api/player?nickname=X, does the same
   - GET /api/player/class/(charClass), returns all the players with the given character class
   - POST /api/player with JSON payload, adds the player to the map
   - (Exercise) DELETE /api/player with JSON payload, removes the player from the map
  */

  implicit val timeout: Timeout = Timeout(2 seconds)
  private val gameAreaMapRoutes =
    pathPrefix("api" / "player") {
      get {
        path("class" / Segment) { charClass =>
          // TODO 1. get the player by class
          val playerByClassFuture = (gameAdmin ? GetPlayerByClass(charClass)).mapTo[List[Player]]
          complete(playerByClassFuture)
        } ~
          (path(Segment) | parameter('nickname)) { nickname =>
          // TODO 2. get the player with nickname
          val playerByNickname = (gameAdmin ? GetPlayer(nickname)).mapTo[Option[Player]]
            complete(playerByNickname)
        } ~
        pathEndOrSingleSlash {
          // TODO 3. get all the players
          complete((gameAdmin ? GetAllPlayer).mapTo[List[Player]])
        }
      } ~
      post {
        // TODO 4. insert the player
        entity(as[Player]) { player =>
          complete((gameAdmin ? AddPlayer(player)).map(_ => StatusCodes.OK))
        }
      } ~
      delete {
        entity(as[Player]) { player =>
          complete((gameAdmin ? RemovePlayer(player)).map(_ => StatusCodes.OK))
        }
      }
    }

  Http().bindAndHandle(gameAreaMapRoutes, "localhost", 8080)
}
