package org.example.educonnec_dacs4.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.educonnec_dacs4.client.NetworkClient;
import org.example.educonnec_dacs4.utils.VideoReceiver;
import org.example.educonnec_dacs4.utils.VideoSender;

public class VideoCallController {

    @FXML
    private ImageView localVideo;
    @FXML private ImageView remoteVideo;
    @FXML private Button startCallButton, endCallButton;
    @FXML private Label lblTargetName;
    private final int VIDEO_PORT = 5555;
    private boolean isReceiver; // Biến thành viên
    private String targetName;
    private String peerIP;

    private VideoSender sender;
    private VideoReceiver receiver;
    private String partnerUsername;
    // SỬA ĐIỂM 1: THÊM THAM SỐ boolean isReceiver
    public void setTargetInfo(String targetName, String peerIP, boolean isReceiver, String partnerUsername) {
        this.targetName = targetName;
        this.peerIP = peerIP;
        this.isReceiver = isReceiver;
        this.partnerUsername = partnerUsername; // <--- GÁN BIẾN
        lblTargetName.setText("Đang gọi: " + targetName);
    }

    @FXML
    public void initialize() {
        startCall();
        // KIỂM TRA NULL TRƯỚC KHI SỬ DỤNG endCallButton
        if (endCallButton != null) {
            endCallButton.setOnAction(e -> endCall());
        } else {
            System.err.println("Lỗi FXML: endCallButton là NULL. Vui lòng kiểm tra fx:id trong FXML.");
        }
    }

    private void startCall() {
        if (peerIP == null || peerIP.isEmpty()) return;
        if (isReceiver) {
            // Nếu mình là người NHẬN cuộc gọi. Phải start VideoReceiver (Lắng nghe)
            receiver = new VideoReceiver(remoteVideo, VIDEO_PORT);
            receiver.start();
            // 2. Start VideoSender (Gửi video đến người gọi A)
            sender = new VideoSender(localVideo, peerIP, VIDEO_PORT);
            sender.start();
        } else {
            // Nếu mình là người GỌI:- Start VideoSender (Kết nối và Gửi đến B)
            sender = new VideoSender(localVideo, peerIP, VIDEO_PORT);
            sender.start();
            // 2. Start VideoReceiver (Lắng nghe để nhận video từ B)
            receiver = new VideoReceiver(remoteVideo, VIDEO_PORT);
            receiver.start();
        }
    }

    @FXML
    public void endCall() {
        // 1. Dừng luồng gửi video (VideoSender)
        if (sender != null) {
            sender.stopSending();
            try {
                // Sử dụng join mà không có đối số nếu bạn không muốn giới hạn thời gian
                sender.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Sender join interrupted.");
            }
        }

        // 2. Dừng luồng nhận video (VideoReceiver)
        if (receiver != null) {
            receiver.stopReceiving();
            try {
                // Sử dụng join mà không có đối số
                receiver.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Receiver join interrupted.");
            }
        }
        if (partnerUsername != null) {
            NetworkClient.getInstance().send("CALL_END|" + partnerUsername);
        }
        Stage stage = (Stage) endCallButton.getScene().getWindow();
        stage.close();
    }
}