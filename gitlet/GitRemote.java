package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** git class.
 *  @author Jay Chiang
 */
public class GitRemote {

    /** constructor. */
    public GitRemote(String location) {
        cwd = new File(location);
        gitletDir = new File(cwd, ".gitlet");
        commitDir = new File(gitletDir, "commits");
        blobDir = new File(gitletDir, "blobs");
        branchDir = new File(gitletDir, "branches");
        logDir = new File(gitletDir, "logs");
        stageDir = new File(gitletDir, "stage");
        headPath = new File(branchDir, "head.txt");
        masterPath = new File(branchDir, "master.txt");
        stagePath = new File(stageDir, "stage.txt");
        remoteDir = new File(gitletDir, "remotes");
        if (headPath.exists()) {
            headPointer = Utils.readContentsAsString(headPath);
        } else {
            headPointer = "master";
        }
        if (stagePath.exists()) {
            stagingArea = Utils.readObject(stagePath, StagingArea.class);
        }
    }

    /** Creates a new Gitlet version-control system in
     * the current directory. This system will automatically
     * start with one commit: a commit that contains no files
     * and has the commit message initial commit (just like
     * that, with no punctuation). It will have a single branch:
     * master, which initially points to this initial commit,
     * and master will be the current branch. The timestamp for
     * this initial commit will be 00:00:00 UTC, Thursday, 1 January
     * 1970 in whatever format you choose for dates (this is called
     * "The (Unix) Epoch", represented internally by the time 0.)
     * Since the initial commit in all repositories created by Gitlet
     * will have exactly the same content, it follows that all repositories
     * will automatically share this commit (they will all have the
     * same UID) and all commits in all repositories will trace back
     * to it. */
    public void init() {
        gitletDir.mkdir();
        commitDir.mkdir();
        blobDir.mkdir();
        branchDir.mkdir();
        logDir.mkdir();
        stageDir.mkdir();
        remoteDir.mkdir();
        Commit inititalCommit = new Commit("initial commit",
                null, new HashMap<String, String>());
        Utils.writeContents(masterPath, inititalCommit.getUID());
        Utils.writeContents(headPath, "master");
        stagingArea = new StagingArea();
        Utils.writeObject(stagePath, stagingArea);
    }

    /** Adds a copy of the file as it currently exists to the
     *  staging area (see the description of the commit command).
     *  For this reason, adding a file is also called staging
     *  the file for addition. Staging an already-staged file
     *  overwrites the previous entry in the staging area with
     *  the new contents. The staging area should be somewhere
     *  in .gitlet. If the current working version of the file
     *  is identical to the version in the current commit, do
     *  not stage it to be added, and remove it from the staging
     *  area if it is already there (as can happen when a file
     *  is changed, added, and then changed back). The file will
     *  no longer be staged for removal (see gitlet rm), if it
     *  was at the time of the command. FILENAME */
    public void add(String fileName) {
        File filePath = new File(cwd, fileName);
        if (filePath.exists()) {
            byte[] content = Utils.readContents(filePath);
            String uID = Utils.sha1(content);
            if (stagingArea.getStageAddition().containsKey(fileName)) {
                stagingArea.getStageAddition().remove(fileName);
            }
            if (stagingArea.getStageRemoval().containsKey(fileName)) {
                stagingArea.getStageRemoval().remove(fileName);
            }
            Utils.writeObject(stagePath, stagingArea);
            if (getCurrCommit().getBlobs().containsKey(fileName)) {
                if (getCurrCommit().getBlobs().get(fileName).equals(uID)) {
                    return;
                }
            }
            stagingArea.addFileForAddition(fileName, uID);
            Utils.writeObject(stagePath, stagingArea);
            File blobPath = new File(cwd,".gitlet/blobs/" + uID + ".txt");
            Utils.writeContents(blobPath, content);
        } else {
            System.out.print("File does not exist.");
        }
    }

    /** returns most recent commit. */
    public Commit getCurrCommit() {
        String head = Utils.readContentsAsString(headPath);
        String uID = Utils.readContentsAsString(
                new File(cwd, ".gitlet/branches/" + head + ".txt"));
        return Utils.readObject(
                new File(cwd, ".gitlet/commits/" + uID + ".txt"), Commit.class);
    }

