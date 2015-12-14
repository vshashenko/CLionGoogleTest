package gtestrunner;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GoogleTestRunner implements ProjectComponent
{
    private Project _project;
    private List<TargetInfo> _availableTargets = new ArrayList<>();
    private TargetInfo _selectedTarget;
    private Exception _targetReadingException;
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
        updateAvailableTargetList();

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

    public List<TargetInfo> get_availableTargets() throws Exception
    {
        if (_targetReadingException != null)
        {
            throw _targetReadingException;
        }

        return _availableTargets;
    }

    public TargetInfo get_selectedTarget() throws Exception
    {
        if (_targetReadingException != null)
        {
            throw _targetReadingException;
        }

        return _selectedTarget;
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

    private void updateAvailableTargetList()
    {
        _availableTargets.clear();
        _selectedTarget = null;
        _targetReadingException = null;

        try
        {
            String workspacePath = _project.getWorkspaceFile().getPath();
            List<TargetInfo> availableTargets = CLionProjectReader.readAllTargets(workspacePath);

            for (int i = 0; i < availableTargets.size(); i++)
            {
                TargetInfo item = availableTargets.get(i);

                item.executableInfo = readExecutablePath(_project, item);
            }

            _availableTargets.addAll(availableTargets);
            _selectedTarget = availableTargets.size() > 0 ? availableTargets.get(0) : null;
        }
        catch (Exception ex)
        {
            _targetReadingException = ex;
        }

        if (_projectListener != null)
        {
            _projectListener.exeInfoChanged();
        }
    }

    private static ExecutableInfo readExecutablePath(@NotNull Project project, TargetInfo targetInfo) throws Exception
    {
        if (targetInfo.runTargetProjectName.isEmpty())
        {
            throw new NoProjectException();
        }

        ApplicationInfo appInfo = ApplicationInfo.getInstance();
        String clionFolder = String.format(".CLion%s%s", appInfo.getMajorVersion(), appInfo.getMinorVersion().split("\\.")[0]);

        Path cbpPath = Paths.get(
                System.getProperty("user.home"),
                clionFolder,
                "system",
                "cmake",
                "generated",
                project.getLocationHash(),
                project.getLocationHash(),
                targetInfo.configName,
                targetInfo.runTargetProjectName + ".cbp");

        String exePath = CLionProjectReader.readExecutablePath(cbpPath.toString(), targetInfo.runTargetName);

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
            updateAvailableTargetList();
        }
    }
}
