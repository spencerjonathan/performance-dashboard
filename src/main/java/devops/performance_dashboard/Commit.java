package devops.performance_dashboard;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.jgit.revwalk.RevCommit;

import com.google.gson.Gson;

public class Commit {

	static long day = 60 * 60 * 24;

	private String author;
	private int team;
	private Date commitTime;
	private String reference;

	@Override
	public int hashCode() {
		return reference.hashCode();
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

		Commit other = (Commit) o;

		// field comparison - for the purpose of this class it is sufficient to
		// check that the references are equal
		return reference.equals(other.reference);
	}

	@Override
	public String toString() {
		Gson gson = new Gson();

		return gson.toJson(this);
	}

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
		// this.commitTime = new Date(new Long(commit.getCommitTime()/day) * new
		// Long(1000) * day);
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
