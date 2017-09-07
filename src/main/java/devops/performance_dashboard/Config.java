package devops.performance_dashboard;

public class Config {
	private String username;
	private String password;
	private String uri;
	private String localRepository;
	
	public Config(String username, String password, String uri, String localRepository) {
		this.username = username;
		this.password = password;
		this.uri = uri;
		this.localRepository = localRepository;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getLocalRepository() {
		return localRepository;
	}

	public void setLocalRepository(String local_repository) {
		this.localRepository = local_repository;
	}
	
}
