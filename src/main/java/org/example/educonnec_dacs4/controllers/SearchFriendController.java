package org.example.educonnec_dacs4.controllers;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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

    @FXML private VBox searchResultList;
    @FXML private Label lblSearchResultTitle;
    @FXML private ScrollPane searchScrollPane;    // <--- CẦN THÊM KHAI BÁO NÀY
    @FXML private ScrollPane suggestionScrollPane; // <--- KHAI BÁO ĐÃ CÓ
    @FXML private Label lblSuggestionTitle;

    @FXML private Label lblUsername;
    @FXML private Label lblTimeDate;
    @FXML private ImageView imgAvatar;
    @FXML private AnchorPane rootPane;
    @FXML private AnchorPane notificationPopup;
    @FXML private VBox notificationList;
    @FXML private Button btnCloseNotification;
    @FXML private Button bntNotification;
    @FXML private ImageView imgBell;
    @FXML private Label lblNotificationBadge;
    @FXML private TextField tfSearchInput; // <-- KHAI BÁO MỚI
    @FXML private Button btnSearchFr; // <-- KHAI BÁO MỚI (nếu cần)

    // Nút menu
    @FXML private Button btnHome, btnSearch, btnChat, btnGroupChat, btnFiles, btnLogout;

    private final List<Notification> notifications = new ArrayList<>();
    private final NetworkClient network = NetworkClient.getInstance();
    private boolean isNotificationOpen = false;
    private static final String DEFAULT_AVATAR = "/image/avatar.png";

    private Timeline debounceTimeline;
    private static final Duration DEBOUNCE_DELAY = Duration.millis(300); // Trì hoãn 300ms
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
        //network.requestNotifications();
        NetworkClient.getInstance().send("GET_UNREAD_COUNT");
        setupLiveSearchListener();
    }

    // 🔥 THÊM PHƯƠNG THỨC MỚI
    private void setupLiveSearchListener() {
        if (tfSearchInput != null) {
            debounceTimeline = new Timeline(new KeyFrame(DEBOUNCE_DELAY, e -> handleSearchAction()));
            debounceTimeline.setCycleCount(1); // Chỉ chạy một lần
            // Lắng nghe sự thay đổi nội dung trong TextField
            tfSearchInput.textProperty().addListener((observable, oldValue, newValue) -> {
                // Gọi hàm tìm kiếm ngay lập tức khi nội dung thay đổi
               // handleSearchAction();

                // TÙY CHỌN: Có thể thêm độ trễ (debounce) để tránh gửi quá nhiều request
                // Ví dụ: handleSearchActionWithDebounce(newValue);
                // 1. Dừng Timeline đang chạy (reset)
                if (debounceTimeline != null) {
                    debounceTimeline.stop();
                }

                // 2. Bắt đầu lại Timeline. Nếu người dùng tiếp tục gõ, Timeline sẽ bị reset.
                debounceTimeline.playFromStart();
            });
        }

        // TÙY CHỌN: Giữ lại sự kiện nút bấm nếu muốn tìm kiếm thủ công
        if (btnSearchFr != null) {
            btnSearchFr.setOnAction(e -> handleSearchAction());
        }
    }
    // ================== ĐỒNG HỒ ==================
    private void updateClock() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                lblTimeDate.setText(dtf.format(LocalDateTime.now()))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void updateUserInfo() {
        var client = NetworkClient.getInstance();
        User user = client.getCurrentUser();
        if (user != null) {
            // SỬA DÒNG NÀY: HIỆN TÊN THẬT THAY VÌ USERNAME
            lblUsername.setText(user.getName());
// GỌI HÀM TẢI ẢNH MỚI
            imgAvatar.setImage(loadAvatarFromPathOrUrl(user.getAvatar()));
        }
    }
    private Image loadAvatarFromPathOrUrl(String urlPath) {
        // 1. Kiểm tra URL có hợp lệ không
        if (urlPath != null && (urlPath.startsWith("http://") || urlPath.startsWith("https://"))) {
            try {
                // Tải ảnh từ URL Cloudinary (tải bất đồng bộ)
                return new Image(urlPath, true);
            } catch (Exception e) {
                System.err.println("Lỗi tải ảnh từ Cloudinary URL: " + urlPath);
                // Nếu lỗi khi tải từ URL, chuyển sang ảnh mặc định
            }
        }
        // 2. Tải ảnh mặc định (Fallback)
        try {
            // Luôn tải từ resource project
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream(DEFAULT_AVATAR)));
        } catch (NullPointerException e) {
            System.err.println("Không tìm thấy ảnh mặc định.");
            return null;
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
        bntNotification.setOnAction(e -> showNotificationPopup());

    }
    private void showNotificationPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/notifications.fxml"));
            Parent root = loader.load();

            // Lấy controller để truyền dữ liệu nếu cần
            NotificationController controller = loader.getController();

            Stage popupStage = new Stage();
            popupStage.setScene(new Scene(root));
            popupStage.initStyle(StageStyle.TRANSPARENT);
            popupStage.initModality(Modality.NONE); // Không block Home
            popupStage.setResizable(false);
            popupStage.setWidth(420);
            popupStage.setHeight(600);

            // Đặt vị trí popup ở giữa màn hình
            Stage mainStage = (Stage) bntNotification.getScene().getWindow();
            popupStage.setX(mainStage.getX() + mainStage.getWidth() / 2 - 210);
            popupStage.setY(mainStage.getY() + mainStage.getHeight() / 2 - 300);

            // Cho phép controller đóng popup
            controller.setPopupStage(popupStage);

            popupStage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
