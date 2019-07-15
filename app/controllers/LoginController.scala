package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.ws.WSClient
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import play.api.libs.json.JsValue
import play.api.cache.AsyncCacheApi

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class LoginController @Inject()(cache: AsyncCacheApi, cc: ControllerComponents, ws: WSClient)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def callback(codeOpt: Option[String] = None, stateOpt: Option[String] = None) = Action.async { request =>
    request.session.get("id") match {
      case Some(sessionId) => cache.get[String](sessionId + "state").flatMap { opt =>
        if (stateOpt == opt) {
          (for {
            code <- codeOpt
          } yield {
            getToken(code, sessionId).flatMap { case (idToken, accessToken) =>
              getUser(accessToken).map { user =>
                cache.set(s"${sessionId}profile", user)
                Redirect("/me")
                  .withSession(
                    "id" -> sessionId,
                    "idToken" -> idToken,
                    "accessToken" -> accessToken
                  )
              }
            }.recover {
              case ex: IllegalStateException => Unauthorized(ex.getMessage)
            }
          }).getOrElse(Future.successful(BadRequest("No parameters supplied")))
        } else {
          Future.successful(BadRequest("Invalid state parameter"))
        }
      }.recover {
        case e: Error => InternalServerError(Json.obj("error" -> e.getMessage()))
      }
      case None => Future.successful(Unauthorized(Json.obj("error" -> "cannot find session")))
    }
    
    
  }

  def getToken(code: String, sessionId: String): Future[(String, String)] = {
    val tokenResponse = ws
      .url("https://dev-mz20o35i.eu.auth0.com/oauth/token")
      .withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON)
      .post(
        Json.obj(
          "client_id" -> "oy2zjmILV4TQ0rPwG0dtvi3oSBfF6Apb",
          "client_secret" -> "jiuZAbsv0NZND-EBWYxLBzLsQ-dCTMz5YEYteaouXhkT6fuKMWUKHxcg1JjeHine",
          "redirect_uri" -> "http://localhost:9000/callback",
          "code" -> code,
          "grant_type" -> "authorization_code",
          "audience" -> "http://play-scala-test"
        )
      )
    tokenResponse.flatMap { response =>
      println(response.json.toString())
      (for {
        idToken <- (response.json \ "id_token").asOpt[String]
        accessToken <- (response.json \ "access_token").asOpt[String]
      } yield {
        cache.set(sessionId + "id_token", idToken)
        cache.set(sessionId + "access_token", accessToken)
        Future.successful((idToken, accessToken))
      }).getOrElse(
        Future.failed[(String, String)](
          new IllegalStateException("Tokens not sent")
        )
      )
    }

  }

  def getUser(accessToken: String): Future[JsValue] = {
    val userResponse = ws.url("https://dev-mz20o35i.eu.auth0.com/userinfo")
      .withQueryStringParameters("access_token" -> accessToken)
      .get()

    userResponse.flatMap(response => Future.successful(response.json))
  }

}
