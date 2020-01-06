package fr.gouv.culture.francetransfert.domain.exceptions;

public class DownloadException extends RuntimeException {

    public DownloadException(String extension) {
        super(extension);
    }

}