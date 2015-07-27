package gtestrunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shashenk on 20/07/15.
 */
public class TestCase extends TestItem
{
    private TestSuite _suite;
    private List<String> _failures = new ArrayList<>();

    public TestCase(TestSuite suite, String caseName)
    {
        super(caseName);

        _suite = suite;
    }

    @Override
    public void setStatus(TestCaseStatus value)
    {
        super.setStatus(value);

        if (value == TestCaseStatus.NotRun)
        {
            _failures.clear();
        }
    }


    public TestSuite getSuite()
    {
        return _suite;
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
        return String.format("%s -- %s ms", super.getName(), super.getExecutionTime());
        //return getName();
    }
}
