package com.github.mjreid.flinkwrapper

import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.core.JsonParseException
import play.api.libs.json._
import play.api.libs.ws.StandaloneWSRequest
import play.api.libs.ws.ahc.{StandaloneAhcWSClient, StandaloneAhcWSResponse}
import play.shaded.ahc.org.asynchttpclient.request.body.multipart.FilePart
import play.shaded.ahc.org.asynchttpclient.{AsyncCompletionHandler, AsyncHttpClient, Response => AHCResponse}

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * FlinkRestClient is the primary contact point for the Flink REST server.
  *
  * While this class can be invoked manually with a custom client configuration, it's recommended to use one of the
  * factory methods on the FlinkRestClient object to create a client instance.
  */
class FlinkRestClient(flinkRestClientConfig: FlinkRestClientConfig) extends AutoCloseable {

  implicit private val system = flinkRestClientConfig.maybeActorSystem.getOrElse(ActorSystem())
  implicit private val materializer = ActorMaterializer()
  private val wsClient = StandaloneAhcWSClient()
  // Append a trailing slash if not present
  private val url = if (flinkRestClientConfig.url.endsWith("/")) flinkRestClientConfig.url else flinkRestClientConfig.url + "/"
  private val responseHandler = flinkRestClientConfig.responseHandler

  /**
    * getConfig returns the system level configuration level of the Flink server.
    */
  def getConfig()(implicit ec: ExecutionContext): Future[DashboardConfiguration] = {
    wsClient.url(url + "config").get().map(responseHandler.handleResponse[DashboardConfiguration])
  }

  /**
    * getClusterOverview returns the overview of the Flink cluster.
    */
  def getClusterOverview()(implicit ec: ExecutionContext): Future[FlinkClusterOverview] = {
    wsClient.url(url + "overview").get().map(responseHandler.handleResponse[FlinkClusterOverview])
  }
  /**
    * getJobManagerConfig returns the job manager configurations of the Flink server.
    */
  def getJobManagerConfig()(implicit ec: ExecutionContext): Future[Seq[FlinkConfig]] = {
    wsClient.url(url + "jobmanager/config").get().map(responseHandler.handleResponse[Seq[FlinkConfig]])
  }

  /**
    * getClusterTaskManagers gets a list of all task managers.
    */
  def getClusterTaskManagers()(implicit ec: ExecutionContext): Future[ClusterTaskManagers] = {
    wsClient.url(url + "taskmanagers").get().map(responseHandler.handleResponse[ClusterTaskManagers])
  }

  /**
    * getJobsList gets a list of all jobs, separated by the state of each job.
    */
  def getJobsList()(implicit ec: ExecutionContext): Future[JobIdsWithStatusOverview] = {
    wsClient.url(url + "jobs").get().map(responseHandler.handleResponse[JobIdsWithStatusOverview])
  }

  /**
    * getJobOverview provides a list of all running and finished jobs with a medium level of detail.
    */
  def getJobOverview()(implicit ec: ExecutionContext): Future[MultipleJobsDetails] = {
    wsClient.url(url + "jobs/overview").get().map(responseHandler.handleResponse[MultipleJobsDetails])
  }

  def getJarsList()(implicit ec: ExecutionContext): Future[JarsListInfo] = {
    wsClient.url(url + "jars").get().map(responseHandler.handleResponse[JarsListInfo])
  }
  /**
    * runProgram starts a job on the Flink server.
    *
    * IMPORTANT - The jarId is *not* the same as what appears in the Flink web UI -- there are hidden GUID values
    * prepended to the JAR name. If you use the uploadJar method, the correct value will be
    * returned in [[UploadJarResult.filename]].
    */
  def runProgram(
    jarId: String,
    programArguments: Option[Seq[String]] = None,
    entryClass: Option[String] = None,
    parallelism: Option[Int] = None,
    savepointPath: Option[String] = None,
    allowNonRestoredState: Option[Boolean] = None
  )(implicit ec: ExecutionContext): Future[JarRunResponseBody] = {
    val body = JarRunRequestBody(entryClass, None, programArguments, parallelism, allowNonRestoredState, savepointPath)
    wsClient.url(url + s"jars/$jarId/run")
      .post(Json.toJson(body))
      .map(responseHandler.handleResponse[JarRunResponseBody])
  }

