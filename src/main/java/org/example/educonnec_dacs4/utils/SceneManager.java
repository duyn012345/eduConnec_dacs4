// File: EduConnect-Client/src/main/java/org/example/educonnec_dacs4/utils/SceneManager.java

package org.example.educonnec_dacs4.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class SceneManager {
    private static Stage stage;
    private static Stage modalStage;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void changeScene(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/fxml/" + fxml));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Không tìm thấy file:" + fxml);
        }
    }
    public static <T> T showModal(String fxmlFileName, String title, javafx.util.Callback<T, Void> callback) {
        try {
            try {
                FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/fxml/" + fxmlFileName));
                Parent root = loader.load();

                T controller = loader.getController();

                modalStage = new Stage();
                modalStage.initModality(Modality.APPLICATION_MODAL);
                modalStage.initStyle(StageStyle.UTILITY);
                modalStage.setTitle(title);
                modalStage.setResizable(false);

                Scene scene = new Scene(root);
                modalStage.setScene(scene);

                // Gọi callback để truyền dữ liệu (ví dụ: setUser)
                if (callback != null) {
                    callback.call(controller);
                }

                modalStage.showAndWait(); // Chặn cho đến khi đóng

                return controller;
            } catch (IOException e) {
                e.printStackTrace();
                showError("Không thể mở: " + fxmlFileName);
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Đóng modal hiện tại
     */
    public static void closeModal() {
        if (modalStage != null && modalStage.isShowing()) {
            modalStage.close();
        }
    }

    private static void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi giao diện");
        alert.setContentText(msg);
        alert.show();
    }
}
