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

import org.scalactic.{Equality, TolerantNumerics}

class CalibrationHistogramTest extends AggregatorTest {
  it should "return correct histogram" in {
    implicit val doubleEq: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.001)
    val data = Seq(
      (0.15, 1.15), // lb
      (0.288, 1.288), // rounding error puts this in (0.249, 0.288)
      (0.30, 1.30), // (0.288, 0.3269)
      (0.36, 1.36), // (0.3269, 0.365)
      (0.555, 1.555), // (0.5219, 0.5609)
      (1.2, 2.2), // ub
      (0.7, 1.7) // ub
    ).map { case (p, a) => Prediction(a, p) }

    val actual = run(CalibrationHistogram(0.21, 0.60, 10))(data)

    val expected = List(
      CalibrationHistogramBucket(Double.NegativeInfinity, 0.21, 1.0, 1.15, 0.15),
      CalibrationHistogramBucket(0.21, 0.249, 0.0, 0.0, 0.0),
      CalibrationHistogramBucket(0.249, 0.288, 1.0, 1.288, 0.288),
      CalibrationHistogramBucket(0.288, 0.327, 1.0, 1.30, 0.30),
      CalibrationHistogramBucket(0.327, 0.366, 1.0, 1.36, 0.36),
      CalibrationHistogramBucket(0.366, 0.405, 0.0, 0.0, 0.0),
      CalibrationHistogramBucket(0.405, 0.4449, 0.0, 0.0, 0.0),
      CalibrationHistogramBucket(0.444, 0.483, 0.0, 0.0, 0.0),
      CalibrationHistogramBucket(0.483, 0.522, 0.0, 0.0, 0.0),
      CalibrationHistogramBucket(0.522, 0.561, 1.0, 1.555, 0.555),
      CalibrationHistogramBucket(0.561, 0.6, 0.0, 0.0, 0.0),
      CalibrationHistogramBucket(0.6, Double.PositiveInfinity, 2.0, 3.9, 1.9)
    )

    assert(actual.length == expected.length)
    (0 until expected.length).foreach { i =>
      assert(actual(i).numPredictions === expected(i).numPredictions)
      assert(actual(i).sumPredictions === expected(i).sumPredictions)
      assert(actual(i).sumLabels === expected(i).sumLabels)
      assert(actual(i).lowerThresholdInclusive === expected(i).lowerThresholdInclusive)
      assert(actual(i).upperThresholdExclusive === expected(i).upperThresholdExclusive)
    }
  }
}
