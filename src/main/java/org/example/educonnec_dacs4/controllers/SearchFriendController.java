package org.example.educonnec_dacs4.controllers;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.educonnec_dacs4.client.NetworkClient;
import org.example.educonnec_dacs4.model.Notification;
import org.example.educonnec_dacs4.model.User;
import org.example.educonnec_dacs4.utils.SceneManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SearchFriendController {

    @FXML private AnchorPane suggestionPane;
    @FXML private VBox suggestionList;
    @FXML private VBox requestList;
    @FXML private Label lblUsername;
    @FXML private Label lblTimeDate;
    @FXML private ImageView imgAvatar;
    @FXML private AnchorPane rootPane;
    @FXML private AnchorPane notificationPopup;
    @FXML private VBox notificationList;
    @FXML private Button btnCloseNotification;
    @FXML private Button bntNotification;
    @FXML private ImageView imgBell;

    // Nút menu
    @FXML private Button btnHome, btnSearch, btnChat, btnGroupChat, btnFiles, btnLogout;

    private final List<Notification> notifications = new ArrayList<>();
    private final NetworkClient network = NetworkClient.getInstance();
    private boolean isNotificationOpen = false;
    private static final String DEFAULT_AVATAR = "/image/avatar.png";

    @FXML
    public void initialize() {
        updateClock();
        updateUserInfo();
        //loadCurrentUser();
        setupButtons();
        setupNetworkListener(); // ĐĂNG KÝ SUBSCRIBE CHUẨN

        // Load dữ liệu lần đầu
        requestFriendSuggestions();
        requestFriendRequests();
        network.requestNotifications();
    }

    // ================== ĐỒNG HỒ ==================
    private void updateClock() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                lblTimeDate.setText(dtf.format(LocalDateTime.now()))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    // ================== LOAD USER ==================
//    private void loadCurrentUser() {
//        User user = network.getCurrentUser();
//        if (user != null) {
//            lblUsername.setText(user.getName());
//            imgAvatar.setImage(loadImage(user.getAvatar()));
//        } else {
//            lblUsername.setText("Khách");
//            imgAvatar.setImage(loadImage(null));
//        }
//    }
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
    private Image loadImage(String path) {
        if (path == null || path.trim().isEmpty()) {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream(DEFAULT_AVATAR)));
        }
        try {
            // ĐÚNG CÁCH: dùng URL đầy đủ + timestamp để tránh cache
            String imageUrl = getClass().getResource("/image/" + path).toExternalForm() + "?v=" + System.currentTimeMillis();
            return new Image(imageUrl, true); // true = load async, mượt hơn
        } catch (Exception e) {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream(DEFAULT_AVATAR)));
        }
    }
    // ================== NÚT MENU ==================
    private void setupButtons() {
        btnHome.setOnAction(e -> SceneManager.changeScene("home.fxml"));
        btnSearch.setOnAction(e -> SceneManager.changeScene("searchFriend.fxml"));
        btnChat.setOnAction(e -> SceneManager.changeScene("chat.fxml"));
        btnGroupChat.setOnAction(e -> SceneManager.changeScene("groupChat.fxml"));
        btnFiles.setOnAction(e -> SceneManager.changeScene("fileDoc.fxml"));
        btnLogout.setOnAction(e -> logout());
        bntNotification.setOnAction(e -> SceneManager.changeScene("notifications.fxml"));

    }

    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc muốn đăng xuất?", ButtonType.OK, ButtonType.CANCEL);
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                network.disconnect();
                SceneManager.changeScene("login.fxml");
            }
        });
    }

    // ĐĂNG KÝ SUBSCRIBE TOÀN CỤC – CHỈ 1 LẦN DUY NHẤT!
    private void setupNetworkListener() {
        NetworkClient.getInstance().subscribe(this::handleMessage);
    }

    // XỬ LÝ TẤT CẢ TIN NHẮN TỪ SERVER – ĐÚNG TÊN, ĐÚNG THAM SỐ!
    private void handleMessage(String cmd, String payload) {
        Platform.runLater(() -> {
            switch (cmd) {
                case "SUGGESTIONS" -> {
                    suggestionList.getChildren().clear();
                    List<User> suggestions = User.fromJsonArray(payload);
                    for (User u : suggestions) {
                        suggestionList.getChildren().add(createSuggestionItem(u));
                    }
                }

                case "FRIEND_REQUEST_LIST" -> {
                    requestList.getChildren().clear();
                    List<User> requests = User.fromJsonArray(payload);
                    for (User u : requests) {
                        requestList.getChildren().add(createRequestItem(u));
                    }
                }

                case "NOTIFICATIONS" -> {
                    String[] parts = payload.split("\\|", 2);
                    String jsonList = parts.length > 1 ? parts[1] : "[]";
                    notifications.clear();
                    notifications.addAll(Notification.fromJsonArray(jsonList));
                    updateNotificationIcon();
                }

                case "NEW_NOTIFICATION" -> {
                    Notification n = Notification.fromJson(payload);
                    notifications.add(0, n);
                    updateNotificationIcon();
                    if (n.getTitle() != null && n.getTitle().toLowerCase().contains("kết bạn")) {
                        requestFriendRequests();
                    }
                }

                case "FRIEND_REQUEST_SUCCESS" -> showAlert("Thành công", "Đã gửi lời mời kết bạn!");

                case "FRIEND_REQUEST_FAIL", "ERROR" -> showAlert("Lỗi", payload);

                case "UPDATE_PROFILE_OK" -> {
                    User user = network.getCurrentUser();
                    if (user != null && payload != null) {
                        String[] p = payload.split("\\|");
                        if (p.length >= 6) {
                            user.setName(p[0]);
                            user.setAvatar(p.length > 5 && !p[5].isEmpty() ? p[5] : null);
                            lblUsername.setText(user.getName());
                            imgAvatar.setImage(loadImage(user.getAvatar()));
                        }
                    }
                }
            }
        });
    }

    private void updateNotificationIcon() {
        boolean hasUnread = notifications.stream().anyMatch(n -> !n.isRead());
        String path = hasUnread ? "/image/notification_red.png" : "/image/notification_gray.png";
        imgBell.setImage(loadImage(path));
    }

    private void requestFriendSuggestions() { network.send("GET_SUGGESTIONS"); }
    private void requestFriendRequests() { network.send("GET_FRIEND_REQUESTS"); }

    private AnchorPane createSuggestionItem(User u) {
        AnchorPane box = new AnchorPane();
        box.setPrefSize(280, 60);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        ImageView avatar = new ImageView(loadImage(u.getAvatar()));
        avatar.setFitWidth(45); avatar.setFitHeight(45);
        avatar.setLayoutX(10); avatar.setLayoutY(8);

        Label name = new Label(u.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        name.setLayoutX(70); name.setLayoutY(20);

        Button btnAdd = new Button("Thêm bạn");
        btnAdd.setLayoutX(190); btnAdd.setLayoutY(15);
        btnAdd.setStyle("-fx-background-color: #1877f2; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAdd.setOnAction(e -> {
            network.send("FRIEND_REQUEST|" + u.getUsername());
            btnAdd.setText("Đã gửi");
            btnAdd.setDisable(true);
            btnAdd.setStyle("-fx-background-color: gray;");
        });

        box.getChildren().addAll(avatar, name, btnAdd);
        return box;
    }

    private AnchorPane createRequestItem(User u) {
        AnchorPane box = new AnchorPane();
        box.setPrefSize(280, 70);
        box.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-padding: 10;");

        ImageView avatar = new ImageView(loadImage(u.getAvatar()));
        avatar.setFitWidth(50); avatar.setFitHeight(50);
        avatar.setLayoutX(10); avatar.setLayoutY(10);

        Label name = new Label(u.getName() + " muốn kết bạn");
        name.setStyle("-fx-font-weight: bold;");
        name.setLayoutX(75); name.setLayoutY(20);

        Button accept = new Button("Chấp nhận");
        accept.setLayoutX(75); accept.setLayoutY(45);
        accept.setStyle("-fx-background-color: #42b72a; -fx-text-fill: white;");
        accept.setOnAction(e -> {
            network.send("ACCEPT_FRIEND|" + u.getUserId());
            requestList.getChildren().remove(box);
            showAlert("Thành công", "Đã kết bạn với " + u.getName());
        });

        Button decline = new Button("Từ chối");
        decline.setLayoutX(180); decline.setLayoutY(45);
        decline.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
        decline.setOnAction(e -> {
            network.send("REJECT_FRIEND|" + u.getUserId());
            requestList.getChildren().remove(box);
        });

        box.getChildren().addAll(avatar, name, accept, decline);
        return box;
    }

    private void showAlert(String title, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).show();
    }



}

