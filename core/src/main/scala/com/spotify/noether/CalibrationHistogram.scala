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

import com.twitter.algebird.{Aggregator, Semigroup}

import scala.math.floor

/**
 * Histogram bucket.
 *
 * @param lowerThresholdInclusive Lower bound on bucket, inclusive
 * @param upperThresholdExclusive Upper bound on bucket, exclusive
 * @param numPredictions Number of predictions in this bucket
 * @param sumLabels Sum of label values for this bucket
 * @param sumPredictions Sum of prediction values for this bucket
 */
final case class CalibrationHistogramBucket(
  lowerThresholdInclusive: Double,
  upperThresholdExclusive: Double,
  numPredictions: Double,
  sumLabels: Double,
  sumPredictions: Double
)

/**
 * Split predictions into Tensorflow Model Analysis compatible CalibrationHistogramBucket buckets.
 *
 * If a prediction is less than the lower bound, it belongs to the bucket [-inf, lower bound)
 * If it is greater than or equal to the upper bound, it belongs to the bucket (upper bound, inf]
 *
 * @param lowerBound Left boundary, inclusive
 * @param upperBound Right boundary, exclusive
 * @param numBuckets Number of buckets in the histogram
 */
// scalastyle:off no.whitespace.after.left.bracket
final case class CalibrationHistogram(
  lowerBound: Double = 0.0,
  upperBound: Double = 1.0,
  numBuckets: Int = 10
) extends Aggregator[
      Prediction[Double, Double],
      Map[Double, (Double, Double, Long)],
      List[CalibrationHistogramBucket]
    ] {
  val bucketSize = (upperBound - lowerBound) / numBuckets.toDouble

  private def thresholdsFromBucket(b: Double): (Double, Double) = b match {
    case Double.PositiveInfinity => (upperBound, b)
    case Double.NegativeInfinity => (b, lowerBound)
    case _ => {
      (lowerBound + (b * bucketSize), lowerBound + (b * bucketSize) + bucketSize)
    }
  }

  def prepare(input: Prediction[Double, Double]): Map[Double, (Double, Double, Long)] = {
    val bucketNumber = input.predicted match {
      case p if p < lowerBound  => Double.NegativeInfinity
      case p if p >= upperBound => Double.PositiveInfinity
      case _ =>
        floor((input.predicted - lowerBound) / bucketSize)
    }

    Map((bucketNumber, (input.predicted, input.actual, 1L)))
  }

  def semigroup: Semigroup[Map[Double, (Double, Double, Long)]] =
    Semigroup.mapSemigroup[Double, (Double, Double, Long)]

  def present(m: Map[Double, (Double, Double, Long)]): List[CalibrationHistogramBucket] = {
    val buckets = Vector(Double.NegativeInfinity) ++
      (0 until numBuckets).map(_.toDouble).toVector ++
      Vector(Double.PositiveInfinity)
    buckets.map { l =>
      val (lb, ub) = thresholdsFromBucket(l)
      m.get(l) match {
        case None => CalibrationHistogramBucket(lb, ub, 0.0, 0.0, 0.0)
        case Some((predictionSum, labelSum, numExamples)) =>
          CalibrationHistogramBucket(lb, ub, numExamples.toDouble, labelSum, predictionSum)
      }
    }.toList
  }
}
// scalastyle:on no.whitespace.after.left.bracket