  /**
    * runProgram starts a job on the Flink server.
    *
    * IMPORTANT - The jarId is *not* the same as what appears in the Flink web UI -- there are hidden GUID values
    * prepended to the JAR name. If you use the uploadJar method, the correct value will be
    * returned in [[UploadJarResult.filename]].
    */
  def planProgram(
                  jarId: String,
                  programArguments: Option[Seq[String]] = None,
                  entryClass: Option[String] = None,
                  parallelism: Option[Int] = None,
                  savepointPath: Option[String] = None,
                  allowNonRestoredState: Option[Boolean] = None
                )(implicit ec: ExecutionContext): Future[JarRunResponseBody] = {
    val body = JarRunRequestBody(entryClass, None, programArguments, parallelism, allowNonRestoredState, savepointPath)
    wsClient.url(url + s"jars/$jarId/plan")
      .withBody(Json.toJson(body))
      .get()
      .map(responseHandler.handleResponse[JarRunResponseBody])
  }

  /**
    * uploadJar uploads a JAR to the Flink server.
    */
  def uploadJar(
    file: File
  )(implicit ec: ExecutionContext): Future[UploadJarResult] = {
    // This is a mess and resorts to manually using the underlying async http client (rather than play-ws) because
    // of lack of support for multipart form uploads. See https://github.com/playframework/play-ws/issues/84
    // And on top of that, calling this method causes a dangling thread for smaller JAR files due to a bug in Netty;
    // see the sordid history at https://github.com/AsyncHttpClient/async-http-client/issues/233
    val filePart =
      new FilePart("jarfile", file, "application/x-java-archive", StandardCharsets.UTF_8, file.getName)
    val underlyingClient = wsClient.underlying[AsyncHttpClient]
    val requestBuilder = underlyingClient
      .preparePost(url + "jars/upload")
      .addBodyPart(filePart)

    val result = Promise[StandaloneAhcWSResponse]()
    val handler = new AsyncCompletionHandler[AHCResponse]() {
      override def onCompleted(response: AHCResponse): AHCResponse = {
        result.success(StandaloneAhcWSResponse(response))
        response
      }

      override def onThrowable(t: Throwable): Unit = {
        result.failure(t)
      }
    }

    underlyingClient.executeRequest(requestBuilder.build(), handler)
    val resultF = result.future

    resultF.map { response =>
      try {
        val json = Json.parse(response.body)
        json.validate[UploadJarResult] match {
          case JsSuccess(uploadJarResult, _) => uploadJarResult
          case JsError(e) => throw new RuntimeException(e.toString)
        }
      } catch {
        case e: JsonParseException => throw FlinkWrapperInvalidJsonException("Response was not valid JSON", e)
      }
    }
  }

  /**
    * getJobDetails returns detailed information about a single job.
    *
    * If the job does not exist, None is returned in the future.
    */
  def getJobDetails(
    jobId: String
  )(implicit ec: ExecutionContext): Future[Option[JobDetailsInfo]] = {
    wsClient.url(url + s"jobs/$jobId").get().map(responseHandler.handleResponseWith404[JobDetailsInfo])
  }

  /**
    * getJobPlan returns the job plan JSON for a given job.
    *
    * If the job does not exist, None is returned in the future.
    */
  def getJobPlan(
    jobId: String
  )(implicit ec: ExecutionContext): Future[Option[JobPlan]] = {
    wsClient.url(url + s"jobs/$jobId/plan").get().map(responseHandler.handleResponseWith404[JobPlan])
  }

  /**
    * cancelJob cancels an in progress job.
    *
    * Note that even if the job ID does not exist or is not in a cancellable state, this still returns a success.
    */
  def cancelJob(jobId: String)(implicit ec: ExecutionContext): Future[Unit] = {
    wsClient.url(url + s"jobs/$jobId").patch("").map(responseHandler.handleResponse[JsValue]).map { _ => () }
  }

