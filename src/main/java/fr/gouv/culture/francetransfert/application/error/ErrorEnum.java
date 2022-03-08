package fr.gouv.culture.francetransfert.application.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorEnum {
	TECHNICAL_ERROR("TECHNICAL_ERROR"), DOWNLOAD_LIMIT("DOWNLOAD_LIMIT"), DELETED_ENCLOSURE("DELETED_ENCLOSURE"),
	WRONG_PASSWORD("WRONG_PASSWORD"), USER_DELETED("USER_DELETED"), MAX_TRY("MAX_TRY"),
	WRONG_ENCLOSURE("WRONG_ENCLOSURE");

	private String value;
}
