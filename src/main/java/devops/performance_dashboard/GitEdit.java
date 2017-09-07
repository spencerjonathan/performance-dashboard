package devops.performance_dashboard;

public class GitEdit {

	private String branch;
	private String revision;
	private String type;
	private int begin;
	private int length;

	public GitEdit(String branch, String revision, String type, int begin, int length) {
		// TODO Auto-generated constructor stub
		this.branch=branch;
		this.revision=revision;
		this.type=type;
		this.begin=begin;
		this.length=length;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getBegin() {
		return begin;
	}

	public void setBegin(int begin) {
		this.begin = begin;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

}
