import java.util

import scala.meta.Defn._
import scala.meta._
import java.io.{File, FileWriter}
import java.nio.file.{Files, Path, Paths}
import java.nio.charset.StandardCharsets
import java.text._
import java.util.Calendar

import org.scalameta.tests.typecheckError.Options
import scalafix.{CfgPerMethod, SemanticDB}
//import zamblauskas.csv.parser._
//import zamblauskas.functional._

import scala.collection.immutable
import scala.language.postfixOps
import sys.process._

object main extends App {

  def getProjectName(path: Path): String = {
    var tmp = path.toString
    tmp = tmp.replace("C:\\Users\\emill\\dev\\", "")
    val firstSlah = Math.max(tmp.indexOf('\\'), tmp.indexOf('/'))
    return tmp.substring(0, firstSlah)
  }

  def getCommitHashesFromLog(logString: String): Iterator[String] = {
    val re = """commit ([\w]+)""".r
    re.findAllIn(logString).map(x => x.substring(7))
  }

  def getGitTopLevel(path: File) = {
    execCommand("cd " + path + " && git rev-parse --show-toplevel").trim
  }

  def execCommand(command: String): String = {
    println("> " + command)
    return ("cmd /C " + command) !!
  }

  def parseCsvFromFile(file: File): immutable.Seq[Array[String]] = {

    var result = List[Array[String]]()
    if (file.exists) {
      val bufferedSource = scala.io.Source.fromFile(file)
      for (line <- bufferedSource.getLines) {
        val cols: Array[String] = line.split(",").map(_.trim)
        result = cols :: result

        //println(s"${cols(0)}|${cols(1)}|${cols(2)}|${cols(3)}")
      }
      bufferedSource.close
    }
    return result
  }

  def goTroughAllCommits(projectPath: File): Any = {
    //execCommand("cd " + projectPath)
    //System.setProperty("user.dir", projectPath.getAbsolutePath)
    var gitTopLevel = getGitTopLevel(projectPath)

    val projectName = getProjectName(projectPath.toPath)
    val result = parseCsvFromFile(new File("C:\\Users\\emill\\Dropbox\\slimmerWorden\\2018-2019-Semester2\\THESIS\\csv\\log_" + projectName + ".csv"))
    val hashesFromSvg: Seq[String] = result.map(x => x(1))

    execCommand("cd " + gitTopLevel + " && git reset .")
    clearGitRepo(new File(gitTopLevel))
    //execCommand("cd " + gitTopLevel + " && git clean -f") // for untracked files
    //execCommand("cd " + gitTopLevel + " && git checkout .") // for modified files
    //execCommand("cd " + gitTopLevel + " && git checkout master") // To get the full commit list from the beginning


    val hashes = getCommitHashesFromLog(execCommand("cd " + gitTopLevel + " && git log --format=\"commit %H\""))
    for (hash <- hashes) {
      try {
        if (!hashesFromSvg.contains(hash)) {
          execCommand("cd " + gitTopLevel + " && git clean -f") // for untracked files
          execCommand("cd " + gitTopLevel + " && git checkout .") // for modified files
          execCommand("cd " + gitTopLevel + " && git checkout " + hash)
          println(hash)
          doStatsForProject(projectPath, projectName, hash)
        }
      } catch {
        case x: Throwable => {
          println("Exception while calculating stats. Manually delete all files and start again, plz.")
          var commitStats = new CommitStats
          commitStats.commitHash = hash
          logToCsv(commitStats, projectName) // Add empty line to avoid calculating this commit in the future
          clearGitRepo(new File(gitTopLevel))
        }
      }
      //return
    }
  }

  // In case of complicated git mistakes
  def deleteRecursively(file: File): Unit = {
    if (file.isDirectory)
      file.listFiles.foreach(deleteRecursively)
    if (file.exists && !file.delete)
      throw new Exception(s"Unable to delete ${file.getAbsolutePath}")
  }

  def clearGitRepo(gitTopLevel: File) = {
    val filesAndFolders: Array[File] = gitTopLevel.listFiles //isFile to find files
    for (f <- filesAndFolders) {
      println(f)
      if (!f.getAbsolutePath.contains(".git")) {
        println("deleting this.")
        //f.delete()
        deleteRecursively(f)
      }
    }
    execCommand("cd " + gitTopLevel + " && git checkout master")
    execCommand("cd " + gitTopLevel + " && git checkout .")
  }

  def replaceTemplatesInString(file: String, commitStats: CommitStats, projectName: String): String = {
    var svg = file
    val nop = commitStats.nop_set.size
    val noc = commitStats.noc_set.size
    val nom = commitStats.nom_set.size
    val loc = commitStats.loc

    svg = svg.replaceAll("%PROJECT%", projectName)

    svg = svg.replaceAll("%NOP%", nop.toString)
    svg = svg.replaceAll("%NOC%", noc.toString)
    svg = svg.replaceAll("%NOM%", nom.toString)
    svg = svg.replaceAll("%LOC%", loc.toString)

    val df = new DecimalFormat(".##")
    svg = svg.replaceAll("%NOC/NOP%", df.format(noc.toDouble / nop))
    svg = svg.replaceAll("%NOM/NOC%", df.format(nom.toDouble / noc))
    svg = svg.replaceAll("%LOC/NOM%", df.format(loc.toDouble / nom))

    return svg
  }

