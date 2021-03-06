package gtestrunner;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Utils
{
    public static void expandAll(JTree tree)
    {
        for (int i = 0; i < tree.getRowCount(); i++)
        {
            tree.expandRow(i);
        }
    }

    public static void collapseAll(JTree tree)
    {
        for (int i = 0; i < tree.getRowCount(); i++)
        {
            tree.collapseRow(i);
        }
    }

    public static HashSet<Integer> getExpansionStates(JTree tree)
    {
        HashSet<Integer> expandedRows = new HashSet<>();

        for (int i = 0; i < tree.getRowCount(); i++)
        {
            if (tree.isExpanded(i))
            {
                expandedRows.add(i);
            }
        }

        return expandedRows;
    }

    public static void setExpansionStates(JTree tree, HashSet<Integer> expandedRows)
    {
        for (int i = 0; i < tree.getRowCount(); i++)
        {
            if (expandedRows.contains(i))
            {
                tree.expandRow(i);
            }
            else
            {
                tree.collapseRow(i);
            }
        }
    }

    public static String stringJoin(String delim, List<String> stringList)
    {
        StringBuilder builder = new StringBuilder();

        for(int i=0; i < stringList.size(); i++)
        {
            builder.append(stringList.get(i));

            if (i != stringList.size())
            {
                builder.append(delim);
            }
        }

        return builder.toString();
    }

    public static ProcessResult executeProcess(IProcessOutputObserver observer, List<String> command)
            throws IOException, InterruptedException
    {
        String[] args = new String[command.size()];
        command.toArray(args);

        return executeProcess(observer, args);
    }

    public static ProcessResult executeProcess(IProcessOutputObserver observer, String... command)
            throws IOException, InterruptedException
    {
        ProcessBuilder pb = new ProcessBuilder(command);

        pb.redirectErrorStream(true);
        Process process = pb.start();

        if (observer != null)
        {
            observer.onProcessCreated(process);
        }

        List<String> outputLines = new ArrayList<>();

        try(InputStreamReader streamReader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(streamReader))
        {
            while (true)
            {
                String line = bufferedReader.readLine();
                if (line == null)
                {
                    break;
                }

                if (observer != null)
                {
                    observer.onNewLine(line);
                }

                outputLines.add(line);
            }
        }

        process.waitFor();

        ProcessResult processResult = new ProcessResult();

        processResult.exitValue = process.exitValue();
        processResult.outputLines = outputLines;

        return processResult;
    }

    public static String toString(Exception ex)
    {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));

        return sw.toString();
    }

    public static void log(String message)
    {
        if (message == null)
        {
            message = "<null>";
        }

        try
        {
            Files.write(Paths.get("d:\\devel\\gtest_runner_log.txt"), (message + "\r\n").getBytes(), StandardOpenOption.APPEND);
        }
        catch (IOException e)
        {
        }
    }
}
