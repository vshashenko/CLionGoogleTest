package gtestrunner;

import javax.swing.*;
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
