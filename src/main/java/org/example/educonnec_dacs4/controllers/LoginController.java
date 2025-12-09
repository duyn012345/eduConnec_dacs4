package org.example.educonnec_dacs4.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.educonnec_dacs4.client.NetworkClient;
import org.example.educonnec_dacs4.model.User;
import org.example.educonnec_dacs4.utils.SceneManager;
public class LoginController {
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;
    private final String SERVER_IP = "127.0.0.1";
    private final int SERVER_PORT = 9000;
    private final NetworkClient client = NetworkClient.getInstance();
    private java.util.function.BiConsumer<String, String> loginListener;

//    public void initialize() {
//        // Kết nối server
//        if (!client.isConnected()) {
//            if (!client.connect(SERVER_IP, SERVER_PORT)) {
//                showError("Không thể kết nối đến server!\nVui lòng kiểm tra server đang chạy trên port 9000.");
//                return;
//            }
//        }
//        client.setOnMessageReceived((cmd, payload) -> Platform.runLater(() -> {
//            System.out.println("Client nhận: " + cmd);
//
//            switch (cmd) {
//                case "LOGIN_OK" -> handleLoginSuccess(payload);
//                case "LOGIN_FAIL" -> showError("Đăng nhập thất bại!\n" + payload);
//            }
//        }));
//    }
    @FXML
    public void initialize() {
        // Kết nối server (Giữ nguyên)
        if (!client.isConnected()) {
            if (!client.connect(SERVER_IP, SERVER_PORT)) {
                showError("Không thể kết nối đến server!\nVui lòng kiểm tra server đang chạy trên port 9000.");
                return;
            }
        }

        // TẠO VÀ ĐĂNG KÝ LISTENER
        loginListener = (cmd, payload) -> Platform.runLater(() -> {
            System.out.println("LoginController nhận: " + cmd);
            switch (cmd) {
                case "LOGIN_OK" -> {
                    // HỦY ĐĂNG KÝ NGAY LẬP TỨC TRƯỚC KHI CHUYỂN SCENE
                    client.unsubscribe(loginListener);
                    handleLoginSuccess(payload);
                }
                case "LOGIN_FAIL" -> showError("Đăng nhập thất bại!\n" + payload);
                // BỎ HẲN default case
            }
        });

        // BỎ DÒNG client.setOnMessageReceived(...)
        // Thay bằng client.subscribe(...)
        client.subscribe(loginListener);
    }

