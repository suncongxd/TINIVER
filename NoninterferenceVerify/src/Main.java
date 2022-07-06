import com.uppaal.engine.EngineException;
import org.apache.commons.cli.*;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class Main {
    public static HashSet<String> handleCollectionSuffix(HashSet<String> set){
        HashSet<String> ret = new HashSet<>();
        for(String s : set){
            if(s.contains("!")){
                ret.add(s.substring(0,s.length()-1) + "?");
            }
            if(s.contains("?")){
                ret.add(s.substring(0,s.length()-1) + "!");
            }
        }
        return ret;
    }
    public static Document BNNIorBSNNI(Document document,HashSet<String> delateActions, HashSet<String> allActions,HashSet<String> lowLevelActions){
        Document doc = DocumentHelper.createDocument();
        Element nta = doc.addElement("nta");
        nta.add((Element) document.getRootElement().element("declaration").clone());
        nta.add(ConstructTest.BNNIorBSNNIConsructTest(delateActions,allActions,lowLevelActions,document));
        allActions = handleCollectionSuffix(allActions);
        lowLevelActions = handleCollectionSuffix(lowLevelActions);
        nta.add(ConstructRefinement.formalConsructRef(allActions,lowLevelActions,document));
        nta.add(ConstructTest.createfallBackAutomata());
        nta.addElement("system").setText("system test,ref,Fallback;");
        //nta.addElement("queries").addElement("query").addElement("formula").setText("A[] not test.err");
        Parse.addFallback(doc);
        Parse.addPriorityForBNNIandBSNNI(doc);
        return doc;
    }

    public static Document SIRNNI(Document document,HashSet<String> delateActions, HashSet<String> allActions,HashSet<String> lowLevelActions){
        Document doc = DocumentHelper.createDocument();
        Element nta = doc.addElement("nta");
        nta.add((Element) document.getRootElement().element("declaration").clone());
        nta.add(ConstructTest.SIRNNIConsructTest(delateActions,allActions,lowLevelActions,document));
        allActions = handleCollectionSuffix(allActions);
        lowLevelActions = handleCollectionSuffix(lowLevelActions);
        nta.add(ConstructRefinement.formalConsructRef(allActions,lowLevelActions,document));
        nta.add(ConstructTest.createfallBackAutomata());
        nta.addElement("system").setText("system test,ref,Fallback;");
        //nta.addElement("queries").addElement("query").addElement("formula").setText("A[] not test.err");
        Parse.addFallback(doc);
        Parse.addPriorityForSIRNNI(doc);
        return doc;
    }


    public static void main(String[] args) throws DocumentException, IOException, EngineException {
        String flag = "BNNI"; //default property verified.
        String path = "inputModel/cav18/BNNI_2.xml";
        String sub_path = path.substring(path.indexOf("inputModel")+10,path.lastIndexOf("/")+1);
        //System.out.println("sp:"+sub_path);
        String out_path = "outputModel/"+sub_path;

        Options options = new Options();
        options.addOption("h",false,"help");
        options.addOption("i",true,"input_model_path");
        options.addOption("o",true,"output_dir_for_intermediate_model");
        options.addOption("p", true, "verified_security_property");
        options.addOption("u",true,"uppaal_path");

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if(cmd.hasOption("p")){
                String property=cmd.getOptionValue("p");
                if(property!=null & (property.equals("BNNI") || property.equals("BSNNI") || property.equals("SIRNNI")))
                    flag=property;
            }
            if(cmd.hasOption("i")){
                path = cmd.getOptionValue("i");
            }
            if(cmd.hasOption("o")){
                out_path = cmd.getOptionValue("o");
            }
            if(cmd.hasOption("u")){
                Verify.uppaal_path=cmd.getOptionValue("u");
            }
            if(cmd.hasOption("h")){
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("NoninterferenceVerify", options);
                return;
            }
        } catch(ParseException pe){
            System.out.println("Command Line Parsing Exception: "+pe.getMessage());
        }

        Document document = Parse.getDocumentByPath(path);

        ArrayList<HashSet<String>> sets = Parse.getSet(document);
        HashSet<String> delateActions = new HashSet<>();
        HashSet<String> lowLevelActions = new HashSet<>();
        HashSet<String> allActions = new HashSet<>();
        /** XML文件中按照先后定义的先后顺序分别为机密动作集合，公开动作集合 要求模型的时钟变量为c **/
        /** XML文件中按照先后定义的先后顺序分别为机密动作集合，公开动作集合 要求模型的时钟变量为c **/
        /** XML文件中按照先后定义的先后顺序分别为机密动作集合，公开动作集合 要求模型的时钟变量为c **/
        switch (flag){
            case "BSNNI" : //删除所有高级别动作
                delateActions.addAll(sets.get(0));
                break;
            default: //SIRNNI和BNNI都是删除所有高级别输入动作
                for(String s : sets.get(0)){
                    if(s.contains("!")){
                        delateActions.add(s);
                    }
                }
                break;
        }
        lowLevelActions.addAll(sets.get(1));
        for(HashSet<String> set : sets){
            allActions.addAll(set);
        }
        HashSet<String> highActions = new HashSet<>();
        highActions.addAll(allActions);
        highActions.removeAll(lowLevelActions);
        //写文件
        OutputFormat format = new OutputFormat();
        format.setIndentSize(2);
        format.setNewlines(true);
        format.setTrimText(true);
        format.setPadText(true);
        format.setEncoding("utf-8");

        File out_dir=new File(out_path);
        if (!out_dir.exists()){
            out_dir.mkdirs();
        }
        String fname= path.substring(path.lastIndexOf("/")+1).replace(".xml","_test.xml");
        out_path=out_dir.toString()+"/"+fname;

        FileOutputStream file =  new FileOutputStream(out_path);
        XMLWriter writer = new XMLWriter(file, format);
        switch (flag){
            case "SIRNNI" :
                writer.write(SIRNNI(document,delateActions,allActions,lowLevelActions));
                break;
            default:
                writer.write(BNNIorBSNNI(document,delateActions,allActions,lowLevelActions));
                break;
        }
        writer.close();
        Verify.verify(flag,highActions,out_path);
    }
}
