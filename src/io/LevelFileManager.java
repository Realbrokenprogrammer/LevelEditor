package io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import editor.entities.GameObject;

public class LevelFileManager {
	
	private Map<Integer, String> types;
	
	public LevelFileManager() {
		types = new HashMap<Integer, String>();
		File file = new File("types.cfg");
		try {
			Scanner scan = new Scanner(file);
			while (scan.hasNext()) {
				String str = scan.nextLine();
				types.put(Integer.parseInt(str.split(" ")[0]), str.split(" ")[1]);
			}
			scan.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public Level loadFile(File file) {
		try {
			Path path = file.toPath();
			byte[] bytes =  Files.readAllBytes(path);
			
			// Read width and hight of the level in tiles
			int width = bytesToInt(new byte[] {bytes[0], bytes[1], bytes[2], bytes[3]});
			int height = bytesToInt(new byte[] {bytes[4], bytes[5], bytes[6], bytes[7]});
			LevelSettings ls = new LevelSettings(width, height, 32); // Temporary, also save tile size
			
			// Init level map
			ArrayList<ArrayList<GameObject>> levelMap = new ArrayList<ArrayList<GameObject>>();
			for (int i = 0; i < 8; i++) {
				levelMap.add(new ArrayList<GameObject>());
			}
			
			// Read all objects
			for (int i = 8; i < bytes.length; i+=22) {
				GameObject o = new GameObject();
				int layer = bytes[i];
				o.x = bytesToFloat(new byte[] {bytes[i + 1], bytes[i + 2], bytes[i + 3], bytes[i + 4]});
				o.y = bytesToFloat(new byte[] {bytes[i + 5], bytes[i + 6], bytes[i + 7], bytes[i + 8]});
				o.width = bytesToFloat(new byte[] {bytes[i + 9], bytes[i + 10], bytes[i + 11], bytes[i + 12]});
				o.height = bytesToFloat(new byte[] {bytes[i + 13], bytes[i + 14], bytes[i + 15], bytes[i + 16]});
				o.scale = bytesToFloat(new byte[] {bytes[i + 17], bytes[i + 18], bytes[i + 19], bytes[i + 20]});
				o.setObjectName(types.get((int) bytes[i + 21]));
				o.imageURL = "res/sprites/" + o.getObjectName() + ".png";
				levelMap.get(layer).add(o);
			}
			
			Level level = new Level(ls, levelMap);
			return level;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void writeFile(Level level, String filePath) {
		ArrayList<ArrayList<GameObject>> levelMap = level.levelMap;
		try {
			FileOutputStream fos = new FileOutputStream(filePath);
			BufferedOutputStream out = new BufferedOutputStream(fos);
			
			byte[] width = intToBytes(level.levelSettings.width);
			byte[] height = intToBytes(level.levelSettings.height);
			
			out.write(width);
			out.write(height);
			
			for (int i = 0; i < levelMap.size(); i++) {
				for (int j = 0; j < levelMap.get(i).size(); j++) {
					GameObject o = levelMap.get(i).get(j);
					out.write((byte) i); // Layer
					out.write(floatToBytes((float) o.x));
					out.write(floatToBytes((float) o.y));
					out.write(floatToBytes((float) o.width));
					out.write(floatToBytes((float) o.height));
					out.write(floatToBytes((float) o.scale));
					out.write(getKey(o.getObjectName())); // Type
				}
			}
			
			out.flush();
			fos.close();			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int getKey(String value) {
		for (Entry<Integer, String> entry : types.entrySet()) {
	        if (entry.getValue().equals(value)) {
	            return entry.getKey();
	        }
	    }
	    return -1;
	}
	
	private byte[] intToBytes(int value) {
	     return ByteBuffer.allocate(4).putInt(value).array();
	}

	private int bytesToInt(byte[] bytes) {
	     return ByteBuffer.wrap(bytes).getInt();
	}
	
	private float bytesToFloat(byte[] bytes) {
	    int intBits = 
	      bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
	    return Float.intBitsToFloat(intBits);  
	}
	
	private byte[] floatToBytes(float val) {
	    int intBits =  Float.floatToIntBits(val);
	    return new byte[] {
	      (byte) (intBits >> 24), (byte) (intBits >> 16), (byte) (intBits >> 8), (byte) (intBits) };
	}
}
