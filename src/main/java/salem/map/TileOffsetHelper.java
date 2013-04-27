package salem.map;

import java.awt.*;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TileOffsetHelper {

	public Point parseOffset(File match) {
		String patternString = "tile_(-?\\d+)_(-?\\d+).png";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(match.getName());
		if (!matcher.matches()) {
			throw new IllegalArgumentException("unexpected file name " + match);
		}
		int x = Integer.parseInt(matcher.group(1));
		int y = Integer.parseInt(matcher.group(2));
		return new Point(x, y);

	}

}
