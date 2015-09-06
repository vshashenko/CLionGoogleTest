package gtestrunner;

import com.intellij.ui.HyperlinkAdapter;
import org.w3c.dom.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
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
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class RunnerToolWindow
{
    private JPanel _rootView;
    private JTree _testTree;
    private JEditorPane _errorArea;
    private JButton _discoverButton;
    private JButton _runAllButton;
    private JButton _runSelectedButton;
    private JButton _stopButton;
    private JButton _expandAllButton;
    private JButton _collapseAllButton;
    private JSplitPane _splitPane;
    private JTextField _executablePathArea;
    private JProgressBar _progressBar;
    private JPanel _summaryCards;
    private JScrollPane _testTreeScrollPane;
    private JLabel _summaryPassed;
    private JLabel _summaryFailed;
    private JLabel _summaryDisabled;
    private JLabel _summaryTotalTime;
    private JLabel _discoveryStatus;

    private DefaultTreeModel _testListModel;

    private ImageIcon _disabledIcon;
    private ImageIcon _notRunIcon;
    private ImageIcon _failedIcon;
    private ImageIcon _successIcon;

    private ConsoleOutputWindow _consoleOutputWindow;

    private List<String> _additionalArguments;
    private TestSuites _testSuites;

    private Path _resultFile;
    private SwingWorker<ProcessResult, String> _testExecutionWorker;
    private SwingWorker<ProcessResult, String> _testDiscoveryWorker;
    private Process _process;

    private IExternalCommandExecutor _externalCommandExecutor;

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
            JPanel _renderer = new JPanel();
            JLabel _timeLabel = new JLabel("50 ms");

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

                //_renderer.add(c);
                //_renderer.setBackground(c.getBackground());

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node.getUserObject() instanceof TestCase)
                {
                    TestCase testCase = (TestCase) node.getUserObject();

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

                //return _renderer;
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

        _expandAllButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                onExpandAllButtonClicked(e);
            }
        });

        _collapseAllButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                onCollapseAllButtonClicked(e);
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

        _testTree.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                JTree tree = ((JTree) e.getComponent());
                int row = tree.getRowForLocation(e.getX(), e.getY());
                if (row != -1)
                {
                    if (e.getClickCount() == 2)
                    {
                        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                        onTreeItemActivated(path);
                    }
                }
            }
        });

        _errorArea.setContentType("text/html");

        _errorArea.addHyperlinkListener(new HyperlinkAdapter()
        {
            @Override
            protected void hyperlinkActivated(HyperlinkEvent hyperlinkEvent)
            {
                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                {
                    onHyperlinkActivated(hyperlinkEvent);
                }
            }
        });
    }

    private void onTreeItemActivated(TreePath selPath)
    {
        Object[] objects = selPath.getPath();

        if (objects.length == 3)
        {
            DefaultMutableTreeNode obj2 = (DefaultMutableTreeNode) objects[2];
            TestCase testCase = (TestCase) obj2.getUserObject();

            try
            {
                if (_externalCommandExecutor != null)
                {
                    _externalCommandExecutor.gotoTestCase(testCase.getName());
                }
            }
            catch (Exception ex)
            {
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));

                _errorArea.setText(sw.toString().replace("\n", "<br>"));
            }
        }
    }

    private void onHyperlinkActivated(HyperlinkEvent hyperlinkEvent)
    {
        try
        {
            if (_externalCommandExecutor != null)
            {

                String link = hyperlinkEvent.getURL().toString().substring(5);
                String[] linkParts = link.split(":");
                String filePath = linkParts[0];
                int lineNumber = Integer.parseInt(linkParts[1]) - 1;

                _externalCommandExecutor.gotoFile(filePath, lineNumber);
            }
        }
        catch (Exception ex)
        {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));

            _errorArea.setText(sw.toString().replace("\n", "<br>"));
        }
    }

    public JComponent getRootView()
    {
        return _rootView;
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

    public void setExternalCommandExecutor(IExternalCommandExecutor externalCommandExecutor)
    {
        _externalCommandExecutor = externalCommandExecutor;
    }

    private void onDiscoverButtonClicked(ActionEvent e)
    {
        discoverTests();
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

        runTests(testList, TestRunMode.Selected);
    }

    private void onRunAllButtonClicked(ActionEvent e)
    {
        List<String> testList = new ArrayList<>();
        testList.add("*");

        runTests(testList, TestRunMode.All);
    }

    private void onStopButtonClicked(ActionEvent e)
    {
        _process.destroy();
    }

    private void onExpandAllButtonClicked(ActionEvent e)
    {
        Utils.expandAll(_testTree);
    }

    private void onCollapseAllButtonClicked(ActionEvent e)
    {
        Utils.collapseAll(_testTree);
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
                    String message = formatFailures(testCase.getFailures());
                    _errorArea.setText(message);
                    break;
            }
        }
    }

    private void discoverTests()
    {
        // If a discovery operation changes number of suites, then the restore will not work correct.
        final TestTreeState testTreeState = new TestTreeState();
        testTreeState.expansionState = Utils.getExpansionStates(_testTree);
        testTreeState.selectionRows = _testTree.getSelectionRows();
        testTreeState.verticalScrollValue = _testTreeScrollPane.getVerticalScrollBar().getValue();

        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)_testListModel.getRoot();
        rootNode.removeAllChildren();
        _testListModel.reload();

        _consoleOutputWindow.textArea1.setText("");

        final List<String> args = new ArrayList<>();

        args.add(_executablePathArea.getText());
        args.add("--gtest_list_tests");
        args.addAll(_additionalArguments);

        _testDiscoveryWorker = new SwingWorker<ProcessResult, String>()
        {
            @Override
            protected ProcessResult doInBackground() throws Exception
            {
                return Utils.executeProcess(new IProcessOutputObserver()
                {
                    @Override
                    public void onProcessCreated(Process process)
                    {
                        _process = process;
                    }

                    @Override
                    public void onNewLine(String s)
                    {
                        publish(s);
                    }
                }, args);
            }

            @Override
            protected void process(List<String> chunks)
            {
                onTestDiscoveryProgress(chunks);
            }

            @Override
            protected void done()
            {
                onTestDiscoveryFinished(testTreeState);
            }
        };

        CardLayout cl = (CardLayout)(_summaryCards.getLayout());
        cl.show(_summaryCards, "ProgressCard");

        _runSelectedButton.setEnabled(false);
        _runAllButton.setEnabled(false);
        _discoverButton.setEnabled(false);
        _stopButton.setEnabled(true);
        _progressBar.setIndeterminate(true);

        _testDiscoveryWorker.execute();
    }

    private void onTestDiscoveryProgress(List<String> lines)
    {
        for (String line : lines)
        {
            _consoleOutputWindow.textArea1.append(line);
            _consoleOutputWindow.textArea1.append("\n");
        }
    }

    private void onTestDiscoveryFinished(final TestTreeState testTreeState)
    {
        _stopButton.setEnabled(false);
        _progressBar.setIndeterminate(false);
        _runSelectedButton.setEnabled(true);
        _runAllButton.setEnabled(true);
        _discoverButton.setEnabled(true);

        CardLayout cl = (CardLayout)(_summaryCards.getLayout());
        cl.show(_summaryCards, "DiscoveryCard");

        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)_testListModel.getRoot();
        ProcessResult processResult = null;

        try
        {
            processResult = _testDiscoveryWorker.get();

            _consoleOutputWindow.textArea1.append("\n\n");
            String exitMessage = String.format(">> The process has exited with the code: %d", processResult.exitValue);
            _consoleOutputWindow.textArea1.append(exitMessage);
            _consoleOutputWindow.textArea1.append("\n");

            processDiscoveryResults(processResult.outputLines, rootNode);

            Utils.setExpansionStates(_testTree, testTreeState.expansionState);
            _testTree.setSelectionRows(testTreeState.selectionRows);

            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    _testTreeScrollPane.getVerticalScrollBar().setValue(testTreeState.verticalScrollValue);
                }
            });
        }
