package fr.gouv.culture.francetransfert.application.resources;

import fr.gouv.culture.francetransfert.application.resources.model.DownloadRepresentation;
import fr.gouv.culture.francetransfert.application.security.services.TokenService;
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

    @Autowired
    TokenService tokenService;


    @GetMapping("/download")
    @ApiOperation(httpMethod = "GET", value = "Download  ")
    public DownloadRepresentation processDownload(HttpServletResponse response,
                                                  @RequestParam("enclosure") String enclosureId,
                                                  @RequestParam("recipient") String recipientMail,
                                                  @RequestParam("token") String recipientId,
                                                  @RequestParam("password") String password) throws Exception {
        DownloadRepresentation downloadRepresentation = downloadServices.processDownload(enclosureId, recipientMail, recipientId, password);
        response.setStatus(HttpStatus.OK.value());
        return downloadRepresentation;
    }


    @GetMapping("/download-info")
    @ApiOperation(httpMethod = "GET", value = "Download Info")
    public DownloadRepresentation downloadinfo(HttpServletResponse response,
                                                @RequestParam("enclosure") String enclosureId,
                                                @RequestParam("recipient") String recipientMail,
                                                @RequestParam("token") String recipientId) throws Exception {
        DownloadRepresentation downloadRepresentation = downloadServices.downloadInfo(enclosureId, recipientMail, recipientId);
        response.setStatus(HttpStatus.OK.value());
        return downloadRepresentation;
    }




}