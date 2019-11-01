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

package com.spotify.noether.tfx

import com.spotify.noether._
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.{Assertion, FlatSpec, Matchers}
import tensorflow_model_analysis.MetricsForSliceOuterClass.MetricsForSlice
import tensorflow_model_analysis.MetricsForSliceOuterClass.CalibrationHistogramBuckets
import scala.collection.JavaConverters._

class TfmaConverterTest extends FlatSpec with Matchers {
  implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(0.001)

  "TfmaConverter" should "work with ConfusionMatrix" in {
    val data = List(
      (0, 0),
      (0, 1),
      (0, 0),
      (1, 0),
      (1, 1),
      (1, 1),
      (1, 1)
    ).map { case (s, pred) => Prediction(pred, s) }

    val evalResult = ConfusionMatrix(Seq(0, 1)).asTfmaProto(data)
    val cmProto = evalResult.metrics.get

    val cm = cmProto.getMetricsMap
      .get("Noether_ConfusionMatrix")
      .getConfusionMatrixAtThresholds
      .getMatrices(0)

    assert(cm.getFalseNegatives.toLong === 1L)
    assert(cm.getFalsePositives.toLong === 1L)
    assert(cm.getTrueNegatives.toLong === 2L)
    assert(cm.getTruePositives.toLong === 3L)

    val plotCm = evalResult.plots.get.plotData.getPlotData.getConfusionMatrixAtThresholds
      .getMatrices(0)

    assert(plotCm.getFalseNegatives.toLong === 1L)
    assert(plotCm.getFalsePositives.toLong === 1L)
    assert(plotCm.getTrueNegatives.toLong === 2L)
    assert(plotCm.getTruePositives.toLong === 3L)
  }

  it should "work with BinaryConfusionMatrix" in {
    val data = List(
      (false, 0.1),
      (false, 0.6),
      (false, 0.2),
      (true, 0.2),
      (true, 0.8),
      (true, 0.7),
      (true, 0.6)
    ).map { case (pred, s) => Prediction(pred, s) }

    val evalResult = BinaryConfusionMatrix().asTfmaProto(data)
    val cmProto = evalResult.metrics.get

    val cm = cmProto.getMetricsMap
      .get("Noether_ConfusionMatrix")
      .getConfusionMatrixAtThresholds
      .getMatrices(0)

    assert(cm.getThreshold === 0.5)
    assert(cm.getTruePositives.toLong === 3L)
    assert(cm.getFalseNegatives.toLong === 1L)
    assert(cm.getFalsePositives.toLong === 1L)
    assert(cm.getTrueNegatives.toLong === 2L)

    val plotCm = evalResult.plots.get.plotData.getPlotData.getConfusionMatrixAtThresholds
      .getMatrices(0)

    assert(plotCm.getFalseNegatives.toLong === 1L)
    assert(plotCm.getFalsePositives.toLong === 1L)
    assert(plotCm.getTrueNegatives.toLong === 2L)
    assert(plotCm.getTruePositives.toLong === 3L)
  }

  it should "work with ClassificationReport" in {
    val data = List(
      (0.1, false),
      (0.1, true),
      (0.4, false),
      (0.6, false),
      (0.6, true),
      (0.6, true),
      (0.8, true)
    ).map { case (s, pred) => Prediction(pred, s) }

    val metrics = ClassificationReport().asTfmaProto(data).metrics

    def assertMetric(name: String, expected: Double): Assertion =
      assert(metrics.get.getMetricsMap.get(name).getDoubleValue.getValue === expected)

    assertMetric("Noether_Accuracy", 0.7142857142857143)
    assertMetric("Noether_FPR", 0.333)
    assertMetric("Noether_FScore", 0.75)
    assertMetric("Noether_MCC", 0.4166666666666667)
    assertMetric("Noether_Precision", 0.75)
    assertMetric("Noether_Recall", 0.75)
  }

  it should "work with ErrorRateSummary" in {
    val classes = 10
    def s(idx: Int): List[Double] = 0.until(classes).map(i => if (i == idx) 1.0 else 0.0).toList

    val data =
      List((s(1), 1), (s(3), 1), (s(5), 5), (s(2), 3), (s(0), 0), (s(8), 1)).map {
        case (scores, label) => Prediction(label, scores)
      }

    val ersProto: MetricsForSlice = ErrorRateSummary.asTfmaProto(data).metrics.get

    val ersV =
      ersProto.getMetricsMap.get("Noether_ErrorRateSummary").getDoubleValue.getValue
    assert(ersV === 0.5)
  }

