package com.uonow.jira.tm4j;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import com.alibaba.fastjson2.JSON;
import com.formdev.flatlaf.FlatLightLaf;
import com.uonow.jira.tm4j.config.ConfigManager;
import com.uonow.jira.tm4j.pojo.CaseRequest;
import com.uonow.jira.tm4j.pojo.CaseResp;

import kong.unirest.core.Unirest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TM4JUploadApp extends JFrame {

	private static final long serialVersionUID = 2223305836612635256L;
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JTextField testCycleField;
	private JTextField projectKeyField;

	private JButton fileChooseButton;
	private JButton submitButton;
	private JLabel statusLabel;
	private JProgressBar uploadProgressBar;
	private static final String RESUTL_URL = "%s/testrun/%s/testcase/%s/testresult";
	private static final String RUN_URL = "%s/testrun/%s/testcase/%s/testresult";

	// private static final String CASE_ATTACHMETS_URL =
	// "%s/testcase/%s/attachments";
	private static final String EC_ATTACHMETS_URL = "%s/testresult/%s/attachments";
	private static final String CYCLE_URL = "%s/testrun/%s/";
	private ExecutorService executorService;
	private ConfigManager configManager;

	private String tm4jBaseUrl;
	private File choosedFile;

	public TM4JUploadApp() {
		log.info("start wabi tm4j loader");
		configManager = new ConfigManager();
		tm4jBaseUrl = configManager.getJiraBaseUrl();

		// Setup FlatLaf Look and Feel
		try {
			UIManager.setLookAndFeel(new FlatLightLaf());
		} catch (Exception e) {
			e.printStackTrace();
		}

		setTitle("WABI TM4J Upload Utility v1.0");
		setSize(450, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new GridBagLayout());

		// Initialize executor service for background uploads
		executorService = Executors.newSingleThreadExecutor();

		initComponents();
		setupLayout();
		// Center the window
		centerWindow();

		log.info("started, waiting uploading");
	}

	private void centerWindow() {
		// Get the screen size
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		// Calculate the center point
		int centerX = (screenSize.width - getWidth()) / 2;
		int centerY = (screenSize.height - getHeight()) / 2;

		// Set the location
		setLocation(centerX, centerY);
	}

	private void initComponents() {
		// Username field
		usernameField = new JTextField(20);
		usernameField.setToolTipText("Jira Username");

		// Password field
		passwordField = new JPasswordField(20);
		passwordField.setToolTipText("Jira Password");

		// Test Cycle field
		testCycleField = new JTextField(20);
		testCycleField.setToolTipText("Test Cycle ID");
		if (configManager.getDefaultTestCycle() != null && !"".equals(configManager.getDefaultTestCycle().trim())) {
			testCycleField.setText(configManager.getDefaultTestCycle().trim());
		}
		// Upload type dropdown
//		uploadTypeComboBox = new JComboBox<>(
//				new String[] { "Select Upload Type", "WABI Result JSON", "Case Attachments", "Execution Attachments" });
		projectKeyField = new JTextField(20);
		projectKeyField.setToolTipText("Project Key");

		if (configManager.getProjectKey() != null && !"".equals(configManager.getProjectKey().trim())) {
			projectKeyField.setText(configManager.getProjectKey().trim());
		}

		// File choose button
		fileChooseButton = new JButton("Choose File");

		// Submit button
		submitButton = new JButton("Submit Upload");

		// Status label
		statusLabel = new JLabel("Ready to upload");

		// Progress bar
		uploadProgressBar = new JProgressBar(0, 100);
		uploadProgressBar.setForeground(new Color(0, 180, 0)); // Bright green color

		uploadProgressBar.setStringPainted(true);
		uploadProgressBar.setVisible(false);

		// Submit button listener
		submitButton.addActionListener(e -> upload());

		// Upload type selection listener
//		uploadTypeComboBox.addActionListener(e -> {
//			fileChooseButton.setEnabled(uploadTypeComboBox.getSelectedIndex() > 0);
//		});

		// File choose button listener
		fileChooseButton.addActionListener(e -> {
			JFileChooser fileChooser = new JFileChooser();
			int result = fileChooser.showOpenDialog(this);

			if (result == JFileChooser.APPROVE_OPTION) {
				statusLabel.setText("File selected: " + fileChooser.getSelectedFile().getName());
				choosedFile = fileChooser.getSelectedFile();
			}
		});
	}

	private void setupLayout() {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.gridx = 0;
		gbc.gridy = 0;
		add(new JLabel("Username:"), gbc);

		gbc.gridx = 1;
		add(usernameField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		add(new JLabel("Password:"), gbc);

		gbc.gridx = 1;
		add(passwordField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		add(new JLabel("Project Key:"), gbc);

		gbc.gridx = 1;
		add(projectKeyField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		add(new JLabel("Test Cycle:"), gbc);

		gbc.gridx = 1;
		add(testCycleField, gbc);

		gbc.gridy = 4;
		add(fileChooseButton, gbc);

		gbc.gridy = 5;
		add(submitButton, gbc);

		gbc.gridy = 6;
		add(statusLabel, gbc);

		gbc.gridy = 7;
		add(uploadProgressBar, gbc);
	}

	private void upload() {
		// Validate inputs
		if (!validateInputs())
			return;

		// Reset and show progress bar
		uploadProgressBar.setValue(0);
		uploadProgressBar.setVisible(true);

		// Perform upload in background
		executorService.submit(() -> {
			try {
				uploadZipOrOtherFiles(choosedFile);
			} catch (Exception e) {
				updateProgressAndStatus(0, "Upload Failed: " + e.getMessage());
			}
		});
	}

	private boolean validateInputs() {
		if (usernameField.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please enter username");
			return false;
		}
		if (new String(passwordField.getPassword()).trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please enter password");
			return false;
		}
		if (testCycleField.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please enter test cycle ID");
			return false;
		}
		if (projectKeyField.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please enter Project Key()");
			return false;
		}
		return true;
	}

	/**
	 * zip:<br/>
	 * 1.json+pdf/other doc as attachment or <br/>
	 * 2. manually test csv/xls+pdf/other doc as attachment
	 *
	 * json:
	 * 
	 * pdf:
	 * 
	 * @param upFile
	 * @throws Exception
	 */
	private void uploadZipOrOtherFiles(File upFile) throws Exception {
		// juddge file type ,most time it will be zip , but may also json or csv
		// if it is zip, then check it is many json in side or also with pdf
		// if have pdf, upload to execution attachments

		updateProgressAndStatus(50, "Processing:" + upFile.getName());

		// Send to TM4J test run endpoint

		// process json and update one by one

		if (upFile.getName().toLowerCase().endsWith(".zip")) {

			uploadAttachments(upFile);
		}

	}

	private void uploadAttachments(File zipFile) throws Exception {

		try (ZipFile zip = new ZipFile(zipFile)) {

			Map<String, List<ZipEntry>> caseAttachmentMap = new HashMap<>();
			// Get PDF entries
			List<ZipEntry> jsonEntries = new ArrayList<>();
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String fileNameString = entry.getName().toLowerCase();
				String caseId = entry.getName().replaceAll(".*/|.*\\\\|\\.[^.]+$", "");
				log.info("caseid:{}", caseId);
				if (!entry.isDirectory() && fileNameString.endsWith(".json")) {

					jsonEntries.add(entry);
				} else {
					// attachements
					List<ZipEntry> otherEntries = caseAttachmentMap.get(caseId);
					if (otherEntries == null) {
						otherEntries = new ArrayList<>();
					}
					otherEntries.add(entry);
					caseAttachmentMap.put(caseId, otherEntries);

				}
			}

			String testCycleId = testCycleField.getText().trim();
			String projectKey = new String(projectKeyField.getText().trim());

			// create cycal if not existing

			// process json and upload pdf under each case
			processOneTestCase(zip, caseAttachmentMap, jsonEntries, testCycleId, projectKey);

			// Final update
			updateProgressAndStatus(100, "All Uploaded Successfully");
		}
	}

	public void processOneTestCase(ZipFile zip, Map<String, List<ZipEntry>> caseAttachmentMap,
			List<ZipEntry> jsonEntries, String testCycleId, String projectKey) throws Exception {

		String username = usernameField.getText().trim();
		String password = new String(passwordField.getPassword());

		// Upload PDFs with progress tracking
		int totalFiles = jsonEntries.size();

		for (int i = 0; i < totalFiles; i++) {
			ZipEntry entry = jsonEntries.get(i);

			// Update progress
			int progress = (int) (((double) (i + 1) / totalFiles) * 100);
			updateProgressAndStatus(progress, "Uploading  " + (i + 1) + " of " + totalFiles);

			// Upload individual PDF
			try (InputStream is = zip.getInputStream(entry)) {

				// process json
				String caseId = entry.getName().replaceAll(".json", "");
				log.info(caseId);

				String endpoint = String.format(RESUTL_URL, tm4jBaseUrl, testCycleId, caseId);
				log.info(endpoint);
				CaseRequest request = new CaseRequest();
				request.setComment("updated by WABI Robot,executed by user:" + username);
				request.setStatus("Pass");
				// request.setProjectKey(projectKey);
				// request.setTestCaseKey(caseId)

				String runIdString = createOneCaseExecResult(endpoint, request, username, password);
				log.info("run id {}", runIdString);
				List<ZipEntry> aEntries = caseAttachmentMap.get(caseId);
				if (aEntries != null) {
					log.info("also upload attachments for case {}", caseId);
					uploadAttachments(runIdString, username, password, aEntries, zip);
				}

				updateProgressAndStatus(100, "Upload Complete for case: " + caseId);

			}
		}

	}

	private void uploadAttachments(String eid, String username, String password, List<ZipEntry> aEntries, ZipFile zip)
			throws Exception {

		// String eId = entry.getName().replaceAll(".*-E(\\d+)\\.pdf", "$1");
		String endpoint = String.format(EC_ATTACHMETS_URL, tm4jBaseUrl, eid);
		log.info("process Execution Id:[{}]", eid);

		for (ZipEntry entry : aEntries) {
			try (InputStream is = zip.getInputStream(entry)) {
				log.info("add attachement [{}] on test case execution: [{}]", entry.getName(), eid);
				uploadAttachmentToEndPoint(endpoint, is, entry.getName(), username, password);
			}
		}
	}

	private void uploadAttachmentToEndPoint(String endpoint, InputStream attachment, String fileName, String username,
			String pwd) throws Exception {

		log.info("upload file {} to {}", fileName, endpoint);
		// Prepare multipart request
		Unirest.post(endpoint).basicAuth(username, pwd).field("file", attachment, fileName)
				.uploadMonitor((field, fName, bytesWritten, totalBytes) -> {
					// updateProgressBarWithBytesLeft(totalBytes - bytesWritten);
					log.info("total: {},wrtie:{}", totalBytes, bytesWritten);
					if (totalBytes > 0) {
						int progressValue = (int) ((bytesWritten * 100L) / totalBytes);
						updateProgressAndStatus(Math.min(progressValue, 100), "Uploaded attachment: " + fileName);
					}

				}).asString().ifSuccess(response -> {
					log.info("result {}", response.getBody());
				}).ifFailure(response -> {
					log.error("Oh No! Status {},{}", response.getStatus(), response.getBody());
					updateProgressAndStatus(100, response.getBody());
					response.getParsingError().ifPresent(e -> {
						log.error("Parsing Exception: ", e);
						log.error("Original body: " + e.getOriginalBody());
					});
				});

	}

	public String postJson(String url, String json, String user, String pwd) {

		// Create HttpClient
		HttpClient client = HttpClient.newHttpClient();

		String result = "";
		// Encode credentials
		String auth = Base64.getEncoder().encodeToString((user + ":" + pwd).getBytes());

		// Create HttpRequest
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)) // Target URL
				.header("Content-Type", "application/json") // Set headers
				.header("Authorization", "Basic " + auth).POST(HttpRequest.BodyPublishers.ofString(json)) // Attach JSON
																											// payload
				.build();

		try {
			// Send request and get response
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			// Print response details
			log.info("Status Code: " + response.statusCode());
			log.info("Response Body: " + response.body());
			
			CaseResp resp=JSON.parseObject(response.body(),CaseResp.class);
			result=""+resp.getId();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;

	}

	private String createOneCaseExecResult(String endpoint, CaseRequest request, String user, String pwd) {
		log.info("Send request to [{}], with body [{}],user [{}]", endpoint, request, user);
		String id = "7";
		try {

			id=postJson(endpoint, JSON.toJSONString(request), user, pwd);

			// Unirest.config().setDefaultBasicAuth(user, pwd);
			//kong.unirest.core.HttpResponse<String> idString = Unirest.post(endpoint).basicAuth(user, pwd).body(request).asString();

			log.info(id);
		} catch (Exception e) {
			log.error("error {}", e);
		}
//		HttpResponse<CaseResp> resp = Unirest.post(endpoint).basicAuth(user, pwd).body(request)
//				.asObject(CaseResp.class)
//				.ifFailure(response -> {
//					log.error("Oh No! Status {},{}", response.getStatus(), response.getBody());
//					updateProgressAndStatus(100, JSON.toJSONString(response.getBody()));
//					response.getParsingError().ifPresent(e -> {
//						log.error("Parsing Exception: ", e);
//						log.error("Original body: " + e.getOriginalBody());
//					});
//				});
//		CaseResp resp2 = resp.getBody();
//		if (resp2 != null) {
//			idString = resp2.getId();
//		}
		return id;
	}

//	private String extractCaseId(Integer type, String filename) {
//
//		if (type == 3) {
//			return filename.replaceAll(".*-E(\\d+)\\.pdf", "$1");
//		}
//		// Extract numeric case ID from filename
//		return filename.replaceAll(".pdf", "");
//	}

	private void updateProgressAndStatus(int progressValue, String message) {
		SwingUtilities.invokeLater(() -> {
			uploadProgressBar.setValue(progressValue);
			statusLabel.setText(message);

			// Hide progress bar when complete
			if (progressValue == 100) {
				Timer timer = new Timer(2000, e -> {
					uploadProgressBar.setVisible(false);
				});
				timer.setRepeats(false);
				timer.start();
			}
		});
	}

	@Override
	public void dispose() {
		// Shutdown executor service when closing
		if (executorService != null) {
			executorService.shutdown();
		}
		Unirest.shutDown();
		super.dispose();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			new TM4JUploadApp().setVisible(true);
		});
	}
}