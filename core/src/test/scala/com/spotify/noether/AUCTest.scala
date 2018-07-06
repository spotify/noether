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

class AUCTest extends AggregatorTest {
  private implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(0.1)

  private val data =
    List(
      (0.1, false),
      (0.1, true),
      (0.4, false),
      (0.6, false),
      (0.6, true),
      (0.6, true),
      (0.8, true)
    ).map { case (s, pred) => Prediction(pred, s) }

  it should "return ROC AUC" in {
    assert(run(AUC(ROC, samples = 50))(data) === 0.7)
  }

  it should "return PR AUC" in {
    assert(run(AUC(PR, samples = 50))(data) === 0.83)
  }

  it should "return points of a PR Curve" in {
    val expected = Array(
      (0.0, 1.0),
      (0.0, 1.0),
      (0.25, 1.0),
      (0.75, 0.75),
      (0.75, 0.6),
      (1.0, 0.5714285714285714)
    ).map { case (a, b) => MetricCurvePoint(a, b) }
    assert(run(Curve(PR, samples = 5))(data).points === MetricCurvePoints(expected).points)
  }

  it should "return points of a ROC Curve" in {
    val expected = Array(
      (0.0, 0.0),
      (0.0, 0.25),
      (0.3333333333333333, 0.75),
      (0.6666666666666666, 0.75),
      (1.0, 1.0),
      (1.0, 1.0)
    ).map { case (a, b) => MetricCurvePoint(a, b) }
    assert(run(Curve(ROC, samples = 5))(data).points === MetricCurvePoints(expected).points)
  }
}
