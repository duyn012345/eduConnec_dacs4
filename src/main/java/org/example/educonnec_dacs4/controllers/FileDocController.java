package org.example.educonnec_dacs4.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox; // Thêm HBox
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.educonnec_dacs4.client.NetworkClient;
import org.example.educonnec_dacs4.model.Document;
import org.example.educonnec_dacs4.model.User;
import org.example.educonnec_dacs4.utils.SceneManager;

import java.awt.Desktop; // Dùng để mở file bên ngoài
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class FileDocController {

    // --- FXML Components (List Views và Search) ---
    @FXML private ListView<Document> lvAllDocuments;
    @FXML private ListView<Document> lvMyFiles; // MỚI: File của tôi
    @FXML private ListView<Document> lvRecentDownloads;
    @FXML private TextField tfSearchInput;
    @FXML private Button btnSearchFr;

    // --- FXML Components (Xem trước) ---
    @FXML private VBox previewContentBox;
    @FXML private ImageView imgPreview;
    @FXML private Label lblPreviewFilename;
    @FXML private Label lblPreviewFileType;
    @FXML private Button btnDownload; // Đổi tên từ btnOpenDocument thành btnDownload
    @FXML private Button btnViewDocument; // MỚI: Nút xem file

    // --- FXML Components (Navigation & Info) ---
    @FXML private Label lblName;
    @FXML private Label lblTimeDate;
    @FXML private ImageView imgAvatar;
    @FXML private Button btnHome, btnSearch, btnChat, btnGroupChat, btnFiles, btnLogout;
    @FXML private Button bntNotification;
    @FXML private Label lblNotificationBadge;
    @FXML private ImageView imgBell;

    // --- Dữ liệu và Utility ---
    private NetworkClient client;
    private final ObservableList<Document> allDocuments = FXCollections.observableArrayList();
    private final ObservableList<Document> myFiles = FXCollections.observableArrayList(); // MỚI: Data cho MyFiles
    private final ObservableList<Document> recentDownloads = FXCollections.observableArrayList();

    private static final String DEFAULT_AVATAR = "/org/example/educonnec_dacs4/image/avatar.png";

    @FXML
    public void initialize() {
        client = NetworkClient.getInstance();
        updateClock();
        setupButtons();
        setupListViews();
        setupSelectionListeners();
        client.subscribe(this::handleMessage);
        loadDocuments();
        updateUserInfo();
    }

    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        Platform.runLater(() -> lblTimeDate.setText(now.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))));
    }

    private void updateUserInfo() {
        var client = NetworkClient.getInstance();
        User user = client.getCurrentUser();
        if (user != null) {
            // SỬA DÒNG NÀY: HIỆN TÊN THẬT THAY VÌ USERNAME
            lblName.setText(user.getName());
// GỌI HÀM TẢI ẢNH MỚI
            imgAvatar.setImage(loadAvatarFromPathOrUrl(user.getAvatar()));
        }
    }

    private Image loadAvatarFromPathOrUrl(String urlPath) {
        // 1. Kiểm tra URL có hợp lệ không
        if (urlPath != null && (urlPath.startsWith("http://") || urlPath.startsWith("https://"))) {
            try {
                // Tải ảnh từ URL Cloudinary
                // Tham số true giúp tải bất đồng bộ (async), tránh làm treo giao diện
                return new Image(urlPath, true);
            } catch (Exception e) {
                System.err.println("Lỗi tải ảnh từ Cloudinary URL: " + urlPath + ". Dùng ảnh mặc định.");
                // Nếu lỗi khi tải từ URL, dùng ảnh mặc định
            }
        }

        // 2. Tải ảnh mặc định (Fallback)
        try {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/avatar.png")));
        } catch (NullPointerException e) {
            System.err.println("Không tìm thấy ảnh mặc định.");
            return null; // Trả về null nếu ảnh mặc định cũng không tồn tại
        }
    }


    private void setupButtons() {
        btnHome.setOnAction(e -> SceneManager.changeScene("home.fxml"));
        btnSearch.setOnAction(e -> SceneManager.changeScene("searchFriend.fxml"));
        btnChat.setOnAction(e -> SceneManager.changeScene("chat.fxml"));
        btnGroupChat.setOnAction(e -> SceneManager.changeScene("groupChat.fxml"));
        btnFiles.setOnAction(e -> SceneManager.changeScene("fileDoc.fxml"));
        btnLogout.setOnAction(e -> logout());
        btnSearchFr.setOnAction(e -> handleSearch(tfSearchInput.getText()));

        // Gắn sự kiện cho nút Tải về
//        btnDownload.setOnAction(e -> {
//            Document selected = getSelectedDocument();
//            if (selected != null) {
//                handleDownloadFile(selected.getFilePath(), selected.getFilename());
//            }
//        });

        // Gắn sự kiện cho nút Xem file (MỚI)
        btnViewDocument.setOnAction(e -> {
            Document selected = getSelectedDocument();
            if (selected != null) {
                handleViewDocument(selected.getFilePath(), selected.getFilename());
            }
        });
    }

    private Document getSelectedDocument() {
        Document selected = lvAllDocuments.getSelectionModel().getSelectedItem();
        if (selected == null) selected = lvMyFiles.getSelectionModel().getSelectedItem();
        if (selected == null) selected = lvRecentDownloads.getSelectionModel().getSelectedItem();
        return selected;
    }

    private void handleSearch(String query) {
        if (query.isEmpty()) {
            lvAllDocuments.setItems(allDocuments);
            return;
        }
        ObservableList<Document> filteredList = FXCollections.observableArrayList();
        String lowerCaseQuery = query.toLowerCase();
        for (Document item : allDocuments) {
            if (item.getFilename().toLowerCase().contains(lowerCaseQuery) ||
                    item.getFileType().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(item);
            }
        }
        lvAllDocuments.setItems(filteredList);
    }

    private void setupListViews() {
        lvAllDocuments.setItems(allDocuments);
        lvMyFiles.setItems(myFiles); // Set data cho MyFiles
        lvRecentDownloads.setItems(recentDownloads);

        // Thiết lập Cell Factory chung
        lvAllDocuments.setCellFactory(lv -> createDocumentCell());
        lvMyFiles.setCellFactory(lv -> createDocumentCell()); // Dùng chung Cell cho MyFiles
        lvRecentDownloads.setCellFactory(lv -> createDocumentCell());
    }

    private void setupSelectionListeners() {
        // Không cần gọi showPreview nữa
        lvAllDocuments.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lvMyFiles.getSelectionModel().clearSelection();
                lvRecentDownloads.getSelectionModel().clearSelection();
            }
        });

        lvMyFiles.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lvAllDocuments.getSelectionModel().clearSelection();
                lvRecentDownloads.getSelectionModel().clearSelection();
            }
        });

        lvRecentDownloads.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lvAllDocuments.getSelectionModel().clearSelection();
                lvMyFiles.getSelectionModel().clearSelection();
            }
        });
    }

    private void showPreview(Document doc) {
        String fileType = doc.getFileType().toLowerCase();
        lblPreviewFilename.setText(doc.getFilename());
        lblPreviewFileType.setText("Loại tệp: " + fileType.toUpperCase());

        // Hiện nút Tải về
        btnDownload.setVisible(true);

        // Kiểm tra loại tệp để quyết định ẩn/hiện nút Xem file
        if (fileType.matches("pdf|doc|docx|txt|html")) {
            btnViewDocument.setVisible(true); // Chỉ cho phép xem các loại tài liệu phổ biến
        } else {
            btnViewDocument.setVisible(false);
        }

        imgPreview.setImage(null);

        if (fileType.matches("png|jpg|jpeg|gif")) {
            try {
                Image image = new Image(doc.getFilePath(), true);
                imgPreview.setImage(image);
                imgPreview.setFitHeight(120.0);
                imgPreview.setFitWidth(300.0);
            } catch (Exception e) {
                System.err.println("Lỗi tải ảnh xem trước: " + e.getMessage());
                showDefaultIcon("/org/example/educonnec_dacs4/image/icon_image_broken.png");
            }
        } else {
            // Hiển thị icon cho các loại file khác
            String iconPath = switch (fileType) {
                case "pdf" -> "/org/example/educonnec_dacs4/image/icon_pdf.png";
                case "doc", "docx" -> "/org/example/educonnec_dacs4/image/icon_doc.png";
                case "txt" -> "/org/example/educonnec_dacs4/image/icon_txt.png";
                default -> "/org/example/educonnec_dacs4/image/icon_file.png";
            };
            showDefaultIcon(iconPath);
        }

        // Cập nhật list File đã xem gần đây (Client-side tracking) và ghi log xem
        updateRecentDownloadsList(doc);
        client.send("MARK_DOCUMENT_VIEWED|" + doc.getDocumentId());
    }

    private void showDefaultIcon(String iconPath) {
        try {
            // Tải icon nhỏ
            imgPreview.setImage(new Image(getClass().getResourceAsStream(iconPath), 80, 80, true, true));
            imgPreview.setFitHeight(80.0);
            imgPreview.setFitWidth(80.0);
        } catch (NullPointerException e) {
            imgPreview.setImage(null);
        }
    }

    private ListCell<Document> createDocumentCell() {
        return new ListCell<>() {
            private final Hyperlink fileLink = new Hyperlink();
            private final Label dateLabel = new Label();
            private final ImageView viewIcon = new ImageView(); // MỚI: ImageView cho icon "mắt"
            private final VBox fileInfo = new VBox(fileLink, dateLabel);
            // Sửa HBox để chứa fileInfo và viewIcon
            private final HBox hBox = new HBox(10, fileInfo, viewIcon);

            {
                hBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(fileInfo, javafx.scene.layout.Priority.ALWAYS);

                fileLink.setStyle("-fx-font-weight: bold;");
                dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

                // Thiết lập icon "mắt"
                try {
                    viewIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/org/example/educonnec_dacs4/image/view_eye_icon.png"))));
                } catch (Exception e) {
                    System.err.println("Không tìm thấy icon mắt.");
                }
                viewIcon.setFitWidth(20);
                viewIcon.setFitHeight(20);
                viewIcon.setStyle("-fx-cursor: hand;");

                // Xử lý sự kiện click trên Hyperlink (TÊN FILE) để TẢI VỀ (Yêu cầu cũ)
                fileLink.setOnAction(e -> {
                    if (getItem() != null) {
                        handleDownloadFile(getItem().getFilePath(), getItem().getFilename());
                    }
                });

                // Xử lý sự kiện click trên ICON MẮT để MỞ GIAO DIỆN XEM FILE (Yêu cầu MỚI)
                viewIcon.setOnMouseClicked(event -> {
                    if (getItem() != null) {
                        openFileViewer(getItem()); // Gọi hàm mở giao diện xem file
                        // Ghi log xem (view log)
                        client.send("MARK_DOCUMENT_VIEWED|" + getItem().getDocumentId());
                        // Cập nhật list File đã xem gần đây (Client-side tracking)
                        updateRecentDownloadsList(getItem());
                    }
                });

                // Khi click vào cell (trừ nút Tải về), ta sẽ CHỌN cell để kích hoạt Listener
                this.setOnMouseClicked(event -> {
                    if (getItem() != null) {
                        // Chọn mục này để kích hoạt Listener (dùng cho các mục đích khác nếu cần)
                        getListView().getSelectionModel().select(getItem());
                    }
                });
            }

            @Override
            protected void updateItem(Document item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String fileIcon = switch (item.getFileType().toLowerCase()) {
                        case "pdf" -> "📄 ";
                        case "png", "jpg", "jpeg", "gif" -> "🖼️ ";
                        case "doc", "docx" -> "📝 ";
                        default -> "📁 ";
                    };

                    fileLink.setText(fileIcon + item.getFilename());
                    dateLabel.setText("Tải lên: " + item.getCreatedAt());

                    // Ẩn icon mắt nếu loại file không hỗ trợ xem trước
                    String fileType = item.getFileType().toLowerCase();
                    boolean supported = fileType.matches("pdf|doc|docx|txt|html|png|jpg|jpeg|gif");
                    viewIcon.setVisible(supported);
                    viewIcon.setManaged(supported);

                    setGraphic(hBox);
                }
            }
        };
    }
    private void openFileViewer(Document doc) {
        try {
            // Tải FXML của giao diện xem file
            javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/org/example/educonnec_dacs4/FileViewer.fxml"));
            javafx.scene.Parent parent = fxmlLoader.load();

            // Lấy Controller và thiết lập dữ liệu
//            FileViewerController viewerController = fxmlLoader.getController();
//            viewerController.setDocument(doc.getFilename(), doc.getFilePath(), doc.getFileType());

            // Tạo Stage mới (cửa sổ mới)
            Stage stage = new Stage();
            stage.setTitle("Xem Tài liệu: " + doc.getFilename());
            stage.setScene(new javafx.scene.Scene(parent));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL); // Chặn tương tác với cửa sổ chính
            stage.show();

        } catch (IOException e) {
            System.err.println("Không thể mở giao diện xem file: " + e.getMessage());
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Không thể tải giao diện xem tệp tin.").show();
        }
    }
    // --- Logic Xử lý View Document (MỚI) ---
    private void handleViewDocument(String fileUrl, String filename) {
        // Tùy chọn 1: Tải file tạm thời và mở bằng Desktop (phổ biến)
        new Thread(() -> {
            try {
                // Tải file vào thư mục tạm thời
                File tempFile = downloadTempFile(fileUrl, filename);
                if (tempFile != null) {
                    Platform.runLater(() -> {
                        // Mở file bằng ứng dụng mặc định của hệ thống
                        if (Desktop.isDesktopSupported()) {
                            try {
                                Desktop.getDesktop().open(tempFile);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            new Alert(Alert.AlertType.WARNING, "Chức năng mở file không được hỗ trợ trên hệ thống này.").show();
                        }
                    });
                }
            } catch (IOException e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Lỗi khi xem file: " + e.getMessage()).show());
            }
        }).start();

        // Tùy chọn 2 (Phức tạp hơn): Nếu muốn xem ngay trên giao diện JavaFX (Cần thư viện bên thứ 3)
        // Ví dụ: Dùng WebView để hiển thị PDF, hoặc Apache POI để đọc DOCX.
        // Tuy nhiên, việc này rất phức tạp và thường dùng cách 1 (Mở bằng Desktop)
    }

    // Hàm phụ trợ để tải file tạm thời (dùng cho cả View và Download)
    private File downloadTempFile(String fileUrl, String suggestedFileName) throws IOException {
        URL url = new URL(fileUrl);
        URLConnection connection = url.openConnection();

        // Tạo file tạm thời với tiền tố (prefix) và hậu tố (suffix)
        String suffix = suggestedFileName.substring(suggestedFileName.lastIndexOf('.'));
        File tempFile = File.createTempFile("educonnect_view_", suffix);
        tempFile.deleteOnExit(); // Tự động xóa khi ứng dụng thoát

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return tempFile;
        }
    }


    // --- Giao tiếp với Server ---

    private void loadDocuments() {
        client.send("GET_ALL_DOCUMENTS");
        client.send("GET_MY_FILES"); // Yêu cầu lấy File của tôi
        client.send("GET_RECENTLY_VIEWED_FILES");
    }

    private void handleMessage(String cmd, String payload) {
        switch (cmd) {
            case "ALL_DOCUMENTS_LIST" -> updateDocumentList(allDocuments, payload);
            case "MY_FILES_LIST" -> updateDocumentList(myFiles, payload); // Xử lý list My Files
            case "RECENTLY_VIEWED_FILES" -> updateDocumentList(recentDownloads, payload);
            default -> { /* Bỏ qua các lệnh không liên quan */ }
        }
    }

    private void updateDocumentList(ObservableList<Document> list, String payload) {
        list.clear();
        if (payload != null && !payload.isEmpty()) {
            for (String item : payload.split(";;")) {
                String[] p = item.split("\\|", -1);
                if (p.length == 8) {
                    try {
                        int documentId = Integer.parseInt(p[0]);
                        int userId = Integer.parseInt(p[1]);

                        Integer convId = p[2].isEmpty() || p[2].equalsIgnoreCase("NULL") ? null : Integer.parseInt(p[2]);
                        Integer groupId = p[3].isEmpty() || p[3].equalsIgnoreCase("NULL") ? null : Integer.parseInt(p[3]);

                        String filename = p[4];
                        String filePath = p[5];
                        String fileType = p[6];
                        String createdAt = p[7];

                        Document doc = new Document(documentId, userId, convId, groupId, filename, filePath, fileType, createdAt);
                        // Chỉ thêm vào list nếu nó chưa có
                        if (list.stream().noneMatch(d -> d.getDocumentId() == documentId)) {
                            list.add(doc);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Lỗi parse dữ liệu tài liệu: " + item);
                    }
                }
            }
        }
        Platform.runLater(() -> { /* refresh nếu cần */ });
    }

    // --- Logic Tải về ---

    private void handleDownloadFile(String fileUrl, String suggestedFileName) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "Không có đường dẫn tải về.").show();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu Tệp Tin");
        fileChooser.setInitialFileName(suggestedFileName);

        Stage stage = (Stage) btnHome.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);

        if (saveFile != null) {
            new Thread(() -> {
                try {
                    // Tái sử dụng logic tải file
                    downloadFileToLocation(fileUrl, saveFile);
                    Platform.runLater(() -> {
                        new Alert(Alert.AlertType.INFORMATION, "Tải tệp tin thành công:\n" + saveFile.getAbsolutePath()).show();
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        new Alert(Alert.AlertType.ERROR, "Lỗi khi tải tệp tin: " + e.getMessage()).show();
                    });
                }
            }).start();
        }
    }

    private void downloadFileToLocation(String fileUrl, File saveFile) throws IOException {
        URLConnection connection = new URL(fileUrl).openConnection();

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(saveFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
    private void updateRecentDownloadsList(Document doc) {
        // Cần đảm bảo hàm này vẫn hoạt động để cập nhật lvRecentDownloads
        recentDownloads.removeIf(d -> d.getDocumentId() == doc.getDocumentId());
        recentDownloads.add(0, doc);
        if (recentDownloads.size() > 10) {
            recentDownloads.remove(10, recentDownloads.size());
        }
        Platform.runLater(lvRecentDownloads::refresh);
    }
    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Đăng xuất");
        alert.setHeaderText("Bạn có chắc muốn đăng xuất?");
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                NetworkClient.getInstance().disconnect();
                SceneManager.changeScene("login.fxml");
            }
        });
    }

}