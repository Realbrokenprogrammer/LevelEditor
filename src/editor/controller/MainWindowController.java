package editor.controller;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainWindowController implements LevelEditorController {
	
	@FXML public MenuItem menuFileNew;
	@FXML public Canvas canvas;
	@FXML public ScrollPane scrollPane;
	@FXML public AnchorPane anchorPane;
	@FXML public HBox layerBar;
	
	private LevelSettings levelSettings;
	private int[][] tileMap;
	private int currentLayer = 4;
	
	private final int TILE_SIZE = 50;
	
	/*
	Layers:
	1 - Foreground General 2
		Tiles can't be placed here. Objects can't kill you. This is most for foreground looks / paralax.
	2 - Foreground Tile
		Tile layer that goes over the main ground layer. Might be used to hide things. Player won't interract with this.
	3 - Foreground Tile General
		Can't place tiles here but you can place objects.
	4 - Active Tile
		Everything is collideable here. Player will interract with objects and tiles here.
	5 - Back Tile General
		Mostly for objects. Things you want to place behind walls like traps etc.
	6 - Back Tile
		Background tiles.
	7 - Back General 2
		For paralax.
	8 - Back General 3
		For background.
	
	Props:
		* Rocks
	Animated props:
		* Rain
		* Clouds
	Objects: - Usually things you interract with or kill you.
		* End level object. (Object you touch to "win")
		* Traps, saws, lava.
		NOTE: Should be able to set a start and finish destination to make the object move between those positions.
*/
	
	@FXML public void initialize() {
		AnchorPane.setTopAnchor(scrollPane, 0.0);
		AnchorPane.setBottomAnchor(scrollPane, 0.0);
		AnchorPane.setLeftAnchor(scrollPane, 0.0);
		AnchorPane.setRightAnchor(scrollPane, 0.0);
		
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
		this.canvas.setWidth(levelSettings.width * TILE_SIZE + 15);
		this.canvas.setHeight(levelSettings.height * TILE_SIZE + 15);
		Stage stage = (Stage) this.canvas.getScene().getWindow();
		int width = (int) (this.canvas.getWidth() + 7);
		int height = (int) (this.canvas.getHeight() + 55);
		
		// TODO: Get size of user screen.
		if (width > 1600) {
			width = 1600;
		}
		if (height > 900) {
			height = 900;
		}
		stage.setWidth(width);
		stage.setHeight(height);
		
		canvas.setOnMouseMoved(e -> {
			drawGrid(levelSettings, e.getX(), e.getY());
		});
		
		ObservableList<Node> list = layerBar.getChildren();
		for(int i = 1; i < list.size(); i++) {
			Pane p = (Pane) list.get(i);
			p.setOnMouseEntered(e -> {
				p.setStyle("-fx-background-color: #CCCCCC");
			});
			p.setOnMouseExited(e -> {
				p.setStyle("-fx-background-color: #FFFFFF");
			});
			p.setOnMouseClicked(e -> {
				Text t = (Text) p.getChildren().get(0);
				currentLayer = Integer.parseInt(t.getText());
				System.out.println(t.getText());
			});
		}
	}
	
	private void drawGrid(LevelSettings levelSettings, double mouseX, double mouseY) {
		int x = (int) (mouseX / TILE_SIZE);
		int y = (int) (mouseY / TILE_SIZE);
		
		GraphicsContext g = this.canvas.getGraphicsContext2D();
		g.clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
		g.setStroke(Color.GRAY);
		for (int i = 0; i < levelSettings.width; i++) {
			for (int j = 0; j < levelSettings.height; j++) {
				g.strokeRect(i * TILE_SIZE, j * TILE_SIZE, TILE_SIZE, TILE_SIZE);
			}
		}
		g.setStroke(Color.GREEN);
		g.setLineWidth(3);
		g.strokeRect(x * TILE_SIZE, (y * TILE_SIZE), TILE_SIZE, TILE_SIZE);
		g.setLineWidth(1);
	}
}
