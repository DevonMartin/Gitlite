package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static gitlet.Utils.*;


/** Represents a gitlet commit object.
 * This class works hand-in-hand with the Repository
 * class, and together they provide a version control
 * system for Gitlet's users. Commits are records of
 * files saved as their SHA1 ID generated from their
 * contents at the time of storage. Commits work in
 * the .gitlet directory for storing and retrieving
 * commitments and files.
 *
 * @author Devon Martin
 */
class Commit implements Serializable {

    /** The length of a commit name is 2 less than a
     * full SHA1 ID because the first two characters
     * are used for the directory within objects.
     */
    private static final int COMMIT_NAME_LENGTH = UID_LENGTH - 2;
    /** The message of a commit.
     */
    private final String message;
    /** The pattern used for a displaying a commits
     * time.
     */
    private static final String PATTERN = "EEE MMM dd HH:mm:ss yyyy Z";
    /** The formatter used for a displaying a commits
     * time.
     */
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(PATTERN);
    /** The time a commit was made.
     */
    private final String time;
    /** The number to input into new Date() for comparing two
     * commits' times.
     */
    private final long timeForComparison;
    /** The previously most recent commit of a branch
     * when a new commit is made. In the case of a
     * merge, it is the commit of the branch that the
     * user is on at the time of the merge.
     */
    private final String parent1;
    /** The previously most recent commit of the branch
     * the user is not currently on during a merge.
     */
    private final String parent2;
    /** The list of files that were recorded in a
     * commit. Initially copied over from their parent.
     */
    private HashSet<String> files = new HashSet<>();
    /** The full SHA1 ID of a commit.
     */
    private String sha;
    /** Initializes a new Commit with a provided message,
     * date and parents.
     */
    private Commit(String msg, Date date, String parent1, String parent2) {
        this.message = msg;
        this.time = SIMPLE_DATE_FORMAT.format(date);
        this.timeForComparison = date.getTime();
        this.parent1 = parent1;
        this.parent2 = parent2;
    }
    /** Returns a commit that was not a merge.
     */
    private static Commit getCommit(String msg, String parent) {
        return new Commit(msg, new Date(), parent, null);
    }
    /** Returns a commit that was a merge.
     */
    private static Commit getCommit(String msg, String parent1, String parent2) {
        return new Commit(msg, new Date(), parent1, parent2);
    }
    /** Returns the unique ID used to store a commit.
     */
    String getID() {
        return sha;
    }

    /** Returns the amount of time that has passed since The Epoch
     * to the creation of this commit.
     */
    long getTime() {
        return timeForComparison;
    }
    /** Return the message of a commit.
     */
    String getMessage() {
        return message;
    }