    @FXML
    public void login() {
        String identifier = tfEmail.getText().trim();
        String password = pfPassword.getText();

        if (identifier.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }
        client.send("LOGIN|" + identifier + "|" + password);
    }
    private void handleLoginSuccess(String payload) {
        try {
            String[] parts = payload.split("\\|", -1);
            if (parts.length < 6) {
                showError("Dữ liệu từ server không hợp lệ!");
                return;
            }
            User user = new User();
            user.setName(parts[0]);
            user.setUsername(parts[1]);
            user.setEmail(parts[2]);
            user.setRole(parts[3]);
            user.setUserId(Integer.parseInt(parts[4]));
            user.setAvatar(parts[5]);
            user.setCreatedAt(parts.length > 6 ? parts[6] : "Chưa xác định");
            // LƯU USER HIỆN TẠI
            client.setCurrentUser(user);
           SceneManager.changeScene("home.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi xử lý đăng nhập: " + e.getMessage());
        }
    }
    @FXML
    public void goRegister() {
        SceneManager.changeScene("register.fxml");
    }
    private void showError(String msg) {
        Platform.runLater(() ->
                new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait()
        );
    }
}
//package org.example.educonnec_dacs4.controllers;
//
//import javafx.application.Platform;
//import javafx.fxml.FXML;
//import javafx.scene.control.*;
//import org.example.educonnec_dacs4.client.NetworkClient;
//import org.example.educonnec_dacs4.model.User;
//import org.example.educonnec_dacs4.utils.SceneManager;
//
//public class LoginController {
//
//    @FXML private TextField tfEmail;
//    @FXML private PasswordField pfPassword;
//
//    private final String SERVER_IP = "127.0.0.1";
//    private final int SERVER_PORT = 9000;
//    private final NetworkClient client = NetworkClient.getInstance();
//
//    @FXML
//    public void initialize() {
//        // Kết nối server
//        if (!client.isConnected()) {
//            if (!client.connect(SERVER_IP, SERVER_PORT)) {
//                showError("Không thể kết nối đến server!\nVui lòng kiểm tra server đang chạy trên port 9000.");
//                return;
//            }
//        }
//
//        // ĐĂNG KÝ LISTENER CHỈ MỘT LẦN – QUAN TRỌNG NHẤT!
//        client.setOnMessageReceived((cmd, payload) -> Platform.runLater(() -> {
//            System.out.println("Client nhận lệnh: " + cmd); // Debug
//
//            if ("LOGIN_OK".equals(cmd)) {
//                handleLoginSuccess(payload);
//            } else if ("LOGIN_FAIL".equals(cmd)) {
//                showError("Đăng nhập thất bại!\n" + payload);
//            }
//        }));
//    }
//
//    @FXML
//    public void login() {
//        String identifier = tfEmail.getText().trim();
//        String password = pfPassword.getText();
//
//        if (identifier.isEmpty() || password.isEmpty()) {
//            showError("Vui lòng nhập đầy đủ thông tin!");
//            return;
//        }
//
//        // Gửi lệnh đăng nhập
//        client.send("LOGIN|" + identifier + "|" + password);
//    }
//
//    // XỬ LÝ ĐĂNG NHẬP THÀNH CÔNG → CHUYỂN MÀN HÌNH
//    private void handleLoginSuccess(String payload) {
//        try {
//            String[] parts = payload.split("\\|", -1);
//            System.out.println("LOGIN_OK payload: " + payload);
//
//            if (parts.length < 6) {
//                showError("Dữ liệu từ server không đủ! (thiếu email/role/id)");
//                return;
//            }
//
//            User user = new User();
//            user.setName(parts[0]);              // 0: tên
//            user.setUsername(parts[1]);          // 1: username
//            user.setEmail(parts[2]);             // 2: email (mới thêm)
//            user.setRole(parts[3]);              // 3: role
//            user.setUserId(Integer.parseInt(parts[4]));  // 4: userId ← SỬA Ở ĐÂY
//            user.setAvatar(parts[5]);
//            // Thêm dòng này (phần còn lại giữ nguyên)
//            user.setCreatedAt(parts.length > 6 ? parts[6] : "Chưa xác định");// 5: avatar
//
//            client.setCurrentUser(user);
//            SceneManager.changeScene("home.fxml");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            showError("Lỗi xử lý đăng nhập: " + e.getMessage());
//        }
//    }
//
//    @FXML
//    public void goRegister() {
//        SceneManager.changeScene("register.fxml");
//    }
//
//    private void showError(String msg) {
//        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
//    }
//}

//package org.example.educonnec_dacs4.controllers;
//
//import javafx.application.Platform;
//import javafx.fxml.FXML;
//import javafx.scene.control.*;
//import org.example.educonnec_dacs4.client.NetworkClient;
//import org.example.educonnec_dacs4.model.User;
//import org.example.educonnec_dacs4.utils.SceneManager;
//
//public class LoginController {
//
//    @FXML private TextField tfEmail;
//    @FXML private PasswordField pfPassword;
//
//    private final String SERVER_IP = "127.0.0.1";
//    private final int SERVER_PORT = 9000;
//    private final NetworkClient client = NetworkClient.getInstance();
//
//    @FXML
//    public void initialize() {
//        // Chỉ kết nối 1 lần khi vào màn hình login
//        if (!client.isConnected()) {
//            if (!client.connect(SERVER_IP, SERVER_PORT)) {
//                showError("Không thể kết nối đến server!\nVui lòng kiểm tra server.");
//            }
//        }
//
//        // Đăng ký listener CHỈ MỘT LẦN duy nhất
//       // client.setOnMessageReceived((cmd, payload) -> Platform.runLater(() -> handleServerMessage(cmd, payload)));
//    }
//
//    @FXML
//    public void login() {
//        String identifier = tfEmail.getText().trim(); // có thể là email hoặc username
//        String password = pfPassword.getText();
//
//        if (identifier.isEmpty() || password.isEmpty()) {
//            showError("Vui lòng nhập đầy đủ thông tin!");
//            return;
//        }
//
//        if (!client.isConnected()) {
//            showError("Mất kết nối server. Đang thử lại...");
//            if (!client.connect(SERVER_IP, SERVER_PORT)) {
//                showError("Không thể kết nối đến server!");
//                return;
//            }
//        }
//
//        // Gửi lệnh đăng nhập
//        client.send("LOGIN|" + identifier + "|" + password);
//    }
//
//    @FXML
//    public void goRegister() {
//        try {
//            SceneManager.changeScene("register.fxml");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void showError(String message) {
//        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).show();
//    }
//}