package com.spotify.noether

import com.twitter.algebird.{Aggregator, Semigroup}

/**
 * Compute the average precision of all the predictions, truncated at ranking position k.
 *
 * If for a prediction, the ranking algorithm returns n (n is less than k) results, the precision
 * value will be computed as #(relevant items retrieved) / k. This formula also applies when
 * the size of the ground truth set is less than k.
 *
 * If a prediction has an empty ground truth set, zero will be used as precision together
 *
 * See the following paper for detail:
 *
 * IR evaluation methods for retrieving highly relevant documents. K. Jarvelin and J. Kekalainen
 *
 * @param k the position to compute the truncated precision, must be positive
 */
case class PrecisionAtK[T](k: Int)
    extends Aggregator[RankingPrediction[T], (Double, Long), Double] {
  require(k > 0, "ranking position k should be positive")
  def prepare(input: RankingPrediction[T]): (Double, Long) = {
    val labSet = input.actual.toSet
    if (labSet.nonEmpty) {
      val n = math.min(input.predicted.length, k)
      var i = 0
      var cnt = 0
      while (i < n) {
        if (labSet.contains(input.predicted(i))) {
          cnt += 1
        }
        i += 1
      }
      (cnt.toDouble / k, 1L)
    } else {
      (0.0, 1L)
    }
  }

  def semigroup: Semigroup[(Double, Long)] = implicitly[Semigroup[(Double, Long)]]

  def present(score: (Double, Long)): Double = score._1 / score._2
}