  def logToCsv(commitStats: CommitStats, projectName: String) = {
    val nop = commitStats.nop_set.size
    val noc = commitStats.noc_set.size
    val nom = commitStats.nom_set.size
    val loc = commitStats.loc
    val avg_cc = commitStats.cc_set.sum / commitStats.cc_set.size
    val timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance.getTime)

    var str = projectName + ", " + commitStats.commitHash + ", " + nop + ", " + noc + ", " + nom + ", " + loc + ", " + avg_cc + ", " + timeStamp + "\n"
    val fw = new FileWriter("C:\\Users\\emill\\Dropbox\\slimmerWorden\\2018-2019-Semester2\\THESIS\\csv\\log_" + projectName + ".csv", true)
    try {
      fw.write(str)
    }
    finally fw.close()
  }

  def doStatsForProject(projectPath: File, projectName: String, commitHash: String) {

    def recursiveListFiles(f: File): Array[File] = {
      val these = f.listFiles()
      these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
    }

    //val scalaFiles = recursiveListFiles(projectPath)

    val commitStats = new CommitStats
    commitStats.commitHash = commitHash

    def consumeFile(exampleTree: Tree) = {


      val packageCollection = exampleTree.collect {
        case q: Pkg => q.name

      }
      packageCollection.foreach(x => commitStats.nop_set += x.toString)
      //println(packageCollection)

      //println(exampleTree)
      val classCollection = exampleTree.collect {
        case q: Defn.Class => q.name
        case q: Defn.Object => q.name
      }
      classCollection.foreach(x => commitStats.noc_set += x.toString)
      //println(classCollection)

      val functionCollection = exampleTree.collect {
        case q: Defn.Def => q.name

      }
      functionCollection.foreach(x => commitStats.nom_set += x.toString)
      //println(functionCollection)

      commitStats.loc += exampleTree.toString.count(x => x == '\n')
    }

    /*
        for (f <- scalaFiles) {
          if (f.toString.endsWith(".scala")) {
            //println(f)

          val bytes = java.nio.file.Files.readAllBytes(path)
          val text = new String(bytes, "UTF-8")
          val input = Input.VirtualFile(path.toString, text)
          val exampleTree = input.parse[Source].get

            //val path = java.nio.file.Paths.get(f)
            consumeFile(f.toPath)
          }
        }*/

    var scalaRoot = projectPath.getAbsolutePath.replace("\\", "/")
    if (!scalaRoot.endsWith("/")) scalaRoot += "/"
    if (scalaRoot.endsWith("src/main/"))
      scalaRoot = scalaRoot.substring(0, scalaRoot.length - "src/main/".length)

    val semanticDB = new SemanticDB(new File(scalaRoot))
    val documents = semanticDB.documents

    //var totalCC = 0
    //var totalMethods = 0
    for (main_doc <- documents) {
      println("\n Doc: " + main_doc.tdoc.uri)
      val doc = semanticDB.reload(main_doc.tdoc.uri)

      consumeFile(doc.sdoc.tree)

      println("Generating CFGs...")

      val methodMap = CfgPerMethod.compute(doc)

      for (pair <- methodMap) {
        val CC = CfgPerMethod.calculateCC(pair._2)
        println(pair._1 + ": CC= " + CC)
        //totalCC += CC
        commitStats.cc_set += CC
        //totalMethods += 1
      }
    }


    /*val filename = "C:\\Users\\emill\\Dropbox\\slimmerWorden\\2018-2019-Semester2\\THESIS\\maproef1819-emile\\svg\\pyramid.svg"
    var svg = scala.io.Source.fromFile(filename).getLines.mkString("\n")

    svg = replaceTemplatesInString(svg, commitStats, projectName)
    Files.write(Paths.get("C:\\Users\\emill\\Dropbox\\slimmerWorden\\2018-2019-Semester2\\THESIS\\maproef1819-emile\\svg\\pyramid_" + projectName + ".svg"),
      svg.getBytes(StandardCharsets.UTF_8))
    */

    logToCsv(commitStats, projectName)
  }

  goTroughAllCommits(new File("C:\\Users\\emill\\dev\\scalafixtemplate\\src\\main"))
  //goTroughAllCommits(new File("C:\\Users\\emill\\dev\\CTT-editor\\src\\main"))
  //goTroughAllCommits(new File("C:\\Users\\emill\\dev\\MoVE\\src\\main"))
  //goTroughAllCommits(new File("C:\\Users\\emill\\dev\\playframework\\framework\\src\\play\\src\\main"))
}
