package org.example.educonnec_dacs4.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.example.educonnec_dacs4.client.NetworkClient;
import org.example.educonnec_dacs4.model.User;
import org.example.educonnec_dacs4.utils.SceneManager;

import java.io.File;

public class EditProfileController {

    @FXML private TextField txtName, txtEmail;
    @FXML private TextField txtUsername;     // Giữ lại nhưng disable
    @FXML private ImageView imgAvatar;

    private User user;
    private Runnable onUpdateSuccess;
    private String tempAvatarPath = "";

    public void setUser(User user, Runnable callback) {
        this.user = user;
        this.onUpdateSuccess = callback;

        txtName.setText(user.getName());
        txtUsername.setText(user.getUsername());
        txtEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        // KHÓA USERNAME – KHÔNG CHO SỬA
        txtUsername.setEditable(false);
        txtUsername.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #555;");

        loadAvatar();
    }

    private void loadAvatar() {
        imgAvatar.setImage(loadImage(user.getAvatar()));
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
    @FXML
    private void chooseAvatar() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Hình ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fc.showOpenDialog(imgAvatar.getScene().getWindow());
        if (file != null) {
            tempAvatarPath = file.toURI().toString();
            imgAvatar.setImage(new Image(tempAvatarPath));
        }
    }
    @FXML
    private void saveProfile() {
        String name = txtName.getText().trim();
        String email = txtEmail.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            alert("Vui lòng nhập đầy đủ họ tên và email!");
            return;
        }

        String avatar = tempAvatarPath.isEmpty() ? user.getAvatar() : tempAvatarPath;

        // CHỈ GỬI LỆNH – ĐỂ PROFILECONTROLLER XỬ LÝ
        NetworkClient.getInstance().send("UPDATE_PROFILE|" + name + "|" + email + "|" + avatar);

        // Tự động đóng modal sau 1 giây (để server kịp xử lý)
        Platform.runLater(() -> {
            try { Thread.sleep(800); } catch (Exception ignored) {}
            alertSuccess("Cập nhật hồ sơ thành công!");
            if (onUpdateSuccess != null) onUpdateSuccess.run();
            SceneManager.closeModal();
        });
    }

    @FXML
    private void cancelEdit() {
        SceneManager.closeModal();
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    private void alertSuccess(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}