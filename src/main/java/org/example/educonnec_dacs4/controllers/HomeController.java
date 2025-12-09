package org.example.educonnec_dacs4.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.educonnec_dacs4.client.NetworkClient;
import org.example.educonnec_dacs4.model.User;
import org.example.educonnec_dacs4.utils.SceneManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class HomeController {

    @FXML private Label lblUsername;
    @FXML private Label lblTimeDate;
    @FXML private ImageView imgAvatar;
    @FXML private Button bntProfile;
    @FXML private Button bntNotification;
    @FXML private ImageView imgBell;
    @FXML private Label lblNotificationBadge;
    // Nút chữ
    @FXML private Button btnHome, btnSearch, btnChat, btnGroupChat, btnFiles, btnLogout;

    // Nút icon
    @FXML private Button btnHome1, btnSearch1, btnChat1, btnGroupChat1, btnFiles1, btnLogout1;

    @FXML
    private void initialize() {
        updateUserInfo();
        updateClock();
        setupButtons();
        NetworkClient.getInstance().setOnMessageReceived((cmd, payload) -> {
            if ("UPDATE_PROFILE_OK".equals(cmd)) {
                Platform.runLater(this::updateUserInfo);
            }
        });
        // ĐĂNG KÝ LISTENER ĐẦY ĐỦ CHO TẤT CẢ CÁC LỆNH
        NetworkClient.getInstance().subscribe(this::handleMessage);

        // YÊU CẦU SERVER GỬI SỐ LƯỢNG THÔNG BÁO CHƯA ĐỌC LẦN ĐẦU
        NetworkClient.getInstance().send("GET_UNREAD_COUNT");
    }
    // PHƯƠNG THỨC XỬ LÝ TẤT CẢ CÁC LỆNH TỪ SERVER
    private void handleMessage(String cmd, String payload) {
        Platform.runLater(() -> {
            switch (cmd) {
                case "UPDATE_PROFILE_OK" -> updateUserInfo();
                case "NOTIFICATION_UNREAD_COUNT" -> handleUnreadCount(payload); // Xử lý Badge
                case "NEW_NOTIFICATION" -> handleNewNotification(payload); // Kích hoạt cập nhật Badge
                // Thêm các case cần thiết khác nếu HomeController cần xử lý
            }
        });
    }

    // XỬ LÝ SỐ LƯỢNG CHƯA ĐỌC VÀ CẬP NHẬT BADGE
    private void handleUnreadCount(String payload) {
        try {
            int count = Integer.parseInt(payload.trim());

            if (lblNotificationBadge != null) {
                lblNotificationBadge.setText(String.valueOf(count));
                lblNotificationBadge.setVisible(count > 0);
            }

            // Cập nhật màu sắc Icon nếu cần thiết
            if (imgBell != null) {
                String path = count > 0 ? "/image/notification_red.png" : "/image/notification_gray.png";
                // Chú ý: bạn nên dùng hàm loadAvatar/loadImage để đảm bảo an toàn
                imgBell.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path))));
            }
        } catch (Exception e) {
            System.err.println("Lỗi xử lý NOTIFICATION_UNREAD_COUNT: " + payload);
            if (lblNotificationBadge != null) lblNotificationBadge.setVisible(false);
        }
    }

    private void handleNewNotification(String payload) {
        // Khi nhận được thông báo mới, ta yêu cầu server gửi lại số lượng chưa đọc
        // để đảm bảo Badge được cập nhật đồng bộ.
        NetworkClient.getInstance().send("GET_UNREAD_COUNT");

        // Nếu bạn muốn hiển thị Alert/Toast:
        // String[] p = payload.split("\\|", 2);
        // new Alert(Alert.AlertType.INFORMATION, p[1]).show();
    }

    private void updateUserInfo() {
        var client = NetworkClient.getInstance();
        User user = client.getCurrentUser();
        if (user != null) {
            // SỬA DÒNG NÀY: HIỆN TÊN THẬT THAY VÌ USERNAME
            lblUsername.setText(user.getName());
// GỌI HÀM TẢI ẢNH MỚI
            imgAvatar.setImage(loadAvatarFromPathOrUrl(user.getAvatar()));
        }
    }
    private Image loadAvatarFromPathOrUrl(String urlPath) {
        // 1. Kiểm tra URL có hợp lệ không
        if (urlPath != null && (urlPath.startsWith("http://") || urlPath.startsWith("https://"))) {
            try {
                // Tải ảnh từ URL Cloudinary
                // Tham số true giúp tải bất đồng bộ (async), tránh làm treo giao diện
                return new Image(urlPath, true);
            } catch (Exception e) {
                System.err.println("Lỗi tải ảnh từ Cloudinary URL: " + urlPath + ". Dùng ảnh mặc định.");
                // Nếu lỗi khi tải từ URL, dùng ảnh mặc định
            }
        }

        // 2. Tải ảnh mặc định (Fallback)
        try {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/avatar.png")));
        } catch (NullPointerException e) {
            System.err.println("Không tìm thấy ảnh mặc định.");
            return null; // Trả về null nếu ảnh mặc định cũng không tồn tại
        }
    }

    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        lblTimeDate.setText(now.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy")));
    }

    private void setupButtons() {

        // Trang chủ
        btnHome.setOnAction(e -> SceneManager.changeScene("home.fxml"));
        btnHome1.setOnAction(e -> SceneManager.changeScene("home.fxml"));

        // Tìm bạn
        btnSearch.setOnAction(e -> SceneManager.changeScene("searchFriend.fxml"));
        btnSearch1.setOnAction(e -> SceneManager.changeScene("searchFriend.fxml"));

        // Chat đơn
        btnChat.setOnAction(e -> SceneManager.changeScene("chat.fxml"));
        btnChat1.setOnAction(e -> SceneManager.changeScene("chat.fxml"));

        // Chat nhóm
        btnGroupChat.setOnAction(e -> SceneManager.changeScene("groupChat.fxml"));
        btnGroupChat1.setOnAction(e -> SceneManager.changeScene("groupChat.fxml"));

        // File
        btnFiles.setOnAction(e -> SceneManager.changeScene("fileDoc.fxml"));
        btnFiles1.setOnAction(e -> SceneManager.changeScene("fileDoc.fxml"));

        bntProfile.setOnAction(e -> SceneManager.changeScene("profile.fxml"));
        bntNotification.setOnAction(e -> showNotificationPopup());

        // Logout
        btnLogout.setOnAction(e -> logout());
        btnLogout1.setOnAction(e -> logout());
    }

    private void showNotificationPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/notifications.fxml"));
            Parent root = loader.load();

            // Lấy controller để truyền dữ liệu nếu cần
            NotificationController controller = loader.getController();

            Stage popupStage = new Stage();
            popupStage.setScene(new Scene(root));
            popupStage.initStyle(StageStyle.TRANSPARENT);
            popupStage.initModality(Modality.NONE); // Không block Home
            popupStage.setResizable(false);
            popupStage.setWidth(420);
            popupStage.setHeight(600);

            // Đặt vị trí popup ở giữa màn hình
            Stage mainStage = (Stage) bntNotification.getScene().getWindow();
            popupStage.setX(mainStage.getX() + mainStage.getWidth() / 2 - 210);
            popupStage.setY(mainStage.getY() + mainStage.getHeight() / 2 - 300);

            // Cho phép controller đóng popup
            controller.setPopupStage(popupStage);

            popupStage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Đăng xuất");
        alert.setHeaderText("Bạn có chắc muốn đăng xuất?");
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                NetworkClient.getInstance().disconnect();
                SceneManager.changeScene("login.fxml");
            }
        });
    }
}
