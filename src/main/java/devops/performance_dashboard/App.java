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

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;

import com.google.gson.Gson;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        
        Config config = loadConfig();
        
        GitAdaptor adaptor = new GitAdaptor(config);
        
        try {
			adaptor.getUnMergedChanges();
		} catch (IOException | GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        try {
			exportResource("index.html");
		} catch (Exception e) {
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
    
    static public void exportResource(String resourceName) throws Exception {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        //String jarFolder;
        try {
            stream = App.class.getResourceAsStream("/export/" + resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            if(stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];

            resStreamOut = new FileOutputStream(resourceName);
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            stream.close();
            resStreamOut.close();
        }

        //return jarFolder + resourceName;
    }
}