//        catch (ExecutionException ex)
//        {
//        }
//        catch (InterruptedException ex)
//        {
//        }
        catch (DiscoveryParsingException ex)
        {
            //TODO write message about failed discovery in the test tree window instead?
            _errorArea.setText(
                    "The output from the test executable doesn't appear to be a list of tests.<br>" +
                    "Please check Console tab for details.");
        }
        catch (Exception ex)
        {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));

            _errorArea.setText("Exception:\n" + sw.toString().replace("\n", "<br>"));
        }
    }

    private void runTests(List<String> testList, final TestRunMode runMode)
    {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)_testListModel.getRoot();
        resetTestsToNotRun(rootNode, testList);

        try
        {
            _resultFile = Files.createTempFile("gtestrunner", "");
        }
        catch (Exception ex)
        {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));

            _errorArea.setText("Exception:\n" + sw.toString().replace("\n", "<br>"));

            return;
        }

        String testNamesString = Utils.stringJoin(":", testList);

        final List<String> args = new ArrayList<>();

        args.add(_executablePathArea.getText());
        args.add("--gtest_filter=" + testNamesString);
        args.add("--gtest_output=xml:" + _resultFile.toString()); // whitespaces in filename
        args.addAll(_additionalArguments);

        //TODO refactoring: tie console window text to the process output so the call is not duplicated.
        _consoleOutputWindow.textArea1.setText("");

        _testExecutionWorker = new SwingWorker<ProcessResult, String>()
        {
            @Override
            protected ProcessResult doInBackground() throws Exception
            {
                return Utils.executeProcess(new IProcessOutputObserver()
                {
                    @Override
                    public void onProcessCreated(Process process)
                    {
                        _process = process;
                    }

                    @Override
                    public void onNewLine(String s)
                    {
                        publish(s);
                    }
                }, args);
            }

            @Override
            protected void process(List<String> chunks)
            {
                onTestRunProgress(chunks);
            }

            @Override
            protected void done()
            {
                onTestRunFinished(runMode);
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

    private void onTestRunProgress(List<String> lines)
    {
        for (String line : lines)
        {
            _consoleOutputWindow.textArea1.append(line);
            _consoleOutputWindow.textArea1.append("\n");
        }
    }

    private void onTestRunFinished(TestRunMode runMode)
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

            _consoleOutputWindow.textArea1.append("\n\n");
            String exitMessage = String.format(">> The process has exited with the code: %d", processResult.exitValue);
            _consoleOutputWindow.textArea1.append(exitMessage);
            _consoleOutputWindow.textArea1.append("\n");

            processTestResults(_resultFile, rootNode, runMode);

            onTreeSelectionChanged(new TreeSelectionEvent(_testTree, _testTree.getSelectionPath(), true, null, null));
        }
//        catch (ExecutionException ex)
//        {
//        }
//        catch (InterruptedException ex)
//        {
//        }
        catch (Exception ex)
        {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));

            _errorArea.setText("Exception:\n" + sw.toString().replace("\n", "<br>"));
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

        ProcessResult processResult = Utils.executeProcess(null, args);

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

        ProcessResult processResult = Utils.executeProcess(null, args);

        return processResult.outputLines;
    }

    private void processDiscoveryResults(List<String> outputLines, DefaultMutableTreeNode rootNode)
            throws DiscoveryParsingException
    {
        int testCount = 0;
        TestSuite currentSuite = null;
        DefaultMutableTreeNode suiteNode = null;

        Pattern testSuiteNameTemplate = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*\\.$");
        Pattern testCaseNameTemplate = Pattern.compile("^  [a-zA-Z_][a-zA-Z0-9_]*$");

        for (String currentLine : outputLines)
        {
            if (testSuiteNameTemplate.matcher(currentLine).matches())
            {
                String suiteName = currentLine.substring(0, currentLine.length() - 1);
                currentSuite = new TestSuite(suiteName);

                suiteNode = new DefaultMutableTreeNode(currentSuite);
                rootNode.add(suiteNode);
            }
            else if (testCaseNameTemplate.matcher(currentLine).matches())
            {
                if (currentSuite == null)
                {
                    throw new DiscoveryParsingException();
                }

                TestCase testCase = new TestCase(currentSuite, currentLine.substring(2));
                testCount++;

                DefaultMutableTreeNode caseNode = new DefaultMutableTreeNode(testCase);
                suiteNode.add(caseNode);
            }
            else
            {
                throw new DiscoveryParsingException();
            }
        }

        _testSuites = new TestSuites(testCount, "", 0);

        _testListModel.reload();
        Utils.expandAll(_testTree);

        // TODO singular/plular form of "tests".
        _discoveryStatus.setText(String.format("%d tests have been found", _testSuites.getTestCount()));
    }

    private void processTestResults(Path resultFile, DefaultMutableTreeNode rootNode, TestRunMode runMode)
            throws Exception
    {
        if (!Files.exists(resultFile))
        {
            throw new Exception("Result file does not exist.");
        }

        if (runMode == TestRunMode.All)
        {
            rootNode.removeAllChildren();
            _testListModel.reload();
        }

        _errorArea.setText("");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File(resultFile.toString()));

        Element testSuitesElement = doc.getDocumentElement();

        String testCount = testSuitesElement.getAttribute("tests");
        String timestamp = testSuitesElement.getAttribute("timestamp");
        String time = testSuitesElement.getAttribute("time");
        String failures = testSuitesElement.getAttribute("failures");
        String disabled = testSuitesElement.getAttribute("disabled");
        String failed = testSuitesElement.getAttribute("failures");

        NodeList testSuiteNodeList = testSuitesElement.getElementsByTagName("testsuite");

        for (int i1 = 0; i1 < testSuiteNodeList.getLength(); i1++)
        {
            Element testSuiteElement = (Element) testSuiteNodeList.item(i1);

            String suiteName = testSuiteElement.getAttribute("name");
            DefaultMutableTreeNode suiteNode = findChildByTestItemName(rootNode, suiteName);
            TestSuite currentSuite = null;
            if (suiteNode == null)
            {
                if (runMode == TestRunMode.All)
                {
                    currentSuite = new TestSuite(suiteName);
                    suiteNode = new DefaultMutableTreeNode(currentSuite);
                    rootNode.add(suiteNode);
                }
                else
                {
                    continue;
                }
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
                    if (runMode == TestRunMode.All)
                    {
                        TestCase testCase = new TestCase(currentSuite, caseName);

                        caseNode = new DefaultMutableTreeNode(testCase);
                        suiteNode.add(caseNode);
                    }
                    else
                    {
                        continue;
                    }
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

                if (runMode == TestRunMode.Selected)
                {
                    _testListModel.nodeChanged(caseNode);
                }
            }
        }

        if (runMode == TestRunMode.All)
        {
            _testListModel.reload();
            Utils.expandAll(_testTree);
        }

        _testSuites = new TestSuites(Integer.parseInt(testCount), "", 0);
        _testSuites.setExecutionTime((int) (Double.parseDouble(time) * 1000));
        _testSuites.setStatus(failures.equals("0") ? TestCaseStatus.Success : TestCaseStatus.Failed);
        _testSuites.setTimestamp(timestamp);
        _testSuites.setDisabledTestCount(Integer.parseInt(disabled));
        _testSuites.setFailedTestCount(Integer.parseInt(failed));

        _summaryPassed.setText(Integer.toString(_testSuites.getTestCount()));
        _summaryFailed.setText(Integer.toString(_testSuites.getFailedTestCount()));
        _summaryDisabled.setText(Integer.toString(_testSuites.getDisabledTestCount()));
        _summaryTotalTime.setText(Integer.toString(_testSuites.getExecutionTime()) + " ms");
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

    private void resetTestsToNotRun(DefaultMutableTreeNode parent, List<String> testList)
    {
        Enumeration<DefaultMutableTreeNode> nodeEnum = parent.depthFirstEnumeration();

        while(nodeEnum.hasMoreElements())
        {
            DefaultMutableTreeNode node = nodeEnum.nextElement();

            if (node.getUserObject() instanceof TestCase)
            {
                TestCase testCase = (TestCase)node.getUserObject();

                if (testList.contains(testCase.getFullName()) ||
                    testList.contains(testCase.getSuite().getName() + ".*"))
                {
                    if (testCase.getStatus() != TestCaseStatus.NotRun)
                    {
                        testCase.setStatus(TestCaseStatus.NotRun);

                        _testListModel.nodeChanged(node);
                    }
                }
            }
        }
    }

    private static String formatFailures(List<String> failures)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<html>");
        stringBuilder.append(String.format("The test has %d failure(s).", failures.size()));
        stringBuilder.append("<br><br>");

        for (String failure : failures)
        {
            String[] failureLines = failure.split("\n");

            for (String line : failureLines)
            {
                if (line.contains(":") && line.contains("/"))
                {
                    stringBuilder.append(String.format("<a href=\"file://%s\">%s</a>", line, line));
                }
                else
                {
                    stringBuilder.append(line);
                }

                stringBuilder.append("<br>");
            }
        }

        stringBuilder.append("</html>");

        return stringBuilder.toString();
    }
}
