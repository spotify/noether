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

import org.scalactic.TolerantNumerics

class ConfusionMatrixAggregatorTest extends AggregatorTest {
  private implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(0.1)

  it should "return correct scores" in {
    val data = List(
      (0.1, false), (0.1, true), (0.4, false), (0.6, false), (0.6, true), (0.6, true), (0.8, true)
    ).map{case(s, pred) => Prediction(pred, s)}

    val matrix = run(ConfusionMatrixAggregator())(data)

    assert(matrix.tp === 3L)
    assert(matrix.fp === 1L)
    assert(matrix.fn === 1L)
    assert(matrix.tn === 2L)
  }
}

