package com.bench.engine

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.abs

class PowerReading(
    val volts: Double,
    val milliamps: Double,
    val watts: Double,
    val level: Int,
    val tempC: Double,
    val chargeMah: Double,
    val fullMah: Double,
    val charging: Boolean,
    val etaHours: Double
)

/** Battery power from the public BatteryManager API (no Shizuku needed). Polled once a second. */
object PowerReader {
    fun read(ctx: Context): PowerReading? {
        val bm = ctx.getSystemService(BatteryManager::class.java) ?: return null
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val volts = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000.0
        val temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val raw = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        var ua = if (raw == Long.MIN_VALUE) 0.0 else raw.toDouble()
        if (abs(ua) < 20000.0) ua *= 1000.0 // some phones report mA instead of µA
        val mA = abs(ua) / 1000.0

        val cnt = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val chargeMah = if (cnt > 0L) cnt / 1000.0 else 0.0
        val full = if (chargeMah > 0.0 && pct > 0) chargeMah / (pct / 100.0) else 0.0
        val eta = if (mA > 50.0 && full > 0.0) {
            (if (charging) full - chargeMah else chargeMah) / mA
        } else 0.0
        return PowerReading(volts, mA, volts * mA / 1000.0, pct, temp, chargeMah, full, charging, eta)
    }

    fun flow(ctx: Context): Flow<PowerReading> = flow {
        while (true) {
            val r = read(ctx)
            if (r != null) emit(r)
            delay(1000)
        }
    }
}
