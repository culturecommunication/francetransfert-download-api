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
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RateServices {
    private static final Logger LOGGER = LoggerFactory.getLogger(RateServices.class);

    public void createSatisfactionFT(RateRepresentation rateRepresentation) throws DownloadException {
        try {
            if (null == rateRepresentation) {
                LOGGER.error("rateRepresentation is null");
                throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), UUID.randomUUID().toString());
            }
            rateRepresentation.setMailAdress(DownloadUtils.base64Decoder(rateRepresentation.getMailAdress()));
            String jsonInString = new Gson().toJson(rateRepresentation);

            RedisManager redisManager = RedisManager.getInstance();
            redisManager.publishFT(RedisQueueEnum.SATISFACTION_QUEUE.getValue(), jsonInString);
        } catch (Exception e) {
            throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), UUID.randomUUID().toString());
        }
    }
}
