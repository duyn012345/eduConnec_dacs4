package org.example.educonnec_dacs4.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.educonnec_dacs4.client.NetworkClient;
import org.example.educonnec_dacs4.utils.SceneManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GroupChatController {
    @FXML
    private Label lblUsername;
    private Label lblName;
    @FXML private Label lblTimeDate;
    @FXML private ImageView imgAvatar;
    @FXML private Button btnEdit, btnSave, btnSeenHistory;
    // Nút chữ
    @FXML private Button btnHome, btnSearch, btnChat, btnGroupChat, btnFiles, btnLogout;

    @FXML
    private void initialize() {
        //updateUserInfo();
        updateClock();
        setupButtons();
    }

//    private void updateUserInfo() {
//        var client = NetworkClient.getInstance();
//        if (client.isLoggedIn()) {
//
//            lblUsername.setText(client.getCurrentUser().getUsername());
//
//            String avatarPath = client.getCurrentUser().getAvatar();
//            if (avatarPath != null && !avatarPath.isEmpty()) {
//                try {
//                    imgAvatar.setImage(new Image(avatarPath));
//                } catch (Exception e) {
//                    fallbackAvatar();
//                }
//            } else {
//                fallbackAvatar();
//            }
//        }
//    }

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
        // Tìm bạn
        btnSearch.setOnAction(e -> SceneManager.changeScene("searchFriend.fxml"));
        // Chat đơn
        btnChat.setOnAction(e -> SceneManager.changeScene("chat.fxml"));
        // Chat nhóm
        btnGroupChat.setOnAction(e -> SceneManager.changeScene("groupChat.fxml"));
        // File
        btnFiles.setOnAction(e -> SceneManager.changeScene("fileDoc.fxml"));
        // Logout
        btnLogout.setOnAction(e -> logout());
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
