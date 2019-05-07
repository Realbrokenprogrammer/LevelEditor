package editor.ui;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;

import editor.controller.MainWindowController;
import editor.entities.GameObject;
import editor.entities.Pair;
import editor.event.EditorEventHandler;
import io.Level;
import io.LevelFileManager;
import io.LevelSettings;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
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
	public EditorEventHandler eventHandler;
	private LevelFileManager levelFileManager;
	private LevelSettings levelSettings;

	private GraphicsContext g;
	private ArrayList<Point> grid;
	public ArrayList<ArrayList<GameObject>> levelMap;
	private ArrayList<GameObject> selectedObjects;
	private ArrayList<Pair<GameObject, Point>> movingObjects;
	private ArrayList<GameObject> clipboard;
	private Rectangle selectRectangle;
	private GameObject[] allObjects;
	private GameObject currentObject;
	private int currentLayer = 4;
	private double scale = 1.0;
	private double width = 0;
	private double height = 0;
	private double objectScale = 1.0;

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
	private boolean showOnlyCurrentLayer = false;
	private boolean highlightOverlaps = false;

	/*
	 * TODO: 
	 * - Ability to select from all layers
	 * - Header for level file
	 */

	public LevelPane(MainWindowController mainController) {
		this.mainController = mainController;
		levelFileManager = new LevelFileManager();
		clipboard = new ArrayList<GameObject>();
		selectedObjects = new ArrayList<GameObject>();
		levelMap = new ArrayList<ArrayList<GameObject>>();
		eventHandler = new EditorEventHandler();
		for (int i = 0; i < 8; i++) {
			levelMap.add(new ArrayList<GameObject>());
		}
		g = this.getGraphicsContext2D();
	}

	private void setGrid(int width, int height) {
		grid = new ArrayList<Point>();
		for (int i = 0; i < width / levelSettings.tileSize; i++) {
			for (int j = 0; j < height / levelSettings.tileSize; j++) {
				grid.add(new Point((int) (i * levelSettings.tileSize), (int) (j * levelSettings.tileSize)));
			}
		}
	}
	
	public void updateMapSize(LevelSettings levelSettings, int width, int height) {
		this.levelSettings = levelSettings;
		setGrid(width, height);
		this.width = width;
		this.height = height;
	}

	/**
	 * Draws every object that will be displayed in the LevelPane.
	 */
	public void draw() {
		g.setFill(new Color(0.2, 0.2, 0.2, 1));
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		g.scale(scale, scale);

		// Draw grid.
		g.setStroke(new Color(0.5, 0.5, 0.5, 1));
		for (int i = 0; i < grid.size(); i++) {
			double x = grid.get(i).x + viewportX;
			double y = grid.get(i).y + viewportY;
			if (isInView(x, y, levelSettings.tileSize, levelSettings.tileSize)) {
				g.strokeRect(x, y, levelSettings.tileSize, levelSettings.tileSize);
			}
		}
		g.setFill(Color.BLACK);

		// Draw all placed objects.
		for (int i = 0; i < levelMap.size(); i++) {
			if (showOnlyCurrentLayer) {
				i = currentLayer - 1;
			}
			for (int j = 0; j < levelMap.get(i).size(); j++) {
				GameObject t = levelMap.get(i).get(j);
				for (int k = 0; k < allObjects.length; k++) {
					if (t.getObjectName().equals(allObjects[k].getObjectName())) {
						double x = (t.x + viewportX) / t.scale;
						double y = (t.y + viewportY) / t.scale;
						g.scale(t.scale, t.scale);
						if (isInView(x, y, allObjects[k].image.getWidth(), allObjects[k].image.getHeight())) {
							g.drawImage(allObjects[k].image, x, y);
						}
						g.scale(1 / t.scale, 1 / t.scale);
					}
				}
			}
			if (showOnlyCurrentLayer) {
				break;
			}
		}

		// Highlight overlaps.
		if (highlightOverlaps) {
			for (int i = 0; i < levelMap.size(); i++) {
				for (int j = 0; j < levelMap.get(i).size(); j++) {
					for (int k = 0; k < levelMap.get(i).size(); k++) {
						GameObject o = levelMap.get(i).get(k);
						if (k != j && levelMap.get(i).get(j).overlaps(o)) {
							drawHighlight(o);
						}
					}
				}
			}
		}

		// Draw selected objects.
		for (int i = 0; i < selectedObjects.size(); i++) {
			GameObject t = selectedObjects.get(i);
			drawSelected(t);
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

		// Draw select rectangle.
		g.setFill(new Color(0, 0, 1, 0.3));
		if (selectRectangle != null) {
			Rectangle r = getAdjustedRect();
			getSelectedObjects(r);
			g.fillRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
		}

		g.scale(1 / scale, 1 / scale);
	}

	private boolean isInView(double x, double y, double width, double height) {
		if (x > -width && x < this.getWidth() / scale && y > -height && y < this.getHeight() / scale) {
			return true;
		}
		return false;
	}

	private void drawHighlight(GameObject o) {
		WritableImage img = null;
		for (int i = 0; i < allObjects.length; i++) {
			if (o.getObjectName().equals(allObjects[i].getObjectName())) {
				img = allObjects[i].highlightPixels;
				break;
			}
		}

		double s = 10.0 * ((o.width) / (double) (img.getWidth() - 10.0));
		g.scale(o.scale, o.scale);
		g.drawImage(img, ((o.x - s / 2) + viewportX) / o.scale, ((o.y - s / 2) + viewportY) / o.scale);
		g.scale(1 / o.scale, 1 / o.scale);
	}

	private void drawSelected(GameObject o) {
		WritableImage img = null;
		for (int i = 0; i < allObjects.length; i++) {
			if (o.getObjectName().equals(allObjects[i].getObjectName())) {
				img = allObjects[i].selectedPixels;
				break;
			}
		}

		double s = 10.0 * ((o.width) / (double) (img.getWidth() - 10.0));
		double x = ((o.x - s / 2) + viewportX) / o.scale;
		double y = ((o.y - s / 2) + viewportY) / o.scale;
		g.scale(o.scale, o.scale);
		if (isInView(x, y, img.getWidth(), img.getHeight())) {
			g.drawImage(img, x, y);
		}
		g.scale(1 / o.scale, 1 / o.scale);
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
			double adjustedX = x - levelSettings.tileSize / 2 - viewportX;
			double adjustedY = y - levelSettings.tileSize / 2 - viewportY;
			if (grid.get(i).distance(adjustedX, adjustedY) < result.distance(adjustedX, adjustedY)) {
				result = grid.get(i);
			}
		}
		return result;
	}

	public void save(File file) {
		Level level = new Level(levelSettings, levelMap);
		FileChooser fc = new FileChooser();
		if (file == null) {
			String filePath = fc.showSaveDialog(this.getScene().getWindow()).getAbsolutePath();
			levelFileManager.writeFile(level, filePath);
		} else {
			levelFileManager.writeFile(level, file.getAbsolutePath());
		}
		
	}

	/**
	 * Initializes a level. Sets the size for the map pane and calculates all
	 * points for the grid. This method also adds all necessary listeners.
	 * 
	 * @param width
	 * @param height
	 */
	public void setMapSize(LevelSettings levelSettings, int width, int height) {
		this.width = width;
		this.height = height;
		this.levelSettings = levelSettings;
		setGrid(width, height);

		Stage stage = (Stage) this.getScene().getWindow();
		this.setWidth(stage.getWidth() - 10);
		this.setHeight(stage.getHeight() - 115);

		stage.widthProperty().addListener((obs, oldVal, newVal) -> {
			this.setWidth(stage.getWidth() - 10);
			draw();
		});

		stage.heightProperty().addListener((obs, oldVal, newVal) -> {
			this.setHeight(stage.getHeight() - 115);
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
					if (isCtrlDown) {
						placeContinuously();
					} else {
						if (isOverSelected()) {
							if (movingIndex == -1) {
								initMovingEvent();
							}
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
				if (isCtrlDown) {
					save(mainController.openedFile);
				} else {
					if (!snapToGrid) {
						snapToGrid = true;
					} else {
						snapToGrid = false;
					}
				}
			}
			if (e.getCode() == KeyCode.C && isCtrlDown) {
				copyToClipboard();
			}
			if (e.getCode() == KeyCode.V && isCtrlDown) {
				pasteClipboard();
			}
			if (e.getCode() == KeyCode.Z && isCtrlDown) {
				eventHandler.undo(levelMap);
			}
			if (e.getCode() == KeyCode.Y && isCtrlDown) {
				eventHandler.redo(levelMap);
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
				} else if (e.getDeltaY() < 0) {
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
			if (movingIndex != -1) {
				addMovingEvent();
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

	private void initMovingEvent() {
		movingObjects = new ArrayList<Pair<GameObject, Point>>();
		for (int i = 0; i < selectedObjects.size(); i++) {
			Point p = new Point();
			p.setLocation(selectedObjects.get(i).x, selectedObjects.get(i).y);
			movingObjects.add(new Pair<GameObject, Point>(selectedObjects.get(i), p));
		}
	}

	private void addMovingEvent() {
		ArrayList<Pair<GameObject, Pair<Point, Point>>> moving = new ArrayList<Pair<GameObject, Pair<Point, Point>>>();
		for (int i = 0; i < selectedObjects.size(); i++) {
			Point nwPoint = new Point();
			nwPoint.setLocation(selectedObjects.get(i).x, selectedObjects.get(i).y);
			Pair<Point, Point> points = new Pair<Point, Point>(movingObjects.get(i).two, nwPoint);
			moving.add(new Pair<GameObject, Pair<Point, Point>>(selectedObjects.get(i), points));
		}
		eventHandler.addMoveEvent(moving);
	}

	public void copyToClipboard() {
		if (!selectedObjects.isEmpty()) {
			clipboard.clear();
			for (int i = 0; i < selectedObjects.size(); i++) {
				clipboard.add(selectedObjects.get(i));
			}
		}
	}

	public void pasteClipboard() {
		double deltaX = clipboard.get(0).x - (mouseX / scale - viewportX);
		double deltaY = clipboard.get(0).y - (mouseY / scale - viewportY);
		selectedObjects.clear();
		ArrayList<Pair<GameObject, Integer>> placed = new ArrayList<Pair<GameObject, Integer>>();
		for (int i = 0; i < clipboard.size(); i++) {
			GameObject t = new GameObject();
			t.type = clipboard.get(i).type;
			t.setObjectName(clipboard.get(i).getObjectName());
			t.imageURL = "res/sprites/" + t.getObjectName() + ".png";
			t.width = clipboard.get(i).width;
			t.height = clipboard.get(i).height;
			t.scale = clipboard.get(i).scale;
			t.x = clipboard.get(i).x - deltaX;
			t.y = clipboard.get(i).y - deltaY;
			t.properties = clipboard.get(i).properties;
			selectedObjects.add(t);
			levelMap.get(currentLayer - 1).add(t);
			placed.add(new Pair<GameObject, Integer>(t, currentLayer - 1));
		}
		eventHandler.addPlaceEvent(placed);
	}

	public void deleteSelected() {
		ArrayList<Pair<GameObject, Integer>> deleted = new ArrayList<Pair<GameObject, Integer>>();
		for (int i = 0; i < selectedObjects.size(); i++) {
			for (int j = 0; j < levelMap.size(); j++) {
				if (levelMap.get(j).contains(selectedObjects.get(i))) {
					deleted.add(new Pair<GameObject, Integer>(selectedObjects.get(i), j));
					levelMap.get(j).remove(selectedObjects.get(i));
					break;
				}
			}
		}
		eventHandler.addDeleteEvent(deleted);
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

	private boolean isOverObject() {
		for (int i = 0; i < levelMap.get(currentLayer - 1).size(); i++) {
			GameObject o = levelMap.get(currentLayer - 1).get(i);
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
		/*
		 * mainController.propertyScroll.setVisible(true); while
		 * (mainController.propertyPanel.getChildren().size() > 1) {
		 * mainController.propertyPanel.getChildren().remove(1); } for (int i =
		 * 0; i < selectedObjects.get(0).properties.length; i++) { Property p =
		 * selectedObjects.get(0).properties[i]; HBox hbox = new HBox();
		 * hbox.setSpacing(5); hbox.setPadding(new Insets(0, 0, 0, 5)); Text t =
		 * new Text(p.name + ":"); TextField tf = new TextField();
		 * tf.setText(p.value); hbox.getChildren().add(t);
		 * hbox.getChildren().add(tf);
		 * mainController.propertyPanel.getChildren().add(hbox); }
		 */
	}

	private void hideProperties() {
		/*
		 * mainController.propertyScroll.setVisible(false); if
		 * (selectedObjects.size() > 0) { for (int i = 1; i <
		 * mainController.propertyPanel.getChildren().size(); i++) { HBox hbox =
		 * (HBox) mainController.propertyPanel.getChildren().get(i); TextField
		 * tf = (TextField) hbox.getChildren().get(1);
		 * selectedObjects.get(0).properties[i - 1].value = tf.getText(); } }
		 * while (mainController.propertyPanel.getChildren().size() > 1) {
		 * mainController.propertyPanel.getChildren().remove(1); }
		 */
	}

	private boolean isMouseOutsideGrid() {
		double x = mouseX / scale - viewportX;
		double y = mouseY / scale - viewportY;
		if (x >= 0 && x <= width && y >= 0 && y <= height) {
			return false;
		}
		return true;
	}

	private void placeContinuously() {
		if (currentObject != null && !isOverObject() && !isMouseOutsideGrid() && snapToGrid) {
			placeObject();
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
		ArrayList<Pair<GameObject, Integer>> placed = new ArrayList<Pair<GameObject, Integer>>();
		placed.add(new Pair<GameObject, Integer>(o, currentLayer - 1));
		eventHandler.addPlaceEvent(placed);
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

	public void setHighlightOverlaps(boolean highlightOverlaps) {
		this.highlightOverlaps = highlightOverlaps;
	}

	public void setShowOnlyCurrentLayer(boolean showOnlyCurrentLayer) {
		this.showOnlyCurrentLayer = showOnlyCurrentLayer;
	}

	public void setCurrentLayer(int currentLayer) {
		this.currentLayer = currentLayer;
	}

	public void setCurrentObject(GameObject currentObject) {
		this.currentObject = currentObject;
	}
}
