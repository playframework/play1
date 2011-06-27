package play.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import play.utils.Utils;

public class StopTask extends Task {

    private String pid;

    public void setPid(String pid) {
        this.pid = pid;
    }

    @Override
    public void execute() throws BuildException {
        if (this.pid == null) {
            throw new BuildException("PID property missing");
        }
        try {
            Utils.kill(this.pid);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

}
