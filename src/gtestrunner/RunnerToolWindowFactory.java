package gtestrunner;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RunnerToolWindowFactory implements ToolWindowFactory, DumbAware
{
    private RunnerToolWindow _runnerToolWindow;
    private ConsoleOutputWindow _consoleWindow;

    public RunnerToolWindowFactory()
    {
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow)
    {
        _runnerToolWindow = new RunnerToolWindow();
        _consoleWindow = new ConsoleOutputWindow();

        _runnerToolWindow.setConsoleOutputWindow(_consoleWindow);
        //_runnerToolWindow.setWorkspacePath(project.getWorkspaceFile().getPath());

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        Content testsContent = contentFactory.createContent(_runnerToolWindow.root, "Tests", false);
        toolWindow.getContentManager().addContent(testsContent);

        Content consoleContent = contentFactory.createContent(_consoleWindow.root, "Console", false);
        toolWindow.getContentManager().addContent(consoleContent);
    }
}
