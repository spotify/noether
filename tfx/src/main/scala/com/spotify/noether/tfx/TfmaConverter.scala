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
  def convertToTfmaProto(underlying: T): Aggregator[A, B, EvalResult]
}

sealed trait Plot {
  val plotData: PlotsForSlice
}
object Plot {
  case class CalibrationHistogram(plotData: PlotsForSlice) extends Plot
  case class ConfusionMatrix(plotData: PlotsForSlice) extends Plot
}

case class EvalResult(metrics: MetricsForSlice, plots: Option[Plot])
object EvalResult {
  def apply(metrics: MetricsForSlice): EvalResult = EvalResult(metrics, None)
  def apply(metrics: MetricsForSlice, plot: Plot): EvalResult = EvalResult(metrics, Some(plot))
}

object TfmaConverter {

  implicit val errorRateSummaryConverter
    : TfmaConverter[Prediction[Int, List[Double]], (Double, Long), ErrorRateSummary.type] =
    new TfmaConverter[Prediction[Int, List[Double]], (Double, Long), ErrorRateSummary.type] {
      override def convertToTfmaProto(underlying: ErrorRateSummary.type)
        : Aggregator[Prediction[Int, List[Double]], (Double, Long), EvalResult] =
        ErrorRateSummary.andThenPresent { ers =>
          val metrics = MetricsForSlice
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
          EvalResult(metrics)
        }
    }

  implicit val binaryConfusionMatrixConverter
    : TfmaConverter[BinaryPred, Map[(Int, Int), Long], BinaryConfusionMatrix] =
    new TfmaConverter[BinaryPred, Map[(Int, Int), Long], BinaryConfusionMatrix] {
      override def convertToTfmaProto(underlying: BinaryConfusionMatrix)
        : Aggregator[BinaryPred, Map[(Int, Int), Long], EvalResult] = {
        underlying
          .andThenPresent(
            (denseMatrixToConfusionMatrix(Some(underlying.threshold)) _)
              .andThen { cm =>
                val metrics = confusionMatrixToMetric(cm)
                val plots = PlotsForSlice
                  .newBuilder()
                  .setSliceKey(SliceKey.getDefaultInstance)
                  .setPlotData(
                    PlotData
                      .newBuilder()
                      .setConfusionMatrixAtThresholds(cm))
                  .build()
                EvalResult(metrics, Plot.ConfusionMatrix(plots))
              })
      }

    }

  implicit val confusionMatrixConverter
    : TfmaConverter[Prediction[Int, Int], Map[(Int, Int), Long], ConfusionMatrix] =
    new TfmaConverter[Prediction[Int, Int], Map[(Int, Int), Long], ConfusionMatrix] {
      override def convertToTfmaProto(underlying: ConfusionMatrix)
        : Aggregator[Prediction[Int, Int], Map[(Int, Int), Long], EvalResult] =
        underlying.andThenPresent((denseMatrixToConfusionMatrix() _).andThen { cm =>
          val metrics = confusionMatrixToMetric(cm)
          val plots = PlotsForSlice
            .newBuilder()
            .setSliceKey(SliceKey.getDefaultInstance)
            .setPlotData(
              PlotData
                .newBuilder()
                .setConfusionMatrixAtThresholds(cm))
            .build()
          EvalResult(metrics, Plot.ConfusionMatrix(plots))
        })
    }

  implicit val aucConverter: TfmaConverter[BinaryPred, MetricCurve, AUC] =
    new TfmaConverter[BinaryPred, MetricCurve, AUC] {
      override def convertToTfmaProto(
        underlying: AUC): Aggregator[BinaryPred, MetricCurve, EvalResult] =
        underlying
          .andThenPresent { areaValue =>
            val metricName = underlying.metric match {
              case ROC => "Noether_AUC:ROC"
              case PR  => "Noether_AUC:PR"
            }
            val metrics = MetricsForSlice
              .newBuilder()
              .setSliceKey(SliceKey.getDefaultInstance)
              .putMetrics(metricName,
                          MetricValue
                            .newBuilder()
                            .setDoubleValue(DoubleValue.newBuilder().setValue(areaValue))
                            .build())
              .build()
            EvalResult(metrics)
          }
    }

