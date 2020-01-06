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

    private int nbPrevDownloads;
    private LocalDate validUntilDate;
    private String senderEmail;
    private List<FileRepresentation> rootFiles;
    private List<DirectoryRepresentation> rootDirs;
    private String downloadURL;
}
