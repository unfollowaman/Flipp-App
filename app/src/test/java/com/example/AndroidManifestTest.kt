package com.example

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
