package com.spotify.noether

import com.twitter.algebird.MultiAggregator

object AggregatorExample {
  def main(args: Array[String]): Unit = {
    val multiAggregator =
      MultiAggregator(AUC(ROC), AUC(PR), ClassificationReport(), BinaryConfusionMatrix())
        .andThenPresent{case (roc, pr, report, cm) =>
          (roc, pr, report.accuracy, report.recall, report.precision, cm(1, 1), cm(0, 0))
        }

    val predictions = List(Prediction(false, 0.1), Prediction(false, 0.6), Prediction(true, 0.9))

    // scalastyle:off regex
    println(multiAggregator.apply(predictions))
    // scalastyle:on regex
  }
}
