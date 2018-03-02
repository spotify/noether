package com.spotify.ml.aggregators

import com.twitter.algebird.Semigroup
import com.twitter.algebird.Aggregator

case class LogLossPrediction(scores: List[Double], label: Int) extends Serializable {
  override def toString: String = s"$label,${scores.mkString(":")}"
}

case object LogLossAggregator extends Aggregator[LogLossPrediction, (Double, Long), Double] {
  def prepare(input: LogLossPrediction): (Double, Long) = (math.log(input.scores(input.label)), 1L)
  def semigroup: Semigroup[(Double, Long)] = implicitly[Semigroup[(Double, Long)]]
  def present(score: (Double, Long)): Double = -1*(score._1/score._2)
}