package fr.gouv.culture.francetransfert.application.services;

import com.google.gson.Gson;
import fr.gouv.culture.francetransfert.application.error.ErrorEnum;
import fr.gouv.culture.francetransfert.application.resources.model.rate.RateRepresentation;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import fr.gouv.culture.francetransfert.domain.utils.DownloadUtils;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.RedisManager;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.enums.RedisQueueEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RateServices {
    private static final Logger LOGGER = LoggerFactory.getLogger(RateServices.class);
    
    @Autowired
    RedisManager redisManager;

    public void createSatisfactionFT(RateRepresentation rateRepresentation) throws DownloadException {
        try {
            if (null == rateRepresentation) {
                String uuid = UUID.randomUUID().toString();
                LOGGER.error("Type: {} -- id: {} ", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
                throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
            }
            rateRepresentation.setMailAdress(DownloadUtils.base64Decoder(rateRepresentation.getMailAdress()));
            String jsonInString = new Gson().toJson(rateRepresentation);

//            RedisManager redisManager = RedisManager.getInstance();
            redisManager.publishFT(RedisQueueEnum.SATISFACTION_QUEUE.getValue(), jsonInString);
        } catch (Exception e) {
            String uuid = UUID.randomUUID().toString();
            LOGGER.error("Type: {} -- id: {} ", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
            throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
        }
    }
}