  it should "work with AUC" in {
    val data = List(
      (false, 0.1),
      (false, 0.6),
      (false, 0.2),
      (true, 0.2),
      (true, 0.8),
      (true, 0.7),
      (true, 0.6)
    ).map { case (pred, s) => Prediction(pred, s) }

    val aucROCProto = AUC(ROC).asTfmaProto(data).metrics.get
    val aucPRProto = AUC(PR).asTfmaProto(data).metrics.get

    val actualROC = aucROCProto.getMetricsMap.get("Noether_AUC:ROC").getDoubleValue.getValue
    val actualPR = aucPRProto.getMetricsMap.get("Noether_AUC:PR").getDoubleValue.getValue

    assert(actualROC === 0.833)
    assert(actualPR === 0.896)
  }

  it should "work with LogLoss" in {
    val classes = 10

    def s(idx: Int, score: Double): List[Double] =
      0.until(classes).map(i => if (i == idx) score else 0.0).toList

    val data = List((s(0, 0.8), 0), (s(1, 0.6), 1), (s(2, 0.7), 2)).map {
      case (scores, label) => Prediction(label, scores)
    }

    val logLossProto: MetricsForSlice = LogLoss.asTfmaProto(data).metrics.get

    val logLoss = logLossProto.getMetricsMap.get("Noether_LogLoss").getDoubleValue.getValue
    assert(logLoss === 0.363548039673)
  }

  it should "work with MeanAveragePrecision" in {
    import RankingData._
    val proto = MeanAveragePrecision[Int]().asTfmaProto(rankingData).metrics.get
    val meanAvgPrecision = proto.getMetricsMap
      .get("Noether_MeanAvgPrecision")
      .getDoubleValue
      .getValue
    assert(meanAvgPrecision === 0.355026)
  }

  it should "work with NdcgAtK" in {
    import RankingData._
    implicit val doubleEq: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.1)

    def getNdcgAtK(v: Int): Double =
      NdcgAtK[Int](v)
        .asTfmaProto(rankingData)
        .metrics
        .get
        .getMetricsMap
        .get("Noether_NdcgAtK")
        .getDoubleValue
        .getValue

    assert(getNdcgAtK(3) === 1.0 / 3)
    assert(getNdcgAtK(5) === 0.328788)
    assert(getNdcgAtK(10) === 0.487913)
    assert(getNdcgAtK(15) === getNdcgAtK(10))
  }

  it should "work with PrecisionAtK" in {
    import RankingData._
    implicit val doubleEq: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.1)

    def getPrecisionAtK(v: Int): Double =
      PrecisionAtK[Int](v)
        .asTfmaProto(rankingData)
        .metrics
        .get
        .getMetricsMap
        .get("Noether_PrecisionAtK")
        .getDoubleValue
        .getValue

    assert(getPrecisionAtK(1) === 1.0 / 3)
    assert(getPrecisionAtK(2) === 1.0 / 3)
    assert(getPrecisionAtK(3) === 1.0 / 3)
    assert(getPrecisionAtK(4) === 0.75 / 3)
    assert(getPrecisionAtK(5) === 0.8 / 3)
    assert(getPrecisionAtK(10) === 0.8 / 3)
    assert(getPrecisionAtK(15) === 8.0 / 45)
  }

  it should "work with CalibrationHistogram" in {
    val data = Seq(
      (0.15, 1.15), // lb
      (0.288, 1.288), // rounding error puts this in (0.249, 0.288)
      (0.30, 1.30), // (0.288, 0.3269)
      (0.36, 1.36), // (0.3269, 0.365)
      (0.555, 1.555), // (0.5219, 0.5609)
      (1.2, 2.2), // ub
      (0.7, 1.7) // ub
    ).map { case (p, a) => Prediction(a, p) }

    val result = CalibrationHistogram(0.21, 0.60, 10).asTfmaProto(data)

    def protoToCaseClass(p: CalibrationHistogramBuckets.Bucket): CalibrationHistogramBucket = {
      CalibrationHistogramBucket(
        p.getLowerThresholdInclusive,
        p.getUpperThresholdExclusive,
        p.getNumWeightedExamples.getValue,
        p.getTotalWeightedLabel.getValue,
        p.getTotalWeightedRefinedPrediction.getValue
      )
    }

    val actual =
      result.plots.get.plotData.getPlotData.getCalibrationHistogramBuckets.getBucketsList.asScala
        .map(protoToCaseClass)

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
