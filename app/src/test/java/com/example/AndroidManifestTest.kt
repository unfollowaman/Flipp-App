package com.example

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

  @Test
  fun `zero network permissions in manifest`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
    val permissions = packageInfo.requestedPermissions ?: emptyArray()
    assertFalse("Manifest must not declare INTERNET permission", permissions.contains("android.permission.INTERNET"))
    assertFalse("Manifest must not declare ACCESS_NETWORK_STATE permission", permissions.contains("android.permission.ACCESS_NETWORK_STATE"))
  }

  @Test
  fun `launcher icons, label, and brand assets are configured`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appInfo = context.applicationInfo
    assertEquals(R.mipmap.ic_launcher, appInfo.icon)
    assertEquals(R.string.app_name, appInfo.labelRes)
    assertEquals("Flipp", context.getString(R.string.app_name))
    assertEquals(0xFFFFD23F.toInt(), ContextCompat.getColor(context, R.color.ic_launcher_background))
    assertNotNull(ContextCompat.getDrawable(context, R.drawable.flipp_logo))
    assertNotNull(ContextCompat.getDrawable(context, R.drawable.flipp_logo_small))
  }
}
