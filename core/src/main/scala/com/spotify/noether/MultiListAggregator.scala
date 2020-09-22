package com.spotify.noether

import com.twitter.algebird.{Aggregator, Semigroup}

/**
 * Aggregator which combines an unbounded list of other aggregators.
 * Each aggregator in the list is tagged by a string. The string(aka name) could be used to
 * retrieve the aggregated value from the Map emitted by the "present" function.
 */
object MultiListAggregator {

  def apply[A, B, C](aggregatorsMap: List[(String, Aggregator[A, B, C])])
  : Aggregator[A, List[B], Map[String, C]] = {
    val aggregators = aggregatorsMap.collect { case (_ , v) => v }

    new Aggregator[A, List[B], Map[String, C]] {
      def prepare(input: A): List[B] =
        aggregators.map(_.prepare(input))

      def semigroup: Semigroup[List[B]] = new Semigroup[List[B]] {
        def plus(x: List[B], y: List[B]): List[B] =
          x.zip(y).zip(aggregators).map {
            case ((a, b), agg) => agg.semigroup.plus(a, b)
          }
      }

      def present(reduction: List[B]): Map[String, C] = Map(aggregators
        .zip(reduction)
        .zip(aggregatorsMap.collect { case (k, _) => k })
        .map {
          case ((aggregator, reduction), aggKey) =>
            aggKey -> aggregator.present(reduction)
        }: _*)
    }
  }
}
