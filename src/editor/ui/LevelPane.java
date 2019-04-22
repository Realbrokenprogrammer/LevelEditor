package editor.ui;

import java.awt.Point;
import java.util.ArrayList;

import editor.controller.MainWindowController;
import editor.entities.GameObject;
import editor.entities.Property;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * Graphical component for the level editor. This Pane contains all visual
 * representations that a level has. This is where objects get placed and edited
 * and all functionality for interacting with the level is implemented in this
 * class.
 * 
 * @author Jesper Bergstrom
 * @name LevelPane.java
 * @version 0.00.00
 */
public class LevelPane extends Canvas {
	
	private MainWindowController mainController;

	private GraphicsContext g;
	private ArrayList<Point> grid;
	private ArrayList<ArrayList<GameObject>> levelMap;
	private ArrayList<GameObject> selectedObjects;
	private ArrayList<GameObject> clipboard;
	private Rectangle selectRectangle;
	private GameObject[] allObjects;
	private GameObject currentObject;
	private int currentLayer = 4;
	private double scale = 1.0;
	private double objectScale = 1.0;
	private final int TILE_SIZE = 32;
	
	private double mouseX = 0;
	private double mouseY = 0;
	private double prevX = 0;
	private double prevY = 0;
	
	private double viewportX = 0;
	private double viewportY = 0;

	private int movingIndex = -1;
	private boolean isSelecting = false;
	private boolean isDragging = false;
	private boolean isMouseDown = false;
	private boolean isAltDown = false;
	private boolean isCtrlDown = false;
	private boolean isSDown = false;
	private boolean snapToGrid = true;
	
	/*
	 * TODO:
	 * - Only view selected layer (implement layers)
	 * - Undo + Redo
	 * - Improve zooming (center the zoom)
	 * - Place objects continuously (by holding down mouse btn + some hotkey)
	 * - Handle overlapping objects some way
	 * - Improve placement of pasted objects + select them when pasted
	 * - Load all assets from file
	 * - Export level file
	 */

	/**
	 * Constructor initializes all members of the LevelPane and adds listeners
	 * for the scrollbars in the scrollpane.
	 */
	public LevelPane(MainWindowController mainController) {
		this.mainController = mainController;
		clipboard = new ArrayList<GameObject>();
		selectedObjects = new ArrayList<GameObject>();
		levelMap = new ArrayList<ArrayList<GameObject>>();
		for (int i = 0; i < 8; i++) {
			levelMap.add(new ArrayList<GameObject>());
		}
		g = this.getGraphicsContext2D();
	}
	
	/**
	 * Draws every object that will be displayed in the LevelPane.
	 */
	public void draw() {
		g.setFill(new Color(0.2, 0.2, 0.2, 1));
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		g.scale(scale, scale);
		
		g.setFill(new Color(0, 0, 1, 0.3));
		if (selectRectangle != null) {
			Rectangle r = getAdjustedRect();
			getSelectedObjects(r);
			g.fillRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
		}
		g.setStroke(new Color(0.5, 0.5, 0.5, 1));
		
		// Draw grid.
		for (int i = 0; i < grid.size(); i++) {
			double x = grid.get(i).x + viewportX;
			double y = grid.get(i).y + viewportY;
			g.strokeRect(x, y, TILE_SIZE, TILE_SIZE);
		}
		g.setFill(Color.BLACK);
		
		// Draw all placed objects.
		for (int i = 0; i < levelMap.size(); i++) {
			for (int j = 0; j < levelMap.get(i).size(); j++) {
				GameObject t = levelMap.get(i).get(j);
				for (int k = 0; k < allObjects.length; k++) {
					if (t.getObjectName() == allObjects[k].getObjectName()) {
						g.scale(t.scale, t.scale);
						g.drawImage(allObjects[k].image, (t.x + viewportX) / t.scale, (t.y + viewportY) / t.scale);
						g.scale(1 / t.scale, 1 / t.scale);
					}
				}
			}
		}
		
		// Draw selected objects.
		for (int i = 0; i < selectedObjects.size(); i++) {
			GameObject t = selectedObjects.get(i);
			WritableImage img = null;
			for (int j = 0; j < allObjects.length; j++) {
				if (t.getObjectName() == allObjects[j].getObjectName()) {
					img = allObjects[j].selectedPixels;
					break;
				}
			}
			double s = 10.0 * ((t.width) / (double) (img.getWidth() - 10.0));
			g.scale(t.scale, t.scale);
			g.drawImage(img, ((t.x - s / 2) + viewportX) / t.scale, ((t.y - s / 2) + viewportY) / t.scale);
			g.scale(1 / t.scale, 1 / t.scale);
		}
		
		// Draw current object at cursor.
		if (currentObject != null) {
			GameObject t = currentObject;
			Image img = new Image("file:" + t.imageURL, t.width * objectScale, t.height * objectScale, false, false);
			double x = mouseX / scale;
			double y = mouseY / scale;
			if (snapToGrid) {
				Point p = findClosestGridPoint(x, y);
				g.drawImage(img, p.x + viewportX, p.y + viewportY);
			} else {
				g.drawImage(img, x - (t.width) / 2, y - (t.height) / 2);
			}
		}
		
		g.scale(1 / scale, 1 / scale);
	}
	
