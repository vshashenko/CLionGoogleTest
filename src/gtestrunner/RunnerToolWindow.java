package gtestrunner;

import org.w3c.dom.*;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

// - Tree icons for suites
// - Transparent icons
// - Read gtest executable from the project data
// - Splitter position in the middle
// - Run gtest process in a separate thread (ExecutorService)
// - Add progress indication while executing tests (based on prev. execution times?)
// - Two run buttons: Run all, Run dropdown (selected, failed, not run)
// - Execution times in right aligned column of the tree control
// - No horizontal scroll bar
// - Goto failed line of source code from failure report
// - Overview of a test run (execution date-time, running time, number of tests in different states)

public class RunnerToolWindow
{
    public JPanel root;
    private JTree _testTree;
    private JButton _discoverButton;
    private JTextArea _errorArea;
    private JButton _runAllButton;
    private JButton _runSelectedButton;
    private JSplitPane _splitPane;
    private JTextField _executablePathArea;
    private DefaultTreeModel _testListModel;

    private ImageIcon _disabledIcon;
    private ImageIcon _notRunIcon;
    private ImageIcon _failedIcon;
    private ImageIcon _successIcon;

    private ConsoleOutputWindow _consoleOutputWindow;

    private List<String> _additionalArguments;

    RunnerToolWindow()
    {
        //_executablePathArea.setText("/home/shashenk/Devel/navcloud-cpp-sdk/bin/NavCloudSDKTestMain");
        _executablePathArea.setText("c:\\MinGW\\bin\\GoogleTestSample.exe");


        _additionalArguments = new ArrayList<>();
        _additionalArguments.add("--username");
        _additionalArguments.add("vadim.shashenko@tomtom.com");
        _additionalArguments.add("--password");
        _additionalArguments.add("dobraijoetra");

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Root");
        _testListModel = new DefaultTreeModel(rootNode);
        _testTree.setModel(_testListModel);

        _disabledIcon = new ImageIcon(getClass().getResource("images/Disabled.png"));
        _notRunIcon = new ImageIcon(getClass().getResource("images/NotRun.png"));
        _failedIcon = new ImageIcon(getClass().getResource("images/Failed.png"));
        _successIcon = new ImageIcon(getClass().getResource("images/Success.png"));

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(_notRunIcon);

        _testTree.setCellRenderer(new DefaultTreeCellRenderer()
        {
            @Override
            public Component getTreeCellRendererComponent(JTree tree,
                                                          Object value,
                                                          boolean selected,
                                                          boolean expanded,
                                                          boolean isLeaf,
                                                          int row,
                                                          boolean focused)
            {
                Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);

                DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
                if (node.getUserObject() instanceof TestCase)
                {
                    TestCase testCase = (TestCase)node.getUserObject();

                    switch (testCase.getStatus())
                    {
                        case Disabled:
                            setIcon(_disabledIcon);
                            break;

                        case NotRun:
                            setIcon(_notRunIcon);
                            break;

                        case Failed:
                            setIcon(_failedIcon);
                            break;

                        case Success:
                            setIcon(_successIcon);
                            break;
                    }
                }

                return c;
            }
        });

        _discoverButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                onDiscoverButtonClicked(e);
            }
        });

        _runSelectedButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                onRunSelectedButtonClicked(e);
            }
        });

        _runAllButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                onRunAllButtonClicked(e);
            }
        });

        _testTree.addTreeSelectionListener(new TreeSelectionListener()
        {
            @Override
            public void valueChanged(TreeSelectionEvent e)
            {
                onTreeSelectionChanged(e);
            }
        });
    }

    public void setConsoleOutputWindow(ConsoleOutputWindow consoleOutputWindow)
    {
        _consoleOutputWindow = consoleOutputWindow;
    }

    public void setWorkspacePath(String workspacePath)
    {
        try
        {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(workspacePath));

            NodeList nodeList0 = doc.getElementsByTagName("project");
            for (int i0 = 0; i0 < nodeList0.getLength(); i0++)
            {
                Element elem0 = (Element) nodeList0.item(i0);
                NodeList nodeList1 = elem0.getElementsByTagName("component");

                for (int i1 = 0; i1 < nodeList1.getLength(); i1++)
                {
                    Element elem1 = (Element) nodeList1.item(i0);

                    if (!elem1.getAttribute("name").equals("RunManager"))
                    {
                        continue;
                    }

                    String configName = elem1.getAttribute("selected").split("\\.")[1];

                    NodeList nodeList2 = elem1.getElementsByTagName("configuration");

                    for (int i2 = 0; i2 < nodeList2.getLength(); i2++)
                    {
                        Element elem2 = (Element) nodeList2.item(i0);

                        if (!elem2.getAttribute("name").equals(configName))
                        {
                            continue;
                        }

                        String executableName = elem2.getAttribute("RUN_TARGET_NAME");
                        String executableArgs = elem2.getAttribute("PROGRAM_PARAMS");
                    }
                }

            }
        }
        catch (Exception e)
        {
            _errorArea.setText("Error setting workspace path.");
        }
    }

    private void onDiscoverButtonClicked(ActionEvent e)
    {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)_testListModel.getRoot();
        root.removeAllChildren();
        _testListModel.reload();

        try
        {
            List<String> outputLines = getTestList();

            TestSuite currentSuite = null;
            DefaultMutableTreeNode suiteNode = null;

            for (String currentLine : outputLines)
            {
                currentLine = currentLine.trim();

                if (currentLine.endsWith("."))
                {
                    if (currentLine.length() > 1)
                    {
                        String suiteName = currentLine.substring(0, currentLine.length() - 1);
                        currentSuite = new TestSuite(suiteName);

                        suiteNode = new DefaultMutableTreeNode(currentSuite);
                        root.add(suiteNode);
                    }
                }
                else
                {
                    TestCase testCase = new TestCase(currentSuite, currentLine);

                    DefaultMutableTreeNode caseNode = new DefaultMutableTreeNode(testCase);
                    suiteNode.add(caseNode);
                }
            }

            _testListModel.reload();
            Utils.expandAll(_testTree);

            String s = Utils.stringJoin("\n", outputLines);
            _consoleOutputWindow.textArea1.setText(s);
        }
