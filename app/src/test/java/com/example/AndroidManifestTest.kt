package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidManifestTest {

  @Test
  fun `allowBackup is configured to false`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val flags = context.applicationInfo.flags
    val isAllowBackupEnabled = (flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
    assertFalse("android:allowBackup should be set to false in AndroidManifest.xml", isAllowBackupEnabled)
  }

  @Test
  fun `mainActivity is exported as launcher activity`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val componentName = ComponentName(context, MainActivity::class.java)
    val activityInfo = context.packageManager.getActivityInfo(componentName, 0)
    assertTrue("MainActivity must be exported so the system launcher can launch the app", activityInfo.exported)
  }
}
