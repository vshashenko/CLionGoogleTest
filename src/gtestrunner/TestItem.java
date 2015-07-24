package gtestrunner;

/**
 * Created by shashenk on 23/07/15.
 */
public class TestItem
{
    private String _name;

    public TestItem(String name)
    {
        _name = name;
    }

    public String getName()
    {
        return _name;
    }

    @Override
    public String toString()
    {
        return getName();
    }
}