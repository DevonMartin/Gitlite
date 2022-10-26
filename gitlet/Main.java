package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author Devon Martin
 */
public class Main {
    /** Helper function to ensure that the number of arguments
     * is correct for each command. Errors and exits program
     * if not.
     * @param args The arguments array passed into main
     * @param n    The number of required arguments
     */
    private static void paramLenCheck(String[] args, int n) {
        if (args.length != n) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    static Repository repo = null;

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String firstArg = args[0];
        if (firstArg.equals("init")) {
            paramLenCheck(args, 1);
            new Repository();
        } else if (!Repository.inRepo()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else {
            repo = Repository.loadHead();
            switch (firstArg) {
                case "add":
                    paramLenCheck(args, 2);
                    repo.add(args[1]);
                    break;
                case "commit":
                    paramLenCheck(args, 2);
                    repo.commit(args[1]);
                    break;
                case "rm":
                    paramLenCheck(args, 2);
                    repo.rm(args[1]);
                    break;
                case "log":
                    paramLenCheck(args, 1);
                    repo.log();
                    break;
                case "global-log":
                    paramLenCheck(args, 1);
                    Repository.globalLog();
                    break;
                case "find":
                    paramLenCheck(args, 2);
                    Repository.find(args[1]);
                    break;
                case "status":
                    paramLenCheck(args, 1);
                    repo.status();
                    break;
                case "checkout":
                    repo.checkout(args);
                    break;
                case "branch":
                    paramLenCheck(args, 2);
                    repo.branch(args[1]);
                    break;
                case "rm-branch":
                    paramLenCheck(args, 2);
                    repo.rmBranch(args[1]);
                    break;
                case "reset":
                    paramLenCheck(args, 2);
                    repo.reset(args[1]);
                    break;
                case "merge":
                    paramLenCheck(args, 2);
                    repo.merge(args[1]);
                    break;
                default:
                    System.out.println("No command with that name exists.");
            }
        }
    }
}
