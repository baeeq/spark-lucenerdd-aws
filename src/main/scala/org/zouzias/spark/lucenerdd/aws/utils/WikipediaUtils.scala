package org.zouzias.spark.lucenerdd.aws.utils

import java.text.{DateFormat, FieldPosition, ParsePosition}
import java.util.Date

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.joda.time.DateTime



object WikipediaUtils {


  val topK = 10

  def loadWikipediaTitles(implicit sqlContext: SQLContext): RDD[String] = {
    sqlContext.read.parquet("s3://spark-lucenerdd/wikipedia/enwiki-latest-all-titles.parquet")
      .map(row => row.getString(0)).map(_.replaceAll("_", " ")).map(_.replaceAll("[^a-zA-Z0-9\\s]", ""))
  }

  def sampleTopKWikipediaTitles(k: Int)(implicit sqlContext: SQLContext): List[String] = {
    loadWikipediaTitles.sample(false, 0.01).take(k).toList
  }


  def dayString(): String = {
    val date = new DateTime()
    s"${date.year()} ${date.monthOfYear()} ${date.dayOfWeek().getAsText()}"
  }

}
