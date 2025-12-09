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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Objects;
import java.util.function.BiConsumer;

public class EditProfileController {

    @FXML private TextField txtName, txtEmail;
    @FXML private TextField txtUsername;
    @FXML private ImageView imgAvatar;

    private User user;
    private Runnable onUpdateSuccess;
    // Biến tạm lưu URL avatar mới sau khi upload thành công (URL Cloudinary)
    private String tempAvatarPath = "";
    private BiConsumer<String, String> currentListener;

    public void setUser(User user, Runnable callback) {
        this.user = user;
        this.onUpdateSuccess = callback;

        txtName.setText(user.getName());
        txtUsername.setText(user.getUsername());
        txtEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        txtUsername.setEditable(false);
        txtUsername.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #555;");
        // Khởi tạo tempAvatarPath bằng avatar hiện tại
        this.tempAvatarPath = user.getAvatar() != null ? user.getAvatar() : "";

        loadAvatar();
    }
    private void loadAvatar() {
        imgAvatar.setImage(loadImage(user.getAvatar()));
    }

    // Đảm bảo hàm loadImage an toàn như trong ProfileController
    private Image loadImage(String path) {
        if (path == null || path.trim().isEmpty()) {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/avatar.png")));
        }

        if (path.startsWith("http://") || path.startsWith("https://")) {
            try {
                return new Image(path, true);
            } catch (Exception e) {
                System.err.println("Lỗi tải ảnh từ URL: " + path);
            }
        }

        try {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/avatar.png")));
        } catch (Exception e) {
            System.err.println("Không tìm thấy ảnh mặc định.");
            return null;
        }
    }

    // --- XỬ LÝ LƯU HỒ SƠ ---
    @FXML
    private void saveProfile() {
        String name = txtName.getText().trim();
        String email = txtEmail.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            alert("Vui lòng nhập đầy đủ họ tên và email!");
            return;
        }
        // Lấy URL Avatar mới nhất (từ tempAvatarPath) hoặc URL cũ (từ user.getAvatar())
        String avatarUrlToSend = tempAvatarPath.isEmpty() ? user.getAvatar() : tempAvatarPath;
        if (avatarUrlToSend == null) avatarUrlToSend = "";
        String payload = String.join("|", name, email, avatarUrlToSend);

        String finalAvatarUrlToSend = avatarUrlToSend;

        // B1: Gỡ listener hiện tại (để tránh xử lý tin nhắn cũ)
        NetworkClient.getInstance().setOnMessageReceived(null);

        // B2: Định nghĩa listener tạm thời cho tác vụ này
        currentListener = (cmd, payloadResp) -> {
            Platform.runLater(() -> {
                if ("UPDATE_PROFILE_OK".equals(cmd)) {
                    // CẬP NHẬT DỮ LIỆU CỤC BỘ TRÊN CLIENT
                    user.setName(name);
                    user.setEmail(email);
                    user.setAvatar(finalAvatarUrlToSend);
                    // Đóng form và gọi callback
                    closeOnSuccess();
                } else if ("UPDATE_PROFILE_FAIL".equals(cmd)) {
                    showAlert("Cập nhật thất bại", payloadResp);
                    // RẤT QUAN TRỌNG: Gỡ listener sau khi xử lý FAIL
                    NetworkClient.getInstance().setOnMessageReceived(null);
                }
            });
        };
        // B3: Gán listener và gửi lệnh
        NetworkClient.getInstance().setOnMessageReceived(currentListener);
        NetworkClient.getInstance().send("UPDATE_PROFILE|" + payload);
    }

    // --- XỬ LÝ UPLOAD AVATAR ---
    private String getBase64EncodedImage(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(bytes);
    }

    @FXML
    private void chooseAvatarAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(imgAvatar.getScene().getWindow());

        if (selectedFile != null) {
            try {
                // ... (HIỂN THỊ ẢNH TẠM THỜI)
                String base64Image = getBase64EncodedImage(selectedFile);

                NetworkClient.getInstance().setOnMessageReceived(null);

                // 2. Định nghĩa Listener chỉ cho Upload
                currentListener = (cmd, payload) -> Platform.runLater(() -> {
                    if ("UPLOAD_AVATAR_OK".equals(cmd)) {
                        // Cập nhật đường dẫn avatar tạm thời (URL Cloudinary)
                        tempAvatarPath = payload;
                        imgAvatar.setImage(loadImage(tempAvatarPath)); // Tải ảnh từ URL Cloudinary mới
                        alertSuccess("Tải ảnh lên Cloudinary thành công!");
                    } else if ("UPLOAD_AVATAR_FAIL".equals(cmd)) {
                        showAlert("Lỗi tải ảnh", payload);
                        loadAvatar(); // Quay lại ảnh cũ nếu thất bại
                    }
                    // Xóa listener sau khi hoàn thành
                    NetworkClient.getInstance().setOnMessageReceived(null);
                });
// 3. Đăng ký Listener và gửi lệnh
                NetworkClient.getInstance().setOnMessageReceived(currentListener);
                NetworkClient.getInstance().send("UPLOAD_AVATAR|" + base64Image);

            } catch (IOException e) {
                showAlert("Lỗi", "Không thể đọc file ảnh.");
            }
        }
    }

    private void closeOnSuccess() {
        Platform.runLater(() -> {
            alertSuccess("Cập nhật hồ sơ thành công!");
            // Gọi callback (onUpdateSuccess) để ProfileController refresh giao diện
            if (onUpdateSuccess != null) onUpdateSuccess.run();
            SceneManager.closeModal();
            // Sau khi đóng, xóa listener cuối cùng
            NetworkClient.getInstance().setOnMessageReceived(null);
        });
    }

    @FXML
    private void cancelEdit() {
        SceneManager.closeModal();
        // Sau khi hủy, xóa listener cuối cùng
        NetworkClient.getInstance().setOnMessageReceived(null);
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    private void alertSuccess(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}
