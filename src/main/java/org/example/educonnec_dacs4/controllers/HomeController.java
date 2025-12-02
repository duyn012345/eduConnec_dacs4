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

public class HomeController {

    @FXML private Label lblUsername;
    @FXML private Label lblTimeDate;
    @FXML private ImageView imgAvatar;
    @FXML private Button bntProfile;
    @FXML private Button bntNotification;
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
    }

    private void updateUserInfo() {
        var client = NetworkClient.getInstance();
        User user = client.getCurrentUser();
        if (user != null) {
            // SỬA DÒNG NÀY: HIỆN TÊN THẬT THAY VÌ USERNAME
            lblUsername.setText(user.getName());

            String avatarPath = user.getAvatar();
            if (avatarPath != null && !avatarPath.isEmpty()) {
                try {
                    imgAvatar.setImage(new Image(avatarPath));
                } catch (Exception e) {
                    fallbackAvatar();
                }
            } else {
                fallbackAvatar();
            }
        }
    }

    private void fallbackAvatar() {
        imgAvatar.setImage(new Image(getClass().getResourceAsStream("/image/avatar.png")));
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
        bntNotification.setOnAction(e -> SceneManager.changeScene("notifications.fxml"));

        // Logout
        btnLogout.setOnAction(e -> logout());
        btnLogout1.setOnAction(e -> logout());
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
