package gitlet;

import java.io.Serializable;
import java.util.HashMap;

/** staging area class.
 *  @author Jay Chiang
 */
public class StagingArea implements Serializable {

    /** constructor. */
    public StagingArea() {
        stageAddition = new HashMap<>();
        stageRemoval = new HashMap<>();
    }

    /** stage NAME and UID for addition. */
    public void addFileForAddition(String name, String uID) {
        stageAddition.put(name, uID);
    }

    /** stage NAME and UID for removal. */
    public void addFileForRemoval(String name, String uID) {
        stageRemoval.put(name, uID);
    }

    /** returns stage for addition. */
    public HashMap<String, String> getStageAddition() {
        return stageAddition;
    }

    /** returns stage for removal. */
    public HashMap<String, String> getStageRemoval() {
        return stageRemoval;
    }

    /** stage for addition. */
    private HashMap<String, String> stageAddition;
    /** stage for removal. */
    private HashMap<String, String>  stageRemoval;

}
