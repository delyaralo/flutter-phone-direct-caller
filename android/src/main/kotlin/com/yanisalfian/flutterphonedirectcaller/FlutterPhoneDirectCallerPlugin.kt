package com.yanisalfian.flutterphonedirectcaller

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

/** FlutterPhoneDirectCallerPlugin  */
class FlutterPhoneDirectCallerPlugin : FlutterPlugin, ActivityAware {
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null

    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "flutter_phone_direct_caller")
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "callNumber" -> {
                    val number = call.argument<String>("number")?.replace("#", "%23")
                    if (number != null) {
                        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            result.success(callNumber(number))
                        } else {
                            ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.CALL_PHONE), CALL_REQ_CODE)
                            result.success(null) // You may need to handle permission result callback
                        }
                    } else {
                        result.error("INVALID_NUMBER", "Invalid phone number.", null)
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addRequestPermissionsResultListener(MyRequestPermissionsResultListener())
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    private fun callNumber(number: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$number")
            if (isTelephonyEnabled) {
                activity?.startActivity(intent)
                true
            } else {
                Log.d("FlutterPhoneDirectCaller", "Telephony not enabled")
                false
            }
        } catch (e: Exception) {
            Log.d("FlutterPhoneDirectCaller", "Error calling number: ${e.message}")
            false
        }
    }

    private val isTelephonyEnabled: Boolean
        get() {
            val tm = activity?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return tm.phoneType != TelephonyManager.PHONE_TYPE_NONE
        }

    companion object {
        private const val CALL_REQ_CODE = 0
    }

    private inner class MyRequestPermissionsResultListener : PluginRegistry.RequestPermissionsResultListener {
        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
            if (requestCode == CALL_REQ_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Handle permission granted (You may need to trigger callNumber again)
            }
            return false
        }
    }
}
