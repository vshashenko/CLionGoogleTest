package gtestrunner;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        _runnerToolWindow.setProject(project);

        try
        {
            ExecutableInfo exeInfo = readExecutablePath(project);
            _runnerToolWindow.setExecutablePath(exeInfo.command, exeInfo.arguments);
        }
        catch (Exception ex)
        {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));

            JOptionPane.showMessageDialog(null, sw.toString());
        }

        // /home/shashenk/.clion10/system/cmake/generated/e592c795/e592c795/Debug/GoogleTestSample.cbp
        // C:\Users\v.shashenko\.clion10\system\cmake\generated\7c65729b\7c65729b\Debug/GoogleTestSample.cbp

        //_runnerToolWindow.readExecutablePath(cbpPath);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        Content testsContent = contentFactory.createContent(_runnerToolWindow.root, "Tests", false);
        toolWindow.getContentManager().addContent(testsContent);

        Content consoleContent = contentFactory.createContent(_consoleWindow.root, "Console", false);
        toolWindow.getContentManager().addContent(consoleContent);
    }

    public static ExecutableInfo readExecutablePath(@NotNull Project project) throws Exception
    {
        String workspacePath = project.getWorkspaceFile().getPath();
        TargetInfo targetInfo = CLionProjectReader.readSelectedTarget(workspacePath);

        ApplicationInfo appInfo = ApplicationInfo.getInstance();
        String clionFolder = String.format(".clion%s%s", appInfo.getMajorVersion(), appInfo.getMinorVersion());

        Path cbpPath = Paths.get(
                System.getProperty("user.home"),
                clionFolder,
                "system",
                "cmake",
                "generated",
                project.getLocationHash(),
                project.getLocationHash(),
                targetInfo.config,
                targetInfo.projectName + ".cbp");

        String exePath = CLionProjectReader.readExecutablePath(cbpPath.toString(), targetInfo.runName);

        ExecutableInfo exeInfo = new ExecutableInfo();
        exeInfo.command = exePath;
        exeInfo.arguments = targetInfo.params;

        return exeInfo;
    }
}
