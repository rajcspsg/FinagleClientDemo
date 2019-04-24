package com.twitter.finagle

import java.net.InetAddress
import java.util.concurrent.Executors
import com.github.benmanes.caffeine.cache.LoadingCache
import com.twitter.cache.caffeine.CaffeineCache
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.finagle.FixedInetResolver.cache
import com.twitter.finagle.stats.{DefaultStatsReceiver, StatsReceiver}
import com.twitter.finagle.util.DefaultTimer
import com.twitter.logging.Logger
import com.twitter.util.FuturePool.defaultExecutor
import com.twitter.util.{Duration, ExecutorServiceFuturePool, Future, FuturePool, Timer}

class BoundedPoolInetResolver(cache: LoadingCache[String, Future[Seq[InetAddress]]],
                              statsReceiver: StatsReceiver) extends InetResolver(
  CaffeineCache.fromLoadingCache(cache),
  statsReceiver,
  None,
  BoundedPoolInetResolver.boundedPool
) {

  override val scheme = BoundedPoolInetResolver.scheme

  private[this] val cacheStatsReceiver = statsReceiver.scope("cache")
  private[this] val cacheGauges = Seq(
    cacheStatsReceiver.addGauge("size") { cache.estimatedSize },
    cacheStatsReceiver.addGauge("evicts") { cache.stats().evictionCount },
    cacheStatsReceiver.addGauge("hit_rate") { cache.stats().hitRate.toFloat }
  )

}

object BoundedPoolInetResolver {

  lazy val boundedPool: FuturePool =
    new ExecutorServiceFuturePool(defaultExecutor) {
      override def toString: String =
        s"BoundedPoolInetResolver.boundedPool($defaultExecutor)"
    }

  lazy val defaultExecutor = Executors.newFixedThreadPool(64,
    new NamedPoolThreadFactory("boundedFuturePool", makeDaemons = true)
  )

  val scheme = "boundedpoolinet"

  def apply(): InetResolver =
    apply(DefaultStatsReceiver)

  def apply(unscopedStatsReceiver: StatsReceiver): InetResolver =
    apply(unscopedStatsReceiver, 16000)

  def apply(unscopedStatsReceiver: StatsReceiver, maxCacheSize: Long): InetResolver =
    apply(unscopedStatsReceiver, maxCacheSize, Stream.empty, DefaultTimer)

  def apply(
             unscopedStatsReceiver: StatsReceiver,
             maxCacheSize: Long,
             backoffs: Stream[Duration],
             timer: Timer
           ): InetResolver = {
    val statsReceiver = unscopedStatsReceiver.scope("boundedpool=").scope("boundedpool_dns")

    new BoundedPoolInetResolver(
      cache(
        new DnsResolver(statsReceiver, boundedPool),
        maxCacheSize,
        backoffs,
        timer
      ),
      statsReceiver
    )
  }

}