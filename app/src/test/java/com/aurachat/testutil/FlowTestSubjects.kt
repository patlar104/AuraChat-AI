package com.aurachat.testutil

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class StateFlowSubject<T>(initialValue: T) {
    private val mutableFlow = MutableStateFlow(initialValue)

    val flow: Flow<T> = mutableFlow.asStateFlow()

    var value: T
        get() = mutableFlow.value
        set(newValue) {
            mutableFlow.value = newValue
        }
}

class SharedFlowSubject<T>(
    replay: Int = 0,
    extraBufferCapacity: Int = 16,
) {
    private val mutableFlow = MutableSharedFlow<T>(
        replay = replay,
        extraBufferCapacity = extraBufferCapacity,
    )

    val flow: Flow<T> = mutableFlow.asSharedFlow()

    fun tryEmit(value: T): Boolean = mutableFlow.tryEmit(value)
}