//                case "SUGGESTIONS" -> {
//                    suggestionList.getChildren().clear();
//                    List<User> suggestions = User.fromJsonArray(payload);
//                    for (User u : suggestions) {
//                        suggestionList.getChildren().add(createSuggestionItem(u));
//                    }
//                }
                case "SUGGESTIONS" -> {
                    List<User> users = User.fromJsonArray(payload);
                    String currentQuery = tfSearchInput.getText().trim();
                    // Quyết định hiển thị vào danh sách nào
                    VBox targetList = currentQuery.isEmpty() ? suggestionList : searchResultList;
                    targetList.getChildren().clear();

                    // Ẩn/Hiện các khu vực
                    if (currentQuery.isEmpty()) {
                        // CHẾ ĐỘ GỢI Ý MẶC ĐỊNH
                        searchScrollPane.setVisible(false);
                        lblSearchResultTitle.setVisible(false);

                        suggestionScrollPane.setVisible(true);
                        lblSuggestionTitle.setVisible(true);

                    } else {
                        // CHẾ ĐỘ TÌM KIẾM
                        suggestionScrollPane.setVisible(false);
                        lblSuggestionTitle.setVisible(false);

                        searchScrollPane.setVisible(true);
                        lblSearchResultTitle.setVisible(true);
                        lblSearchResultTitle.setText("Kết quả tìm kiếm cho: " + currentQuery);
                    }

                    // Thêm các mục tìm thấy
                    for (User u : users) {
                        targetList.getChildren().add(createSuggestionItem(u));
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
                  }

                case "NEW_NOTIFICATION" -> {
                    network.send("GET_UNREAD_COUNT");
                    Notification n = Notification.fromJson(payload);
                        // 2. BƯỚC SỬA: Kiểm tra n có phải là null không
                    if (n != null) {
                        notifications.add(0, n);
                        // updateNotificationIcon();

                        // Chỉ thực hiện kiểm tra getTitle() nếu n không null
                        if (n.getTitle() != null && n.getTitle().toLowerCase().contains("kết bạn")) {
                            requestFriendRequests();
                        }
                    } else {
                        System.err.println("Lỗi phân tích JSON Notification: " + payload);
                    }
                }
                case "NOTIFICATION_UNREAD_COUNT" -> handleUnreadCount(payload); // XỬ LÝ BADGE (MỚI)
              //  case "FRIEND_REQUEST_SUCCESS" -> showAlert("Thành công", "Đã gửi lời mời kết bạn!");
                case "FRIEND_REQUEST_FAIL", "ERROR" -> showAlert("Lỗi", payload);
                case "UPDATE_PROFILE_OK" -> {
                    User user = network.getCurrentUser();
                    if (user != null && payload != null) {
                        String[] p = payload.split("\\|");
                        if (p.length >= 6) {
                            user.setName(p[0]);
                            // ⚠️ SỬA ĐỔI: Cập nhật Avatar URL
                            String newAvatarUrl = p.length > 5 && !p[5].isEmpty() ? p[5] : null;
                            user.setAvatar(newAvatarUrl); // Cập nhật đối tượng User
                              lblUsername.setText(user.getName());
                            imgAvatar.setImage(loadAvatarFromPathOrUrl(user.getAvatar()));
                        }
                    }
                }
                case "FRIEND_REQUEST_REJECTED" -> {
                    // Server gửi: FRIEND_REQUEST_REJECTED|{username}|{user_id}
                    String[] parts = payload.split("\\|");
                    if (parts.length < 2) return;
                    int rejectedUserId;
                    try {
                        rejectedUserId = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ex) {
                        return;
                    }

                    // Cập nhật lại trạng thái nút bấm trong danh sách gợi ý
                    updateSuggestionItemStatus(rejectedUserId, "Thêm bạn");
                    showAlert("Thông báo", parts[0] + " đã từ chối lời mời kết bạn của bạn.");
                }
            }
        });
    }
    private AnchorPane createSuggestionItem(User u) {
        AnchorPane box = new AnchorPane();
        box.setPrefSize(280, 60);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        ImageView avatar = new ImageView(loadAvatarFromPathOrUrl(u.getAvatar()));
        avatar.setFitWidth(50); avatar.setFitHeight(50);
        avatar.setLayoutX(10); avatar.setLayoutY(10);

        Label name = new Label(u.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        name.setLayoutX(70); name.setLayoutY(20);

        Button btnAdd = new Button();
        btnAdd.setLayoutX(190); btnAdd.setLayoutY(15);
        btnAdd.setUserData(u.getUserId()); // Lưu userId vào nút để dễ dàng tìm kiếm sau này

        // ====================================================================
        // >>> LOGIC MỚI: KIỂM TRA TRẠNG THÁI TỪ SERVER/USER MODEL <<<
      //  String status = u.getFriendshipStatus() != null ? u.getFriendshipStatus() : "NONE";
        String status = u.getFriendshipStatus() != null ? u.getFriendshipStatus().toLowerCase() : "none";
        if ("pending".equalsIgnoreCase(status)) {
            // Lời mời đã được gửi và đang chờ
            btnAdd.setText("Đã gửi");
            btnAdd.setDisable(true);
            btnAdd.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #6c757d;");
        } else if ("accepted".equalsIgnoreCase(status)) {
            // Đã chấp nhận (server nên lọc người này ra, nhưng nếu có thì xử lý)
            btnAdd.setText("Bạn bè");
            btnAdd.setDisable(true);
            btnAdd.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;"); // Màu xanh lá cho bạn bè
        } else {
            // Trạng thái 'none' (hoặc 'rejected' nếu bạn muốn hiển thị lại nút sau khi bị từ chối)
            btnAdd.setText("Thêm bạn");
            btnAdd.setDisable(false);
            btnAdd.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        }
//        if ("REQUEST_SENT".equals(status)) {
//            btnAdd.setText("Đã gửi");
//            btnAdd.setDisable(true);
//            btnAdd.setStyle("-fx-background-color: gray; -fx-text-fill: white; -fx-font-weight: bold;");
//        } else if ("FRIENDS".equals(status)) {
//            btnAdd.setText("Bạn bè");
//            btnAdd.setDisable(true);
//            btnAdd.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
//        } else { // Trạng thái "NONE" hoặc mặc định
//            btnAdd.setText("Thêm bạn");
//            btnAdd.setDisable(false);
//            btnAdd.setStyle("-fx-background-color: #1877f2; -fx-text-fill: white; -fx-font-weight: bold;");
//        }
        // ====================================================================

        // Xử lý sự kiện khi click nút "Thêm bạn"
        if (!btnAdd.isDisabled()) { // Chỉ thêm sự kiện cho nút "Thêm bạn"
            btnAdd.setOnAction(e -> {
                network.send("FRIEND_REQUEST|" + u.getUsername());
                Platform.runLater(() -> {
                    // Đổi trạng thái nút ngay lập tức
                    btnAdd.setText("Đã gửi");
                    btnAdd.setDisable(true);
                    btnAdd.setStyle("-fx-background-color: gray; -fx-text-fill: white; -fx-font-weight: bold;");
                    showAlert("Thành công", "Đã gửi lời mời kết bạn đến " + u.getName() + "!");
                });
            });
        }

        box.getChildren().addAll(avatar, name, btnAdd);
        return box;
    }

    private void updateSuggestionItemStatus(int userId, String status) {
    for (javafx.scene.Node node : suggestionList.getChildren()) {
        if (node instanceof AnchorPane box) {
            // Duyệt qua các children để tìm nút Button
            for (javafx.scene.Node child : box.getChildren()) {
                if (child instanceof Button btn && btn.getUserData() != null) {
                    // Kiểm tra userId đã lưu trong UserData
                    if (btn.getUserData() instanceof Integer && (Integer) btn.getUserData() == userId) {

                        // Nếu status là "Thêm bạn" (khi bị từ chối)
                        if ("Thêm bạn".equals(status)) {
                            btn.setText("Thêm bạn");
                            btn.setDisable(false);
                            btn.setStyle("-fx-background-color: #1877f2; -fx-text-fill: white; -fx-font-weight: bold;");
                        }

                        return;
                    }
                }
            }
        }
    }
}
private void handleUnreadCount(String payload) {
    try {
        int count = Integer.parseInt(payload.trim());

        if (lblNotificationBadge != null) {
            lblNotificationBadge.setText(String.valueOf(count));
            lblNotificationBadge.setVisible(count > 0);
        }

        // Nếu vẫn muốn đổi màu imgBell
        if (imgBell != null) {
            String path = count > 0 ? "/image/notification_red.png" : "/image/notification_gray.png";
            imgBell.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)))); }
    } catch (Exception e) {
        System.err.println("Lỗi xử lý NOTIFICATION_UNREAD_COUNT: " + payload);
        if (lblNotificationBadge != null) lblNotificationBadge.setVisible(false);
    }
}
    private void requestFriendSuggestions() {
        network.send("GET_SUGGESTIONS");
    }
    private void requestFriendRequests() {
        network.send("GET_FRIEND_REQUESTS");
    }
    private AnchorPane createRequestItem(User u) {
        AnchorPane box = new AnchorPane();
        box.setPrefSize(280, 70);
        box.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-padding: 10;");

        ImageView avatar = new ImageView(loadAvatarFromPathOrUrl(u.getAvatar()));
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
    @FXML
    private void handleSearchAction() {
        String query = tfSearchInput.getText().trim();
        if (query.isEmpty()) {
            requestFriendSuggestions(); // Gửi GET_SUGGESTIONS (không tham số)
        } else {
            // Gửi yêu cầu tìm kiếm nâng cao lên Server (với từ khóa)
            network.send("GET_SUGGESTIONS|" + query);
        }
    }

//    private void requestFriendSuggestions() {
//        network.send("GET_SUGGESTIONS");
//    }
//    @FXML
//    private void handleSearchAction() {
//        String query = tfSearchInput.getText().trim();
//        if (query.isEmpty()) {
//            // Nếu rỗng, hiển thị lại gợi ý mặc định
//            requestFriendSuggestions();
//            // Đặt lại tiêu đề mặc định
//            //lblSuggestionTitle.setText("Gợi ý bạn bè");
//        } else {
//            // Gửi yêu cầu tìm kiếm nâng cao lên Server
//            network.send("GET_SUGGESTIONS|" + query);
//            lblSuggestionTitle.setText("Rỗng cho tìm kiếm: " + query);
//        }
//    }

}
