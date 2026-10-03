package org.ubaierbhat.android.barcodekeyboard.scanner

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class ScannerLifecycleOwner : LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = registry

    fun start() {
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
