package fr.gouv.culture.francetransfert.application.services;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.application.error.ErrorEnum;
import fr.gouv.culture.francetransfert.application.error.MaxTryException;
import fr.gouv.culture.francetransfert.application.error.PasswordException;
import fr.gouv.culture.francetransfert.application.error.UnauthorizedAccessException;
import fr.gouv.culture.francetransfert.application.resources.model.DirectoryRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.Download;
import fr.gouv.culture.francetransfert.application.resources.model.DownloadRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.FileRepresentation;
import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RecipientKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisQueueEnum;
import fr.gouv.culture.francetransfert.core.enums.RootDirKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RootFileKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.TypeStat;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.exception.StatException;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.services.StorageManager;
import fr.gouv.culture.francetransfert.core.utils.Base64CryptoService;
import fr.gouv.culture.francetransfert.core.utils.DateUtils;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import fr.gouv.culture.francetransfert.domain.exceptions.ExpirationEnclosureException;

@Service
public class DownloadServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadServices.class);

	@Value("${enclosure.max.download}")
	private int maxDownload;

	@Value("${enclosure.max.password.try}")
	private int maxPasswordTry;

	@Value("${bucket.prefix}")
	private String bucketPrefix;

	@Autowired
	StorageManager storageManager;

	@Autowired
	RedisManager redisManager;

	@Autowired
	Base64CryptoService base64CryptoService;

	public Download generateDownloadUrlWithPassword(String enclosureId, String recipientMail, String recipientId,
			String password) throws ExpirationEnclosureException, MetaloadException, UnsupportedEncodingException {
		validatePassword(enclosureId, password, recipientId);
		String recipientMailD = base64CryptoService.base64Decoder(recipientMail);
		validateDownloadAuthorization(enclosureId, recipientMailD, recipientId);
		downloadProgress(enclosureId, recipientId);
		String statMessage = TypeStat.DOWNLOAD + ";" + enclosureId + ";" + recipientMailD;
		redisManager.publishFT(RedisQueueEnum.STAT_QUEUE.getValue(), statMessage);
		return getDownloadUrl(enclosureId);
	}

	public Download generatePublicDownload(String enclosureId, String password)
			throws MetaloadException, UnsupportedEncodingException {
		validatePassword(enclosureId, password, "");
		RedisUtils.incrementNumberOfDownloadPublic(redisManager, enclosureId);
		String statMessage = TypeStat.DOWNLOAD + ";" + enclosureId;
		redisManager.publishFT(RedisQueueEnum.STAT_QUEUE.getValue(), statMessage);
		return getDownloadUrl(enclosureId);
	}

	public String getNumberOfDownloadPublic(String enclosureId) {
		Map<String, String> enclosureMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
		if (enclosureMap != null) {
			return enclosureMap.get(EnclosureKeysEnum.PUBLIC_DOWNLOAD_COUNT.getKey());
		} else {
			throw new DownloadException(ErrorEnum.WRONG_ENCLOSURE.getValue(), enclosureId);
		}
	}

	public DownloadRepresentation getDownloadInfo(String enclosureId, String recipientId, String recipientMailInBase64)
			throws UnsupportedEncodingException, ExpirationEnclosureException, MetaloadException {

		// validate Enclosure download right
		String recipientMail = base64CryptoService.base64Decoder(recipientMailInBase64);
		LocalDate expirationDate = validateDownloadAuthorization(enclosureId, recipientMail, recipientId);
		checkDeletePlis(enclosureId);

		try {

			String passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosureId,
					EnclosureKeysEnum.PASSWORD.getKey());
			String message = RedisUtils.getEnclosureValue(redisManager, enclosureId,
					EnclosureKeysEnum.MESSAGE.getKey());
			String senderMail = RedisUtils.getEmailSenderEnclosure(redisManager, enclosureId);

			List<FileRepresentation> rootFiles = getRootFiles(enclosureId);
			List<DirectoryRepresentation> rootDirs = getRootDirs(enclosureId);

			return DownloadRepresentation.builder().validUntilDate(expirationDate).senderEmail(senderMail)
					.recipientMail(recipientMail).message(message).rootFiles(rootFiles).rootDirs(rootDirs)
					.withPassword(!StringUtils.isEmpty(passwordRedis)).build();
		} catch (Exception e) {
			throw new DownloadException("Cannot get Download Info : " + e.getMessage(), enclosureId, e);
		}
	}

	public DownloadRepresentation getDownloadInfoPublic(String enclosureId)
			throws ExpirationEnclosureException, MetaloadException {
		LocalDate expirationDate = validateDownloadAuthorizationPublic(enclosureId);
		checkDeletePlis(enclosureId);
		try {
			List<FileRepresentation> rootFiles = getRootFiles(enclosureId);
			List<DirectoryRepresentation> rootDirs = getRootDirs(enclosureId);
			return DownloadRepresentation.builder().validUntilDate(expirationDate).rootFiles(rootFiles)
					.rootDirs(rootDirs).build();
		} catch (Exception e) {
			throw new DownloadException("Cannot get Download Info : " + e.getMessage(), enclosureId, e);
		}
	}

	private void checkDeletePlis(String enclosureId) {
		Map<String, String> tokenMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ADMIN_TOKEN.getKey(enclosureId));

		if (tokenMap.size() == 0) {
			throw new DownloadException(ErrorEnum.DELETED_ENCLOSURE.getValue(), enclosureId);
		}
	}

	private Download getDownloadUrl(String enclosureId) throws DownloadException {
		try {
			String bucketName = RedisUtils.getBucketName(redisManager, enclosureId, bucketPrefix);
			String fileToDownload = storageManager.getZippedEnclosureName(enclosureId);
			int expireInMinutes = 2; // periode to exipre the generated URL
			String downloadURL = storageManager.generateDownloadURL(bucketName, fileToDownload, expireInMinutes)
					.toString();
			return Download.builder().downloadURL(downloadURL).build();
		} catch (Exception e) {
			throw new DownloadException("Cannot get Download URL : " + e.getMessage(), enclosureId, e);
		}
	}

	/**
	 * Method to validate download authorization : validate number of download,
	 * validate expiration date and validate recipientId sended by the front
	 *
	 * @param enclosureId
	 * @param recipientMail
	 * @param recipientId
	 * @return enclosure expiration Date
	 * @throws MetaloadException
	 * @throws ExpirationEnclosureException
	 */
	private LocalDate validateDownloadAuthorization(String enclosureId, String recipientMail, String recipientId)
			throws ExpirationEnclosureException, MetaloadException {
		validateRecipientId(enclosureId, recipientMail, recipientId);
		validateNumberOfDownload(recipientId);
		LocalDate expirationDate = validateExpirationDate(enclosureId);
		return expirationDate;
	}

	private LocalDate validateExpirationDate(String enclosureId)
			throws ExpirationEnclosureException, MetaloadException {
		LocalDate expirationDate = DateUtils.convertStringToLocalDate(
				RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.EXPIRED_TIMESTAMP.getKey()));
		if (LocalDate.now().isAfter(expirationDate)) {
			throw new ExpirationEnclosureException("Vous ne pouvez plus telecharger ces fichiers");
		}
		return expirationDate;
	}

	private int validateNumberOfDownload(String recipientId) throws MetaloadException {
		int numberOfDownload = RedisUtils.getNumberOfDownloadsPerRecipient(redisManager, recipientId);
		if (maxDownload <= numberOfDownload) {
			String uuid = UUID.randomUUID().toString();
			throw new DownloadException(ErrorEnum.DOWNLOAD_LIMIT.getValue(), uuid);
		}
		return numberOfDownload;
	}

	private void validateRecipientId(String enclosureId, String recipientMail, String recipientId) {
		try {
			String recipientIdRedis = RedisUtils.getRecipientId(redisManager, enclosureId, recipientMail);
			if (!recipientIdRedis.equals(recipientId)) {
				throw new DownloadException("NewRecipient id send not equals to Redis recipient id for this enclosure",
						enclosureId);
			}
		} catch (Exception e) {
			throw new DownloadException("Error while validating recipient Id : " + e.getMessage(), enclosureId, e);
		}
	}

	public void validatePassword(String enclosureId, String password, String recipientEncodedMail)
			throws UnsupportedEncodingException, MetaloadException {
		String passwordUnHashed = "";
		int passwordCountTry = 0;
		String recipientId = "";
		Boolean recipientDeleted = false;

		if (StringUtils.isNotBlank(recipientEncodedMail)) {
			String recipientMailD = base64CryptoService.base64Decoder(recipientEncodedMail);
			recipientId = RedisUtils.getRecipientId(redisManager, enclosureId, recipientMailD);
			recipientDeleted = RedisUtils.isRecipientDeleted(redisManager, recipientId);

		}
		if(!recipientDeleted){
		Map<String, String> enclosureMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
		Boolean publicLink = Boolean.valueOf(enclosureMap.get(EnclosureKeysEnum.PUBLIC_LINK.getKey()));
		try {
			if (!publicLink) {
				passwordCountTry = RedisUtils.getPasswordTryCountPerRecipient(redisManager, recipientId);
			}
			passwordUnHashed = getUnhashedPassword(enclosureId);
		} catch (Exception e) {
			throw new DownloadException("Error Unhashing password", enclosureId, e);
		}

		if (!(password != null && passwordUnHashed != null && password.equals(passwordUnHashed))) {
			if (!publicLink) {
				RedisUtils.incrementNumberOfPasswordTry(redisManager, recipientId);
				redisManager.hsetString(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId),
						RecipientKeysEnum.LAST_PASSWORD_TRY.getKey(), LocalDateTime.now().toString(), -1);
				if ((passwordCountTry + 1) >= maxPasswordTry) {
					throw new MaxTryException("Nombre d'essais maximum atteint", enclosureId);
				}
			}
			throw new PasswordException(ErrorEnum.WRONG_PASSWORD.getValue(), enclosureId, passwordCountTry + 1);
		} else {
			if (passwordCountTry <= maxPasswordTry) {
				RedisUtils.resetPasswordTryCountPerRecipient(redisManager, recipientId);
			} else {
				throw new MaxTryException("Nombre d'essais maximum atteint", enclosureId);
			}
		}
		}else {
			throw new UnauthorizedAccessException("Unauthorized");
		}
	}

	private String getUnhashedPassword(String enclosureId) throws MetaloadException, StatException {
		String passwordRedis;
		String passwordUnHashed;
		passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.PASSWORD.getKey());
		if (passwordRedis != null && !StringUtils.isEmpty(passwordRedis)) {
			passwordUnHashed = base64CryptoService.aesDecrypt(passwordRedis);
		} else {
			throw new DownloadException("No Password for enclosure {}", enclosureId);
		}
		return passwordUnHashed;
	}

	private void downloadProgress(String enclosureId, String recipientId) throws DownloadException {
		try {
			// increment nb_download for this recipient
			RedisUtils.incrementNumberOfDownloadsForRecipient(redisManager, recipientId);
			// add to queue Redis download progress: to send download mail in progress to
			// the sender
			String downloadQueueValue = enclosureId + ":" + recipientId;
			redisManager.rpush(RedisQueueEnum.DOWNLOAD_QUEUE.getValue(), downloadQueueValue);
		} catch (Exception e) {
			throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue() + " : " + e.getMessage(), enclosureId, e);
		}
	}

	private List<FileRepresentation> getRootFiles(String enclosureId) throws DownloadException {
		List<FileRepresentation> rootFiles = new ArrayList<>();
		redisManager.lrange(RedisKeysEnum.FT_ROOT_FILES.getKey(enclosureId), 0, -1).forEach(rootFileName -> {
			String size = "";
			String hashRootFile = RedisUtils.generateHashsha1(enclosureId + ":" + rootFileName);
			try {
				size = redisManager.getHgetString(RedisKeysEnum.FT_ROOT_FILE.getKey(hashRootFile),
						RootFileKeysEnum.SIZE.getKey());
			} catch (Exception e) {
				throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), enclosureId, e);
			}
			FileRepresentation rootFile = new FileRepresentation();
			rootFile.setName(rootFileName);
			rootFile.setSize(Long.valueOf(size));
			rootFiles.add(rootFile);
			LOGGER.debug("root file: {}", rootFileName);
		});
		return rootFiles;
	}

	private List<DirectoryRepresentation> getRootDirs(String enclosureId) throws DownloadException {
		List<DirectoryRepresentation> rootDirs = new ArrayList<>();
		redisManager.lrange(RedisKeysEnum.FT_ROOT_DIRS.getKey(enclosureId), 0, -1).forEach(rootDirName -> {
			String size = "";
			String hashRootDir = RedisUtils.generateHashsha1(enclosureId + ":" + rootDirName);
			try {
				size = redisManager.getHgetString(RedisKeysEnum.FT_ROOT_DIR.getKey(hashRootDir),
						RootDirKeysEnum.TOTAL_SIZE.getKey());
			} catch (Exception e) {
				throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), enclosureId, e);
			}
			DirectoryRepresentation rootDir = new DirectoryRepresentation();
			rootDir.setName(rootDirName);
			rootDir.setTotalSize(Long.valueOf(size));
			rootDirs.add(rootDir);
			LOGGER.debug("root Dir: {}", rootDirName);
		});
		return rootDirs;
	}

	public void validatePublic(String enclosureId) throws UnauthorizedAccessException {
		Map<String, String> enclosureMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
		Boolean publicLink = Boolean.valueOf(enclosureMap.get(EnclosureKeysEnum.PUBLIC_LINK.getKey()));
		if (!publicLink) {
			throw new UnauthorizedAccessException("Unauthorized");
		}
	}

	public void validateToken(String enclosureId, String token) {
		Map<String, String> tokenMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ADMIN_TOKEN.getKey(enclosureId));
		if (tokenMap != null) {
			if (!token.equals(tokenMap.get(EnclosureKeysEnum.TOKEN.getKey()))) {
				throw new UnauthorizedAccessException("Invalid Token");
			}
		} else {
			throw new UnauthorizedAccessException("Invalid Token");
		}
	}

	private LocalDate validateDownloadAuthorizationPublic(String enclosureId)
			throws ExpirationEnclosureException, MetaloadException {
		LocalDate expirationDate = validateExpirationDate(enclosureId);
		return expirationDate;
	}
}
