# noether 

This library contains a set of monoids and aggregators for ML oriented tasks in Scala.  The library builds upon
the aggregators and monoids found in the Algebird Library.

# Aggregators

Currently the library has metric driven aggregators which is broken down below. 

## Metrics

These metrics are split between different types of predictions.  The simple case is binary
but some multi-class predictions are provided also.

### Binary

These Aggregators are meant for a single binary label and a score that is a valid probability.

1. Confusion Matrix
2. Precision Recall Curve
3. ROC Curve
4. AUC ROC
5. AUC PR

### Multi-class

These Aggregators are meant for a multi-class setup where the scores a distribtion over labels and
the label is an int representing the label.

1. Error Rate
2. Log Loss

# License

Copyright 2016-2018 Spotify AB.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0

