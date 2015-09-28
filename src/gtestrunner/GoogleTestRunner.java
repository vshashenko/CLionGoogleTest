package gtestrunner;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

public class GoogleTestRunner implements ProjectComponent
{
    private Project _project;
    private ExecutableInfo _exeInfo;
    private Exception _exeInfoError;
    private IProjectListener _projectListener;
    private VirtualFileListener _virtualFileListener;

    public GoogleTestRunner(Project project)
    {
        _project = project;
    }

    public void initComponent()
    {
        // TODO: insert component initialization logic here
    }

    public void disposeComponent()
    {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName()
    {
        return "GoogleTestRunner";
    }

    public void projectOpened()
    {
        updateExeInfo();

        _virtualFileListener = new VirtualFileAdapter()
        {
            @Override
            public void contentsChanged(@NotNull VirtualFileEvent event)
            {
                onFileContentsChanged(event);
            }
        };

        VirtualFileSystem virtualFileSystem = _project.getBaseDir().getFileSystem();
        virtualFileSystem.addVirtualFileListener(_virtualFileListener);
    }

    public void projectClosed()
    {
        VirtualFileSystem virtualFileSystem = _project.getBaseDir().getFileSystem();
        virtualFileSystem.removeVirtualFileListener(_virtualFileListener);
        _virtualFileListener = null;
    }

    public void set_projectListener(IProjectListener projectListener)
    {
        _projectListener = projectListener;
    }

    public ExecutableInfo get_exeInfo() throws Exception
    {
        if (_exeInfo == null)
        {
            throw _exeInfoError;
        }

        return _exeInfo;
    }

    public void openTestCase(String caseName) throws Exception
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

    public void openFile(String filePath, int lineNumber) throws Exception
    {
        VirtualFileSystem virtualFileSystem = _project.getBaseDir().getFileSystem();

        VirtualFile file = virtualFileSystem.findFileByPath(filePath);
        OpenFileDescriptor fileDescriptor = new OpenFileDescriptor(_project, file, lineNumber, 0);
        fileDescriptor.navigateInEditor(_project, true);
    }

    private void updateExeInfo()
    {
        _exeInfo = null;
        _exeInfoError = null;

        try
        {
            _exeInfo = readExecutablePath(_project);
        }
        catch (Exception ex)
        {
            _exeInfoError = ex;
        }

        if (_projectListener != null)
        {
            _projectListener.exeInfoChanged();
        }
    }

    private static ExecutableInfo readExecutablePath(@NotNull Project project) throws Exception
    {
        String workspacePath = project.getWorkspaceFile().getPath();
        TargetInfo targetInfo = CLionProjectReader.readSelectedTarget(workspacePath);

        if (targetInfo.projectName.isEmpty())
        {
            throw new NoProjectException();
        }

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
                targetInfo.configName,
                targetInfo.projectName + ".cbp");

        String exePath = CLionProjectReader.readExecutablePath(cbpPath.toString(), targetInfo.targetName);

        ExecutableInfo exeInfo = new ExecutableInfo();
        exeInfo.command = exePath;
        exeInfo.arguments = targetInfo.programParams;

        return exeInfo;
    }

    private void onFileContentsChanged(VirtualFileEvent event)
    {
        String workspacePath = _project.getWorkspaceFile().getPath();

        if (event.getFile().getPath().equals(workspacePath))
        {
            updateExeInfo();
        }
    }
}
