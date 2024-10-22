package com.example.as_ip_camera_streaming

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var lensFacing = CameraSelector.LENS_FACING_BACK // 기본 후면 카메라 선택
    private val cameraRequestCode = 101
    private val audioRequestCode  = 102
    private val notificationRequestCode  = 103
    private lateinit var server: SimpleWebServer // NanoHTTPD 기반의 서버

    @Volatile
    var latestFrame: ByteArray? = null // 최신 프레임을 저장할 변수

    // **추가된 부분: 진동 상태 변수 및 Vibrator 변수 선언**
    private var isVibrating = false  // 진동 상태를 추적하는 변수
    private var vibrator: Vibrator? = null // vibrator 변수 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        val switchCameraButton: Button = findViewById(R.id.switchCameraButton)
        val ipAddressTextView: TextView = findViewById(R.id.ipAddressTextView)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Vibrator 초기화
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // 권한 요청 추가
        requestPermissions()

        // 카메라 전환 버튼 클릭 리스너
        switchCameraButton.setOnClickListener {
            // 후면 카메라와 전면 카메라를 전환
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT // 전면 카메라로 전환
            } else {
                CameraSelector.LENS_FACING_BACK // 후면 카메라로 전환
            }
            startCamera() // 카메라 전환 후 카메라를 다시 시작합니다.
        }

        // IP 주소 표시 및 서버 시작
        val ipAddress = getLocalIpAddress() // 로컬 네트워크에서 IP 주소를 가져옵니다.
        if (ipAddress != null) {
            val serverAddress = "$ipAddress:8080"
            ipAddressTextView.text = "Server running at: $serverAddress"
            server = SimpleWebServer(8080, this) // NanoHTTPD 서버를 포트 8080으로 시작, MainActivity 전달
            server.start() // 서버 시작
        } else {
            ipAddressTextView.text = "Unable to retrieve IP address."
        }
    }

    // 진동 시작 함수
    fun startVibration() {
        if (!isVibrating) {
            isVibrating = true
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0)
                vibrator?.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
            }
        }
    }

    // 진동 중지 함수
    fun stopVibration() {
        if (isVibrating) {
            isVibrating = false
            vibrator?.cancel()
        }
    }

    // 권한 요청 함수
    private fun requestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }
        // 알림 권한 요청은 API 레벨 33 이상에서만 가능
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // **진동 권한 요청 추가 (필요한 경우)**
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.VIBRATE)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), cameraRequestCode)
        } else {
            startCamera() // 권한이 모두 허용되어 있으면 카메라 시작
        }
    }

    // 권한 요청 결과 처리 함수
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraRequestCode) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (allPermissionsGranted) {
                startCamera()
            } else {
                Log.e("Permission", "필수 권한이 필요합니다.")
            }
        }
    }

    private fun startCamera() {
        // PreviewView를 TextureView로 강제 설정
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 카메라 프리뷰 설정
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider) // 프리뷰를 화면에 제공
            }

            // 선택된 카메라 렌즈 설정 (전면 또는 후면)
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            // ImageAnalysis 설정
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480)) // 원하는 해상도로 설정
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                val nv21 = yuv420ToNv21(imageProxy)
                val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 80, out)
                latestFrame = out.toByteArray()
                imageProxy.close()
            })

            try {
                // 카메라를 바인딩합니다. 기존 바인딩된 것을 모두 해제한 후 재바인딩합니다.
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // YUV_420_888 이미지를 NV21 형식의 바이트 배열로 변환하는 함수
    private fun yuv420ToNv21(imageProxy: ImageProxy): ByteArray {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }

    // 로컬 IP 주소를 가져오는 함수
    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue // 비활성화되었거나 루프백 인터페이스는 무시
                val addrs = java.util.Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress // IPv4 주소를 반환
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null // 유효한 IP 주소를 찾지 못한 경우 null 반환
    }

    // 앱이 종료될 때 카메라 쓰레드와 서버를 종료
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() // 카메라 쓰레드 종료
        server.stop() // HTTP 서버 종료

        // 앱 종료 시 진동도 중지
        stopVibration()
    }
}
