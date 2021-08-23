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

import breeze.linalg.DenseMatrix
import com.twitter.algebird.{Aggregator, Semigroup}

/**
 * Special Case for a Binary Confusion Matrix to make it easier to compose with other binary
 * aggregators
 *
 * @param threshold
 *   Threshold to apply on predictions
 */
case class BinaryConfusionMatrix(threshold: Double = 0.5)
    extends Aggregator[Prediction[Boolean, Double], Map[(Int, Int), Long], DenseMatrix[Long]] {
  private val confusionMatrix = ConfusionMatrix(Seq(0, 1))

  def prepare(input: Prediction[Boolean, Double]): Map[(Int, Int), Long] = {
    val pred = Prediction(if (input.actual) 1 else 0, if (input.predicted > threshold) 1 else 0)
    confusionMatrix.prepare(pred)
  }
  def semigroup: Semigroup[Map[(Int, Int), Long]] = confusionMatrix.semigroup
  def present(m: Map[(Int, Int), Long]): DenseMatrix[Long] =
    confusionMatrix.present(m)
}
