package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/** The commit class.
 *  @author Jay Chiang
 */
public class Commit implements Serializable {

    /** defualt constructor using MESSAGE, PARENTUID, BLOBS. */
    public Commit(String message, String parentUID,
                  HashMap<String, String> blobs) {
        _message = message;
        _parentUID = parentUID;
        _mergeParent = null;
        if (_parentUID == null) {
            _timestamp = "Thu Jan 01 00:00:00 1970 -0800";
        } else {
            ZonedDateTime time = ZonedDateTime.now();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(
                    "EEE LLL dd HH:mm:ss yyyy Z");
            _timestamp = time.format(timeFormatter);
        }
        _blobs = blobs;
        _UID = Utils.getSha1(this);
        File cwd = new File(System.getProperty("user.dir"));
        File globalLog = new File(cwd, ".gitlet/logs/glbal-log.txt");
        if (globalLog.exists()) {
            String content = makeLog();
            content +=  Utils.readContentsAsString(globalLog);
            Utils.writeContents(globalLog, content);
        } else {
            Utils.writeContents(globalLog, makeLog());
        }
        File commitDir = new File(cwd, ".gitlet/commits");
        Utils.writeObject(new File(commitDir, _UID + ".txt"),
                this);
    }

    /** special constructor for a merge commit using MESSAGE
     * PARENTUID, BLOBS, and MERGEPARENT. */
    public Commit(String message, String parentUID,
                  HashMap<String, String> blobs, String mergeParent) {
        _message = message;
        _parentUID = parentUID;
        _mergeParent = mergeParent;
        if (_parentUID == null) {
            _timestamp = "Thu Jan 01 00:00:00 1970 -0800";
        } else {
            ZonedDateTime time = ZonedDateTime.now();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(
                    "EEE LLL dd HH:mm:ss yyyy Z");
            _timestamp = time.format(timeFormatter);
        }
        _blobs = blobs;
        _UID = Utils.getSha1(this);
        File cwd = new File(System.getProperty("user.dir"));
        File globalLog = new File(cwd, ".gitlet/logs/glbal-log.txt");
        if (globalLog.exists()) {
            String content = makeLogMerge();
            content +=  Utils.readContentsAsString(globalLog);
            Utils.writeContents(globalLog, content);
        } else {
            Utils.writeContents(globalLog, makeLogMerge());
        }
        File commitDir = new File(cwd, ".gitlet/commits");
        Utils.writeObject(new File(commitDir, _UID + ".txt"),
                this);
    }

    /** returns log. */
    public String makeLog() {
        String result = "";
        result += "===" + "\n";
        result += "commit " + _UID + "\n";
        result += "Date: " + _timestamp + "\n";
        result += _message + "\n" + "\n";
        return result;
    }

    /** returns log for merge. */
    public String makeLogMerge() {
        String result = "";
        result += "===" + "\n";
        result += "commit " + _UID + "\n";
        result += "Merge: " + _parentUID.substring(0, 7) + " "
                + _mergeParent.substring(0, 7) + "\n";
        result += "Date: " + _timestamp + "\n";
        result += _message + "\n" + "\n";
        return result;
    }

    /** returns message. */
    public String getMessage() {
        return _message;
    }

    /** returns timestamp. */
    public String getTimestamp() {
        return _timestamp;
    }

    /** returns parentUID. */
    public String getParentUID() {
        return _parentUID;
    }

    /** returns mergeParentUID. */
    public String getMergeParentUID() {
        return _mergeParent;
    }

    /** returns self UID. */
    public String getUID() {
        return _UID;
    }

    /** returns blobs. */
    public HashMap<String, String> getBlobs() {
        return _blobs;
    }

    /** message. */
    private String _message;
    /** timestamp. */
    private String _timestamp;
    /** parentUID.. */
    private String _parentUID;
    /** UID. */
    private String _UID;
    /** blobs. */
    private HashMap<String, String> _blobs;
    /** mergeParentUID. */
    private String _mergeParent;
}
