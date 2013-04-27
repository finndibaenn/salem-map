package salem.map;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashHelper {
	private byte[] md5bytes(String path) throws IOException, NoSuchAlgorithmException {

		InputStream fis = new FileInputStream(path);

		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance("MD5");
		int numRead;

		do {
			numRead = fis.read(buffer);
			if (numRead > 0) {
				complete.update(buffer, 0, numRead);
			}
		} while (numRead != -1);

		fis.close();
		return complete.digest();

	}

	public String md5File(String filename) throws Exception {
		byte[] b = md5bytes(filename);
		String result = "";

		for (byte aB : b) {
			result += Integer.toString((aB & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}
}
