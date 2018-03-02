package com.spotify.ml.aggregators

import com.twitter.algebird.{Aggregator, Semigroup}
import breeze.linalg._

case class Curve(cm: List[ConfusionMatrix])

object AreaUnderCurve {
  def trapezoid(points: Seq[(Double, Double)]): Double = {
    require(points.length == 2)
    val x = points.head
    val y = points.last
    (y._1 - x._1) * (y._2 + x._2) / 2.0
  }

  def of(curve: List[(Double, Double)]): Double = {
    curve.toIterator.sliding(2).withPartial(false).aggregate(0.0)(
      seqop = (auc: Double, points: Seq[(Double, Double)]) => auc + trapezoid(points),
      combop = _ + _
    )
  }
}

sealed trait AUCMetric
case object ROC extends AUCMetric
case object PR extends AUCMetric

case class AUCAggregator(metric: AUCMetric, samples: Int = 100)
  extends Aggregator[Prediction, Curve, Double]
  with Serializable {

  private lazy val thresholds = linspace(0.0, 1.0, samples)
  private lazy val aggregators = thresholds.data.map(ConfusionMatrixAggregator(_)).toList

  def prepare(input: Prediction): Curve = Curve(aggregators.map(_.prepare(input)))

  def semigroup: Semigroup[Curve] = {
    val sg = ConfusionMatrixAggregator().semigroup
    Semigroup.from{case(l, r) => Curve(l.cm.zip(r.cm).map{case(cl, cr) => sg.plus(cl, cr)})}
  }

  def present(c: Curve): Double = {
    val total = c.cm.map { matrix =>
      val scores = ConfusionMatrixAggregator().present(matrix)
      metric match {
        case ROC => (scores.fpr, scores.recall)
        case PR => (scores.recall, scores.precision)
      }
    }.reverse

    val combined = metric match {
      case ROC => total ++ List((1.0,1.0))
      case PR => List((0.0,1.0)) ++ total
    }

    AreaUnderCurve.of(combined)
  }
}