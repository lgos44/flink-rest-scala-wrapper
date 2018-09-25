package com.github.mjreid.flinkwrapper

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class JarRunRequestBody(
                              entryClass: Option[String],
                              programArgs: Option[String],
                              parallelism: Option[Int],
                              allowNonRestoredState: Option[Boolean],
                              savepointPath: Option[String]
                            )

object JarRunRequestBody {
  implicit val reads: Reads[JarRunRequestBody] = (
    (JsPath \ "entryClass").readNullable[String] and
      (JsPath \ "programArgs").readNullable[String] and
      (JsPath \ "parallelism").readNullable[Int] and
      (JsPath \ "allowNonRestoredState").readNullable[Boolean] and
      (JsPath \ "savepointPath").readNullable[String]
    )(JarRunRequestBody.apply _)

  implicit val writes: Writes[JarRunRequestBody] = (
    (JsPath \ "entryClass").writeNullable[String] and
      (JsPath \ "programArgs").writeNullable[String] and
      (JsPath \ "parallelism").writeNullable[Int] and
      (JsPath \ "allowNonRestoredState").writeNullable[Boolean] and
      (JsPath \ "savepointPath").writeNullable[String]
  )(unlift(JarRunRequestBody.unapply))
}