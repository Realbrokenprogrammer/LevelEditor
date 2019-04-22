package editor.event;

import java.awt.Point;
import java.util.ArrayList;

import editor.entities.GameObject;
import editor.entities.Pair;

/**
 * This Class represents an event that can happen in the level editor.
 * At the moment, 3 types of events can take place:
 * - Delete: The user provides the objects that were deleted and the index 
 *   of the layer the objects were placed in.
 * - Move: The user provides the objects that were moved, the starting point
 *   and the ending point of each object.
 * - Place: The user provides the objects that were placed and the index
 *   of the layer the objects were placed in.
 * 
 * @author Jesper Bergstrom
 * @name EditorEvent.java
 * @version 0.00.00
 */
public class EditorEvent {
	
	public EventType type;
	public ArrayList<Pair<GameObject, Integer>> deletedObjects;
	public ArrayList<Pair<GameObject, Pair<Point, Point>>> movedObjects;
	public ArrayList<Pair<GameObject, Integer>> placedObjects;
	
	public EditorEvent(EventType type, ArrayList<Pair<GameObject, Integer>> placedObjects, ArrayList<Pair<GameObject, Integer>> deletedObjects, ArrayList<Pair<GameObject, Pair<Point, Point>>> movedObjects) {
		this.type = type;
		if (type == EventType.DELETE) {
			this.deletedObjects = deletedObjects;
		} else if (type == EventType.PLACE) {
			this.placedObjects = placedObjects;
		} else if (type == EventType.MOVE) {
			this.movedObjects = movedObjects;
		}
	}
}
