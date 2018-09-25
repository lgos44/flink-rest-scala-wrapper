package com.github.mjreid.flinkwrapper

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class CancelJobAccepted(
  requestId: Long,
  location: String
)

case class AsynchronousOperationResult(
                                        status: QueueStatus,
                                        operation: Option[Operation]
)


object StatusTypes {

  sealed trait StatusType
  case object InProgress extends StatusType
  case object Success extends StatusType
  case object Failed extends StatusType
  case object Completed extends StatusType

  implicit val reads: Reads[StatusType] = Reads {
    case JsString(statusString) =>
      statusString.toUpperCase match {
        case "IN_PROGRESS" => JsSuccess(InProgress)
        case "SUCCESS" => JsSuccess(Success)
        case "FAILED" => JsSuccess(Failed)
        case "COMPLETED" => JsSuccess(Completed)
        case _ => JsError("Not a valid cancellation status")
      }
    case _ => JsError("Not a valid cancellation status")
  }
}

object CancelJobAccepted {
  implicit val reads: Reads[CancelJobAccepted] = (
    (JsPath \ "request-id").read[Long] and
      (JsPath \ "location").read[String]
  )(CancelJobAccepted.apply _)
}

object AsynchronousOperationResult {
 implicit val reads: Reads[AsynchronousOperationResult] = (
   (JsPath \ "status").read[QueueStatus] and
     (JsPath \ "operation").readNullable[Operation]
 )(AsynchronousOperationResult.apply _)
}


case class QueueStatus(id: StatusTypes.StatusType)

object QueueStatus {
  implicit val reads: Reads[QueueStatus] = (JsPath \ "id").read[StatusTypes.StatusType].map(QueueStatus(_))
}

case class Operation(location: Option[String])

object Operation {
  implicit val reads: Reads[Operation] = (JsPath \ "location").readNullable[String].map(Operation(_))
}
