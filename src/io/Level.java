package io;

import editor.entities.GameObject;
import java.util.ArrayList;

public class Level {
	
	public LevelSettings levelSettings;
	public ArrayList<ArrayList<GameObject>> levelMap;
	
	public Level(LevelSettings levelSettings, ArrayList<ArrayList<GameObject>> levelMap) {
		this.levelSettings = levelSettings;
		this.levelMap = levelMap;
	}
}
