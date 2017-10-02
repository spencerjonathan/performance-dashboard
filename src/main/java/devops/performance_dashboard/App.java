package devops.performance_dashboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		System.out.println("Hello World!");

		Config config = loadConfig();

		GitAdaptor gitAdaptor = null;
		try {
			gitAdaptor = new GitAdaptor(config);
			gitAdaptor.updateRepository();
		} catch (GitAPIException | IOException e2) {
			e2.printStackTrace();
			System.exit(-1);
		}

		JiraAdaptor jiraAdaptor = new JiraAdaptor(config);

		App app = new App();

		try {
			app.exportResources(config);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<JiraVersion> jiraVersions = null;

		try {
			jiraVersions = jiraAdaptor.getProjectVersions();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Map<Commit, JiraVersion> releaseCommits = null;
		try {
			releaseCommits = gitAdaptor.getCommitsByVersion(jiraVersions);
		} catch (GitAPIException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (MissingObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IncorrectObjectTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Map<JiraVersion, List<Commit>> commitsByVersion = new HashMap<JiraVersion, List<Commit>>();

		for (Commit c : releaseCommits.keySet()) {
			System.out.println("Commit: " + c + " went live in " + releaseCommits.get(c));

			if (!commitsByVersion.containsKey(releaseCommits.get(c))) {
				commitsByVersion.put(releaseCommits.get(c), new ArrayList<Commit>());
			}

			commitsByVersion.get(releaseCommits.get(c)).add(c);

		}

		List<CycleTimeRecord> cycleTimeRecords = new ArrayList<CycleTimeRecord>();

		for (JiraVersion jv : commitsByVersion.keySet()) {
			CycleTimeRecord ctr = new CycleTimeRecord(jv, commitsByVersion.get(jv));
			cycleTimeRecords.add(ctr);
		}
		
		Collections.sort(cycleTimeRecords);

		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
		// Write git.js file
		File output_file = new File(config.getTargetFolder() + "cycle_times.js");
		FileWriter writer = null;
		try {
			writer = new FileWriter(output_file);

			writer.write("var cycleTimesData = ");
			gson.toJson(cycleTimeRecords, writer);
			writer.write(";\n");
			
			writer.write("var commitLookupURI = \"" + config.getCommitLookupUri() + "\";");

			writer.close();

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			gitAdaptor.getUnMergedChanges();
		} catch (IOException | GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			gitAdaptor.getCommitHistory();
		} catch (NoHeadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static Config loadConfig() {
		File config_file = new File("./config.json");

		if (!config_file.exists()) {
			System.out.println("Config file does not exist");
			System.exit(-1);
		}

		Reader config_reader = null;
		try {
			config_reader = new BufferedReader(new FileReader(config_file));
		} catch (FileNotFoundException e) {

			e.printStackTrace();
			System.out.println("Error reading Config file");
			System.exit(-1);
		}

		Gson gson = new Gson();
		Config config = gson.fromJson(config_reader, Config.class);

		try {
			config_reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return config;
	}

	public void exportResources(Config config) throws Exception {
		final String path = "export";
		final File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

		if (jarFile.isFile()) { // Run with JAR file
			final JarFile jar = new JarFile(jarFile);

			// gives ALL entries in jar
			final Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				final String name = entries.nextElement().getName();
				// System.out.println(name);
				if (name.startsWith(path + "/")) { // filter according to the
													// path

					String targetName = config.getTargetFolder();
					targetName = targetName.concat(name.replaceAll("^export\\/", ""));
					System.out.println("Creating " + targetName);

					File file = new File(targetName);
					if (targetName.endsWith("/")) {
						if (!file.exists()) {

							file.mkdir();
						}
					} else {
						exportResource(name, targetName);
					}

				}
			}
			jar.close();
		} else {
			System.out.println("Not running in a JAR!");
		}
	}

	public void exportResource(String resourceName, String target) throws Exception {
		InputStream stream = null;
		OutputStream resStreamOut = null;

		System.out.println("Copying " + resourceName + " to " + target);

		// String jarFolder;
		try {
			stream = App.class.getResourceAsStream("/" + resourceName);
			if (stream == null) {
				throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
			}

			int readBytes;
			byte[] buffer = new byte[4096];

			resStreamOut = new FileOutputStream(target);
			while ((readBytes = stream.read(buffer)) > 0) {
				resStreamOut.write(buffer, 0, readBytes);
			}
		} catch (Exception ex) {
			throw ex;
		} finally {
			if (stream != null)
				stream.close();

			if (resStreamOut != null)
				resStreamOut.close();
		}

		// return jarFolder + resourceName;
	}
}
