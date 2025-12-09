module org.example.educonnec_dacs4 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.desktop;
    requires javafx.swing;
   // requires org.bytedeco.javacpp;
    requires org.bytedeco.javacv;
    requires cloudinary.core;
    //requires org.bytedeco.opencv;
    requires javafx.web;

    opens org.example.educonnec_dacs4 to javafx.fxml;
    exports org.example.educonnec_dacs4;

    exports org.example.educonnec_dacs4.controllers;
    opens org.example.educonnec_dacs4.controllers to javafx.fxml;

    opens org.example.educonnec_dacs4.model to com.google.gson;

    exports org.example.educonnec_dacs4.utils;
    opens org.example.educonnec_dacs4.utils to javafx.fxml;

}
