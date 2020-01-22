package fr.gouv.culture.francetransfert.domain.utils;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class DownloadUtils {
	
	private DownloadUtils() {
		// private Constructor
	}

    public static String base64Decoder(String string) throws UnsupportedEncodingException {
        byte[] asBytes = Base64.getUrlDecoder().decode(string);
        return new String(asBytes, "utf-8");
    }
}
