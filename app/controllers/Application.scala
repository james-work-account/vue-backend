package controllers

import connectors.MongoConnector
import javax.inject.Inject
import models.Post
import org.slf4j
import play.api._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject()(mongoConnector: MongoConnector) extends Controller {

  val logger: slf4j.Logger = Logger.logger

  def getAllPosts(): Action[AnyContent] = Action.async {
    mongoConnector.getAllPosts.map {
      posts =>
        Ok(Json.toJson(posts))
    }
  }

  def getSinglePost(id: String): Action[AnyContent] = Action.async {
    mongoConnector.getPost(id).map {
      post =>
        val jsonPost = if(post.isDefined) Json.toJson(post) else {
          logger.info(s"No message found with ID [$id]")
          Json.obj()
        }
        Ok(jsonPost)
    }
  }

  def createPost(): Action[AnyContent] = Action.async {
    implicit request =>
      val post = request.body.asJson.getOrElse(Json.obj()).as[Post]
      mongoConnector.createPost(post).map {
        _ =>
          Logger.logger.info(s"Created Post with ID [${post.id}]")
          Ok(Json.toJson(post))
      }
  }

}