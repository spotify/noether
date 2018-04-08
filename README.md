noether
=======


[![Build Status](https://travis-ci.org/spotify/noether.svg?branch=master)](https://travis-ci.org/spotify/noether)
[![codecov.io](https://codecov.io/github/spotify/noether/coverage.svg?branch=master)](https://codecov.io/github/spotify/noether?branch=master)
[![GitHub license](https://img.shields.io/github/license/spotify/noether.svg)](./LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.spotify/noether-core_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.spotify/noether-core_2.12)

This library contains a set of monoids and aggregators for ML oriented tasks in Scala. The library builds upon the monoids and aggregators found in the [Algebird](https://github.com/twitter/algebird) library.

# Aggregators

Currently the library has metric driven aggregators which is broken down below.

## Metrics

These metrics are split between different types of predictions. The simple case is binary but some multi-class predictions are provided also.

### Binary

These Aggregators are meant for a single binary label and a score that is a valid probability.

1. Confusion Matrix
1. Precision Recall Curve
1. ROC Curve
1. AUC ROC
1. AUC PR

### Multi-class

These Aggregators are meant for a multi-class setup where the scores a distribution over labels and the label is an int representing the label.

1. Error Rate
1. Log Loss

# License

Copyright 2016-2018 Spotify AB.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
