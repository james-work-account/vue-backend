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

  val logger: slf4j.Logger = Logger.logger

  def getAllPosts(): Action[AnyContent] = Action.async {
    mongoConnector.getAllPosts.map {
      posts =>
        Ok(Json.toJson(posts))
    }.recover {
      case ex =>
        logger.error(ex.getMessage, ex)
        InternalServerError(ex.getMessage)
    }
  }

  def getSinglePost(id: String): Action[AnyContent] = Action.async {
    mongoConnector.getPost(id).map {
      post =>
        if (post.isDefined) {
          Ok(Json.toJson(post))
        } else {
          logger.info(s"No message found with ID [$id]")
          NoContent
        }
    }.recover {
      case ex =>
        logger.error(ex.getMessage, ex)
        InternalServerError(ex.getMessage)
    }
  }

  def upsertPost(): Action[AnyContent] = Action.async {
    implicit request =>
      val post = request.body.asJson.getOrElse(Json.obj()).as[Post]
      val errors = validatePost(post)
      if (errors.isEmpty) {
        mongoConnector.upsertPost(post).map {
          _ =>
            logger.info(s"[upsertPost] successful")
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

  def deletePost(): Action[AnyContent] = Action.async {
    implicit request =>
      val post = request.body.asJson.getOrElse(Json.obj()).as[Post]
      mongoConnector.deletePost(post).map {
        _ =>
          logger.info(s"[deletePost] successful or no post to be deleted")
          NoContent
      }.recover {
        case ex =>
          logger.error(ex.getMessage, ex)
          InternalServerError(ex.getMessage)
      }
  }

  private def validatePost(post: Post): List[String] =
    List((post.id.nonEmpty, "id"), (post.title.nonEmpty, "title"), (post.body.nonEmpty, "body"))
      .filterNot(_._1)
      .map(_._2)

}