package EasyBQ

import org.apache.spark.sql.SparkSession
import com.mongodb.spark._
import com.mongodb.spark.sql._
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import java.util.ArrayList
import scala.collection.JavaConversions._
import org.bson.Document

import org.apache.log4j.Logger
import org.apache.log4j.Level

object EasyBQ123 {

  def main(args: Array[String]) {
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("com").setLevel(Level.OFF)
    val rankedGames = new TypeOfGame("rankedGames")
    println(rankedGames.first_15min_gain("XP"))
    // println(rankedGames.hero_having_most_stats("kill"))
  }
}

class TypeOfGame(val collection: String) {

  def get_spark_session(): SparkSession = {
    if (!(Array(
            "matchResults",
            "professionalGames",
            "publicGames",
            "rankedGames"
        ) contains collection)) {
      throw new IllegalArgumentException(
          collection + "is not a valid collection name"
      )
    }

    return SparkSession
      .builder()
      .master("local")
      .appName("Test")
      .config(
          "spark.mongodb.input.uri",
          "mongodb://127.0.0.1/dota2." + collection
      )
      .config(
          "spark.mongodb.output.uri",
          "mongodb://127.0.0.1/dota2." + collection
      )
      .getOrCreate()

  }

  def first_15min_gain(gold_or_XP: String): String = {
    val spark = get_spark_session()
    val rdd   = MongoSpark.load(spark.sparkContext)

    val combatlogs = rdd
      .flatMap(
          x =>
            x.get("combatlog")
              .asInstanceOf[ArrayList[Document]]
      )

    val valid_events = combatlogs
      .filter(
          x => x.get("type") == gold_or_XP
      )
      .filter(
          x => x.getDouble("time") <= 15 * 60
      )

    val hero_gain = valid_events
      .groupBy(x => x.get("target"))
      .map(
          x =>
            Tuple2(
                x._1,
                x._2.aggregate(0)(
                    (acc, item) =>
                      acc +
                        item.getInteger(
                            if (gold_or_XP == "gold") "change" else "xp"
                        ),
                    (acc1, acc2) => acc1 + acc2
                )
            )
      )

    val result: String = hero_gain
      .reduce(
          (x, y) => if (x._2 > y._2) x else y
      )
      ._1
      .toString()

    spark.stop()
    return result
  }

  def hero_having_most_stats(stats: String): String = {
    val spark = get_spark_session()
    val rdd   = MongoSpark.load(spark.sparkContext)

    return "Nothing yet"
  }
}
