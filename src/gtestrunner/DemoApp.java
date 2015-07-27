package gtestrunner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
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

        tabbedPane.addTab("Tests", runnerToolWindow.root);
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
        String clionProjectFolder = "/home/shashenk/Devel/navcloud-cpp-sdk";
        ExecutableInfo exeInfo = null;

        try
        {
            Path workspacePath = new File(clionProjectFolder + "/.idea/workspace.xml").toPath();
            TargetInfo targetInfo = CLionProjectReader.readSelectedTarget(workspacePath.toString());

            Path cbpPath = Paths.get(
                    System.getProperty("user.home"),
                    ".clion10",
                    "system",
                    "cmake",
                    "generated",
                    "e592c795",
                    "e592c795",
                    targetInfo.config,
                    targetInfo.projectName + ".cbp");

            exeInfo = new ExecutableInfo();
            exeInfo.command = CLionProjectReader.readExecutablePath(cbpPath.toString(), targetInfo.runName);
            exeInfo.arguments = targetInfo.params;
        }
        catch (Exception ex)
        {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));

            JOptionPane.showMessageDialog(null, sw.toString());
        }

        return exeInfo;
    }
}
