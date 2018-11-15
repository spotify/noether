Noether
=======

[![Build Status](https://travis-ci.org/spotify/noether.svg?branch=master)](https://travis-ci.org/spotify/noether)
[![codecov.io](https://codecov.io/github/spotify/noether/coverage.svg?branch=master)](https://codecov.io/github/spotify/noether?branch=master)
[![GitHub license](https://img.shields.io/github/license/spotify/noether.svg)](./LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.spotify/noether-core_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.spotify/noether-core_2.12)
[![Scaladoc](https://img.shields.io/badge/scaladoc-latest-blue.svg)](https://spotify.github.io/noether/latest/api/com/spotify/noether/index.html)

> [Emmy Noether](https://en.wikipedia.org/wiki/Emmy_Noether) was a German mathematician known for her landmark contributions to abstract algebra and theoretical physics. 

Noether is a collection of Machine Learning tools targeted at the JVM and Scala.
It relies heavily on the [Algebird](https://github.com/twitter/algebird) library especially for Aggregators.

# Aggregators

Aggregators enable creation of reusable and composable aggregation functions. Most Machine Learning loss functions and metrics can be 
decomposed into a single aggregator.  This becomes useful when a model produces a set of predictions and one or more metrics are needed
to be computed on this collection.

Below is an example for a binary classification task. Algebird's MultiAggregator can be used to combine multiple metrics into a 
single callable aggregator.

```scala
val multiAggregator =
  MultiAggregator(AUC(ROC), AUC(PR), ClassificationReport(), BinaryConfusionMatrix())
    .andThenPresent{case (roc, pr, report, cm) =>
      (roc, pr, report.accuracy, report.recall, report.precision, cm(1, 1), cm(0, 0))
    }

val predictions = List(Prediction(false, 0.1), Prediction(false, 0.6), Prediction(true, 0.9))

println(multiAggregator(predictions))
```

## Prediction Object

Most aggregators take a single parameterized class called Prediction as input to the aggregator. However the type of
the prediction object differ based on the aggregator. In the above example each binary classifier takes a prediction 
of type `Prediction[Boolean, Double]` where the first type is the label and the second in the predicted score.

Other aggregators will takes slightly different types such as the Error Rate Aggregator which expects `Prediction[Int, List[Double]]`
where the types are label and a list of scores.

## Available Aggregators

See the docs on each aggregator for a more detailed walk-through on the functionality and the return objects.

1. ConfusionMatrix
    1.  Includes a special BinaryConfusionMatrix case to make composition easier with the other binary classification metrics.
2. AUC
    1. Supports both ROC and PR
3. ClassificationReport
    1. Returns a list of summary metrics for a binary classification problem.
4. LogLoss
    1. Available for multiclass. Returns the total log loss for the predictions.
5. ErrorRateSummary
    1. Available for multiclass. Returns the proportion of misclassified predictions.w      

# Tensorflow Model Analysis Support 

Noether supports outputting metrics as TFX `metrics_for_slice` protobufs, which can be used in 
TFMA methods. This is available in the `noether-tfx` package:

```scala
libraryDependencies += "com.spotify" %% "noether-tfx" % noetherVersion
```

```scala
import com.spotify.noether.tfx._

val data = List(
  (0, 0),
  (0, 1),
  (0, 0),
  (1, 0),
  (1, 1),
  (1, 1),
  (1, 1)
).map { case (s, pred) => Prediction(pred, s) }

val tfmaProto = ConfusionMatrix(Seq(0, 1)).asTfmaProto(data)
```

# License

Copyright 2016-2018 Spotify AB.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
