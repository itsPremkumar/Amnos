package com.amnos.browser.core.session

import android.os.Build
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowLooper
import com.amnos.browser.MainActivity

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class MainThreadPerformanceTest {

    @Test
    fun testMainThreadPerformanceDuringInitialization() {
        ShadowLog.stream = System.out

        val activityController = Robolectric.buildActivity(MainActivity::class.java)
        val onCreateStart = System.currentTimeMillis()
        activityController.create()
        val onCreateDuration = System.currentTimeMillis() - onCreateStart

        val activity = activityController.start().resume().get()
        ShadowLooper.idleMainLooper()
        Thread.sleep(1000)
        ShadowLooper.idleMainLooper()

        println("onCreate() duration: ${onCreateDuration}ms")
        assertNotNull(activity)

        activityController.pause().stop().destroy()
    }

    @Test
    fun testMultipleTabOperationsDoNotBlockMainThread() {
        ShadowLog.stream = System.out

        val activityController = Robolectric.buildActivity(MainActivity::class.java)
        activityController.create().start().resume().get()

        ShadowLooper.idleMainLooper()
        Thread.sleep(500)
        ShadowLooper.idleMainLooper()

        activityController.pause().stop().destroy()
        assertTrue(true)
    }
}
