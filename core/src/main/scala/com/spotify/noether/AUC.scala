/*
 * Copyright 2018 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.noether

import breeze.linalg._
import com.twitter.algebird.{Aggregator, Semigroup}

case class MetricCurve(cm: Array[Map[(Int, Int), Long]]) extends Serializable

case class MetricCurvePoints(points: Array[MetricCurvePoint]) extends Serializable

case class MetricCurvePoint(x: Double, y: Double) extends Serializable

private[noether] object AreaUnderCurve {
  def trapezoid(points: Seq[MetricCurvePoint]): Double = {
    val x = points.head
    val y = points.last
    (y.x - x.x) * (y.y + x.y) / 2.0
  }

  def of(curve: MetricCurvePoints): Double = {
    curve.points.toIterator
      .sliding(2)
      .withPartial(false)
      .aggregate(0.0)(
        seqop = (auc: Double, points: Seq[MetricCurvePoint]) => auc + trapezoid(points),
        combop = _ + _
      )
  }
}

/**
 * Which function to apply on the list of confusion matrices prior to the AUC calculation.
 */
sealed trait AUCMetric

/**
 * <a href="https://en.wikipedia.org/wiki/Receiver_operating_characteristic">
 *   Receiver operating characteristic Curve
 * </a>
 */
case object ROC extends AUCMetric

/**
 * <a href="https://en.wikipedia.org/wiki/Precision_and_recall">
 *   Precision Recall Curve
 * </a>
 */
case object PR extends AUCMetric

/**
 * Compute a series of points for a collection of predictions.
 *
 * Internally a linspace is defined using the given number of [[samples]]. Each point in the
 * linspace represents a threshold which is used to build a confusion matrix. The (x,y) location
 * of the line is then returned.
 *
 * [[AUCMetric]] which is given to the aggregate selects the function to apply on
 * the confusion matrix prior to the AUC calculation.
 *
 * @param metric  Which function to apply on the confusion matrix.
 * @param samples Number of samples to use for the curve definition.
 */
case class Curve(metric: AUCMetric, samples: Int = 100)
    extends Aggregator[Prediction[Boolean, Double], MetricCurve, MetricCurvePoints] {
  private lazy val thresholds = linspace(0.0, 1.0, samples)
  private lazy val aggregators =
    thresholds.data.map(ClassificationReport(_)).toArray

  def prepare(input: Prediction[Boolean, Double]): MetricCurve =
    MetricCurve(aggregators.map(_.prepare(input)))

  def semigroup: Semigroup[MetricCurve] = {
    val sg = ClassificationReport().semigroup
    Semigroup.from {
      case (l, r) =>
        MetricCurve(l.cm.zip(r.cm).map { case (cl, cr) => sg.plus(cl, cr) })
    }
  }

  def present(c: MetricCurve): MetricCurvePoints = {
    val total = c.cm.map { matrix =>
      val scores = ClassificationReport().present(matrix)
      metric match {
        case ROC => MetricCurvePoint(scores.fpr, scores.recall)
        case PR  => MetricCurvePoint(scores.recall, scores.precision)
      }
    }.reverse

    val points = metric match {
      case ROC => total ++ Array(MetricCurvePoint(1.0, 1.0))
      case PR  => Array(MetricCurvePoint(0.0, 1.0)) ++ total
    }

    MetricCurvePoints(points)
  }
}

/**
 * Compute the "Area Under the Curve" for a collection of predictions. Uses the Trapezoid method to
 * compute the area.
 *
 * Internally a linspace is defined using the given number of [[samples]]. Each point in the
 * linspace represents a threshold which is used to build a confusion matrix.
 * The area is then defined on this list of confusion matrices.
 *
 * [[AUCMetric]] which is given to the aggregate selects the function to apply on
 * the confusion matrix prior to the AUC calculation.
 *
 * @param metric  Which function to apply on the confusion matrix.
 * @param samples Number of samples to use for the curve definition.
 */
case class AUC(metric: AUCMetric, samples: Int = 100)
    extends Aggregator[Prediction[Boolean, Double], MetricCurve, Double] {
  private val curve = Curve(metric, samples)
  def prepare(input: Prediction[Boolean, Double]): MetricCurve = curve.prepare(input)
  def semigroup: Semigroup[MetricCurve] = curve.semigroup
  def present(c: MetricCurve): Double = AreaUnderCurve.of(curve.present(c))
}
