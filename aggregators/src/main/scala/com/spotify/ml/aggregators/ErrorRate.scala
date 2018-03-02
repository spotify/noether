package com.spotify.ml.aggregators

import com.twitter.algebird.Semigroup
import com.twitter.algebird.Aggregator

case class ErrorPrediction(scores: List[Double], label: Int) extends Serializable {
  override def toString: String = s"$label,${scores.mkString(":")}"
}

case object ErrorRateAggregator extends Aggregator[ErrorPrediction, (Double, Long), Double] {
  def prepare(input: ErrorPrediction): (Double, Long) = {
    val best = input.scores.zipWithIndex.maxBy(_._1)._2
    if(best == input.label) (0.0, 1L) else (1.0, 1L)
  }
  def semigroup: Semigroup[(Double, Long)] = implicitly[Semigroup[(Double, Long)]]
  def present(score: (Double, Long)): Double = score._1/score._2
}