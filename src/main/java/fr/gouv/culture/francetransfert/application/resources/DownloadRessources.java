package fr.gouv.culture.francetransfert.application.resources;

import fr.gouv.culture.francetransfert.application.error.UnauthorizedAccessException;
import fr.gouv.culture.francetransfert.application.resources.model.Download;
import fr.gouv.culture.francetransfert.application.resources.model.DownloadRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.ValidatePasswordMetaData;
import fr.gouv.culture.francetransfert.application.resources.model.ValidatePasswordRepresentation;
import fr.gouv.culture.francetransfert.application.services.DownloadServices;
import fr.gouv.culture.francetransfert.application.services.RateServices;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.RedisManager;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.utils.RedisUtils;
import fr.gouv.culture.francetransfert.model.RateRepresentation;
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

    @Autowired
    private RedisManager redisManager;


    @GetMapping("/generate-download-url")
    @ApiOperation(httpMethod = "GET", value = "Generate download URL ")
    public Download generateDownloadUrlWithPassword(HttpServletResponse response,
                                                  @RequestParam("enclosure") String enclosureId,
                                                  @RequestParam("recipient") String recipientMail,
                                                  @RequestParam("token") String recipientId,
                                                  @RequestParam(value = "password", required = false) String password) throws Exception {
        LOGGER.info("start generate download URL ");
        Download downloadURL = downloadServices.generateDownloadUrlWithPassword(enclosureId, recipientMail, recipientId, password);
        response.setStatus(HttpStatus.OK.value());
        return downloadURL;
    }

    @GetMapping("/generate-download-url-public")
    @ApiOperation(httpMethod = "GET", value = "Generate download URL ")
    public Download generateDownloadUrlWithPassword(HttpServletResponse response,
                                                    @RequestParam("enclosure") String enclosureId, @RequestParam("password") String password) throws UnauthorizedAccessException, Exception {
        LOGGER.info(
                "start generate download URL ");
        downloadServices.validatePublic(enclosureId);
        Download downloadURL = downloadServices.generatePublicDownload(enclosureId, password);
        response.setStatus(HttpStatus.OK.value());
        return downloadURL;
    }

    @PostMapping("/validate-password")
    @ApiOperation(httpMethod = "POST", value = "Validate password")
    public ValidatePasswordRepresentation validatePassword(@RequestBody @Valid ValidatePasswordMetaData metaData) throws Exception {
        ValidatePasswordRepresentation representation = new ValidatePasswordRepresentation();
        try{
            downloadServices.validatePassword(redisManager,metaData.getEnclosureId(), metaData.getPassword(), metaData.getRecipientId());
            representation.setValid(true);
        }catch (Exception e){
            representation.setValid(false);
            representation.setPasswordTryCount(RedisUtils.getPasswordTryCountPerRecipient(redisManager, metaData.getRecipientId()));
            throw e;
        }
        return representation;
    }


    @GetMapping("/download-info")
    @ApiOperation(httpMethod = "GET", value = "Download Info without URL ")
    public DownloadRepresentation downloadinfo(HttpServletResponse response,
                                                @RequestParam("enclosure") String enclosureId,
                                                @RequestParam("recipient") String recipientMailInBase64,
                                                @RequestParam("token") String recipientId) throws Exception {
        LOGGER.info("start donlowad info ");
        DownloadRepresentation downloadRepresentation = downloadServices.getDownloadInfo(enclosureId, recipientId, recipientMailInBase64);
        response.setStatus(HttpStatus.OK.value());
        return downloadRepresentation;
    }
    

    @RequestMapping(value = "/satisfaction", method = RequestMethod.POST)
    @ApiOperation(httpMethod = "POST", value = "Rates the app on a scvale of 1 to 4")
    public void createSatisfactionFT(HttpServletResponse response,
                             @Valid @RequestBody RateRepresentation rateRepresentation) throws DownloadException {
        LOGGER.info("start Satisfaction ");
        rateServices.createSatisfactionFT(rateRepresentation);
    }

    @GetMapping("/download-count-public")
    public String getDownloadCount(@RequestParam("enclosure") String enclosureId, @RequestParam("token") String token) throws DownloadException{
        downloadServices.validateToken(enclosureId, token);
        return downloadServices.getNumberOfDownloadPublic(enclosureId);
    }

    @GetMapping("/download-info-public")
    public DownloadRepresentation downloadInfoPublic(HttpServletResponse response,@RequestParam("enclosure") String enclosureId ) throws UnauthorizedAccessException,Exception {
        LOGGER.info(
                "start download info public ");
        downloadServices.validatePublic(enclosureId);
        DownloadRepresentation downloadRepresentation = downloadServices.getDownloadInfoPublic(enclosureId);
        response.setStatus(HttpStatus.OK.value());
        return downloadRepresentation;
    }

}
