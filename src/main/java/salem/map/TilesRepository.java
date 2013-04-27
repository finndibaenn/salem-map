package salem.map;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class TilesRepository {
	private HashHelper helper = new HashHelper();

	Map<String, List<File>> md5s = new HashMap<String, List<File>>();
	Map<File, PlaySession> filesVsSessions = new HashMap<File, PlaySession>();


	public void add(File tile, PlaySession session) throws Exception {
		filesVsSessions.put(tile, session);
		String hash = helper.md5File(tile.getAbsolutePath());
		List<File> matches;
		if (hasMatch(tile)) {
			matches = md5s.get(hash);
		} else {
			matches = new ArrayList<File>();
			md5s.put(hash, matches);
		}
		matches.add(tile);

	}

	private boolean hasMatch(File tile) throws Exception {
		String hash = helper.md5File(tile.getAbsolutePath());
		List<File> matches = md5s.get(hash);
		return !(matches == null || matches.isEmpty()) && compare(tile, matches.get(0));
	}

	private boolean compare(File file1, File file2) throws IOException {
		byte[] buf1 = new byte[1024], buf2 = new byte[1024];
		FileInputStream fis1 = new FileInputStream(file1), fis2 = new FileInputStream(file2);
		int read1, read2;
		try {
			do {
				read1 = fis1.read(buf1);
				read2 = fis2.read(buf2);
				if (read1 < 0 && read2 < 0) {
					return true;
				} else if (read1 < 0 || read2 < 0) {
					// different
					System.out.println("read1 < 0 || read2 < 0");
					return false;
				} else if (read1 == read2) {
					for (int i = 0; i < read1; i++) {
						if (buf1[i] != buf2[i]) {
							System.out.println("byte diff at " + i);
							return false;
						}
					}
				} else {
					// ?
					System.out.println("?");
					return false;
				}
			} while (read1 >= 0 && read2 >= 0);
			return true;
		} finally {
			fis1.close();
			fis2.close();
		}
	}

	public Collection<PlaySession> findMatches(PlaySession session) throws Exception {
		HashSet<PlaySession> matchedSessions = new HashSet<PlaySession>();
		for (File tile : session.tiles) {
			String hash = helper.md5File(tile.getAbsolutePath());
			List<File> matches = md5s.get(hash);
			for (File match : matches) {
				PlaySession other = this.filesVsSessions.get(match);

				if (other != session) {
					matchedSessions.add(other);
				}
			}
		}
		return matchedSessions;
	}

	public void remove(PlaySession session) throws Exception {
		for (File file : session.tiles()) {
			filesVsSessions.remove(file);
			String hash = helper.md5File(file.getAbsolutePath());
			List<File> files = md5s.get(hash);
			files.remove(file);
			if (files.isEmpty()) {
				md5s.remove(hash);
			}
		}
	}

	public Set<PlaySession> allSessions() {
		HashSet<PlaySession> playSessions = new HashSet<PlaySession>();
		for (PlaySession session : filesVsSessions.values()) {
			playSessions.add(session);
		}
		return playSessions;
	}
}
