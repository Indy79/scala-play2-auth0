package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import java.security.SecureRandom
import java.math.BigInteger
import java.{util => ju}
import play.api.cache.AsyncCacheApi

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(cache: AsyncCacheApi, cc: ControllerComponents)
    extends AbstractController(cc) {

  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def login = Action {
    // Generate random state parameter
    object RandomUtil {
      private val random = new SecureRandom()

      def alphanumeric(nrChars: Int = 24): String = {
        new BigInteger(nrChars * 5, random).toString(32)
      }
    }
    val state = RandomUtil.alphanumeric()

    val id = ju.UUID.randomUUID().toString()
    cache.set(id + "state", state)
    val query = String.format(
      "authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=openid profile&audience=%s&state=%s",
      "oy2zjmILV4TQ0rPwG0dtvi3oSBfF6Apb",
      "http://localhost:9000/callback",
      "http://play-scala-test",
      state
    )
    Redirect(String.format("https://%s/%s", "dev-mz20o35i.eu.auth0.com", query))
      .withSession("id" -> id)
  }

  def logout = Action {
    Redirect(
      String.format(
        "https://%s/v2/logout?client_id=%s&returnTo=http://localhost:9000",
        "dev-mz20o35i.eu.auth0.com",
        "oy2zjmILV4TQ0rPwG0dtvi3oSBfF6Apb"
      )
    ).withNewSession
  }

}
