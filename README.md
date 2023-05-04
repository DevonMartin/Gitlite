# Gitlet

<!-- 

Uncomment this picture when logo is finished and uploaded!

<img src="./img/GitletLogo.png" align="right" alt="Gitlet logo" height="auto" width="12%">

-->

Gitlet is a lightweight recreation of Git, i.e. a version control system for your local work station. It was built with Java, with the entirety of the logic and design of the codebase made by me.

## Commands

### init

##### Usage:

	java gitlet.Main init

Initializes a new Gitlet repository in your local directory. A newly created folder, .gitlet, stores all files backed up with Gitlet, along with the necessary information for Gitlet to retrieve backed up files for a user. A single branch, called "master", is created at initialization. Repositories start with a single, empty commit labeled with the message "initial commit".

### add

##### Usage

	java gitlet.Main add [file name or "." for all files in the CWD]

Adds a file as it exists in the current working directory to a staging area. The file is then "staged for addition" to the next commit. If a file with the same name has already been staged, the current version of the file overwrites the previous version. If the current version of the file matches the previously commited version (i.e. if the file is "tracked" by the previous commit), but there is a different version staged for addition, the staged version is removed. If an added file is staged for removal, it is unstaged for removal.

### rm

##### Usage

	java gitlet.Main rm [file name]
	
If the given file is currently staged for addition, it is removed from staging. If the file is tracked by the latest commit, it is removed from the users CWD and tracked as being staged for removal.

### status

##### Usage

	java gitlet.Main status

Displays information about the current state of the local repository. 

<details><summary><b>Examples</b></summary>

<p align="center">
<img src="./src/status 1.png" alt="Demo of status command following the initialization of a new Gitlet repository" height="auto" width="85%">
<img src="./src/status 2.png" alt="Demo of status command following the addition of a file" height="auto" width="85%">
</p>

</details>

### commit

##### Usage

	java gitlet.Main commit [commit message]
	
Saves a copy of files modified and staged for either addition or removal in a new "commitment". A commitment is a class which has been serialized and given an ID based on its SHA-1 value in hexadecimal format. This ID is displayed in the log and global log, and can be used, either in full or a substring of it beginning at the first character, to access files from the commitment.

### log

##### Usage

	java gitlet.Main log
	
Displays a record of commitments and merges that have taken place on the current branch, starting with the most recent. For merges, the log will display the first seven characters of both parents' IDs, starting with the branch that was checked out at the time of the merge. 

<details><summary><b>Examples</b></summary>

<p align="center">
<img src="./src/log 1.png" alt="Demo of log command following the initialization of a new Gitlet repository and a single commitment made by the user" height="auto" width="85%">

</details>

### find

##### Usage

	java gitlet.Main find [commit message]
	
Searches through all commitments made in the current repository and returns, in order of most recent first, the IDs of all commitments that have a message matching the requested message.

<details><summary><b>Examples</b></summary>

<p align="center">
<img src="./src/find 1.png" alt="Demo of find command in which a user finds two commitments with the same message" height="auto" width="85%">

</details>

### branch

##### Usage

	java gitlet.Main branch [new branch name]
	
Creates a new branch with the given name, if a branch with that name does not currently exist. Does not switch the user to the new branch.

### rm-branch

##### Usage

	java gitlet.Main rm-branch [branch name]
	
Removes a branch from the current repository if it is not the current branch. This simply removes the reference to the commitment which that branch pointed to. It does not remove the history of any commitments, nor does it remove the versions of files stored under that branch.

### checkout

##### Usage 1

	java gitlet.Main checkout [branch name]
	
Switches the user to a branch with the name provided if it exists and if the user does not have any untracked files in the way. All files in the CWD are replaced with the files tracked by the latest commitment of the branch checked out.

##### Usage 2

	java gitlet.Main checkout -- [file name]
	
Checks out the version of a file from the latest commitment in the current branch if it exists. Replaces the file in the CWD if one with the same name exists.

##### Usage 3

	java gitlet.Main checkout [commit id] -- [file name]
	
Checks out the version of a file from the commitment with the provided ID if the commitment exists and if a file with the given name exists in the commitment. Replaces the file in the CWD if one with the same name exists.

### global-log

##### Usage

	java gitlet.Main global-log
	
Displays a log of all commitments and merges ever made in the current repository, from newest to oldest. 

### reset

##### Usage

	java gitlet.Main reset [commit ID]
	
Essentially checks out a given commit ID by replacing all files in the CWD with the files tracked by the given commitment. Changes the latest commitment pointed to by the current branch to the given commitment. As with removing a branch, this does not remove the history of any commitments, nor does it remove the versions of files stored under that branch.

### merge

##### Usage

	java gitlet.Main merge [branch name]
	
Merges the given branch into the current branch. Merges combine the files stored by each branch's latest commit. A merge compares files of each branch to the files contained at the "split point" of the branches, or the latest common ancestor commitment.

Any files that have been modified differently in each branch are in "merge conflict" and are treated specialy. The contents of each file are combined into a single file, with the contents of the current branch's version on top, and the merged branch's contents on bottom.



<details><summary><b>Examples</b></summary>

<p align="center">
<img src="./src/merge 1.png" alt="Demo of file contents after a merge conflict" height="auto" width="85%">

</details>



## Files

* **gitlet/commit.java** stores data for a single commitment including the parent commitments and tracked files, and gets serialized and saved locally. Also contains helper functions for creating commitments.
* **gitlet/Dumpable.java** and **gitlet/GitletException.java** are files originally provided with the spec and used by gitlet/Utils.java.
* **gitlet/Main.java** is the driver class for Gitlet, simply checking that input parameters are valid, and running functions after parsing the input.
* **gitlet/repository.java** stores the latest commitment of a particular branch, and gets serialized and saved locally. Repositories are saved by their branch name, and a "HEAD" file containing the name of the current branch is also saved. Also contains helper functions for manipulating the current repository. 
* **gitlet/Utils.java** was originally provided with the spec and contains utility functions for manipulating files and utilising SHA-1 encryption. 

## Installation
1. [Download](https://github.com/DevonMartin/Gitlet/archive/refs/heads/main.zip) this repository. *README.md*, *LICENSE*, the *src* folder and the *testing* folder can be deleted.
2. In a terminal window at the directory of the downloaded files, type ```make```.

And that's it! Any files in this directory can now be backed up with Gitlet.
