package org.zouzias.spark.lucenerdd.aws.utils

import org.zouzias.spark.lucenerdd.aws.utils.SearchType.SearchType


case class SearchInfo(searchType: SearchType, executorInstances: String, executorMemory: String, executorCores: String){

  override def print(): String = {
    s"${searchType.toString}-${executorInstances}-${executorMemory}-${executorCores}"
  }
}
