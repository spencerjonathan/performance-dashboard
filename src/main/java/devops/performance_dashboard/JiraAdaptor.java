package devops.performance_dashboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class JiraAdaptor {
	
	//private Config config;
	private final String versionsLocation;

	public JiraAdaptor(Config config) {
		System.out.println("Constructing JiraAdaptor");
		//this.config = config;
		
		this.versionsLocation = config.getJiraUri() + "/rest/api/2/project/" + config.getJiraProject() + "/versions";
		
		// Setup username and password authentication
		Authenticator.setDefault (new Authenticator() {
		    protected PasswordAuthentication getPasswordAuthentication() {
		        return new PasswordAuthentication (config.getUsername(), config.getPassword().toCharArray());
		    }
		});
	}
	
	public List<JiraVersion> getProjectVersions() throws IOException {
		System.out.println("Requesting release versions from JIRA from: " + versionsLocation);
		URL url = new URL(versionsLocation);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		
		conn.setDoOutput(true);
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
					+ conn.getResponseCode());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(
			(conn.getInputStream())));

		String output;
		String response = "";
		System.out.println("Output from JIRA .... \n");
		while ((output = br.readLine()) != null) {
			System.out.println(output);
			response = response.concat(output);
		}
		conn.disconnect();
		
		
		Gson gson = new Gson();
		java.lang.reflect.Type type = new TypeToken<List<JiraVersion>>() {
		}.getType();
		List<JiraVersion> returnValue = gson.fromJson(response, type);

		return returnValue;
		
	}

}
