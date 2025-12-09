package org.example.educonnec_dacs4.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.educonnec_dacs4.client.NetworkClient;
import org.example.educonnec_dacs4.model.User;
import org.example.educonnec_dacs4.utils.CloudinaryClientUploader;
import org.example.educonnec_dacs4.utils.SceneManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.Collectors; // ĐẢM BẢO CÓ IMPORT NÀY

public class ChatController {

    @FXML private ListView<FriendItem> lvFriends;
    @FXML private ListView<String> lvOnline;
  //  @FXML private TextArea taChat;
    @FXML private ListView<HBox> lvChatHistory;
    @FXML private TextField tfMessage;
    @FXML private Label lblChatWith;
    @FXML private Label lblStatus;
    @FXML private Label lblCurrentUser;
    @FXML private ImageView imgAvatar;
    @FXML private Button bntNotification;
    @FXML private Button btnCreateGoup;
    @FXML private Button btnHome, btnSearch, btnChat, btnGroupChat, btnFiles, btnLogout;
    @FXML private Label lblNotificationBadge;
    @FXML private ImageView imgBell;
    @FXML private ImageView imgChatAvatar;
    @FXML private Button btnCallVideo;
    @FXML private Button btnSendFile;
    @FXML private ProgressBar pbUploadProgress;
    @FXML private Button btnSearchChat;
    @FXML private TextField tfSearchFriend;

    private final NetworkClient client = NetworkClient.getInstance();
    private static final String DEFAULT_AVATAR = "/image/avatar.png";
    private static final String GROUP_AVATAR = "/image/group_avatar.png"; // THÊM DÒNG NÀY (Nếu bạn muốn avatar nhóm khác default)
    private Stage videoCallStage;
    private FriendItem currentlyHandlingSelection = null; // Thêm biến trạng thái toàn cục
    private File fileToUpload;
    private HBox tempSendingMessage = null;
    // Class con để lưu thông tin bạn bè và nhóm chat
    private static class FriendItem {
        private final String name;
        private final String username; // Null nếu là nhóm
        private final String avatar;
        private boolean isOnline;
        private final boolean isGroup;
        private Integer convId;
        private boolean hasNewMessage = false;

        // Constructor cho chat 1-1
        FriendItem(String name, String username, String avatar, boolean online) {
            this.name = name; this.username = username; this.avatar = avatar; this.isOnline = online;
            this.isGroup = false;
        }
        // Constructor cho chat nhóm
        FriendItem(String name, Integer convId) {
            this.name = name; this.convId = convId;
            this.username = null; this.isGroup = true;
            this.avatar = GROUP_AVATAR; // SỬ DỤNG GROUP_AVATAR
            this.isOnline = true;
        }

        public String getName() { return name; }
        public String getUsername() { return username; }
        public String getAvatar() { return avatar; }
        public boolean isOnline() { return isOnline; }
        public boolean isGroup() { return isGroup; }
        public Integer getConvId() { return convId; }
        public void setOnline(boolean online) { this.isOnline = online; }
        public void setConvId(Integer convId) { this.convId = convId; }
        public boolean hasNewMessage() { return hasNewMessage; }
        public void setNewMessage(boolean newMessage) { this.hasNewMessage = newMessage; }
        @Override
        public String toString() { return name; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FriendItem that = (FriendItem) o;

            // 1. Nếu cả hai đều là NHÓM, SO SÁNH bằng CONVERSATION ID (ConvId).
            // ConvId là định danh duy nhất, không phải TÊN nhóm.
            if (this.isGroup() && that.isGroup()) {
                // Cần đảm bảo convId không null trước khi so sánh, mặc dù nó luôn phải có
                return Objects.equals(this.convId, that.convId);
            }
            // 2. Nếu cả hai đều là CHAT 1-1, SO SÁNH bằng USERNAME.
            else if (!this.isGroup() && !that.isGroup()) {
                return Objects.equals(this.username, that.username);
            }
            return false;
        }
        @Override
        public int hashCode() {
           // return Objects.hash(isGroup() ? name : username, isGroup);
            if (isGroup()) {
                // Dùng convId để tạo hash cho nhóm
                return Objects.hash(convId, true);
            } else {
                // Dùng username để tạo hash cho chat 1-1
                return Objects.hash(username, false);
            }
        }
    }
    private final Set<String> onlineFriends = new HashSet<>();
    private final Map<String, FriendItem> usernameToFriendItemMap = new HashMap<>();
   // private final Map<String, FriendItem> groupNameToFriendItemMap = new HashMap<>();
    private final Map<String, Integer> conversationMap = new HashMap<>();
    private final Map<String, String> nameToUsernameMap = new HashMap<>();
    private final Map<Integer, FriendItem> groupIdToFriendItemMap = new HashMap<>();
    private final Map<String, FriendItem> groupNameToFriendItemMap = new HashMap<>();
    private final ObservableList<FriendItem> allFriendsAndGroups = FXCollections.observableArrayList();
    private String currentChatUsername;
    private Integer currentConversationId;

