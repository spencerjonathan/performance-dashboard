package devops.performance_dashboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.eclipse.jgit.api.errors.GitAPIException;

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
			adaptor.cloneRepository();
		} catch (IOException | GitAPIException e) {
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
}
