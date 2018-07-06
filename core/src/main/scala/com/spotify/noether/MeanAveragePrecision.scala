package com.spotify.noether

import com.twitter.algebird.{Aggregator, Semigroup}

/**
 * Returns the mean average precision (MAP) of all the predictions.
 * If a query has an empty ground truth set, the average precision will be zero
 */
case class MeanAveragePrecision[T]()
    extends Aggregator[RankingPrediction[T], (Double, Long), Double] {
  def prepare(input: RankingPrediction[T]): (Double, Long) = {
    val labSet = input.actual.toSet
    if (labSet.nonEmpty) {
      var i = 0
      var cnt = 0
      var precSum = 0.0
      val n = input.predicted.length
      while (i < n) {
        if (labSet.contains(input.predicted(i))) {
          cnt += 1
          precSum += cnt.toDouble / (i + 1)
        }
        i += 1
      }
      (precSum / labSet.size, 1L)
    } else {
      (0.0, 1L)
    }
  }

  def semigroup: Semigroup[(Double, Long)] = implicitly[Semigroup[(Double, Long)]]

  def present(score: (Double, Long)): Double = score._1 / score._2
}
