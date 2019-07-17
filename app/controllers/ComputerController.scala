package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import java.security.SecureRandom
import java.math.BigInteger
import java.{util => ju}
import play.api.cache.AsyncCacheApi
import scala.concurrent.Future
import play.api.db.DBApi
import scala.concurrent.ExecutionContext
import play.api.db.Database
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.Json

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Computer(id: Option[Long] = None, name: String)

object Computer {

  implicit val reads = Json.reads[Computer]
  implicit val writes = Json.writes[Computer]

  import anorm._
  import anorm.SqlParser._

  val parser = long("id") ~ str("name") map {
    case id ~ name => (Some(id) -> name)
  } map (data => (Computer.apply _).tupled(data))

  def getAll()(
      implicit ec: ExecutionContext,
      db: Database
  ): Future[Iterable[Computer]] = {
    Future.successful {
      db.withConnection { implicit c =>
        SQL"SELECT * FROM computer".as[Iterable[Computer]](parser.*)
      }
    }
  }

  def getById(id: String)(
      implicit ec: ExecutionContext,
      db: Database
  ): Future[Option[Computer]] = {
    Future.successful {
      db.withConnection { implicit c =>
        SQL"SELECT * FROM computer WHERE id = $id"
          .as[Option[Computer]](parser.singleOpt)
      }
    }
  }

  def getByCompanyId(companyId: String)(
      implicit ec: ExecutionContext,
      db: Database
  ): Future[Iterable[Computer]] = {
    Future.successful {
      db.withConnection { implicit c =>
        SQL"SELECT * FROM computer WHERE company_id = $companyId"
          .as[Iterable[Computer]](parser.*)
      }
    }
  }

  def getCompanyFromComputer(id: String)(
      implicit ec: ExecutionContext,
      db: Database
  ): Future[Option[Company]] = {
    import _root_.controllers.Company.{parser => CParser}
    Future.successful {
      db.withConnection { implicit c =>
        SQL"SELECT cm.* FROM computer AS cp INNER JOIN company AS cm ON cm.id = cp.company_id WHERE cp.id = $id"
          .as[Option[Company]](CParser.singleOpt)
      }
    }
  }

}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class ComputerController @Inject()(
    config: Configuration,
    cache: AsyncCacheApi,
    cc: ControllerComponents,
    LoggedAction: LoggedAction
)(implicit ec: ExecutionContext, dbapi: DBApi)
    extends AbstractController(cc) {

  import _root_.controllers.Company.{reads, writes}

  implicit val db = dbapi.database("default")

  def listAll() = LoggedAction.async {
    Computer.getAll().map { list =>
      list.map(company => Json.toJson(company))
    } map { json =>
      Ok(Json.toJson(json))
    }
  }

  def getById(id: String) = LoggedAction.async {
    Computer.getById(id).map {
      case Some(computer) => Ok(Json.toJson(computer))
      case None           => NotFound
    }
  }

  def getCompanyFromComputer(id: String) = LoggedAction.async {
    Computer.getCompanyFromComputer(id).map {
      case Some(company) => Ok(Json.toJson(company))
      case None          => NotFound
    }
  }

}