  /**
    * cancelJob cancels an in progress job with a savepoint.
    *
    * If a target directory is supplied, it is used; otherwise, Flink defaults to the directory configured on the server.
    *
    * This method is asynchronous, on the Flink side; saving the state may take an extended period of time.
    * The [[CancelJobAccepted.location]] can be fed into [[getCancellationStatus()]] to query the status of the
    * cancellation.
    */
  def cancelJobWithSavepoint(
    jobId: String,
    targetDirectory: Option[String] = None
  )(implicit ec: ExecutionContext): Future[CancelJobAccepted] = {
    val targetDirectoryUrl = targetDirectory
      .map { unencoded => URLEncoder.encode(unencoded, "UTF-8")}
      .map { d => s"target-directory/$d/" }.getOrElse("")
    val fullUrl = url + s"jobs/$jobId/cancel-with-savepoint/$targetDirectoryUrl"

    wsClient.url(fullUrl).get().map(responseHandler.handleResponse[CancelJobAccepted])
  }

  /**
    * getCancellationStatus returns the status of a cancellation that is in progress (i.e. as the result of
    * [[cancelJobWithSavepoint()]].
    */
  def getCancellationStatus(
    location: String
  )(implicit ec: ExecutionContext): Future[AsynchronousOperationResult] = {
    wsClient.url(url + location).get().map(responseHandler.handleResponseIgnoreStatusCodes[AsynchronousOperationResult])
  }

  /**
    * cancelJob cancels an in progress job with a savepoint.
    *
    * If a target directory is supplied, it is used; otherwise, Flink defaults to the directory configured on the server.
    *
    * This method is asynchronous, on the Flink side; saving the state may take an extended period of time.
    * The [[CancelJobAccepted.location]] can be fed into [[getCancellationStatus()]] to query the status of the
    * cancellation.
    */
  def triggerSavepoint(jobId: String,
                       targetDirectory: Option[String] = None,
                       cancelJob: Boolean
                       )(implicit ec: ExecutionContext): Future[TriggerResponse] = {
    val fullUrl = url + s"jobs/$jobId/savepoints"
    wsClient.url(fullUrl)
      .addHttpHeaders("Content-Type" -> "application/json")
      .post(Json.toJson(SavepointTriggerRequestBody(targetDirectory, cancelJob))).map(responseHandler.handleResponse[TriggerResponse])
  }

  /**
    * getCancellationStatus returns the status of a cancellation that is in progress (i.e. as the result of
    * [[cancelJobWithSavepoint()]].
    */
  def getSavepointStatus( triggerId: String,
                          jobId: String
                         )(implicit ec: ExecutionContext): Future[AsynchronousOperationResult] = {
      wsClient.url(url + s"jobs/$jobId/savepoints/$triggerId").get().map(responseHandler.handleResponseIgnoreStatusCodes[AsynchronousOperationResult])
  }


  /**
    * getJobExceptions returns all exceptions associated with the job.
    *
    * If the job does not exist, None is returned in the future.
    */
  def getJobExceptions(
    jobId: String
  )(implicit ec: ExecutionContext): Future[Option[JobExceptions]] = {
    wsClient.url(url + s"jobs/$jobId/exceptions/").get().map(responseHandler.handleResponseWith404[JobExceptions])
  }

  /**
    * close terminates the actor system and closes the underlying HTTP client.
    *
    * This method must be called to properly clean up this client.
    */
  def close(): Unit = {
    wsClient.close()
    system.terminate()
  }
}

object FlinkRestClient {

  /**
    * apply creates a new [[FlinkRestClient]] pointing toward the given URL. It will create a new actor system.
    */
  def apply(url: String): FlinkRestClient = {
    new FlinkRestClient(FlinkRestClientConfig(
      url
    ))
  }

  /**
    * apply creates a new [[FlinkRestClient]] pointing toward the given URL, using the given actor system for HTTP
    * requests.
    */
  def apply(url: String, system: ActorSystem): FlinkRestClient = {
    new FlinkRestClient(FlinkRestClientConfig(
      url,
      Some(system)
    ))
  }
}

case class FlinkRestClientConfig(
  url: String,
  maybeActorSystem: Option[ActorSystem] = None,
  responseHandler: FlinkResponseHandler = FlinkResponseHandler
)