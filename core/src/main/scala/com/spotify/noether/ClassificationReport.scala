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
import com.twitter.algebird.{Aggregator, Semigroup}

/**
  * Classification Report
  *
  * @param mcc <a href="https://bit.ly/2Jw7vL3"> Matthews Correlation Coefficient </a>
  * @param fscore <a href="https://en.wikipedia.org/wiki/F1_score"> f-score </a>
  * @param precision <a href="https://en.wikipedia.org/wiki/Precision_and_recall"> Precision </a>
  * @param recall <a href="https://en.wikipedia.org/wiki/Precision_and_recall"> Recall </a>
  * @param accuracy <a href="https://en.wikipedia.org/wiki/Accuracy_and_precision"> Accuracy </a>
  * @param fpr <a href="https://en.wikipedia.org/wiki/False_positive_rate"> False Positive Rate </a>
  */
final case class Report(mcc: Double,
                        fscore: Double,
                        precision: Double,
                        recall: Double,
                        accuracy: Double,
                        fpr: Double)

/**
  * Generate a Classification Report for a collection of binary predictions.
  * The output of this aggregator will be a [[Report]] object.
  *
  * @param threshold Threshold to apply to get the predictions.
  * @param beta Beta parameter used in the f-score calculation.
  */
final case class ClassificationReport(threshold: Double = 0.5,
                                      beta: Double = 1.0)
    extends Aggregator[Prediction[Boolean, Double],
                       Map[(Int, Int), Long],
                       Report] {

  private val aggregator = MultiClassificationReport(Seq(0, 1))

  def prepare(input: Prediction[Boolean, Double]): Map[(Int, Int), Long] = {
    val predicted = Prediction(
      if (input.actual) 1 else 0,
      if (input.predicted > threshold) 1 else 0
    )
    aggregator.prepare(predicted)
  }

  def semigroup: Semigroup[Map[(Int, Int), Long]] = aggregator.semigroup

  def present(m: Map[(Int, Int), Long]): Report = aggregator.present(m)(1)
}

/**
  * Generate a Classification Report for a collection of multiclass predictions. A report is
  * generated for each class by treating the predictions as binary of either "class" or "not class".
  * The output of this aggregator will be a map of classes and their [[Report]] objects.
  *
  * @param labels List of possible label values.
  * @param beta Beta parameter used in the f-score calculation.
  */
final case class MultiClassificationReport(labels: Seq[Int], beta: Double = 1.0)
    extends Aggregator[Prediction[Int, Int],
                       Map[(Int, Int), Long],
                       Map[Int, Report]] {

  private val aggregator = ConfusionMatrix(labels)

  override def prepare(input: Prediction[Int, Int]): Map[(Int, Int), Long] =
    aggregator.prepare(input)

  override def semigroup: Semigroup[Map[(Int, Int), Long]] =
    aggregator.semigroup

  //scalastyle:off cyclomatic.complexity
  override def present(m: Map[(Int, Int), Long]): Map[Int, Report] = {
    val mat = m.withDefaultValue(0L)
    labels.foldLeft(Map.empty[Int, Report]) { (result, clazz) =>
      val fp = mat
        .filterKeys { case (p, a) => p == clazz && a != clazz }
        .values
        .sum
        .toDouble
      val tp = mat(clazz -> clazz).toDouble
      val tn = mat
        .filterKeys { case (p, a) => p != clazz && a != clazz }
        .values
        .sum
        .toDouble
      val fn = mat
        .filterKeys { case (p, a) => p != clazz && a == clazz }
        .values
        .sum
        .toDouble

      val mccDenom = math.sqrt((tp + fp) * (tp + fn) * (tn + fp) * (tn + fn))
      val mcc = if (mccDenom > 0.0) ((tp * tn) - (fp * fn)) / mccDenom else 0.0

      val precDenom = tp + fp
      val precision = if (precDenom > 0.0) tp / precDenom else 1.0

      val recallDenom = tp + fn
      val recall = if (recallDenom > 0.0) tp / recallDenom else 1.0

      val accuracyDenom = tp + fn + tn + fp
      val accuracy = if (accuracyDenom > 0.0) (tp + tn) / accuracyDenom else 0.0

      val fpDenom = fp + tn
      val fpr = if (fpDenom > 0.0) fp / fpDenom else 0.0

      val betaSqr = Math.pow(beta, 2.0)

      val fScoreDenom = (betaSqr * precision) + recall
      val fscore = if (fScoreDenom > 0.0) {
        (1 + betaSqr) * ((precision * recall) / fScoreDenom)
      } else { 1.0 }

      result + (clazz -> Report(mcc, fscore, precision, recall, accuracy, fpr))
    }
  }
  //scalastyle:on cyclomatic.complexity
}
