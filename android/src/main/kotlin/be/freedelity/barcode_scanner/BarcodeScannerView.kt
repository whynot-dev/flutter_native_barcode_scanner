// Copyright 2023 Freedelity. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package be.freedelity.barcode_scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.platform.PlatformView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class BarcodeScannerView(activity: Activity, barcodeScannerController: BarcodeScannerController, context: Context, creationParams: Map<String?, Any?>?, activityBinding: ActivityPluginBinding) : PlatformView, PluginRegistry.RequestPermissionsResultListener {

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val previewView: PreviewView = PreviewView(context)
    private val activityBinding: ActivityPluginBinding = activityBinding
    private val controller: BarcodeScannerController = barcodeScannerController
    private val params: Map<String?, Any?>? = creationParams
    private val viewContext: Context = context

    override fun getView(): View {
        return previewView
    }

    override fun dispose() {
        activityBinding.removeRequestPermissionsResultListener(this)
        cameraExecutor.shutdown()
    }

    init {
        if( allPermissionsGranted(context) ) {
            barcodeScannerController.startCamera(creationParams, context, previewView, cameraExecutor)
        } else {
            activityBinding.addRequestPermissionsResultListener(this)
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                controller.startCamera(params, viewContext, previewView, cameraExecutor)
            }
            activityBinding.removeRequestPermissionsResultListener(this)
            return true
        }
        return false
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).toTypedArray()
    }

    private fun allPermissionsGranted(context: Context) = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
