package gtestrunner;

/**
 * Created by shashenk on 28/07/15.
 */
public class TestSuites extends TestItem
{
    private String _timestamp;
    private int _executionTime;
    private int _testCount;
    private int _disabledTestCount;
    private int _failedTestCount;

    TestSuites(int testCount, String timestamp, int executionTime)
    {
        super("");

        _timestamp = timestamp;
        _executionTime = executionTime;
        _testCount = testCount;
    }

    public String getTimestamp()
    {
        return _timestamp;
    }

    public void setTimestamp(String value)
    {
        _timestamp = value;
    }

    public int getExecutionTime()
    {
        return _executionTime;
    }

    public void setExecutionTime(int value)
    {
        _executionTime = value;
    }

    public int getTestCount()
    {
        return _testCount;
    }

    public int getDisabledTestCount()
    {
        return _disabledTestCount;
    }

    public void setDisabledTestCount(int value)
    {
        _disabledTestCount = value;
    }

    public int getFailedTestCount()
    {
        return _failedTestCount;
    }

    public void setFailedTestCount(int value)
    {
        _failedTestCount = value;
    }
}
