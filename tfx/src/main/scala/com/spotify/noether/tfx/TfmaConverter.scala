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

import scala.collection.JavaConverters._

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

case class EvalResult(metrics: Option[MetricsForSlice], plots: Option[Plot])
object EvalResult {
  def apply(metrics: MetricsForSlice): EvalResult = EvalResult(Some(metrics), None)
  def apply(metrics: MetricsForSlice, plot: Plot): EvalResult =
    EvalResult(Some(metrics), Some(plot))
  def apply(plot: Plot): EvalResult = EvalResult(None, Some(plot))
}

object TfmaConverter {

  implicit val errorRateSummaryConverter
    : TfmaConverter[Prediction[Int, List[Double]], (Double, Long), ErrorRateSummary.type] =
    new TfmaConverter[Prediction[Int, List[Double]], (Double, Long), ErrorRateSummary.type] {
      override def convertToTfmaProto(underlying: ErrorRateSummary.type)
        : Aggregator[Prediction[Int, List[Double]], (Double, Long), EvalResult] =
        ErrorRateSummary.andThenPresent { ers =>
          val metrics = buildDoubleMetric("Noether_ErrorRateSummary", ers)
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
                val plots = buildConfusionMatrixPlot(cm)
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
          val plots = buildConfusionMatrixPlot(cm)
          EvalResult(metrics, Plot.ConfusionMatrix(plots))
        })
    }

  implicit val classificationReportConverter
    : TfmaConverter[BinaryPred, Map[(Int, Int), Long], ClassificationReport] =
    new TfmaConverter[BinaryPred, Map[(Int, Int), Long], ClassificationReport] {
      override def convertToTfmaProto(underlying: ClassificationReport)
        : Aggregator[BinaryPred, Map[(Int, Int), Long], EvalResult] =
        underlying.andThenPresent { report =>
          val allMetrics = Map(
            "Noether_Accuracy" -> report.accuracy,
            "Noether_FPR" -> report.fpr,
            "Noether_FScore" -> report.fscore,
            "Noether_MCC" -> report.mcc,
            "Noether_Precision" -> report.precision,
            "Noether_Recall" -> report.recall
          )
          val metrics = buildDoubleMetrics(allMetrics)
          EvalResult(metrics)
        }
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
            val metrics = buildDoubleMetric(metricName, areaValue)
            EvalResult(metrics)
          }
    }

  implicit val logLossConverter
    : TfmaConverter[Prediction[Int, List[Double]], (Double, Long), LogLoss.type] =
    new TfmaConverter[Prediction[Int, List[Double]], (Double, Long), LogLoss.type] {
      override def convertToTfmaProto(underlying: LogLoss.type)
        : Aggregator[Prediction[Int, List[Double]], (Double, Long), EvalResult] =
        underlying.andThenPresent { logLoss =>
          val metrics = buildDoubleMetric("Noether_LogLoss", logLoss)
          EvalResult(metrics)
        }
    }

  implicit def meanAvgPrecisionConverter[T]
    : TfmaConverter[RankingPrediction[T], (Double, Long), MeanAveragePrecision[T]] =
    new TfmaConverter[RankingPrediction[T], (Double, Long), MeanAveragePrecision[T]] {
      override def convertToTfmaProto(underlying: MeanAveragePrecision[T])
        : Aggregator[RankingPrediction[T], (Double, Long), EvalResult] =
        underlying.andThenPresent { meanAvgPrecision =>
          val metrics = buildDoubleMetric("Noether_MeanAvgPrecision", meanAvgPrecision)
          EvalResult(metrics)
        }
    }

  implicit def ndcgAtKConverter[T]
    : TfmaConverter[RankingPrediction[T], (Double, Long), NdcgAtK[T]] =
    new TfmaConverter[RankingPrediction[T], (Double, Long), NdcgAtK[T]] {
      override def convertToTfmaProto(
        underlying: NdcgAtK[T]): Aggregator[RankingPrediction[T], (Double, Long), EvalResult] =
        underlying.andThenPresent { ndcgAtK =>
          val metrics = buildDoubleMetric("Noether_NdcgAtK", ndcgAtK)
          EvalResult(metrics)
        }
    }

  implicit def precisionAtK[T]
    : TfmaConverter[RankingPrediction[T], (Double, Long), PrecisionAtK[T]] =
    new TfmaConverter[RankingPrediction[T], (Double, Long), PrecisionAtK[T]] {
      override def convertToTfmaProto(
        underlying: PrecisionAtK[T]): Aggregator[RankingPrediction[T], (Double, Long), EvalResult] =
        underlying.andThenPresent { precisionAtK =>
          val metrics = buildDoubleMetric("Noether_PrecisionAtK", precisionAtK)
          EvalResult(metrics)
        }
    }

  implicit def calibrationHistogram: TfmaConverter[Prediction[Double, Double],
                                                   Map[Double, (Double, Double, Long)],
                                                   CalibrationHistogram] =
    new TfmaConverter[Prediction[Double, Double],
                      Map[Double, (Double, Double, Long)],
                      CalibrationHistogram] {
      override def convertToTfmaProto(underlying: CalibrationHistogram)
        : Aggregator[Prediction[Double, Double], Map[Double, (Double, Double, Long)], EvalResult] =
        underlying.andThenPresent { calibrationHistogram =>
          val plot = buildCalibrationHistogramPlot(calibrationHistogram)
          EvalResult(Plot.CalibrationHistogram(plot))
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

  private def buildDoubleMetric(name: String, value: Double): MetricsForSlice =
    MetricsForSlice
      .newBuilder()
      .setSliceKey(SliceKey.getDefaultInstance)
      .putMetrics(name,
                  MetricValue
                    .newBuilder()
                    .setDoubleValue(DoubleValue.newBuilder().setValue(value))
                    .build())
      .build()

  private def buildDoubleMetrics(metrics: Map[String, Double]): MetricsForSlice = {
    MetricsForSlice
      .newBuilder()
      .setSliceKey(SliceKey.getDefaultInstance)
      .putAllMetrics(metrics.mapValues { m =>
        MetricValue
          .newBuilder()
          .setDoubleValue(
            DoubleValue
              .newBuilder()
              .setValue(m))
          .build()
      }.asJava)
      .build()
  }

  private def buildConfusionMatrixPlot(cm: ConfusionMatrixAtThresholds): PlotsForSlice =
    PlotsForSlice
      .newBuilder()
      .setSliceKey(SliceKey.getDefaultInstance)
      .setPlotData(
        PlotData
          .newBuilder()
          .setConfusionMatrixAtThresholds(cm))
      .build()

  private def mkDoubleValue(d: Double): DoubleValue =
    DoubleValue
      .newBuilder()
      .setValue(d)
      .build()

  private def buildCalibrationHistogramPlot(ch: List[CalibrationHistogramBucket]): PlotsForSlice =
    PlotsForSlice
      .newBuilder()
      .setSliceKey(SliceKey.getDefaultInstance)
      .setPlotData(
        PlotData
          .newBuilder()
          .setCalibrationHistogramBuckets(CalibrationHistogramBuckets
            .newBuilder()
            .addAllBuckets(ch.map { b =>
              CalibrationHistogramBuckets.Bucket
                .newBuilder()
                .setLowerThresholdInclusive(b.lowerThresholdInclusive)
                .setUpperThresholdExclusive(b.upperThresholdExclusive)
                .setTotalWeightedRefinedPrediction(mkDoubleValue(b.sumPredictions))
                .setTotalWeightedLabel(mkDoubleValue(b.sumLabels))
                .setNumWeightedExamples(mkDoubleValue(b.numPredictions))
                .build()
            }.asJava))
          .build())
      .build()

}
