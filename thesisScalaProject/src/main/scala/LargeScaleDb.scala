import java.io.File
import java.util.{ArrayList, Date}
import java.security.PublicKey
import java.sql._
import java.time.ZonedDateTime
import java.util

import slick.jdbc.SQLiteProfile.api._
import slickEmileProfile.Tables
import slickEmileProfile.Tables.BuildTriesRow

import scala.concurrent.{Await, duration}

object LargeScaleDb {

  val db = Database.forConfig("slickEmileProfile")
  // Should call this if database must close: finally db.close

  val build_tries: TableQuery[Tables.BuildTries] = TableQuery[Tables.BuildTries]

  //private var conn: Connection = _
  //try {
  //  Class.forName("org.sqlite.JDBC")
  //  conn = DriverManager.getConnection("jdbc:sqlite:C:\\Users\\emill\\Dropbox\\slimmerWorden\\2018-2019-Semester2\\THESIS\\out\\LargeScaleDb.sqlite")
  //} catch {
  //  case e: Exception =>
  //    System.err.println(e.getClass.getName + ": " + e.getMessage)
  //    System.exit(0)
  //}

  def insertBuildTry(buildPath: File, stdOutput: String, buildType: String): Unit = {

    var f = db.run(build_tries += BuildTriesRow(0, new Timestamp(new Date().getTime).toString, buildPath.toString, stdOutput, null))
    Await.result(f, duration.Duration(30, "sec"))
  }

  def getSuccesfullProjects: Seq[Tables.BuildTriesRow] = {
    var query = build_tries.filter(_.stdoutput.like("%[success]%"))
    var f = db.run(query.result)
    Await.result(f, duration.Duration(30, "sec"))
  }

  def getBuildTry(buildPath: File): Seq[_root_.slickEmileProfile.Tables.BuildTriesRow] = {
    var query = build_tries.filter(_.buildpath.like(buildPath.toString))
    var f = db.run(query.result)
    Await.result(f, duration.Duration(30, "sec"))
  }

  def hadSuccesfullBuild(buildPath: File): Boolean = {
    var builds = getBuildTry(buildPath)
    builds.exists(x => x.stdoutput.contains("[success]"))
  }

  def hadSuccesfullBuild(buildPath: File, buildType: String): Boolean = {
    var builds = getBuildTry(buildPath)
    builds.exists(x => x.stdoutput.contains("[success]") && x.buildtype == buildType)
  }
}


//case class BuildTry(id: Long, buildPath: File, buildDateTime: Long, stdOutput: String, buildType: String)