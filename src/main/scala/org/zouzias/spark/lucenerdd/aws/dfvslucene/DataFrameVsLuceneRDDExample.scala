package org.zouzias.spark.lucenerdd.aws.dfvslucene

import org.apache.spark.sql.{Row, SQLContext, SaveMode}
import org.apache.spark.{Logging, SparkConf, SparkContext}
import org.zouzias.spark.lucenerdd.aws.utils._
import org.zouzias.spark.lucenerdd.facets.FacetedLuceneRDD
import org.zouzias.spark.lucenerdd._
import org.apache.spark.sql.functions._


/**
 * Dataframe vs LuceneRDD
 */
object DataFrameVsLuceneRDDExample extends Logging {

  val k = 10
  val fieldName = "Country"

  def main(args: Array[String]) {

    // initialise spark context
    val conf = new SparkConf().setAppName(DataFrameVsLuceneRDDExample.getClass.getName)

    implicit val sc = new SparkContext(conf)
    implicit val sqlContext = new SQLContext(sc)

    val executorMemory = conf.get("spark.executor.memory")
    val executorCores = conf.get("spark.executor.cores")
    val executorInstances = conf.get("spark.executor.instances")

    log.info(s"Executor instances: ${executorInstances}")
    log.info(s"Executor cores: ${executorCores}")
    log.info(s"Executor memory: ${executorMemory}")

    logInfo("Loading Cities")
    val citiesDF = sqlContext.read.parquet("s3://recordlinkage/world-cities-maxmind.parquet")
    citiesDF.cache()
    val total = citiesDF.count()
    logInfo(s"${total} Cities loaded successfully")


    val dfStart = System.currentTimeMillis()
    val dfResults = citiesDF.groupBy(fieldName).count().sort(desc("count")).take(k)
    val dfEnd = System.currentTimeMillis()

    val luceneRDD = FacetedLuceneRDD(citiesDF.select(fieldName))
    luceneRDD.cache()
    luceneRDD.count()
    val lucStart =System.currentTimeMillis()
    val lucEnd =System.currentTimeMillis()


    println("=" * 20)
    println(s"DF time: ${ (dfEnd - dfStart) / 1000D } seconds")
    println("=" * 20)
    println(s"Lucene time: ${(lucEnd - lucStart) / 1000D} seconds")
    println("=" * 20)


    // linkedDF.write.mode(SaveMode.Overwrite).parquet(s"s3://spark-lucenerdd/timings/v0.0.20/dataframe-vs-lucenerdd-${today}.parquet")

    // terminate spark context
    sc.stop()

  }

  def timeLuceneFacetedSearch(luceneRDD: FacetedLuceneRDD[Row], iters: Long, searchInfo: SearchInfo)(implicit sqlContext: SQLContext): Unit = {
    val timings = (1L until iters).map{ case _ =>

      val start = System.currentTimeMillis()
      val luceneResults = luceneRDD.facetQuery("*:*", fieldName, k)
      val end = System.currentTimeMillis()

      Math.max(0L, end - start)
    }


    import sqlContext.implicits._
    val timingsDF = timings.map(Timing(searchInfo.searchType.toString, _)).toDF()

    timingsDF.write.mode(SaveMode.Append).parquet(s"s3://spark-lucenerdd/timings/v${Utils.Version}/timing-dfvslucene-${WikipediaUtils.dayString}-${searchInfo.toString()}.parquet")

  }
}
