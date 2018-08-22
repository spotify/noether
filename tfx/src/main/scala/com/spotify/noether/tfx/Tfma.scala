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

import com.twitter.algebird.Aggregator
import tensorflow_model_analysis.MetricsForSliceOuterClass.MetricsForSlice

object Tfma {
  trait ConversionOps[A, B, T <: Aggregator[A, B, _]] {
    val self: T
    val converter: TfmaConverter[A, B, T]
    def asTfmaProto: Aggregator[A, B, EvalResult] = converter.convertToTfmaProto(self)
  }

  object ConversionOps {
    def apply[A, B, T <: Aggregator[A, B, _]](
      instance: T,
      tfmaConverter: TfmaConverter[A, B, T]): ConversionOps[A, B, T] =
      new ConversionOps[A, B, T] {
        override val self: T = instance
        override val converter: TfmaConverter[A, B, T] = tfmaConverter
      }
  }
}
