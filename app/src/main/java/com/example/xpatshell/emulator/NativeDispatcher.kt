package com.example.xpatshell.emulator

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.media.MediaRecorder
import android.os.*
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.*

class NativeDispatcher(
    private val ctx: Context,
    private val callback: (JSONObject) -> Unit
) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null

    // Track pending permission requests
    companion object {
        const val REQ_CAMERA = 1001
        const val REQ_MIC = 1002
        const val REQ_LOCATION = 1003
        const val REQ_SMS = 1004
    }

    fun dispatch(id: String, module: String, action: String, params: JSONObject) {
        val response = JSONObject().apply {
            put("id", id)
            put("module", module)
            put("action", action)
        }

        try {
            when (module) {
                "vibrate" -> handleVibrate(action, params, response)
                "toast" -> handleToast(action, params, response)
                "notify" -> handleNotify(action, params, response)
                "filesystem" -> handleFilesystem(action, params, response)
                "battery" -> handleBattery(action, params, response)
                "location" -> checkPermissionThenRun(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    REQ_LOCATION
                ) { handleLocation(action, params, response) }

                "sms" -> checkPermissionThenRun(
                    Manifest.permission.SEND_SMS,
                    REQ_SMS
                ) { handleSms(action, params, response) }

                "torch" -> checkPermissionThenRun(
                    Manifest.permission.CAMERA,
                    REQ_CAMERA
                ) { handleTorch(action, params, response) }

                "recorder" -> checkPermissionThenRun(
                    Manifest.permission.RECORD_AUDIO,
                    REQ_MIC
                ) { handleRecorder(action, params, response) }

                "device" -> handleDevice(action, params, response)
                else -> {
                    response.put("status", "error")
                    response.put("error", "Unknown module: $module")
                }
            }
        } catch (e: Exception) {
            response.put("status", "error")
            response.put("error", e.message)
        }

        callback(response)
    }

    // Helper to request permission only when needed
    private fun checkPermissionThenRun(permission: String, reqCode: Int, runAction: () -> Unit) {
        val act = ctx as? Activity ?: return
        if (ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(act, arrayOf(permission), reqCode)
            Toast.makeText(ctx, "Permission required for this action", Toast.LENGTH_SHORT).show()
        } else {
            runAction()
        }
    }

    // ---------- Vibrate ----------
    private fun handleVibrate(action: String, params: JSONObject, resp: JSONObject) {
        val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        when (action) {
            "vibrate" -> {
                val ms = params.optLong("duration", 200)
                @Suppress("DEPRECATION") v.vibrate(ms)
                resp.put("status", "ok")
            }
            "pattern" -> {
                val arr = params.optJSONArray("pattern") ?: JSONArray()
                val pattern = LongArray(arr.length()) { i -> arr.optLong(i, 100) }
                @Suppress("DEPRECATION") v.vibrate(pattern, -1)
                resp.put("status", "ok")
            }
            "cancel" -> { v.cancel(); resp.put("status", "ok") }
            else -> resp.put("status", "error").put("error", "Unknown vibrate action")
        }
    }

    // ---------- Toast ----------
    private fun handleToast(action: String, params: JSONObject, resp: JSONObject) {
        if (action == "show") {
            Toast.makeText(ctx, params.optString("message", ""), Toast.LENGTH_SHORT).show()
            resp.put("status", "ok")
        } else {
            resp.put("status", "error").put("error", "Unknown toast action")
        }
    }

    // ---------- Notification ----------
    private fun handleNotify(action: String, params: JSONObject, resp: JSONObject) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "xpatshell-default"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "XPAT Shell", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        if (action == "show") {
            val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            val builder = NotificationCompat.Builder(ctx, channelId)
                .setContentTitle(params.optString("title", "XPAT"))
                .setContentText(params.optString("message", ""))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
            nm.notify(id, builder.build())
            resp.put("status", "ok").put("id", id)
        } else {
            nm.cancelAll()
            resp.put("status", "ok")
        }
    }

    // ---------- Filesystem ----------
    private fun handleFilesystem(action: String, params: JSONObject, resp: JSONObject) {
        val dir = ctx.filesDir
        when (action) {
            "write" -> {
                val f = File(dir, params.optString("path", "file.txt"))
                f.parentFile?.mkdirs()
                FileOutputStream(f).use { it.write(params.optString("data", "").toByteArray()) }
                resp.put("status", "ok").put("path", f.absolutePath)
            }
            "read" -> {
                val f = File(dir, params.optString("path", "file.txt"))
                if (!f.exists()) resp.put("status", "error").put("error", "Not found")
                else {
                    val base64 = Base64.encodeToString(FileInputStream(f).readBytes(), Base64.NO_WRAP)
                    resp.put("status", "ok").put("data_base64", base64)
                }
            }
            else -> resp.put("status", "error").put("error", "Unknown filesystem action")
        }
    }

    // ---------- Battery ----------
    private fun handleBattery(action: String, params: JSONObject, resp: JSONObject) {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        if (action == "level") {
            resp.put("status", "ok").put("level", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
        } else resp.put("status", "error").put("error", "Unknown battery action")
    }

    // ---------- Location ----------
    @Suppress("MissingPermission")
    private fun handleLocation(action: String, params: JSONObject, resp: JSONObject) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = lm.getProviders(true)
        var loc: Location? = null
        for (p in providers) lm.getLastKnownLocation(p)?.let { loc = it; return@let }
        if (loc != null) {
            val data = JSONObject()
            data.put("lat", loc!!.latitude)
            data.put("lon", loc!!.longitude)
            data.put("accuracy", loc!!.accuracy)
            resp.put("status", "ok").put("location", data)
        } else {
            resp.put("status", "error").put("error", "No location available")
        }
    }

    // ---------- SMS ----------
    @Suppress("MissingPermission")
    private fun handleSms(action: String, params: JSONObject, resp: JSONObject) {
        if (action == "send") {
            try {
                val sms = android.telephony.SmsManager.getDefault()
                sms.sendTextMessage(params.optString("to", ""), null, params.optString("body", ""), null, null)
                resp.put("status", "ok")
            } catch (e: Exception) {
                resp.put("status", "error").put("error", e.message)
            }
        } else resp.put("status", "error").put("error", "Unknown sms action")
    }

    // ---------- Torch ----------
    private fun handleTorch(action: String, params: JSONObject, resp: JSONObject) {
        val cam = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cam.cameraIdList.firstOrNull() ?: return
        when (action) {
            "on" -> cam.setTorchMode(id, true)
            "off" -> cam.setTorchMode(id, false)
        }
        resp.put("status", "ok")
    }

    // ---------- Recorder ----------
    private fun handleRecorder(action: String, params: JSONObject, resp: JSONObject) {
        when (action) {
            "start" -> {
                val out = File(ctx.filesDir, "rec_${System.currentTimeMillis()}.mp4")
                mediaRecorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(out.absolutePath)
                    prepare(); start()
                }
                currentRecordingFile = out
                resp.put("status", "ok").put("path", out.absolutePath)
            }
            "stop" -> {
                mediaRecorder?.stop(); mediaRecorder?.release(); mediaRecorder = null
                resp.put("status", "ok").put("path", currentRecordingFile?.absolutePath)
            }
            else -> resp.put("status", "error").put("error", "Unknown recorder action")
        }
    }

    // ---------- Device Info ----------
    private fun handleDevice(action: String, params: JSONObject, resp: JSONObject) {
        if (action == "info") {
            val info = JSONObject().apply {
                put("model", Build.MODEL)
                put("manufacturer", Build.MANUFACTURER)
                put("os", "Android")
                put("osVersion", Build.VERSION.RELEASE)
                put("sdkInt", Build.VERSION.SDK_INT)
                put("appName", ctx.packageName)
            }
            resp.put("status", "ok").put("info", info)
        } else resp.put("status", "error").put("error", "Unknown device action")
    }
}