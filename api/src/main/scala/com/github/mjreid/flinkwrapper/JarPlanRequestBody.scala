package com.github.mjreid.flinkwrapper

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class JarPlanRequestBody(
                               entryClass: Option[String],
                               programArgs: Option[String],
                               programArgsList: Option[Seq[String]],
                               parallelism: Option[Int],
                               allowNonRestoredState: Option[Boolean],
                               savepointPath: Option[String]
                            )

object JarPlanRequestBody {
  implicit val reads: Reads[JarPlanRequestBody] = (
    (JsPath \ "entryClass").readNullable[String] and
      (JsPath \ "programArgs").readNullable[String] and
      (JsPath \ "programArgsList").readNullable[Seq[String]] and
      (JsPath \ "parallelism").readNullable[Int] and
      (JsPath \ "allowNonRestoredState").readNullable[Boolean] and
      (JsPath \ "savepointPath").readNullable[String]
    )(JarPlanRequestBody.apply _)

  implicit val writes: Writes[JarPlanRequestBody] = (
    (JsPath \ "entryClass").writeNullable[String] and
      (JsPath \ "programArgs").writeNullable[String] and
      (JsPath \ "programArgsList").writeNullable[Seq[String]] and
      (JsPath \ "parallelism").writeNullable[Int] and
      (JsPath \ "allowNonRestoredState").writeNullable[Boolean] and
      (JsPath \ "savepointPath").writeNullable[String]
    )(unlift(JarPlanRequestBody.unapply))
}