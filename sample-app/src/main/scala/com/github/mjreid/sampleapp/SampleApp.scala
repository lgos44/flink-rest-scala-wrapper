package com.github.mjreid.sampleapp

import java.io.File
import java.util.concurrent.TimeUnit

import com.github.mjreid.flinkwrapper._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

/**
  * SampleApp is a small program that serves as an "integration test suite" of sorts. Though this requires a Flink
  * instance at localhost:8081, plus some sample Flink jobs, so it's mostly manual to set up.
  *
  * Hopefully temporary until a real integration test solution is added.
  */
object SampleApp extends App {

  val flinkUrl = "http://localhost:8081"
  val flinkClient = FlinkRestClient(flinkUrl)

  def runGetConfig(): Unit = {
    val result = flinkClient.getConfig().map { config =>
      println(config)
    }

    Await.result(result, FiniteDuration(1, TimeUnit.SECONDS))
  }

  def runGetJobsList(): Unit = {
    val result = flinkClient.getJobsList().map { jobsList =>
      println(jobsList)
    }

    Await.result(result, FiniteDuration(1, TimeUnit.SECONDS))
  }

  def runGetJobOverview(): Unit = {
    val result = flinkClient.getJobOverview().map { jobOverview =>
      println(jobOverview)
    }
    Await.result(result, FiniteDuration(10, TimeUnit.SECONDS))
  }

  def runGetJarsList(): JarsListInfo = {
    val resultF = flinkClient.getJarsList()
    val result = Await.result(resultF, FiniteDuration(1, TimeUnit.SECONDS))
    println(result)
    result
  }

  def runStartProgram(jarName: String, mainClass: Option[String], programArguments: Option[Seq[String]] = None): JarRunResponseBody = {
    val result = flinkClient.runProgram(
      jarName,
      entryClass = mainClass.orElse(Some("org.example.WordCount")),
      programArguments = programArguments
    )

    val jobResult = Await.result(result, FiniteDuration(60, TimeUnit.SECONDS))
    println(jobResult)
    jobResult
  }

  def runUploadJar(): String = {
    val resultF = flinkClient.uploadJar(
      new File("/a.jar")
    )
    val result = Await.result(resultF, FiniteDuration(300, TimeUnit.SECONDS))
    println(result)
    new File(result.filename).getName
  }

  def runGetJobDetails(jobId: String): Unit = {
    // val jobId = "67fef029dd746f4c47cf61d28189a4fd"

    val result = flinkClient.getJobDetails(jobId)

    val response = Await.result(result, FiniteDuration(1, TimeUnit.SECONDS))
    println(response)
  }

  def runGetJobPlan(jobId: String): Unit = {
    val result = flinkClient.getJobPlan(jobId)
    val response = Await.result(result, FiniteDuration(1, TimeUnit.SECONDS))
    println(response)
  }

  def runCancelJob(jobId: String): Unit = {
    val resultF = flinkClient.cancelJob(jobId)
    val result = Await.result(resultF, FiniteDuration(1, TimeUnit.SECONDS))
    println(result)
  }

  def runTriggerSavepoint(jobId: String, savepointPath: String): TriggerResponse = {
    val resultF = flinkClient.triggerSavepoint(jobId, Some(savepointPath), true)
    val result = Await.result(resultF, FiniteDuration(1, TimeUnit.SECONDS))
    println(result)
    result
  }

  def runGetSavepointStatus(triggerId: String, jobId:String): AsynchronousOperationResult = {
    val resultF = flinkClient.getSavepointStatus(triggerId, jobId)
    val result = Await.result(resultF, FiniteDuration(1, TimeUnit.SECONDS))
    println(result)
    result
  }

  def runGetJobExceptions(str: String): JobExceptions = {
    val resultF = flinkClient.getJobExceptions(str)
    val result = Await.result(resultF, FiniteDuration(1, TimeUnit.SECONDS))
    println(result)
    result.get
  }

  runGetConfig()
  runGetJobsList()
  runGetJarsList()
  runGetJobOverview()
  runGetJobOverview()
  val jarName = runUploadJar()
  val runProgramResult = runStartProgram(jarName, None)
  runGetJobDetails(runProgramResult.jobId)
  runGetJobPlan(runProgramResult.jobId)

  {
    // Streaming and cancellation testing
    val kafkaProgramResult = runStartProgram(jarName, Some("org.example.WordCount"))
    Thread.sleep(1000)
    runGetJobDetails(kafkaProgramResult.jobId)
    runCancelJob(kafkaProgramResult.jobId)
    runGetJobDetails(kafkaProgramResult.jobId)
  }

  {
    // Streaming and cancellation with savepoint testing
    val kafkaProgramResult = runStartProgram(jarName, Some("org.example.WordCount"))
    Thread.sleep(1000)
    runGetJobDetails(kafkaProgramResult.jobId)
    val cancelJobAccepted = runTriggerSavepoint(kafkaProgramResult.jobId, "tmp")
    runGetJobDetails(kafkaProgramResult.jobId)
    var status = runGetSavepointStatus(cancelJobAccepted.requestId, kafkaProgramResult.jobId)
    println(status)
    while (status.status.id != StatusTypes.Failed && status.status.id != StatusTypes.Completed) {
      Thread.sleep(10000)
      status = runGetSavepointStatus(cancelJobAccepted.requestId, kafkaProgramResult.jobId)
      println(status)
    }

    val jobExceptions = runGetJobExceptions(kafkaProgramResult.jobId)
    println(jobExceptions)
  }

  flinkClient.close()
}
