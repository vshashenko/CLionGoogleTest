package gtestrunner;

import com.intellij.openapi.project.*;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RunnerToolWindowFactory implements ToolWindowFactory, DumbAware
{
    private RunnerToolWindow _runnerToolWindow;
    private ConsoleOutputWindow _consoleWindow;

    public RunnerToolWindowFactory()
    {
    }

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull ToolWindow toolWindow)
    {
        final GoogleTestRunner runner = project.getComponent(GoogleTestRunner.class);
        runner.set_projectListener(new IProjectListener()
        {
            @Override
            public void exeInfoChanged()
            {
                updateExecutablePath(project);
            }
        });

        _runnerToolWindow = new RunnerToolWindow();
        _consoleWindow = new ConsoleOutputWindow();

        _runnerToolWindow.setConsoleOutputWindow(_consoleWindow);
        _runnerToolWindow.setExternalCommandExecutor(new IExternalCommandExecutor()
        {
            @Override
            public void gotoFile(String filePath, int lineNumber) throws Exception
            {
                runner.openFile(filePath, lineNumber);
            }

            @Override
            public void gotoTestCase(String caseName) throws Exception
            {
                runner.openTestCase(caseName);
            }
        });

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        Content testsContent = contentFactory.createContent(_runnerToolWindow.getRootView(), "Tests", false);
        toolWindow.getContentManager().addContent(testsContent);

        Content consoleContent = contentFactory.createContent(_consoleWindow.root, "Console", false);
        toolWindow.getContentManager().addContent(consoleContent);

        updateExecutablePath(project);
    }

    private void updateExecutablePath(Project project)
    {
        _runnerToolWindow.setAvailableTargets(new ArrayList<TargetInfo>());
        _runnerToolWindow.setError("");

        try
        {
            List<TargetInfo> availableTargets = project.getComponent(GoogleTestRunner.class).get_availableTargets();

            _runnerToolWindow.setAvailableTargets(availableTargets);
            //_runnerToolWindow.setCurrentTarget(null);
        }
        catch (NoProjectException ex)
        {
            _runnerToolWindow.setError("Project does not exist. Probably run executable is not selected.");
        }
        catch (Exception ex)
        {
            _runnerToolWindow.setError(Utils.toString(ex));
        }
    }
}
