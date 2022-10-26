package gitlet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 * This class works hand-in-hand with the Commit class,
 * and together they provide a version control system
 * for Gitlet's users. Repository implements the
 * directories used by the program, and maintains a
 * record of the last commitment made on the Repository.
 * Each Repository is a branch which can have its own
 * path of version control, and each branch that isn't
 * the first (master) "branches" off of another branch.
 * Repository also allows for backing up previously
 * saved versions of files.
 *
 * @author Devon Martin
 */
class Repository implements Serializable {

    /** The Current Working Directory of the user.
     */
    private static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory of a repository.
     */
    private static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The reference to the branch a user is currently working in.
     */
    private static final File HEAD = join(GITLET_DIR, "HEAD");
    /** The global log file.
     */
    static final File GLOBAL_LOG_FILE = join(GITLET_DIR, "global log");
    /** The directory storing directories for file
     * and commit storage within the .gitlet directory.
     */
    static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    /** The directory storing all branches of a
     * repository.
     */
    private static final File REFS_DIR = join(GITLET_DIR, "refs");
    /** The directory temporarily storing files while
     * they are staged for addition in the next commit.
     */
    static final File STAGING_DIR = join(GITLET_DIR, "staging");
    /** The possible characters in a hexadecimal string, for creating
     * directories for storing files saved by an encrypted name.
     */
    static final String[] HEXADECIMAL_CHARS = { "0", "1", "2", "3", "4", "5", "6", "7",
                                                "8", "9", "a", "b", "c", "d", "e", "f" };
    /** The branch this repo belongs to.
     */
    private String currentBranch = "master";
    /** The last commit made on this repository, stored by
     * its full unique ID.
     */
    private String latestCommit;
    /** Return the latest Commit of a repository.
     */
    Commit getLatestCommit() {
        return Commit.getCommitFromString(latestCommit);
    }
    /** The files staged for removal.
     */
    HashSet<String> rmStage = new HashSet<>();
    /** Initializes a new repository and calls helper functions to:
     * -create .gitlet and related directories
     * -create and save an initial commit
     * -create an initial branch, saving this repo as "master"
     * -set the current HEAD to this repo
     */
    Repository() {
        if (inRepo()) {
            System.out.println("A Gitlet version-control system"
                    + " already exists in the current directory.");
            return;
        }
        createDirectories();
        latestCommit = Commit.firstCommit();
        initializeGlobalLog();
        branch(currentBranch);
        setHeadTo(currentBranch);
    }
    /** Returns true if the CWD contains a .gitlet/ dir.
     */
    static boolean inRepo() {
        return GITLET_DIR.exists();
    }
    /** Returns the Repository stored by HEAD file.
     */
    static Repository loadHead() {
        String branch = readContentsAsString(HEAD);
        return readObject(join(REFS_DIR, branch), Repository.class);
    }
    /** Creates .gitlet/ dir and all dirs within.
     */
    private static void createDirectories() {
        OBJECTS_DIR.mkdirs();
        for (String c1 : HEXADECIMAL_CHARS) {
            for (String c2 : HEXADECIMAL_CHARS) {
                join(OBJECTS_DIR, c1 + c2).mkdir();
            }
        }
        REFS_DIR.mkdir();
        STAGING_DIR.mkdir();
    }
    private void initializeGlobalLog() {
        try (FileWriter writer = new FileWriter(GLOBAL_LOG_FILE)) {
            writer.write(getLatestCommit().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /** Change what branch the HEAD points at and the user is in.
     */
    private static void setHeadTo(String branch) {
        try (FileWriter writer = new FileWriter(HEAD, false)) {
            writer.write(branch);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /** Add a file to staging if it varies from the currently staged version
     * and the currently tracked version. If a version is already staged,
     * but the file matches the currently tracked version, remove it from
     * staging. If the file is added, and was staged for removal, remove it
     * from the removal staging.
     * @param file  The file name in the CWD to add. Use File.getName() for files.
     */
    void add(String file) {
        List<String> filesInCWD = plainFilenamesIn(CWD);
        if (filesInCWD != null) {
            if (file.equals(".")) {
                for (String fileInCWD : filesInCWD) {
                    add(fileInCWD);
                }
                return;
            }
            if (filesInCWD.contains(file)) {
                if (rmStage.remove(file)) {
                    updateBranch();
                }
                Commit c = getLatestCommit();
                if (c.containsExactFile(join(CWD, file))) {
                    join(STAGING_DIR, file).delete();
                    return;
                }
                Path from = join(CWD, file).toPath();
                Path to = join(STAGING_DIR, file).toPath();
                try {
                    Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("File does not exist.");
            }
        }
    }
    /** Create a new commitment if the staging dir or the
     * rmStage contains files and the message is not blank.
     * Clears staging dir and rmStage if successful.
     */
    void commit(String msg) {
        List<String> filesInStagingDir = plainFilenamesIn(STAGING_DIR);
        if (filesInStagingDir != null) {
            if (filesInStagingDir.size() == 0
                    && rmStage.size() == 0) {
                System.out.println("No changes added to the commit.");
            } else if (msg.equals("")) {
                System.out.println("Please enter a commit message.");
            } else {
                latestCommit = Commit.makeCommitment(msg);
                rmStage = new HashSet<>();
                updateBranch();
                updateGlobalLog();
            }
        }
    }
    /** Saves the current state of a repository in its
     * branch file and updates HEAD to reflect this.
     */
    private void updateBranch() {
        File branchFile = join(REFS_DIR, currentBranch);
        writeObject(branchFile, this);
    }
    /** Add the latest commit's .toString() to the GLOBAL_LOG_FILE;
     */
    private void updateGlobalLog() {
        String updatedLog = getLatestCommit().toString()
                + "\n" + readContentsAsString(GLOBAL_LOG_FILE);
        try (FileWriter writer = new FileWriter(GLOBAL_LOG_FILE, false)) {
            writer.write(updatedLog);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /** Remove a file from staging if it is staged and from the
     * CWD if it is already tracked by the previous commit.
     */
    void rm(String file) {
        boolean check1 = join(STAGING_DIR, file).delete();
        boolean check2 = false;
        Commit c = getLatestCommit();
        if (c.containsFileName(file)) {
            rmStage.add(file);
            updateBranch();
            join(CWD, file).delete();
            check2 = true;
        }
        if (!(check1 || check2)) {
            System.out.println("No reason to remove the file.");
        }
    }
    /** Prints the log of all commitments of the current HEAD,
     * following the path of only parent1 of each commitment.
     */
    void log() {
        getLatestCommit().log();
    }
    /** Prints the log of all commitments stored in .gitlet, in
     * alphabetical order.
     */
    static void globalLog() {
        System.out.println(readContentsAsString(GLOBAL_LOG_FILE));
    }
    /** Print the unique ID of any commit which has a message
     * exactly matching the message provided by the user.
     */
    static void find(String msg) {
        boolean found = false;
        for (Commit c : Commit.getAllCommits()) {
            if (c.getMessage().equals(msg)) {
                System.out.println(c.getID());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }
    /** Driver function for printing the status of a Repository, including:
     * -branches, with an asterisk next to the current
     * -files staged for addition in the next commitment
     * -files tracked as being removed from the next commitment
     * -unstaged but tracked modifications, such as edits or manual removals
     * -files not tracked at all, but present in the CWD
     */
    void status() {
        statusBranches();
        statusStagedFiles();
        statusRemovedFiles();
        statusNotStaged();
        statusUntracked();
        System.out.println();
    }
    /** "Branches" helper function for status.
     */
    private void statusBranches() {
        System.out.println("=== Branches ===");
        List<String> branches = plainFilenamesIn(REFS_DIR);
        if (branches != null) {
            for (String b : branches) {
                if (b.equals(currentBranch)) {
                    System.out.print("*");
                }
                System.out.println(b);
            }
        }
    }
    /** "Staged Files" helper function for status.
     */
    private void statusStagedFiles() {
        System.out.println("\n=== Staged Files ===");
        List<String> stagedFiles = plainFilenamesIn(STAGING_DIR);
        if (stagedFiles != null) {
            for (String file : stagedFiles) {
                System.out.println(file);
            }
        }
    }
    /** "Removed Files" helper function for status.
     */
    private void statusRemovedFiles() {
        System.out.println("\n=== Removed Files ===");
        for (String file : rmStage) {
            System.out.println(file);
        }
    }
    /** "Modifications Not Staged For Commit" helper function for status.
     */
    private void statusNotStaged() {
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        Commit c = getLatestCommit();
        List<String> filesInCWD = plainFilenamesIn(CWD);
        if (filesInCWD != null) {
            for (String fileString : filesInCWD) {
                File cwdFile = join(CWD, fileString);
                File stagingFile = join(STAGING_DIR, fileString);
                if ((c.containsFileName(fileString)
                        && !c.containsExactFile(cwdFile)
                        && !stagingFile.exists())
                    ||
                        (stagingFile.exists()
                                && !Commit.getFileID(cwdFile)
                                .equals(Commit.getFileID(stagingFile)))) {
                    System.out.println(fileString + " (modified)");
                }
            }
        }
        List<String> filesInStaging = plainFilenamesIn(STAGING_DIR);
        if (filesInStaging != null) {
            for (String fileString : filesInStaging) {
                File cwdFile = join(CWD, fileString);
                if (!cwdFile.exists()) {
                    System.out.println(fileString + " (deleted)");
                }
            }
        }
        for (String fileString : c.getCommittedFiles()) {
            File cwdFile = join(CWD, fileString);
            File stagingFile = join(STAGING_DIR, fileString);
            if (!rmStage.contains(fileString)
                    && !cwdFile.exists()
                    && !stagingFile.exists()) {
                System.out.println(fileString + " (deleted)");
            }
        }
    }
    /** "Untracked Files" helper function for status.
     */
    private void statusUntracked() {
        System.out.println("\n=== Untracked Files ===");
        for (String file : getUntrackedFiles()) {
            System.out.println(file);
        }
    }
    /** Returns a list of all untracked files.
     */
    private List<String> getUntrackedFiles() {
        List<String> returnList = new ArrayList<>();
        Commit c = getLatestCommit();
        List<String> filesInCWD = plainFilenamesIn(CWD);
        List<String> stagedFiles = plainFilenamesIn(STAGING_DIR);
        boolean stagedFilesNotNull = stagedFiles != null;
        if (filesInCWD != null) {
            for (String file : filesInCWD) {
                if (!(c.containsFileName(file)
                        || (stagedFilesNotNull && stagedFiles.contains(file)))
                        || (rmStage.contains(file))) {
                    returnList.add(file);
                }
            }
        }
        return returnList;
    }
    /** Driver function for checkout which parses the arguments passed
     * to it and calls the appropriate checkout function.
     * -if() is checking out a file from the HEAD repo.
     * -else if() is checking out a file from a specified commit.
     * -else() is switching branches and then switching files in the
     * CWD to the new HEAD.
     */
    void checkout(String[] args) {
        if (args.length == 3 && args[1].equals("--")) {
            checkoutGetFile(getLatestCommit(), args[2]);
        } else if (args.length == 4 && args[2].equals("--")) {
            Commit c = Commit.getCommitFromString(args[1]);
            checkoutGetFile(c, args[3]);
        } else if (args.length == 2) {
            checkoutChangeBranch(args[1]);
            checkoutCommit(loadHead().getLatestCommit());
        } else {
            System.out.println("Incorrect operands.");
        }
    }
    /** Updates the HEAD file to represent a different branch if
     * the requested branch exists, and it is not the current branch.
     */
    private void checkoutChangeBranch(String reqBranch) {
        File branchFile = join(REFS_DIR, reqBranch);
        if (reqBranch.equals(currentBranch)) {
            System.out.println("No need to checkout the current branch.");
        } else if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
        } else {
            untrackedFileCheck();
            setHeadTo(reqBranch);
        }
    }
    /** Changes files in the CWD to that of another commit. Must be
     * used AFTER checking for untracked files in the way.
     */
    private void checkoutCommit(Commit newCommit) {
        List<String> filesInCWD = plainFilenamesIn(CWD);
        List<String> stagedFiles = plainFilenamesIn(STAGING_DIR);
        if (filesInCWD != null) {
            for (String file : filesInCWD) {
                join(CWD, file).delete();
            }
        }
        for (String file : newCommit.getCommittedFiles()) {
            checkoutGetFile(newCommit, file);
        }
        if (stagedFiles != null) {
            for (String file : stagedFiles) {
                join(STAGING_DIR, file).delete();
            }
        }
    }
    /** Helper function for checkout which copies a previously committed
     * file into the CWD.
     * @param reqFile the original name of a file.
     */
    private void checkoutGetFile(Commit c, String reqFile) {
        if (c.containsFileName(reqFile)) {
            String fullFileName = c.getFullFileName(reqFile);
            String dirName = fullFileName.substring(0, 2);
            String fileName = fullFileName.substring(2);
            File dir = join(OBJECTS_DIR, dirName);
            Path to = join(CWD, reqFile).toPath();
            Path from = join(dir, fileName).toPath();
            try {
                Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("File does not exist in that commit.");
        }
    }
    /** Checks for any untracked files in the current commit.
     * If untracked files exist, exit the program without allowing
     * a checkout.
     */
    private void untrackedFileCheck() {
        if (getUntrackedFiles().size() != 0) {
            System.out.println(
                    "There is an untracked file in the way;"
                            + " delete it, or add and commit it first."
            );
            System.exit(0);
        }
    }
    /** Creates a branch, which is a pointer to the current repository,
     * with the provided name, if one with such name does not currently
     * exist.
     */
    void branch(String name) {
        File branchFile = join(REFS_DIR, name);
        if (branchFile.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        currentBranch = name;
        writeObject(branchFile, this);
    }
    /** Remove a branch from Gitlet's memory, if the requested branch is not
     * the current branch, while maintaining a record of the commits that branch
     * had.
     */
    void rmBranch(String name) {
        if (name.equals(currentBranch)) {
            System.out.println("Cannot remove the current branch.");
        } else if (!join(REFS_DIR, name).delete()) {
            System.out.println("A branch with that name does not exist.");
        }
    }
    /** Resets back to the state of a previous commit if there are no
     * untracked files in the way. The branch remains the same, but
     * the head commit reflects the change.
     */
    void reset(String commitString) {
        untrackedFileCheck();
        Commit c = Commit.getCommitFromString(commitString);
        latestCommit = c.getID();
        updateBranch();
        checkoutCommit(c);
    }
    /** Merges secondary branch into current branch.
     * Cases:
     * 8. Files are modified in different ways in current and given.
     * "Merge Conflict"
     */
    void merge(String givenBranch) {
        Commit currentCommit = getLatestCommit();
        Repository givenBranchRepo = mergeFailureCasesCheck(givenBranch);
        Commit givenCommit = givenBranchRepo.getLatestCommit();
        Commit ancestorCommit = mergeLCA(currentCommit, givenCommit);
        if (ancestorCommit.equals(givenCommit)) {
            System.out.println("Given branch is an ancestor of the current branch.");
        } else if (ancestorCommit.equals(currentCommit)) {
            checkoutCommit(givenCommit);
            latestCommit = givenCommit.getID();
            updateBranch();
            System.out.println("Current branch fast-forwarded.");
        } else {
            HashSet<String> visited = new HashSet<>();
            boolean b1 = mergeGivenFiles(currentCommit, givenCommit, ancestorCommit, visited);
            boolean b2 = mergeCurrentFiles(currentCommit, givenCommit, ancestorCommit, visited);
            boolean b3 = mergeAncestorFiles(currentCommit, givenCommit, ancestorCommit, visited);
            if (b1 || b2 || b3) {
                System.out.println("Encountered a merge conflict.");
            }
            mergeMakeCommitment(givenBranch, currentCommit, givenCommit);
        }
    }
    /** Helper function for merge that checks for failure cases.
     * @return the Repository class of the branch upon success.
     */
    private Repository mergeFailureCasesCheck(String branch) {
        List<String> filesInStagingDir = plainFilenamesIn(STAGING_DIR);
        if ((filesInStagingDir != null && filesInStagingDir.size() != 0)
                || rmStage.size() != 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        File givenBranchFile = join(REFS_DIR, branch);
        if (!givenBranchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branch.equals(currentBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        untrackedFileCheck();
        return readObject(givenBranchFile, Repository.class);
    }
    /** Iteratively searches for and returns the Latest Common
     * Ancestor of the current and given commits using BFS.
     * @return The Latest Common Ancestor Commit of current and given.
     */
    private Commit mergeLCA(Commit current, Commit given) {
        Queue<Commit> currents = new LinkedList<>();
        Queue<Commit> givens = new LinkedList<>();
        Commit latestAncestor = Commit.getCommitFromString(Commit.firstCommit());
        while (true) {
            if (current.getTime() > given.getTime()) {
                mergeAddParentsToQueue(current, currents);
                if (currents.isEmpty()) {
                    break;
                }
                current = currents.poll();
            } else if (given.getTime() > current.getTime()) {
                mergeAddParentsToQueue(given, givens);
                if (givens.isEmpty()) {
                    break;
                }
                given = givens.poll();
            } else {
                if (current.getTime() > latestAncestor.getTime()) {
                    latestAncestor = current;
                }
                if (currents.isEmpty() || givens.isEmpty()) {
                    break;
                }
                current = currents.poll();
                given = givens.poll();
            }
        }
        return latestAncestor;
    }

    /** Compares a Commits parents based on their time of creation
     * and adds them to the queue given with the most recent Commit
     * being added first.
     */
    private void mergeAddParentsToQueue(Commit child, Queue<Commit> queue) {
        Commit[] parents = child.getParents();
        if (parents.length == 1) {
            queue.add(parents[0]);
        } else if (parents.length == 2) {
            if (parents[0].getTime() > parents[1].getTime()) {
                queue.add(parents[0]);
                queue.add(parents[1]);
            } else {
                queue.add(parents[1]);
                queue.add(parents[0]);
            }
        }
    }
    /** Handles merge duties of cycling through all Given files.
     *
     * @param given    The Given Commit from merge.
     * @param current  The Current Commit from merge.
     * @param ancestor The Ancestor Commit from merge.
     * @param visited  The HashSet of files that have already been checked from merge.
     * @return         True if a merge conflict occurred.
     */
    private boolean mergeGivenFiles(Commit current, Commit given,
                                 Commit ancestor, HashSet<String> visited) {
        boolean conflict = false;
        for (String file : given.getCommittedFiles()) {
            if (visited.contains(file)) {
                continue;
            }
            boolean currentContainsFile = current.containsFileName(file);
            boolean ancestorContainsFile = ancestor.containsFileName(file);
            String givenFullFileName = given.getFullFileName(file);
            if ((currentContainsFile
                    && ancestorContainsFile
                    && current.getFullFileName(file).equals(ancestor.getFullFileName(file))
                    ||
                    (!currentContainsFile
                            && !ancestorContainsFile))) {
                checkoutGetFile(given, file);
                add(file);
                visited.add(file);
            } else if (currentContainsFile
                    && !ancestorContainsFile
                    && !givenFullFileName.equals(current.getFullFileName(file))) {
                mergeConflict(current, given, file);
                conflict = true;
                visited.add(file);
            }
        }
        return conflict;
    }
    /** Handles merge duties of cycling through all Current files.
     *
     * @param current  The Current Commit from merge.
     * @param given    The Given Commit from merge.
     * @param ancestor The Ancestor Commit from merge.
     * @param visited  The HashSet of files that have already been checked from merge.
     * @return         True if a merge conflict occurred.
     */
    private boolean mergeCurrentFiles(Commit current, Commit given,
                                   Commit ancestor, HashSet<String> visited) {
        boolean conflict = false;
        for (String file : current.getCommittedFiles()) {
            if (visited.contains(file)) {
                continue;
            }
            boolean givenHasFile = given.containsFileName(file);
            String currentFileName = current.getFullFileName(file);
            boolean ancestorHasExactFile = ancestor.containsExactFile(currentFileName);
            boolean ancestorHasFile = ancestor.containsFileName(file);
            if (!givenHasFile
                && ancestorHasExactFile) {
                rm(file);
                visited.add(file);
            } else if (givenHasFile
                    && !ancestorHasFile
                    && !given.getFullFileName(file).equals(currentFileName)) {
                mergeConflict(current, given, file);
                conflict = true;
                visited.add(file);
            }
        }
        return conflict;
    }
    /** Handles merge duties of cycling through all Ancestor files.
     *
     * @param current  The Current Commit from merge.
     * @param given    The Given Commit from merge.
     * @param ancestor The Ancestor Commit from merge.
     * @param visited  The HashSet of files that have already been checked from merge.
     * @return         True if a merge conflict occurred.
     */
    private boolean mergeAncestorFiles(Commit current, Commit given,
                                    Commit ancestor, HashSet<String> visited) {
        boolean conflict = false;
        for (String file : ancestor.getCommittedFiles()) {
            if (visited.contains(file)) {
                continue;
            }
            boolean currentHasFile = current.containsFileName(file);
            boolean givenHasFile = given.containsFileName(file);
            String originalFile = ancestor.getFullFileName(file);
            boolean currentHasExactFile = current.containsExactFile(originalFile);
            boolean givenHasExactFile = given.containsExactFile(originalFile);
            if ((currentHasFile
                 && givenHasFile
                 && !currentHasExactFile
                 && !givenHasExactFile
                 && !current.getFullFileName(file).equals(given.getFullFileName(file)))
                ||
                    (currentHasFile
                     && !givenHasFile
                     && !currentHasExactFile)
                ||
                    (givenHasFile
                     && !currentHasFile
                     && !givenHasExactFile)) {
                mergeConflict(current, given, file);
                conflict = true;
                visited.add(file);
            }
        }
        return conflict;
    }

    /** Handles reading and writing the file that has the merge conflict
     * by combining the contents of both files and forcing the user to
     * choose the correct version and delete the incorrect.
     * @param current          The Current Commit from merge.
     * @param given            The Given Commit from merge.
     * @param fileOriginalName The name of the file that has the merge conflict.
     */
    private void mergeConflict(Commit current, Commit given, String fileOriginalName) {
        File file = join(CWD, fileOriginalName);
        String s = "<<<<<<< HEAD\n";
        s += mergeFileReader(current, fileOriginalName);
        s += "\n=======\n";
        s += mergeFileReader(given, fileOriginalName);
        s += ">>>>>>>";
        writeContents(file, s);
        add(fileOriginalName);
    }
    /** Reads a file tracked by a commit as a string.
     *
     * @param c    The commit tracking the file.
     * @param file The file's original name.
     * @return     The contents of the file as a string.
     */
    private String mergeFileReader(Commit c, String file) {
        if (!c.containsFileName(file)) {
            return "";
        }
        String fullFileName = c.getFullFileName(file);
        String dirName = fullFileName.substring(0, 2);
        String fileName = fullFileName.substring(2);
        File fileToRead = join(OBJECTS_DIR, dirName, fileName);
        return readContentsAsString(fileToRead);
    }

    /** Makes a commitment after a merge has happened.
     *
     * @param givenBranch   The branch that was merged in.
     * @param currentCommit The current commitment.
     * @param givenCommit   The current commitment of the givenBranch.
     */
    private void mergeMakeCommitment(String givenBranch,
                                     Commit currentCommit, Commit givenCommit) {
        String msg = "Merged " + givenBranch + " into " + currentBranch + ".";
        latestCommit = Commit.makeMergeCommitment(msg, currentCommit, givenCommit);
        rmStage = new HashSet<>();
        updateBranch();
        updateGlobalLog();
    }
}
