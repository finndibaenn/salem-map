package salem.map;

import java.awt.*;

/**
 * session1.tiles.coords + translate = session2.matchingtile.coords
 */
public class SessionMatch {
	public int intersectionCount = 0;
	public int matchingTiles = 0;

	private SessionMatch(PlaySession session1, PlaySession session2, int xtranslate, int ytranslate, Point matchedTileInSession1, Point matchedTileInSession2) {
		this.session1 = session1;
		this.session2 = session2;
		this.xtranslate = xtranslate;
		this.ytranslate = ytranslate;

		this.matchedTileInSession1 = matchedTileInSession1;
		this.matchedTileInSession2 = matchedTileInSession2;
	}

	public static Builder Builder() {
		return new Builder();
	}

	public static class Builder {

		private PlaySession session1;
		private PlaySession session2;
		private int xtranslate;
		private int ytranslate;
		private Point matchedTileInSession1;
		private Point matchedTileInSession2;

		public Builder session1(PlaySession session) {
			session1 = session;
			return this;
		}

		public Builder session2(PlaySession session) {
			this.session2 = session;
			return this;
		}

		public Builder xtranslate(int x) {
			this.xtranslate = x;
			return this;
		}

		public Builder ytranslate(int y) {
			this.ytranslate = y;
			return this;
		}

		public SessionMatch build() {
			return new SessionMatch(session1, session2, xtranslate, ytranslate, matchedTileInSession1, matchedTileInSession2);
		}

		public Builder matchedTileInSession1(Point matchOffset) {
			matchedTileInSession1 = matchOffset;
			return this;
		}

		public Builder matchedTileInSession2(Point offset) {
			matchedTileInSession2 = offset;
			return this;
		}
	}

	public final PlaySession session1;
	public final PlaySession session2;

	public final int xtranslate;
	public final int ytranslate;
	public final Point matchedTileInSession1;
	public final Point matchedTileInSession2;

	@Override
	public String toString() {
		return "SessionMatch{" +
				"session1=" + session1 +
				", session2=" + session2 +
				", xtranslate=" + xtranslate +
				", ytranslate=" + ytranslate +
				'}';
	}
}
