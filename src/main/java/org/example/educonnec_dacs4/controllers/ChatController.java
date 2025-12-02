package org.example.educonnec_dacs4.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import org.example.educonnec_dacs4.client.NetworkClient;
import org.example.educonnec_dacs4.model.User;
import org.example.educonnec_dacs4.utils.SceneManager;

import java.time.LocalDateTime;
import java.util.*;

public class ChatController {

    @FXML private ListView<String> lvFriends;
    @FXML private ListView<String> lvOnline;
    @FXML private TextArea taChat;
    @FXML private TextField tfMessage;
    @FXML private Label lblChatWith;
    @FXML private Label lblStatus;
    @FXML private Label lblCurrentUser;
    @FXML private ImageView imgAvatar;
    @FXML private Button bntNotification;


    private static final String DEFAULT_AVATAR = "/image/avatar.png";

    @FXML private Button btnHome, btnSearch, btnChat, btnGroupChat, btnFiles, btnLogout;

    private final NetworkClient client = NetworkClient.getInstance();

    private String currentChatWith = null;
    private String currentChatUsername;
    private Integer currentConversationId = null;
    private final Map<String, Integer> conversationMap = new HashMap<>();
    private final ObservableList<String> friendList = FXCollections.observableArrayList();
    private final Set<String> onlineFriends = new HashSet<>();
    private final Map<String, String> nameToUsernameMap = new HashMap<>();
    private final Map<String, String> usernameToNameMap = new HashMap<>();

