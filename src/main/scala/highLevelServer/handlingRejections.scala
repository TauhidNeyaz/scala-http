//package highLevelServer
//
//import akka.actor.ActorSystem
//import akka.dispatch.affinity.RejectionHandler
//import akka.http.scaladsl.model.StatusCodes
//import akka.stream.ActorMaterializer
//import akka.http.scaladsl.server.Directives._
//import akka.http.scaladsl.server.Rejection
//
//object handlingRejections extends App {
//  implicit val system: ActorSystem = ActorSystem("HandlingRejection")
//  implicit val materializer: ActorMaterializer = ActorMaterializer()
//  import system.dispatcher
//
//  val simpleRoute =
//    path("api" / "myEndpoint") {
//      get {
//        complete(StatusCodes.OK)
//      } ~
//      parameter('params) { params =>
//        complete(StatusCodes.OK)
//      }
//    }
//
//  // Rejection handlers
//  val badRequestHandler: RejectionHandler = { rejections: Seq[Rejection] =>
//    println(s"I have encountered rejections: $rejections")
//    Some(complete(StatusCodes.BadRequest))
//  }
//
//  val forbiddenHandler: RejectionHandler = { rejections: Seq[Rejection] =>
//    println(s"I have encountered rejections: $rejections")
//    Some(complete(StatusCodes.Forbidden))
//  }
//}
