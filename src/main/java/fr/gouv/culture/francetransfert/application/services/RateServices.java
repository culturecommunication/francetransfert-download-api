package fr.gouv.culture.francetransfert.application.services;

import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import fr.gouv.culture.francetransfert.application.error.ErrorEnum;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import fr.gouv.culture.francetransfert.enums.TypeStat;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.RedisManager;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.enums.RedisQueueEnum;
import fr.gouv.culture.francetransfert.model.RateRepresentation;
import fr.gouv.culture.francetransfert.utils.Base64CryptoService;

@Service
public class RateServices {
	private static final Logger LOGGER = LoggerFactory.getLogger(RateServices.class);

	@Autowired
	RedisManager redisManager;

	@Autowired
	Base64CryptoService base64CryptoService;

	public void createSatisfactionFT(RateRepresentation rateRepresentation) throws DownloadException {
		try {

			if (null == rateRepresentation) {
				String uuid = UUID.randomUUID().toString();
				LOGGER.error("Type: {} -- id: {} -- message {} ", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid, "rateRepresentation is null");
				throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
			}

			String domain = base64CryptoService.base64Decoder(rateRepresentation.getMailAdress()).split("@")[1];
			rateRepresentation.setDate(LocalDate.now());
			rateRepresentation.setDomain(domain);
			rateRepresentation.setHashMail(null);
			rateRepresentation.setMailAdress(null);
			rateRepresentation.setType(TypeStat.DOWNLOAD_SATISFACTION);
			String jsonInString = new Gson().toJson(rateRepresentation);
			redisManager.publishFT(RedisQueueEnum.SATISFACTION_QUEUE.getValue(), jsonInString);

		} catch (Exception e) {
			String uuid = UUID.randomUUID().toString();
			LOGGER.error("Type: {} -- id: {} -- message {}", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid, e.getMessage(), e);
			throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
		}
	}
}
