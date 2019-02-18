package controllers

import connectors.MongoConnector
import javax.inject.Inject
import models.Post
import org.slf4j
import play.api._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Application @Inject()(mongoConnector: MongoConnector) extends Controller {

  val logger = Logger(this.getClass)

  def getAllPosts(): Action[AnyContent] = Action.async {
    mongoConnector.getAllPosts.map {
      posts =>
        Ok(Json.toJson(posts))
    }
  }

  def getSinglePost(id: String): Action[AnyContent] = Action.async {
    mongoConnector.getPost(id).map {
      post =>
        val jsonPost = if (post.isDefined) Json.toJson(post) else {
          logger.info(s"No message found with ID [$id]")
          Json.obj()
        }
        Ok(jsonPost)
    }
  }

  def createPost(): Action[AnyContent] = Action.async {
    implicit request =>
      val post = request.body.asJson.getOrElse(Json.obj()).as[Post]
      val errors = validatePost(post)
      if (errors.isEmpty) {
        mongoConnector.createPost(post).map {
          _ =>
            logger.info(s"Created Post with ID [${post.id}]")
            NoContent
        }.recover {
          case ex =>
            logger.error(ex.getMessage, ex)
            InternalServerError(ex.getMessage)
        }
      } else {
        errors match {
          case head :: Nil => logger.error(s"Could not create post - field [$head] is required")
          case list: List[String] => logger.error(s"Could not create post - fields [${list.mkString(", ")}] are required")
        }
        Future.successful(BadRequest(s"Required: ${errors.mkString(", ")}"))
      }
  }

  private def validatePost(post: Post): List[String] =
    List((post.id.nonEmpty, "id"), (post.title.nonEmpty, "title"), (post.body.nonEmpty, "body"))
      .filterNot(_._1)
      .map(_._2)

}