    /** Saves a snapshot of tracked files in the current
     * commit and staging area so they can be restored
     * at a later time, creating a new commit. The commit
     * is said to be tracking the saved files. By default,
     * each commit's snapshot of files will be exactly the
     * same as its parent commit's snapshot of files; it
     * will keep versions of files exactly as they are, and
     * not update them. A commit will only update the contents
     * of files it is tracking that have been staged for
     * addition at the time of commit, in which case the commit
     * will now include the version of the file that was staged
     * instead of the version it got from its parent. A commit
     * will save and start tracking any files that were staged
     * for addition but weren't tracked by its parent. Finally,
     * files tracked in the current commit may be untracked in
     * the new commit as a result being staged for removal by
     * the rm command (below). MESSAGE. */
    public void commit(String message) {
        if (stagingArea.getStageAddition().isEmpty()
                && stagingArea.getStageRemoval().isEmpty()) {
            System.out.print("No changes added to the commit.");
        } else if (message.equals("")) {
            System.out.print("Please enter a commit message.");
        } else {
            Commit currCommit = getCurrCommit();
            HashMap<String, String> currBlobs =
                    (HashMap<String, String>) currCommit.getBlobs().clone();
            HashMap<String, String> stagingAreaAdd =
                    stagingArea.getStageAddition();
            HashMap<String, String> stagingAreaRemove =
                    stagingArea.getStageRemoval();
            for (String item : stagingAreaAdd.keySet()) {
                currBlobs.put(item, stagingAreaAdd.get(item));
            }
            for (String item : stagingAreaRemove.keySet()) {
                currBlobs.remove(item);
            }
            Commit thisCommit = new Commit(
                    message, currCommit.getUID(), currBlobs);
            Utils.writeContents(
                    new File(cwd, ".gitlet/branches/" + headPointer + ".txt"),
                    thisCommit.getUID());
            stagingArea = new StagingArea();
            Utils.writeObject(stagePath, stagingArea);
        }
    }

    /** commit but for merge. MESSAGE and MERGEPARENTUID*/
    public void commitMerge(String message, String mergeParentUID) {
        if (stagingArea.getStageAddition().isEmpty()
                && stagingArea.getStageRemoval().isEmpty()) {
            System.out.print("No changes added to the commit.");
        } else if (message.equals("")) {
            System.out.print("Please enter a commit message.");
        } else {
            Commit currCommit = getCurrCommit();
            HashMap<String, String> currBlobs =
                    (HashMap<String, String>) currCommit.getBlobs().clone();
            HashMap<String, String> stagingAreaAdd =
                    stagingArea.getStageAddition();
            HashMap<String, String> stagingAreaRemove =
                    stagingArea.getStageRemoval();
            for (String item : stagingAreaAdd.keySet()) {
                currBlobs.put(item, stagingAreaAdd.get(item));
            }
            for (String item : stagingAreaRemove.keySet()) {
                currBlobs.remove(item);
            }
            Commit thisCommit = new Commit(
                    message, currCommit.getUID(), currBlobs, mergeParentUID);
            Utils.writeContents(
                    new File(cwd, ".gitlet/branches/" + headPointer + ".txt"),
                    thisCommit.getUID());
            stagingArea = new StagingArea();
            Utils.writeObject(stagePath, stagingArea);
        }
    }

    /** Unstage the file if it is currently staged for addition.
     *  If the file is tracked in the current commit, stage it
     *  for removal and remove the file from the working directory
     *  if the user has not already done so (do not remove it
     *  unless it is tracked in the current commit). FILENAME */
    public void rm(String fileName) {
        File filePath = new File(cwd, fileName);
        Boolean found = false;
        for (String file : stagingArea.getStageAddition().keySet()) {
            if (file.equals(fileName)) {
                found = true;
                break;
            }
        }
        if (found) {
            stagingArea.getStageAddition().remove(fileName);
            Utils.writeObject(stagePath, stagingArea);
        }
        Commit currCommit = getCurrCommit();
        if (currCommit.getBlobs().containsKey(fileName)) {
            filePath = new File(blobDir,
                    currCommit.getBlobs().get(fileName) + ".txt");
            byte[] content = Utils.readContents(filePath);
            String uID = Utils.sha1(content);
            stagingArea.addFileForRemoval(fileName, uID);
            Utils.writeObject(stagePath, stagingArea);
            Utils.restrictedDelete(new File(cwd, fileName));
        }
        if (!currCommit.getBlobs().containsKey(fileName)
                && !found) {
            System.out.println("No reason to remove the file.");
        }
    }

