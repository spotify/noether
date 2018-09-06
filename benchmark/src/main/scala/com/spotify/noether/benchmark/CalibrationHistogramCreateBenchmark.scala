package com.spotify.noether
package benchmark

import com.spotify.noether.benchmark.CalibrationHistogramCreateBenchmark.CalibrationHistogramState

import scala.util.Random
import org.openjdk.jmh.annotations._


object PredictionUtils {

  def generatePredictions(nbPrediction : Int): Seq[Prediction[Boolean, Double]] ={
    Seq.fill(nbPrediction)(Prediction(Random.nextBoolean(), Random.nextInt(99).toFloat/100))
  }
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

    var histogram : CalibrationHistogram = _

    @Setup
    def setup(): Unit = {
      histogram = CalibrationHistogram(lowerBound, upperBound, nbBucket)
    }
  }
}

class CalibrationHistogramCreateBenchmark {

  @Benchmark
  def createCalibrationHistogram(calibrationHistogramState: CalibrationHistogramState): Double = {
    calibrationHistogramState.histogram.bucketSize
  }
}
