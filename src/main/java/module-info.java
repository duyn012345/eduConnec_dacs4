module org.example.educonnec_dacs4 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;


    opens org.example.educonnec_dacs4 to javafx.fxml;
    exports org.example.educonnec_dacs4;

    exports org.example.educonnec_dacs4.controllers;
    opens org.example.educonnec_dacs4.controllers to javafx.fxml;

    opens org.example.educonnec_dacs4.model to com.google.gson;
}