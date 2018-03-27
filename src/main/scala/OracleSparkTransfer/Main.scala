package OracleSparkTransfer

import java.util.Properties

import org.apache.spark.sql.SparkSession

/**
  * Created by Devang Patel on 3/24/2018.
  */

case class AppConfig(
                      master: String = "local[*]",
                      user: String = "",
                      password: String = "",
                      dbHostPort: String = "",
                      dbName: String = "",
                      query: String = "",
                      outputPath: String = ""
                    )

object Main {

  def main(args: Array[String]): Unit = {

    val parser = new scopt.OptionParser[AppConfig]("Spark Oracle Data Transfer Utility: 1.0.0") {
      head("Spark Oracle Data Transfer Utility", "1.0.0")
      opt[String]('u', "user").required.valueName("<user>").action((x,c) => c.copy(user = x)).text("Oracle User Name: REQUIRED")
      opt[String]('p', "password").required.valueName("<password>").action((x,c) => c.copy(password = x)).text("Oracle Password: REQUIRED")
      opt[String]('h', "hostport").required.valueName("<host:port>").action((x,c) => c.copy(dbHostPort = x)).text("Oracle Database Host:Port: REQUIRED")
      opt[String]('d', "db").required.valueName("<database>").action((x,c) => c.copy(dbName = x)).text("Oracle Database Name: REQUIRED")
      opt[String]('o', "outputpath").required.valueName("<outputpath>").action((x,c) => c.copy(outputPath = x)).text("Output Path: REQUIRED")
      opt[String]('q', "sqlquery").required.valueName("<sqlquery>").action((x,c) => c.copy(query = x)).text("Oracle SQL Query to pull data: REQUIRED")
    }

    parser.parse(args, AppConfig()) match {
      case Some(config) => {

        val spark = SparkSession.builder().master(config.master).appName("Spark_Oracle_Data_Transfer").getOrCreate()
        val jdbcURL = s"jdbc:oracle:thin:@//${config.dbHostPort}/${config.dbName}"
        val oracleProperties = new Properties()
        oracleProperties.setProperty("user", config.user)
        oracleProperties.setProperty("password", config.password)
        oracleProperties.setProperty("driver", "oracle.jdbc.driver.OracleDriver")

        val oracleDF = spark.read.jdbc(url = jdbcURL, table = s"(${config.query}) oracle_data_pull", properties = oracleProperties)
        // print schema of dataframe.
        oracleDF.printSchema()
        oracleDF.show()
        // write data out as parquet files.
        oracleDF.write.parquet(config.outputPath)
      }
      case None => parser.showUsageAsError
    }

  }
}