package fr.gouv.culture.francetransfert.domain.exceptions;

import lombok.Data;

@Data
public class DownloadException extends RuntimeException {
	private String type;
	private String id;
	private Throwable ex;

	public DownloadException(String type, String id, Throwable ex) {
		super();
		this.type = type;
		this.id = id;
		this.ex = ex;
	}

	public DownloadException(String type, String id) {
		super();
		this.type = type;
		this.id = id;
	}

}
