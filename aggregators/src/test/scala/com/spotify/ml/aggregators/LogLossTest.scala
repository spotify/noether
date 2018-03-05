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
 
package com.spotify.ml.aggregators

import org.scalactic.TolerantNumerics
import org.scalatest.{FlatSpec, Matchers}

class LogLossTest extends FlatSpec with Matchers {
  private implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(0.1)
  private val classes = 10
  private def s(idx: Int, score: Double): List[Double] =
    0.until(classes).map(i => if(i == idx) score else 0.0).toList

  it should "return correct scores" in {
      val data = List((s(0, 0.8), 0), (s(1, 0.6), 1), (s(2, 0.7), 2))
        .map{case(scores, label) => LogLossPrediction(scores, label)}

      assert(LogLossAggregator(data) === 0.363548039673)
    }
}