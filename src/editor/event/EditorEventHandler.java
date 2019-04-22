package editor.event;

import java.awt.Point;
import java.util.ArrayList;

import editor.entities.GameObject;
import editor.entities.Pair;

/**
 * This class is used for tracking and handling events. A class can use this
 * to record events and can then use the undo and redo methods to undo or redo
 * the events.
 * 
 * @author Jesper Bergstrom
 * @name EditorEventHandler.java
 * @version 0.00.00
 */
public class EditorEventHandler {

	private ArrayList<EditorEvent> events;
	private int index = -1;
	
	public EditorEventHandler() {
		events = new ArrayList<EditorEvent>();
	}
	
	/**
	 * Undoes the most recent event recorded.
	 * 
	 * @param levelMap
	 */
	public void undo(ArrayList<ArrayList<GameObject>> levelMap) {
		if (index > -1) {
			if (events.get(index).type == EventType.PLACE) {
				undoPlace(levelMap);
			} else if (events.get(index).type == EventType.DELETE) {
				undoDelete(levelMap);
			} else if (events.get(index).type == EventType.MOVE) {
				undoMove(levelMap);
			}
			index--;
		}
	}
	
	/**
	 * Redoes the most recent event that has been undone.
	 * 
	 * @param levelMap
	 */
	public void redo(ArrayList<ArrayList<GameObject>> levelMap) {
		if (index < events.size() - 1) {
			index++;
			if (events.get(index).type == EventType.PLACE) {
				redoPlace(levelMap);
			} else if (events.get(index).type == EventType.DELETE) {
				redoDelete(levelMap);
			} else if (events.get(index).type == EventType.MOVE) {
				redoMove(levelMap);
			}
		}
	}
	
	/**
	 * Adds a move type event to the event list. All events that happened after the current
	 * event index will be deleted when a new event is added.
	 * 
	 * @param movedObjects
	 */
	public void addMoveEvent(ArrayList<Pair<GameObject, Pair<Point, Point>>> movedObjects) {
		EditorEvent e = new EditorEvent(EventType.MOVE, null, null, movedObjects);
		for (int i = events.size() - 1; i > index; i--) {
			events.remove(i);
		}
		events.add(e);
		index = events.size() - 1;
	}
	
	/**
	 * Adds a place type event to the event list. All events that happened after the current
	 * event index will be deleted when a new event is added.
	 * 
	 * @param placeObjects
	 */
	public void addPlaceEvent(ArrayList<Pair<GameObject, Integer>> placeObjects) {
		EditorEvent e = new EditorEvent(EventType.PLACE, placeObjects, null, null);
		for (int i = events.size() - 1; i > index; i--) {
			events.remove(i);
		}
		events.add(e);
		index = events.size() - 1;
	}
	
	/**
	 * Adds a delete type event to the event list. All events that happened after the current
	 * event index will be deleted when a new event is added.
	 * 
	 * @param deleteObjects
	 */
	public void addDeleteEvent(ArrayList<Pair<GameObject, Integer>> deleteObjects) {
		EditorEvent e = new EditorEvent(EventType.DELETE, null, deleteObjects, null);
		for (int i = events.size() - 1; i > index; i--) {
			events.remove(i);
		}
		events.add(e);
		index = events.size() - 1;
	}
	
	private void undoMove(ArrayList<ArrayList<GameObject>> levelMap) {
		EditorEvent e = events.get(index);
		for (int i = 0; i < e.movedObjects.size(); i++) {
			GameObject o = e.movedObjects.get(i).one;
			Point p = e.movedObjects.get(i).two.one;
			o.x = p.x;
			o.y = p.y;
		}
	}
	
	private void undoDelete(ArrayList<ArrayList<GameObject>> levelMap) {
		EditorEvent e = events.get(index);
		for (int i = 0; i < e.deletedObjects.size(); i++) {
			GameObject o = e.deletedObjects.get(i).one;
			int layerIndex = e.deletedObjects.get(i).two;
			levelMap.get(layerIndex).add(o);
		}
	}
	
	private void undoPlace(ArrayList<ArrayList<GameObject>> levelMap) {
		EditorEvent e = events.get(index);
		for (int i = 0; i < e.placedObjects.size(); i++) {
			levelMap.get(e.placedObjects.get(i).two).remove(e.placedObjects.get(i).one);
		}
	}
	
	private void redoPlace(ArrayList<ArrayList<GameObject>> levelMap) {
		EditorEvent e = events.get(index);
		for (int i = 0; i < e.placedObjects.size(); i++) {
			levelMap.get(e.placedObjects.get(i).two).add(e.placedObjects.get(i).one);
		}
	}
	
	private void redoDelete(ArrayList<ArrayList<GameObject>> levelMap) {
		EditorEvent e = events.get(index);
		for (int i = 0; i < e.deletedObjects.size(); i++) {
			GameObject o = e.deletedObjects.get(i).one;
			int layerIndex = e.deletedObjects.get(i).two;
			levelMap.get(layerIndex).remove(o);
		}
	}
	
	private void redoMove(ArrayList<ArrayList<GameObject>> levelMap) {
		EditorEvent e = events.get(index);
		for (int i = 0; i < e.movedObjects.size(); i++) {
			GameObject o = e.movedObjects.get(i).one;
			Point p = e.movedObjects.get(i).two.two;
			o.x = p.x;
			o.y = p.y;
		}
	}
}
