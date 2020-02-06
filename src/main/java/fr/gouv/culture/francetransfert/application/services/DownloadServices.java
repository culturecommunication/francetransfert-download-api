package fr.gouv.culture.francetransfert.application.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import fr.gouv.culture.francetransfert.application.error.UnauthorizedAccessException;
import fr.gouv.culture.francetransfert.application.resources.model.DirectoryRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.DownloadRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.FileRepresentation;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import fr.gouv.culture.francetransfert.domain.utils.DownloadUtils;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.RedisManager;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.enums.RedisQueueEnum;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.enums.RootDirKeysEnum;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.enums.RootFileKeysEnum;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.utils.DateUtils;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.utils.RedisUtils;
import fr.gouv.culture.francetransfert.francetransfert_storage_api.StorageManager;

@Service
public class DownloadServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadServices.class);

    @Value("${enclosure.max.download}")
    private int maxDownload;

    @Value("${bucket.prefix}")
    private String bucketPrefix;

    @Autowired
    PasswordHasherServices passwordHasherServices;

    public String generateDownloadUrlWithPassword(String enclosureId, String recipientMail, String recipientId, String password) throws Exception {
        RedisManager redisManager = RedisManager.getInstance();
        try {
            validatePassword(redisManager, enclosureId, password);
            String recipientMailD = DownloadUtils.base64Decoder(recipientMail);
            validateDownloadAuthorization(redisManager, enclosureId, recipientMailD, recipientId);
            downloadProgress(redisManager, enclosureId, recipientId);

        } catch (Exception e) {
            throw new DownloadException("errors validation");
        }
        return getDownloadUrl(redisManager, enclosureId);
    }

    public DownloadRepresentation getDownloadInfo(String enclosureId, String recipientId, String recipientMailInBase64) throws Exception {
        RedisManager redisManager = RedisManager.getInstance();
        //validate Enclosure download right
        String recipientMail = DownloadUtils.base64Decoder(recipientMailInBase64);
        LocalDate expirationDate = validateDownloadAuthorization(redisManager, enclosureId, recipientMail, recipientId);

        String passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.PASSWORD.getKey());
        String senderMail = RedisUtils.getSenderEnclosure(redisManager, enclosureId);
        List<FileRepresentation> rootFiles = getRootFiles(redisManager, enclosureId);
        List<DirectoryRepresentation> rootDirs = getRootDirs(redisManager, enclosureId);

        return DownloadRepresentation.builder()
                .validUntilDate(expirationDate)
                .senderEmail(senderMail)
                .rootFiles(rootFiles)
                .rootDirs(rootDirs)
                .withPassword(!StringUtils.isEmpty(passwordRedis))
                .build();
    }

    private String getDownloadUrl(RedisManager redisManager, String enclosureId) throws Exception {
        StorageManager storageManager = StorageManager.getInstance();
        String bucketName = RedisUtils.getBucketName(redisManager, enclosureId, bucketPrefix);
        String fileToDownload = storageManager.getZippedEnclosureName(enclosureId)+".zip";
        return storageManager.generateDownloadURL(bucketName, fileToDownload).toString();
    }

    /**
     * Method to validate download authorization : validate number of download, validate expiration date and validate recipientId sended by the front
     * @param redisManager
     * @param enclosureId
     * @param recipientMail
     * @param recipientId
     * @return enclosure expiration Date
     * @throws Exception
     */
    private LocalDate validateDownloadAuthorization(RedisManager redisManager, String enclosureId, String recipientMail, String recipientId) throws Exception {
        validateRecipientId(redisManager, enclosureId, recipientMail, recipientId);
        validateNumberOfDownload(redisManager, recipientId);
        LocalDate expirationDate = validateExpirationDate(redisManager, enclosureId);
        return expirationDate;
    }

    private LocalDate validateExpirationDate(RedisManager redisManager, String enclosureId) throws Exception {
        LocalDate expirationDate = DateUtils.convertStringToLocalDate(RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.EXPIRED_TIMESTAMP.getKey()));
        if (LocalDate.now().isAfter(expirationDate)) {
            LOGGER.error("vous ne pouvez plus telecharger ces fichiers");
            throw new DownloadException("vous ne pouvez plus telecharger ces fichiers");
        }
        return expirationDate;
    }

    private int validateNumberOfDownload(RedisManager redisManager, String recipientId) throws Exception {
        int numberOfDownload = RedisUtils.getNumberOfDownloadsPerRecipient(redisManager, recipientId);
        if (maxDownload < numberOfDownload) {
            LOGGER.error("vous avez atteint le nombre maximum de telechargement");
            throw new DownloadException("vous avez atteint le nombre maximum de telechargement");
        }
        return numberOfDownload;
    }

    private void validateRecipientId(RedisManager redisManager, String enclosureId, String recipientMail, String recipientId) throws Exception {
        String recipientIdRedis = RedisUtils.getRecipientId(redisManager, enclosureId, recipientMail);
        if (!recipientIdRedis.equals(recipientId)) {
            LOGGER.error("accès interdit");
            throw new DownloadException("accès interdit");
        }
    }

    private void validatePassword(RedisManager redisManager, String enclosureId, String password) throws Exception {
        String passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.PASSWORD.getKey());
        String passwordHashed = "";
        if (!StringUtils.isEmpty(password)) {
            passwordHashed = passwordHasherServices.calculatePasswordHashed(password);
        }
        if (!(password != null && passwordRedis != null && passwordHashed.equals(passwordRedis))) {
            LOGGER.error("accès interdit");
            throw new UnauthorizedAccessException("accès interdit");
        }
    }

    private void downloadProgress(RedisManager redisManager, String enclosureId, String recipientId) throws Exception {
        // increment nb_download for this recipient
        RedisUtils.incrementNumberOfDownloadsForRecipient(redisManager, recipientId);
        // add to queue Redis download progress: to send download mail in progress to the sender
        String downloadQueueValue =  enclosureId + ":" + recipientId;
        redisManager.rpush(RedisQueueEnum.DOWNLOAD_QUEUE.getValue(), downloadQueueValue);
    }

    private List<FileRepresentation> getRootFiles(RedisManager redisManager, String enclosureId) {
        List<FileRepresentation> rootFiles = new ArrayList<>();
        List<DirectoryRepresentation> rootDirs = new ArrayList<>();
        redisManager.lrange(RedisKeysEnum.FT_ROOT_FILES.getKey(enclosureId),0,-1).forEach(rootFileName-> {
            String size = "";
            String hashRootFile = RedisUtils.generateHashsha1(enclosureId + ":" + rootFileName);
            try {
                size = redisManager.getHgetString(
                        RedisKeysEnum.FT_ROOT_FILE.getKey(hashRootFile),
                        RootFileKeysEnum.SIZE.getKey()
                );
            } catch (Exception e) {
                LOGGER.error("download errors");
                throw new DownloadException("download errors");
            }
            FileRepresentation rootFile = new FileRepresentation();
            rootFile.setName(rootFileName);
            rootFile.setSize(Integer.valueOf(size));
            rootFiles.add(rootFile);
            LOGGER.debug("root file: {}",rootFileName);
        });
        return rootFiles;
    }

    private List<DirectoryRepresentation> getRootDirs(RedisManager redisManager, String enclosureId) {
        List<DirectoryRepresentation> rootDirs = new ArrayList<>();
        redisManager.lrange(RedisKeysEnum.FT_ROOT_DIRS.getKey(enclosureId),0,-1).forEach(rootDirName-> {
            String size = "";
            String hashRootDir = RedisUtils.generateHashsha1(enclosureId + ":" + rootDirName);
            try {
                size = redisManager.getHgetString(
                        RedisKeysEnum.FT_ROOT_DIR.getKey(hashRootDir),
                        RootDirKeysEnum.TOTAL_SIZE.getKey()
                );
            } catch (Exception e) {
                LOGGER.error("download errors");
                throw new DownloadException("download errors");
            }
            DirectoryRepresentation rootDir = new DirectoryRepresentation();
            rootDir.setName(rootDirName);
            rootDir.setTotalSize(Integer.valueOf(size));
            rootDirs.add(rootDir);
            LOGGER.debug("root Dir: {}",rootDirName);
        });
        return rootDirs;
    }
}