//package org.example.educonnec_dacs4.controllers;
//
//import javafx.animation.*;
//import javafx.application.Platform;
//import javafx.fxml.FXML;
//import javafx.scene.control.*;
//import javafx.scene.image.Image;
//import javafx.scene.image.ImageView;
//import javafx.scene.layout.AnchorPane;
//import javafx.scene.layout.VBox;
//import javafx.util.Duration;
//import org.example.educonnec_dacs4.client.NetworkClient;
//import org.example.educonnec_dacs4.model.Notification;
//import org.example.educonnec_dacs4.model.User;
//import org.example.educonnec_dacs4.utils.SceneManager;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//
//public class SearchFriendController {
//
//    @FXML private AnchorPane suggestionPane;
//    @FXML private VBox suggestionList; // List gợi ý bạn bè
//    @FXML private VBox requestList; // List lời mời đến bạn
//    @FXML private Label lblUsername;
//    @FXML private Label lblTimeDate;
//    @FXML private ImageView imgAvatar;
//    @FXML private AnchorPane rootPane;
//    @FXML private AnchorPane notificationPopup;
//    @FXML private VBox notificationList;
//    @FXML private Button btnCloseNotification;
//    @FXML private Button bntNotification;
//    @FXML private ImageView imgBell;
//    private boolean isNotificationOpen = false;
//
//
//    // Nút menu
//    @FXML private Button btnHome, btnSearch, btnChat, btnGroupChat, btnFiles, btnLogout;
//
//    private final List<Notification> notifications = new ArrayList<>();
//    private final NetworkClient network = NetworkClient.getInstance();
//    private static final String DEFAULT_AVATAR = "/image/avatar.png";
//
//    @FXML
//    public void initialize() {
//        updateClock();
//        loadCurrentUser();
//        setupButtons();
//        setupNetworkListener();
//
//        requestFriendSuggestions();
//        requestFriendRequests();
//        network.requestNotifications();
//        // Ẩn popup lúc đầu
//        if (notificationPopup != null) {
//            notificationPopup.setVisible(false);
//        }
//        // Nhấn chuông → mở/đóng popup
//        bntNotification.setOnAction(e -> toggleNotificationPopup());
//
//        // Nút X đóng popup
//        if (btnCloseNotification != null) {
//            btnCloseNotification.setOnAction(e -> closeNotificationPopup());
//        }
//        // Nhấn ra ngoài popup → tự đóng (rất chuyên nghiệp)
//        // SỬA LẠI ĐOẠN NÀY – RẤT QUAN TRỌNG!
//        if (notificationPopup != null) {
//            notificationPopup.setOnMouseClicked(e -> e.consume()); // ngăn đóng khi click vào popup
//        }
//        // Nhấn ra ngoài popup → tự động đóng
//        if (rootPane != null) {
//            rootPane.setOnMouseClicked(e -> {
//                if (isNotificationOpen && notificationPopup != null) {
//                    if (!notificationPopup.getBoundsInParent().contains(e.getSceneX(), e.getSceneY())) {
//                        closeNotificationPopup();
//                    }
//                }
//            });
//        }
//    }
//    // ================== Đồng hồ ==================
//    private void updateClock() {
//        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
//        lblTimeDate.setText(dtf.format(LocalDateTime.now()));
//        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
//                lblTimeDate.setText(dtf.format(LocalDateTime.now()))));
//        clock.setCycleCount(Animation.INDEFINITE);
//        clock.play();
//    }
//    // ================== Load user hiện tại ==================
//    private void loadCurrentUser() {
//        User user = network.getCurrentUser();
//        if(user != null) {
//            lblUsername.setText(user.getName());
//            imgAvatar.setImage(loadImage(user.getAvatar()));
//        } else {
//            imgAvatar.setImage(loadImage(null));
//        }
//    }
//    // ================== Nút menu ==================
//    private void setupButtons() {
//        btnHome.setOnAction(e -> SceneManager.changeScene("home.fxml"));
//        btnSearch.setOnAction(e -> SceneManager.changeScene("searchFriend.fxml"));
//        btnChat.setOnAction(e -> SceneManager.changeScene("chat.fxml"));
//        btnGroupChat.setOnAction(e -> SceneManager.changeScene("groupChat.fxml"));
//        btnFiles.setOnAction(e -> SceneManager.changeScene("fileDoc.fxml"));
//        btnLogout.setOnAction(e -> logout());
//    }
//    private void logout() {
//        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.setTitle("Đăng xuất");
//        alert.setHeaderText("Bạn có chắc muốn đăng xuất?");
//        alert.showAndWait().ifPresent(res -> {
//            if (res == ButtonType.OK) {
//                NetworkClient.getInstance().disconnect();
//                SceneManager.changeScene("login.fxml");
//            }
//        });
//    }
//    // ================== Listener mạng ==================
//    private void setupNetworkListener() {
//        network.setOnMessageReceived((cmd, payload) -> Platform.runLater(() -> {
//            switch(cmd) {
//                case "SUGGESTIONS": // gợi ý người dùng chưa là bạn
//                    suggestionList.getChildren().clear();
//                    List<User> suggestions = User.fromJsonArray(payload);
//                    for(User u : suggestions) {
//                        suggestionList.getChildren().add(createSuggestionItem(u));
//                    }
//                    break;
//                case "FRIEND_REQUEST_LIST": // danh sách lời mời đến bạn
//                    requestList.getChildren().clear();
//                    List<User> requests = User.fromJsonArray(payload);
//                    for(User u : requests) {
//                        requestList.getChildren().add(createRequestItem(u));
//                    }
//                    break;
//                case "NOTIFICATIONS":
//                    String[] parts = payload.split("\\|", 2);
//                    int unreadCount = Integer.parseInt(parts[0]);
//                    String jsonList = parts[1];
//                    notifications.clear();
//                    notifications.addAll(Notification.fromJsonArray(jsonList));
//                    updateNotificationIcon();
//                    break;
//                case "NEW_NOTIFICATION":
//                    Notification n = Notification.fromJson(payload);
//                    notifications.add(n);
//                    updateNotificationIcon();
//                    if (n.getTitle() != null && n.getTitle().toLowerCase().contains("kết bạn")) {
//                        requestFriendRequests(); // cực kỳ mượt!
//                    }
//                    break;
//                case "FRIEND_REQUEST_SUCCESS":
//                    // thông báo cho người gửi đã gửi thành công
//                    new Alert(Alert.AlertType.INFORMATION, "Đã gửi lời mời kết bạn!", ButtonType.OK).show();
//                    break;
//                case "FRIEND_REQUEST_FAIL":
//                    new Alert(Alert.AlertType.ERROR, payload, ButtonType.OK).show();
//                    break;
//                case "UPDATE_PROFILE_OK":
//                    User user = network.getCurrentUser();
//                    if (user != null && payload != null) {
//                        String[] p = payload.split("\\|");
//                        if (p.length >= 6) {
//                            user.setName(p[0]);
//                            user.setAvatar(p[5]);
//                        }
//                        lblUsername.setText(user.getName());
//                        Platform.runLater(() -> {
//                            Image newAvatar = new Image("/image/" + user.getAvatar() + "?" + System.currentTimeMillis(), true);
//                            imgAvatar.setImage(newAvatar);
//                        });
//                    }
//                    break;
//                case "ERROR":
//                    new Alert(Alert.AlertType.ERROR, payload, ButtonType.OK).show();
//                    break;
//                default:
//                    System.out.println("Unknown command: " + cmd);
//            }
//        }));
//    }
//    private void updateNotificationIcon() {
//        boolean hasUnread = notifications.stream().anyMatch(n -> !n.isRead());
//        String path = hasUnread ? "/image/notification_red.png" : "/image/notification_gray.png";
//        imgBell.setImage(loadImage(path));
//    }
//    // ================== Gợi ý bạn bè ==================
//    private void requestFriendSuggestions() {
//        network.send("GET_SUGGESTIONS");
//    }
//
//    private AnchorPane createSuggestionItem(User u) {
//        AnchorPane box = new AnchorPane();
//        box.setPrefSize(280, 60);
//
//        ImageView avatar = new ImageView(loadImage(u.getAvatar()));
//        avatar.setFitWidth(45);
//        avatar.setFitHeight(45);
//        avatar.setLayoutX(10);
//        avatar.setLayoutY(5);
//
//        Label name = new Label(u.getName());
//        name.setLayoutX(70);
//        name.setLayoutY(15);
//
//        Button btnAdd = new Button("Thêm");
//        btnAdd.setLayoutX(200);
//        btnAdd.setLayoutY(10);
//        btnAdd.setPrefSize(60, 30);
//        btnAdd.setOnAction(e -> {
//            network.send("FRIEND_REQUEST|" + u.getUsername()); // gửi ID người nhận
//            btnAdd.setText("Đã gửi");
//            btnAdd.setDisable(true);
//
//            FadeTransition ft = new FadeTransition(Duration.millis(300), box);
//            ft.setFromValue(1.0);
//            ft.setToValue(0.3);
//            ft.play();
//        });
//
//        box.getChildren().addAll(avatar, name, btnAdd);
//        return box;
//    }
//    // ================== Lời mời đến bạn ==================
//    private void requestFriendRequests() {
//        network.send("GET_FRIEND_REQUESTS"); // server gửi danh sách lời mời đến bạn
//    }
//
//    private AnchorPane createRequestItem(User u) {
//        AnchorPane box = new AnchorPane();
//        box.setPrefSize(280, 60);
//
//        ImageView avatar = new ImageView(loadImage(u.getAvatar()));
//        avatar.setFitWidth(45);
//        avatar.setFitHeight(45);
//        avatar.setLayoutX(10);
//        avatar.setLayoutY(5);
//
//        Label name = new Label(u.getName());
//        name.setLayoutX(70);
//        name.setLayoutY(15);
//
//        Button btnAccept = new Button("Chấp nhận");
//        btnAccept.setLayoutX(180);
//        btnAccept.setLayoutY(10);
//        btnAccept.setPrefSize(70, 30);
//        btnAccept.setOnAction(e -> {
//            network.send("ACCEPT_FRIEND|" + u.getUserId());  // ← ĐÚNG LỆNH
//            requestList.getChildren().remove(box);
//            showAlert("Thành công", "Đã chấp nhận lời mời từ " + u.getName());
//        });
//
//        Button btnDecline = new Button("Từ chối");
//        btnDecline.setLayoutX(260);
//        btnDecline.setLayoutY(10);
//        btnDecline.setPrefSize(60, 30);
//        btnDecline.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
//        btnDecline.setOnAction(e -> {
//            network.send("REJECT_FRIEND|" + u.getUserId());  // ← CHỈ DÙNG LỆNH NÀY
//            requestList.getChildren().remove(box);
//            showAlert("Đã từ chối", "Đã từ chối lời mời từ " + u.getName());
//        });
//
//        box.getChildren().addAll(avatar, name, btnAccept, btnDecline);
//        return box;
//    }
//    private void showAlert(String title, String message) {
//        Platform.runLater(() -> {
//            Alert a = new Alert(Alert.AlertType.INFORMATION);
//            a.setTitle(title);
//            a.setHeaderText(null);
//            a.setContentText(message);
//            a.show();
//        });
//    }
//    // ================== Helper load ảnh ==================
//    private Image loadImage(String path) {
//        try {
//            if(path != null && !path.isEmpty()) {
//                return new Image(getClass().getResourceAsStream("/image/" + path));
//            }
//        } catch (Exception ignored) {}
//        return new Image(getClass().getResourceAsStream(DEFAULT_AVATAR));
//    }
//    private void toggleNotificationPopup() {
//        if (isNotificationOpen) {
//            closeNotificationPopup();
//        } else {
//            openNotificationPopup();
//        }
//    }
//    private void openNotificationPopup() {
//        notificationList.getChildren().clear();
//        // Sắp xếp mới nhất lên đầu
//        notifications.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
//        for (Notification n : notifications) {
//            notificationList.getChildren().add(createNotificationItem(n));
//        }
//        notificationPopup.setVisible(true);
//        isNotificationOpen = true;
//        // Animation đẹp
//        notificationPopup.setOpacity(0);
//        FadeTransition ft = new FadeTransition(Duration.millis(250), notificationPopup);
//        ft.setFromValue(0);
//        ft.setToValue(1);
//        ft.play();
//    }
//    private void closeNotificationPopup() {
//        if (!notificationPopup.isVisible()) return;
//
//        FadeTransition ft = new FadeTransition(Duration.millis(200), notificationPopup);
//        ft.setFromValue(1);
//        ft.setToValue(0);
//        ft.setOnFinished(e -> notificationPopup.setVisible(false));
//        ft.play();
//        isNotificationOpen = false;
//    }
//    private AnchorPane createNotificationItem(Notification n) {
//        AnchorPane item = new AnchorPane();
//        item.setPrefHeight(70);
//        item.setStyle("-fx-background-color: " + (n.isRead() ? "#ffffff" : "#e3f2fd") +
//                "; -fx-background-radius: 10; -fx-padding: 10; -fx-border-radius: 10; -fx-border-color: #eee;");
//        Label title = new Label(n.getTitle());
//        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
//        title.setLayoutX(14);
//        title.setLayoutY(10);
//
//        Label content = new Label(n.getContent());
//        content.setStyle("-fx-font-size: 13; -fx-text-fill: #555;");
//        content.setLayoutY(30);
//        content.setPrefWidth(280);
//        content.setWrapText(true);
//
//        Label time = new Label(n.getCreatedAt().substring(0, 16).replace("T", " "));
//        time.setStyle("-fx-font-size: 11; -fx-text-fill: gray;");
//        time.setLayoutX(240);
//        time.setLayoutY(10);
//
//        item.getChildren().addAll(title, content, time);
//        // Nhấn vào thông báo → đánh dấu đã đọc
//        item.setOnMouseClicked(e -> {
//            if (!n.isRead()) {
//                network.send("MARK_NOTIFICATION_READ|" + n.getId());
//                n.setRead(true);
//                item.setStyle("-fx-background-color: white;");
//                updateNotificationIcon();
//            }
//        });
//
//        return item;
//    }
//    // Nút "Đánh dấu tất cả đã đọc"
//    public void markAllAsRead() {
//        network.send("MARK_ALL_NOTIFICATIONS_READ");
//        notifications.forEach(n -> n.setRead(true));
//        notificationList.getChildren().clear();
//        openNotificationPopup(); // refresh lại danh sách
//        updateNotificationIcon();
//    }
//}
