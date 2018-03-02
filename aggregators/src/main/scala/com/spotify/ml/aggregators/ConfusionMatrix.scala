package com.spotify.ml.aggregators

import com.twitter.algebird.Semigroup
import com.twitter.algebird.Aggregator

case class Prediction(label: Int, score: Double) extends Serializable {
  override def toString: String = s"$label,$score"
}

case class ConfusionMatrix(tp: Int = 0, fp: Int = 0, fn: Int = 0, tn: Int = 0)
  extends Serializable

case class Scores(fscore: Double, precision: Double, recall: Double, fpr: Double)
  extends Serializable

case class ConfusionMatrixAggregator(threshold: Double = 0.5, beta: Double = 1.0)
  extends Aggregator[Prediction, ConfusionMatrix, Scores]
    with Serializable {

  def prepare(input: Prediction): ConfusionMatrix =
    (input.label, input.score) match {
      case (1, score) if score > threshold => ConfusionMatrix(tp = 1)
      case (1, score) if score < threshold => ConfusionMatrix(fn = 1)
      case (0, score) if score < threshold => ConfusionMatrix(tn = 1)
      case (0, score) if score > threshold => ConfusionMatrix(fp = 1)
    }

  def semigroup: Semigroup[ConfusionMatrix] =
    Semigroup.from{case(l, r) =>
      val tp = l.tp + r.tp
      val fp = l.fp + r.fp
      val fn = l.fn + r.fn
      val tn = l.tn + r.tn

      ConfusionMatrix(tp, fp, fn, tn)
    }

  def present(m: ConfusionMatrix): Scores = {
    val precDenom = m.tp.toDouble + m.fp.toDouble
    val precision = if(precDenom > 0.0) m.tp.toDouble/precDenom else 1.0

    val recallDenom = m.tp.toDouble + m.fn.toDouble
    val recall = if(recallDenom > 0.0) m.tp.toDouble/recallDenom else 1.0

    val fpDenom = m.fp.toDouble + m.tn.toDouble
    val fpr = if(fpDenom > 0.0) m.fp.toDouble/fpDenom else 0.0

    val betaSqr = Math.pow(beta, 2.0)

    val fScoreDenom = (betaSqr*precision) + recall
    val fscore = if(fScoreDenom > 0.0){
      (1 + betaSqr) * ((precision*recall) / fScoreDenom)
    } else { 1.0 }

    Scores(fscore, precision, recall, fpr)
  }
}