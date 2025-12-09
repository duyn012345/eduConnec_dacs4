//package com.example.demo1;
//
//import javafx.application.Platform;
//import javafx.embed.swing.SwingFXUtils;
//import javafx.scene.image.ImageView;
//import org.bytedeco.javacv.*;
//import org.bytedeco.javacv.Frame;
//
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
//
//public class VideoSender extends Thread {
//    private final ImageView localView;
//    private final String peerIP;
//    private final int port;
//    private volatile boolean running = true;
//
//    public VideoSender(ImageView localView, String peerIP, int port) {
//        this.localView = localView;
//        this.peerIP = peerIP;
//        this.port = port;
//    }
//
//    @Override
//    public void run() {
//        try (DatagramSocket socket = new DatagramSocket()) {
//            OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
//            grabber.start();
//            Java2DFrameConverter converter = new Java2DFrameConverter();
//            InetAddress address = InetAddress.getByName(peerIP);
//
//            while (running) {
//                Frame frame = grabber.grab();
//                if (frame == null) continue;
//
//                BufferedImage bImage = converter.getBufferedImage(frame);
//
//                // Resize để giảm kích thước dữ liệu
//                BufferedImage smallImg = new BufferedImage(320, 240, BufferedImage.TYPE_3BYTE_BGR);
//                Graphics2D g = smallImg.createGraphics();
//                g.drawImage(bImage, 0, 0, 320, 240, null);
//                g.dispose();
//
//                // Hiển thị local
//                Platform.runLater(() -> localView.setImage(SwingFXUtils.toFXImage(smallImg, null)));
//
//                // Chuyển ảnh sang byte JPEG
//                byte[] data = ImageUtils.bufferedImageToJpegBytes(smallImg, 0.5f);
//
//                // Gửi dữ liệu
//                DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
//                socket.send(packet);
//
//                Thread.sleep(33); // ~30fps
//            }
//            grabber.stop();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void stopSending() {
//        running = false;
//    }
//}
package org.example.educonnec_dacs4.utils;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.net.Socket;

public class VideoSender extends Thread {
    private final ImageView localView;
    private final String peerIP;
    private final int port;
    private volatile boolean running = true;

    public VideoSender(ImageView localView, String peerIP, int port) {
        this.localView = localView;
        this.peerIP = peerIP;
        this.port = port;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(peerIP, port);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
            grabber.start();
            Java2DFrameConverter converter = new Java2DFrameConverter();

            while (running) {
                Frame frame = grabber.grab();
                if (frame == null) continue;

                BufferedImage bImage = converter.getBufferedImage(frame);

                // Resize ảnh 320x240 để TCP gửi nhanh
                BufferedImage smallImg = new BufferedImage(320, 240, BufferedImage.TYPE_3BYTE_BGR);
                Graphics2D g = smallImg.createGraphics();
                g.drawImage(bImage, 0, 0, 320, 240, null);
                g.dispose();

                // Hiển thị local
                Platform.runLater(() -> localView.setImage(SwingFXUtils.toFXImage(smallImg, null)));

                // Chuyển sang JPEG
                byte[] data = ImageUtils.bufferedImageToJpegBytes(smallImg);

                // Gửi độ dài + dữ liệu
                dos.writeInt(data.length);
                dos.write(data);
                dos.flush();

                Thread.sleep(33); // ~30 FPS
            }

            grabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopSending() {
        running = false;
    }
}
