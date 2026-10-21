package com.example

import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLoggerTest {

    @Test
    fun `test e without throwable`() {
        AppLogger.e("TEST_TAG", "error message")
        val logs = ShadowLog.getLogs()
        val log = logs.find { it.tag == "TEST_TAG" && it.msg == "error message" }
        assertNotNull(log)
        assertEquals(Log.ERROR, log?.type)
        assertNull(log?.throwable)
    }

    @Test
    fun `test e with throwable`() {
        val exception = RuntimeException("something went wrong")
        AppLogger.e("TEST_TAG", "error message with exception", exception)
        val logs = ShadowLog.getLogs()
        val log = logs.find { it.tag == "TEST_TAG" && it.msg == "error message with exception" }
        assertNotNull(log)
        assertEquals(Log.ERROR, log?.type)
        assertEquals(exception, log?.throwable)
    }

    @Test
    fun `test d`() {
        AppLogger.d("TEST_TAG", "debug message")
        val logs = ShadowLog.getLogs()
        val log = logs.find { it.tag == "TEST_TAG" && it.msg == "debug message" }
        assertNotNull(log)
        assertEquals(Log.DEBUG, log?.type)
    }

    @Test
    fun `test i`() {
        AppLogger.i("TEST_TAG", "info message")
        val logs = ShadowLog.getLogs()
        val log = logs.find { it.tag == "TEST_TAG" && it.msg == "info message" }
        assertNotNull(log)
        assertEquals(Log.INFO, log?.type)
    }

    @Test
    fun `test w without throwable`() {
        AppLogger.w("TEST_TAG", "warning message")
        val logs = ShadowLog.getLogs()
        val log = logs.find { it.tag == "TEST_TAG" && it.msg == "warning message" }
        assertNotNull(log)
        assertEquals(Log.WARN, log?.type)
        assertNull(log?.throwable)
    }

    @Test
    fun `test w with throwable`() {
        val exception = IllegalArgumentException("invalid argument")
        AppLogger.w("TEST_TAG", "warning message with exception", exception)
        val logs = ShadowLog.getLogs()
        val log = logs.find { it.tag == "TEST_TAG" && it.msg == "warning message with exception" }
        assertNotNull(log)
        assertEquals(Log.WARN, log?.type)
        assertEquals(exception, log?.throwable)
    }
}
