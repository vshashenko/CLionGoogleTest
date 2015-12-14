package gtestrunner;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * Created by shashenk on 24/07/15.
 */
public class DemoApp
{
    private static List<TargetInfo> _availableTargets = new ArrayList<>();
    private static TargetInfo _selectedTarget;

    public static void main(String[] args)
    {
        RunnerToolWindow runnerToolWindow = new RunnerToolWindow();
        ConsoleOutputWindow consoleWindow = new ConsoleOutputWindow();

        runnerToolWindow.setConsoleOutputWindow(consoleWindow);

        runnerToolWindow.setAvailableTargets(readAvailableTargets());

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Tests", runnerToolWindow.getRootView());
        tabbedPane.addTab("Console", consoleWindow.root);

        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (dimension.getWidth() / 2);
        int height = (int) (dimension.getHeight() / 2);

        JFrame frame = new JFrame("GTest Runner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(tabbedPane);
        frame.pack();
        frame.setSize(width, height);
        frame.setLocationByPlatform(true);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static List<TargetInfo> readAvailableTargets()
    {
        List<TargetInfo> availableTargets = null;

        try
        {
            String clionProjectFolder = getTestSampleFolder();
            Path workspacePath = new File(clionProjectFolder + "/.idea/workspace.xml").toPath();

            availableTargets = CLionProjectReader.readAllTargets(workspacePath.toString());

            for (int i = 0; i < availableTargets.size(); i++)
            {
                TargetInfo item = availableTargets.get(i);

                item.executableInfo = readExecutablePath(item);
            }

            _availableTargets.addAll(availableTargets);
            _selectedTarget = availableTargets.size() > 0 ? availableTargets.get(0) : null;
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(null, Utils.toString(ex));
        }

        return availableTargets;
    }

    private static ExecutableInfo readExecutablePath(TargetInfo targetInfo) throws Exception
    {
        if (targetInfo.runTargetProjectName.isEmpty())
        {
            throw new NoProjectException();
        }

        String clionFolder = ".CLion12";
        String clionProjectHash = getTeshSampleHash();

        Path cbpPath = Paths.get(
                System.getProperty("user.home"),
                clionFolder,
                "system",
                "cmake",
                "generated",
                clionProjectHash,
                clionProjectHash,
                targetInfo.configName,
                targetInfo.runTargetProjectName + ".cbp");

        String exePath = CLionProjectReader.readExecutablePath(cbpPath.toString(), targetInfo.runTargetName);

        ExecutableInfo exeInfo = new ExecutableInfo();
        exeInfo.command = exePath;
        exeInfo.arguments = targetInfo.programParams;

        return exeInfo;
    }

    private static String getTestSampleFolder() throws Exception
    {
        String os = System.getProperty("os.name");
        if (os.equals("Linux"))
        {
            return "/home/shashenk/Devel/navcloud-cpp-sdk";
        }
        else if (os.startsWith("Windows"))
        {
            return "d:\\devel\\GoogleTestSample";
        }
        else
        {
            throw new Exception("Unsupported OS.");
        }
    }

    private static String getTeshSampleHash() throws Exception
    {
        String os = System.getProperty("os.name");
        if (os.equals("Linux"))
        {
            return "e592c795";
        }
        else if (os.startsWith("Windows"))
        {
            return "7c65729b";
        }
        else
        {
            throw new Exception("Unsupported OS.");
        }
    }
}
