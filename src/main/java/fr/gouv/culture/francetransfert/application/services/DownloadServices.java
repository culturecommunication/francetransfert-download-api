package fr.gouv.culture.francetransfert.application.services;

import com.opengroup.mc.francetransfert.api.francetransfert_metaload_api.RedisManager;
import com.opengroup.mc.francetransfert.api.francetransfert_metaload_api.enums.*;
import com.opengroup.mc.francetransfert.api.francetransfert_metaload_api.utils.DateUtils;
import com.opengroup.mc.francetransfert.api.francetransfert_metaload_api.utils.RedisUtils;
import com.opengroup.mc.francetransfert.api.francetransfert_storage_api.StorageManager;
import fr.gouv.culture.francetransfert.application.error.UnauthorizedAccessException;
import fr.gouv.culture.francetransfert.application.resources.model.DirectoryRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.DownloadRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.FileRepresentation;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DownloadServices {
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadServices.class);

    @Value("${enclosure.max.download}")
    private int maxDownload;

    @Value("${enclosure.expire.days}")
    private int expiredays;


    public DownloadRepresentation processDownload(String mailRecipient, String enclosureId, String password) throws Exception {
        RedisManager redisManager = RedisManager.getInstance();
        String passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.PASSWORD.getKey());
        if (!(password != null && passwordRedis != null && password.equals(passwordRedis))) {
            throw new UnauthorizedAccessException("accès interdit");
        }
        return getDownloadInfo(redisManager, mailRecipient, enclosureId, true);
    }

    public DownloadRepresentation downloadAuthority(String mailRecipient, String enclosureId, boolean withPassword) throws Exception {
        RedisManager redisManager = RedisManager.getInstance();
        DownloadRepresentation downloadRepresentation = null;
        if (withPassword) {
            downloadRepresentation = processDownloadWithoutUrl(redisManager, mailRecipient, enclosureId);
        } else {
            downloadRepresentation = processDownloadWithUrl(redisManager, mailRecipient, enclosureId);
        }
        return downloadRepresentation;
    }

    private DownloadRepresentation processDownloadWithoutUrl(RedisManager redisManager, String mailRecipient, String enclosureId) throws Exception {
        return getDownloadInfo(redisManager, mailRecipient, enclosureId, false);
    }

    private DownloadRepresentation processDownloadWithUrl(RedisManager redisManager, String mailRecipient, String enclosureId) throws Exception {
        String passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.PASSWORD.getKey());
        if (!StringUtils.isEmpty(passwordRedis)) {
            throw new UnauthorizedAccessException("accès interdit");
        }
        return getDownloadInfo(redisManager, mailRecipient, enclosureId, true);
    }

    private DownloadRepresentation getDownloadInfo(RedisManager redisManager, String mailRecipient, String enclosureId, boolean withUrl) throws Exception {
        int numberOfDownload = RedisUtils.getNumberOfDownloadsPerRecipient(redisManager, mailRecipient, enclosureId);
        if (maxDownload < numberOfDownload) {
            throw new DownloadException("vous avez atteint le nombre maximum de telechargement");
        }
        LocalDate expirationDate = DateUtils.convertStringToLocalDate(RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.TIMESTAMP.getKey())).plusDays(expiredays);
        if (LocalDate.now().isAfter(expirationDate)) {
            throw new DownloadException("vous ne pouvez plus telecharger ces fichiers");
        }
        String senderMail = RedisUtils.getSenderEnclosure(redisManager, enclosureId);
        String downloadUrl = withUrl ? getDownloadUrl(redisManager, enclosureId) : null;
        List<FileRepresentation> rootFiles = getRootFiles(redisManager, enclosureId);
        List<DirectoryRepresentation> rootDirs = getRootDirs(redisManager, enclosureId);

        return DownloadRepresentation.builder()
                .nbPrevDownloads(numberOfDownload)
                .validUntilDate(expirationDate)
                .senderEmail(senderMail)
                .rootFiles(rootFiles)
                .rootDirs(rootDirs)
                .downloadURL(downloadUrl)
                .build();
    }

    private String getDownloadUrl(RedisManager redisManager, String enclosureId) throws Exception {
        StorageManager storageManager = new StorageManager();
        String bucketName = RedisUtils.getBucketName(redisManager, enclosureId);
        String fileToDownload = enclosureId+".zip";
        return storageManager.generateDownloadURL(bucketName, fileToDownload).toString();
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
                throw new DownloadException("erreur de telechargement.");
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
                throw new DownloadException("erreur de telechargement.");
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
