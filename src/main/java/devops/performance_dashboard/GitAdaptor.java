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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.Edit.Type;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
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
			System.out.println("Not performing Git Pull!");
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

			String path = diff.getNewPath();
			if (!isExcludedPath(path)) {

				// Get the list of edits
				FileHeader header = formatter.toFileHeader(diff);
				EditList editlist = header.toEditList();

				for (Edit edit : editlist) {

					int length = edit.getLengthB();
					if (edit.getType().equals(Type.DELETE)) {
						length = edit.getLengthA();
						path = diff.getOldPath();
					}

					GitEdit gedit = new GitEdit(commit.getAuthorIdent().getName(), branch.getName(), commit.getName(),
							path, edit.getType().toString(), edit.getBeginA(), length);

					gedits.add(gedit);

				}
			}

		}

		return gedits;
	}

	private List<GitEdit> findEditsForCommit(Git git, RevCommit commit, List<Ref> branches)
			throws MissingObjectException, IncorrectObjectTypeException, IOException, NoHeadException, GitAPIException {
		
		RevWalk walk = new RevWalk(git.getRepository());

		for (Ref branch : branches) {

			// System.out.println("\nScanning branch: " + branch.getName());
			// System.out.println("-------------------------------------");

			RevCommit tip = walk.parseCommit(branch.getLeaf().getObjectId());
			if (isMergedInto(git, commit, tip)) {

				System.out.println("\nFound in branch " + branch.getName());
				return findEditsForCommitInBranch(git.getRepository(), walk, commit, branch);

			}

		}

		System.out.println("Not found in any branches");

		return new ArrayList<GitEdit>();
	}

	private boolean isMergedInto(Git git, RevCommit base, RevCommit tip)
			throws NoHeadException, GitAPIException, IOException {

		RevWalk walk = new RevWalk(git.getRepository());

		walk.markStart(tip);

		RevCommit mergeBase;
		while ((mergeBase = walk.next()) != null) {
			// System.out.print("c");
			if (mergeBase.equals(base)) {
				walk.release();
				walk.dispose();
				return true;
			}
		}

		walk.release();
		walk.dispose();
		return false;

	}

	public void getUnMergedChanges() throws CorruptObjectException, MissingObjectException, IOException,
			InvalidRemoteException, TransportException, GitAPIException {

		Git git = getUpdatedRepository();

		Repository repository = git.getRepository();

		ObjectId master = repository.resolve("refs/heads/master");

		List<GitEdit> gedits = new ArrayList<GitEdit>();

		List<Ref> branches = git.branchList().setListMode(ListMode.ALL).call();

		removeExcludedBranches(branches);

		System.out.print("Finding un-merged commits.. ");

		Iterable<RevCommit> commits = git.log().all().not(master).call();

		System.out.println("(Complete)");
		for (RevCommit commit : commits) {

			System.out.print("Processing: " + commit.getName() + " - ");

			if (commit.getParents().length < 2) { // Don't include merge commits
				List<GitEdit> return_list = findEditsForCommit(git, commit, branches);
				gedits.addAll(return_list);
			} else {
				System.out.println("Ignoring because is a merge commit");
			}
		}

		writeFiles(gedits);

	}

	private void removeExcludedBranches(List<Ref> branches) {

		for (Iterator<String> configIterator = config.getBranchExcludes().iterator(); configIterator.hasNext();) {

			String excludeString = configIterator.next();

			for (Iterator<Ref> i = branches.iterator(); i.hasNext();) {
				Ref currentRef = i.next();
				String branchName = currentRef.getName();

				if (branchName.matches(excludeString)) {
					System.out.println("Removing Branch " + currentRef.getName()
							+ " because it matches exclude string '" + excludeString + "'");
					i.remove();
				}
			}

		}

	}

	private boolean isExcludedPath(String path) {

		for (Iterator<String> configIterator = config.getPathExcludes().iterator(); configIterator.hasNext();) {

			String excludeString = configIterator.next();

			if (path.matches(excludeString)) {
				System.out.println("Removing file " + path + " because it matches exclude string '"
						+ excludeString + "'");
				return true;
				
			}

		}
		
		return false;

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

	public void getCommitHistory() throws IOException, NoHeadException, GitAPIException {
		File repositoryFile = new File(config.getLocalRepository());
		Git git = null;

		git = Git.open(repositoryFile);

		Iterable<RevCommit> commits = git.log().all().call();

		for (RevCommit commit : commits) {
			Commit c = new Commit(commit);

		}
	}

}
