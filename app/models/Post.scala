package models

import play.api.libs.json._

case class Post(id: String, time: String, title: String, body: String)

object Post {
  implicit val reads: Reads[Post] = Json.reads[Post]

  implicit val writes: Writes[Post] = new Writes[Post] {
    override def writes(o: Post): JsValue = {
      val minBody: String = if(o.body.length < 50) o.body else o.body.slice(0, 50) + "..."
      Json.obj(
        "id" -> o.id,
        "time" -> o.time,
        "title" -> o.title,
        "body" -> o.body,
        "minBody" -> minBody
      )
    }
  }

  val writesToDb: Writes[Post] = new Writes[Post] {
    override def writes(o: Post): JsValue = Json.obj(
      "id" -> o.id,
      "time" -> o.time,
      "title" -> o.title,
      "body" -> o.body
    )
  }
}