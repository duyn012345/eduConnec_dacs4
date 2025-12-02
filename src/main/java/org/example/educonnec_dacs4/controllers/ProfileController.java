package org.example.educonnec_dacs4.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.educonnec_dacs4.client.NetworkClient;
import org.example.educonnec_dacs4.model.User;
import org.example.educonnec_dacs4.utils.SceneManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ProfileController {

    @FXML private Label lblName, lblUsername, lblStatus, lblTimeDate;
    @FXML private Label lblDisplayName, lblDisplayUsername, lblDisplayEmail, lblDisplayRole, lblCreatedAt;
    @FXML private ImageView imgAvatar;
    @FXML private Button btnEdit, btnSeenHistory;

    // Menu buttons
    @FXML private Button btnHome, btnSearch, btnChat, btnGroupChat, btnFiles, btnLogout;

    private final NetworkClient client = NetworkClient.getInstance();
    private User currentUser;

    @FXML
    private void initialize() {
        setupButtons();
        updateClock();
        loadCurrentUserInfo(); // Tải thông tin ngay khi vào
//        NetworkClient.getInstance().setOnMessageReceived((cmd, payload) -> {
//            if ("UPDATE_PROFILE_OK".equals(cmd)) {
//                Platform.runLater(this::updateProfileUI);
//            }
//        });
        // Cập nhật đồng hồ mỗi giây
        new Thread(() -> {
            while (true) {
                Platform.runLater(this::updateClock);
                try { Thread.sleep(1000); } catch (Exception ignored) {}
            }
        }).start();
    }
    private void loadCurrentUserInfo() {
        currentUser = client.getCurrentUser();

        if (currentUser != null) {
            updateProfileUI();
        } else {
            new Alert(Alert.AlertType.ERROR, "Không tìm thấy thông tin người dùng!").showAndWait();
            return;
        }

        // DỌN DẸP LISTENER CŨ TRƯỚC KHI SET MỚI → SIÊU QUAN TRỌNG!!!
        client.setOnMessageReceived(null); // XÓA CÁI CŨ ĐI

        client.setOnMessageReceived((cmd, payload) -> Platform.runLater(() -> {
            switch (cmd) {
                case "UPDATE_PROFILE_OK" -> {
                    String[] p = payload.split("\\|", 7);
                    currentUser.setName(p[0]);
                    currentUser.setUsername(p[1]);
                    currentUser.setEmail(p[2]);
                    currentUser.setRole(p.length > 3 ? p[3] : "Thành viên");
                    currentUser.setUserId(Integer.parseInt(p[4]));
                    currentUser.setAvatar(p.length > 5 ? p[5] : "");
                    currentUser.setCreatedAt(p.length > 6 ? p[6] : "Chưa xác định");
                    updateProfileUI();
                }
                case "USER_PROFILE_UPDATED" -> {
                    String[] p = payload.split("\\|", 4);
                    int userId = Integer.parseInt(p[0]);
                    String newName = p[1];
                    String newEmail = p[3];
                    if (currentUser.getUserId() == userId) {
                        currentUser.setName(newName);
                        currentUser.setEmail(newEmail);
                        updateProfileUI();
                    }
                }
            }
        }));
    }
    private void updateProfileUI() {
        if (currentUser == null) return;

        lblName.setText(currentUser.getName());
        lblUsername.setText( currentUser.getUsername());
        lblStatus.setText("Online");
        lblStatus.setStyle("-fx-text-fill: #28a745;");

        lblDisplayName.setText(currentUser.getName());
        lblDisplayUsername.setText(currentUser.getUsername());
        lblDisplayEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "Chưa cập nhật");
        lblDisplayRole.setText(currentUser.getRole());
        lblCreatedAt.setText(currentUser.getCreatedAt()); // Có thể lấy từ DB sau

        // Load avatar
        Image avatarImage = loadImage(currentUser.getAvatar());
        imgAvatar.setImage(avatarImage);
    }
    private Image loadImage(String path) {
        try {
            if (path != null && path.startsWith("/image/")) {
                return new Image(getClass().getResourceAsStream(path));
            } else if (path != null && !path.isEmpty()) {
                return new Image(path);
            }
        } catch (Exception e) {
            // Nếu lỗi → dùng mặc định
        }
        return new Image(getClass().getResourceAsStream("/image/avatar.png"));
    }
    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        lblTimeDate.setText(now.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy")));
    }

    @FXML
    private void openEditProfile() {
        if (currentUser == null) {
            new Alert(Alert.AlertType.WARNING, "Không thể tải thông tin!").show();
            return;
        }

        SceneManager.showModal("editProfile.fxml", "Chỉnh sửa hồ sơ", controller -> {
            if (controller instanceof EditProfileController editCtrl) {
                editCtrl.setUser(currentUser, this::updateProfileUI);
            }
            return null;
        });
    }

    private void setupButtons() {
        btnHome.setOnAction(e -> SceneManager.changeScene("home.fxml"));
        btnSearch.setOnAction(e -> SceneManager.changeScene("searchFriend.fxml"));
        btnChat.setOnAction(e -> SceneManager.changeScene("chat.fxml"));
        btnGroupChat.setOnAction(e -> SceneManager.changeScene("groupChat.fxml"));
        btnFiles.setOnAction(e -> SceneManager.changeScene("fileDoc.fxml"));
        btnLogout.setOnAction(e -> logout());
    }

    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Đăng xuất");
        alert.setHeaderText("Bạn có chắc chắn muốn đăng xuất?");
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                client.disconnect();
                SceneManager.changeScene("login.fxml");
            }
        });
    }
}