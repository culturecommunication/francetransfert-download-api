package fr.gouv.culture.francetransfert.application.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import fr.gouv.culture.francetransfert.application.error.ErrorEnum;
import fr.gouv.culture.francetransfert.application.error.MaxTryException;
import fr.gouv.culture.francetransfert.application.error.PasswordException;
import fr.gouv.culture.francetransfert.application.error.UnauthorizedAccessException;
import fr.gouv.culture.francetransfert.application.resources.model.DirectoryRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.Download;
import fr.gouv.culture.francetransfert.application.resources.model.DownloadRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.FileRepresentation;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import fr.gouv.culture.francetransfert.domain.exceptions.ExpirationEnclosureException;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.RedisManager;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.enums.RedisQueueEnum;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.enums.RootDirKeysEnum;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.enums.RootFileKeysEnum;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.utils.DateUtils;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.utils.RedisUtils;
import fr.gouv.culture.francetransfert.francetransfert_storage_api.StorageManager;
import fr.gouv.culture.francetransfert.utils.Base64CryptoService;

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
			String password) throws Exception {
//            RedisManager redisManager = RedisManager.getInstance();
		validatePassword(redisManager, enclosureId, password, recipientId);
		String recipientMailD = base64CryptoService.base64Decoder(recipientMail);
		validateDownloadAuthorization(redisManager, enclosureId, recipientMailD, recipientId);
		downloadProgress(redisManager, enclosureId, recipientId);
		return getDownloadUrl(redisManager, enclosureId);
	}

	public Download generatePublicDownload(String enclosureId, String password) throws Exception {
		validatePassword(redisManager, enclosureId, password, "");
		RedisUtils.incrementNumberOfDownloadPublic(redisManager, enclosureId);
		return getDownloadUrl(redisManager, enclosureId);
	}

	public String getNumberOfDownloadPublic(String enclosureId) {
		Map<String, String> enclosureMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
		if (enclosureMap != null) {
			return enclosureMap.get(EnclosureKeysEnum.PUBLIC_DOWNLOAD_COUNT.getKey());
		} else {
			String uuid = UUID.randomUUID().toString();
			LOGGER.error("Type: {} -- id: {} -- Message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid,
					"Invalid Token");
			throw new DownloadException(ErrorEnum.WRONG_ENCLOSURE.getValue(), uuid);
		}
	}

	public DownloadRepresentation getDownloadInfo(String enclosureId, String recipientId, String recipientMailInBase64)
			throws Exception {

//        RedisManager redisManager = RedisManager.getInstance();
		// validate Enclosure download right
		String recipientMail = base64CryptoService.base64Decoder(recipientMailInBase64);
		LocalDate expirationDate = validateDownloadAuthorization(redisManager, enclosureId, recipientMail, recipientId);
		try {
			String passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosureId,
					EnclosureKeysEnum.PASSWORD.getKey());
			String message = RedisUtils.getEnclosureValue(redisManager, enclosureId,
					EnclosureKeysEnum.MESSAGE.getKey());
			String senderMail = RedisUtils.getEmailSenderEnclosure(redisManager, enclosureId);
			List<FileRepresentation> rootFiles = getRootFiles(redisManager, enclosureId);
			List<DirectoryRepresentation> rootDirs = getRootDirs(redisManager, enclosureId);

			return DownloadRepresentation.builder().validUntilDate(expirationDate).senderEmail(senderMail)
					.message(message).rootFiles(rootFiles).rootDirs(rootDirs)
					.withPassword(!StringUtils.isEmpty(passwordRedis)).build();
		} catch (Exception e) {
			String uuid = UUID.randomUUID().toString();
			LOGGER.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid,
					e.getMessage(), e);
			throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
		}
	}

	public DownloadRepresentation getDownloadInfoPublic(String enclosureId) throws Exception {
		LocalDate expirationDate = validateDownloadAuthorizationPublic(redisManager, enclosureId);
		try {
			List<FileRepresentation> rootFiles = getRootFiles(redisManager, enclosureId);
			List<DirectoryRepresentation> rootDirs = getRootDirs(redisManager, enclosureId);
			return DownloadRepresentation.builder().validUntilDate(expirationDate).rootFiles(rootFiles)
					.rootDirs(rootDirs).build();
		} catch (Exception e) {
			String uuid = UUID.randomUUID().toString();
			LOGGER.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid,
					e.getMessage(), e);
			throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
		}
	}

	private Download getDownloadUrl(RedisManager redisManager, String enclosureId) throws DownloadException {
		try {
//            StorageManager storageManager = StorageManager.getInstance();
			String bucketName = RedisUtils.getBucketName(redisManager, enclosureId, bucketPrefix);
			String fileToDownload = storageManager.getZippedEnclosureName(enclosureId) + ".zip";
			int expireInMinutes = 2; // periode to exipre the generated URL
			String downloadURL = storageManager.generateDownloadURL(bucketName, fileToDownload, expireInMinutes)
					.toString();
			return Download.builder().downloadURL(downloadURL).build();
		} catch (Exception e) {
			String uuid = UUID.randomUUID().toString();
			LOGGER.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid,
					e.getMessage(), e);
			throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
		}
	}

	/**
	 * Method to validate download authorization : validate number of download,
	 * validate expiration date and validate recipientId sended by the front
	 * 
	 * @param redisManager
	 * @param enclosureId
	 * @param recipientMail
	 * @param recipientId
	 * @return enclosure expiration Date
	 * @throws Exception
	 */
	private LocalDate validateDownloadAuthorization(RedisManager redisManager, String enclosureId, String recipientMail,
			String recipientId) throws Exception {
		validateRecipientId(redisManager, enclosureId, recipientMail, recipientId);
		validateNumberOfDownload(redisManager, recipientId);
		LocalDate expirationDate = validateExpirationDate(redisManager, enclosureId);
		return expirationDate;
	}

	private LocalDate validateExpirationDate(RedisManager redisManager, String enclosureId) throws Exception {
		LocalDate expirationDate = DateUtils.convertStringToLocalDate(
				RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.EXPIRED_TIMESTAMP.getKey()));
		if (LocalDate.now().isAfter(expirationDate)) {
			throw new ExpirationEnclosureException("Vous ne pouvez plus telecharger ces fichiers");
		}
		return expirationDate;
	}

	private int validateNumberOfDownload(RedisManager redisManager, String recipientId) throws Exception {
		int numberOfDownload = RedisUtils.getNumberOfDownloadsPerRecipient(redisManager, recipientId);
		if (maxDownload <= numberOfDownload) {
			String uuid = UUID.randomUUID().toString();
			LOGGER.error("Type: {} -- id: {} ", ErrorEnum.DOWNLOAD_LIMIT.getValue(), uuid);
			throw new DownloadException(ErrorEnum.DOWNLOAD_LIMIT.getValue(), uuid);
		}
		return numberOfDownload;
	}

	private void validateRecipientId(RedisManager redisManager, String enclosureId, String recipientMail,
			String recipientId) throws Exception {
		try {
			String recipientIdRedis = RedisUtils.getRecipientId(redisManager, enclosureId, recipientMail);
			if (!recipientIdRedis.equals(recipientId)) {
				String uuid = UUID.randomUUID().toString();
				LOGGER.error("Type: {} -- id: {} -- message : ", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid,
						"recipient id send not equals to Redis recipient id for this enclosure");
				throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
			}
		} catch (Exception e) {
			String uuid = UUID.randomUUID().toString();
			LOGGER.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid,
					e.getMessage(), e);
			throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
		}
	}

	public void validatePassword(RedisManager redisManager, String enclosureId, String password, String recipientId)
			throws Exception {
		String passwordRedis = "";
		String passwordUnHashed = "";
		int passwordCountTry = 0;
		Map<String, String> enclosureMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
		Boolean publicLink = Boolean.valueOf(enclosureMap.get(EnclosureKeysEnum.PUBLIC_LINK.getKey()));
		try {
			if (!publicLink) {
				passwordCountTry = RedisUtils.getPasswordTryCountPerRecipient(redisManager, recipientId, enclosureId);
			}
			passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosureId,
					EnclosureKeysEnum.PASSWORD.getKey());
			if (passwordRedis != null && !StringUtils.isEmpty(passwordRedis)) {
				passwordUnHashed = base64CryptoService.aesDecrypt(passwordRedis);
			} else {
				return;
			}
		} catch (Exception e) {
			String uuid = UUID.randomUUID().toString();
			LOGGER.error("Error validation:", e);
			LOGGER.error("Type: {} -- id: {} ", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
			throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
		}
		if (!(password != null && passwordRedis != null && password.trim().equals(passwordUnHashed.trim()))) {
			if (!publicLink) {
				RedisUtils.incrementNumberOfPasswordTry(redisManager, recipientId, enclosureId);
				if (passwordCountTry > maxPasswordTry) {
					throw new MaxTryException("Nombre d'essais maximum atteint");
				}
			}
			throw new PasswordException(ErrorEnum.WRONG_PASSWORD.getValue(), null, passwordCountTry + 1);
		} else {
			if (passwordCountTry < maxPasswordTry) {
				RedisUtils.resetPasswordTryCountPerRecipient(redisManager, recipientId, enclosureId);
			} else {
				throw new MaxTryException("Nombre d'essais maximum atteint");
			}
		}
	}

	private void downloadProgress(RedisManager redisManager, String enclosureId, String recipientId)
			throws DownloadException {
		try {
			// increment nb_download for this recipient
			RedisUtils.incrementNumberOfDownloadsForRecipient(redisManager, recipientId);
			// add to queue Redis download progress: to send download mail in progress to
			// the sender
			String downloadQueueValue = enclosureId + ":" + recipientId;
			redisManager.rpush(RedisQueueEnum.DOWNLOAD_QUEUE.getValue(), downloadQueueValue);
		} catch (Exception e) {
			String uuid = UUID.randomUUID().toString();
			LOGGER.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid,
					e.getMessage(), e);
			throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
		}
	}

	private List<FileRepresentation> getRootFiles(RedisManager redisManager, String enclosureId)
			throws DownloadException {
		List<FileRepresentation> rootFiles = new ArrayList<>();
		List<DirectoryRepresentation> rootDirs = new ArrayList<>();
		redisManager.lrange(RedisKeysEnum.FT_ROOT_FILES.getKey(enclosureId), 0, -1).forEach(rootFileName -> {
			String size = "";
			String hashRootFile = RedisUtils.generateHashsha1(enclosureId + ":" + rootFileName);
			try {
				size = redisManager.getHgetString(RedisKeysEnum.FT_ROOT_FILE.getKey(hashRootFile),
						RootFileKeysEnum.SIZE.getKey());
			} catch (Exception e) {
				String uuid = UUID.randomUUID().toString();
				LOGGER.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid,
						e.getMessage(), e);
				throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
			}
			FileRepresentation rootFile = new FileRepresentation();
			rootFile.setName(rootFileName);
			rootFile.setSize(Long.valueOf(size));
			rootFiles.add(rootFile);
			LOGGER.debug("root file: {}", rootFileName);
		});
		return rootFiles;
	}

	private List<DirectoryRepresentation> getRootDirs(RedisManager redisManager, String enclosureId)
			throws DownloadException {
		List<DirectoryRepresentation> rootDirs = new ArrayList<>();
		redisManager.lrange(RedisKeysEnum.FT_ROOT_DIRS.getKey(enclosureId), 0, -1).forEach(rootDirName -> {
			String size = "";
			String hashRootDir = RedisUtils.generateHashsha1(enclosureId + ":" + rootDirName);
			try {
				size = redisManager.getHgetString(RedisKeysEnum.FT_ROOT_DIR.getKey(hashRootDir),
						RootDirKeysEnum.TOTAL_SIZE.getKey());
			} catch (Exception e) {
				String uuid = UUID.randomUUID().toString();
				LOGGER.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid,
						e.getMessage(), e);
				throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
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
				String uuid = UUID.randomUUID().toString();
				LOGGER.error("Type: {} -- id: {} -- Message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid,
						"Invalid Token");
				throw new UnauthorizedAccessException("Invalid Token");
			}
		} else {
			String uuid = UUID.randomUUID().toString();
			LOGGER.error("Type: {} -- id: {} -- Message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), uuid,
					"Invalid Token");
			throw new UnauthorizedAccessException("Invalid Token");
		}
	}

	private LocalDate validateDownloadAuthorizationPublic(RedisManager redisManager, String enclosureId)
			throws Exception {
		LocalDate expirationDate = validateExpirationDate(redisManager, enclosureId);
		return expirationDate;
	}
}
