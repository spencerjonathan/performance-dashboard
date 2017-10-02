package devops.performance_dashboard;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CycleTimeRecord implements Comparable<CycleTimeRecord> {

	private Date releaseDate;
	private String releaseName;
	private List<Commit> commits;
	
	private long meanWait;
	private double waitStdDev;
	private long minWait;
	private long maxWait;
	
	@Override
	public int compareTo(CycleTimeRecord other) {
		return releaseDate.compareTo(other.releaseDate);
	}
	
	public Date getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(Date releaseDate) {
		this.releaseDate = releaseDate;
	}

	public String getReleaseName() {
		return releaseName;
	}

	public void setReleaseName(String releaseName) {
		this.releaseName = releaseName;
	}

	public List<Commit> getCommits() {
		return commits;
	}

	public void setCommits(List<Commit> commits) {
		this.commits = commits;
	}

	public long getMeanWait() {
		return meanWait;
	}

	public void setMeanWait(long meanWait) {
		this.meanWait = meanWait;
	}

	public double getWaitStdDev() {
		return waitStdDev;
	}

	public void setWaitStdDev(double waitStdDev) {
		this.waitStdDev = waitStdDev;
	}

	public long getMinWait() {
		return minWait;
	}

	public void setMinWait(long minWait) {
		this.minWait = minWait;
	}

	public long getMaxWait() {
		return maxWait;
	}

	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
	}

	public CycleTimeRecord(JiraVersion jv, List<Commit> commits) {
		releaseName = jv.getName();
		releaseDate = jv.getReleaseDate();
		this.commits = commits; 
		
		this.meanWait = 0;
		this.waitStdDev = 0;
		
		boolean firstCommit = true;
		
		for (Commit c : commits) {
			long diff = this.releaseDate.getTime() - c.getCommitTime().getTime();
		    long diffDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
		    
		    meanWait += diffDays;
		    
		    
		    if (firstCommit) {
		    	this.minWait = diffDays;
		    	this.maxWait = diffDays;
		    	firstCommit = false;
		    } else {
		    	this.minWait = Math.min(diffDays, this.minWait);
		    	this.maxWait = Math.max(diffDays, this.maxWait);
		    }
		    
		}
		
		meanWait = meanWait / commits.size();
		
		for (Commit c : commits) {
			long diff = this.releaseDate.getTime() - c.getCommitTime().getTime();
		    long diffDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
			
			waitStdDev = waitStdDev + Math.pow((meanWait - diffDays), 2);
		}
		
		waitStdDev = waitStdDev / commits.size();
		
		waitStdDev = Math.pow(waitStdDev, 0.5);
		
	}

}
