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

import com.twitter.algebird.MultiAggregator

object AggregatorExample {
  def main(args: Array[String]): Unit = {
    val multiAggregator =
      MultiAggregator((AUC(ROC), AUC(PR), ClassificationReport(), BinaryConfusionMatrix()))
        .andThenPresent { case (roc, pr, report, cm) =>
          (roc, pr, report.accuracy, report.recall, report.precision, cm(1, 1), cm(0, 0))
        }

    val predictions = List(Prediction(false, 0.1), Prediction(false, 0.6), Prediction(true, 0.9))

    multiAggregator.apply(predictions)
    ()
  }
}
