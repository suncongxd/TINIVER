import org.dom4j.Document;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.HashSet;

public class ConstructRefinement {
    /**
     * BNNI，BSNNI，SIRNNI精化自动机构造
     * @param allActions
     * @param lowLevelActions
     * @param document
     * @return
     */
    public static Element formalConsructRef(HashSet<String> allActions, HashSet<String> lowLevelActions, Document document){
        Element refAutomata = Parse.createTemplate(document);
        refAutomata.element("name").setText("ref");
        ArrayList<Element> locations = Parse.getAllLocationsInTemplate(refAutomata);
        ArrayList<Element> transitions = Parse.getAllTransitionInTemplate(refAutomata);
        Parse.actionErrFilter(refAutomata,locations,transitions,lowLevelActions,allActions);
        Parse.timeErrFilter(refAutomata,transitions,lowLevelActions);
        return refAutomata;
    }

    /**
     * GNI精化自动机构造
     * @param document
     * @return
     */
    public static Element GNIConsructRef(Document document){
        Element refAutomata = Parse.createTemplate(document);
        refAutomata.element("name").setText("ref");
        return  refAutomata;
    }
}
