package editor.controller;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainWindowController implements LevelEditorController {
	
	@FXML public MenuItem menuFileNew;
	@FXML public Canvas canvas;
	
	private LevelSettings levelSettings;
	private int[][] tileMap;
	
	private final int TILE_SIZE = 50;
	
	@FXML public void initialize() {
		menuFileNew.setOnAction(e -> {
			Parent root;
			URI location = new File("res/" + LEVEL_SETTINGS_WINDOW).toURI();
			LevelSettingsWindowController levelSettingsWindowController;
			
			try {
				FXMLLoader loader = new FXMLLoader(location.toURL());
				root = loader.load();
				
				Stage stage = new Stage();
				stage.setTitle("Level Settings");
				stage.setScene(new Scene(root));
				
				levelSettingsWindowController = loader.<LevelSettingsWindowController>getController();
				levelSettingsWindowController.getSettings(this);
				
				stage.initModality(Modality.APPLICATION_MODAL);
				stage.show();
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		});
	}
	
	public void setNewLevel(LevelSettings levelSettings) {
		this.levelSettings = levelSettings;
		this.tileMap = new int[levelSettings.width][levelSettings.height];
		this.canvas.setWidth(levelSettings.width * TILE_SIZE);
		this.canvas.setHeight(levelSettings.height * TILE_SIZE);
		Stage stage = (Stage) this.canvas.getScene().getWindow();
		stage.setWidth(this.canvas.getWidth() + 7);
		stage.setHeight(this.canvas.getHeight() + 55);
		stage.setResizable(false);
		
		GraphicsContext g = this.canvas.getGraphicsContext2D();
		for (int i = 0; i < levelSettings.width; i++) {
			for (int j = 0; j < levelSettings.height; j++) {
				g.strokeRect(i * TILE_SIZE, j * TILE_SIZE, TILE_SIZE, TILE_SIZE);
			}
		}
	}
}
