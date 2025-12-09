//package org.example.educonnec_dacs4.controllers;
//
//import javafx.fxml.FXML;
//import javafx.scene.control.Label;
//import javafx.scene.web.WebView;
//import javafx.stage.Stage;
//
//public class FileViewerController {
//
//    @FXML private Label lblViewerTitle;
//    @FXML private WebView webView;
//
//    @FXML
//    public void initialize() {
//        // Tùy chỉnh ban đầu nếu cần
//    }
//
//    // Hàm này sẽ được gọi từ FileDocController
//    public void setDocument(String filename, String filePath, String fileType) {
//        lblViewerTitle.setText("Tài liệu đang xem: " + filename);
//
//        // Tùy chọn 1: Dùng Google Docs Viewer cho các loại file văn phòng
//        if (fileType.matches("pdf|doc|docx|ppt|pptx|xls|xlsx")) {
//            // URL của file phải là công khai (Public) để Google Viewer hoạt động
//            String viewerUrl = "https://docs.google.com/gview?url=" + filePath + "&embedded=true";
//            webView.getEngine().load(viewerUrl);
//        }
//        // Tùy chọn 2: Hiển thị ảnh trực tiếp
//        else if (fileType.matches("png|jpg|jpeg|gif")) {
//            webView.getEngine().load(filePath);
//        }
//        // Tùy chọn 3: Hiển thị nội dung dạng text (nếu có thể tải về và đọc text)
//        else {
//            // Hiển thị thông báo hoặc tải file và hiển thị nội dung text (phức tạp hơn)
//            String content = "Không hỗ trợ xem trước trực tiếp loại tệp: " + fileType.toUpperCase() + ". Vui lòng tải về.";
//            webView.getEngine().loadContent(content);
//        }
//    }
//
//    @FXML
//    private void handleClose() {
//        // Đóng cửa sổ hiện tại (Stage)
//        Stage stage = (Stage) lblViewerTitle.getScene().getWindow();
//        stage.close();
//    }
//}