package gitlet;

import ucb.junit.textui;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/** The suite of all JUnit tests for the gitlet package.
 *  @author Jay Chiang
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        System.exit(textui.runClasses(UnitTest.class));
    }

    /** A dummy test to avoid complaint. */
    @Test
    public void test1() {
        Git git = new Git();
        git.init();
        git.add("world.txt");
        git.commit("add world.txt");
        git.branch("b1");
        git.rm("world.txt");
        git.commit("rm world.txt");
        git.checkout("b1");
        assertTrue(!(new File(System.getProperty("user.dir"),
                "world.txt").exists()));
    }

    /** A dummy test to avoid complaint. */
    @Test
    public void test2() {
        Git git = new Git();
        git.init();
        assertTrue(new File(System.getProperty("user.dir"),
                ".gitlet").exists());
    }
    /** A dummy test to avoid complaint. */
    @Test
    public void test3() {
        Git git = new Git();
        git.init();
        git.commit("yee");
        assertTrue(true);
        git.reset("84d15b085b99b7ba8af8b477adeb2e41e935cfe9");
        git.find("initial commit");
        assertTrue(true);
    }
    /** A dummy test to avoid complaint. */
    @Test
    public void test4() {
        Git git = new Git();
        git.init();
        git.commit("hahahaha");
        git.reset("84d15b085b99b7ba8af8b477adeb2e41e935cfe9");
        git.find("what can't find this");
        git.branch("b1");
        git.branch("b2");
        git.branch("b3");
        git.branch("b4");
        git.checkout("b3");
        assertTrue(true);
    }

}


