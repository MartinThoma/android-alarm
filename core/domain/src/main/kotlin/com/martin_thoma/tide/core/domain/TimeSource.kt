package com.martin_thoma.tide.core.domain

import java.time.ZonedDateTime

/** The current time in the device's current time zone. Injected so tests can control it. */
fun interface TimeSource {
    fun now(): ZonedDateTime
}

/** Reads the zone on every call, so a time-zone change takes effect immediately. */
object SystemTimeSource : TimeSource {
    override fun now(): ZonedDateTime = ZonedDateTime.now()
}
