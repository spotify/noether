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
import com.twitter.algebird.{Aggregator, MultiAggregator}
import tensorflow_model_analysis.MetricsForSliceOuterClass.ConfusionMatrixAtThresholds._
import tensorflow_model_analysis.MetricsForSliceOuterClass._

trait TfmaConverter[A, B, T <: Aggregator[A, B, _]] {
  def convertToTfmaProto(underlying: T): Aggregator[A, B, MetricsForSlice]

  def compose[B_, T_ <: Aggregator[A, B_, _]](
    implicit c: TfmaConverter[A, B_, T_]): TfmaConverter[A, (B, B_), (T, T_)] = {
    val outer = this
    new TfmaConverter[A, (B, B_), (T, T_)] {
      override def convertToTfmaProto(
        underlying: (T, T_)): Aggregator[A, (B, B_), MetricsForSlice] = underlying match {
        case (t1, t2) =>
          val a1 = outer.convertToTfmaProto(t1)
          val a2 = c.convertToTfmaProto(t2)
          val joined = a1.join(a2)
          joined.andThenPresent {
            case (proto1, proto2) =>
              val allMetrics = proto1.getMetricsMap
              allMetrics.putAll(proto2.getMetricsMap)
              MetricsForSlice
                .newBuilder()
                .setSliceKey(SliceKey.getDefaultInstance)
                .putAllMetrics(allMetrics)
                .build()
          }
      }
    }
  }

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

  implicit val logLossConverter
    : TfmaConverter[Prediction[Int, List[Double]], (Double, Long), LogLoss.type] =
    new TfmaConverter[Prediction[Int, List[Double]], (Double, Long), LogLoss.type] {
      override def convertToTfmaProto(underlying: LogLoss.type)
        : Aggregator[Prediction[Int, List[Double]], (Double, Long), MetricsForSlice] =
        underlying.andThenPresent { logLoss =>
          val metricName = "Noether_LogLoss"
          MetricsForSlice
            .newBuilder()
            .setSliceKey(SliceKey.getDefaultInstance)
            .putMetrics(metricName,
                        MetricValue
                          .newBuilder()
                          .setDoubleValue(DoubleValue.newBuilder().setValue(logLoss))
                          .build())
            .build()
        }
    }

  implicit def meanAvgPrecisionConverter[T]
    : TfmaConverter[RankingPrediction[T], (Double, Long), MeanAveragePrecision[T]] =
    new TfmaConverter[RankingPrediction[T], (Double, Long), MeanAveragePrecision[T]] {
      override def convertToTfmaProto(underlying: MeanAveragePrecision[T])
        : Aggregator[RankingPrediction[T], (Double, Long), MetricsForSlice] =
        underlying.andThenPresent { meanAvgPrecision =>
          val metricName = "Noether_MeanAvgPrecision"
          MetricsForSlice
            .newBuilder()
            .setSliceKey(SliceKey.getDefaultInstance)
            .putMetrics(metricName,
                        MetricValue
                          .newBuilder()
                          .setDoubleValue(DoubleValue.newBuilder().setValue(meanAvgPrecision))
                          .build())
            .build()
        }
    }

  implicit def multiAggConverter[A,
                                 B1,
                                 B2,
                                 C1,
                                 C2,
                                 T1 <: Aggregator[A, B1, C1],
                                 T2 <: Aggregator[A, B2, C2]](
    implicit e1: TfmaConverter[A, B1, T1],
    e2: TfmaConverter[A, B2, T2]): TfmaConverter[A, (B1, B2), (T1, T2)] = e1.compose(e2)
}
