package gtestrunner;

import com.intellij.openapi.project.*;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;

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
        try
        {
            ExecutableInfo exeInfo = project.getComponent(GoogleTestRunner.class).get_exeInfo();
            _runnerToolWindow.setExecutablePath(exeInfo.command, exeInfo.arguments);
            _runnerToolWindow.setError("");
        }
        catch (Exception ex)
        {
            _runnerToolWindow.setExecutablePath("", "");

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));

            _runnerToolWindow.setError(sw.toString());
        }
    }
}
