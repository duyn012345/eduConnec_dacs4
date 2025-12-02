package org.example.educonnec_dacs4.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.educonnec_dacs4.client.NetworkClient;
import org.example.educonnec_dacs4.utils.SceneManager;

public class RegisterController {

    @FXML private TextField tfName, tfUsername, tfEmail;
    @FXML private PasswordField pfPassword, pfPasswordConfirm;

    private final String SERVER_IP = "127.0.0.1";
    private final int SERVER_PORT = 9000;
    private final NetworkClient client = NetworkClient.getInstance();

    @FXML
    public void initialize() {
        // Tái sử dụng kết nối cũ nếu có, hoặc kết nối mới
        if (!client.isConnected()) {
            client.connect(SERVER_IP, SERVER_PORT);
        }

        // Listener chỉ đăng ký 1 lần
        client.setOnMessageReceived((cmd, payload) -> Platform.runLater(() -> handleServerMessage(cmd, payload)));
    }

    @FXML
    public void register() {
        String name = tfName.getText().trim();
        String username = tfUsername.getText().trim();
        String email = tfEmail.getText().trim();
        String pass = pfPassword.getText();
        String confirmPass = pfPasswordConfirm.getText();

        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showError("Vui lòng điền đầy đủ thông tin!");
            return;
        }
        if (!pass.equals(confirmPass)) {
            showError("Mật khẩu xác nhận không khớp!");
            return;
        }
        if (pass.length() < 6) {
            showError("Mật khẩu phải từ 6 ký tự trở lên!");
            return;
        }
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showError("Email không hợp lệ!");
            return;
        }

        client.send("REGISTER|" + name + "|" + username + "|" + email + "|" + pass);
    }

    private void handleServerMessage(String cmd, String payload) {
        switch (cmd) {
            case "REGISTER_OK":
                new Alert(Alert.AlertType.INFORMATION,
                        "Đăng ký thành công!\nBạn có thể đăng nhập ngay.", ButtonType.OK).show();
                try {
                    SceneManager.changeScene("login.fxml");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case "REGISTER_FAIL":
                showError("Đăng ký thất bại!\n" + payload);
                break;

            case "ERROR":
                showError("Lỗi server: " + payload);
                break;
        }
    }

    @FXML
    public void goLogin() {
        try {
            SceneManager.changeScene("login.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).show();
    }
}
//package org.example.educonnec_dacs4.controllers;
//
//import javafx.application.Platform;
//import javafx.event.ActionEvent;
//import javafx.fxml.FXML;
//import javafx.scene.control.*;
//import org.example.educonnec_dacs4.client.NetworkClient;
//import org.example.educonnec_dacs4.HelloApplication;
//import org.example.educonnec_dacs4.utils.SceneManager;
//
//public class RegisterController {
//    @FXML private TextField tfName;
//    @FXML private TextField tfUsername;
//    @FXML private TextField tfEmail;
//    @FXML private PasswordField pfPassword;
//    @FXML private PasswordField pfPasswordConfirm;
//
//    private final String SERVER_IP = "127.0.0.1";
//    private final int SERVER_PORT = 9000;
//
//    @FXML
//    public void register(ActionEvent event) {
//        String name = tfName.getText().trim();
//        String username = tfUsername.getText().trim();
//        String email = tfEmail.getText().trim();
//        String pass = pfPassword.getText();
//        String confirmPass = pfPasswordConfirm.getText();
//
//        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
//            showError("Vui lòng điền đầy đủ thông tin!");
//            return;
//        }
//
//        if (!pass.equals(confirmPass)) {
//            showError("Mật khẩu xác nhận không khớp!");
//            return;
//        }
//
//        if (pass.length() < 6) {
//            showError("Mật khẩu phải có ít nhất 6 ký tự!");
//            return;
//        }
//
//        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
//            showError("Email không hợp lệ!");
//            return;
//        }
//
//        NetworkClient client = NetworkClient.getInstance();
//
//        if (!client.connect(SERVER_IP, SERVER_PORT)) {
//            showError("Không thể kết nối đến server!");
//            return;
//        }
//
//        client.setOnMessageReceived((cmd, payload) -> Platform.runLater(() -> {
//            if ("REGISTER_OK".equals(cmd)) {
//                new Alert(Alert.AlertType.INFORMATION,
//                        "Đăng ký thành công!\nBạn có thể đăng nhập ngay bây giờ.",
//                        ButtonType.OK
//                ).show();
//                try {
//                    SceneManager.changeScene("login.fxml");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            } else if ("REGISTER_FAIL".equals(cmd)) {
//                showError("Đăng ký thất bại!\n" + payload);
//                client.disconnect();
//            }
//        }));
//
//        client.send("REGISTER|" + name + "|"  + username + "|" + email + "|" + pass);
//    }
//
//    @FXML
//    public void goLogin(ActionEvent event) throws Exception {
//        NetworkClient.getInstance().disconnect();
//        SceneManager.changeScene("login.fxml");
//    }
//
//    private void showError(String message) {
//        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).show();
//    }
//}
