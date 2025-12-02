package org.example.educonnec_dacs4.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import org.example.educonnec_dacs4.client.NetworkClient;
import org.example.educonnec_dacs4.utils.SceneManager;

public class MenuController {

    // === NÚT CHÍNH (luôn luôn có trong cả 2 menu) ===
    @FXML private Button btnHome;
    @FXML private Button btnSearch;
    @FXML private Button btnChat;
    @FXML private Button btnGroupChat;
    @FXML private Button btnFiles;
    @FXML private Button btnLogout;

    // === NÚT ICON PHỤ (chỉ có trong menu_full.fxml) ===
    @FXML private Button btnHome1;
    @FXML private Button btnSearch1;
    @FXML private Button btnChat1;
    @FXML private Button btnGroupChat1;
    @FXML private Button btnFiles1;
    @FXML private Button btnLogout1;

    @FXML
    private void initialize() {
        // Gắn action cho các nút chính (luôn luôn tồn tại)
        btnHome.setOnAction(e -> SceneManager.changeScene("home.fxml"));
        btnSearch.setOnAction(e -> SceneManager.changeScene("searchFriend.fxml"));
        btnChat.setOnAction(e -> SceneManager.changeScene("chat.fxml"));
        btnGroupChat.setOnAction(e -> SceneManager.changeScene("groupChat.fxml"));
        btnFiles.setOnAction(e -> SceneManager.changeScene("fileDoc.fxml"));
        btnLogout.setOnAction(e -> logout());

        // Gắn action cho nút icon nếu chúng tồn tại (menu rộng)
        safeSetAction(btnHome1, btnHome);
        safeSetAction(btnSearch1, btnSearch);
        safeSetAction(btnChat1, btnChat);
        safeSetAction(btnGroupChat1, btnGroupChat);
        safeSetAction(btnFiles1, btnFiles);
        safeSetAction(btnLogout1, btnLogout);

        // Hiệu ứng hover đẹp (chỉ áp dụng cho nút tồn tại)
        addHoverEffect(btnHome, btnHome1);
        addHoverEffect(btnSearch, btnSearch1);
        addHoverEffect(btnChat, btnChat1);
        addHoverEffect(btnGroupChat, btnGroupChat1);
        addHoverEffect(btnFiles, btnFiles1);
    }

    // Helper: chỉ set action nếu nút tồn tại (không bị null)
    private void safeSetAction(Button iconBtn, Button mainBtn) {
        if (iconBtn != null && mainBtn != null) {
            iconBtn.setOnAction(mainBtn.getOnAction());
        }
    }

    private void logout() {
        new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc muốn đăng xuất không?", ButtonType.YES, ButtonType.NO)
                .showAndWait()
                .ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        NetworkClient.getInstance().disconnect();
                        SceneManager.changeScene("login.fxml");
                    }
                });
    }

    // Hover effect: chỉ chạy nếu cả 2 nút đều tồn tại
    private void addHoverEffect(Button textBtn, Button iconBtn) {
        if (textBtn == null) return;
        String hoverColor = "#34495e";
        String normal = "-fx-background-color: transparent;";

        textBtn.setStyle(normal);
        textBtn.setOnMouseEntered(e -> textBtn.setStyle("-fx-background-color: " + hoverColor + ";"));
        textBtn.setOnMouseExited(e -> textBtn.setStyle(normal));

        if (iconBtn != null) {
            iconBtn.setOnMouseEntered(e -> textBtn.setStyle("-fx-background-color: " + hoverColor + ";"));
            iconBtn.setOnMouseExited(e -> textBtn.setStyle(normal));
        }
    }
}