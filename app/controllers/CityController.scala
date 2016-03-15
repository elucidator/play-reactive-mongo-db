package controllers

import javax.inject._

import models.City
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo._
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import utils.Errors

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CityController @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext) extends Controller with MongoController with ReactiveMongoComponents {

  def cities: JSONCollection = db.collection[JSONCollection]("city")

  def create(name: String, population: Int) = Action.async {
    cities.insert(City(name, population)).map(lastError =>
      Ok("Mongo LastError: %s".format(lastError)))
  }

  def createFromJson = Action.async(parse.json) { request =>
    City.formatter.reads(request.body) match {
      case JsSuccess(city, _) =>
        cities.insert(city).map { lastError =>
          Logger.debug(s"Successfully inserted with LastError: $lastError")
          Created
        }
      case JsError(errors) => Future.successful(BadRequest("Could not build a city from the json provided. " + Errors.show(errors)))
    }
  }

  def findByName(name: String) = Action.async {
    // let's do our query
    val cursor: Cursor[City] = cities.
      // find all people with name `name`
      find(Json.obj("name" -> name)).
      // perform the query and get a cursor of JsObject
      cursor[City](ReadPreference.primary)

    // gather all the JsObjects in a list
    val futurePersonsList: Future[List[City]] = cursor.collect[List]()

    // everything's ok! Let's reply with a JsValue
    futurePersonsList.map { cities =>
      Ok(Json.toJson(cities))
    }
  }
}


