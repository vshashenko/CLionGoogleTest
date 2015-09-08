package gtestrunner;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.*;
import com.intellij.openapi.vfs.*;
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
    private Project _project;
    private VirtualFileListener _virtualFileListener;

    public RunnerToolWindowFactory()
    {
        Notifications.Bus.notify(new Notification("test", "RunnerToolWindowFactory", "a", NotificationType.INFORMATION));

        ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter()
        {
            @Override
            public void projectOpened(Project project)
            {
                onProjectOpened(project);
            }

            @Override
            public void projectClosed(Project project)
            {
                onProjectClosed(project);
            }
        });
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow)
    {
        Notifications.Bus.notify(new Notification("test", "createToolWindowContent", "a", NotificationType.INFORMATION));

        _runnerToolWindow = new RunnerToolWindow();
        _consoleWindow = new ConsoleOutputWindow();

        _runnerToolWindow.setConsoleOutputWindow(_consoleWindow);
        _runnerToolWindow.setExternalCommandExecutor(new IExternalCommandExecutor()
        {
            @Override
            public void gotoFile(String filePath, int lineNumber) throws Exception
            {
                openFile(filePath, lineNumber);
            }

            @Override
            public void gotoTestCase(String caseName) throws Exception
            {
                openTestCase(caseName);
            }
        });

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        Content testsContent = contentFactory.createContent(_runnerToolWindow.getRootView(), "Tests", false);
        toolWindow.getContentManager().addContent(testsContent);

        Content consoleContent = contentFactory.createContent(_consoleWindow.root, "Console", false);
        toolWindow.getContentManager().addContent(consoleContent);
    }

    private void onProjectOpened(Project project)
    {
        Notifications.Bus.notify(new Notification("test", "projectOpened " + project.getName(), "a", NotificationType.INFORMATION));

        _project = project;

        updateExecutablePath();

        _virtualFileListener = new VirtualFileAdapter()
        {
            @Override
            public void contentsChanged(@NotNull VirtualFileEvent event)
            {
                onFileContentsChanged(event);
            }
        };

        VirtualFileSystem virtualFileSystem = project.getBaseDir().getFileSystem();
        virtualFileSystem.addVirtualFileListener(_virtualFileListener);
    }

    private void onProjectClosed(Project project)
    {
        Notifications.Bus.notify(new Notification("test", "projectClosed ", "a", NotificationType.INFORMATION));

        VirtualFileSystem virtualFileSystem = project.getBaseDir().getFileSystem();
        virtualFileSystem.removeVirtualFileListener(_virtualFileListener);

        _project = null;
    }

    private static ExecutableInfo readExecutablePath(@NotNull Project project) throws Exception
    {
        String workspacePath = project.getWorkspaceFile().getPath();
        TargetInfo targetInfo = CLionProjectReader.readSelectedTarget(workspacePath);

        ApplicationInfo appInfo = ApplicationInfo.getInstance();
        String clionFolder = String.format(".clion%s%s", appInfo.getMajorVersion(), appInfo.getMinorVersion().split("\\.")[0]);

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

    private void onFileContentsChanged(VirtualFileEvent event)
    {
        if (!_project.isOpen())
        {
            return;
        }

        String workspacePath = _project.getWorkspaceFile().getPath();

        if (event.getFile().getPath().equals(workspacePath))
        {
            updateExecutablePath();
        }
    }

    private void updateExecutablePath()
    {
        try
        {
            ExecutableInfo exeInfo = readExecutablePath(_project);
            _runnerToolWindow.setExecutablePath(exeInfo.command, exeInfo.arguments);
        }
        catch (Exception ex)
        {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));

            JOptionPane.showMessageDialog(null, sw.toString());
        }
    }

    private void openTestCase(String caseName) throws Exception
    {
        ProcessResult pr = Utils.executeProcess(null, "grep", "-nwrI", "--include=*.cpp", caseName, _project.getBaseDir().getPath());

        if (pr.outputLines.size() > 0)
        {
            String line = pr.outputLines.get(0);
            String[] parts = line.split(":");
            String filePath = parts[0];
            int lineNumber = Integer.parseInt(parts[1]) - 1;

            openFile(filePath, lineNumber);
        }
    }

    private void openFile(String filePath, int lineNumber) throws Exception
    {
        VirtualFileSystem virtualFileSystem = _project.getBaseDir().getFileSystem();

        VirtualFile file = virtualFileSystem.findFileByPath(filePath);
        OpenFileDescriptor fileDescriptor = new OpenFileDescriptor(_project, file, lineNumber, 0);
        fileDescriptor.navigateInEditor(_project, true);
    }
}
