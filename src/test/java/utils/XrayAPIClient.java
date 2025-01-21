package utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class XrayAPIClient {
    private static final String XRAY_BASE_URL = "https://somfycucumber.atlassian.net/rest/raven/2.0/import/execution/cucumber";
    private static final String JIRA_EMAIL = System.getenv("JIRA_EMAIL");
    private static final String JIRA_API_TOKEN = System.getenv("JIRA_API_TOKEN");

    public static void uploadResults() {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost uploadFile = new HttpPost(XRAY_BASE_URL);

            // Jira authentication
            String auth = JIRA_EMAIL + ":" + JIRA_API_TOKEN;
            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            uploadFile.setHeader("Authorization", "Basic " + encodedAuth);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody(
                "file",
                new File("target/cucumber-reports/cucumber.json")
            );

            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);

            CloseableHttpResponse response = httpClient.execute(uploadFile);
            System.out.println("Xray upload response: " + response.getStatusLine());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 