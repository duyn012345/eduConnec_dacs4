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
import org.example.educonnec_dacs4.utils.SceneManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationController {

    @FXML private VBox notificationContainer;
    @FXML private Label lblUnreadCount;
    @FXML private Button btnBack;
    private final NetworkClient client = NetworkClient.getInstance();
    private Stage popupStage; // để đóng popup

    public void setPopupStage(Stage stage) {
        this.popupStage = stage;
    }
    @FXML
    public void initialize() {
        // ĐĂNG KÝ NHẬN THÔNG BÁO REALTIME (chỉ 1 lần duy nhất)
        client.subscribe(this::handleMessage);

        // TẢI THÔNG BÁO LẦN ĐẦU
        loadNotifications();

        // THÊM DÒNG NÀY: Click nút Back → quay lại màn hình trước
        btnBack.setOnMouseClicked(event -> {
            if (popupStage != null) {
                popupStage.close();
            }
        });
        btnBack.setOnMouseEntered(e -> btnBack.setOpacity(1.0));
        btnBack.setOnMouseExited(e -> btnBack.setOpacity(0.7));
    }

    private void handleMessage(String cmd, String payload) {
        switch (cmd) {
            case "NOTIFICATIONS" -> handleNotifications(payload);
            case "NOTIFICATION_UNREAD_COUNT" -> updateUnreadCount(Integer.parseInt(payload));
            case "NEW_NOTIFICATION" -> loadNotifications(); // nếu server đẩy realtime
            default -> { /* ignore */ }
        }
    }

    // Trong NotificationController.java

    private void handleNotifications(String payload) {
        // 1. Giả định Server gửi: <UNREAD_COUNT>|<JSON_LIST>
        String[] parts = payload.split("\\|", 2);

        int unreadCount;
        String jsonList;

        if (parts.length == 2) {
            // Trường hợp 1: Có đầy đủ COUNT|JSON
            try {
                unreadCount = Integer.parseInt(parts[0]);
                jsonList = parts[1];
            } catch (NumberFormatException e) {
                System.err.println("Lỗi phân tích số lượng thông báo chưa đọc: " + parts[0]);
                jsonList = parts[1];
                unreadCount = 0; // ⚠️ Dòng này khiến unreadCount không còn là effectively final
            }
        } else {
            // Trường hợp 2: Payload chỉ là JSON_LIST (Server không gửi COUNT|)
            jsonList = payload;
            unreadCount = 0; // ⚠️ Dòng này cũng khiến unreadCount không còn là effectively final
        }

        // 2. Phân tích JSON
        List<Notification> notifications = Notification.fromJsonArray(jsonList); // ⚠️ Dòng gán này khiến notifications không còn là effectively final

        // ===============================================
        // 💡 BƯỚC KHẮC PHỤC: TẠO CÁC BIẾN FINAL MỚI
        // ===============================================
        final int finalUnreadCount = unreadCount;
        final List<Notification> finalNotifications = notifications;

        // 3. Cập nhật giao diện
        Platform.runLater(() -> {
            notificationContainer.getChildren().clear();
            for (Notification n : finalNotifications) { // Sử dụng biến finalNotifications
                addNotificationItem(n);
            }
            updateUnreadCount(finalUnreadCount); // Sử dụng biến finalUnreadCount
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
            updateItemStyle(item, lblTitle, lblMessage, unreadDot, n.isRead());

            // SIÊU THÔNG MINH: CHUYỂN MÀN HÌNH ĐÚNG NHƯ BẠN MUỐN
            item.setOnMouseClicked(e -> {
                // 1. Đánh dấu đã đọc
                if (!n.isRead()) {
                    client.send("MARK_NOTIFICATION_READ|" + n.getId());
                    n.setRead(true);
                    Platform.runLater(() -> updateItemStyle(item, lblTitle, lblMessage, unreadDot, true));
                }

                // 2. Đóng popup
                if (popupStage != null) {
                    popupStage.close();
                }

                String title = n.getTitle().toLowerCase();

                // LOẠI 1: LỜI MỜI KẾT BẠN → MỞ TÌM BẠN
                if (title.contains("gửi lời mời kết bạn") ||
                        title.contains("mời kết bạn") ||
                        title.contains("muốn kết bạn") ||
                        title.contains("đã gửi lời mời kết bạn")) {
                    SceneManager.changeScene("searchFriend.fxml");
                    return;
                }

                // LOẠI 2: TIN NHẮN MỚI HOẶC ĐỒNG Ý KẾT BẠN → CHỈ MỞ CHAT (KHÔNG CHỌN AI!)
                if (title.contains("tin nhắn mới") ||
                        title.contains("đồng ý kết bạn") ||
                        title.contains("đã gửi tin nhắn") ||
                        title.contains("Nhóm mới") ||
                        title.contains("đã gửi bạn một tin nhắn")) {
                    SceneManager.changeScene("chat.fxml"); // CHỈ MỞ CHAT, KHÔNG CHỌN NGƯỜI
                    return;
                }

                // MẶC ĐỊNH: MỞ CHAT
                SceneManager.changeScene("chat.fxml");
            });

            notificationContainer.getChildren().add(item);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Giữ nguyên hàm update style đẹp như cũ
    private void updateItemStyle(VBox item, Label title, Label message, Circle dot, boolean isRead) {
        if (isRead) {
            item.setStyle("-fx-background-color: #ffffff;");
            dot.setVisible(false);
            title.setStyle("-fx-font-weight: normal; -fx-text-fill: #555555;");
            message.setStyle("-fx-font-weight: normal; -fx-text-fill: #666666;");
        } else {
            item.setStyle("-fx-background-color: #e3f2fd;");
            dot.setVisible(true);
            dot.setFill(Color.web("#007bff"));
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
            message.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");
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