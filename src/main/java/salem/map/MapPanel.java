package salem.map;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class MapPanel extends JPanel {
	public static final int IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;
	public static final int TILE_SIZE = 100;
	public static final double ZOOM_FACTOR = 0.1;
	private final TileOffsetHelper offsetHelper = new TileOffsetHelper();
	private final HashHelper hashHelper = new HashHelper();
	private final boolean debug = false;
	private BufferedImage image;
	private PlaySession session;
	private double zoomFactor = 1;
	private AffineTransform transform = new AffineTransform();

	public MapPanel() {
		addMouseWheelListener(new MouseInputAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				super.mouseWheelMoved(e);    //To change body of overridden methods use File | Settings | File Templates.

				if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
					JViewport viewport = (JViewport) getParent();
					Rectangle viewRect = viewport.getViewRect();// visible portion
					debug("mouseWheelEvent : type =" + e.getScrollType() + ",units = " + e.getUnitsToScroll() + ",scrollAmount=" + e.getScrollAmount() + ",wheelRotation=" + e.getWheelRotation() + ",x=" + e.getX() + ",y=" + e.getY());
//					debug("old Vp = " + viewPosition);
					debug("currentViewRect : " + viewRect);
					debug("oldSize" + getPreferredSize());
					double oldzoom = zoomFactor;
					zoomFactor = zoomFactor * (1 - e.getUnitsToScroll() * ZOOM_FACTOR);
					debug("zoom : " + oldzoom + "=>" + zoomFactor);
					transform.scale(zoomFactor / oldzoom, zoomFactor / oldzoom);
					Dimension preferredSize = new Dimension(getPreferredSize());
					preferredSize.setSize((int) (image.getWidth() * zoomFactor), ((int) (image.getHeight() * zoomFactor)));
					setPreferredSize(preferredSize);
//					Rectangle newViewRect = new Rectangle();
//					Point newPoint = new Point(((int) (p.x * zoomFactor / oldzoom)), ((int) (p.y * zoomFactor / oldzoom)));
//					newViewRect.width = (int) (viewRect.width * oldzoom / zoomFactor);
//					newViewRect.height = (int) (viewRect.height * oldzoom / zoomFactor);
//					newViewRect.x = (int) (newPoint.x - (p.x - viewRect.x) * (oldzoom / zoomFactor));
//					newViewRect.y = (int) (newPoint.y - (p.y - viewRect.y) * (oldzoom / zoomFactor));
					viewport.setViewPosition(new Point(
							Math.max(0, ((int) (e.getX() * (zoomFactor / oldzoom - 1) + viewRect.x))),
							Math.max(0, ((int) (e.getY() * (zoomFactor / oldzoom - 1) + viewRect.y)))
					));
					debug("new size= " + getPreferredSize());
					debug("new Vp = " + viewport.getViewPosition());
					debug("new viewRect " + viewport.getViewRect());

					repaint();
//					getParent().validate();
//					getParent().doLayout();
//					System.out.println(getParent());
//					JScrollPane scrollPane = (JScrollPane) getParent().getParent();
//					scrollPane.getViewport().setViewPosition();
/*
					transform.translate(e.getPoint().getX(), e.getPoint().getY());
					transform.scale(zoomFactor, zoomFactor);

*/
				}
			}

		});
	}

	private void debug(String s) {
		if (debug) {
			System.out.println(s);
		}
	}

	public void setSession(PlaySession session, Point highlighedTile) throws IOException {
		this.session = session;
		zoomFactor = 1;
		transform = new AffineTransform();

		removeAll();

		BufferedImage myImage = session != null ? createMap(session, highlighedTile) : new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
		setImage(myImage);

		repaint();
		if (null != highlighedTile && null != session) {
			Point topLeft = getBounds(session).getLocation();
			Point centerPoint = new Point((highlighedTile.x - topLeft.x) * TILE_SIZE + TILE_SIZE / 2, (highlighedTile.y - topLeft.y) * TILE_SIZE + TILE_SIZE / 2);
			Point transformedPoint = new Point();
			transform.transform(centerPoint, transformedPoint);
			centerViewPortOn(transformedPoint);
		}

	}

	private void centerViewPortOn(Point center) {

		JViewport viewport = (JViewport) getParent();
		Dimension viewportSize = viewport.getSize();
		viewport.setViewPosition(new Point(Math.max((int) (center.getX() - viewportSize.width / 2), 0), Math.max((int) (center.getY() - viewportSize.height / 2), 0)));
	}

	private void setImage(BufferedImage image1) {
		this.image = image1;
		setPreferredSize(new Dimension(image1.getWidth(), image1.getHeight()));
		setSize(new Dimension(image1.getWidth(), image1.getHeight()));
	}

	private BufferedImage createMap(PlaySession session, Point highlighedTile) throws IOException {
		Rectangle bounds = getBounds(session);
		BufferedImage bufferedImage = new BufferedImage((int) bounds.getWidth() * TILE_SIZE, (int) (bounds.getHeight() * TILE_SIZE), IMAGE_TYPE);

//        System.out.println("top " + top + ", left=" + left + ",height=" + height + ", width=" + width + ", bottom=" + bottom + ", right=" + right);
		for (File tile : session.tiles) {
			Point point = offsetHelper.parseOffset(tile);
			int x = TILE_SIZE * (int) (point.getX() - bounds.getX());
			int y = TILE_SIZE * (int) (point.getY() - bounds.getY());
			Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
			graphics.drawImage(ImageIO.read(tile), x, y, null);

			if (point.equals(highlighedTile)) {
				highlightMatchingTile(graphics, x, y);
			}

			drawSessionBorder(graphics, session, point, x, y);
		}
		return bufferedImage;
	}

	private void drawSessionBorder(Graphics2D graphics, PlaySession session, Point tilePosition, int x, int y) {
		Color oldColor = graphics.getColor();
		graphics.setColor(Color.red);
		int offset = 2;
		if (session.isWestBoundary(tilePosition)) {
			graphics.fillRect(x + 1, y, offset, TILE_SIZE);
		}
		if (session.isEastBoundary(tilePosition)) {
			graphics.fillRect(x + TILE_SIZE - offset, y, offset, TILE_SIZE);
		}
		if (session.isNorthBoundary(tilePosition)) {
			graphics.fillRect(x, y + 1, TILE_SIZE, offset);
		}
		if (session.isSouthBoundary(tilePosition)) {
			graphics.fillRect(x, y + TILE_SIZE - offset, TILE_SIZE, offset);
		}
		graphics.setColor(oldColor);
	}

	private void highlightMatchingTile(Graphics2D graphics, int x, int y) {
		Color oldColor = graphics.getColor();
		graphics.setColor(Color.white);
		for (int dx = 0; dx < TILE_SIZE; dx += 20) {
			graphics.drawLine(x + dx, y, x + TILE_SIZE, y + TILE_SIZE - dx);
			graphics.drawLine(x, y + dx, x + TILE_SIZE - dx, y + TILE_SIZE);

			graphics.drawLine(x, y + dx, x + dx, y);
			graphics.drawLine(x + dx, y + TILE_SIZE, x + TILE_SIZE, y + dx);
		}
		graphics.drawLine(x + 1, y + 1, x + TILE_SIZE - 1, y + TILE_SIZE - 1);
		graphics.drawLine(x + TILE_SIZE - 1, y + 1, x + 1, y + TILE_SIZE - 1);
		graphics.setColor(oldColor);
	}

	private Rectangle getBounds(PlaySession session) {

		Rectangle bounds = new Rectangle();
		bounds.x = 0 - session.origin.x;
		bounds.y = 0 - session.origin.y;
		bounds.width = session.tileMap[0].length;
		bounds.height = session.tileMap.length;

/*
		List<File> tiles = session.tiles;
		int left = 0, top = 0, right = 0, bottom = 0;
		for (File tile : tiles) {
			Point point = offsetHelper.parseOffset(tile);
			left = (int) Math.min(left, point.getX());
			top = (int) Math.min(top, point.getY());
			bottom = (int) Math.max(bottom, point.getY());
			right = (int) Math.max(right, point.getX());
		}
		assert bounds.x == left;
		assert bounds.y == top;
		assert bounds.width == right - left + 1;
		assert bounds.height == bottom - top + 1;
*/
//		bounds.setBounds(left, top, right - left + 1, bottom - top + 1);
		return bounds;
	}

	static class MergeSpec {
		final SessionMatch match;
		final Point session1Translate;
		final Point session2Translate;

		MergeSpec(SessionMatch match, Point session1Translate, Point session2Translate) {
			this.match = match;
			this.session1Translate = session1Translate;
			this.session2Translate = session2Translate;
		}
	}

	public MergeSpec merge(PlaySession otherSession) throws Exception {
		SessionMatch sessionMatch = computeMatch(otherSession);

		return mergeWithMatch(otherSession, sessionMatch);
	}

	public MergeSpec mergeWithMatch(PlaySession otherSession, SessionMatch sessionMatch) throws IOException {
		PlaySession session1 = session;
		image = createMap(session, sessionMatch.matchedTileInSession2);
		BufferedImage otherSessionmap = createMap(otherSession, sessionMatch.matchedTileInSession1);

		Rectangle myBounds = getBounds(session1);
		Rectangle otherBounds = getBounds(otherSession);

		Rectangle mergedBounds = getMergedBounds(sessionMatch);
/*
				System.out.println("match : " + sessionMatch);
        System.out.println("myBounds : " + myBounds);
        System.out.println("otherBounds : " + otherBounds);
        System.out.println("mergedBounds : " + mergedBounds);
*/

		Point session1Translate = new Point((myBounds.x + sessionMatch.xtranslate) - mergedBounds.x, (myBounds.y + sessionMatch.ytranslate) - mergedBounds.y);

//        System.out.println("session1Translate " + session1Translate);
		Point otherTranslate = new Point(otherBounds.x - mergedBounds.x, otherBounds.y - mergedBounds.y);
//        System.out.println("otherTranslate " + otherTranslate);

		BufferedImage mergedMap = new BufferedImage(
				mergedBounds.width * TILE_SIZE,
				mergedBounds.height * TILE_SIZE,
				IMAGE_TYPE);
		Graphics2D mergedMapGraphics = mergedMap.createGraphics();
		mergedMapGraphics.drawImage(image, session1Translate.x * TILE_SIZE, session1Translate.y * TILE_SIZE, null);
		mergedMapGraphics.drawImage(otherSessionmap, otherTranslate.x * TILE_SIZE, otherTranslate.y * TILE_SIZE, null);
		setImage(mergedMap);

		Point highlighedPoint = new Point(sessionMatch.matchedTileInSession2.x + session1Translate.x, sessionMatch.matchedTileInSession2.y + session1Translate.y);

		Point centerPoint = new Point((highlighedPoint.x - mergedBounds.x) * TILE_SIZE + TILE_SIZE / 2, (highlighedPoint.y - mergedBounds.y) * TILE_SIZE + TILE_SIZE / 2);
		Point transformedPoint = new Point();
		transform.transform(centerPoint, transformedPoint);
		centerViewPortOn(transformedPoint);

		return new MergeSpec(sessionMatch, session1Translate, otherTranslate);
	}

	private Rectangle getMergedBounds(SessionMatch sessionMatch) {
		Rectangle myBounds = getBounds(sessionMatch.session1);
		Rectangle otherBounds = getBounds(sessionMatch.session2);
		int left = Math.min(myBounds.x + sessionMatch.xtranslate, otherBounds.x);
		int top = Math.min(myBounds.y + sessionMatch.ytranslate, otherBounds.y);
		int right = Math.max(myBounds.x + myBounds.width + sessionMatch.xtranslate, otherBounds.x + otherBounds.width);
		int bottom = Math.max(myBounds.y + myBounds.height + sessionMatch.ytranslate, otherBounds.y + otherBounds.height);
		return new Rectangle(left, top, right - left, bottom - top);
	}

	private SessionMatch computeMatch(PlaySession otherSession) throws Exception {
		PlaySession session1 = session;
		Map<String, File> md5sVsTiles = new HashMap<String, File>();
		for (File tile : session1.tiles) {
			md5sVsTiles.put(hashHelper.md5File(tile.getAbsolutePath()), tile);
		}
		SessionMatch sessionMatch = null;
		for (File tile : otherSession.tiles) {
			File sesion1Match = md5sVsTiles.get(hashHelper.md5File(tile.getAbsolutePath()));
			if (null != sesion1Match) {
				// todo check the maps match
				Point matchOffset = offsetHelper.parseOffset(sesion1Match);
				Point offset = offsetHelper.parseOffset(tile);
				sessionMatch = SessionMatch.Builder()
						.session1(session1)
						.session2(otherSession)
						.xtranslate((int) (offset.getX() - matchOffset.getX()))
						.ytranslate((int) (offset.getY() - matchOffset.getY()))
						.matchedTileInSession1(matchOffset)
						.matchedTileInSession2(offset)
						.build();
				break;
			}
		}
		if (sessionMatch == null) {
			throw new IllegalArgumentException("invalid match");
		}
		return sessionMatch;
	}

	public List<SessionMatch> computeAllMatches(PlaySession otherSession) throws Exception {
		PlaySession session1 = session;
		Map<String, File> md5sVsTiles = new HashMap<String, File>();
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
				evaluateMath(sessionMatch);
				if (sessionMatch.matchingTiles > 0) {
					allMatches.add(sessionMatch);
				} else {
					System.out.println("wrong match " + sesion1Match.getAbsolutePath() + " , " + tile.getAbsolutePath());
				}
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

	private void evaluateMath(SessionMatch sessionMatch) throws Exception {

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
//				System.out.println("match [intersect : " + intersectionCount + ",matching " + matchingTiles);
			}
		}
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (image != null) {

			Graphics2D g2D = (Graphics2D) g;
			AffineTransform currentTransform = g2D.getTransform();
			currentTransform.concatenate(transform);
			g2D.setTransform(currentTransform);
			g.drawImage(image, 0, 0, null);
			g.setColor(Color.white);
			for (int x = 0, max = image.getWidth(); x < max; x += TILE_SIZE) {
				g.drawLine(x, 2, x, image.getHeight() - 4);
			}
			for (int y = 0, max = image.getHeight(); y < max; y += TILE_SIZE) {
				g.drawLine(2, y, image.getWidth() - 4, y);
			}
		}
	}
}
