package org.example.educonnec_dacs4.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.example.educonnec_dacs4.client.NetworkClient;
import org.example.educonnec_dacs4.model.Notification;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationController {

    @FXML private VBox notificationContainer;
    @FXML private Label lblUnreadCount;
    @FXML private Button bntBack;
    private final NetworkClient client = NetworkClient.getInstance();

    @FXML
    public void initialize() {
        // ĐĂNG KÝ NHẬN THÔNG BÁO REALTIME (chỉ 1 lần duy nhất)
        client.subscribe(this::handleMessage);

        // TẢI THÔNG BÁO LẦN ĐẦU
        loadNotifications();

        // Click vào tiêu đề "Thông báo" → đánh dấu tất cả đã đọc
        if (notificationContainer.getParent() instanceof VBox parent) {
            parent.setOnMouseClicked(e -> markAllAsRead());
        }
        // THÊM DÒNG NÀY: Click nút Back → quay lại màn hình trước
        bntBack.setOnMouseClicked(event -> {
             Stage stage = (Stage) bntBack.getScene().getWindow();
            stage.close();
 });
        bntBack.setOnMouseEntered(e -> bntBack.setOpacity(1.0));
        bntBack.setOnMouseExited(e -> bntBack.setOpacity(0.7));
    }

    private void handleMessage(String cmd, String payload) {
        switch (cmd) {
            case "NOTIFICATIONS" -> handleNotifications(payload);
            case "NOTIFICATION_UNREAD_COUNT" -> updateUnreadCount(Integer.parseInt(payload));
            case "NEW_NOTIFICATION" -> loadNotifications(); // nếu server đẩy realtime
            default -> { /* ignore */ }
        }
    }

    private void handleNotifications(String payload) {
        String[] parts = payload.split("\\|", 2);
        int unread = Integer.parseInt(parts[0]);
        String json = parts.length > 1 ? parts[1] : "[]";

        List<Notification> notifications = Notification.fromJsonArray(json);

        Platform.runLater(() -> {
            notificationContainer.getChildren().clear();
            for (Notification n : notifications) {
                addNotificationItem(n);
            }
            updateUnreadCount(unread);
        });
    }

    private void addNotificationItem(Notification n) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/notification_item.fxml"));
            VBox item = loader.load();

            Label lblTitle = (Label) item.lookup("#lblTitle");
            Label lblMessage = (Label) item.lookup("#lblMessage");
            Label lblTime = (Label) item.lookup("#lblTime");
            Circle unreadDot = (Circle) item.lookup("#unreadDot");

            lblTitle.setText(n.getTitle());
            lblMessage.setText(n.getContent());
            lblTime.setText(formatTime(n.getCreatedAt()));

            // Nếu chưa đọc → tô màu + hiện chấm xanh
            if (!n.isRead()) {
                item.setStyle("-fx-background-color: #e3f2fd;");
                unreadDot.setVisible(true);
                unreadDot.setFill(Color.web("#007bff"));
            }

            // Click vào thông báo → đánh dấu đã đọc
            item.setOnMouseClicked(e -> {
                if (!n.isRead()) {
                    client.send("MARK_NOTIFICATION_READ|" + n.getId());
                    n.setRead(true);
                    item.setStyle("-fx-background-color: #ffffff;");
                    unreadDot.setVisible(false);
                }
            });

            notificationContainer.getChildren().add(item);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateUnreadCount(int count) {
        Platform.runLater(() -> {
            if (count > 0) {
                lblUnreadCount.setText(count > 99 ? "99+" : String.valueOf(count));
                lblUnreadCount.setVisible(true);
            } else {
                lblUnreadCount.setVisible(false);
            }
        });
    }

    private void markAllAsRead() {
        client.send("MARK_ALL_NOTIFICATIONS_READ");
        // Tự động cập nhật UI
        notificationContainer.getChildren().forEach(node -> {
            node.setStyle("-fx-background-color: #ffffff;");
            Circle dot = (Circle) node.lookup("#unreadDot");
            if (dot != null) dot.setVisible(false);
        });
        updateUnreadCount(0);
    }

    private void loadNotifications() {
        client.send("GET_NOTIFICATIONS");
    }

    // Định dạng thời gian đẹp như Zalo
    private String formatTime(String dateTime) {
        try {
            LocalDateTime time = LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime now = LocalDateTime.now();

            long minutes = java.time.Duration.between(time, now).toMinutes();
            if (minutes < 1) return "Vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            if (minutes < 1440) return (minutes / 60) + " giờ trước";
            return time.format(DateTimeFormatter.ofPattern("HH:mm dd/MM"));
        } catch (Exception e) {
            return dateTime;
        }
    }
}