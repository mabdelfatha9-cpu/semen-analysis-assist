package com.labtools.semenanalysis.data

import android.content.Context
import com.labtools.semenanalysis.model.CalibrationData
import android.os.Build

/**
 * Lightweight persistence for the current calibration. A real deployment
 * should key this by physical setup (phone + microscope + objective) since
 * changing any of those invalidates the microns-per-pixel ratio.
 */
object CalibrationStore {
    private const val PREFS_NAME = "calibration_prefs"
    private const val KEY_MICRONS_PER_PIXEL = "microns_per_pixel"
    private const val KEY_CHAMBER_DEPTH = "chamber_depth_microns"
    private const val KEY_CALIBRATED_AT = "calibrated_at"

    fun save(context: Context, micronsPerPixel: Double, chamberDepthMicrons: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_MICRONS_PER_PIXEL, micronsPerPixel.toFloat())
            .putFloat(KEY_CHAMBER_DEPTH, chamberDepthMicrons.toFloat())
            .putLong(KEY_CALIBRATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun load(context: Context): CalibrationData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_MICRONS_PER_PIXEL)) return null
        return CalibrationData(
            micronsPerPixel = prefs.getFloat(KEY_MICRONS_PER_PIXEL, 0f).toDouble(),
            chamberDepthMicrons = prefs.getFloat(KEY_CHAMBER_DEPTH, 20f).toDouble(),
            calibratedAtEpochMillis = prefs.getLong(KEY_CALIBRATED_AT, 0L),
            deviceLabel = Build.MODEL ?: "unknown-device"
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
