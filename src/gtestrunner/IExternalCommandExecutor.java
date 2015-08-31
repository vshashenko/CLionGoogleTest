package gtestrunner;

/**
 * Created by shashenk on 31/08/15.
 */
public interface IExternalCommandExecutor
{
    void gotoTestCase(String caseName) throws Exception;

    void gotoFile(String filePath, int lineNumber) throws Exception;
}
