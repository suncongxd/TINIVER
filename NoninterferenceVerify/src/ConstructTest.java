import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.HashSet;

public class ConstructTest {
    /**
     * 构造辅助测试自动机
     * @return
     */
    public static Element createfallBackAutomata(){
        Element fallback = DocumentHelper.createElement("template");
        fallback.addElement("name").setText("Fallback");
        fallback.addElement("declaration");
        fallback.addElement("location").addAttribute("id","id0");
        fallback.addElement("init").addAttribute("ref","id0");
        Element loop = fallback.addElement("transition");
        loop.addElement("source").addAttribute("ref","id0");
        loop.addElement("target").addAttribute("ref","id0");
        Element action = loop.addElement("label").addAttribute("kind","synchronisation");
        action.setText("fallback!");
        return fallback;
    }
    /**
     * 构造BNNI的测试自动机
     */
    public static Element BNNIorBSNNIConsructTest(HashSet<String> delateActions, HashSet<String> allActions, HashSet<String> lowLevelActions, Document document){
        Element testAutomata = Parse.createTemplate(document);
        testAutomata.element("name").setText("test");
        Parse.handleSuffix(testAutomata);
        Parse.delateEdge(testAutomata,delateActions);
        //需要去掉不可达的节点
        Parse.delateUselessLocation(testAutomata);
        Parse.createErrLocation(testAutomata);
        Parse.modifyGuard(testAutomata);
        ArrayList<Element> locations = Parse.getAllLocationsInTemplate(testAutomata);
        ArrayList<Element> transitions = Parse.getAllTransitionInTemplate(testAutomata);
        Parse.actionErr(testAutomata,locations,transitions,lowLevelActions,allActions);
        Parse.timeErr(testAutomata,transitions,lowLevelActions);
        Parse.fallBack(testAutomata,transitions,lowLevelActions);
        Parse.delateInvariant(locations);
        return testAutomata;
    }

    public static Element SIRNNIConsructTest(HashSet<String> delateActions, HashSet<String> allActions, HashSet<String> lowLevelActions, Document document){
        Element testAutomata = Parse.createTemplate(document);
        testAutomata.element("name").setText("test");
        Parse.handleSuffix(testAutomata);
        Parse.delateEdge(testAutomata,delateActions);
        //需要去掉不可达的节点
        Parse.delateUselessLocation(testAutomata);
        Parse.createErrLocation(testAutomata);
        Parse.modifyGuard(testAutomata);
        ArrayList<Element> locations = Parse.getAllLocationsInTemplate(testAutomata);
        ArrayList<Element> transitions = Parse.getAllTransitionInTemplate(testAutomata);
        Parse.actionErr(testAutomata,locations,transitions,lowLevelActions,allActions);
        Parse.timeErr(testAutomata,transitions,lowLevelActions);
        Parse.SIRFallBack(testAutomata,transitions,lowLevelActions);
        Parse.delateInvariant(locations);
        return testAutomata;
    }

}
