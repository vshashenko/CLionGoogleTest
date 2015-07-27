package gtestrunner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.nio.file.Path;

public class CLionProjectReader
{
    public static TargetInfo readSelectedTarget(String workspacePath) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(workspacePath));

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        Element selectedTargetElem = (Element)xpath
                .compile("/project/component[@name='RunManager']")
                .evaluate(doc, XPathConstants.NODE);

        String selectedTarget = selectedTargetElem.getAttribute("selected");

        String[] selectedTargetParts = selectedTarget.split("\\.");
        String factoryName = selectedTargetParts[0];
        String targetName = selectedTargetParts[1];

        Element selectedTargetConfig =  (Element)xpath
                .compile(String.format("configuration[@name='%s' and @factoryName='%s']", targetName, factoryName))
                .evaluate(selectedTargetElem, XPathConstants.NODE);

        TargetInfo targetInfo = new TargetInfo();

        targetInfo.projectName = selectedTargetConfig.getAttribute("PROJECT_NAME");
        targetInfo.runName = selectedTargetConfig.getAttribute("RUN_TARGET_NAME");
        targetInfo.params = selectedTargetConfig.getAttribute("PROGRAM_PARAMS");
        targetInfo.config = selectedTargetConfig.getAttribute("CONFIG_NAME");

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
