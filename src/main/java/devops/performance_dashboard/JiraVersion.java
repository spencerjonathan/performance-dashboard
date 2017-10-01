package devops.performance_dashboard;

import java.util.Date;

import com.google.gson.Gson;

public class JiraVersion implements Comparable<JiraVersion> {
	
	private Date releaseDate;
	private String name;
	
	public JiraVersion(Date releaseDate, String name) {
	
		this.releaseDate = releaseDate;
		this.name = name;
		
	}
	
	@Override
	public int hashCode() {
		return name.concat(releaseDate.toString()).hashCode();
	}

	@Override
	public boolean equals(Object o) {
		// self check
		if (this == o)
			return true;
		// null check
		if (o == null)
			return false;
		// type check and cast
		if (getClass() != o.getClass())
			return false;

		JiraVersion other = (JiraVersion) o;

		return name.equals(other.name) && releaseDate.equals(other.releaseDate);
	}
	
	@Override
	public String toString() {
		Gson gson = new Gson();
		
		return gson.toJson(this);
	}

	public Date getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(Date releaseDate) {
		this.releaseDate = releaseDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int compareTo(JiraVersion other) {
		return releaseDate.compareTo(other.releaseDate);
	}
	
	
	

}
