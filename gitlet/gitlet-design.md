# Gitlet Design Document

**Author: Jay Chiang**

## Classes and Data Structures

###Blob
This class stores the contents of a file

###Commit
This class contains log messages, other metadata
(commit date, author, etc.), references to blobs, 
and references to 
parent commits. The repository also maintains a 
mapping from branch heads (in this course, we've 
used names like master, proj2, etc.) to references
to commits

###Main
This class contains the methods that executes the 
equivalent git commands and the references to the
head and master commits

## Algorithms

###Blob Class
1. Blob(String name, File file): 
The class constructor. Records the name, 
reference to a file, UID, and the current 
version (1 by default). 
2. updateBlob(File file):
Returns a new Blob with the same name 
and UID, but references another file and 
increments version by 1.

###Commit
1. Commit(String message, String author):
The class constructor. Records the log message,
commit date, author, UID, references to Blobs, and
references to parent commits, and name of branch heads. 
2. addBlob(Blob blob): 
Adds a new Blob to the list of 
references to Blobs.
3. addChild(Commit commit):
Adds commit to child and sets parent of commit to this. 

###Main
1. main(String[] args): 
decides which command (method) to run based on the args
provided using a switch. 
2. init():
Instantiates a new gitlet directory containing an initial
commit containing a no files and has the commit message 
"initial commit". Adds one branch (master), which initially 
points to this initial commit, and set head branch to master.
Sets timestamp for this initial commit to be 00:00:00 UTC,
Thursday, 1 January 1970. Does not run if there is already a 
gitlet directory. 
3. add(File file):
Checks if file exists in the staging area. If does and is
same as the current working version, remove it. If does and
is not the same as the current working version, replace it.
If does not exist, add it to the staging area. Prints "File
does not exist" if cannot find file. 
4. commit(String message):
Instantiate a new commit with objects from staging area.
Clears the staging area. Uses addChild to add the current
commit to the commit tree. Prints "No changes added to the commit" 
if staging area empty. Prints "Please enter a commit message" if
not message is provided. 
5. rm(File file):
Removes file from staging area. If file is tracked in the
current commit, remove it from current directory. if file 
not staged nor tracked by head commit, prints "No reason to remove the file"
6. log():
Traverse the current branch from inital commit and print out
each commit's UID, data, and message.
7. global-log():
Traverse all branches from inital commit and print out
each commit's UID, data, and message.
8. find(String message):
Traverse entire commit tree and print out the UID of
commit trees whose message is the same as message. If no
message found, prints "Found no commit with that message"
9. status():
Prints out list of branches and specify current branch. 
Prints out objects in the staging area (added and modified).
Prints out objects that have been removed.
Prints out files that are in the current directory but not 
in the staging area (untracked).
10. checkout(String fileName):
    If the file does not exist in the previous commit, abort, prints "File does not exist in that commit".
    If no commit with the given id exists, prints "No commit with that id exists".
    If the file does not exist in the given commit, prints "File does not exist in that commit".
    If no branch with that name exists, prints "No such branch exists."
    If that branch is the current branch, prints "No need to checkout the current branch".
    If a working file is untracked in the current branch and would be overwritten by the checkout,
    prints "There is an untracked file in the way; delete it, or add and commit it first" and exit.
Otherwise, if file exists in working directory, replace it with the 
head's. If the file is not in working directory, add the head's.
11. checkout(String commitUID, String filName):
Traverse through the commit tree and find the file with 
commitUID and add/replace it in the working directory.
If a branch with the given name already exists, 
prints "A branch with that name already exists".
12. checkout(String branchName):
Takes all files in the commit at the head of the given
branch, and puts them in the working directory, 
overwriting the versions of the files that are 
already there if they exist. Set head to given branch. 
Remove other files in working directory. Clear staging area.
13. branch(String name):
Add a new branch to the commit tree by instantiating a
Commit. 
14. rm-branch(String name):
Traverse through commit tree and delete given branch.
If a branch with the given name does not exist, aborts and
prints "A branch with that name does not exist".
If you try to remove the branch you're currently on, 
aborts and prints "Cannot remove the current branch".
15. reset(String commitUID):
    If no commit with the given id exists,
    prints "No commit with that id exists".
    If a working file is untracked in the current branch
    and would be overwritten by the reset, prints
    "There is an untracked file in the way; delete it,
    or add and commit it first" and exits.
Otherwise, checks out all the files tracked by the given commit. 
Removes tracked files that are not present in that 
commit. Also moves the current branch's head to that
commit node.
16. merge(String branchName):
Find split point by traversing the tree backwards from 
current and given branch until the two meet. For all files
contained in split, current, given, do the following: 1. If
file is modified in given but not in current, keep given. 2. If
file is modified in current but not in given, keep current. 3.
If file is modified the same way, keep either. 4. If file is
modified differently, raise conflict. 5. If file not in split
nor given but in current, keep current. 6. If file not in 
split nor current but in given, keep given. 7. If file not 
modified in current but not in given, remove file. 8. If file
not modified in given but not in current, remove file. 

## Persistence

1. The entire commit tree after every operation that
would modify it is automatically written to the gitlet
directory using writeObject. The head and master branches
are also recorded. 
2. After every commit, the log is overwritten and stored
in the directory using writeObject
3. After every commit, the contents of working directory is
written to the gitlet diretory using writeObject. 
4. after every add, remove, or modify operation, the staging
area is written into the gitlet directory using writeObject.


