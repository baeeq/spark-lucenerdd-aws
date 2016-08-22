package org.zouzias.spark.lucenerdd.aws.linkage

import org.apache.spark.sql.{SQLContext, SaveMode}
import org.apache.spark.{Logging, SparkConf, SparkContext}
import org.zouzias.spark.lucenerdd.aws.utils.{LinkedRecord, WikipediaUtils}
import org.zouzias.spark.lucenerdd.{LuceneRDD, _}
/**
 * H1B visas vs geonames cities linkage example
 */
object VisaGeonamesLinkageExample extends Logging {

  def main(args: Array[String]) {

    // initialise spark context
    val conf = new SparkConf().setAppName("VisaGeonamesLinkageExample")

    //
    implicit val sc = new SparkContext(conf)
    implicit val sqlContext = new SQLContext(sc)

    val today = WikipediaUtils.dayString()
    val executorMemory = conf.get("spark.executor.memory")
    val executorCores = conf.get("spark.executor.cores")
    val executorInstances = conf.get("spark.executor.instances")
    val fieldName = "lca_case_employer_city"


    log.info(s"Executor instances: ${executorInstances}")
    log.info(s"Executor cores: ${executorCores}")
    log.info(s"Executor memory: ${executorMemory}")

    logInfo("Loading H1B Visa")
    val visa = sqlContext.read.parquet("s3://h1b-visa-enigma.io/enigma-io-h1bvisa.parquet")
    val luceneRDD = LuceneRDD(visa.select(fieldName))
    luceneRDD.cache()
    logInfo("H1B Visas loaded successfully")

    logInfo("Loading Geonames Cities")
    val citiesDF = sqlContext.read.parquet("s3://recordlinkage/geonames-usa-cities.parquet")
    val cities = citiesDF.flatMap(row => Option(row.getString(1)))
    cities.cache()
    logInfo("Geonames cities loaded successfully")

    val orLinker = (cityName: String) => {
      val nameTokenized = cityName.split(" ").map(_.replaceAll("[^a-zA-Z0-9]", "")).filter(_.length > 3).mkString(" OR ")

      if (nameTokenized.nonEmpty){
        s"${fieldName}:(${nameTokenized})"
      }
      else {
        "*:*"
      }
    }

    val linked = luceneRDD.link(cities, orLinker, 3)

    linked.cache

    import sqlContext.implicits._
    val linkedDF = linked.map{ case (left, right) => LinkedRecord(left, right.headOption.map(_.doc.textField(fieldName).toArray))}
      .toDF()

    linkedDF.write.mode(SaveMode.Overwrite).parquet(s"s3://spark-lucenerdd/timings/v0.0.20/visa-vs-geonames-linkage-${today}.parquet")

    // terminate spark context
    sc.stop()

  }
}
