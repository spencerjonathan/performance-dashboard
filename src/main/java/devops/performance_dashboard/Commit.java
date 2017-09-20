package devops.performance_dashboard;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.jgit.revwalk.RevCommit;

public class Commit {
	
	static long day = 60 * 60 * 24;
	
	private String author;
	private int team;
	private Date commitTime;
	private String reference;

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Date getCommitTime() {
		return commitTime;
	}

	public void setCommitTime(Date commitTime) {
		this.commitTime = commitTime;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public Commit(RevCommit commit, Config config) {
		this.author = commit.getAuthorIdent().getName();
		
		this.commitTime = new Date(new Long(commit.getCommitTime()) * new Long(1000));
		
		// Round to date
		this.commitTime = DateUtils.truncate(commitTime, Calendar.DAY_OF_MONTH);
		//this.commitTime = new Date(new Long(commit.getCommitTime()/day) * new Long(1000) * day);
		this.reference = commit.getName();
		this.team = config.team(author);
	}

	public int getTeam() {
		return team;
	}

	public void setTeam(int team) {
		this.team = team;
	}

}