	public void setAllObjects(GameObject[] allObjects) {
		this.allObjects = allObjects;
	}
	
	private void getSelectedObjects(Rectangle r) {
		selectedObjects.clear();
		for (int i = 0; i < levelMap.get(currentLayer - 1).size(); i++) {
			GameObject o = levelMap.get(currentLayer - 1).get(i);
			if (r.intersects(o.x + viewportX, o.y + viewportY, o.width, o.height)) {
				selectedObjects.add(o);
			}
		}
	}
	
	private Rectangle getAdjustedRect() {
		double rY = selectRectangle.getY();
		double rX = selectRectangle.getX();
		double width = selectRectangle.getWidth();
		double height = selectRectangle.getHeight();
		if (width < 0) {
			rX += width;
			width *= -1;
		}
		if (height < 0) {
			rY += height;
			height *= -1;
		}
		return new Rectangle(rX, rY, width, height);
	}
	
	private Point findClosestGridPoint(double x, double y) {
		Point result = grid.get(0);
		for (int i = 0; i < grid.size(); i++) {
			double adjustedX = x - TILE_SIZE / 2 - viewportX;
			double adjustedY = y - TILE_SIZE / 2 - viewportY;
			if (grid.get(i).distance(adjustedX, adjustedY) <
					result.distance(adjustedX, adjustedY)) {
				result = grid.get(i);
			}
		}
		return result;
	}

