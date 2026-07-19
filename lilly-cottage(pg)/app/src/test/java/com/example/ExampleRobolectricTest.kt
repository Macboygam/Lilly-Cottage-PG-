package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Lilly Cottage(PG)", appName)
  }

  @Test
  fun `verify DormViewModel initialization`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DormViewModel(app)
    assertNotNull(viewModel)
    assertNotNull(viewModel.allMembers.value)
    assertNotNull(viewModel.allPayments.value)
    assertNotNull(viewModel.pendingMembers.value)
    assertEquals(3, viewModel.reminderDaysConfig.value)
  }
}
