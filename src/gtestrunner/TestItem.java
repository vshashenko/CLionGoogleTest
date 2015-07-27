package gtestrunner;

/**
 * Created by shashenk on 23/07/15.
 */
public class TestItem
{
    private TestCaseStatus _status;
    private int _executionTime;
    private String _name;

    public TestItem(String name)
    {
        _name = name;
        _status = TestCaseStatus.NotRun;
        _executionTime = 0;
    }

    public String getName()
    {
        return _name;
    }

    public TestCaseStatus getStatus()
    {
        return _status;
    }

    public void setStatus(TestCaseStatus value)
    {
        _status = value;
    }

    public int getExecutionTime()
    {
        return _executionTime;
    }

    public void setExecutionTime(int value)
    {
        _executionTime = value;
    }

    @Override
    public String toString()
    {
        return getName();
    }
}