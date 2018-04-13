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
 * Generic Consfusion Matrix Aggregator for any dimension.
 * Thresholds must be applied to make a prediction prior to using this aggregator.
 *
 * @param labels List of possible label values
 */
final case class ConfusionMatrix(labels: Seq[Int])
    extends Aggregator[Prediction[Int, Int], Map[(Int, Int), Long], DenseMatrix[Long]] {

  def prepare(input: Prediction[Int, Int]): Map[(Int, Int), Long] =
    Map((input.predicted, input.actual) -> 1L)

  def semigroup: Semigroup[Map[(Int, Int), Long]] =
    Semigroup.mapSemigroup[(Int, Int), Long]

  def present(m: Map[(Int, Int), Long]): DenseMatrix[Long] = {
    val mat = DenseMatrix.zeros[Long](labels.size, labels.size)
    for {
      i <- labels
      j <- labels
    } {
      mat(i, j) = m.getOrElse((i, j), 0L)
    }
    mat
  }
}
