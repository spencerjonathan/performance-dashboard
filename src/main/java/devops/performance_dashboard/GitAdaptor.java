package devops.performance_dashboard;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
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

public class GitAdaptor {
	
	private Config config;

	public GitAdaptor(Config config) {
		System.out.println("Constructing GitAdaptor");
		this.config = config;
		
	}

	public void cloneRepository() throws CorruptObjectException, MissingObjectException, IOException,
			InvalidRemoteException, TransportException, GitAPIException {
		File repositoryFile = new File(config.getLocalRepository());
		Git git = null;

		if (!repositoryFile.exists()) {

			CredentialsProvider cp = new UsernamePasswordCredentialsProvider(config.getUsername(), config.getPassword());
			System.out.println("Cloning repository");
			git = Git.cloneRepository().setURI(config.getUri())
					.setDirectory(repositoryFile).setCredentialsProvider(cp).call();

		} else {

			git = Git.open(repositoryFile);

		}

		Repository repository = null;

		repository = new FileRepository(repositoryFile + "/.git");

		Iterable<RevCommit> commits = null;

		ObjectId master = repository.resolve("refs/heads/master");
		System.out.println("Finding un-merged commits");
		commits = git.log().all().not(master).call();

		List<GitEdit> gedits = new ArrayList<GitEdit>();
		
		RevWalk rw = new RevWalk(repository);
		for (RevCommit rev : commits) {
			System.out.println("Commit: " + rev + ", name: " + rev.getName() + ", id: " + rev.getId().getName());
			RevCommit parent = null;

			parent = rw.parseCommit(rev.getParent(0).getId());

			OutputStream outputStream = DisabledOutputStream.INSTANCE;

			DiffFormatter formatter = new DiffFormatter(outputStream);
			formatter.setRepository(git.getRepository());
			List<DiffEntry> entries = null;

			entries = formatter.scan(parent.getTree(), rev.getTree());

			for (DiffEntry diff : entries) {
				System.out.println(MessageFormat.format("({0} {1} {2}", diff.getChangeType().name(),
						diff.getNewMode().getBits(), diff.getNewPath()));

				FileHeader header = formatter.toFileHeader(diff);

				List<String> branches = getBranchesContainingCommit(git, rev.getId().getName());

				EditList editlist = header.toEditList();

				for (Edit edit : editlist) {
					System.out.println("Type: " + edit.getType().toString());
					System.out.println("A: " + edit.getBeginA() + " - " + edit.getEndA());
					System.out.println("B: " + edit.getBeginB() + " - " + edit.getEndB());
					System.out.println("A length: " + edit.getLengthA());
					System.out.println("B length: " + edit.getLengthB());
					System.out.println("Branch: " + branches.get(0));

					int length = edit.getLengthB();
					if (edit.getType().equals(Type.DELETE)) {
						length = edit.getLengthA();
					}

					String branch="Unknown";
					if (branches.size() > 0) {
						branch=branches.get(0);
					}
					
					GitEdit gedit = new GitEdit(branch, rev.getName(), edit.getType().toString(), edit.getBeginA(), length);
					
					Gson gson = new Gson();
					String json = gson.toJson(gedit);
					System.out.println(json);
					
					gedits.add(gedit);
				}
			}
			/*
			 * FileHeader fileHeader = formatter.toFileHeader(entries.get(0));
			 * return fileHeader.toEditList();
			 */
			
			

		}
		
		File output_file = new File("./git.js");
		if (!output_file.canWrite()) {
			System.err.println("Cannot write git.json output file)");
			//System.exit(-1);
		}
		
		FileWriter writer = new FileWriter(output_file);
		Gson gson = new Gson();
		
		writer.write("var gitWIPData = ");
		gson.toJson(gedits, writer);
		writer.write(";");
		
		writer.close();

	}

	public List<String> getBranchesContainingCommit(Git git, String id) throws RevisionSyntaxException,
			AmbiguousObjectException, IncorrectObjectTypeException, IOException, RefAlreadyExistsException,
			RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
		List<String> branches = new ArrayList<String>();

		// Find commit

		RevWalk walk = new RevWalk(git.getRepository());
		ObjectId foundId = git.getRepository().resolve(id + "^0");
		RevCommit commit = walk.parseCommit(foundId);

		// For all Remote Branches
		for (Ref branch : git.branchList().call()) {
			System.out.println("Checking out: " + branch.getName());
			git.checkout().setName(branch.getName()).call();

			RevCommit head = walk.parseCommit(git.getRepository().resolve(Constants.HEAD));
			
			if (walk.isMergedInto(commit, head)) {
				branches.add(branch.getName());

			}	
		}

		walk.release();
		walk.dispose();
		
		return branches;
	}

}
