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

import org.scalatest.{FlatSpec, Matchers}

class MulticlassConfusionMatrixTest extends FlatSpec with Matchers {

  it should "return correct confusion matrix" in {
    val data =
      List(
        (0,0), (0,0), (0,0),
        (0,1), (0,1),
        (1,0), (1,0), (1,0), (1,0),
        (1,1), (1,1),
        (2,1),
        (2,2), (2,2), (2,2)
      ).map{case(p, a) => PredictionResult(p, a)}

    val labels = Seq(0,1,2)
    val actual = MulticlassConfusionMatrixAggregator(labels)(data)

    val expected = Map[(Int, Int), Long](
      (0,0) -> 3L,
      (0,1) -> 2L,
      (0,2) -> 0L,
      (1,0) -> 4L,
      (1,1) -> 2L,
      (1,2) -> 0L,
      (2,0) -> 0L,
      (2,1) -> 1L,
      (2,2) -> 3L
    )
    assert(actual == expected)
  }
}
