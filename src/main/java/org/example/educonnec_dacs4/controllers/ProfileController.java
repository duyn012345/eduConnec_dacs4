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
        //  updateClock();
        loadCurrentUserInfo(); // Tải thông tin ngay khi vào
//        NetworkClient.getInstance().setOnMessageReceived((cmd, payload) -> {
//            if ("UPDATE_PROFILE_OK".equals(cmd)) {
//                Platform.runLater(this::updateProfileUI);
//            }
//        });

    }
    //    private void loadCurrentUserInfo() {
//        currentUser = client.getCurrentUser();
//
//        if (currentUser != null) {
//            updateProfileUI();
//        } else {
//            new Alert(Alert.AlertType.ERROR, "Không tìm thấy thông tin người dùng!").showAndWait();
//            return;
//        }
//
//        // DỌN DẸP LISTENER CŨ TRƯỚC KHI SET MỚI → SIÊU QUAN TRỌNG!!!
//        client.setOnMessageReceived(null); // XÓA CÁI CŨ ĐI
//
//        client.setOnMessageReceived((cmd, payload) -> Platform.runLater(() -> {
//            switch (cmd) {
//                case "UPDATE_PROFILE_OK" -> {
//                    // SỬ DỤNG GIỚI HẠN -1 ĐỂ ĐẢM BẢO SPLIT NHẬN ĐỦ CÁC CHUỖI RỖNG
//                    String[] p = payload.split("\\|", -1);
//
//                    // Đảm bảo có ít nhất 5 trường cơ bản (Name, Username, Email, Role, UserId)
//                    if (p.length < 5) {
//                        System.err.println("Lỗi phân tích UPDATE_PROFILE_OK: Thiếu trường cơ bản.");
//                        return;
//                    }
//
//                    // 1. Gán các trường cơ bản
//                    currentUser.setName(p[0]);
//                    currentUser.setUsername(p[1]);
//                    currentUser.setEmail(p[2]);
//
//                    // 2. Gán Role, có thể là trường rỗng
//                    currentUser.setRole(p.length > 3 ? p[3] : "Thành viên");
//
//                    // 3. Gán UserId (Yêu cầu phải có và phải là số)
//                    try {
//                        currentUser.setUserId(Integer.parseInt(p[4]));
//                    } catch (NumberFormatException e) {
//                        System.err.println("Lỗi phân tích UserId: " + p[4]);
//                        return;
//                    }
//
//                    // 4. Gán Avatar URL (Trường thứ 6)
//                    currentUser.setAvatar(p.length > 5 && !p[5].isEmpty() ? p[5] : null); // Gán null nếu trống
//
//                    // 5. Gán CreatedAt (Trường thứ 7)
//                    currentUser.setCreatedAt(p.length > 6 ? p[6] : "Chưa xác định");
//
//                    updateProfileUI();
//                }
//                case "USER_PROFILE_UPDATED" -> {
//                    String[] p = payload.split("\\|", 4);
//                    int userId = Integer.parseInt(p[0]);
//                    String newName = p[1];
//                    String newEmail = p[3];
//                    if (currentUser.getUserId() == userId) {
//                        currentUser.setName(newName);
//                        currentUser.setEmail(newEmail);
//                        updateProfileUI();
//                    }
//                }
//            }
//        }));
//    }
    private void setupGlobalListener() {
        // DỌN DẸP LISTENER CŨ TRƯỚC KHI SET MỚI
        client.setOnMessageReceived(null);

        client.setOnMessageReceived((cmd, payload) -> Platform.runLater(() -> {
            switch (cmd) {
                case "UPDATE_PROFILE_OK" -> {
                    // ... logic xử lý UPDATE_PROFILE_OK (đã sửa ở câu trước)
                    updateProfileUI();
                }
                case "USER_PROFILE_UPDATED" -> {
                    // ... logic xử lý USER_PROFILE_UPDATED
                    updateProfileUI();
                }
            }
        }));
    }

    private void loadCurrentUserInfo() {
        currentUser = client.getCurrentUser();

        if (currentUser != null) {
            updateProfileUI();
        } else {
            new Alert(Alert.AlertType.ERROR, "Không tìm thấy thông tin người dùng!").showAndWait();
            return;
        }

        // Gọi hàm thiết lập Listener toàn cục
        setupGlobalListener();
    }

    @FXML
    private void openEditProfile() {
        if (currentUser == null) {
            new Alert(Alert.AlertType.WARNING, "Không thể tải thông tin!").show();
            return;
        }

        SceneManager.showModal("editProfile.fxml", "Chỉnh sửa hồ sơ", controller -> {
            if (controller instanceof EditProfileController editCtrl) {
                // Thay vì chỉ gửi updateProfileUI, ta gửi cả setupGlobalListener
                editCtrl.setUser(currentUser, () -> {
                    updateProfileUI();
                    setupGlobalListener(); // ⬅️ QUAN TRỌNG: Thiếp lập lại Listener toàn cục
                });
            }
            return null;
        });
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
        if (path == null || path.trim().isEmpty()) {
            // Trường hợp 1: Không có đường dẫn, dùng ảnh mặc định từ resources
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/avatar.png")));
        }

        // Trường hợp 2: Có đường dẫn/URL. Ưu tiên tải từ URL công cộng (Cloudinary)
        if (path.startsWith("http://") || path.startsWith("https://")) {
            try {
                // Tải ảnh từ URL Cloudinary (tải bất đồng bộ)
                return new Image(path, true);
            } catch (Exception e) {
                System.err.println("Lỗi tải ảnh từ URL: " + path);
                // Tiếp tục sang fallback nếu URL lỗi
            }
        }

        // Trường hợp 3: Fallback
        try {
            // Cố gắng tải từ resource project
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/avatar.png")));
        } catch (Exception e) {
            System.err.println("Không tìm thấy ảnh mặc định.");
            return null;
        }
    }
    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        lblTimeDate.setText(now.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy")));
    }

