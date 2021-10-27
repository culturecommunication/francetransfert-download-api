package fr.gouv.culture.francetransfert.application.resources.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DownloadPasswordMetaData {
    @NotBlank(message = "EnclosureId obligatoire")
    private String enclosure;
    private String recipient;
    private String password;
    private String token;

}