    /** Returns an array of the two parents of this.
     * If a parent is null, ignore it.
     */
    Commit[] getParents() {
        if (parent1 == null) {
            return new Commit[] {};
        } else if (parent2 == null) {
            return new Commit[] {getCommitFromString(parent1)};
        }
        return new Commit[] {getCommitFromString(parent1), getCommitFromString(parent2)};
    }
    /** Returns a HashSet of all files stored by a
     * commit by their original name.
     */
    HashSet<String> getCommittedFiles() {
        HashSet<String> returnList = new HashSet<>();
        for (String file : files) {
            returnList.add(file.substring(UID_LENGTH));
        }
        return returnList;
    }
    /** Returns whether a fileName length matches
     * the COMMIT_NAME_LENGTH.
     */
    static Boolean isCommit(String fileName) {
        return fileName.length() == COMMIT_NAME_LENGTH;
    }
    /** Returns true if a commit has some file saved with
     * the same name as the fileName provided.
     */
    Boolean containsFileName(String fileName) {
        return getCommittedFiles().contains(fileName);
    }
    /** Returns true if a commit has an exact version of a file.
     */
    Boolean containsExactFile(File file) {
        String fileName = getFileID(file) + file.getName();
        return files.contains(fileName);
    }
    /** Returns true if a commit has an exact version of a file.
     */
    Boolean containsExactFile(String file) {
        return files.contains(file);
    }
    /** Returns the full-length name of a file stored by a commit.
     */
    String getFullFileName(String fileName) {
        for (String fullFileName : files) {
            if (fullFileName.substring(UID_LENGTH).equals(fileName)) {
                return fullFileName;
            }
        }
        return null;
    }
    /** Searches for and returns a Commit by a full or partial name.
     */
    static Commit getCommitFromString(String commit) {
        if (commit.length() > 2) {
            String dirString = commit.substring(0, 2);
            File dir = join(Repository.OBJECTS_DIR, dirString);
            List<String> dirFiles = plainFilenamesIn(dir);
            if (dirFiles != null) {
                String commitStr = commit.substring(2);
                for (String file : dirFiles) {
                    String fileSubstring = file.substring(0, commitStr.length());
                    if (Commit.isCommit(file) && fileSubstring.equals(commitStr)) {
                        return readObject(join(dir, file), Commit.class);
                    }
                }
            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
        return null;
    }
    /** Returns the unique ID of a file, based on its contents.
     */
    static String getFileID(File file) {
        String s = readContentsAsString(file);
        return sha1(serialize(s));
    }
    /** Creates a base, parent-less commit and returns its ID.
     */
    static String firstCommit() {
        Commit c = new Commit("initial commit", new Date(0), null, null);
        c.saveCommitment();
        return c.getID();
    }
    /** Saves a commit in its current state by its unique ID.
     */
    private void saveCommitment() {
        sha = sha1(serialize(this));
        File file = join(Repository.OBJECTS_DIR, sha.substring(0, 2), sha.substring(2));
        writeObject(file, this);
    }
    static String makeCommitment(String msg) {
        Commit parent = Main.repo.getLatestCommit();
        Commit child = getCommit(msg, parent.getID());
        child.files = (HashSet<String>) parent.files.clone();
        return makeCommitmentHelper(child);
    }
    static String makeMergeCommitment(String msg, Commit parent1, Commit parent2) {
        Commit child = getCommit(msg, parent1.getID(), parent2.getID());
        child.files = (HashSet<String>) parent1.files.clone();
        return makeCommitmentHelper(child);
    }
    /** Creates a new commit.
     * @param c    A newly created Commit.
     * @return     The ID of the commitment for storage in the repo.
     */
    private static String makeCommitmentHelper(Commit c) {
        List<String> filesInStagingDir = plainFilenamesIn(Repository.STAGING_DIR);
        if (filesInStagingDir != null) {
            for (String file : plainFilenamesIn(Repository.STAGING_DIR)) {
                /* Remove the previous version of a file
                 * that has been updated and staged.
                 */
                if (c.containsFileName(file)) {
                    c.removeFileFromCommit(file);
                }
                /* Add the file to the new commit.
                 */
                c.addFileToCommit(file);
            }
        }
        /* Remove files from commit that have been staged for removal.
         */
        for (String file : Main.repo.rmStage) {
            c.removeFileFromCommit(file);
        }
        c.saveCommitment();
        return c.sha;
    }
    /** Stores a copy of a file in a commit.
     */
    private void addFileToCommit(String fileString) {
        File file = join(Repository.STAGING_DIR, fileString);
        String fullFileName = getFileID(file) + fileString;
        files.add(fullFileName);
        saveFileForCommit(file.toPath(), fullFileName);
    }
    /** Move a file from the staging directory into its ID-directory.
     */
    private static void saveFileForCommit(Path file, String fileName) {
        Path destination = join(Repository.OBJECTS_DIR,
                fileName.substring(0, 2), fileName.substring(2)).toPath();
        try {
            Files.move(file, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /** Removes a file from a commit if it exists. Only used when
     * a child is updating a file tracked by its parent.
     *
     * @param fileToRemove  The name of a file in the user's CWD.
     */
    private void removeFileFromCommit(String fileToRemove) {
        for (String file : files) {
            if (file.substring(UID_LENGTH).equals(fileToRemove)) {
                files.remove(file);
                return;
            }
        }
    }
    /** Used in printing the log of a repository.
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("===\ncommit ").append(sha).append("\n");
        if (parent2 != null) {
            str.append("Merge: ").append(parent1, 0, 7).append(" ");
            str.append(parent2, 0, 7).append("\n");
        }
        str.append("Date: ").append(time).append("\n").append(message).append("\n");
        return str.toString();
    }
    /** Iteratively logs commits, following the line of
     * the first parents while ignoring the second parents
     * in cases of merging.
     */
    void log() {
        System.out.println(this);
        Commit c = this;
        while (c.parent1 != null) {
            c = getCommitFromString(c.parent1);
            System.out.println(c);
        }
    }
    /** Searches the objects directory for all commits.
     * @return Every commit made in the CWD.
     */
    static ArrayList<Commit> getAllCommits() {
        ArrayList<Commit> returnFiles = new ArrayList<>();
        /** The following code is adapted from:
         *  @author Holger
         *  @date   Jan 14, 2016 at 17:33
         *  @link   https://stackoverflow.com/a/34796064/19117711
          */
        Pattern pattern = Pattern.compile("commit (.+)");
        try (Stream<String> lines = Files.lines(Repository.GLOBAL_LOG_FILE.toPath())) {
            lines.map(pattern::matcher)
                    .filter(Matcher::find)
                    .forEach(mr -> returnFiles.add(getCommitFromString(mr.group(1))));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnFiles;
    }

    /** Used in comparing commits during a merge.
     *
     * @param o The Commit to compare to.
     * @return  True if the ID of two commits are the same.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Commit)) {
            return false;
        }
        Commit c = (Commit) o;
        return c.getID().equals(this.getID());
    }

    /** The first six digits of an ID of a commit is likely sufficient
     * in keeping files unique for a hash code.
     * @return The first six digits of the ID of a commit, converted
     * from Hex to Decimal.
     */
    @Override
    public int hashCode() {
        return Integer.parseInt(sha.substring(0, 6), 16);
    }
}
