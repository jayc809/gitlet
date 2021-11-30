package gitlet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Jay Chiang
 */
public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        Git git = new Git();
        if (args.length == 0) {
            System.out.println("Please enter a command.");
        } else {
            if (args[0].equals("init") && checkInput(1, args)) {
                git.init();
            } else if (args[0].equals("add") && checkInput(2, args)) {
                git.add(args[1]);
            } else if (args[0].equals("commit") && checkInput(2, args)) {
                git.commit(args[1]);
            } else if (args[0].equals("rm") && checkInput(2, args)) {
                git.rm(args[1]);
            } else if (args[0].equals("log") && checkInput(1, args)) {
                git.log();
            } else if (args[0].equals("global-log") && checkInput(1, args)) {
                git.globalLog();
            } else if (args[0].equals("find") && checkInput(2, args)) {
                git.find(args[1]);
            } else if (args[0].equals("status") && checkInput(1, args)) {
                git.status();
            } else if (args[0].equals("checkout")) {
                if (!(args.length == 2
                    || (args.length == 3 && args[1].equals("--"))
                    || (args.length == 4 && args[2].equals("--"))
                    )) {
                    System.out.println("Incorrect Operands");
                } else {
                    git.checkout(args);
                }
            } else if (args[0].equals("branch") && checkInput(2, args)) {
                    git.branch(args[1]);
            } else if (args[0].equals("rm-branch") && checkInput(2, args)) {
                git.rmBranch(args[1]);
            } else if (args[0].equals("reset") && checkInput(2, args)) {
                git.reset(args[1]);
            } else if (args[0].equals("merge") && checkInput(2, args)) {
                git.merge(args[1]);
            } else if (args[0].equals("add-remote") && checkInput(3, args)) {
                git.addRemote(args[1], args[2]);
            } else if (args[0].equals("rm-remote") && checkInput(2, args)) {
                git.rmRemote(args[1]);
            } else {
                System.out.println("No command with that name exists.");
            }
        }
    }

    /** returns validity of input uising N and ARGS. */
    public static Boolean checkInput(int n, String... args) {
        if (args.length != n) {
            System.out.println("Incorrect Operands");
            return false;
        }
        return true;
    }
}
