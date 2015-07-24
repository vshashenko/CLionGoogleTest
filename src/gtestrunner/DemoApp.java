package gtestrunner;

import javax.swing.*;
import java.awt.*;

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
}
