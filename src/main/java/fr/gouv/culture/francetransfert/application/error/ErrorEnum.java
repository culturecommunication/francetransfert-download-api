package fr.gouv.culture.francetransfert.application.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorEnum {
    TECHNICAL_ERROR("TECHNICAL_ERROR"),
    DOWNLOAD_LIMIT("DOWNLOAD_LIMIT"),
    WRONG_PASSWORD("WRONG_PASSWORD"),
    WRONG_ENCLOSURE("WRONG_ENCLOSURE");

    private String value;
}
