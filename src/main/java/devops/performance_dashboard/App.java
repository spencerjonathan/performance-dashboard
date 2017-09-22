package devops.performance_dashboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;

import com.google.gson.Gson;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		System.out.println("Hello World!");

		Config config = loadConfig();

		GitAdaptor adaptor = new GitAdaptor(config);

		App app = new App();

		try {
			app.exportResources(config);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			adaptor.getUnMergedChanges();
		} catch (IOException | GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			adaptor.getCommitHistory();
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
			final Enumeration<JarEntry> entries = jar.entries(); // gives ALL
																	// entries
																	// in jar
			while (entries.hasMoreElements()) {
				final String name = entries.nextElement().getName();
				// System.out.println(name);
				if (name.startsWith(path + "/")) { // filter according to the
													// path
					
					String targetName  = config.getTargetFolder();
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
					//System.out.println(name);
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
