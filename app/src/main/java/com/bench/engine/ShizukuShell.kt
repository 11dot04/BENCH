package com.bench.engine

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import rikka.shizuku.Shizuku

/**
 * Optional shell-level data through Shizuku. Same approach as the microtag bridge: a plain shell
 * process, no AIDL service. Everything here is a no-op unless Shizuku runs and the user allowed it.
 */
object ShizukuShell {
    enum class State { MISSING, NO_PERMISSION, READY }

    private val _state = MutableStateFlow(State.MISSING)
    val state: StateFlow<State> = _state
    private var inited = false

    private val onBinder = Shizuku.OnBinderReceivedListener { update() }
    private val onDead = Shizuku.OnBinderDeadListener { update() }
    private val onPermission = Shizuku.OnRequestPermissionResultListener { _, _ -> update() }

    @Synchronized
    fun init() {
        if (inited) {
            update()
            return
        }
        inited = true
        try {
            Shizuku.addBinderReceivedListenerSticky(onBinder)
            Shizuku.addBinderDeadListener(onDead)
            Shizuku.addRequestPermissionResultListener(onPermission)
        } catch (e: Throwable) {
            Log.w(TAG, "listener setup failed: " + e.message)
        }
        update()
    }

    fun update() {
        _state.value = try {
            if (!Shizuku.pingBinder()) State.MISSING
            else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) State.READY
            else State.NO_PERMISSION
        } catch (_: Throwable) {
            State.MISSING
        }
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder() && !Shizuku.shouldShowRequestPermissionRationale()) Shizuku.requestPermission(1001)
        } catch (e: Throwable) {
            Log.w(TAG, "requestPermission failed: " + e.message)
        }
    }

    /** Runs a shell command (stdout and stderr) or returns null if it could not run. Call off the main thread. */
    fun run(command: String): String? = try {
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
        )
        m.isAccessible = true
        val process = m.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
        val out = process.inputStream.bufferedReader().readText()
        process.waitFor()
        out
    } catch (e: Throwable) {
        Log.w(TAG, "shell failed: " + e.message)
        null
    }

    private const val TAG = "BenchShizuku"
}
