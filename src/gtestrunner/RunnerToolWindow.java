package gtestrunner;

import com.intellij.openapi.project.Project;
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
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

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
    private JTextField _runSummary;
    private JProgressBar _progressBar;
    private JPanel _summaryCards;
    private JButton _stopButton;
    private DefaultTreeModel _testListModel;

    private ImageIcon _disabledIcon;
    private ImageIcon _notRunIcon;
    private ImageIcon _failedIcon;
    private ImageIcon _successIcon;

    private ConsoleOutputWindow _consoleOutputWindow;

    private List<String> _additionalArguments;
    private TestSuites _testSuites;

    private Path _resultFile;
    private SwingWorker<ProcessResult, Void> _testExecutionWorker;
    private Process _process;

    RunnerToolWindow()
    {
        _additionalArguments = new ArrayList<>();

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Root");
        _testListModel = new DefaultTreeModel(rootNode);
        _testTree.setModel(_testListModel);

        _stopButton.setEnabled(false);

        _disabledIcon = new ImageIcon(getClass().getResource("images/Disabled.png"));
        _notRunIcon = new ImageIcon(getClass().getResource("images/NotRun.png"));
        _failedIcon = new ImageIcon(getClass().getResource("images/Failed.png"));
        _successIcon = new ImageIcon(getClass().getResource("images/Success.png"));

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(_notRunIcon);

        _splitPane.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                _splitPane.setDividerLocation(0.5);
            }
        });

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

        _stopButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                onStopButtonClicked(e);
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

    public void setExecutablePath(String s, String params)
    {
        _executablePathArea.setText(s);

        _additionalArguments.clear();
        Collections.addAll(_additionalArguments, params.split(" "));
    }

    public void setProject(Project project)
    {
        _errorArea.append(project.toString());
        _errorArea.append("\n");
        _errorArea.append(project.getBasePath());
        _errorArea.append("\n");
        _errorArea.append(project.getLocationHash());
        _errorArea.append("\n");
        _errorArea.append(project.getName());
        _errorArea.append("\n");
        _errorArea.append(project.getPresentableUrl());
        _errorArea.append("\n");
        _errorArea.append(project.getProjectFilePath());
        _errorArea.append("\n");
        _errorArea.append(project.getBaseDir().getPath());
        _errorArea.append("\n");
        _errorArea.append(project.getProjectFile().getPath());
        _errorArea.append("\n");
        _errorArea.append(project.getWorkspaceFile().getPath());
        _errorArea.append("\n");
    }

    private void onDiscoverButtonClicked(ActionEvent e)
    {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)_testListModel.getRoot();
        root.removeAllChildren();
        _testListModel.reload();

        try
        {
            int testCount = 0;
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
                    testCount++;

                    DefaultMutableTreeNode caseNode = new DefaultMutableTreeNode(testCase);
                    suiteNode.add(caseNode);
                }
            }

            _testSuites = new TestSuites(testCount, "", 0);

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

        runTests2(testList);
    }

    private void onRunAllButtonClicked(ActionEvent e)
    {
        onDiscoverButtonClicked(null);

        List<String> testList = new ArrayList<>();
        testList.add("*");

        runTests2(testList);
    }

    private void onStopButtonClicked(ActionEvent e)
    {
        _process.destroy();
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
            resultFile = new File("/home/shashenk/Devel/gtestrunner_results.txt").toPath();
            //resultFile = new File("d:\\devel\\gtestrunner_results.txt").toPath();

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

    private void runTests2(final List<String> testList)
    {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)_testListModel.getRoot();
        resetAllTestsToNotRun(rootNode);

        //final Path resultFile = Files.createTempFile("gtestrunner", "");
        _resultFile = new File("/home/shashenk/Devel/gtestrunner_results.txt").toPath();
        //final Path resultFile = new File("d:\\devel\\gtestrunner_results.txt").toPath();

        String testNamesString = Utils.stringJoin(":", testList);

        final List<String> args = new ArrayList<>();

        args.add(_executablePathArea.getText());
        args.add("--gtest_filter=" + testNamesString);
        args.add("--gtest_output=xml:" + _resultFile.toString()); // whitespaces in filename
        args.addAll(_additionalArguments);

        _testExecutionWorker = new SwingWorker<ProcessResult, Void>()
        {
            @Override
            protected ProcessResult doInBackground() throws Exception
            {
                return executeProcess(args);
            }

            @Override
            protected void done()
            {
                onWorkerDone();
            }
        };

        CardLayout cl = (CardLayout)(_summaryCards.getLayout());
        cl.show(_summaryCards, "ProgressCard");

        _runSelectedButton.setEnabled(false);
        _runAllButton.setEnabled(false);
        _discoverButton.setEnabled(false);
        _stopButton.setEnabled(true);
        _progressBar.setIndeterminate(true);

        _testExecutionWorker.execute();
    }

    private void onWorkerDone()
    {
        _stopButton.setEnabled(false);
        _progressBar.setIndeterminate(false);
        _runSelectedButton.setEnabled(true);
        _runAllButton.setEnabled(true);
        _discoverButton.setEnabled(true);

        CardLayout cl = (CardLayout)(_summaryCards.getLayout());
        cl.show(_summaryCards, "SummaryCard");


        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)_testListModel.getRoot();
        ProcessResult processResult = null;

        try
        {
            processResult = _testExecutionWorker.get();

            processTestResults(_resultFile, rootNode);

            String s = Utils.stringJoin("\n", processResult.outputLines);

            _consoleOutputWindow.textArea1.setText(String.format("The process exited with the code: %d", processResult.exitValue));
            _consoleOutputWindow.textArea1.append("\n\n");
            _consoleOutputWindow.textArea1.append(s);

            onTreeSelectionChanged(new TreeSelectionEvent(_testTree, _testTree.getSelectionPath(), true, null, null));
        }
        catch (ExecutionException ex)
        {
        }
        catch (InterruptedException ex)
        {
        }
        catch (Exception ex)
        {
        }
        finally
        {
            if (_resultFile != null)
            {
                try
                {
                    Files.deleteIfExists(_resultFile);
                }
                catch(IOException e)
                {
                }
            }
        }
    }

    private List<String> getTestList() throws IOException, InterruptedException
    {
        List<String> args = new ArrayList<>();

        args.add(_executablePathArea.getText());
        args.add("--gtest_list_tests");
        args.addAll(_additionalArguments);

        ProcessResult processResult = executeProcess(args);

        return processResult.outputLines;
    }

    private List<String> runTests(List<String> testNames, Path resultFile) throws IOException, InterruptedException
    {
        String testNamesString = Utils.stringJoin(":", testNames);

        List<String> args = new ArrayList<>();

        args.add(_executablePathArea.getText());
        args.add("--gtest_filter=" + testNamesString);
        args.add("--gtest_output=xml:" + resultFile.toString()); // whitespaces in filename
        args.addAll(_additionalArguments);

        ProcessResult processResult = executeProcess(args);

        return processResult.outputLines;
    }

    private ProcessResult executeProcess(List<String> command) throws IOException, InterruptedException
    {
        String[] args = new String[command.size()];
        command.toArray(args);

        return executeProcess(args);
    }

    private ProcessResult executeProcess(String... command) throws IOException, InterruptedException
    {
        ProcessBuilder pb = new ProcessBuilder(command);

        pb.redirectErrorStream(true);
        _process = pb.start();

        List<String> outputLines = new ArrayList<>();

        try(InputStreamReader streamReader = new InputStreamReader(_process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(streamReader))
        {
            while (true)
            {
                String line = bufferedReader.readLine();
                if (line == null)
                {
                    break;
                }

                outputLines.add(line);
            }
        }

        _process.waitFor();

        ProcessResult processResult = new ProcessResult();

        processResult.exitValue = _process.exitValue();
        processResult.outputLines = outputLines;

        return processResult;
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

        Element testSuitesElement = doc.getDocumentElement();

        //String testCount = testSuitesElement.getAttribute("tests");
        String timestamp = testSuitesElement.getAttribute("timestamp");
        String time = testSuitesElement.getAttribute("time");
        String failures = testSuitesElement.getAttribute("failures");
        String disabled = testSuitesElement.getAttribute("disabled");
        String failed = testSuitesElement.getAttribute("failures");

        _testSuites.setExecutionTime((int) (Double.parseDouble(time) * 1000));
        _testSuites.setStatus(failures.equals("0") ? TestCaseStatus.Success : TestCaseStatus.Failed);
        _testSuites.setTimestamp(timestamp);
        _testSuites.setDisabledTestCount(Integer.parseInt(disabled));
        _testSuites.setFailedTestCount(Integer.parseInt(failed));

        NodeList testSuiteNodeList = testSuitesElement.getElementsByTagName("testsuite");

        for (int i1 = 0; i1 < testSuiteNodeList.getLength(); i1++)
        {
            Element testSuiteElement = (Element) testSuiteNodeList.item(i1);

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

        String runSummary = String.format("Tests: %d, disabled: %d, failed: %d,  timestamp: %s, time: %d ms",
                _testSuites.getTestCount(),
                _testSuites.getDisabledTestCount(),
                _testSuites.getFailedTestCount(),
                _testSuites.getTimestamp(),
                _testSuites.getExecutionTime());

        _runSummary.setText(runSummary);
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
}
