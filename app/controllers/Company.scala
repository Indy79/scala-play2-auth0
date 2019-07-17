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

case class Company(
    id: Option[Long] = None,
    name: String,
    computers: Option[Computer] = None
)

object Company {

  implicit val reads = Json.reads[Company]
  implicit val writes = Json.writes[Company]

  import anorm._
  import anorm.SqlParser._

  val parser = long("id") ~ str("name") map {
    case id ~ name => ((Some(id), name, None))
  } map (data => (Company.apply _).tupled(data))

  def getAll()(
      implicit ec: ExecutionContext,
      db: Database
  ): Future[Iterable[Company]] = {
    Future.successful {
      db.withConnection { implicit c =>
        SQL("""
          SELECT * 
          FROM company
        """).as[Iterable[Company]](parser.*)
      }
    }
  }

  def getById(id: String)(
      implicit ec: ExecutionContext,
      db: Database
  ): Future[Option[Company]] = {
    Future.successful {
      db.withConnection { implicit c =>
        SQL(s"""
          SELECT * 
          FROM company
          WHERE id = $id
        """).as[Option[Company]](parser.singleOpt)
      }
    }
  }

}

@Singleton
class CompanyController @Inject()(
    config: Configuration,
    cache: AsyncCacheApi,
    cc: ControllerComponents,
    LoggedAction: LoggedAction
)(implicit ec: ExecutionContext, dbapi: DBApi)
    extends AbstractController(cc) {

  import _root_.controllers.Computer.{reads, writes}

  implicit val db = dbapi.database("default")

  def listAll() = LoggedAction.async {
    Company.getAll().map { list =>
      list.map(company => Json.toJson(company))
    } map { json =>
      Ok(Json.toJson(json))
    }
  }

  def getById(id: String) = LoggedAction.async {
    Company.getById(id).map {
      case Some(company) => Ok(Json.toJson(company))
      case None          => NotFound
    }
  }

  def getComputersByCompanyId(id: String) = LoggedAction.async {
    Computer.getByCompanyId(id).map { list =>
      list.map(company => Json.toJson(company))
    } map { json =>
      Ok(Json.toJson(json))
    }
  }

}
