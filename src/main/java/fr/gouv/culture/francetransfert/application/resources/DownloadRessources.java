package fr.gouv.culture.francetransfert.application.resources;

import fr.gouv.culture.francetransfert.application.resources.model.Download;
import fr.gouv.culture.francetransfert.application.resources.model.DownloadRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.rate.RateRepresentation;
import fr.gouv.culture.francetransfert.application.services.DownloadServices;
import fr.gouv.culture.francetransfert.application.services.RateServices;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

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
    private RateServices rateServices;


    @GetMapping("/generate-download-url")
    @ApiOperation(httpMethod = "GET", value = "Generate download URL ")
    public Download generateDownloadUrlWithPassword(HttpServletResponse response,
                                                  @RequestParam("enclosure") String enclosureId,
                                                  @RequestParam("recipient") String recipientMail,
                                                  @RequestParam("token") String recipientId,
                                                  @RequestParam(value = "password", required = false) String password) throws Exception {
        LOGGER.info("===========================================================================================================================");
        LOGGER.info("=============================================== start generate download URL ===============================================");
        LOGGER.info("===========================================================================================================================");
        Download downloadURL = downloadServices.generateDownloadUrlWithPassword(enclosureId, recipientMail, recipientId, password);
        response.setStatus(HttpStatus.OK.value());
        return downloadURL;
    }


    @GetMapping("/download-info")
    @ApiOperation(httpMethod = "GET", value = "Download Info without URL ")
    public DownloadRepresentation downloadinfo(HttpServletResponse response,
                                                @RequestParam("enclosure") String enclosureId,
                                                @RequestParam("recipient") String recipientMailInBase64,
                                                @RequestParam("token") String recipientId) throws Exception {
        LOGGER.info("===================================================================================================================");
        LOGGER.info("=============================================== start donlowad info ===============================================");
        LOGGER.info("===================================================================================================================");
        DownloadRepresentation downloadRepresentation = downloadServices.getDownloadInfo(enclosureId, recipientId, recipientMailInBase64);
        response.setStatus(HttpStatus.OK.value());
        return downloadRepresentation;
    }
    

    @RequestMapping(value = "/satisfaction", method = RequestMethod.POST)
    @ApiOperation(httpMethod = "POST", value = "Rates the app on a scvale of 1 to 4")
    public void createSatisfactionFT(HttpServletResponse response,
                             @Valid @RequestBody RateRepresentation rateRepresentation) throws DownloadException {
        LOGGER.info("==================================================================================================================");
        LOGGER.info("=============================================== start Satisfaction ===============================================");
        LOGGER.info("==================================================================================================================");
        rateServices.createSatisfactionFT(rateRepresentation);
    }
}
