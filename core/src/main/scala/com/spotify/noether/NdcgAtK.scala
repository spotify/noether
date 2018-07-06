package com.spotify.noether

import com.twitter.algebird.{Aggregator, Semigroup}

/**
 * Compute the average NDCG value of all the predictions, truncated at ranking position k.
 * The discounted cumulative gain at position k is computed as:
 *    sum,,i=1,,^k^ (2^{relevance of ''i''th item}^ - 1) / log(i + 1),
 * and the NDCG is obtained by dividing the DCG value on the ground truth set. In the current
 * implementation, the relevance value is binary.
 * If a query has an empty ground truth set, zero will be used as ndcg
 *
 * See the following paper for detail:
 *
 * IR evaluation methods for retrieving highly relevant documents. K. Jarvelin and J. Kekalainen
 *
 * @param k the position to compute the truncated ndcg, must be positive
 */
case class NdcgAtK[T](k: Int) extends Aggregator[RankingPrediction[T], (Double, Long), Double] {
  require(k > 0, "ranking position k should be positive")
  def prepare(input: RankingPrediction[T]): (Double, Long) = {
    val labSet = input.actual.toSet

    if (labSet.nonEmpty) {
      val labSetSize = labSet.size
      val n = math.min(math.max(input.predicted.length, labSetSize), k)
      var maxDcg = 0.0
      var dcg = 0.0
      var i = 0
      while (i < n) {
        val gain = 1.0 / math.log(i + 2.0)
        if (i < input.predicted.length && labSet.contains(input.predicted(i))) {
          dcg += gain
        }
        if (i < labSetSize) {
          maxDcg += gain
        }
        i += 1
      }
      (dcg / maxDcg, 1L)
    } else {
      (0.0, 1L)
    }
  }

  def semigroup: Semigroup[(Double, Long)] = implicitly[Semigroup[(Double, Long)]]

  def present(score: (Double, Long)): Double = score._1 / score._2
}
