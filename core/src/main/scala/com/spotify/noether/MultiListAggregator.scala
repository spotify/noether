/*
 * Copyright 2020 Spotify AB.
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
 * Aggregator which combines an unbounded list of other aggregators.
 * Each aggregator in the list is tagged by a string. The string(aka name) could be used to
 * retrieve the aggregated value from the Map emitted by the "present" function.
 */
object MultiListAggregator {

  def apply[A, B, C](
    aggregatorsMap: List[(String, Aggregator[A, B, C])]
  ): Aggregator[A, List[B], Map[String, C]] = {
    val aggregators = aggregatorsMap.collect { case (_, v) => v }

    new Aggregator[A, List[B], Map[String, C]] {
      def prepare(input: A): List[B] =
        aggregators.map(_.prepare(input))

      def semigroup: Semigroup[List[B]] = new Semigroup[List[B]] {
        def plus(x: List[B], y: List[B]): List[B] =
          x.zip(y).zip(aggregators).map {
            case ((a, b), agg) => agg.semigroup.plus(a, b)
          }
      }

      def present(reduction: List[B]): Map[String, C] = Map(
        aggregators
          .zip(reduction)
          .zip(aggregatorsMap.collect { case (k, _) => k })
          .map {
            case ((aggregator, reduction), aggKey) =>
              aggKey -> aggregator.present(reduction)
          }: _*
      )
    }
  }
}
