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

private[noether] case class Curve(cm: List[Map[(Int, Int), Long]])

private[noether] object AreaUnderCurve {
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
  extends Aggregator[Prediction[Boolean, Double], Curve, Double] {

  private lazy val thresholds = linspace(0.0, 1.0, samples)
  private lazy val aggregators = thresholds.data.map(ClassificationReport(_)).toList

  def prepare(input: Prediction[Boolean, Double]): Curve = Curve(aggregators.map(_.prepare(input)))

  def semigroup: Semigroup[Curve] = {
    val sg = ClassificationReport().semigroup
    Semigroup.from{case(l, r) => Curve(l.cm.zip(r.cm).map{case(cl, cr) => sg.plus(cl, cr)})}
  }

  def present(c: Curve): Double = {
    val total = c.cm.map { matrix =>
      val scores = ClassificationReport().present(matrix)
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
