package gtestrunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shashenk on 20/07/15.
 */
public class TestCase extends TestItem
{
    private TestSuite _suite;
    private TestCaseStatus _status;
    private int _executionTime;
    private List<String> _failures = new ArrayList<>();

    public TestCase(TestSuite suite, String caseName)
    {
        super(caseName);

        _suite = suite;
        _status = TestCaseStatus.NotRun;
        _executionTime = 0;
    }

    public TestSuite getSuite()
    {
        return _suite;
    }

    public TestCaseStatus getStatus()
    {
        return _status;
    }

    public void setStatus(TestCaseStatus value)
    {
        _status = value;

        if (_status == TestCaseStatus.NotRun)
        {
            _failures.clear();
        }
    }

    public int getExecutionTime()
    {
        return _executionTime;
    }

    public void setExecutionTime(int value)
    {
        _executionTime = value;
    }

    public String getFullName()
    {
        return _suite.getName() + "." + super.getName();
    }

    public List<String> getFailures()
    {
        return _failures;
    }

    @Override
    public String toString()
    {
        return String.format("%s -- %s ms", super.getName(), _executionTime);
        //return getName();
    }
}
