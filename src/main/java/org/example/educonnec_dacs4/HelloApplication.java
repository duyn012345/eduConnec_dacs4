// File: EduConnect-Client/src/main/java/org/example/educonnec_dacs4/HelloApplication.java

package org.example.educonnec_dacs4;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.educonnec_dacs4.utils.SceneManager;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {
        SceneManager.init(stage);
        SceneManager.changeScene("welcom.fxml");  // hoặc login.fxml
        stage.setTitle("EduConnect - Kết nối học tập");
        stage.setResizable(false);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}
//package org.example.educonnec_dacs4;
//
//import javafx.application.Application;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Scene;
//import javafx.stage.Stage;
//
//public class HelloApplication extends Application {
//
//    private static Stage mainStage;
//    @Override
//    public void start(Stage stage) throws Exception {
//        mainStage = stage;
//        changeScene("welcom.fxml");
//        stage.setTitle("EduConnect");
//        stage.show();
//    }
//    public static void changeScene(String fxml) throws Exception {
//        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxml));
//        Scene scene = new Scene(loader.load());
//        mainStage.setScene(scene);
//    }
//
//    public static void main(String[] args) {
//        launch();
//    }
//}
