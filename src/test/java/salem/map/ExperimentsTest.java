package salem.map;

import junit.framework.TestCase;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.List;

public class ExperimentsTest extends TestCase {

	private final HashHelper imageIdentifier = new HashHelper();

	public void testLoadImage() throws IOException {
		BufferedImage image = loadImage("/tile_0_0.png");
		assertNotNull(image);
		assertEquals(100, image.getHeight());
		assertEquals(100, image.getWidth());
	}

	private BufferedImage loadImage(String name) throws IOException {
		InputStream inputStream = getClass().getResourceAsStream(name);
		System.out.println(inputStream);
		return ImageIO.read(inputStream);
	}

	public void testHash() throws Exception {
		String name = "/tile_0_0.png";
		URL url = getClass().getResource(name);

		String s = imageIdentifier.md5File(url.getFile());
		System.out.println(s);
	}

    /*public void testHashMultiple() throws Exception {
				String rootPath = "C:\\Users\\abettik\\Salem\\map\\plymouth.seatribe.se";
        File root = new File(rootPath);

        computeHashesRecursively(root, new HashMap<String, File>());
    }*/

	private void computeHashesRecursively(File root, Map<String, File> hashes) throws Exception {

		if (!root.isDirectory()) {
			return;

		}
		TilesRepository repo = new TilesRepository();
		File[] files = root.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				PlaySession session = new PlaySession(file, repo, new DefaultBoundedRangeModel());
//                computeHashesRecursively(file, hashes, new PlaySession());
			} /*else{
								repo.add(file, playSession);
                String path = file.getAbsolutePath();
                String hash = imageIdentifier.md5File(path);

                System.out.println(path +" : "+ hash);
                if (hashes.containsKey(hash)) {
                    File match = hashes.get(hash);
                    System.out.println("match detected : "+ match.getAbsolutePath() +","+path);
                    System.out.println("full compare : "+ compare(match, file));
                } else {
                    hashes.put(hash, file);
                }
            }*/
		}
	}

	/*public void testGrid() throws Exception {
			TilesRepository repo = new TilesRepository();
			PlaySession session = new PlaySession(new File("C:\\Users\\abettik\\Salem\\map\\plymouth.seatribe.se\\2013-03-24 01.19.08"), repo);
			List<File> tiles = session.tiles;
			int left = 0, top = 0, right = 0, bottom = 0;

			TileOffsetHelper offsetHelper=  new TileOffsetHelper();
			for (File tile : tiles) {
					Point point = offsetHelper.parseOffset(tile);
					left = (int) Math.min(left, point.getX());
					top = (int) Math.min(top, point.getY());
					bottom = (int) Math.max(bottom, point.getY());
					right = (int) Math.max(right, point.getX());
			}
			System.out.println("top = "+top+", left="+left+", bottom="+bottom+", right="+right);


	}*/
	/*public void test1() throws Exception {
		TilesRepository repo = new TilesRepository();
		PlaySession s1 = new PlaySession(new File("C:\\Users\\abettik\\Salem\\map\\plymouth.seatribe.se\\merge_2013-04-09 11.38.44"), repo);
		PlaySession s2 = new PlaySession(new File("C:\\Users\\abettik\\Salem\\map\\plymouth.seatribe.se\\Merge 2013-03-15 21.23.03"), repo);

		Collection<PlaySession> matches = repo.findMatches(s1);
		assertEquals(1, matches.size());
		List<SessionMatch> allMatches = computeAllMatches(s1, s2);
		assertEquals(1, allMatches.size());
		evaluateMatch(allMatches.iterator().next());



	}*/
	public List<SessionMatch> computeAllMatches(PlaySession session, PlaySession otherSession) throws Exception {
		PlaySession session1 = session;
		Map<String, File> md5sVsTiles = new HashMap<String, File>();
		HashHelper hashHelper = new HashHelper();
		TileOffsetHelper offsetHelper = new TileOffsetHelper();
		for (File tile : session1.tiles) {
			md5sVsTiles.put(hashHelper.md5File(tile.getAbsolutePath()), tile);
		}
		List<SessionMatch> allMatches = new ArrayList<SessionMatch>();

		for (File tile : otherSession.tiles) {
			File sesion1Match = md5sVsTiles.get(hashHelper.md5File(tile.getAbsolutePath()));
			if (null != sesion1Match) {
				Point matchOffset = offsetHelper.parseOffset(sesion1Match);
				Point offset = offsetHelper.parseOffset(tile);
				SessionMatch sessionMatch;
				sessionMatch = SessionMatch.Builder()
						.session1(session1)
						.session2(otherSession)
						.xtranslate((int) (offset.getX() - matchOffset.getX()))
						.ytranslate((int) (offset.getY() - matchOffset.getY()))
						.matchedTileInSession1(matchOffset)
						.matchedTileInSession2(offset)
						.build();
				allMatches.add(sessionMatch);
			}
		}
		if (allMatches.isEmpty()) {
			throw new IllegalArgumentException("invalid match");
		}
		Collections.sort(allMatches, new Comparator<SessionMatch>() {
			public int compare(SessionMatch o1, SessionMatch o2) {
				int i = o1.matchingTiles - o2.matchingTiles;
				return i == 0 ? o2.intersectionCount - o1.intersectionCount : i;
			}
		});
		return allMatches;
	}

	private void evaluateMatch(SessionMatch sessionMatch) throws Exception {
		HashHelper hashHelper = new HashHelper();

		int xo1 = sessionMatch.session1.origin.x;
		int yo1 = sessionMatch.session1.origin.y;
		int xo2 = sessionMatch.session2.origin.x;
		int yo2 = sessionMatch.session2.origin.y;
		int xtranslate = sessionMatch.xtranslate;
		int ytranslate = sessionMatch.ytranslate;
		File[][] tileMap1 = sessionMatch.session1.tileMap;
		File[][] tileMap2 = sessionMatch.session2.tileMap;
		int intersectionCount = 0;
		int matchingTiles = 0;
		for (int x = 0, maxx = sessionMatch.session1.tileMap[0].length; x < maxx; x++) {
			for (int y = 0, maxy = sessionMatch.session1.tileMap.length; y < maxy; y++) {
				int x1 = -xo1 + x;
				int y1 = -yo1 + y;
				int x2 = x1 + xtranslate;
				int y2 = y1 + ytranslate;
				File tile1 = tileMap1[y][x];
				int j = y2 + yo2;
				int i = x2 + xo2;
				if (i >= 0 && j >= 0 && i < tileMap2[0].length && j < tileMap2.length) {
					File tile2 = tileMap2[y2 + yo2][x2 + xo2];
					if (tile1 != null && tile2 != null) {
						intersectionCount++;
						if (hashHelper.md5File(tile1.getAbsolutePath()).equals(hashHelper.md5File(tile2.getAbsolutePath()))) {
							matchingTiles++;
						}
					}
					//				} else {
					//					System.out.println("session1 tile is out of session2");
				}
			}

			sessionMatch.intersectionCount = intersectionCount;
			sessionMatch.matchingTiles = matchingTiles;
			if (matchingTiles > 0) {
				System.out.println("match [intersect : " + intersectionCount + ",matching " + matchingTiles);
			}
		}
	}
}