	/**
	 * Initializes a level. Sets the size for the map pane and calculates all
	 * points for the grid. This method also adds all necessary listeners.
	 * 
	 * @param width
	 * @param height
	 */
	public void setMapSize(int width, int height) {
		setGrid(width, height);

		Stage stage = (Stage) this.getScene().getWindow();
		this.setWidth(stage.getWidth() - 10);
		this.setHeight(stage.getHeight() - 91);

		stage.widthProperty().addListener((obs, oldVal, newVal) -> {
			this.setWidth(stage.getWidth() - 10);
			draw();
		});

		stage.heightProperty().addListener((obs, oldVal, newVal) -> {
			this.setHeight(stage.getHeight() - 91);
			mainController.objectPanel.setMinHeight(stage.getHeight());
			mainController.propertyPanel.setMinHeight(stage.getHeight());
			draw();
		});
		
		this.setOnMouseDragged(e -> {
			if (!isMouseDown) {
				isMouseDown = true;
			} else {
				isDragging = true;
				if (e.getButton() == MouseButton.SECONDARY) {
					if (!isSelecting) {
						isSelecting = true;
						selectRectangle = new Rectangle(e.getX() / scale, e.getY() / scale, 0, 0);
					} else {
						selectRectangle.setWidth(e.getX() / scale - selectRectangle.getX());
						selectRectangle.setHeight(e.getY() / scale - selectRectangle.getY());
					}
				} else if (e.getButton() == MouseButton.PRIMARY) {
					if (isOverSelected()) {
						movingIndex = getDragObjectIndex();
						moveSelectedObjects(e.getX(), e.getY());
					} else if (movingIndex != -1) {
						moveSelectedObjects(e.getX(), e.getY());
					} else {
						viewportX -= prevX - e.getX() / scale;
						viewportY -= prevY - e.getY() / scale;
					}
				}
			}
			mouseX = e.getX();
			mouseY = e.getY();
			prevX = e.getX() / scale;
			prevY = e.getY() / scale;
			draw();
		});

		this.getScene().setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.CONTROL) {
				isCtrlDown = true;
			}
			if (e.getCode() == KeyCode.ALT) {
				isAltDown = true;
			}
			if (e.getCode() == KeyCode.S && !isSDown) {
				isSDown = true;
				if (!snapToGrid) {
					snapToGrid = true;
				} else {
					snapToGrid = false;
				}
			}
			if (e.getCode() == KeyCode.C) {
				copyToClipboard();
			}
			if (e.getCode() == KeyCode.V) {
				pasteClipboard();
			}
		});

		this.getScene().setOnKeyReleased(e -> {
			if (e.getCode() == KeyCode.CONTROL) {
				isCtrlDown = false;
			}
			if (e.getCode() == KeyCode.ALT) {
				isAltDown = false;
			}
			if (e.getCode() == KeyCode.S) {
				isSDown = false;
			}
			if (e.getCode() == KeyCode.DELETE) {
				deleteSelected();
			}
			draw();
		});

		this.getScene().addEventFilter(ScrollEvent.ANY, e -> {
			if (isCtrlDown) {
				if (e.getDeltaY() > 0) {
					// Zoom in
					if (scale < 3) {
						scale += 0.2;
					}
				} else if (e.getDeltaY() < 0){
					// Zoom out
					if (scale > 0.4) {
						scale -= 0.2;
					}
				}
			} else if (isAltDown && currentObject != null) {
				if (e.getDeltaY() > 0) {
					if (objectScale < 5) {
						objectScale += 0.2;
					}
				} else {
					if (objectScale > 0.6) {
						objectScale -= 0.2;
					}
				}
			}

			draw();
			e.consume();
		});

		this.setOnMouseMoved(e -> {
			mouseX = e.getX();
			mouseY = e.getY();
			draw();
		});

		this.setOnMousePressed(e -> {
			
		});
		
		this.setOnMouseReleased(e -> {
			if (e.getButton() == MouseButton.PRIMARY && !isDragging) {
				if (currentObject != null) {
					placeObject();
				} else {
					selectObject();
				}
			}
			if (e.getButton() == MouseButton.SECONDARY && !isDragging) {
				selectObject();
			}
			movingIndex = -1;
			isMouseDown = false;
			isDragging = false;
			selectRectangle = null;
			isSelecting = false;
			draw();
		});

		draw();
	}
	
	private void copyToClipboard() {
		if (!selectedObjects.isEmpty()) {
			clipboard.clear();
			for (int i = 0; i < selectedObjects.size(); i++) {
				clipboard.add(selectedObjects.get(i));
			}
		}
	}
	
	private void pasteClipboard() {
		for (int i = 0; i < clipboard.size(); i++) {
			GameObject t = new GameObject();
			t.type = clipboard.get(i).type;
			t.setObjectName(clipboard.get(i).getObjectName());
			t.imageURL = "res/sprites/" + t.getObjectName() + ".png";
			t.width = clipboard.get(i).width;
			t.height = clipboard.get(i).height;
			t.scale = clipboard.get(i).scale;
			t.x = clipboard.get(i).x + 20;
			t.y = clipboard.get(i).y + 20;
			t.properties = clipboard.get(i).properties;
			levelMap.get(currentLayer - 1).add(t);
		}
	}
	
	private void deleteSelected() {
		for (int i = 0; i < selectedObjects.size(); i++) {
			for (int j = 0; j < levelMap.size(); j++) {
				if (levelMap.get(j).contains(selectedObjects.get(i))) {
					levelMap.get(j).remove(selectedObjects.get(i));
					break;
				}
			}
		}
		selectedObjects.clear();
	}
	
	private void moveSelectedObjects(double x, double y) {
		double snapX = 0;
		double snapY = 0;
		
		if (snapToGrid) {
			Point p = findClosestGridPoint(x / scale, y / scale);
			snapX = p.x - selectedObjects.get(movingIndex).x;
			snapY = p.y - selectedObjects.get(movingIndex).y;
		}
		
		for (int i = 0; i < selectedObjects.size(); i++) {
			double nwX = selectedObjects.get(i).x - (prevX - x / scale);
			double nwY = selectedObjects.get(i).y - (prevY - y / scale);
			if (snapToGrid) {
				nwX = selectedObjects.get(i).x + snapX;
				nwY = selectedObjects.get(i).y + snapY;
			}
			selectedObjects.get(i).x = nwX;
			selectedObjects.get(i).y = nwY;
		}
	}
	
	private boolean isOverSelected() {
		for (int i = 0; i < selectedObjects.size(); i++) {
			GameObject o = selectedObjects.get(i);
			if (o.contains(mouseX / scale - viewportX, mouseY / scale - viewportY)) {
				return true;
			}
		}
		return false;
	}
	
	private int getDragObjectIndex() {
		for (int i = 0; i < selectedObjects.size(); i++) {
			GameObject o = selectedObjects.get(i);
			if (o.contains(mouseX / scale - viewportX, mouseY / scale - viewportY)) {
				return i;
			}
		}
		return -1;
	}

	private void selectObject() {
		for (int i = levelMap.get(currentLayer - 1).size() - 1; i >= 0; i--) {
			GameObject o = levelMap.get(currentLayer - 1).get(i);
			if (o.contains(mouseX / scale - viewportX, mouseY / scale - viewportY)) {
				if (!selectedObjects.contains(o)) {
					hideProperties();
					selectedObjects.clear();
					selectedObjects.add(o);
					showProperties();
				}
				draw();
				return;
			}
		}
		hideProperties();
		selectedObjects.clear();
		draw();
	}
	
	private void showProperties() {
		mainController.propertyScroll.setVisible(true);
		while (mainController.propertyPanel.getChildren().size() > 1) {
			mainController.propertyPanel.getChildren().remove(1);
		}
		for (int i = 0; i < selectedObjects.get(0).properties.length; i++) {
			Property p = selectedObjects.get(0).properties[i];
			HBox hbox = new HBox();
			hbox.setSpacing(5);
			hbox.setPadding(new Insets(0, 0, 0, 5));
			Text t = new Text(p.name + ":");
			TextField tf = new TextField();
			tf.setText(p.value);
			hbox.getChildren().add(t);
			hbox.getChildren().add(tf);
			mainController.propertyPanel.getChildren().add(hbox);
		}
	}
	
	private void hideProperties() {
		mainController.propertyScroll.setVisible(false);
		if (selectedObjects.size() > 0) {
			for (int i = 1; i < mainController.propertyPanel.getChildren().size(); i++) {
				HBox hbox = (HBox) mainController.propertyPanel.getChildren().get(i);
				TextField tf = (TextField) hbox.getChildren().get(1);
				selectedObjects.get(0).properties[i - 1].value = tf.getText();
			}	
		}
		while (mainController.propertyPanel.getChildren().size() > 1) {
			mainController.propertyPanel.getChildren().remove(1);
		}
	}

	private void placeObject() {
		GameObject o = copyCurrentObject();
		double x = mouseX / scale;
		double y = mouseY / scale;
		if (snapToGrid) {
			Point p = findClosestGridPoint(x, y);
			o.x = p.x;
			o.y = p.y;
		} else {
			o.x = x - o.width / 2 - viewportX;
			o.y = y - o.height / 2 - viewportY;
		}
		levelMap.get(currentLayer - 1).add(o);
		draw();
	}

	private GameObject copyCurrentObject() {
		GameObject t = new GameObject();
		t.type = currentObject.type;
		t.setObjectName(currentObject.getObjectName());
		t.imageURL = "res/sprites/" + t.getObjectName() + ".png";
		t.width = currentObject.width * objectScale;
		t.height = currentObject.height * objectScale;
		t.scale = objectScale;
		return t;
	}

	/**
	 * Calculates all the points for the grid.
	 * 
	 * @param width
	 * @param height
	 */
	private void setGrid(int width, int height) {
		grid = new ArrayList<Point>();
		for (int i = 0; i < width / TILE_SIZE; i++) {
			for (int j = 0; j < height / TILE_SIZE; j++) {
				grid.add(new Point((int) (i * TILE_SIZE), (int) (j * TILE_SIZE)));
			}
		}
	}

	public void setCurrentObject(GameObject currentObject) {
		this.currentObject = currentObject;
	}

	public GameObject getCurrentObject() {
		return this.currentObject;
	}
}