    @FXML
    public void initialize() {
        lvFriends.setItems(allFriendsAndGroups);
        setupCellFactories();
        setupButtons();
        setupSelectionListeners();
        NetworkClient.getInstance().subscribe(this::handleMessage);
        loadFriendAndOnlineList();
        tfMessage.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendMessage();
                e.consume();
            }
        });
        tfSearchFriend.textProperty().addListener((observable, oldValue, newValue) -> {
            handleSearch(newValue);
        });
        updateUserInfo();
        NetworkClient.getInstance().send("GET_UNREAD_COUNT");
    }

    private void setupButtons() {
        btnHome.setOnAction(e -> SceneManager.changeScene("home.fxml"));
        btnSearch.setOnAction(e -> SceneManager.changeScene("searchFriend.fxml"));
        btnChat.setOnAction(e -> SceneManager.changeScene("chat.fxml"));
        btnGroupChat.setOnAction(e -> SceneManager.changeScene("groupChat.fxml"));
        btnFiles.setOnAction(e -> SceneManager.changeScene("fileDoc.fxml"));
        btnLogout.setOnAction(e -> logout());
        bntNotification.setOnAction(e -> showNotificationPopup());
        btnCreateGoup.setOnAction(e -> showCreateGroupPopup());
        btnCallVideo.setOnAction(e -> startVideoCall());
        btnSendFile.setOnAction(e -> handleSendFile());
    }
    private void setupSelectionListeners() {
        lvFriends.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // Chỉ xử lý khi mục mới được chọn (newVal != null)
            if (newVal != null && newVal != oldVal) {
                   Platform.runLater(() -> {
                       // Xử lý logic chính (handleFriendSelection)
                    handleFriendSelection(newVal);
                });
            }
        });
        // 2. LISTENER CHO DANH SÁCH NGƯỜI DÙNG ONLINE (lvOnline)
        // Xử lý việc chọn một người dùng online để chuyển sang chat 1-1 với họ.
        lvOnline.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            // Chỉ xử lý khi có mục mới được chọn và mục mới khác mục cũ
            if (newValue != null && newValue != oldValue) {
                String selectedName = newValue;
                String username = nameToUsernameMap.get(selectedName);
                // Tìm FriendItem 1-1 tương ứng trong allFriendsAndGroups
                FriendItem selectedItem = usernameToFriendItemMap.get(username);
//                FriendItem selectedItem = allFriendsAndGroups.stream()
//                        .filter(item -> !item.isGroup() && item.getName().equals(selectedName))
//                        .findFirst().orElse(null);

                if (selectedItem != null) {
                Platform.runLater(() -> {
                        // Xóa chọn ở lvOnline để tránh vòng lặp kích hoạt
                        lvOnline.getSelectionModel().clearSelection();

                        // Chọn mục tương ứng trong lvFriends.
                        // Việc này sẽ kích hoạt listener của lvFriends (mục 1).
                        lvFriends.getSelectionModel().select(selectedItem);
                        lvFriends.scrollTo(lvFriends.getSelectionModel().getSelectedIndex());
                    });
                }
            }
        });
    }
    private void setupCellFactories() {
        // DANH SÁCH BẠN BÈ VÀ NHÓM (DÙNG FriendItem)
        lvFriends.setCellFactory(lv -> new ListCell<FriendItem>() {
            private final ImageView img = new ImageView();
            private final Label name = new Label();
            private final Label status = new Label();
            private final HBox box;
            {
                img.setFitWidth(40); img.setFitHeight(40);
                img.setStyle("-fx-shape: circle;");
                name.setStyle("-fx-font-weight: bold;");
               // status.setStyle("-fx-font-size: 11;");
                box = new HBox(10, img, new VBox(name));
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
            @Override
            protected void updateItem(FriendItem item, boolean empty) {
                super.updateItem(item, empty);
                // XÓA MẶC ĐỊNH
                String defaultNameStyle = "-fx-font-weight: bold;";
                String defaultBoxStyle = "";
                name.setStyle(defaultNameStyle);
                box.setStyle(defaultBoxStyle);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    name.setText(item.getName());
                    // --- LOGIC HIỂN THỊ BADGE (TIN NHẮN MỚI) ---
                    if (item.hasNewMessage()) {
                        name.setStyle(defaultNameStyle + " -fx-fill: #1a73e8;"); // In đậm và đổi màu tên
                        box.setStyle("-fx-background-color: #e6f7ff;");          // Tô màu nền nhẹ
                    } else {
                        name.setStyle(defaultNameStyle);
                        box.setStyle(defaultBoxStyle);
                    }
                      if (item.isGroup()) {
                        // HIỂN THỊ CHO NHÓM CHAT
                        status.setText("Nhóm chat");
                        status.setTextFill(Color.GRAY);
                        img.setImage(loadAvatar(GROUP_AVATAR));
                    } else {
                        // HIỂN THỊ CHO CHAT 1-1
                        boolean online = onlineFriends.contains(item.getUsername());
                          status.setText(online ? "Đang hoạt động" : "Offline");
                          status.setTextFill(online ? Color.web("#00d400") : Color.GRAY);
                       img.setImage(loadAvatar(item.getAvatar()));
                    }
                    setGraphic(box);
                }
            }
        });
        // LIST ONLINE (DÙNG String)
        lvOnline.setCellFactory(lv -> new ListCell<String>() {
            private final ImageView img = new ImageView();
            private final Label nameLbl = new Label();
            private final HBox hbox;

            {
                img.setFitWidth(40); img.setFitHeight(40);
                img.setStyle("-fx-shape: circle;");
                nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
                hbox = new HBox(10, img, nameLbl);
                hbox.setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setGraphic(null);
                    return;
                }
                nameLbl.setText(name);
                String username = nameToUsernameMap.get(name);
                FriendItem friendItem = usernameToFriendItemMap.get(username);
                String avatarPath = friendItem != null ? friendItem.getAvatar() : DEFAULT_AVATAR;
                img.setImage(loadAvatar(avatarPath));
                setGraphic(hbox);
            }
        });
    }
  // XỬ LÝ SỰ KIỆN CHỌN VÀ GỬI TIN NHẮN
   private void handleFriendSelection(FriendItem selectedItem) {
        if (selectedItem == null) return;
       // NGĂN CHẶN XỬ LÝ TRÙNG LẶP (Nếu listener bị kích hoạt lại ngay lập tức)
       if (currentlyHandlingSelection != null && currentlyHandlingSelection.equals(selectedItem)) {
             return;
       }
       currentlyHandlingSelection = selectedItem;
        // 1. Cập nhật giao diện
        lblChatWith.setText(selectedItem.getName());
        lblStatus.setText(selectedItem.isGroup() ? "Nhóm chat" : selectedItem.isOnline() ? "Đang hoạt động" : "Offline");
        lblChatWith.setTextFill(Color.web("#00d400"));
        // 2. CẬP NHẬT AVATAR (imgChatAvatar)
       String avatarPath = selectedItem.getAvatar() != null ? selectedItem.getAvatar() : DEFAULT_AVATAR;
       imgChatAvatar.setImage(loadAvatar(avatarPath));
        // --- LOGIC XÓA BADGE (TIN NHẮN MỚI) ---
        if (selectedItem.hasNewMessage()) {
            selectedItem.setNewMessage(false);
            Platform.runLater(() -> {
                lvFriends.refresh(); // Buộc ListView vẽ lại cell để xóa màu nền/style
            });
        }
        // 2. Thiết lập trạng thái chat
        currentConversationId = selectedItem.getConvId();
        currentChatUsername = selectedItem.isGroup() ? null : selectedItem.getUsername();
        // 3. Mở Conversation

        if (currentConversationId != null) {
            openConversation(currentConversationId);
        } else if (currentChatUsername != null) {
            openOrCreateConversation(currentChatUsername);
        } else {
            // SỬA: Dùng ListView
            if (lvChatHistory != null) {
                lvChatHistory.getItems().clear();
            }
        }
       currentlyHandlingSelection = null;
    }
    // XỬ LÝ LỆNH SERVER
     private void handleMessage(String cmd, String payload) {
        switch (cmd) {
            case "FRIEND_LIST" -> updateFriendList(payload);
            case "ONLINE_USERS" -> updateOnlineList(payload);
            case "USER_ONLINE" -> handleUserStatusChange(payload, true);
            case "USER_OFFLINE" -> handleUserStatusChange(payload, false);
            case "CONVERSATION_ID" -> {
                String[] p = payload.split("\\|", 2);
                if (p.length == 2) {
                    String targetUsername = p[0];
                    int convId = Integer.parseInt(p[1]);
                    conversationMap.put(targetUsername, convId);
                    // Cập nhật ConvId vào FriendItem
                    FriendItem item = usernameToFriendItemMap.get(targetUsername);
                    if (item != null) {
                        item.setConvId(convId);
                    }

                    client.send("GET_MESSAGES|" + convId);
                }
            }
            case "PRIVATE_MSG" -> handleNewMessage(payload);
            case "GROUP_MSG" -> handleNewMessage(payload); // THÊM DÒNG NÀY!
            case "CHAT_HISTORY" -> handleChatHistory(payload);
            case "GROUP_CREATED", "NEW_GROUP_CONVERSATION" -> handleNewGroup(payload);
            case "GROUP_CONVERSATIONS" -> handleGroupConversations(payload);
            case "NEW_NOTIFICATION" -> handleNewNotification(payload);
            case "NOTIFICATION_UNREAD_COUNT" -> handleUnreadCount(payload);
            case "INCOMING_CALL" -> handleIncomingCall(payload);
            case "CALL_ACCEPTED" -> handleCallAccepted(payload, true); // True: Mình là người gọi
            case "CALL_DENIED" -> handleCallAccepted(payload, false); // False: Mình là người gọi, nhưng bị từ chối
            case "CALL_ENDED" -> handleCallEndedByPartner(payload); // <-- THÊM DÒNG NÀY
           // case "FILE_OFFER_READY" -> handleFileOfferReady(payload);
             }
    }
    private void updateFriendList(String payload) {
        Collection<FriendItem> existingGroups = groupNameToFriendItemMap.values();
        allFriendsAndGroups.removeIf(item -> !item.isGroup());
        usernameToFriendItemMap.clear();
        nameToUsernameMap.clear();
        conversationMap.clear();

        if (payload != null && !payload.isEmpty()) {
            for (String item : payload.split(",")) {
                String[] p = item.split("\\|", -1);
                // Cấu trúc: name|username|avatar|convId (nếu đã có)
                if (p.length < 3) continue;

                String name = p[0].trim();
                String username = p[1].trim();
                String avatar = p[2].trim();
                // Sửa logic parse Int để tránh lỗi NumberFormatException với chuỗi rỗng
                Integer convId = p.length > 3 && !p[3].trim().isEmpty() ? Integer.parseInt(p[3].trim()) : null;


                if (!username.equals(client.getCurrentUser().getUsername())) {
                    FriendItem itemFriend = new FriendItem(name, username, avatar, onlineFriends.contains(username));
                    itemFriend.setConvId(convId);

                    usernameToFriendItemMap.put(username, itemFriend);
                    nameToUsernameMap.put(name, username);
                    allFriendsAndGroups.add(itemFriend);
                    if (convId != null) {
                        conversationMap.put(username, convId);
                    }
                }
            }
        }
        allFriendsAndGroups.addAll(existingGroups); // Đã dùng existingGroups thay vì groupNameToFriendItemMap.values()
        Platform.runLater(() -> {
            lvFriends.refresh();
        });
    }

    private void updateOnlineList(String payload) {
        lvOnline.getItems().clear();
        onlineFriends.clear();
        if (payload != null && !payload.isEmpty()) {
            for (String entry : payload.split(",")) {
                String[] p = entry.split("\\|", -1);
                // Cấu trúc: name|username|avatar|...
                if (p.length < 3) continue;

                String name = p[0].trim();
                String username = p[1].trim();

                if (!username.equals(client.getCurrentUser().getUsername())) {
                    onlineFriends.add(username);
                    lvOnline.getItems().add(name);

                    // Cập nhật trạng thái online trong allFriendsAndGroups
                    FriendItem item = usernameToFriendItemMap.get(username);
                    if (item != null) {
                        item.setOnline(true);
                    }
                }
            }
        }
        Platform.runLater(() -> {
            lvOnline.refresh();
            lvFriends.refresh();
            updateCurrentStatus();
        });
    }

    private void updateCurrentStatus() {
        if (currentChatUsername == null) {
            lblStatus.setText("Hoạt động gần đây");
            lblStatus.setTextFill(Color.GRAY);
            return;
        }
        boolean isOnline = onlineFriends.contains(currentChatUsername);
        if (isOnline) {
            lblStatus.setText("Đang hoạt động");
            lblStatus.setTextFill(Color.web("#00d400"));
        } else {
            lblStatus.setText("Hoạt động gần đây");
            lblStatus.setTextFill(Color.GRAY);
        }
    }
    private void handleUserStatusChange(String username, boolean isOnline) {
        if (isOnline) {
            onlineFriends.add(username);
        } else {
            onlineFriends.remove(username);
        }
        FriendItem item = usernameToFriendItemMap.get(username);
        if (item != null && !item.isGroup()) {
            item.setOnline(isOnline);
        }
        Platform.runLater(() -> {
            // Cập nhật lvOnline
            String name = usernameToFriendItemMap.values().stream()
                    .filter(i -> i.getUsername() != null && i.getUsername().equals(username))
                    .map(FriendItem::getName).findFirst().orElse(null);
            if (name != null) {
                if (isOnline && !lvOnline.getItems().contains(name)) {
                    lvOnline.getItems().add(name);
                } else if (!isOnline) {
                    lvOnline.getItems().remove(name);
                }
            }
            lvFriends.refresh();
            updateCurrentStatus();
        });
    }
    private HBox createMessageNode(String senderName, String time, String contentUrl, String type, boolean isSelf, String fileName) {
        // Hộp chứa toàn bộ tin nhắn (tên, thời gian, nội dung)
        VBox messageBox = new VBox();
        Label lblHeader = new Label(senderName + " [" + time + "]");
        lblHeader.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        messageBox.setStyle("-fx-padding: 8; -fx-background-color: " + (isSelf ? "#DCF8C6" : "#B8D9D0") + "; -fx-background-radius: 12;");
        messageBox.setMaxWidth(300); // Giới hạn chiều rộng
        // 2. Nội dung chính
        if (type.equals("text")) {
            Label lblContent = new Label(contentUrl);
            lblContent.setWrapText(true);
            messageBox.getChildren().addAll(lblHeader, lblContent);
        } else if (type.equals("image")) {
            //
            Label lblImageHeader = new Label("[Ảnh] " + (fileName != null ? fileName : "Đang tải..."));
            ImageView imgView = new ImageView();
            imgView.setFitWidth(200);
            imgView.setPreserveRatio(true);

            // BẮT ĐẦU TẢI ẢNH TỪ URL (content là URL)
            Image image = new Image(contentUrl, true); // true: tải nền
            image.progressProperty().addListener((obs, oldV, newV) -> {
                if (newV.doubleValue() == 1.0) {
                    imgView.setImage(image);
                    if (fileName == null) {
                        lblImageHeader.setText("Ảnh đã tải:");
                    }
                }
            });
            // Tạo liên kết tải về cho ảnh (vì ImageView không phải nút)
            Hyperlink downloadLink = new Hyperlink("Tải về / Mở ảnh");
            downloadLink.setOnAction(e -> handleDownloadFile(contentUrl, fileName));

            imgView.setImage(image); // Hiển thị ngay (dù có thể chưa tải xong)
            messageBox.getChildren().addAll(lblHeader, lblImageHeader, imgView);
        } else if (type.equals("file")) {
           String displayFileName = fileName != null ? fileName : contentUrl.substring(contentUrl.lastIndexOf('/') + 1);

            Hyperlink fileLink = new Hyperlink("[Tệp] " + displayFileName);
            fileLink.setOnAction(e -> {
                // TODO: Triển khai logic tải tệp tin từ URL 'contentUrl'
                handleDownloadFile(contentUrl, displayFileName);
               // new Alert(Alert.AlertType.INFORMATION, "Chức năng tải tệp tin cần được triển khai: " + contentUrl).show();
            });
            messageBox.getChildren().addAll(lblHeader, fileLink);
        } else {
            Label lblContent = new Label("[Lỗi nội dung/Loại tệp không rõ] " + contentUrl);
            messageBox.getChildren().addAll(lblHeader, lblContent);
        }

        // Căn chỉnh tin nhắn (trái/phải)
        HBox container = new HBox(messageBox);
        container.setAlignment(isSelf ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.setPadding(new javafx.geometry.Insets(2, 5, 2, 5)); // Khoảng cách giữa các tin nhắn
        return container;
    }
@FXML
private void sendMessage() {
    String msg = tfMessage.getText().trim();
    if (msg.isEmpty()) return;
    // Mặc định là text, không cần thay đổi khi gửi tin nhắn text thông thường
    String type = "text";

    if (currentConversationId != null) {
        // Gửi tin nhắn qua Conversation ID (áp dụng cho cả chat 1-1 đã tạo và nhóm chat)
        client.send("SEND_MESSAGE|" + currentConversationId + "|" + msg + "|" + type);
    } else if (currentChatUsername != null) {
        // Trường hợp đặc biệt: Chat 1-1 lần đầu tiên (chưa có ConvId)
        client.send("CREATE_CONVERSATION|" + currentChatUsername + "|" + msg + "|" + type);
    } else {
        new Alert(Alert.AlertType.WARNING, "Vui lòng chọn người hoặc nhóm để chat!").show();
        return;
    }
    // Cập nhật giao diện (logic hiển thị tin nhắn vừa gửi)
    String time = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM"));
    Platform.runLater(() -> {
        HBox messageNode = createMessageNode("Bạn", time, msg, type, true, null); // true = isSelf
        if (lvChatHistory != null) {
            lvChatHistory.getItems().add(messageNode);
            lvChatHistory.scrollTo(lvChatHistory.getItems().size() - 1);
        }
    });
    tfMessage.clear();
}
private void handleNewMessage(String payload) {
// TEXT: convId|senderUsername|time|content|type
    // FILE/IMAGE: convId|senderUsername|time|URL|type|fileName
    String[] p = payload.split("\\|", 6); // SỬA: thành 6
       if (p.length < 5) return;

    int convId = Integer.parseInt(p[0]);
    String senderUsername = p[1];
    String time = p[2];
    String urlContent = p[3];
    String type = p[4];
    // Lấy fileName (chỉ có nếu payload có 6 phần tử)
    String fileName = p.length > 5 ? p[5] : null;

    // TÌM FriendItem mục tiêu dựa trên convId (Áp dụng cho cả 1-1 và nhóm)
    FriendItem targetItem = allFriendsAndGroups.stream()
            .filter(item -> Objects.equals(item.getConvId(), convId))
            .findFirst().orElse(null);

    // 1. HIỂN THỊ TIN NHẮN TRONG CỬA SỔ CHAT ĐANG MỞ
    if (currentConversationId != null && currentConversationId == convId) {

        boolean isSelf = senderUsername.equals(client.getUsername());

        if (isSelf && (type.equals("image") || type.equals("file")) && this.tempSendingMessage != null) {
            Platform.runLater(() -> {
                // Xóa node "Đang gửi..." khỏi ListView
                if (lvChatHistory.getItems().remove(this.tempSendingMessage)) {
                    this.tempSendingMessage = null; // Đã xóa, reset lại
                }
            });
        }

        Platform.runLater(() -> {
            // SỬA: Lấy tên người gửi đúng cho cả 1-1 và nhóm
            String senderName = senderUsername.equals(client.getUsername())
                    ? "Bạn"
                    : usernameToFriendItemMap.getOrDefault(senderUsername, new FriendItem("", senderUsername, "", false)).getName();
           HBox messageNode = createMessageNode(senderName, time, urlContent, type, isSelf, fileName);
            lvChatHistory.getItems().add(messageNode);
            // Cuộn xuống cuối
            lvChatHistory.scrollTo(lvChatHistory.getItems().size() - 1);
        });
         if (targetItem != null) {
            targetItem.setNewMessage(false);
        }
    }
    // 2. XỬ LÝ BADGE VÀ DI CHUYỂN ITEM LÊN ĐẦU
    else if (targetItem != null) {
        // ... (Logic di chuyển và đánh dấu badge giữ nguyên, vì nó đã đúng)
        targetItem.setNewMessage(true);
        Platform.runLater(() -> {
            // Loại bỏ item khỏi vị trí cũ,thêm lại vào vị trí đầu tiên
            allFriendsAndGroups.remove(targetItem);
            allFriendsAndGroups.add(0, targetItem);
            lvFriends.refresh();
        });
    }
}
    @FXML
    private void handleSendFile() {
        if (currentConversationId == null && currentChatUsername == null) {
            new Alert(Alert.AlertType.WARNING, "Vui lòng chọn người hoặc nhóm để gửi file!").show();
            return;
        }

        // Mở File Chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn tệp để gửi");

        // Thiết lập bộ lọc (Tùy chọn: lọc ảnh, file,...)
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tất cả tệp", "*.*"),
                new FileChooser.ExtensionFilter("Hình ảnh", "*.jpg", "*.png", "*.gif"),
                new FileChooser.ExtensionFilter("Tài liệu PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Tài liệu Văn bản", "*.txt", "*.doc", "*.docx")
        );

        // Lấy Stage hiện tại (từ bất kỳ thành phần giao diện nào)
        Stage stage = (Stage) btnSendFile.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            String fileName = selectedFile.getName();
            String fileExtension = getFileExtension(fileName).toLowerCase();
            String messageType = (fileExtension.matches("jpg|jpeg|png|gif")) ? "image" : "file";;

            long fileSize = selectedFile.length();
            this.fileToUpload = selectedFile;
            final long MAX_FILE_SIZE_CUSTOM = 31457280; // 30 MB (30 * 1024 * 1024 bytes)

            if (fileSize > MAX_FILE_SIZE_CUSTOM) {
                String sizeMB = String.format("%.2f", fileSize / 1048576.0);
                new Alert(Alert.AlertType.ERROR,
                        "Tệp tin quá lớn (" + sizeMB + " MB). " +
                                "Kích thước tối đa cho phép là 30 MB."
                ).show();
                return; // Dừng xử lý, không gửi FILE_OFFER
            }
//
//            if (fileExtension.equals("jpg") || fileExtension.equals("jpeg") ||
//                    fileExtension.equals("png") || fileExtension.equals("gif")) {
//                messageType = "image";
//            }

            if (currentConversationId != null) {
                // 1. Gửi tin nhắn tạm thời (Hiển thị "Đang gửi...")
                sendFileOffer(currentConversationId, selectedFile, messageType);

                // 2. Bắt đầu Upload (Tải file lên Cloudinary và gửi lệnh SEND_MESSAGE sau)
                handleFileUpload(currentConversationId, selectedFile, messageType); // Cần đảm bảo hàm này tồn tại
            } else if (currentChatUsername != null) {
                // client.send("CREATE_CONVERSATION_AND_FILE_OFFER|" + currentChatUsername + "|" + fileName + "|" + fileSize + "|" + messageType);
               new Alert(Alert.AlertType.INFORMATION, "Vui lòng gửi một tin nhắn văn bản trước để khởi tạo cuộc trò chuyện.").show();
                // Reset fileToUpload vì ta không upload ngay
                this.fileToUpload = null;
            }
        }
    }

    // Phương thức để gửi đề nghị upload file
    private void sendFileOffer(int convId, File file, String messageType) {
        String fileName = file.getName();
        long fileSize = file.length();
       Platform.runLater(() -> {
              String content = "Đang gửi tệp tin '" + fileName + "' (" + messageType.toUpperCase() + ")...";
              String time = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM"));

              HBox messageNode = createMessageNode("Bạn", time, content, "text", true, null);

              // >>> LƯU THAM CHIẾU <<<
              this.tempSendingMessage = messageNode;
            if (lvChatHistory != null) {
                lvChatHistory.getItems().add(messageNode);
                lvChatHistory.scrollTo(lvChatHistory.getItems().size() - 1);
            }
        });}

    // Hàm hỗ trợ lấy phần mở rộng của file
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
//    private void handleFileOfferReady(String payload) {
//        // Payload: convId|fileName|fileSize|messageType
//        String[] p = payload.split("\\|", 4);
//        if (p.length < 4 || this.fileToUpload == null) {
//            System.err.println("Lỗi: Không tìm thấy file hoặc thiếu thông tin từ Server.");
//            this.fileToUpload = null;
//            return;
//        }
//
//        int convId = Integer.parseInt(p[0]);
//        String fileName = p[1];
//        String messageType = p[3];
//        File uploadFile = this.fileToUpload;
//
//        // QUAN TRỌNG: Thiết lập fileToUpload = null ngay sau khi lấy ra
//        this.fileToUpload = null;
//
//        // BẮT ĐẦU QUÁ TRÌNH UPLOAD TRONG LUỒNG MỚI (Bất đồng bộ)
//        new Thread(() -> {
//            try {
//                // 1. Tạo Public ID (định danh duy nhất trên Cloudinary)
//                String rawFileName = fileName.substring(0, fileName.lastIndexOf('.'));
//                String publicId = client.getUsername() + "_" + System.currentTimeMillis() + "_" + rawFileName;
//
//                // 2. >>> THỰC HIỆN TẢI LÊN FILE LÊN CLOUDINARY <<<
//                String fileUrl = CloudinaryClientUploader.upload(uploadFile, publicId, messageType);
//                if (fileUrl != null) {
//                    // BƯỚC 3: Tải lên thành công, gửi lệnh SEND_MESSAGE chứa URL
//                    String command = "SEND_MESSAGE|" + convId + "|" + fileUrl + "|" + messageType + "|" + fileName;
//                    client.send(command);
//                } else {
//                    Platform.runLater(() -> {
//                        new Alert(Alert.AlertType.ERROR, "Tải tệp tin lên Cloudinary thất bại.").show();
//                    });
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                Platform.runLater(() -> {
//                    new Alert(Alert.AlertType.ERROR, "Lỗi khi tải tệp tin: " + e.getMessage()).show();
//                });
//            }
//        }).start();
//    }

    // TRONG ChatController.java

    /**
     * Xử lý việc tải file từ URL Cloudinary về máy người dùng.
     * @param fileUrl URL của file trên Cloudinary.
     * @param suggestedFileName Tên file gợi ý để lưu.
     */
    private void handleDownloadFile(String fileUrl, String suggestedFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu Tệp Tin");
        fileChooser.setInitialFileName(suggestedFileName);

        // Lấy Stage hiện tại để mở hộp thoại
        Stage stage = (Stage) lvChatHistory.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);

        if (saveFile != null) {
            // 2. Bắt đầu tải file trong LUỒNG NỀN để tránh treo giao diện
            new Thread(() -> {
                try {
                    // Mở kết nối đến URL
                    URLConnection connection = new URL(fileUrl).openConnection();

                    // Mở luồng để đọc dữ liệu từ Cloudinary
                    try (InputStream in = connection.getInputStream();
                         // Mở luồng để ghi dữ liệu vào file cục bộ
                         FileOutputStream out = new FileOutputStream(saveFile)) {

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalBytesRead = 0;
                        long fileSize = connection.getContentLengthLong(); // Tổng kích thước file

                        // Đọc và ghi từng block dữ liệu
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;

                            // Tùy chọn: Cập nhật thanh tiến trình (nếu bạn có thanh progress bar)
                            // double progress = (double) totalBytesRead / fileSize;
                            // Platform.runLater(() -> { /* Cập nhật progress bar */ });
                        }

                        // 3. Thông báo thành công trên Luồng Giao diện
                        Platform.runLater(() -> {
                            new Alert(Alert.AlertType.INFORMATION, "Tải tệp tin thành công:\n" + saveFile.getAbsolutePath()).show();
                        });

                    }
                } catch (IOException e) {
                    // 4. Thông báo lỗi trên Luồng Giao diện
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        new Alert(Alert.AlertType.ERROR, "Lỗi khi tải tệp tin: " + e.getMessage()).show();
                    });
                }
            }).start();
        }
    }
    // HÀM MỚI: Xử lý việc TẢI FILE LÊN CLOUDINARY
    private void handleFileUpload(int convId, File uploadFile, String messageType) {
        String fileName = uploadFile.getName();

        // 1. Chuẩn bị Giao diện (Hiển thị ProgressBar, Vô hiệu hóa chat)
        Platform.runLater(() -> {
            pbUploadProgress.setProgress(0.0);
            pbUploadProgress.setVisible(true);
            tfMessage.setDisable(true);
        });

        new Thread(() -> {
            String fileUrl = null;
            try {
                // 2. Tạo Public ID
                String rawFileName = fileName.substring(0, fileName.lastIndexOf('.'));
                String publicId = client.getUsername() + "_" + System.currentTimeMillis() + "_" + rawFileName;

                // 3. THỰC HIỆN TẢI LÊN (Cần hàm upload có hỗ trợ callback tiến trình)
                // Giả định CloudinaryClientUploader có hàm upload chấp nhận Consumer<Double>
                fileUrl = CloudinaryClientUploader.upload(uploadFile, publicId, messageType);

                if (fileUrl != null) {
                    // Tải lên thành công, gửi lệnh SEND_MESSAGE chứa URL
                    String command = "SEND_MESSAGE|" + convId + "|" + fileUrl + "|" + messageType + "|" + fileName;
                    client.send(command);
                } else {
                    handleUploadFailure("Tải tệp tin lên Cloudinary thất bại.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                handleUploadFailure("Lỗi kết nối khi tải tệp tin: " + e.getMessage());
            } finally {
                // 4. Dọn dẹp Giao diện
                String finalFileUrl = fileUrl;
                Platform.runLater(() -> {
                    pbUploadProgress.setVisible(false);
                    tfMessage.setDisable(false); // Kích hoạt lại ô chat

                    // Dọn dẹp tin nhắn tạm thời nếu tải lên thất bại (fileUrl == null)
                    if (finalFileUrl == null && this.tempSendingMessage != null) {
                        lvChatHistory.getItems().remove(this.tempSendingMessage);
                        this.tempSendingMessage = null;
                    }
                });
                this.fileToUpload = null;
            }
        }).start();
    }

    // HÀM HỖ TRỢ XỬ LÝ LỖI (Dùng chung cho cả logic trên)
    private void handleUploadFailure(String message) {
        Platform.runLater(() -> {
            if (this.tempSendingMessage != null) {
                lvChatHistory.getItems().remove(this.tempSendingMessage);
                this.tempSendingMessage = null;
            }
            new Alert(Alert.AlertType.ERROR, message).show();
        });
    }

    private void handleChatHistory(String payload) {
        lvChatHistory.getItems().clear(); // Xóa ListView
        if (payload == null || payload.isEmpty() || payload.contains("Chưa có tin nhắn")) {
            // Tạo một node đơn giản để thông báo không có tin nhắn
            Label emptyLabel = new Label("Chưa có tin nhắn nào.");
            HBox emptyContainer = new HBox(emptyLabel);
            emptyContainer.setAlignment(Pos.CENTER);
            lvChatHistory.getItems().add(emptyContainer);
            return;
        }

        Platform.runLater(() -> {
            for (String line : payload.split("@@@")) {
                String[] p = line.split("\\|", 6);
                if (p.length < 6) continue;

                // BỎ QUA p[0] (ConvId)
                // p[0] là convId (bỏ qua)
                String sender = p[1]; // Lấy username người gửi
                String time = p[2];
                String urlContent = p[3];
                String type = p[4];
                String fileName = p[5]; // Lấy fileName
                // Xác định tên người gửi và căn chỉnh
                String senderName = sender.equals(client.getUsername())
                        ? "Bạn"
                        : usernameToFriendItemMap.getOrDefault(sender, new FriendItem("", sender, "", false)).getName();
                boolean isSelf = sender.equals(client.getUsername());
                // Tạo Node tùy chỉnh và thêm vào ListView
                HBox messageNode = createMessageNode(senderName, time, urlContent, type, isSelf, fileName);
                lvChatHistory.getItems().add(messageNode);
            }
            // Cuộn xuống cuối
            lvChatHistory.scrollTo(lvChatHistory.getItems().size() - 1);
        });
    }
    private void handleNewGroup(String payload) {
        String[] parts = payload.split("\\|", 2);
        if (parts.length < 2) return;

        int convId = Integer.parseInt(parts[0]);
        String groupName = parts[1];

        Platform.runLater(() -> {
            try {
                // SỬ DỤNG CONSTRUCTOR NHÓM CHỈ CÓ 2 THAM SỐ
                FriendItem groupItem = new FriendItem(groupName,convId);
                // Cập nhật Map tra cứu mới (SỬ DỤNG convId làm key chính)
                groupIdToFriendItemMap.put(convId, groupItem);
                groupNameToFriendItemMap.put(groupName, groupItem); // Vẫn giữ lại cho mục đích khác
                if (!allFriendsAndGroups.contains(groupItem)) {
                    allFriendsAndGroups.add(0, groupItem);
                }
                lvFriends.refresh();
                // *** THÊM LOGIC NÀY: TỰ ĐỘNG CHỌN VÀ TẢI CHAT HISTORY CỦA NHÓM VỪA TẠO ***
                lvFriends.getSelectionModel().select(groupItem);
                lvFriends.scrollTo(0); // Cuộn lên đầu
            } catch (NumberFormatException e) {
                System.err.println("Lỗi parsing ConvId cho nhóm mới: " + payload);
            }
        });
    }
    private void handleGroupConversations(String payload) {
        if (payload == null || payload.isEmpty()) return;
        // Xóa dữ liệu nhóm cũ (chỉ trong Map)
        groupNameToFriendItemMap.clear();
        groupIdToFriendItemMap.clear();
        // Dùng một Set tạm để theo dõi các nhóm đang có trong allFriendsAndGroups
        Set<Integer> existingConvIds = allFriendsAndGroups.stream()
                .filter(FriendItem::isGroup)
                .map(FriendItem::getConvId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (String entry : payload.split(",")) {
            String[] parts = entry.split(":", 2);
            if (parts.length < 2) continue;

            try {
                int convId = Integer.parseInt(parts[0].trim());
                String groupName = parts[1].trim();
                FriendItem groupItem = new FriendItem(groupName, convId);
                // 1. Cập nhật Map tra cứu
                groupNameToFriendItemMap.put(groupName, groupItem);
                groupIdToFriendItemMap.put(convId, groupItem);
                // 2. Thêm vào danh sách hiển thị nếu chưa có
                if (!existingConvIds.contains(convId)) {
                    allFriendsAndGroups.add(groupItem);
                }
            } catch (NumberFormatException e) {
                System.err.println("Lỗi parsing Group ConvId: " + entry);
            }
        }
        Platform.runLater(() -> lvFriends.refresh());
    }
    private void openConversation(int convId) {
        // SỬA: Dùng ListView
        if (lvChatHistory != null) {
            lvChatHistory.getItems().clear();

            // Hiển thị thông báo 'Đang tải tin nhắn...' giữa màn hình
            Label loadingLabel = new Label("Đang tải tin nhắn...");
            HBox loadingNode = new HBox(loadingLabel);
            loadingNode.setAlignment(Pos.CENTER);
            lvChatHistory.getItems().add(loadingNode);
        }
        client.send("GET_MESSAGES|" + convId);
    }

    private void openOrCreateConversation(String username) {
        // SỬA: Dùng ListView
        if (lvChatHistory != null) {
            lvChatHistory.getItems().clear();

            // Hiển thị thông báo 'Đang tải tin nhắn...' giữa màn hình
            Label loadingLabel = new Label("Đang tải tin nhắn...");
            HBox loadingNode = new HBox(loadingLabel);
            loadingNode.setAlignment(Pos.CENTER);
            lvChatHistory.getItems().add(loadingNode);
        }

        Integer convId = conversationMap.get(username);
        if (convId != null) {
            client.send("GET_MESSAGES|" + convId);
        } else {
            client.send("GET_CONVERSATION|" + username);
        }
    }
    private void startVideoCall() {
        if (currentChatUsername == null) {
            new Alert(Alert.AlertType.WARNING, "Chức năng gọi video chỉ hỗ trợ chat 1-1.").show();
            return;
        }
        // 1. Kiểm tra trạng thái online của đối phương
        if (!onlineFriends.contains(currentChatUsername)) {
            new Alert(Alert.AlertType.WARNING, "Người dùng này hiện không hoạt động.").show();
            return;
        }
        // 2. Gửi yêu cầu gọi video qua ServerServer sẽ nhận lệnh, tìm IP của đối phương và gửi INCOMING_CALL
        client.send("CALL_REQUEST|" + currentChatUsername);

        // Tạm thời hiển thị thông báo chờ. Cửa sổ video sẽ mở khi nhận được CALL_ACCEPTED.
        Platform.runLater(() -> {
            lblStatus.setText("Đang chờ phản hồi cuộc gọi...");
            // Vô hiệu hóa nút gọi video để tránh gọi lại
            btnCallVideo.setDisable(true);
        });
    }
    private void openVideoCallWindow(String targetName, String targetIP,  boolean isReceiver, String partnerUsername) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/videoCall.fxml"));
            Parent root = loader.load();

            // Giả định bạn tạo VideoCallController
            VideoCallController controller = loader.getController();

            // Truyền thông tin cho controller
            controller.setTargetInfo(targetName, targetIP, isReceiver, partnerUsername);

            Stage videoStage = new Stage();
            videoStage.setScene(new Scene(root));
            videoStage.setTitle("Gọi Video với " + targetName);
            videoStage.show();
            videoStage.setOnHidden(e -> videoCallStage = null); // Đặt lại khi đóng
            videoCallStage = videoStage; // Lưu lại Stage
            // Thêm listener để xử lý khi cửa sổ bị đóng bằng nút X
            videoStage.setOnCloseRequest(event -> {
                // Giả định bạn có thể gọi endCall() từ controller
                controller.endCall();
            });

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Không thể mở cửa sổ gọi video.").show();
        }
    }
    private void handleIncomingCall(String payload) {
        // payload: A_Username|A_IP|A_Port
        String[] p = payload.split("\\|");
        if (p.length < 3) return;

        String callerUsername = p[0];
        String callerIP = p[1];
        int callerPort = Integer.parseInt(p[2]);

        Platform.runLater(() -> {
            // TÌM TÊN THẬT CỦA NGƯỜI GỌI
            FriendItem callerFriend = usernameToFriendItemMap.get(callerUsername);
            String callerName = callerFriend != null ? callerFriend.getName() : callerUsername;

            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Cuộc gọi đến");
            confirmation.setHeaderText("Cuộc gọi video đến từ " + callerName);
            confirmation.setContentText("Bạn có muốn chấp nhận cuộc gọi không?");

            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // 1. Gửi ACCEPT về Server
                    client.send("CALL_ACCEPT|" + callerUsername);
                    // 2. Mở cửa sổ cuộc gọi (Mình là người nhận, cần start Receiver trước)
                    openVideoCallWindow(callerName, callerIP, true, callerUsername); // True: isReceiver
                } else {
                    // Gửi DENY về Server
                    client.send("CALL_DENY|" + callerUsername);
                }
            });
        });
    }

    private void handleCallAccepted(String payload, boolean isCaller) {
        // isCaller = true: Payload là B_IP|B_Port (Mình nhận IP đối phương)
        // isCaller = false: Payload là A_Username (Chỉ báo bị từ chối)
        if (!isCaller) {
            // Bị từ chối (CALL_DENIED)
            Platform.runLater(() -> {
                new Alert(Alert.AlertType.INFORMATION, "Cuộc gọi đã bị từ chối.").show();
                btnCallVideo.setDisable(false);
            });
            return;
        }

        // Đã được chấp nhận (Mình là người gọi)
        String[] p = payload.split("\\|");
        if (p.length < 2) return;

        String calleeIP = p[0];
        // Port mặc định là 5555, không cần thiết phải truyền

        FriendItem calleeFriend = usernameToFriendItemMap.get(currentChatUsername);

        Platform.runLater(() -> {
            // Mở cửa sổ cuộc gọi (Mình là người gọi, cần start Sender trước)
            openVideoCallWindow(calleeFriend.getName(), calleeIP, false, currentChatUsername); // False: isSender
            btnCallVideo.setDisable(false); // Bật lại nút gọi sau khi kết nối
        });
    }
    // Logic xử lý khi đối tác ngắt cuộc gọi
    private void handleCallEndedByPartner(String partnerUsername) {
        Platform.runLater(() -> {
            // 1. Hiển thị thông báo
            FriendItem partner = usernameToFriendItemMap.get(partnerUsername);
            String name = partner != null ? partner.getName() : partnerUsername;

            new Alert(Alert.AlertType.INFORMATION,
                    "Cuộc gọi đã kết thúc bởi " + name + ".",
                    ButtonType.OK).show();

            // 2. Đóng cửa sổ video (Nếu bạn đã lưu Stage của cuộc gọi)
            // Nếu bạn không lưu Stage, bạn cần tìm cách đóng Stage đang mở.
            // Cách đơn giản nhất là gửi lệnh đến VideoCallController hiện tại (Nếu nó là singleton)
            // HOẶC, nếu bạn đã lưu Stage ở bước trên:
            if (videoCallStage != null && videoCallStage.isShowing()) {
                videoCallStage.close();
            }

            // 3. Bật lại nút gọi video
            btnCallVideo.setDisable(false);
            lblStatus.setText(onlineFriends.contains(currentChatUsername) ? "Đang hoạt động" : "Offline");
        });
    }

    private void loadFriendAndOnlineList() {
        client.send("GET_FRIEND_LIST");
        client.send("REQUEST_ONLINE_LIST");
        client.send("GET_GROUP_CONVERSATIONS");
      }

    private void updateUserInfo() {
        User user = client.getCurrentUser();
        if (user != null) {
            lblCurrentUser.setText(user.getName());
            imgAvatar.setImage(loadAvatar(user.getAvatar()));
        }
    }
    // Trong ChatController.java

    private Image loadAvatar(String path) {
        if (path == null || path.trim().isEmpty()) {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream(DEFAULT_AVATAR))); // Dùng DEFAULT_AVATAR
        }

        // Trường hợp 1: Tải ảnh từ URL công cộng (Cloudinary)
        if (path.startsWith("http://") || path.startsWith("https://")) {
            try {
                return new Image(path, true); // Tải bất đồng bộ
            } catch (Exception e) {
                System.err.println("Lỗi tải ảnh từ URL: " + path);
                // Tiếp tục sang fallback
            }
        }
        // Trường hợp 3: Fallback cuối cùng
        try {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream(DEFAULT_AVATAR)));
        } catch (NullPointerException e) {
            System.err.println("Không tìm thấy ảnh mặc định.");
            return null;
        }
    }
    private void showCreateGroupPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create_group_popup.fxml"));
            Parent root = loader.load();
            CreateGroupController controller = loader.getController();

            Stage popupStage = new Stage();
            popupStage.setScene(new Scene(root));
            popupStage.initStyle(StageStyle.UNDECORATED);
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setResizable(false);
            popupStage.setTitle("Tạo nhóm chat");
            controller.setPopupStage(popupStage);
            // SỬA LỖI: Cannot resolve method 'toObservableList'
            List<String> friendNamesList = allFriendsAndGroups.stream()
                    .filter(item -> !item.isGroup())
                    .map(FriendItem::getName)
                    .collect(Collectors.toList());
            ObservableList<String> friendNames = FXCollections.observableArrayList(friendNamesList);
            controller.setFriendData(friendNames, nameToUsernameMap);
            popupStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Không thể mở cửa sổ tạo nhóm.").show();
        }
    }

    private void showNotificationPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/notifications.fxml"));
            Parent root = loader.load();
            NotificationController controller = loader.getController();

            Stage popupStage = new Stage();
            popupStage.setScene(new Scene(root));
            popupStage.initStyle(StageStyle.TRANSPARENT);
            popupStage.initModality(Modality.NONE);
            popupStage.setResizable(false);
            popupStage.setWidth(420);
            popupStage.setHeight(600);

            Stage mainStage = (Stage) bntNotification.getScene().getWindow();
            popupStage.setX(mainStage.getX() + mainStage.getWidth() / 2 - 210);
            popupStage.setY(mainStage.getY() + mainStage.getHeight() / 2 - 300);

            controller.setPopupStage(popupStage);
            popupStage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
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
    private void handleNewNotification(String payload) {
        String[] p = payload.split("\\|", 2);
        if (p.length < 2) return;
        NetworkClient.getInstance().send("GET_UNREAD_COUNT");
       String title = p.length > 0 ? p[0] : "Thông báo mới";
        String content = p.length > 1 ? p[1] : "";
        System.out.println("Bạn có thông báo mới: " + title + " - " + content);
    }

    private void handleSearch(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            // Nếu ô tìm kiếm rỗng, hiển thị lại toàn bộ danh sách bạn bè/nhóm
            lvFriends.getItems().setAll(allFriendsAndGroups);
            return;
        }

        String lowerCaseSearch = searchText.trim().toLowerCase();

        // Lọc danh sách hiện tại (allFriendsAndGroups)
        List<FriendItem> searchResults = allFriendsAndGroups.stream()
                .filter(item ->
                        item.getName().toLowerCase().contains(lowerCaseSearch) || // Lọc theo tên hiển thị
                                (item.getUsername() != null && item.getUsername().toLowerCase().contains(lowerCaseSearch)) // Lọc theo username (nếu có)
                )
                .collect(Collectors.toList());

        // Cập nhật ListView lvFriends bằng kết quả tìm kiếm
        Platform.runLater(() -> {
            lvFriends.getItems().setAll(searchResults);
            lvFriends.refresh();
        });
    }

    private void logout() {
        new Alert(Alert.AlertType.CONFIRMATION, "Đăng xuất?", ButtonType.YES, ButtonType.NO)
                .showAndWait()
                .ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        client.disconnect();
                        SceneManager.changeScene("login.fxml");
                    }
                });
    }
   }
