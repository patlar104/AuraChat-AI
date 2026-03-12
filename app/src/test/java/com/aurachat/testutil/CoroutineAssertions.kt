package com.aurachat.testutil

import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.fail

suspend inline fun <reified T : Throwable> assertThrowsSuspend(
    noinline block: suspend () -> Unit,
): T {
    val thrown = try {
        block()
        fail("Expected ${T::class.java.simpleName} to be thrown")
    } catch (throwable: Throwable) {
        throwable
    }
    return assertInstanceOf(T::class.java, thrown)
}
