package fr.gouv.culture.francetransfert.domain.utils;

import fr.gouv.culture.francetransfert.application.error.ErrorEnum;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.UUID;

public class DownloadUtils {
	
	private DownloadUtils() {
		// private Constructor
	}

    public static String base64Decoder(String string) throws UnsupportedEncodingException {
	    try {
            byte[] asBytes = Base64.getUrlDecoder().decode(string);
            return new String(asBytes, "utf-8");
        } catch (DownloadException e) {
            throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), UUID.randomUUID().toString());
        }
    }
}
