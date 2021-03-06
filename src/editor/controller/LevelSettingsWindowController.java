package editor.controller;

import editor.ui.LevelPane;
import io.LevelSettings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LevelSettingsWindowController implements LevelEditorController {
	
	@FXML public TextField tileMapHeight;
	@FXML public TextField tileMapWidth;
	@FXML public TextField tileSize;
	@FXML public Button okBtn;

	@FXML public void initialize() {
	}
	
	public void getSettings(MainWindowController controller) {
		okBtn.setOnAction(e -> {
			int width = Integer.parseInt(tileMapWidth.getText());
			int height = Integer.parseInt(tileMapHeight.getText());
			int tile = Integer.parseInt(tileSize.getText());
			LevelSettings levelSettings = new LevelSettings(width, height, tile);
			controller.openedFile = null;
			controller.setNewLevel(levelSettings);
			Stage stage = (Stage) okBtn.getScene().getWindow();
			stage.close();
		});
	}
	
	public void getUpdatedSettings(LevelPane levelPane) {
		okBtn.setOnAction(e -> {
			int width = Integer.parseInt(tileMapWidth.getText());
			int height = Integer.parseInt(tileMapHeight.getText());
			int tile = Integer.parseInt(tileSize.getText());
			LevelSettings levelSettings = new LevelSettings(width, height, tile);
			levelPane.updateMapSize(levelSettings, width * tile, height * tile);
			Stage stage = (Stage) okBtn.getScene().getWindow();
			stage.close();
		});
	}
}
