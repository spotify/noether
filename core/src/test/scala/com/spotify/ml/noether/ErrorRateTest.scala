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

import org.scalactic.TolerantNumerics
import org.scalatest.{FlatSpec, Matchers}

class ErrorMatrixTest extends FlatSpec with Matchers {
  private implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(0.1)
  private val classes = 10
  private def s(idx: Int): List[Double] =
    0.until(classes).map(i => if(i == idx) 1.0 else 0.0).toList

  it should "return correct scores" in {
      val data = List((s(1), 1), (s(3), 1), (s(5), 5), (s(2), 3), (s(0), 0), (s(8), 1))
        .map{case(scores, label) => ErrorPrediction(scores, label)}

      assert(ErrorRateAggregator(data) === 0.5)
    }
}