  implicit val logLossConverter
    : TfmaConverter[Prediction[Int, List[Double]], (Double, Long), LogLoss.type] =
    new TfmaConverter[Prediction[Int, List[Double]], (Double, Long), LogLoss.type] {
      override def convertToTfmaProto(underlying: LogLoss.type)
        : Aggregator[Prediction[Int, List[Double]], (Double, Long), EvalResult] =
        underlying.andThenPresent { logLoss =>
          val metricName = "Noether_LogLoss"
          val metrics = MetricsForSlice
            .newBuilder()
            .setSliceKey(SliceKey.getDefaultInstance)
            .putMetrics(metricName,
                        MetricValue
                          .newBuilder()
                          .setDoubleValue(DoubleValue.newBuilder().setValue(logLoss))
                          .build())
            .build()
          EvalResult(metrics)
        }
    }

  implicit def meanAvgPrecisionConverter[T]
    : TfmaConverter[RankingPrediction[T], (Double, Long), MeanAveragePrecision[T]] =
    new TfmaConverter[RankingPrediction[T], (Double, Long), MeanAveragePrecision[T]] {
      override def convertToTfmaProto(underlying: MeanAveragePrecision[T])
        : Aggregator[RankingPrediction[T], (Double, Long), EvalResult] =
        underlying.andThenPresent { meanAvgPrecision =>
          val metricName = "Noether_MeanAvgPrecision"
          val metrics = MetricsForSlice
            .newBuilder()
            .setSliceKey(SliceKey.getDefaultInstance)
            .putMetrics(metricName,
                        MetricValue
                          .newBuilder()
                          .setDoubleValue(DoubleValue.newBuilder().setValue(meanAvgPrecision))
                          .build())
            .build()
          EvalResult(metrics)
        }
    }

  implicit def ndcgAtKConverter[T]
    : TfmaConverter[RankingPrediction[T], (Double, Long), NdcgAtK[T]] =
    new TfmaConverter[RankingPrediction[T], (Double, Long), NdcgAtK[T]] {
      override def convertToTfmaProto(
        underlying: NdcgAtK[T]): Aggregator[RankingPrediction[T], (Double, Long), EvalResult] =
        underlying.andThenPresent { ndcgAtK =>
          val metricName = "Noether_NdcgAtK"
          val metrics = MetricsForSlice
            .newBuilder()
            .setSliceKey(SliceKey.getDefaultInstance)
            .putMetrics(metricName,
                        MetricValue
                          .newBuilder()
                          .setDoubleValue(DoubleValue.newBuilder().setValue(ndcgAtK))
                          .build())
            .build()
          EvalResult(metrics)
        }
    }

  implicit def precisionAtK[T]
    : TfmaConverter[RankingPrediction[T], (Double, Long), PrecisionAtK[T]] =
    new TfmaConverter[RankingPrediction[T], (Double, Long), PrecisionAtK[T]] {
      override def convertToTfmaProto(
        underlying: PrecisionAtK[T]): Aggregator[RankingPrediction[T], (Double, Long), EvalResult] =
        underlying.andThenPresent { precisionAtK =>
          val metricName = "Noether_PrecisionAtK"
          val metrics = MetricsForSlice
            .newBuilder()
            .setSliceKey(SliceKey.getDefaultInstance)
            .putMetrics(metricName,
                        MetricValue
                          .newBuilder()
                          .setDoubleValue(DoubleValue.newBuilder().setValue(precisionAtK))
                          .build())
            .build()
          EvalResult(metrics)
        }
    }

  private def denseMatrixToMetric(threshold: Option[Double] = None)(
    matrix: DenseMatrix[Long]): MetricsForSlice = {
    val cm = denseMatrixToConfusionMatrix(threshold)(matrix)
    MetricsForSlice
      .newBuilder()
      .setSliceKey(SliceKey.getDefaultInstance)
      .putMetrics(
        "Noether_ConfusionMatrix",
        MetricValue
          .newBuilder()
          .setConfusionMatrixAtThresholds(cm)
          .build()
      )
      .build()
  }

  private def confusionMatrixToMetric(cm: ConfusionMatrixAtThresholds): MetricsForSlice = {
    MetricsForSlice
      .newBuilder()
      .setSliceKey(SliceKey.getDefaultInstance)
      .putMetrics(
        "Noether_ConfusionMatrix",
        MetricValue
          .newBuilder()
          .setConfusionMatrixAtThresholds(cm)
          .build()
      )
      .build()
  }

  private def denseMatrixToConfusionMatrix(threshold: Option[Double] = None)(
    matrix: DenseMatrix[Long]): ConfusionMatrixAtThresholds = {
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

    threshold.foreach(cmBuilder.setThreshold)
    ConfusionMatrixAtThresholds
      .newBuilder()
      .addMatrices(cmBuilder.build())
      .build()
  }
}
