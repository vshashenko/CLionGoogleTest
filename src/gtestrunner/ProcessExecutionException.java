package gtestrunner;

import java.io.IOException;

/**
 * Created by shashenk on 20/07/15.
 */
class ProcessExecutionException extends IOException
{
    private int _exitCode;
    private String _processOutput;

    ProcessExecutionException(int exitCode, String processOutput)
    {
        _exitCode = exitCode;
        _processOutput = processOutput;
    }

    public int getExitCode()
    {
        return _exitCode;
    }

    public String getProcessOutput()
    {
        return _processOutput;
    }
}
