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
import org.scalactic.TolerantNumerics

class ConfusionMatrixTest extends AggregatorTest {
  it should "return correct confusion matrix" in {
    val data =
      List(
        (0,0), (0,0), (0,0),
        (0,1), (0,1),
        (1,0), (1,0), (1,0), (1,0),
        (1,1), (1,1),
        (2,1),
        (2,2), (2,2), (2,2)
      ).map{case(p, a) => Prediction(a, p)}

    val labels = Seq(0,1,2)
    val actual = run(ConfusionMatrix(labels))(data)

    val mat = DenseMatrix.zeros[Long](labels.size, labels.size)
    mat(0,0) = 3L
    mat(0,1) = 2L
    mat(0,2) = 0L
    mat(1,0) = 4L
    mat(1,1) = 2L
    mat(1,2) = 0L
    mat(2,0) = 0L
    mat(2,1) = 1L
    mat(2,2) = 3L
    assert(actual == mat)
  }

  it should "return correct scores" in {
    val data = List(
      (0, 0), (0, 1), (0, 0), (1, 0), (1, 1), (1, 1), (1, 1)
    ).map{case(s, pred) => Prediction(pred, s)}

    val matrix = run(ConfusionMatrix(Seq(0,1)))(data)

    assert(matrix(1, 1) === 3L)
    assert(matrix(0, 1) === 1L)
    assert(matrix(1, 0) === 1L)
    assert(matrix(0, 0) === 2L)
  }
}