//        catch (ProcessExecutionException e)
//        {
//            _errorArea.setText(e.getProcessOutput());
//        }
        catch (Exception ex)
        {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));

            _errorArea.setText("Exception:\n" + sw.toString());
        }
    }

    private void onRunSelectedButtonClicked(ActionEvent e)
    {
        TreePath[] selectionPaths = _testTree.getSelectionPaths();
        if (selectionPaths == null)
        {
            return;
        }

        List<String> testList = new ArrayList<>(selectionPaths.length);

        for (TreePath path : selectionPaths)
        {
            Object[] objects = path.getPath();

            if (objects.length == 2)
            {
                DefaultMutableTreeNode obj1 = (DefaultMutableTreeNode) objects[1];
                TestSuite testSuite = (TestSuite) obj1.getUserObject();

                testList.add(testSuite.getName() + ".*");
            }
            else if (objects.length == 3)
            {
                DefaultMutableTreeNode obj2 = (DefaultMutableTreeNode) objects[2];
                TestCase testCase = (TestCase) obj2.getUserObject();

                testList.add(testCase.getFullName());
            }
        }

        runTests(testList);
    }

    private void onRunAllButtonClicked(ActionEvent e)
    {
        onDiscoverButtonClicked(null);

        List<String> testList = new ArrayList<>();
        testList.add("*");

        runTests(testList);
    }

    private void onTreeSelectionChanged(TreeSelectionEvent e)
    {
        if (e.getPath() == null)
        {
            return;
        }

        // TODO wrong selection when changing from two selected items to a single.
        Object[] path = e.getPath().getPath();

        if (path.length == 3)
        {
            TestCase testCase = (TestCase)((DefaultMutableTreeNode) path[2]).getUserObject();

            switch (testCase.getStatus())
            {
                case NotRun:
                    _errorArea.setText("The test hasn't been run. Run the test too see its results.");
                    break;

                case Disabled:
                    _errorArea.setText("The test is disabled. Enable it in your gtest code and rerun.");
                    break;

                case Success:
                    _errorArea.setText("The test succeeded.");
                    break;

                case Failed:
                    String message = String.format("The test failed in %d place(s).\n\n", testCase.getFailures().size());
                    _errorArea.setText(message);

                    String failures = Utils.stringJoin("\n\n", testCase.getFailures());
                    _errorArea.append(failures);
                    break;
            }
        }
    }

    private void discoverTests()
    {

    }

    private void runTests(List<String> testList)
    {
        Path resultFile = null;

        try
        {
            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)_testListModel.getRoot();
            resetAllTestsToNotRun(rootNode);

            //resultFile = Files.createTempFile("gtestrunner", "");
            //resultFile = new File("/home/shashenk/Devel/gtestrunner_results.txt").toPath();
            resultFile = new File("d:\\devel\\gtestrunner_results.txt").toPath();
            List<String> outputLines = runTests(testList, resultFile);

            processTestResults(resultFile, rootNode);

            String s = Utils.stringJoin("\n", outputLines);
            _consoleOutputWindow.textArea1.setText(s);

            onTreeSelectionChanged(new TreeSelectionEvent(_testTree, _testTree.getSelectionPath(), true, null, null));
        }
