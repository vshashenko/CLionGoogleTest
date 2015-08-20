package gtestrunner;

import javax.swing.*;
import java.util.HashMap;
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
}
