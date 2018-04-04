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
  extends Aggregator[Prediction[Boolean, Double], Curve, Double] {

  private lazy val thresholds = linspace(0.0, 1.0, samples)
  private lazy val aggregators = thresholds.data.map(ClassificationAggregator(_)).toList

  def prepare(input: Prediction[Boolean, Double]): Curve = Curve(aggregators.map(_.prepare(input)))

  def semigroup: Semigroup[Curve] = {
    val sg = ClassificationAggregator().semigroup
    Semigroup.from{case(l, r) => Curve(l.cm.zip(r.cm).map{case(cl, cr) => sg.plus(cl, cr)})}
  }

  def present(c: Curve): Double = {
    val total = c.cm.map { matrix =>
      val scores = ClassificationAggregator().present(matrix)
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
