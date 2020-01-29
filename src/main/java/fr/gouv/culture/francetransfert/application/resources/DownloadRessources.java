package fr.gouv.culture.francetransfert.application.resources;

import fr.gouv.culture.francetransfert.application.resources.model.DownloadRepresentation;
import fr.gouv.culture.francetransfert.application.services.DownloadServices;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@CrossOrigin
@RestController
@RequestMapping("/api-private/download-module")
@Api(value = "Download resources")
@Validated
public class DownloadRessources {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadRessources.class);

    @Autowired
    DownloadServices downloadServices;


    @GetMapping("/generate-download-url")
    @ApiOperation(httpMethod = "GET", value = "Generate download URL ")
    public void generateDownloadUrlWithPassword(HttpServletResponse response,
                                                  @RequestParam("enclosure") String enclosureId,
                                                  @RequestParam("recipient") String recipientMail,
                                                  @RequestParam("token") String recipientId,
                                                  @RequestParam(value = "password", required = false) String password) throws Exception {
        String downloadURL = downloadServices.generateDownloadUrlWithPassword(enclosureId, recipientMail, recipientId, password);
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader("Location", downloadURL);
    }


    @GetMapping("/download-info")
    @ApiOperation(httpMethod = "GET", value = "Download Info without URL ")
    public DownloadRepresentation downloadinfo(HttpServletResponse response,
                                                @RequestParam("enclosure") String enclosureId,
                                                @RequestParam("recipient") String recipientMailInBase64,
                                                @RequestParam("token") String recipientId) throws Exception {
        DownloadRepresentation downloadRepresentation = downloadServices.getDownloadInfo(enclosureId, recipientId, recipientMailInBase64);
        response.setStatus(HttpStatus.OK.value());
        return downloadRepresentation;
    }

}
