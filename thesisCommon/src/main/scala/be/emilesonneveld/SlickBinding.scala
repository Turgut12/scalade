package be.emilesonneveld

import com.typesafe.config.ConfigFactory

/**
  * This generates ORM bindings from the database scheme to .scala code
  * http://slick.lightbend.com/doc/3.3.1/code-generation.html
  */
object SlickBinding {

  def main(args: Array[String]): Unit = {
    val applicationConfig = ConfigFactory.load()
    val profile = "slick.jdbc.SQLiteProfile"
    val jdbcDriver = applicationConfig.getString("slickEmileProfile.driver") //"org.sqlite.JDBC"
    val url = applicationConfig.getString("slickEmileProfile.url") // "jdbc:sqlite:C:\\Use...st.sqlite"
    val pkg = "be.emilesonneveld.slickEmileProfile"
    val outputFolder = "src\\main\\scala"

    slick.codegen.SourceCodeGenerator.main(
      Array(profile, jdbcDriver, url, outputFolder, pkg)
    )
  }
}
