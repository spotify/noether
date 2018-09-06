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
package benchmark

import com.spotify.noether.benchmark.CalibrationHistogramCreateBenchmark.CalibrationHistogramState
import org.openjdk.jmh.annotations._

import scala.util.Random

object PredictionUtils {

  def generatePredictions(nbPrediction: Int): Seq[Prediction[Boolean, Double]] =
    Seq.fill(nbPrediction)(Prediction(Random.nextBoolean(), Random.nextDouble()))
}

object CalibrationHistogramCreateBenchmark {

  @State(Scope.Benchmark)
  class CalibrationHistogramState() {

    @Param(Array("100", "1000", "3000"))
    var nbElement = 0

    @Param(Array("100", "200", "300"))
    var nbBucket = 0

    @Param(Array("0.1", "0.2", "0.3"))
    var lowerBound = 0.0

    @Param(Array("0.2", "0.4", "0.5"))
    var upperBound = 0.0

    var histogram: CalibrationHistogram = _

    @Setup
    def setup(): Unit =
      histogram = CalibrationHistogram(lowerBound, upperBound, nbBucket)
  }

}

class CalibrationHistogramCreateBenchmark {

  @Benchmark
  def createCalibrationHistogram(calibrationHistogramState: CalibrationHistogramState): Double =
    calibrationHistogramState.histogram.bucketSize
}