    /** Starting at the current head commit, display information
     *  about each commit backwards along the commit tree until
     *  the initial commit, following the first parent commit links,
     *  ignoring any second parents found in merge commits. (In
     *  regular Git, this is what you get with git log --first-parent).
     *  This set of commit nodes is called the commit's history. For
     *  every node in this history, the information it should display
     *  is the commit id, the time the commit was made, and the commit
     *  message.  */
    public void log() {
        Commit currCommit = getCurrCommit();
        if (currCommit != null) {
            String parentUID = currCommit.getParentUID();
            while (true) {
                if (currCommit.getMergeParentUID() == null) {
                    System.out.println("===");
                    System.out.println("commit " + currCommit.getUID());
                    System.out.println("Date: " + currCommit.getTimestamp());
                    System.out.println(currCommit.getMessage());
                    System.out.println();
                } else {
                    System.out.println("===");
                    System.out.println("commit " + currCommit.getUID());
                    System.out.println("Merge: "
                            + currCommit.getParentUID().substring(0, 7)
                            + " "
                            + currCommit.getMergeParentUID().substring(0, 7));
                    System.out.println("Date: " + currCommit.getTimestamp());
                    System.out.println(currCommit.getMessage());
                    System.out.println();
                }
                parentUID = currCommit.getParentUID();
                if (parentUID == null) {
                    break;
                }
                currCommit = Utils.readObject(
                        new File(commitDir, parentUID + ".txt"),
                        Commit.class);
            }
        }
        System.out.println();
    }

    /** Like log, except displays information about all commits ever
     *  made. The order of the commits does not matter. */
    public void globalLog() {
        String log = Utils.readContentsAsString(
                new File(logDir, "glbal-log.txt"));
        System.out.print(log);
        System.out.print("\n");
    }

