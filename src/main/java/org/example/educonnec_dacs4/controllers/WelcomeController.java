package org.example.educonnec_dacs4.controllers;

import javafx.event.ActionEvent;
import org.example.educonnec_dacs4.HelloApplication;
import org.example.educonnec_dacs4.utils.SceneManager;

public class WelcomeController {

    public void goLogin(ActionEvent e) throws Exception {
        SceneManager.changeScene("login.fxml");
    }
}
