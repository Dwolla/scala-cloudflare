/*
 * Copyright 2016-2018 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fortysevendeg.scalacheck.datetime.jdk8

import collection.JavaConverters._

import org.scalacheck.Gen
import org.scalacheck.Arbitrary

import java.time._
import java.time.temporal.ChronoUnit.MILLIS

import com.fortysevendeg.scalacheck.datetime.Granularity

trait GenJdk8Ext {

  def genHour = Gen.choose(0, 23)

  def genMinute = Gen.choose(0, 59)

  def genSecond = Gen.choose(0, 59)

  def genNanoOfSecond = Gen.choose(0, 999999999)

  def genYear = Gen.choose(-292278994, 292278994)

  def genMonth = Gen.choose(1, 12)

  def maxDaysInMonth(month: Int, year: Int) = Month.of(month).length(Year.of(year).isLeap)

  def genZonedDateTime(implicit granularity: Granularity[ZonedDateTime]): Gen[ZonedDateTime] =
    for {
      year <- genYear
      month <- genMonth
      dayOfMonth <- Gen.choose(1, maxDaysInMonth(month, year))
      hour <- genHour
      minute <- genMinute
      second <- genSecond
      nanoOfSecond <- genNanoOfSecond
      zoneId <- Gen.oneOf(ZoneId.getAvailableZoneIds.asScala.toList)
    } yield
      granularity.normalize(
        ZonedDateTime
          .of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, ZoneId.of(zoneId)))

  def genLocalDateTime(implicit granularity: Granularity[LocalDateTime]): Gen[LocalDateTime] =
    for {
      year <- genYear
      month <- genMonth
      dayOfMonth <- Gen.choose(1, maxDaysInMonth(month, year))
      hour <- genHour
      minute <- genMinute
      second <- genSecond
      nanoOfSecond <- genNanoOfSecond
    } yield
      granularity.normalize(
        LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond))

  def genLocalTime(implicit granularity: Granularity[LocalTime]): Gen[LocalTime] =
    for {
      hour <- genHour
      minute <- genMinute
      second <- genSecond
      nanoOfSecond <- genNanoOfSecond
    } yield granularity.normalize(LocalTime.of(hour, minute, second, nanoOfSecond))

  def genLocalDate(implicit granularity: Granularity[LocalDate]): Gen[LocalDate] =
    for {
      year <- genYear
      month <- genMonth
      dayOfMonth <- Gen.choose(1, maxDaysInMonth(month, year))
    } yield granularity.normalize(LocalDate.of(year, month, dayOfMonth))

  val genDuration: Gen[Duration] =
    Gen.choose(Long.MinValue, Long.MaxValue / 1000).map(l => Duration.of(l, MILLIS))

  val genPeriod: Gen[Period] =
    Gen.choose(Int.MinValue, Int.MaxValue / 1000).map(l => Period.ofDays(l))

  def genInstant(implicit granularity: Granularity[Instant]): Gen[Instant] =
    for {
      chosen <- Gen.choose(Long.MinValue, Long.MaxValue)
    } yield granularity.normalize(Instant.ofEpochMilli(chosen))
}

object GenJdk8Ext extends GenJdk8Ext

object ArbitraryJdk8Ext extends GenJdk8Ext {
  implicit def arbJdk8ZonedDateTime(implicit granularity: Granularity[ZonedDateTime]): Arbitrary[ZonedDateTime] =
    Arbitrary(genZonedDateTime)

  implicit def arbJdk8LocalDateTime(implicit granularity: Granularity[LocalDateTime]): Arbitrary[LocalDateTime] =
    Arbitrary(genLocalDateTime)

  implicit def arbJdk8LocalDate(implicit granularity: Granularity[LocalDate]): Arbitrary[LocalDate] =
    Arbitrary(genLocalDate)

  implicit def arbJdk8LocalTime(implicit granularity: Granularity[LocalTime]): Arbitrary[LocalTime] =
    Arbitrary(genLocalTime)

  implicit def arbInstant(implicit granularity: Granularity[Instant]): Arbitrary[Instant] =
    Arbitrary(genInstant)

  implicit val arbDuration: Arbitrary[Duration] =
    Arbitrary(genDuration)

}
