package devops.performance_dashboard;

import java.util.List;

public class Config {
	private String username;
	private String password;
	private String uri;
	private String localRepository;
	private boolean performPull;
	private List<String> branchExcludes;
	private List<String> pathExcludes;
	
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

	public boolean getPerformPull() {
		return performPull;
	}

	public void setPerformPull(boolean performPull) {
		this.performPull = performPull;
	}

	public List<String> getBranchExcludes() {
		return branchExcludes;
	}

	public void setBranchExcludes(List<String> branchExcludes) {
		this.branchExcludes = branchExcludes;
	}

	public List<String> getPathExcludes() {
		// TODO Auto-generated method stub
		return this.pathExcludes;
	}

	public void setPathExcludes(List<String> pathExcludes) {
		this.pathExcludes = pathExcludes;
	}
	
}
