package fr.gouv.culture.francetransfert.application.resources.model;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DownloadRepresentation {

    private LocalDate validUntilDate;
    private String senderEmail;
    private String message;
    private List<FileRepresentation> rootFiles;
    private List<DirectoryRepresentation> rootDirs;
    private boolean withPassword;
}
