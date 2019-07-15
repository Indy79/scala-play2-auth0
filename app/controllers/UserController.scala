package controllers

import javax.inject.Inject
import play.api.mvc._
import play.api.mvc.{Action, Controller}
import play.api.libs.json._
import play.api.cache._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.Logging
import play.shaded.ahc.io.netty.handler.codec.http.HttpResponseStatus

class LoggedAction @Inject()(cache:AsyncCacheApi, parser: BodyParsers.Default)(implicit ec: ExecutionContext)
    extends ActionBuilderImpl(parser)
    with Logging {
  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    logger.info("Calling action")
    request.session.get("id") match {
        case Some(id) => cache.get[JsValue](s"${id}profile").flatMap {
            case Some(_) => block(request)
            case None => Future.successful(Results.Unauthorized)
        }
        case None => Future.successful(Results.Unauthorized)
    }
  }
}

class UserController @Inject()(cache: AsyncCacheApi, cc: ControllerComponents, LoggedAction: LoggedAction)(
    implicit ec: ExecutionContext
) extends AbstractController(cc) {

  def index = LoggedAction.async { request =>
    request.session
      .get("id") match {
          case Some(id) => cache.get[JsValue](s"${id}profile").flatMap {
            case Some(profile) => Future.successful(Ok(views.html.user(profile)))
            case None          => Future.successful(Unauthorized)
          }
          case None => Future.successful(Unauthorized)
      }

  }
}
