package connectors

import javax.inject.Singleton
import models.Post
import org.slf4j
import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document, _}
import reactivemongo.play.json._

import scala.concurrent.Future

@Singleton
class MongoConnector {

  val logger: slf4j.Logger = Logger.logger

  import scala.concurrent.ExecutionContext.Implicits.global

  private val mongoUri = "mongodb://localhost:27017/vue-db?authMode=scram-sha1"

  private val driver = MongoDriver()
  private val parsedUri = MongoConnection.parseURI(mongoUri)
  private val connection = parsedUri.map(driver.connection)
  private val futureConnection = Future.fromTry(connection)

  private def vueDB: Future[DefaultDB] = futureConnection.flatMap(_.database("vue-db"))

  private def postsCollection: Future[BSONCollection] = vueDB.map(_.collection("posts"))

  implicit def postDbWriter: BSONDocumentWriter[Post] = new BSONDocumentWriter[Post] {
    override def write(t: Post): BSONDocument = Post.writesToDb.writes(t).as[BSONDocument]
  }

  implicit def postReader: BSONDocumentReader[Post] = Macros.reader[Post]

  def getAllPosts: Future[List[Post]] = {
    postsCollection.flatMap(_.find(Json.obj(), None)(JsObjectWriter, JsObjectWriter).sort(BSONDocument("_id" -> -1)).cursor[Post]()
      .collect[List](-1, Cursor.FailOnError[List[Post]]()))
  }

  def getPost(id: String): Future[Option[Post]] = {
    val query = Json.obj("id" -> id)
    postsCollection.flatMap(_.find(query, None)(JsObjectWriter, JsObjectWriter).one[Post])
  }

  def upsertPost(post: Post): Future[Int] = {
    val selector = document("id" -> post.id)
    postsCollection.flatMap(_.update.one(selector, post, upsert = true).map(_.n))
  }

  def deletePost(post: Post): Future[Int] = {
    val selector = document("id" -> post.id, "title" -> post.title, "body" -> post.body)
    postsCollection.flatMap(_.delete.one(selector).map(_.n))
  }

}
