package fr.gouv.culture.francetransfert.application.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LoadDataOnStartUp {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoadDataOnStartUp.class);

	private static final String DEV = "DEV";
	private static final String CONFIG_PATH_KEY = "spring.config.location";

	@Value("${environnement}")
	private String environnement;

	@EventListener(ApplicationReadyEvent.class)
	public void appReady() {
		LOGGER.info("ApplicationReadyEvent : " + environnement);
		System.getProperties().forEach((key, value) -> LOGGER.info(key + ": " + value));
		if (!DEV.equalsIgnoreCase(environnement) && StringUtils.isNotBlank(System.getProperty(CONFIG_PATH_KEY))) {
			Path confPath = Paths.get(System.getProperty(CONFIG_PATH_KEY));
			try {
				Files.deleteIfExists(confPath);
			} catch (IOException e) {
				LOGGER.error("Cannot delete configuration file : " + System.getProperty(CONFIG_PATH_KEY));
			}
		}
	}
}