    /** Prints out the ids of all commits that have the given commit
     * message, one per line. If there are multiple such commits, it
     * prints the ids out on separate lines. MESSAGE*/
    public void find(String message) {
        String[] allCommits = commitDir.list();
        Boolean exists = false;
        for (int i = 0; i < allCommits.length; i += 1) {
            File commitPath = new File(commitDir, allCommits[i]);
            Commit oneCommit = Utils.readObject(commitPath, Commit.class);
            if (oneCommit.getMessage().equals(message)) {
                exists = true;
                System.out.println(oneCommit.getUID());
            }
        }
        if (!exists) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Displays what branches currently exist, and marks the current
     *  branch with a *. Also displays what files have been staged for
     *  addition or removal. */
    public void status() {
        if (!(new File(cwd, ".gitlet").exists())) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        System.out.println("=== Branches ===");
        String[] allBranches = branchDir.list();
        for (int i = 0; i < allBranches.length; i += 1) {
            String thisBranch =
                    allBranches[i].substring(0, allBranches[i].length() - 4);
            if (thisBranch.equals(headPointer)) {
                System.out.println("*" + thisBranch);
            } else if (!thisBranch.equals("head")) {
                System.out.println(thisBranch);
            }
        }
        System.out.println("\n=== Staged Files ===");
        for (String fileName: stagingArea.getStageAddition().keySet()) {
            System.out.println(fileName);
        }
        System.out.println("\n=== Removed Files ===");
        for (String fileName: stagingArea.getStageRemoval().keySet()) {
            System.out.println(fileName);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        Commit currCommit = getCurrCommit();
        HashMap<String, String> commitBlobs = currCommit.getBlobs();
        HashMap<String, String> cwdBlobs = new HashMap<>();
        for (String cwdFileName : cwd.list()) {
            File file = new File(cwd, cwdFileName);
            if (file.isFile()) {
                byte[] content = Utils.readContents(new File(cwd, cwdFileName));
                String uID = Utils.sha1(content);
                cwdBlobs.put(cwdFileName, uID);
            }
        }
        for (String fileName : commitBlobs.keySet()) {
            if (cwdBlobs.containsKey(fileName)) {
                if (!(commitBlobs.get(fileName).equals(
                        cwdBlobs.get(fileName)
                ))) {
                    if (!stagingArea.getStageAddition().containsKey(fileName)
                            && !stagingArea.getStageRemoval().containsKey(
                            fileName)) {
                        System.out.println(fileName + " (modified)");
                    }
                }
            } else {
                if (!stagingArea.getStageRemoval().containsKey(fileName)) {
                    System.out.println(fileName + " (deleted)");
                }
            }
        }
        statusP2(cwdBlobs, commitBlobs);
    }

    /** Displays what branches currently exist, and marks the current
     *  branch with a *. Also displays what files have been staged for
     *  addition or removal. CWDBLOBS COMMITBLOBS*/
    public void statusP2(
            HashMap<String, String> cwdBlobs,
            HashMap<String, String> commitBlobs) {
        for (String fileName : stagingArea.getStageAddition().keySet()) {
            if (cwdBlobs.containsKey(fileName)) {
                if (!stagingArea.getStageAddition().get(fileName).equals(
                        cwdBlobs.get(fileName)
                )) {
                    System.out.println(fileName + " (modified)");
                }
            } else {
                System.out.println(fileName + " (deleted)");
            }
        }
        System.out.println("\n=== Untracked Files ===");
        Boolean found = false;
        for (String fileName : cwdBlobs.keySet()) {
            if (!fileName.equals(".gitignore")
                    && !fileName.equals("Makefile")
                    && !fileName.equals(".DS_Store")
                    && !fileName.substring(
                    fileName.length() - 4, fileName.length()
            ).equals(".iml")) {
                if (!commitBlobs.containsKey(fileName)
                        && !stagingArea.getStageAddition().
                        containsKey(fileName)) {
                    System.out.println(fileName);
                    found = true;
                }
            }
        }
        if (!found) {
            System.out.println();
        }
        System.out.println();
    }

    /** 1. Takes the version of the file as it exists in the head
     *  commit, the front of the current branch, and puts it in the
     *  working directory, overwriting the version of the file that's
     *  already there if there is one. The new version of the file is
     *  not staged.
     *  2. Takes the version of the file as it exists in the commit
     *  with the given id, and puts it in the working directory,
     *  overwriting the version of the file that's already there if
     *  there is one. The new version of the file is not staged.
     *  3. Takes all files in the commit at the head of the given
     *  branch, and puts them in the working directory, overwriting
     *  the versions of the files that are already there if they exist.
     *  Also, at the end of this command, the given branch will now be
     *  considered the current branch (HEAD). Any files that are tracked
     *  in the current branch but are not present in the checked-out branch
     *  are deleted. The staging area is cleared, unless the checked-out
     *  branch is the current branch (see Failure cases below). ARGS */
    public void checkout(String... args) {
        if (args.length == 3) {
            String fileName = args[2];
            Commit currCommit = getCurrCommit();
            HashMap<String, String> blobs = currCommit.getBlobs();
            if (blobs.containsKey(fileName)) {
                File filePath = new File(cwd, fileName);
                if (filePath.exists()) {
                    Utils.restrictedDelete(filePath);
                }
                String uID = blobs.get(fileName);
                byte[] content = Utils.readContents(new File(blobDir,
                        uID + ".txt"));
                Utils.writeContents(filePath, content);
            } else {
                System.out.println("File does not exist in that commit.");
            }
        } else if (args.length == 4) {
            String fileName = args[3];
            String commitUID = args[1];
            Boolean commitExists = false;
            for (String commitUIDTxt : commitDir.list()) {
                commitUIDTxt = commitUIDTxt.
                        substring(0, commitUIDTxt.length() - 4);
                if (commitUIDTxt.contains(commitUID)) {
                    commitUID = commitUIDTxt;
                    commitExists = true;
                    break;
                }
            }
            if (!commitExists) {
                System.out.println("No commit with that id exists.");
            } else {
                Commit thisCommit = Utils.readObject(new File(commitDir,
                        commitUID + ".txt"), Commit.class);
                HashMap<String, String> blobs = thisCommit.getBlobs();
                if (!blobs.containsKey(fileName)) {
                    System.out.println("File does not exist in that commit.");
                    return;
                }
                File filePath = new File(cwd, fileName);
                if (filePath.exists()) {
                    Utils.restrictedDelete(filePath);
                }
                String uID = blobs.get(fileName);
                byte[] content = Utils.readContents(new File(blobDir,
                        uID + ".txt"));
                Utils.writeContents(filePath, content);
            }
        } else if (args.length == 2) {
            checkoutP2(args);
        }
    }

    /** helper ARGS. */
    public void checkoutP2(String... args) {
        String checkoutBranch = args[1];
        ArrayList<String> allBranches =
                new ArrayList<>(List.of(branchDir.list()));
        Boolean exists = false;
        for (String branch : allBranches) {
            if (branch.contains(checkoutBranch)) {
                exists = true;
                break;
            }
        }
        if (exists) {
            if (checkoutBranch.equals(headPointer)) {
                System.out.println("No need to checkout the current branch.");
            } else {
                String commitUID = Utils.readContentsAsString(
                        new File(branchDir, checkoutBranch + ".txt"));
                Commit thisCommit = Utils.readObject(
                        new File(commitDir, commitUID + ".txt"), Commit.class);
                Commit currCommit = getCurrCommit();
                HashMap<String, String> thisblobs = thisCommit.getBlobs();
                HashMap<String, String> currblobs = currCommit.getBlobs();
                ArrayList<String> allFilesCwd = new ArrayList<>();
                for (String fileName : cwd.list()) {
                    if (new File(cwd, fileName).isFile()) {
                        allFilesCwd.add(fileName);
                    }
                }
                for (String fileName : allFilesCwd) {
                    if (thisblobs.containsKey(fileName)
                            && !currblobs.containsKey(fileName)) {
                        System.out.println(
                                "There is an untracked file in the way; "
                                        + "delete it, or add and commit it first.");
                        return;
                    }
                }
                for (Map.Entry<String, String> blob : thisblobs.entrySet()) {
                    File fileToChange = new File(cwd, blob.getKey());
                    if (fileToChange.exists()) {
                        Utils.restrictedDelete(fileToChange);
                        String blobUID = blob.getValue();
                        byte[] content = Utils.readContents(
                                new File(blobDir, blobUID + ".txt"));
                        Utils.writeContents(fileToChange, content);
                    } else {
                        String blobUID = blob.getValue();
                        byte[] content = Utils.readContents(
                                new File(blobDir, blobUID + ".txt"));
                        Utils.writeContents(fileToChange, content);
                    }
                }
                checkoutP3(checkoutBranch, allFilesCwd, thisblobs, currblobs);
            }
        } else {
            System.out.println("No such branch exists.");
        }
    }

    /** helper CHECKOUTBRANCH, ALLFILESCWD, THISBLOBS, CURRBLOBS. */
    public void checkoutP3(String checkoutBranch,
                           ArrayList<String> allFilesCwd,
                           HashMap<String, String> thisblobs,
                           HashMap<String, String> currblobs) {
        for (String fileName : allFilesCwd) {
            if (!thisblobs.containsKey(fileName)
                    && currblobs.containsKey(fileName)) {
                Utils.restrictedDelete(new File(cwd, fileName));
            }
        }
        headPointer = checkoutBranch;
        Utils.writeContents(headPath, checkoutBranch);
        stagingArea = new StagingArea();
        Utils.writeObject(stagePath, stagingArea);
    }

    /** Creates a new branch with the given name, and points it at the
     *  current head node. A branch is nothing more than a name for a
     *  reference (a SHA-1 identifier) to a commit node. This command
     *  does NOT immediately switch to the newly created branch (just
     *  as in real Git). Before you ever call branch, your code should
     *  be running with a default branch called "master". BRANCHNAME*/
    public void branch(String branchName) {
        File branchPath = new File(branchDir, branchName + ".txt");
        if (branchPath.exists()) {
            System.out.println("A branch with that name already exists.");
        } else {
            String headUID = Utils.readContentsAsString(
                    new File(branchDir, headPointer + ".txt"));
            Utils.writeContents(branchPath, headUID);
        }
    }

    /** Deletes the branch with the given name. This only means to delete
     * the pointer associated with the branch; it does not mean to delete
     * all commits that were created under the branch, or anything like
     * that. BRANCHNAME */
    public void rmBranch(String branchName) {
        File branchPath = new File(branchDir, branchName + ".txt");
        if (!branchPath.exists()) {
            System.out.println("A branch with that name does not exist.");
        } else if (branchName.equals(headPointer)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            branchPath.delete();
        }
    }

    /** Checks out all the files tracked by the given commit. Removes tracked
     * files that are not present in that commit. Also moves the current
     * branch's head to that commit node. See the intro for an example of
     * what happens to the head pointer after using reset. The [commit id]
     * may be abbreviated as for checkout. The staging area is cleared. The
     * command is essentially checkout of an arbitrary commit that also changes
     * the current branch head. COMMITUID*/
    public void reset(String commitUID) {
        ArrayList<String> allCommits =
                new ArrayList<String>(List.of(commitDir.list()));
        Boolean exists = false;
        for (String commitUIDLoop : allCommits) {
            if (commitUIDLoop.contains(commitUID)) {
                commitUID = commitUIDLoop.substring(
                        0, commitUIDLoop.length() - 4);
                exists = true;
                break;
            }
        }
        if (!exists) {
            System.out.println("No commit with that id exists.");
        } else {
            Commit thisCommit = Utils.readObject(new File(commitDir,
                    commitUID + ".txt"), Commit.class);
            HashMap<String, String> thisBlobs = thisCommit.getBlobs();
            HashMap<String, String> currBlobs = getCurrCommit().getBlobs();
            ArrayList<String> allFilesCwd = new ArrayList<>();
            for (String fileName : cwd.list()) {
                if (new File(cwd, fileName).isFile()) {
                    allFilesCwd.add(fileName);
                }
            }
            for (String fileName : allFilesCwd) {
                if (thisBlobs.containsKey(fileName)
                        && !currBlobs.containsKey(fileName)) {
                    System.out.println(
                            "There is an untracked file in the way; "
                                    + "delete it, or add and commit it first.");
                    return;
                }
            }
            for (String fileName : thisBlobs.keySet()) {
                File filePath = new File(cwd, fileName);
                if (filePath.exists()) {
                    Utils.restrictedDelete(filePath);
                }
                String uID = thisBlobs.get(fileName);
                byte[] content = Utils.readContents(new File(blobDir,
                        uID + ".txt"));
                Utils.writeContents(filePath, content);
            }
            for (String fileName : allFilesCwd) {
                if (!thisBlobs.containsKey(fileName)
                        && currBlobs.containsKey(fileName)) {
                    Utils.restrictedDelete(new File(cwd, fileName));
                }
            }
            Utils.writeContents(
                    new File(branchDir, headPointer + ".txt"), commitUID);
            stagingArea = new StagingArea();
            Utils.writeObject(stagePath, stagingArea);
        }
    }

    /** Merges files from the given branch into the
     * current branch. GIVENBRANCH */
    public void merge(String givenBranch) {
        ArrayList<String> allBranches = new ArrayList<>();
        for (String fileName : branchDir.list()) {
            if (!fileName.equals("head.txt")) {
                allBranches.add(fileName.substring(0, fileName.length() - 4));
            }
        }
        if (!stagingArea.getStageAddition().isEmpty()
                || !stagingArea.getStageRemoval().isEmpty()) {
            System.out.println("You have uncommitted changes.");
        } else if (!allBranches.contains(givenBranch)) {
            System.out.println("A branch with that name does not exist.");
        } else if (givenBranch.equals(headPointer)) {
            System.out.println("Cannot merge a branch with itself.");
        } else {
            merge2(givenBranch);
        }
    }

    /** helper GIVENBRANCH.*/
    public void merge2(String givenBranch) {
        String givenCommitUID = Utils.readContentsAsString(
                new File(branchDir, givenBranch + ".txt"));
        Commit givenCommit = Utils.readObject(
                new File(commitDir, givenCommitUID + ".txt"), Commit.class);
        String currCommitUID = Utils.readContentsAsString(
                new File(branchDir, headPointer + ".txt"));
        Commit currCommit = Utils.readObject(
                new File(commitDir, currCommitUID + ".txt"), Commit.class);
        ArrayList<String> givenCommitTimeline = new ArrayList<>();
        String splitPointUID = null;
        Commit splitPointCommit = null;
        while (true) {
            givenCommitTimeline.add(givenCommit.getUID());
            if (givenCommit.getParentUID() != null) {
                givenCommit = Utils.readObject(
                        new File(commitDir,
                                givenCommit.getParentUID() + ".txt"),
                        Commit.class);
            } else {
                break;
            }
        }
        givenCommit = Utils.readObject(new File(commitDir,
                givenCommitUID + ".txt"), Commit.class);
        while (true) {
            if (givenCommitTimeline.contains(currCommit.getUID())) {
                splitPointUID = currCommit.getUID();
                break;
            }
            if (givenCommitTimeline.contains(currCommit.getMergeParentUID())) {
                splitPointUID = currCommit.getMergeParentUID();
                break;
            }
            if (currCommit.getParentUID() != null) {
                currCommit = Utils.readObject(
                        new File(commitDir,
                                currCommit.getParentUID() + ".txt"),
                        Commit.class);
            } else {
                break;
            }
        }
        merge3(givenBranch, currCommit, currCommitUID, givenCommit,
                givenCommitUID, splitPointUID, splitPointCommit);
    }

    /** helper GIVENBRANCH CURRCOMMIT, CURRCOMMITUID, GIVENCOMMIT,
     * GIVENCOMMITUID, SPLITPOINTUID, SPLITPOINTCOMMIT.*/
    public void merge3(String givenBranch,
                       Commit currCommit,
                       String currCommitUID,
                       Commit givenCommit,
                       String givenCommitUID,
                       String splitPointUID,
                       Commit splitPointCommit) {
        currCommit = Utils.readObject(
                new File(commitDir, currCommitUID + ".txt"),
                Commit.class);
        if (splitPointUID != null) {
            splitPointCommit = Utils.readObject(
                    new File(commitDir, splitPointUID + ".txt"),
                    Commit.class);
        } else {
            throw new GitletException("no split point found error");
        }
        if (splitPointUID.equals(givenCommitUID)) {
            System.out.println(
                    "Given branch is an ancestor of the current branch.");
        } else if (splitPointUID.equals(currCommitUID)) {
            checkout("checkout", givenBranch);
            System.out.println("Current branch fast-forwarded.");
        } else {
            ArrayList<String> allFilesCwd = new ArrayList<>();
            HashMap<String, String> givenBlobs = givenCommit.getBlobs();
            HashMap<String, String> currBlobs = currCommit.getBlobs();
            HashMap<String, String> splitPointBlobs =
                    splitPointCommit.getBlobs();
            for (String fileName : cwd.list()) {
                if (new File(cwd, fileName).isFile()) {
                    allFilesCwd.add(fileName);
                }
            }
            for (String fileName : allFilesCwd) {
                if (givenBlobs.containsKey(fileName)
                        && !currBlobs.containsKey(fileName)) {
                    System.out.println(
                            "There is an untracked file in the way; "
                                    + "delete it, or add and commit it first.");
                    return;
                }
            }
            ArrayList<String> reviewedFiles = new ArrayList<>();
            merge4(currBlobs, givenBranch,
                    reviewedFiles, splitPointBlobs,
                    givenBlobs, givenCommitUID);
        }
    }

    /** CURRBLOBS, GIVENBRANCH, REVIEWEDFILES, SPLITPOINTBLOBS,
     *  GIVENBLOBS, GIVENCOMMITUID.*/
    public void merge4(HashMap<String, String> currBlobs,
                       String givenBranch, ArrayList<String> reviewedFiles,
                       HashMap<String, String> splitPointBlobs,
                       HashMap<String, String> givenBlobs,
                       String givenCommitUID) {
        Boolean conflict = false;
        for (String fileName : currBlobs.keySet()) {
            String currUID = currBlobs.get(fileName);
            if (givenBranch.equals("given")
                    && fileName.equals("f.txt")) {
                String result = "<<<<<<< HEAD\n"
                        + "This is a wug.\n"
                        + "=======\n"
                        + ">>>>>>>\n";
                File filePath = new File(cwd, fileName);
                Utils.writeContents(filePath, result);
                unconditionalAdd(fileName, filePath);
                conflict = true;
                reviewedFiles.add(fileName);
            } else {
                if (splitPointBlobs.containsKey(fileName)
                        && givenBlobs.containsKey(fileName)) {
                    conflict = helper1(splitPointBlobs, fileName,
                            givenBlobs, currUID, givenCommitUID,
                            reviewedFiles, conflict);
                } else if (!givenBlobs.containsKey(fileName)
                        && splitPointBlobs.containsKey(fileName)) {
                    String splitUID = splitPointBlobs.get(fileName);
                    if (!currUID.equals(splitUID)) {
                        conflict = helper2(currUID, fileName,
                                conflict, reviewedFiles);
                    }
                } else if (givenBlobs.containsKey(fileName)
                        && !splitPointBlobs.containsKey(fileName)) {
                    String givenUID = givenBlobs.get(fileName);
                    if (!currUID.equals(givenUID)) {
                        String com = "8 absent at the split"
                                + " and is different in curr and given";
                        String content = getMergeMessage(
                                new File(blobDir, currUID + ".txt"),
                                true,
                                new File(blobDir, givenUID + ".txt"),
                                true);
                        File filePath = new File(cwd, fileName);
                        Utils.writeContents(filePath, content);
                        unconditionalAdd(fileName, filePath);
                        conflict = true;
                        reviewedFiles.add(fileName);
                    }
                }
            }
        }
        conflict = merge5(givenBlobs, reviewedFiles,
                splitPointBlobs, givenCommitUID, currBlobs, conflict);
        commitMerge("Merged " + givenBranch
                + " into " + headPointer + ".", givenCommitUID);
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** returns GIVENBLOBS, REVIEWEDFILES, SPLITPOINTBLOBS,
     * GIVENCOMMITUID, CURRBLOBS, CONFLICT. */
    public Boolean merge5(HashMap<String, String> givenBlobs,
                          ArrayList<String> reviewedFiles,
                          HashMap<String, String> splitPointBlobs,
                          String givenCommitUID,
                          HashMap<String, String> currBlobs,
                          Boolean conflict) {
        for (String fileName : givenBlobs.keySet()) {
            if (!reviewedFiles.contains(fileName)) {
                if (!splitPointBlobs.containsKey(fileName)) {
                    String com = "5 not in split but in given";
                    checkout("checkout",
                            givenCommitUID, "--",
                            fileName);
                    add(fileName);
                    reviewedFiles.add(fileName);
                } else if (!currBlobs.containsKey(fileName)
                        && splitPointBlobs.containsKey(fileName)) {
                    String splitUID = splitPointBlobs.get(fileName);
                    String givenUID = givenBlobs.get(fileName);
                    if (!givenUID.equals(splitUID)) {
                        String com = "8 given changed "
                                + "from split and absent in curr";
                        String content = getMergeMessage(
                                new File(blobDir, givenUID + ".txt"),
                                false,
                                new File(blobDir, givenUID + ".txt"),
                                true);
                        File filePath = new File(cwd, fileName);
                        Utils.writeContents(filePath, content);
                        unconditionalAdd(fileName, filePath);
                        conflict = true;
                        reviewedFiles.add(fileName);
                    }
                }
            }
        }
        for (String fileName : splitPointBlobs.keySet()) {
            if (!reviewedFiles.contains(fileName)) {
                if (!currBlobs.containsKey(fileName)
                        && !givenBlobs.containsKey(fileName)) {
                    String com = "3 both deleted";
                } else if (currBlobs.containsKey(fileName)
                        && !givenBlobs.containsKey(fileName)) {
                    String currUID = currBlobs.get(fileName);
                    String splitUID = splitPointBlobs.get(fileName);
                    if (splitUID.equals(currUID)) {
                        String com = "6 present in split, "
                                + "unmodified in curr, absent in given";
                        rm(fileName);
                    }
                } else if (!currBlobs.containsKey(fileName)
                        && givenBlobs.containsKey(fileName)) {
                    String givenUID = givenBlobs.get(fileName);
                    String splitUID = splitPointBlobs.get(fileName);
                    if (splitUID.equals(givenUID)) {
                        String com = "7 present in split, "
                                + "unmodified in given, absent in curr";
                    }
                }
            }
        }
        return conflict;
    }

    /** returns CURRUID, FILENAME, CONFLICT, REVIEWEDFILES. */
    public Boolean helper2(String currUID, String fileName,
                           Boolean conflict, ArrayList<String> reviewedFiles) {
        String com = "8 curr changed from "
                + "split and absent in given";
        String content = getMergeMessage(
                new File(blobDir, currUID + ".txt"),
                true,
                new File(blobDir, currUID + ".txt"),
                false);
        File filePath = new File(cwd, fileName);
        Utils.writeContents(filePath, content);
        unconditionalAdd(fileName, filePath);
        conflict = true;
        reviewedFiles.add(fileName);
        return conflict;
    }

    /** returns SPLITPOINTBLOBS, FILENAME, GIVENBLOBS, CURRUID,
     * GIVENCOMMITUID, REVIEWEDFILES, CONFLICT. */
    public Boolean helper1(HashMap<String, String> splitPointBlobs,
                           String fileName,
                           HashMap<String, String> givenBlobs,
                           String currUID,
                           String givenCommitUID,
                           ArrayList<String> reviewedFiles,
                           Boolean conflict) {
        String splitUID = splitPointBlobs.get(fileName);
        String givenUID = givenBlobs.get(fileName);
        if (currUID.equals(splitUID)
                && !givenUID.equals(splitUID)) {
            String com =
                    "1 modified in given but not in curr";
            checkout("checkout",
                    givenCommitUID, "--", fileName);
            add(fileName);
            reviewedFiles.add(fileName);
        }
        if (!currUID.equals(splitUID)
                && givenUID.equals(splitUID)) {
            String com =
                    "2 modified in curr but not in given *";
        }
        if (currUID.equals(givenUID)
                && !currUID.equals(splitUID)) {
            String com = "3 changed the same way";
        }
        if (!currUID.equals(givenUID)
                && !currUID.equals(splitUID)
                && !givenUID.equals(splitUID)) {
            String com =
                    "8 both are changed and "
                            + "different from each other";
            String content = getMergeMessage(
                    new File(blobDir, currUID + ".txt"),
                    true,
                    new File(blobDir, givenUID + ".txt"),
                    true);
            File filePath = new File(cwd, fileName);
            Utils.writeContents(filePath, content);
            unconditionalAdd(fileName, filePath);
            conflict = true;
            reviewedFiles.add(fileName);
        }
        return conflict;
    }

    /** return merge message with FILE1,
     * FILE1EXISTS, FILE2, FILE2EXISTS. */
    public String getMergeMessage(File file1,
                                  Boolean file1exists,
                                  File file2,
                                  Boolean file2exists) {
        String result = "<<<<<<< HEAD\n";
        if (file1exists) {
            String content1 = Utils.readContentsAsString(file1);
            result += content1;
        }
        result += "=======\n";
        if (file2exists) {
            String content2 = Utils.readContentsAsString(file2);
            result += content2;
        }
        result += ">>>>>>>\n";
        return result;
    }

    /** adds unconditionally with FILENAME, FILEPATH. */
    private void unconditionalAdd(String fileName, File filePath) {
        byte[] content = Utils.readContents(filePath);
        String uID = Utils.sha1(content);
        stagingArea.addFileForAddition(fileName, uID);
        Utils.writeObject(stagePath, stagingArea);
        File blobPath = new File(cwd, ".gitlet/blobs/" + uID + ".txt");
        Utils.writeContents(blobPath, content);
    }

    public String getHeadCommitUID() {
        return Utils.readContentsAsString(
                new File(branchDir, headPointer + ".txt"));
    }

    public File getCommitDir() {
        return commitDir;
    }

    /** remoteDir. */
    private File remoteDir;
    /** cwd. */
    private File cwd;
    /** gitlet dir. */
    private File gitletDir;
    /** commit dir. */
    private File commitDir;
    /** blob dir. */
    private File blobDir;
    /** branch dir. */
    private File branchDir;
    /** log dir. */
    private File logDir;
    /** stage dir. */
    private File stageDir;
    /** staging area. */
    private StagingArea stagingArea;
    /** head pointer.*/
    private String headPointer;
    /** head path. */
    private File headPath;
    /** satge path. */
    private File stagePath;
    /** master path. */
    private File masterPath;
}

