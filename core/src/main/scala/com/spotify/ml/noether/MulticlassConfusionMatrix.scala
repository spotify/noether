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

package com.spotify.ml.noether

import com.twitter.algebird._

final case class PredictionResult(predicted: Int, actual: Int)

final case class MulticlassConfusionMatrixAggregator(labels: Seq[Int])
  extends Aggregator[PredictionResult, Map[(Int, Int), Long], Map[(Int, Int), Long]]
  with Serializable {

  def prepare(input: PredictionResult): Map[(Int, Int), Long] = {
    Map((input.predicted, input.actual) -> 1L)
  }

  def semigroup: Semigroup[Map[(Int, Int), Long]] = Semigroup.mapSemigroup[(Int, Int), Long]

  def present(m: Map[(Int, Int), Long]): Map[(Int, Int), Long] = {
    val b = Map.newBuilder[(Int, Int), Long]

    for(i <- labels;
        j <- labels) {
      b += (((i,j), m.getOrElse((i,j), 0L)))
    }

    b.result()
  }
}
