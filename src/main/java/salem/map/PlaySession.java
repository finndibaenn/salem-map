package salem.map;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaySession {
	static class Comparator implements java.util.Comparator<PlaySession> {

		public int compare(PlaySession o1, PlaySession o2) {
			return o1.rootDir.getName().compareTo(o2.rootDir.getName());
		}
	}

	public static final java.util.Comparator<PlaySession> COMPARATOR = new Comparator();
	List<File> tiles = new ArrayList<File>();
	File rootDir;
	/**
	 * tileMap[y][x]
	 */
	File[][] tileMap;
	Point origin;

	public PlaySession(File file, TilesRepository repo) throws Exception {
		initialize(file, repo);
	}

	public File getRootDir() {
		return rootDir;
	}

	private void initialize(File rootDir, TilesRepository repository) throws Exception {
		if (!rootDir.isDirectory()) {
			return;
		}
		this.rootDir = rootDir;
		File[] files = rootDir.listFiles();
		if (files == null) {
			return;
		}
		int minx = Integer.MAX_VALUE;
		int miny = Integer.MAX_VALUE;
		int maxx = Integer.MIN_VALUE;
		int maxy = Integer.MIN_VALUE;
		TileOffsetHelper tileOffsetHelper = new TileOffsetHelper();
		for (File file : files) {
			if (file.isDirectory()) {
				throw new IllegalStateException("unexpectedDir");
			} else {
				repository.add(file, this);
				tiles.add(file);
				Point position = tileOffsetHelper.parseOffset(file);
				minx = Math.min(position.x, minx);
				miny = Math.min(position.y, miny);
				maxx = Math.max(position.x, maxx);
				maxy = Math.max(position.y, maxy);
			}
		}
		origin = new Point(minx * -1, miny * -1);
		tileMap = new File[maxy - miny + 1][maxx - minx + 1];
		for (File file : files) {
			Point position = tileOffsetHelper.parseOffset(file);
			tileMap[position.y + origin.y][position.x + origin.x] = file;
		}

	}

	public List<File> tiles() {
		return tiles;
	}

	@Override
	public String toString() {
		return "PlaySession{" +
				"rootDir=" + rootDir +
				'}';
	}

	public boolean isNorthBoundary(Point position) {
		if (position.y == -1 * origin.y) {
			return true;
		}
		return tileMap[position.y + origin.y - 1][position.x + origin.x] == null;

	}

	public boolean isWestBoundary(Point position) {
		if (position.x == -1 * origin.x) {
			return true;
		}
		return tileMap[position.y + origin.y][position.x + origin.x - 1] == null;

	}

	public boolean isSouthBoundary(Point position) {
		if (position.y == tileMap.length - origin.y - 1) {
			return true;
		}
		return tileMap[position.y + origin.y + 1][position.x + origin.x] == null;

	}

	public boolean isEastBoundary(Point position) {
		if (position.x == tileMap[0].length - origin.x - 1) {
			return true;
		}
		return tileMap[position.y + origin.y][position.x + origin.x + 1] == null;

	}
}
