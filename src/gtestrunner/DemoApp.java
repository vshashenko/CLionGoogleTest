package gtestrunner;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by shashenk on 24/07/15.
 */
public class DemoApp
{
    public static void main(String[] args)
    {
        RunnerToolWindow runnerToolWindow = new RunnerToolWindow();
        ConsoleOutputWindow consoleWindow = new ConsoleOutputWindow();

        runnerToolWindow.setConsoleOutputWindow(consoleWindow);

        ExecutableInfo exeInfo = readExecutablePath();
        runnerToolWindow.setExecutablePath(exeInfo.command, exeInfo.arguments);

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

    public static ExecutableInfo readExecutablePath()
    {
        ExecutableInfo exeInfo = null;

        try
        {
            String clionProjectHash = getTeshSampleHash();
            String clionProjectFolder = getTestSampleFolder();
            Path workspacePath = new File(clionProjectFolder + "/.idea/workspace.xml").toPath();
            TargetInfo targetInfo = CLionProjectReader.readSelectedTarget(workspacePath.toString());

            Path cbpPath = Paths.get(
                    System.getProperty("user.home"),
                    ".clion11",
                    "system",
                    "cmake",
                    "generated",
                    clionProjectHash,
                    clionProjectHash,
                    targetInfo.configName,
                    targetInfo.projectName + ".cbp");

            exeInfo = new ExecutableInfo();
            exeInfo.command = CLionProjectReader.readExecutablePath(cbpPath.toString(), targetInfo.targetName);
            exeInfo.arguments = targetInfo.programParams;
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(null, Utils.toString(ex));
        }

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
