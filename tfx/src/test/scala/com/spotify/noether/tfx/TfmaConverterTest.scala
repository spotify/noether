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
import org.scalatest.{FlatSpec, Matchers}
import tensorflow_model_analysis.MetricsForSliceOuterClass.MetricsForSlice

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

    val cmProto: MetricsForSlice = ConfusionMatrix(Seq(0, 1)).asTfmaProto(data)

    val cm = cmProto
      .getMetricsMap()
      .get("Noether_ConfusionMatrix")
      .getConfusionMatrixAtThresholds
      .getMatrices(0)

    assert(cm.getFalseNegatives.toLong === 1L)
    assert(cm.getFalsePositives.toLong === 1L)
    assert(cm.getTrueNegatives.toLong === 2L)
    assert(cm.getTruePositives.toLong === 3L)
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

    val cmProto: MetricsForSlice = BinaryConfusionMatrix().asTfmaProto(data)

    val cm = cmProto
      .getMetricsMap()
      .get("Noether_ConfusionMatrix")
      .getConfusionMatrixAtThresholds
      .getMatrices(0)

    assert(cm.getThreshold === 0.5)
    assert(cm.getTruePositives.toLong === 3L)
    assert(cm.getFalseNegatives.toLong === 1L)
    assert(cm.getFalsePositives.toLong === 1L)
    assert(cm.getTrueNegatives.toLong === 2L)
  }

  it should "work with ErrorRateSummary" in {
    val classes = 10
    def s(idx: Int): List[Double] = 0.until(classes).map(i => if (i == idx) 1.0 else 0.0).toList

    val data =
      List((s(1), 1), (s(3), 1), (s(5), 5), (s(2), 3), (s(0), 0), (s(8), 1)).map {
        case (scores, label) => Prediction(label, scores)
      }

    val ersProto: MetricsForSlice = ErrorRateSummary.asTfmaProto(data)

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

    val aucROCProto = AUC(ROC).asTfmaProto(data)
    val aucPRProto = AUC(PR).asTfmaProto(data)

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

    val logLossProto: MetricsForSlice = LogLoss.asTfmaProto(data)

    val logLoss = logLossProto.getMetricsMap.get("Noether_LogLoss").getDoubleValue.getValue
    assert(logLoss === 0.363548039673)
  }

  it should "work with MeanAveragePrecision" in {
    import RankingData._
    val proto = MeanAveragePrecision[Int]().asTfmaProto(rankingData)
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
}
