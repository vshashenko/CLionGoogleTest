package gtestrunner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CLionProjectReader
{
    public static List<TargetInfo> readAllTargets(String workspacePath) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(workspacePath));

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        NodeList runnableConfigurationElems = (NodeList)xpath
                .compile("/project/component[@name='RunManager']/configuration[@RUN_TARGET_NAME and @type='CMakeRunConfiguration']")
                .evaluate(doc, XPathConstants.NODESET);

        ArrayList<TargetInfo> targetList = new ArrayList<>();

        for (int i = 0; i < runnableConfigurationElems.getLength(); i++)
        {
            Element elem = (Element)runnableConfigurationElems.item(i);

            TargetInfo targetInfo = new TargetInfo();

            targetInfo.runTargetProjectName = elem.getAttribute("RUN_TARGET_PROJECT_NAME");
            targetInfo.runTargetName = elem.getAttribute("RUN_TARGET_NAME");
            targetInfo.programParams = elem.getAttribute("PROGRAM_PARAMS");
            targetInfo.configName = elem.getAttribute("CONFIG_NAME");
            targetInfo.workingDir = elem.getAttribute("WORKING_DIR");

            targetList.add(targetInfo);
        }

        return targetList;
    }

    public static TargetInfo readSelectedTarget(String workspacePath) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(workspacePath));

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        Element runManagerElem = (Element)xpath
                .compile("/project/component[@name='RunManager']")
                .evaluate(doc, XPathConstants.NODE);

        String selectedTarget = runManagerElem.getAttribute("selected");

        String[] selectedTargetParts = selectedTarget.split("\\.");
        String factoryName = selectedTargetParts[0];
        String targetName = selectedTargetParts[1];

        Element selectedTargetConfig =  (Element)xpath
                .compile(String.format("configuration[@name='%s' and @factoryName='%s']", targetName, factoryName))
                .evaluate(runManagerElem, XPathConstants.NODE);

        TargetInfo targetInfo = new TargetInfo();

        targetInfo.runTargetProjectName = selectedTargetConfig.getAttribute("RUN_TARGET_PROJECT_NAME");
        targetInfo.runTargetName = selectedTargetConfig.getAttribute("RUN_TARGET_NAME");
        targetInfo.programParams = selectedTargetConfig.getAttribute("PROGRAM_PARAMS");
        targetInfo.configName = selectedTargetConfig.getAttribute("CONFIG_NAME");

        return targetInfo;
    }

    public static String readExecutablePath(String cbpPath, String targetRunName) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(cbpPath));

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        XPathExpression expr1 = xpath.compile(String.format("/CodeBlocks_project_file/Project/Build/Target[@title='%s']/Option/@output", targetRunName));
        String targetOutput = (String) expr1.evaluate(doc, XPathConstants.STRING);

        XPathExpression expr2 = xpath.compile(String.format("/CodeBlocks_project_file/Project/Build/Target[@title='%s']/Option/@working_dir", targetRunName));
        String targetWorkingDir = (String) expr2.evaluate(doc, XPathConstants.STRING);

        return targetOutput;
        // use targetWorkingDir
    }
}
