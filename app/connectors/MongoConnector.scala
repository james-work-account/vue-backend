package connectors

import javax.inject.Singleton
import models.Post
import play.api.libs.json.Json
import reactivemongo.api.Cursor.WithOps
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}

import scala.concurrent.Future
import reactivemongo.play.json._
import reactivemongo.bson._
import reactivemongo.api.collections.bson.BSONCollection

@Singleton
class MongoConnector {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val mongoUri = "mongodb://localhost:27017/vue-db?authMode=scram-sha1"

  private val driver = MongoDriver()
  private val parsedUri = MongoConnection.parseURI(mongoUri)
  private val connection = parsedUri.map(driver.connection(_))
  private val futureConnection = Future.fromTry(connection)

  futureConnection.map(println)

  private def vueDB: Future[DefaultDB] = futureConnection.flatMap(_.database("vue-db"))
  private def postsCollection: Future[BSONCollection] = vueDB.map(_.collection("posts"))

  implicit def postDbWriter: BSONDocumentWriter[Post] = new BSONDocumentWriter[Post] {
    override def write(t: Post): BSONDocument = Post.writesToDb.writes(t).as[BSONDocument]
  }
  implicit def postReader: BSONDocumentReader[Post] = Macros.reader[Post]

  def getAllPosts: Future[List[Post]] = {
    postsCollection.flatMap(_.find(Json.obj(), None)(JsObjectWriter, JsObjectWriter).cursor[Post]()
      .collect[List](-1, Cursor.FailOnError[List[Post]]()))
  }

  def getPost(id: String): Future[Option[Post]] = {
    val query = Json.obj("id" -> id)
    postsCollection.flatMap(_.find(query, None)(JsObjectWriter, JsObjectWriter).one[Post])
  }

  def createPost(post: Post): Future[Unit] = {
    postsCollection.flatMap(_.insert.one(post).map(_ => {}))
  }

  def updatePerson(post: Post): Future[Int] = {
    val selector = document(
      "title" -> post.title,
      "body" -> post.body
    )

    // Update the matching person
    postsCollection.flatMap(_.update.one(selector, post).map(_.n))
  }

}
