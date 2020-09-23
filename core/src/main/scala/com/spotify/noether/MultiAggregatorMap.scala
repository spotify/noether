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
import scala.collection.mutable.ArrayBuffer

/**
 * Aggregator which combines an unbounded list of other aggregators.
 * Each aggregator in the list is tagged by a string. The string(aka name) could be used to
 * retrieve the aggregated value from the Map emitted by the "present" function.
 */
case class MultiAggregatorMap[-A, B, +C](aggregatorsMap: List[(String, Aggregator[A, B, C])])
    extends Aggregator[A, List[B], Map[String, C]] {

  private val aggregators = aggregatorsMap.map(_._2)

  def prepare(input: A): List[B] =
    aggregators.map(_.prepare(input))

  def semigroup: Semigroup[List[B]] = new Semigroup[List[B]] {
    def plus(x: List[B], y: List[B]): List[B] = {
      var i = 0
      val resultList = new ArrayBuffer[B]
      while (i < aggregators.length) {
        resultList.append(aggregators(i).semigroup.plus(x(i), y(i)))
        i += 1
      }
      resultList.toList
    }
  }

  def present(reduction: List[B]): Map[String, C] = {
    var i = 0
    val resultList = new ArrayBuffer[(String, C)]
    val aggregatorsMapKeys = aggregatorsMap.map(_._1)
    while (i < aggregators.length) {
      resultList.append(aggregatorsMapKeys(i) -> aggregators(i).present(reduction(i)))
      i += 1
    }
    resultList.toMap
  }
}