//    @FXML
//    private void openEditProfile() {
//        if (currentUser == null) {
//            new Alert(Alert.AlertType.WARNING, "Không thể tải thông tin!").show();
//            return;
//        }
//
//        SceneManager.showModal("editProfile.fxml", "Chỉnh sửa hồ sơ", controller -> {
//            if (controller instanceof EditProfileController editCtrl) {
//                editCtrl.setUser(currentUser, this::updateProfileUI);
//            }
//            return null;
//        });
//    }

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
    }}
//package org.example.educonnec_dacs4.controllers;
//
//import javafx.application.Platform;
//import javafx.fxml.FXML;
//import javafx.scene.control.*;
//import javafx.scene.image.Image;
//import javafx.scene.image.ImageView;
//import org.example.educonnec_dacs4.client.NetworkClient;
//import org.example.educonnec_dacs4.model.User;
//import org.example.educonnec_dacs4.utils.SceneManager;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.Objects;
//
//public class ProfileController {
//
//    @FXML private Label lblName, lblUsername, lblStatus, lblTimeDate;
//    @FXML private Label lblDisplayName, lblDisplayUsername, lblDisplayEmail, lblDisplayRole, lblCreatedAt;
//    @FXML private ImageView imgAvatar;
//    @FXML private Button btnEdit, btnSeenHistory;
//
//    // Menu buttons
//    @FXML private Button btnHome, btnSearch, btnChat, btnGroupChat, btnFiles, btnLogout;
//
//    private final NetworkClient client = NetworkClient.getInstance();
//    private User currentUser;
//
//    @FXML
//    private void initialize() {
//        setupButtons();
//      //  updateClock();
//        loadCurrentUserInfo(); // Tải thông tin ngay khi vào
////        NetworkClient.getInstance().setOnMessageReceived((cmd, payload) -> {
////            if ("UPDATE_PROFILE_OK".equals(cmd)) {
////                Platform.runLater(this::updateProfileUI);
////            }
////        });
//
//    }
////    private void loadCurrentUserInfo() {
////        currentUser = client.getCurrentUser();
////
////        if (currentUser != null) {
////            updateProfileUI();
////        } else {
////            new Alert(Alert.AlertType.ERROR, "Không tìm thấy thông tin người dùng!").showAndWait();
////            return;
////        }
////
////        // DỌN DẸP LISTENER CŨ TRƯỚC KHI SET MỚI → SIÊU QUAN TRỌNG!!!
////        client.setOnMessageReceived(null); // XÓA CÁI CŨ ĐI
////
////        client.setOnMessageReceived((cmd, payload) -> Platform.runLater(() -> {
////            switch (cmd) {
////                case "UPDATE_PROFILE_OK" -> {
////                    // SỬ DỤNG GIỚI HẠN -1 ĐỂ ĐẢM BẢO SPLIT NHẬN ĐỦ CÁC CHUỖI RỖNG
////                    String[] p = payload.split("\\|", -1);
////
////                    // Đảm bảo có ít nhất 5 trường cơ bản (Name, Username, Email, Role, UserId)
////                    if (p.length < 5) {
////                        System.err.println("Lỗi phân tích UPDATE_PROFILE_OK: Thiếu trường cơ bản.");
////                        return;
////                    }
////
////                    // 1. Gán các trường cơ bản
////                    currentUser.setName(p[0]);
////                    currentUser.setUsername(p[1]);
////                    currentUser.setEmail(p[2]);
////
////                    // 2. Gán Role, có thể là trường rỗng
////                    currentUser.setRole(p.length > 3 ? p[3] : "Thành viên");
////
////                    // 3. Gán UserId (Yêu cầu phải có và phải là số)
////                    try {
////                        currentUser.setUserId(Integer.parseInt(p[4]));
////                    } catch (NumberFormatException e) {
////                        System.err.println("Lỗi phân tích UserId: " + p[4]);
////                        return;
////                    }
////
////                    // 4. Gán Avatar URL (Trường thứ 6)
////                    currentUser.setAvatar(p.length > 5 && !p[5].isEmpty() ? p[5] : null); // Gán null nếu trống
////
////                    // 5. Gán CreatedAt (Trường thứ 7)
////                    currentUser.setCreatedAt(p.length > 6 ? p[6] : "Chưa xác định");
////
////                    updateProfileUI();
////                }
////                case "USER_PROFILE_UPDATED" -> {
////                    String[] p = payload.split("\\|", 4);
////                    int userId = Integer.parseInt(p[0]);
////                    String newName = p[1];
////                    String newEmail = p[3];
////                    if (currentUser.getUserId() == userId) {
////                        currentUser.setName(newName);
////                        currentUser.setEmail(newEmail);
////                        updateProfileUI();
////                    }
////                }
////            }
////        }));
////    }
//private void setupGlobalListener() {
//    // DỌN DẸP LISTENER CŨ TRƯỚC KHI SET MỚI
//    client.setOnMessageReceived(null);
//
//    client.setOnMessageReceived((cmd, payload) -> Platform.runLater(() -> {
//        switch (cmd) {
//            case "UPDATE_PROFILE_OK" -> {
//                // ... logic xử lý UPDATE_PROFILE_OK (đã sửa ở câu trước)
//                updateProfileUI();
//            }
//            case "USER_PROFILE_UPDATED" -> {
//                // ... logic xử lý USER_PROFILE_UPDATED
//                updateProfileUI();
//            }
//        }
//    }));
//}
//
//    private void loadCurrentUserInfo() {
//        currentUser = client.getCurrentUser();
//
//        if (currentUser != null) {
//            updateProfileUI();
//        } else {
//            new Alert(Alert.AlertType.ERROR, "Không tìm thấy thông tin người dùng!").showAndWait();
//            return;
//        }
//
//        // Gọi hàm thiết lập Listener toàn cục
//        setupGlobalListener();
//    }
//
//    @FXML
//    private void openEditProfile() {
//        if (currentUser == null) {
//            new Alert(Alert.AlertType.WARNING, "Không thể tải thông tin!").show();
//            return;
//        }
//
//        SceneManager.showModal("editProfile.fxml", "Chỉnh sửa hồ sơ", controller -> {
//            if (controller instanceof EditProfileController editCtrl) {
//                // Thay vì chỉ gửi updateProfileUI, ta gửi cả setupGlobalListener
//                editCtrl.setUser(currentUser, () -> {
//                    updateProfileUI();
//                    setupGlobalListener(); // ⬅️ QUAN TRỌNG: Thiếp lập lại Listener toàn cục
//                });
//            }
//            return null;
//        });
//    }
//    private void updateProfileUI() {
//        if (currentUser == null) return;
//
//        lblName.setText(currentUser.getName());
//        lblUsername.setText( currentUser.getUsername());
//        lblStatus.setText("Online");
//        lblStatus.setStyle("-fx-text-fill: #28a745;");
//
//        lblDisplayName.setText(currentUser.getName());
//        lblDisplayUsername.setText(currentUser.getUsername());
//        lblDisplayEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "Chưa cập nhật");
//        lblDisplayRole.setText(currentUser.getRole());
//        lblCreatedAt.setText(currentUser.getCreatedAt()); // Có thể lấy từ DB sau
//
//        // Load avatar
//        Image avatarImage = loadImage(currentUser.getAvatar());
//        imgAvatar.setImage(avatarImage);
//    }
//
//    private Image loadImage(String path) {
//        if (path == null || path.trim().isEmpty()) {
//            // Trường hợp 1: Không có đường dẫn, dùng ảnh mặc định từ resources
//            return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/avatar.png")));
//        }
//
//        // Trường hợp 2: Có đường dẫn/URL. Ưu tiên tải từ URL công cộng (Cloudinary)
//        if (path.startsWith("http://") || path.startsWith("https://")) {
//            try {
//                // Tải ảnh từ URL Cloudinary (tải bất đồng bộ)
//                return new Image(path, true);
//            } catch (Exception e) {
//                System.err.println("Lỗi tải ảnh từ URL: " + path);
//                // Tiếp tục sang fallback nếu URL lỗi
//            }
//        }
//
//        // Trường hợp 3: Fallback
//        try {
//            // Cố gắng tải từ resource project
//            return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/avatar.png")));
//        } catch (Exception e) {
//            System.err.println("Không tìm thấy ảnh mặc định.");
//            return null;
//        }
//    }
//    private void updateClock() {
//        LocalDateTime now = LocalDateTime.now();
//        lblTimeDate.setText(now.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy")));
//    }
//
////    @FXML
////    private void openEditProfile() {
////        if (currentUser == null) {
////            new Alert(Alert.AlertType.WARNING, "Không thể tải thông tin!").show();
////            return;
////        }
////
////        SceneManager.showModal("editProfile.fxml", "Chỉnh sửa hồ sơ", controller -> {
////            if (controller instanceof EditProfileController editCtrl) {
////                editCtrl.setUser(currentUser, this::updateProfileUI);
////            }
////            return null;
////        });
////    }
//
//    private void setupButtons() {
//        btnHome.setOnAction(e -> SceneManager.changeScene("home.fxml"));
//        btnSearch.setOnAction(e -> SceneManager.changeScene("searchFriend.fxml"));
//        btnChat.setOnAction(e -> SceneManager.changeScene("chat.fxml"));
//        btnGroupChat.setOnAction(e -> SceneManager.changeScene("groupChat.fxml"));
//        btnFiles.setOnAction(e -> SceneManager.changeScene("fileDoc.fxml"));
//        btnLogout.setOnAction(e -> logout());
//    }
//
//    private void logout() {
//        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.setTitle("Đăng xuất");
//        alert.setHeaderText("Bạn có chắc chắn muốn đăng xuất?");
//        alert.showAndWait().ifPresent(res -> {
//            if (res == ButtonType.OK) {
//                client.disconnect();
//                SceneManager.changeScene("login.fxml");
//            }
//        });
//    }
//}