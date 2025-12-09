package org.example.educonnec_dacs4.utils;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class VideoReceiver extends Thread {
    private final ImageView remoteView;
    private final int port;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public VideoReceiver(ImageView remoteView, int port) {
        this.remoteView = remoteView;
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            this.serverSocket = server; // GÁN THAM CHIẾU
            while (running) {
                Socket client = server.accept();
                DataInputStream dis = new DataInputStream(client.getInputStream());

                while (running && !client.isClosed()) {
                    int length;
                    try {
                        length = dis.readInt();
                    } catch (Exception ex) {
                        break;
                    }

                    byte[] data = new byte[length];
                    dis.readFully(data);

                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
                    if (image != null) {
                        Platform.runLater(() -> remoteView.setImage(SwingFXUtils.toFXImage(image, null)));
                    }
                }

                client.close();
            }
        } catch (Exception e) {
            // In ra Stack Trace nếu có lỗi (ví dụ: Socket bị đóng đột ngột)
            if (running) {
                e.printStackTrace();
            }
        }
    }

    public void stopReceiving() {

        running = false;
        // Bắt buộc đóng ServerSocket để giải phóng accept() đang bị chặn
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