    @FXML
    public void initialize() {
        setupUI();
        updateUserInfo();
        setupButtons();
        setupFriendSelection();

        // KHÔNG ĐĂNG KÝ LISTENER Ở ĐÂY NỮA!
        // Listener đã được đăng ký ở LoginController → global cho toàn app
// ĐĂNG KÝ NHẬN TIN NHẮN TỪ NETWORKCLIENT – CHỈ 1 LẦN DUY NHẤT!
        NetworkClient.getInstance().subscribe(this::handleMessage);

        loadFriendAndOnlineList();

        tfMessage.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
                event.consume();
            }
        });
    }

    private void updateUserInfo() {
        var client = NetworkClient.getInstance();
        User user = client.getCurrentUser();
        if (user != null) {
            // SỬA DÒNG NÀY: HIỆN TÊN THẬT THAY VÌ USERNAME
            lblCurrentUser.setText(user.getName());

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

    private void setupUI() {
        lvFriends.setItems(friendList);
        User user = client.getCurrentUser();
        if (user != null) {
            lblCurrentUser.setText(user.getName());
            imgAvatar.setImage(loadImage(user.getAvatar()));
        } else {
            lblCurrentUser.setText("Khách");
            imgAvatar.setImage(loadImage(null));
        }
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

    private void setupButtons() {
        btnHome.setOnAction(e -> SceneManager.changeScene("home.fxml"));
        btnSearch.setOnAction(e -> SceneManager.changeScene("searchFriend.fxml"));
        btnChat.setOnAction(e -> {
            SceneManager.changeScene("chat.fxml");
            Platform.runLater(this::loadFriendAndOnlineList); // XÓA DÒNG NÀY ĐI!
        });
        btnGroupChat.setOnAction(e -> SceneManager.changeScene("groupChat.fxml"));
        btnFiles.setOnAction(e -> SceneManager.changeScene("fileDoc.fxml"));
        btnLogout.setOnAction(e -> logout());
        bntNotification.setOnAction(e -> SceneManager.changeScene("notifications.fxml"));

    }

    private void loadFriendAndOnlineList() {
        Platform.runLater(() -> {
            client.send("GET_FRIEND_LIST");
            client.send("REQUEST_ONLINE_LIST");
        });
    }

    // XÓA HẾT setupNetworkListener() – CHỈ ĐỂ LẠI HÀM XỬ LÝ RIÊNG
    private void handleMessage(String cmd, String payload) {
        switch (cmd) {
            case "FRIEND_LIST" -> updateFriendList(payload);
            case "ONLINE_USERS" -> updateOnlineList(payload);
            case "USER_ONLINE" -> userOnline(payload);
            case "USER_OFFLINE" -> userOffline(payload);
            case "CONVERSATION_ID" -> handleConversationId(payload);
            case "PRIVATE_MSG" -> handlePrivateMessage(payload);
            case "CHAT_HISTORY" -> handleChatHistory(payload);
            case "UPDATE_PROFILE_OK" -> handleUpdateProfile(payload);
            case "USER_NAME_CHANGED" -> {
                Platform.runLater(() -> {
                    System.out.println("Có người đổi tên → tự động cập nhật danh sách bạn bè!");
                    loadFriendAndOnlineList(); // ← CHỈ 1 DÒNG NÀY LÀ XONG 100%!
                });
            }
            case "ERROR" -> new Alert(Alert.AlertType.ERROR, payload, ButtonType.OK).show();
        }
    }

    // Tách từng hàm xử lý cho dễ bảo trì
    private void updateFriendList(String payload) {
        friendList.clear();
        usernameToNameMap.clear();
        nameToUsernameMap.clear();
        if (payload != null && !payload.trim().isEmpty()) {
            String[] items = payload.split(",");
            for (String item : items) {
                String[] parts = item.split("\\|", 3);
                if (parts.length < 3) continue;
                String name = parts[0].trim();
                String username = parts[1].trim();
                if (!username.equals(client.getUsername())) {
                    friendList.add(name);
                    nameToUsernameMap.put(name, username);
                    usernameToNameMap.put(username, name);
                }
            }
        }
    }

    private void updateOnlineList(String payload) {
        lvOnline.getItems().clear();
        onlineFriends.clear();
        if (payload != null && !payload.trim().isEmpty()) {
            String[] users = payload.split(",");
            for (String entry : users) {
                String[] parts = entry.split("\\|", 2);
                String name = parts.length > 1 ? parts[0].trim() : parts[0].trim();
                String username = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                if (!username.equals(client.getUsername())) {
                    onlineFriends.add(username);
                    usernameToNameMap.put(username, name);
                    lvOnline.getItems().add(name + " (Online)");
                }
            }
        }
        updateCurrentStatus();
    }

    private void userOnline(String username) {
        username = username.trim();
        if (!username.equals(client.getUsername())) {
            onlineFriends.add(username);
            String name = usernameToNameMap.getOrDefault(username, username);
            Platform.runLater(() -> {
                if (!lvOnline.getItems().contains(name + " (Online)")) {
                    lvOnline.getItems().add(name + " (Online)");
                }
            });
            updateCurrentStatus();
        }
    }

    private void userOffline(String username) {
        username = username.trim();
        onlineFriends.remove(username);
        String name = usernameToNameMap.getOrDefault(username, username);
        lvOnline.getItems().removeIf(item -> item.startsWith(name + " "));
        updateCurrentStatus();
    }

    private void handleConversationId(String payload) {
        String[] parts = payload.split("\\|", 2);
        if (parts.length == 2) {
            String targetUsername = parts[0].trim();
            int convId = Integer.parseInt(parts[1]);
            conversationMap.put(targetUsername, convId);
            client.send("GET_MESSAGES|" + convId);
        }
    }

    private void handlePrivateMessage(String payload) {
        String[] p = payload.split("\\|", 4);
        if (p.length < 4) return;

        int convId = Integer.parseInt(p[0]);
        String senderName = p[1];     // Đây là tên thật từ server
        String time = p[2];
        String content = p[3];

        // Kiểm tra có phải tin nhắn của cuộc trò chuyện hiện tại không
        Integer currentConvId = conversationMap.get(currentChatUsername);
        if (currentConvId == null || currentConvId != convId) return;

        // Nếu là mình gửi → hiện "Bạn", còn lại hiện tên thật
        String displayName = senderName.equals(client.getCurrentUser().getName()) ? "Bạn" : senderName;

        Platform.runLater(() -> {
            taChat.appendText(displayName + " [" + time + "]: " + content + "\n");
            taChat.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void handleChatHistory(String payload) {
        taChat.clear();
        if (payload == null || payload.isEmpty()) {
            taChat.appendText("Chưa có tin nhắn nào.\n");
        } else {
            String[] lines = payload.split("\n");
            for (String line : lines) {
                String[] p = line.split("\\|", 3);
                if (p.length < 3) continue;
                String senderUsername = p[0];
                String time = p[1];
                String content = p[2];
                String displayName = senderUsername.equals(client.getUsername())
                        ? "Bạn"
                        : usernameToNameMap.getOrDefault(senderUsername, senderUsername);
                taChat.appendText(displayName + " [" + time + "]: " + content + "\n");
            }
        }
        taChat.setScrollTop(Double.MAX_VALUE);
    }

    private void handleUpdateProfile(String payload) {
        User current = client.getCurrentUser();
        if (current == null || payload == null) return;
        String[] p = payload.split("\\|");
        if (p.length < 6) return;

        current.setName(p[0]);
        current.setAvatar(p[5].isEmpty() ? null : p[5]);

        Platform.runLater(() -> {
            lblCurrentUser.setText(current.getName());
            imgAvatar.setImage(loadImage(current.getAvatar()));
            loadFriendAndOnlineList();
        });
    }

    private void updateCurrentStatus() {
        if (currentChatWith != null) {
            String username = nameToUsernameMap.get(currentChatWith);
            if (username != null && onlineFriends.contains(username)) {
                lblStatus.setText("Đang hoạt động");
                lblStatus.setTextFill(javafx.scene.paint.Color.web("#00d400"));
            } else {
                lblStatus.setText("Hoạt động gần đây");
                lblStatus.setTextFill(javafx.scene.paint.Color.GRAY);
            }
        }
    }

    private void setupFriendSelection() {
        lvFriends.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(currentChatWith)) {
                currentChatWith = newValue;
                currentChatUsername = nameToUsernameMap.get(newValue); // ← lưu username thật
                lblChatWith.setText(newValue);
                updateCurrentStatus();
                if (currentChatUsername == null) return;
                Integer convId = conversationMap.get(currentChatUsername);
                taChat.clear();
                taChat.appendText("Đang tải tin nhắn...\n");
                if (convId != null) {
                    client.send("GET_MESSAGES|" + convId);
                } else {
                    client.send("GET_CONVERSATION|" + currentChatUsername);
                }
            }
        });
    }
    @FXML
    private void sendMessage() {
        if (currentChatWith == null) {
            new Alert(Alert.AlertType.WARNING, "Vui lòng chọn người để chat!").show();
            return;
        }
        String msg = tfMessage.getText().trim();
        if (msg.isEmpty()) return;
        String targetUsername = currentChatUsername; // ← dùng biến mới, không dùng map nữa
        if (targetUsername == null) return;
        Integer convId = conversationMap.get(targetUsername);
        if (convId == null) {
            client.send("CREATE_CONVERSATION|" + targetUsername + "|" + msg);
        } else {
            client.send("SEND_MESSAGE|" + convId + "|" + msg);
        }
        String time = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM"));
        taChat.appendText("Bạn [" + time + "]: " + msg + "\n");
        taChat.setScrollTop(Double.MAX_VALUE);
        tfMessage.clear();
    }

    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Đăng xuất");
        alert.setHeaderText("Bạn có chắc muốn đăng xuất không?");
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                conversationMap.clear();
                client.disconnect();
                SceneManager.changeScene("login.fxml");
            }
        });
    }
}
//package org.example.educonnec_dacs4.controllers;
//import javafx.application.Platform;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.fxml.FXML;
//import javafx.scene.control.*;
//import javafx.scene.image.Image;
//import javafx.scene.image.ImageView;
//import org.example.educonnec_dacs4.client.NetworkClient;
//import org.example.educonnec_dacs4.model.User;
//import org.example.educonnec_dacs4.utils.SceneManager;
//import java.time.LocalDateTime;
//import java.util.*;
//public class ChatController {
//    @FXML private ListView<String> lvFriends;
//    @FXML private ListView<String> lvOnline;
//    @FXML private TextArea taChat;
//    @FXML private TextField tfMessage;
//    @FXML private Label lblChatWith;
//    @FXML private Label lblStatus; // THÊM DÒNG NÀY
//    @FXML private Label lblCurrentUser;
//    @FXML private ImageView imgAvatar;
//    private static final String DEFAULT_AVATAR = "/image/avatar.png";
//    @FXML private Button btnHome, btnSearch, btnChat, btnGroupChat, btnFiles, btnLogout;
//    private final NetworkClient client = NetworkClient.getInstance();
//    private String currentChatWith = null;
//    private String currentChatUsername;
//    private boolean isListenerSetup = false; // THÊM DÒNG NÀY
//    private final Map<String, Integer> conversationMap = new HashMap<>(); // key = username, không phải tên hiển thị!
//    private final ObservableList<String> friendList = FXCollections.observableArrayList();
//    private final Set<String> onlineFriends = new HashSet<>();
//    private final Map<String, LocalDateTime> lastSeenMap = new HashMap<>();
//    private final Map<String, String> nameToUsernameMap = new HashMap<>();
//    private final Map<String, String> usernameToNameMap = new HashMap<>();
//    @FXML
//    public void initialize() {
//        setupUI();
//        setupButtons();
//        // CHỈ ĐĂNG KÝ LISTENER 1 LẦN DUY NHẤT
//        if (!isListenerSetup) {
//            setupNetworkListener();
//            isListenerSetup = true;
//        }
//       // setupNetworkListener();
//        setupFriendSelection();
//        // SIÊU QUAN TRỌNG: TỰ ĐỘNG LOAD DANH SÁCH KHI VÀO MÀN HÌNH CHAT
//        loadFriendAndOnlineList();
//        // Cho phép nhấn Enter để gửi tin
//        tfMessage.setOnKeyPressed(event -> {
//            switch (event.getCode()) {
//                case ENTER:
//                    sendMessage();
//                    event.consume(); // tránh xuống dòng trong TextField
//                    break;
//                default:
//                    break;
//            }
//        });
//    }
//    private void setupUI() {
//        lvFriends.setItems(friendList);
//        User user = client.getCurrentUser();
//        if (user != null) {
//            lblCurrentUser.setText(user.getName() + " (" + user.getUsername() + ")");
//            imgAvatar.setImage(loadImage(user.getAvatar()));
//              } else {
//            lblCurrentUser.setText("Khách");
//            imgAvatar.setImage(loadImage(null));
//        }
//    }
//    private Image loadImage(String path) {
//        try {
//            if(path != null && !path.isEmpty()) {
//                return new Image(getClass().getResourceAsStream("/image/" + path));
//            }
//        } catch (Exception ignored) {}
//        return new Image(getClass().getResourceAsStream(DEFAULT_AVATAR));
//    }
//    private void setupButtons() {
//        btnHome.setOnAction(e -> SceneManager.changeScene("home.fxml"));
//        btnSearch.setOnAction(e -> SceneManager.changeScene("searchFriend.fxml"));
//        // KHI NHẤN NÚT CHAT TRÊN MENU → TỰ LOAD DANH SÁCH BẠN BÈ
//        btnChat.setOnAction(e -> SceneManager.changeScene("chat.fxml"));
//        btnGroupChat.setOnAction(e -> SceneManager.changeScene("groupChat.fxml"));
//        btnFiles.setOnAction(e -> SceneManager.changeScene("fileDoc.fxml"));
//        btnLogout.setOnAction(e -> logout());
//    }
//    // GỌI LẠI DANH SÁCH BẠN BÈ + ONLINE (DÙNG Ở NHIỀU NƠI)
//    private void loadFriendAndOnlineList() {
//        Platform.runLater(() -> {
//            client.send("GET_FRIEND_LIST"); // Lấy danh sách bạn bè
//            client.send("REQUEST_ONLINE_LIST"); // YÊU CẦU SERVER GỬI LẠI DANH SÁCH ONLINE
//        });
//    }
//    private void setupNetworkListener() {
//        client.setOnMessageReceived((cmd, payload) -> Platform.runLater(() -> {
//            switch (cmd) {
//                case "LOGIN_OK" -> {
//                    String[] p = payload.split("\\|", 5);
//                    String displayName = p[0];
//                    String username = p[1];
//                    lblCurrentUser.setText(displayName + " (" + username + ")");
//                    usernameToNameMap.put(username, displayName);
//                    loadFriendAndOnlineList();
//                }
//                case "FRIEND_LIST" -> {
//                    friendList.clear();
//                    usernameToNameMap.clear();
//                    nameToUsernameMap.clear();
//                    if (payload != null && !payload.trim().isEmpty()) {
//                        String[] items = payload.split(",");
//                        for (String item : items) {
//                            String[] parts = item.split("\\|", 3);
//                            if (parts.length < 3) continue;
//                            String name = parts[0].trim();
//                            String username = parts[1].trim();
//                            // String id = parts[2];
//                            if (!username.equals(client.getUsername())) {
//                                friendList.add(name);
//                                nameToUsernameMap.put(name, username);
//                                usernameToNameMap.put(username, name);
//                            }
//                        }
//                    }
//                }
//                case "ONLINE_USERS" -> {
//                    lvOnline.getItems().clear();
//                    onlineFriends.clear();
//                    if (payload != null && !payload.trim().isEmpty()) {
//                        String[] users = payload.split(",");
//                        for (String entry : users) {
//                            String[] parts = entry.split("\\|", 2);
//                            String name = parts.length > 1 ? parts[0].trim() : parts[0].trim();
//                            String username = parts.length > 1 ? parts[1].trim() : parts[0].trim();
//                            if (!username.equals(client.getUsername())) {
//                                onlineFriends.add(username);
//                                usernameToNameMap.put(username, name);
//                                lvOnline.getItems().add(name + " (Online)");
//                            }
//                        }
//                    }
//                    updateCurrentStatus();
//                }
//                case "USER_ONLINE" -> {
//                    String username = payload.trim();
//                    if (!username.equals(client.getUsername())) {
//                        onlineFriends.add(username);
//                        String name = usernameToNameMap.getOrDefault(username, username);
//                        Platform.runLater(() -> {
//                            if (!lvOnline.getItems().contains(name + " (Online)")) {
//                                lvOnline.getItems().add(name + " (Online)");
//                            }
//                        });
//                        updateCurrentStatus();
//                    }
//                }
//                case "USER_OFFLINE" -> {
//                    String username = payload.trim();
//                    onlineFriends.remove(username);
//                    String name = usernameToNameMap.getOrDefault(username, username);
//                    lvOnline.getItems().removeIf(item -> item.startsWith(name + " "));
//                    updateCurrentStatus();
//                }
//                case "CONVERSATION_ID" -> {
//                    String[] parts = payload.split("\\|", 2);
//                    if (parts.length == 2) {
//                        String targetUsername = parts[0].trim(); // Đây là USERNAME thật từ server
//                        int convId = Integer.parseInt(parts[1]);
//                        // LƯU DỰA VÀO USERNAME THẬT, KHÔNG PHẢI TÊN HIỂN THỊ
//                        conversationMap.put(targetUsername, convId);
//                        client.send("GET_MESSAGES|" + convId);
//                    }
//                }
//                // XỬ LÝ TIN NHẮN REAL-TIME
//                case "PRIVATE_MSG" -> {
//                    String[] p = payload.split("\\|", 4);
//                    if (p.length < 4) return;
//                    int convId = Integer.parseInt(p[0]);
//                    String senderUsername = p[1];
//                    String time = p[2];
//                    String content = p[3];
//                    // Kiểm tra có đúng cuộc chat hiện tại không
//                    Integer currentConvId = conversationMap.get(currentChatUsername);
//                    if (currentConvId == null || currentConvId != convId) return;
//                    String displayName = senderUsername.equals(client.getUsername())
//                            ? "Bạn"
//                            : usernameToNameMap.getOrDefault(senderUsername, senderUsername);
//                    taChat.appendText(displayName + " [" + time + "]: " + content + "\n");
//                    taChat.setScrollTop(Double.MAX_VALUE);
//                }
//// XỬ LÝ LỊCH SỬ CHAT
//                case "CHAT_HISTORY" -> {
//                    taChat.clear();
//                    if (payload == null || payload.isEmpty()) {
//                        taChat.appendText("Chưa có tin nhắn nào.\n");
//                    } else {
//                        String[] lines = payload.split("\n");
//                        for (String line : lines) {
//                            String[] p = line.split("\\|", 3);
//                            if (p.length < 3) continue;
//                            String senderUsername = p[0];
//                            String time = p[1];
//                            String content = p[2];
//                            String displayName = senderUsername.equals(client.getUsername())
//                                    ? "Bạn"
//                                    : usernameToNameMap.getOrDefault(senderUsername, senderUsername);
//                            taChat.appendText(displayName + " [" + time + "]: " + content + "\n");
//                        }
//                    }
//                    taChat.setScrollTop(Double.MAX_VALUE);
//                }
//                case "UPDATE_PROFILE_OK" -> {
//                    User current = client.getCurrentUser();
//                    if (current == null || payload == null || payload.isEmpty()) return;
//
//                    String[] p = payload.split("\\|");
//                    if (p.length < 6) return; // Bảo vệ 100%
//
//                    current.setName(p[0]);
//                    current.setAvatar(p[5]); // avatar ở vị trí 5
//
//                    Platform.runLater(() -> {
//                        lblCurrentUser.setText(current.getName() + " (" + current.getUsername() + ")");
//
//                        // FORCE RELOAD ẢNH MỚI – CÁCH PRO NHẤT, KHÔNG BAO GIỜ BỊ CACHE
//                        String avatarPath = current.getAvatar();
//                        if (avatarPath != null && !avatarPath.isEmpty()) {
//                            String url = "/image/" + avatarPath + "?" + System.currentTimeMillis();
//                            Image newImg = new Image(url, true); // true = load async, không giật
//                            imgAvatar.setImage(newImg);
//                        }
//
//                        // Cập nhật tên realtime ở mọi nơi
//                        loadFriendAndOnlineList();
//                    });
//                }
//                case "ERROR" -> new Alert(Alert.AlertType.ERROR, payload, ButtonType.OK).show();
//            }
//        }));
//    }private String getCurrentUsername() {
//        String text = lblCurrentUser.getText();
//        if (text == null) return client.getUsername();
//        int start = text.lastIndexOf("(");
//        int end = text.lastIndexOf(")");
//        if (start != -1 && end != -1 && end > start + 1) {
//            return text.substring(start + 1, end);
//        }
//        return client.getUsername() != null ? client.getUsername() : "unknown";
//    }
//    private void updateCurrentStatus() {
//        if (currentChatWith != null) {
//            String username = nameToUsernameMap.get(currentChatWith);
//            if (username != null && onlineFriends.contains(username)) {
//                lblStatus.setText("Đang hoạt động");
//                lblStatus.setTextFill(javafx.scene.paint.Color.web("#00d400"));
//            } else {
//                lblStatus.setText("Hoạt động gần đây");
//                lblStatus.setTextFill(javafx.scene.paint.Color.GRAY);
//            }
//        }
//    }
//    private void setupFriendSelection() {
//        lvFriends.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
//            if (newValue != null && !newValue.equals(currentChatWith)) {
//                currentChatWith = newValue;
//                currentChatUsername = nameToUsernameMap.get(newValue);
//                lblChatWith.setText(newValue);
//                updateCurrentStatus();
//
//                if (currentChatUsername == null) return;
//
//                Integer convId = conversationMap.get(currentChatUsername);
//
//                taChat.clear(); // XÓA DỮ LIỆU CŨ
//                taChat.appendText("Đang tải tin nhắn...\n");
//
//                if (convId != null) {
//                    // ĐÃ CÓ CONVID → CHỈ LẤY LỊCH SỬ 1 LẦN DUY NHẤT
//                    client.send("GET_MESSAGES|" + convId);
//                } else {
//                    // CHƯA CÓ → TẠO MỚI → SERVER SẼ GỬI CONVERSATION_ID → SAU ĐÓ TỰ ĐỘNG GỌI GET_MESSAGES
//                    client.send("GET_CONVERSATION|" + currentChatUsername);
//                }
//            }
//        });
//    }
//    @FXML
//    private void sendMessage() {
//        if (currentChatWith == null) {
//            new Alert(Alert.AlertType.WARNING, "Vui lòng chọn người để chat!").show();
//            return;
//        }
//        String msg = tfMessage.getText().trim();
//        if (msg.isEmpty()) return;
//        String targetUsername = currentChatUsername; // ← dùng biến mới, không dùng map nữa
//        if (targetUsername == null) return;
//        Integer convId = conversationMap.get(targetUsername);
//        if (convId == null) {
//            client.send("CREATE_CONVERSATION|" + targetUsername + "|" + msg);
//        } else {
//            client.send("SEND_MESSAGE|" + convId + "|" + msg);
//        }
//        String time = java.time.LocalDateTime.now()
//                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM"));
//        taChat.appendText("Bạn [" + time + "]: " + msg + "\n");
//        taChat.setScrollTop(Double.MAX_VALUE);
//        tfMessage.clear();
//    }
//    private void logout() {
//        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.setTitle("Đăng xuất");
//        alert.setHeaderText("Bạn có chắc muốn đăng xuất không?");
//        alert.showAndWait().ifPresent(res -> {
//            if (res == ButtonType.OK) {
//                conversationMap.clear();
//                client.disconnect();
//                SceneManager.changeScene("login.fxml");
//            }
//        });
//    }
//}