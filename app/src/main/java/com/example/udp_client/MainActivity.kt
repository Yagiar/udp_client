package com.example.udp_client

import org.opencv.core.Mat
import org.opencv.highgui.HighGui
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.videoio.VideoCapture
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.ceil

fun main() {
    // Инициализация OpenCV
    nu.pattern.OpenCV.loadLocally()

    val maxLength = 65000
    val host = "127.0.0.1" // IP-адрес получателя
    val port = 5000        // Порт получателя

    val socket = DatagramSocket()
    val address = InetAddress.getByName(host)

    val capture = VideoCapture(0) // Камера по умолчанию
    if (!capture.isOpened) {
        println("Не удалось открыть камеру")
        return
    }

    val frame = Mat()
    while (capture.read(frame)) {
        // Кодирование кадра в формат JPEG
        val buffer = ByteArrayOutputStream()
        val success = Imgcodecs.imencode(".jpg", frame, buffer)

        if (success) {
            val data = buffer.toByteArray()
            val bufferSize = data.size
            val numOfPacks = if (bufferSize > maxLength) ceil(bufferSize.toDouble() / maxLength).toInt() else 1

            // Информация о количестве пакетов
            val frameInfo = "packs:$numOfPacks"
            val infoPacket = DatagramPacket(frameInfo.toByteArray(), frameInfo.length, address, port)
            socket.send(infoPacket)
            println("Отправлено количество пакетов: $numOfPacks")

            // Отправка фрейма по частям
            var left = 0
            var right = maxLength
            for (i in 0 until numOfPacks) {
                val chunk = data.copyOfRange(left, minOf(right, bufferSize))
                left = right
                right += maxLength

                val packet = DatagramPacket(chunk, chunk.size, address, port)
                socket.send(packet)
                println("Пакет $i отправлен: ${chunk.size} байт")
            }
        }
    }

    println("Завершение работы")
    capture.release()
    socket.close()
}