//        catch (ProcessExecutionException e)
//        {
//            _errorArea.setText(e.getProcessOutput());
//        }
        catch (Exception ex)
        {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));

            _errorArea.setText("Exception:\n" + sw.toString());
        }
        finally
        {
            if (resultFile != null)
            {
//                try
//                {
//                    Files.deleteIfExists(resultFile);
//                }
//                catch(IOException e)
//                {
//                }
            }
        }
    }

    private List<String> getTestList() throws IOException, InterruptedException
    {
        List<String> args = new ArrayList<>();

        args.add(_executablePathArea.getText());
        args.add("--gtest_list_tests");
        args.addAll(_additionalArguments);

        List<String> outputLines = executeProcess(args);

        return outputLines;
    }

    private void processTestResults(Path resultFile, DefaultMutableTreeNode root) throws Exception
    {
        if (!Files.exists(resultFile))
        {
            throw new Exception("Result file does not exist.");
        }

        _errorArea.setText("");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File(resultFile.toString()));

        NodeList testSuitesNodeList = doc.getElementsByTagName("testsuites");

        for (int i0 = 0; i0 < testSuitesNodeList.getLength(); i0++)
        {
            Element testSuitesElement = (Element)testSuitesNodeList.item(i0);

            NodeList testSuiteNodeList = testSuitesElement.getElementsByTagName("testsuite");

            for (int i1 = 0; i1 < testSuiteNodeList.getLength(); i1++)
            {
                Element testSuiteElement = (Element)testSuiteNodeList.item(i1);

                String suiteName = testSuiteElement.getAttribute("name");
                DefaultMutableTreeNode suiteNode = findChildByTestItemName(root, suiteName);
                if (suiteNode == null)
                {
                    continue;
                }

                NodeList testCaseNodeList = testSuiteElement.getElementsByTagName("testcase");

                for (int i2 = 0; i2 < testCaseNodeList.getLength(); i2++)
                {
                    Element testCaseElement = (Element) testCaseNodeList.item(i2);

                    String caseName = testCaseElement.getAttribute("name");
                    String caseStatus = testCaseElement.getAttribute("status");
                    String caseTime = testCaseElement.getAttribute("time");

                    DefaultMutableTreeNode caseNode = findChildByTestItemName(suiteNode, caseName);
                    if (caseNode == null)
                    {
                        continue;
                    }

                    TestCase testCase = (TestCase) caseNode.getUserObject();

                    NodeList failureNodes = testCaseElement.getElementsByTagName("failure");

                    for (int i3 = 0; i3 < failureNodes.getLength(); i3++)
                    {
                        Element failureElement = (Element) failureNodes.item(i3);

                        testCase.getFailures().add(failureElement.getAttribute("message"));
                    }

                    testCase.setExecutionTime((int) (Double.parseDouble(caseTime) * 1000));

                    switch (caseStatus)
                    {
                        case "run":
                            testCase.setStatus(failureNodes.getLength() == 0 ? TestCaseStatus.Success : TestCaseStatus.Failed);
                            break;

                        case "notrun":
                            testCase.setStatus(TestCaseStatus.Disabled);
                            break;
                    }

                    _testListModel.nodeChanged(caseNode);
                }
            }
        }
    }

    // TODO replace with a hashmap
    private static DefaultMutableTreeNode findChildByTestItemName(DefaultMutableTreeNode parent, String suiteName)
    {
        List children = Collections.list(parent.children());
        for(Object obj : children)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
            TestItem testItem = (TestItem)node.getUserObject();

            if (testItem.getName().equals(suiteName))
            {
                return node;
            }
        }

        return null;
    }

    private void resetAllTestsToNotRun(DefaultMutableTreeNode parent)
    {
        Enumeration<DefaultMutableTreeNode> nodeEnum = parent.depthFirstEnumeration();
        while(nodeEnum.hasMoreElements())
        {
            DefaultMutableTreeNode node = nodeEnum.nextElement();

            if (node.getUserObject() instanceof TestCase)
            {
                TestCase testCase = (TestCase)node.getUserObject();

                if (testCase.getStatus() != TestCaseStatus.NotRun)
                {
                    testCase.setStatus(TestCaseStatus.NotRun);

                    _testListModel.nodeChanged(node);
                }
            }
        }
    }

    private List<String> runTests(List<String> testNames, Path resultFile) throws IOException, InterruptedException
    {
        String testNamesString = Utils.stringJoin(":", testNames);

        List<String> args = new ArrayList<>();

        args.add(_executablePathArea.getText());
        args.add("--gtest_filter=" + testNamesString);
        args.add("--gtest_output=xml:" + resultFile.toString()); // whitespaces in filename
        args.addAll(_additionalArguments);

        List<String> outputLines = executeProcess(args);

        return outputLines;
    }

    private static List<String> executeProcess(List<String> command) throws IOException, InterruptedException
    {
        String[] args = new String[command.size()];
        command.toArray(args);

        return executeProcess(args);
    }

    // TODO resource disposal?
    private static List<String> executeProcess(String... command) throws IOException, InterruptedException
    {
        ProcessBuilder pb = new ProcessBuilder(command);

        pb.redirectErrorStream(true);
        Process process = pb.start();

        InputStreamReader streamReader = new InputStreamReader(process.getInputStream());
        BufferedReader reader = new BufferedReader(streamReader);

        List<String> outputLines = new ArrayList<>();

        while (true)
        {
            String line = reader.readLine();
            if (line == null)
            {
                break;
            }

            outputLines.add(line);
        }

        streamReader.close();
        reader.close();

        process.waitFor();

        int exitValue = process.exitValue();
        if (exitValue != 0)
        {
            //throw new ProcessExecutionException(exitValue, String.join("\n", outputLines));
        }

        return outputLines;
    }
}
