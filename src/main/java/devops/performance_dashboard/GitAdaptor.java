package devops.performance_dashboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.Edit.Type;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class GitAdaptor {

	private Config config;

	public GitAdaptor(Config config) {
		System.out.println("Constructing GitAdaptor");
		this.config = config;

	}

	private Git getUpdatedRepository() throws InvalidRemoteException, TransportException, GitAPIException, IOException {
		File repositoryFile = new File(config.getLocalRepository());
		Git git = null;

		if (!repositoryFile.exists()) {

			CredentialsProvider cp = new UsernamePasswordCredentialsProvider(config.getUsername(),
					config.getPassword());
			System.out.println("Cloning repository");
			git = Git.cloneRepository().setURI(config.getUri()).setDirectory(repositoryFile).setCredentialsProvider(cp)
					.call();

		}

		git = Git.open(repositoryFile);

		if (config.getPerformPull()) {
			System.out.print("Executing Git Pull to update local repository.. ");
			git.pull().call();
			System.out.println("(Complete)");
		} else {
			System.out.print("Not performing Git Pull!");
		}

		return git;
	}

	private List<GitEdit> findEditsForCommitInBranch(Repository repository, RevWalk walk, RevCommit commit, Ref branch)
			throws MissingObjectException, IncorrectObjectTypeException, IOException {

		// Setup the formatter
		OutputStream outputStream = DisabledOutputStream.INSTANCE;
		DiffFormatter formatter = new DiffFormatter(outputStream);
		formatter.setRepository(repository);

		// Get the diffs in the commit
		RevCommit parent = walk.parseCommit(commit.getParent(0).getId());
		List<DiffEntry> entries = formatter.scan(parent.getTree(), commit.getTree());

		// Create a list of GitEdits to hold the results in
		List<GitEdit> gedits = new ArrayList<GitEdit>();

		for (DiffEntry diff : entries) {

			// Get the list of edits
			FileHeader header = formatter.toFileHeader(diff);
			EditList editlist = header.toEditList();

			for (Edit edit : editlist) {

				int length = edit.getLengthB();
				String path = diff.getNewPath();
				if (edit.getType().equals(Type.DELETE)) {
					length = edit.getLengthA();
					path = diff.getOldPath();
				}

				GitEdit gedit = new GitEdit(commit.getAuthorIdent().getName(), branch.getName(), commit.getName(), path,
						edit.getType().toString(), edit.getBeginA(), length);

				gedits.add(gedit);

			}

		}

		return gedits;
	}

	private List<GitEdit> findEditsForCommit(Git git, RevCommit commit, List<Ref> branches)
			throws MissingObjectException, IncorrectObjectTypeException, IOException, NoHeadException, GitAPIException {
		RevCommit parent = null;

		RevWalk walk = new RevWalk(git.getRepository());

		for (Ref branch : branches) {

			// System.out.println("Commits of branch: " +
			// branch.getName());
			// System.out.println("-------------------------------------");

			RevCommit tip = walk.parseCommit(branch.getLeaf().getObjectId());
			if (isMergedInto(git, commit, tip)) {

				return findEditsForCommitInBranch(git.getRepository(), walk, commit, branch);

			}

		}

		return new ArrayList<GitEdit>();
	}

	private boolean isMergedInto(Git git, RevCommit base, RevCommit tip)
			throws NoHeadException, GitAPIException, IOException {

		RevWalk walk = new RevWalk(git.getRepository());

		walk.markStart(tip);

		RevCommit mergeBase;
		while ((mergeBase = walk.next()) != null)
			if (mergeBase.equals(base))
				return true;
		return false;

	}

	public void getUnMergedChanges() throws CorruptObjectException, MissingObjectException, IOException,
			InvalidRemoteException, TransportException, GitAPIException {

		Git git = getUpdatedRepository();

		Repository repository = git.getRepository();

		ObjectId master = repository.resolve("refs/heads/master");
		System.out.println("Finding un-merged commits");

		List<GitEdit> gedits = new ArrayList<GitEdit>();

		List<Ref> branches = git.branchList().setListMode(ListMode.ALL).call();

		Iterable<RevCommit> commits = git.log().all().not(master).call();

		for (RevCommit commit : commits) {

			if (commit.getParents().length < 2) { // Don't include merge commits
				List<GitEdit> return_list = findEditsForCommit(git, commit, branches);
				gedits.addAll(return_list);
			}
		}

		writeFiles(gedits);

	}

	private void writeFiles(List<GitEdit> gedits) throws IOException {

		Gson gson = new Gson();
		Map<String, List<GitEdit>> history = loadHistory();

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String current_date = dateFormat.format(date);

		history.put(current_date, gedits);

		// Write git.js file
		File output_file = new File("./git.js");
		FileWriter writer = new FileWriter(output_file);

		writer.write("var gitWIPData = ");
		gson.toJson(history, writer);
		writer.write(";");

		writer.close();

		File working_folder = new File("./working");

		if (!working_folder.exists()) {
			working_folder.mkdirs();
		}
		// Write History File
		output_file = new File("./working/git_history.json");
		writer = new FileWriter(output_file);

		gson.toJson(history, writer);

		writer.close();
	}

	private static Map<String, List<GitEdit>> loadHistory() {
		File history_file = new File("./working/git_history.json");

		if (!history_file.exists()) {
			System.out.println("Config file does not exist");
			return new HashMap<String, List<GitEdit>>();
		}

		Reader history_reader = null;
		try {
			history_reader = new BufferedReader(new FileReader(history_file));
		} catch (FileNotFoundException e) {

			e.printStackTrace();
			System.out.println("Error reading History file");
			System.exit(-1);
		}

		Gson gson = new Gson();
		java.lang.reflect.Type type = new TypeToken<Map<String, List<GitEdit>>>() {
		}.getType();
		Map<String, List<GitEdit>> history = gson.fromJson(history_reader, type);

		try {
			history_reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return history;
	}

	/*
	 * private List<String> getBranchesContainingCommit(Git git, String id)
	 * throws RevisionSyntaxException, AmbiguousObjectException,
	 * IncorrectObjectTypeException, IOException, RefAlreadyExistsException,
	 * RefNotFoundException, InvalidRefNameException, CheckoutConflictException,
	 * GitAPIException { List<String> branches = new ArrayList<String>();
	 * 
	 * // Find commit
	 * 
	 * RevWalk walk = new RevWalk(git.getRepository()); ObjectId foundId =
	 * git.getRepository().resolve(id + "^0"); RevCommit commit =
	 * walk.parseCommit(foundId);
	 * 
	 * // For all Remote Branches for (Ref branch : git.branchList().call()) {
	 * System.out.println("Checking out: " + branch.getName());
	 * git.checkout().setName(branch.getName()).call();
	 * 
	 * RevCommit head =
	 * walk.parseCommit(git.getRepository().resolve(Constants.HEAD));
	 * 
	 * if (walk.isMergedInto(commit, head)) { branches.add(branch.getName());
	 * 
	 * } }
	 * 
	 * walk.release(); walk.dispose();
	 * 
	 * return branches; }
	 */
	private void loadHistoryFile() {

	}

}
