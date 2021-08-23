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

/**
 * Generic Prediction Object used by most aggregators
 *
 * @param actual
 *   Real value for this entry. Also normally seen as label.
 * @param predicted
 *   Predicted value. Can be a class or a score depending on the aggregator.
 * @tparam L
 *   Type of the Real Value
 * @tparam S
 *   Type of the Predicted Value
 */
final case class Prediction[L, S](actual: L, predicted: S) {
  override def toString: String = s"$actual,$predicted"
}
