package org.example.educonnec_dacs4.controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.educonnec_dacs4.client.NetworkClient;

import java.util.Map;
import java.util.stream.Collectors;

public class CreateGroupController {

    @FXML private TextField tfGroupName;
    @FXML private ListView<String> lvFriendsSelection;
    @FXML private Button btnCancel;

    private Stage popupStage;
    private final NetworkClient client = NetworkClient.getInstance();
    private Map<String, String> nameToUsernameMap; // Lấy từ ChatController

    public void setPopupStage(Stage stage) {
        this.popupStage = stage;
        btnCancel.setOnAction(e -> popupStage.close());
    }

    // Hàm nhận dữ liệu bạn bè từ ChatController
    public void setFriendData(ObservableList<String> friendNames, Map<String, String> nameToUsernameMap) {
        lvFriendsSelection.setItems(friendNames);
        this.nameToUsernameMap = nameToUsernameMap;
        lvFriendsSelection.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @FXML
    private void handleCreateGroup() {
        String groupName = tfGroupName.getText().trim();
        ObservableList<String> selectedNames = lvFriendsSelection.getSelectionModel().getSelectedItems();

        if (groupName.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Vui lòng nhập tên nhóm.").show();
            return;
        }

        if (selectedNames.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Vui lòng chọn ít nhất một người bạn.").show();
            return;
        }

        // Chuyển tên hiển thị thành danh sách usernames để gửi Server
        String selectedUsernames = selectedNames.stream()
                .map(nameToUsernameMap::get)
                .collect(Collectors.joining(","));

        // Gửi lệnh: CREATE_GROUP|Tên Nhóm|username1,username2,...
        client.send("CREATE_GROUP|" + groupName + "|" + selectedUsernames);

        popupStage.close();
    }
}