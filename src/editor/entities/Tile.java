package editor.entities;

public class Tile extends GameObject {
	
	TileType type = null;
	
	public Tile(double x, double y, TileType type) {
		this.x = x;
		this.y = y;
		this.type = type;
	}
}

enum TileType {
	grass,
	sand,
	dirt,
	water
}
