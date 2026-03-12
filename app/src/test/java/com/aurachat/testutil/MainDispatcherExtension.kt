package com.aurachat.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(
    private val eagerlyRunTasks: Boolean = false,
) : BeforeEachCallback, AfterEachCallback {

    lateinit var scheduler: TestCoroutineScheduler
        private set

    lateinit var dispatcher: TestDispatcher
        private set

    override fun beforeEach(context: ExtensionContext) {
        scheduler = TestCoroutineScheduler()
        dispatcher = if (eagerlyRunTasks) {
            UnconfinedTestDispatcher(scheduler)
        } else {
            StandardTestDispatcher(scheduler)
        }
        Dispatchers.setMain(dispatcher)
    }

    override fun afterEach(context: ExtensionContext) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun MainDispatcherExtension.runTest(
    block: suspend TestScope.() -> Unit,
) = runTest(context = dispatcher, testBody = block)
