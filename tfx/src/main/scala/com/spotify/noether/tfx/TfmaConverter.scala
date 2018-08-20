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

import breeze.linalg.DenseMatrix
import com.google.protobuf.DoubleValue
import com.spotify.noether.{ErrorRateSummary, _}
import com.twitter.algebird.Aggregator
import tensorflow_model_analysis.MetricsForSliceOuterClass.ConfusionMatrixAtThresholds._
import tensorflow_model_analysis.MetricsForSliceOuterClass._

trait TfmaConverter[A, B, T <: Aggregator[A, B, _]] {
  def convertToTfmaProto(underlying: T): Aggregator[A, B, MetricsForSlice]
}

object TfmaConverter {

  private def denseMatrixToMetric(threshold: Option[Double] = None)(
    matrix: DenseMatrix[Long]): MetricsForSlice = {
    val tp = matrix.valueAt(1, 1).toDouble
    val tn = matrix.valueAt(0, 0).toDouble
    val fp = matrix.valueAt(1, 0).toDouble
    val fn = matrix.valueAt(0, 1).toDouble

    val cmBuilder = ConfusionMatrixAtThreshold
      .newBuilder()
      .setFalseNegatives(fn)
      .setFalsePositives(fp)
      .setTrueNegatives(tn)
      .setTruePositives(tp)
      .setPrecision(tp / (tp + fp))
      .setRecall(tp / (tp + fn))

    threshold.foreach(cmBuilder.setThreshold(_))

    MetricsForSlice
      .newBuilder()
      .setSliceKey(SliceKey.getDefaultInstance)
      .putMetrics(
        "Noether_ConfusionMatrix",
        MetricValue
          .newBuilder()
          .setConfusionMatrixAtThresholds(
            ConfusionMatrixAtThresholds
              .newBuilder()
              .addMatrices(cmBuilder.build())
              .build())
          .build()
      )
      .build()
  }

  implicit val errorRateSummaryConverter
    : TfmaConverter[Prediction[Int, List[Double]], (Double, Long), ErrorRateSummary.type] =
    new TfmaConverter[Prediction[Int, List[Double]], (Double, Long), ErrorRateSummary.type] {
      override def convertToTfmaProto(underlying: ErrorRateSummary.type)
        : Aggregator[Prediction[Int, List[Double]], (Double, Long), MetricsForSlice] =
        ErrorRateSummary.andThenPresent { ers =>
          MetricsForSlice
            .newBuilder()
            .setSliceKey(SliceKey.getDefaultInstance)
            .putMetrics("Noether_ErrorRateSummary",
                        MetricValue
                          .newBuilder()
                          .setDoubleValue(
                            DoubleValue
                              .newBuilder()
                              .setValue(ers)
                              .build())
                          .build())
            .build()
        }
    }

  implicit val binaryConfusionMatrixConverter
    : TfmaConverter[BinaryPred, Map[(Int, Int), Long], BinaryConfusionMatrix] =
    new TfmaConverter[BinaryPred, Map[(Int, Int), Long], BinaryConfusionMatrix] {
      override def convertToTfmaProto(underlying: BinaryConfusionMatrix)
        : Aggregator[BinaryPred, Map[(Int, Int), Long], MetricsForSlice] =
        underlying
          .andThenPresent(denseMatrixToMetric(Some(underlying.threshold)))
    }

  implicit val confusionMatrixConverter
    : TfmaConverter[Prediction[Int, Int], Map[(Int, Int), Long], ConfusionMatrix] =
    new TfmaConverter[Prediction[Int, Int], Map[(Int, Int), Long], ConfusionMatrix] {
      override def convertToTfmaProto(underlying: ConfusionMatrix)
        : Aggregator[Prediction[Int, Int], Map[(Int, Int), Long], MetricsForSlice] =
        underlying.andThenPresent(denseMatrixToMetric())
    }

  implicit val aucConverter: TfmaConverter[BinaryPred, MetricCurve, AUC] =
    new TfmaConverter[BinaryPred, MetricCurve, AUC] {
      override def convertToTfmaProto(
        underlying: AUC): Aggregator[BinaryPred, MetricCurve, MetricsForSlice] =
        underlying
          .andThenPresent { areaValue =>
            val metricName = underlying.metric match {
              case ROC => "Noether_AUC:ROC"
              case PR  => "Noether_AUC:PR"
            }
            MetricsForSlice
              .newBuilder()
              .setSliceKey(SliceKey.getDefaultInstance)
              .putMetrics(metricName,
                          MetricValue
                            .newBuilder()
                            .setDoubleValue(DoubleValue.newBuilder().setValue(areaValue))
                            .build())
              .build()
          }
    }
}
