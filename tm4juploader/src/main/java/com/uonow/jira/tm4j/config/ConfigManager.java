package com.uonow.jira.tm4j.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigManager {
	private static final String CONFIG_FILE_NAME = "tm4j-upload.properties";
	private Properties properties;
	private File configFile;
	private String defaultURL = "http://localhost:8080/rest/atm/1.0";
	private static final String CYCLE_KEY = "test.cycle";
	private static final String PROJECT_KEY = "project.key";
	private static final String JIRA_URL_KEY = "jira.base.url";

	public ConfigManager() {
		properties = new Properties();
		configFile = new File(CONFIG_FILE_NAME);

		// Create default config if not exists
		if (!configFile.exists()) {
			createDefaultConfig();
		}

		// Load existing configuration
		loadConfig();
	}

	private void createDefaultConfig() {
		saveConfig("", defaultURL, "");
	}

	private void loadConfig() {
		try (FileInputStream fis = new FileInputStream(configFile)) {
			properties.load(fis);
		} catch (IOException e) {
			log.error("read config file have errors:{}", e.getMessage());
		}
	}

	public void saveConfig(String cycle, String baseUrl, String project) {
		properties.setProperty(CYCLE_KEY, cycle);
		properties.setProperty(PROJECT_KEY, project);
		properties.setProperty(JIRA_URL_KEY, baseUrl);
		try (FileOutputStream fos = new FileOutputStream(configFile)) {
			properties.store(fos, "TM4J Upload Utility Configuration");
		} catch (IOException e) {
			log.error("save config file have errors:{}", e.getMessage());
		}
	}

	public String getDefaultTestCycle() {
		return properties.getProperty(CYCLE_KEY, "");
	}

	public String getJiraBaseUrl() {
		return properties.getProperty(JIRA_URL_KEY, defaultURL);
	}

	public String getProjectKey() {
		return properties.getProperty(PROJECT_KEY, "");
	}

	public String getUserName() {
		return properties.getProperty("username", "");
	}

}