package editor.controller;

import io.LevelSettings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LevelSettingsWindowController implements LevelEditorController {
	
	@FXML public TextField tileMapHeight;
	@FXML public TextField tileMapWidth;
	@FXML public Button okBtn;

	@FXML public void initialize() {
	}
	
	public void getSettings(MainWindowController controller) {
		okBtn.setOnAction(e -> {
			int width = Integer.parseInt(tileMapWidth.getText());
			int height = Integer.parseInt(tileMapHeight.getText());
			LevelSettings levelSettings = new LevelSettings(width, height);
			controller.openedFile = null;
			controller.setNewLevel(levelSettings);
			Stage stage = (Stage) okBtn.getScene().getWindow();
			stage.close();
		});
	}
}
