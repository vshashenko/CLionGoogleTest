package gtestrunner;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileSystem;
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

    public RunnerToolWindowFactory()
    {
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow)
    {
        _project = project;

        _runnerToolWindow = new RunnerToolWindow();
        _consoleWindow = new ConsoleOutputWindow();

        _runnerToolWindow.setConsoleOutputWindow(_consoleWindow);
        _runnerToolWindow.setExternalCommandExecutor(new IExternalCommandExecutor()
        {
            @Override
            public void gotoTestCase(String caseName) throws Exception
            {
                openTestCase(caseName);
            }
        });

        updateExecutablePath();

        VirtualFileSystem virtualFileSystem = project.getBaseDir().getFileSystem();

        virtualFileSystem.addVirtualFileListener(new VirtualFileAdapter()
        {
            @Override
            public void contentsChanged(@NotNull VirtualFileEvent event)
            {
                onFileContentsChanged(event);
            }
        });

        // /home/shashenk/.clion10/system/cmake/generated/e592c795/e592c795/Debug/GoogleTestSample.cbp
        // C:\Users\v.shashenko\.clion10\system\cmake\generated\7c65729b\7c65729b\Debug/GoogleTestSample.cbp

        //_runnerToolWindow.readExecutablePath(cbpPath);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        Content testsContent = contentFactory.createContent(_runnerToolWindow.getRootView(), "Tests", false);
        toolWindow.getContentManager().addContent(testsContent);

        Content consoleContent = contentFactory.createContent(_consoleWindow.root, "Console", false);
        toolWindow.getContentManager().addContent(consoleContent);
    }

    private static ExecutableInfo readExecutablePath(@NotNull Project project) throws Exception
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

    private void onFileContentsChanged(VirtualFileEvent event)
    {
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
        VirtualFileSystem virtualFileSystem = _project.getBaseDir().getFileSystem();

        if (pr.outputLines.size() > 0)
        {
            String line = pr.outputLines.get(0);
            String[] parts = line.split(":");
            String filePath = parts[0];
            int lineNumber = Integer.parseInt(parts[1]) - 1;

            VirtualFile file = virtualFileSystem.findFileByPath(filePath);
            OpenFileDescriptor fileDescriptor = new OpenFileDescriptor(_project, file, lineNumber, 0);
            fileDescriptor.navigateInEditor(_project, true);
        }
    }
}
