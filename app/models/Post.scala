package models

import play.api.libs.json._

case class Post(id: String, title: String, body: String)

object Post {
  implicit val reads: Reads[Post] = Json.reads[Post]

  implicit val writes: Writes[Post] = new Writes[Post] {
    override def writes(o: Post): JsValue = Json.obj(
      "id" -> o.id,
      "title" -> o.title,
      "body" -> o.body,
      "minBody" -> (if(o.body.nonEmpty) o.body.slice(0, 50) + "..." else "")
    )
  }

  val writesToDb: Writes[Post] = new Writes[Post] {
    override def writes(o: Post): JsValue = Json.obj(
      "id" -> o.id,
      "title" -> o.title,
      "body" -> o.body
    )
  }
}