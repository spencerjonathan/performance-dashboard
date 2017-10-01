package devops.performance_dashboard;

import java.util.List;
import java.util.Map;

public class Config {
	private String username;
	private String password;
	private String uri;
	private String localRepository;
	private String releaseBranchPattern;
	private boolean performPull;
	private int historyDays;
	private List<String> branchExcludes;
	private List<String> pathExcludes;
	private Map<String, Integer> authorToTeam;
	private String targetFolder;
	private String jiraUri;
	private String jiraProject;
	
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

	public Map<String, Integer> getAuthorToTeam() {
		return authorToTeam;
	}
	
	public Integer team(String author) {
		if (authorToTeam.get(author) != null)
			return authorToTeam.get(author);
		return -1;
	}

	public void setAuthorToTeam(Map<String, Integer> authorToTeam) {
		this.authorToTeam = authorToTeam;
	}

	public int getHistoryDays() {
		return historyDays;
	}

	public void setHistoryDays(int historyDays) {
		this.historyDays = historyDays;
	}

	public String getTargetFolder() {
		return targetFolder;
	}

	public void setTargetFolder(String targetFolder) {
		this.targetFolder = targetFolder;
	}

	public String getJiraUri() {
		return jiraUri;
	}

	public void setJiraUri(String jiraUri) {
		this.jiraUri = jiraUri;
	}

	public String getJiraProject() {
		return jiraProject;
	}

	public void setJiraProject(String jiraProject) {
		this.jiraProject = jiraProject;
	}

	public String getReleaseBranchPattern() {
		return releaseBranchPattern;
	}

	public void setReleaseBranchPattern(String releaseBranchPattern) {
		this.releaseBranchPattern = releaseBranchPattern;
	}
	
}
