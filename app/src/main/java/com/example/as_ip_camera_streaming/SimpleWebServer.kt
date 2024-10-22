package com.example.as_ip_camera_streaming

import fi.iki.elonen.NanoHTTPD
import java.io.PipedInputStream
import java.io.PipedOutputStream

// NanoHTTPD 기반의 간단한 웹 서버 클래스 (MJPEG 스트림을 반환)
class SimpleWebServer(port: Int, private val activity: MainActivity) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        if (uri == "/alert/start" && method == Method.POST) {
            // 진동 시작 함수 호출
            activity.startVibration()
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Vibration started")
        }

        if (uri == "/alert/stop" && method == Method.POST) {
            // 진동 중지 함수 호출
            activity.stopVibration()
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Vibration stopped")
        }

        // 기존 스트림 처리 코드
        val headers = HashMap<String, String>()
        headers["Content-Type"] = "multipart/x-mixed-replace; boundary=--BoundaryString"

        val pipedInputStream = PipedInputStream()
        val pipedOutputStream = PipedOutputStream(pipedInputStream)

        // 별도의 스레드에서 스트림 처리
        Thread {
            try {
                while (!Thread.interrupted()) {
                    val frameBytes = activity.latestFrame
                    if (frameBytes != null) {
                        pipedOutputStream.write(("--BoundaryString\r\n").toByteArray())
                        pipedOutputStream.write(("Content-Type: image/jpeg\r\n").toByteArray())
                        pipedOutputStream.write(("Content-Length: ${frameBytes.size}\r\n\r\n").toByteArray())
                        pipedOutputStream.write(frameBytes)
                        pipedOutputStream.write("\r\n".toByteArray())
                        pipedOutputStream.flush()
                    }
                    Thread.sleep(100) // 프레임 전송 간격 조절 (10fps)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pipedOutputStream.close()
            }
        }.start()

        return newChunkedResponse(Response.Status.OK, "multipart/x-mixed-replace; boundary=--BoundaryString", pipedInputStream)
    }
}
