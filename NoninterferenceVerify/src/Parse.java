import com.uppaal.model.system.symbolic.SymbolicTrace;
import on.S;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parse {
    /**
     * 获取当前输入模型并删除dtd声明
     * @param path
     * @return document
     * @throws DocumentException
     * @throws IOException
     */
    public static Document getDocumentByPath(String path) throws DocumentException, IOException {
        SAXReader saxReader = new SAXReader();
        File file = new File(path);
        FileInputStream intput = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(intput));
        String tempString;//定义一个字符串，每一次读出该行字符串内容
        List<String> list = new ArrayList<>();//定义一个list字符串集合用来储存每一行的字符串信息
        //删除dtd声明
        while ((tempString = reader.readLine()) != null) {
            if (!"<!DOCTYPE nta PUBLIC '-//Uppaal Team//DTD Flat System 1.1//EN' 'http://www.it.uu.se/research/group/darts/uppaal/flat-1_2.dtd'>".equals(tempString)){
                list.add(tempString);
            }
        }

        FileWriter fd = new FileWriter(file, false);//append传入false表示写入内容时将会覆盖文件中之前存在的内容
        fd.write("");//执行删除操作，写入空内容覆盖之前的内容
        fd.close();
        FileWriter fw = new FileWriter(file, true);
        for (String s : list){
            fw.write(s);
            fw.write(System.getProperty("line.separator"));
            //System.out.println(s);
        }
        fw.close();
        Document document = saxReader.read(file);
        return document;
    }

    /**
     * 获取原自动机模型的tamplate
     * @param doc
     * @return
     */
    public static Element getTemplate(Document doc){
        Element template = null;
        template = doc.getRootElement().element("template");
        return template;
    }

    /**
     * 获取原自动机所有的location
     * @param template
     * @return
     */
    public static HashMap<String,Element> getAllLocationMapInTemplate(Element template){
        HashMap<String,Element> ret = new HashMap<>();
        Iterator locations = template.elementIterator("location");
        while(locations.hasNext()){
            Element location = (Element) locations.next();
            ret.put(location.attributeValue("id"),location);
        }
        return ret;
    }

    public static ArrayList<Element> getAllLocationsInTemplate(Element template){
        ArrayList<Element> ret = new ArrayList<>();
        Iterator locations = template.elementIterator("location");
        while(locations.hasNext()){
            Element location = (Element) locations.next();
            if(!location.attributeValue("id").equals("1000")){
                ret.add(location);
            }
        }
        return ret;
    }

    /**
     * 获取原自动机所有的边
     * @param template
     * @return
     */
    public static ArrayList<Element> getAllTransitionInTemplate(Element template){
        ArrayList<Element> ret = new ArrayList<>();
        Iterator transitions = template.elementIterator("transition");
        while(transitions.hasNext()){
            Element transition = (Element) transitions.next();
            ret.add(transition);
        }
        return ret;
    }

    /**
     * 将action的后缀(!,?)取反
     * @param template
     */
    public static void handleSuffix(Element template){
        for(Element transition : getAllTransitionInTemplate(template)){
            List<Element> labels = transition.elements("label");
            for(Element label : labels){
                if(label.attributeValue("kind").equals("synchronisation")){
                    String action = label.getText();
                    String sub = action.substring(0,action.length()-1);
                    if(action.contains("!")){
                        sub += "?";
                    }
                    if(action.contains("?")){
                        sub += "!";
                    }
                    label.setText(sub);
                }
            }
        }
    }

    /**
     * 根据action删除模版中的边，这些action按照不同的无干扰性进行不同的选择
     * @param actions
     */
    public static void delateEdge(Element template,HashSet<String> actions){
        for(Element transition : getAllTransitionInTemplate(template)){
            List<Element> labels = transition.elements("label");
            for(Element label : labels){
                if(label.attributeValue("kind").equals("synchronisation") && actions.contains(label.getText())){
                    transition.getParent().remove(transition);
                }
            }
        }
    }

    /**
     * 删除那些由于删除边而变得不可达的节点，使用队列实现
     * @param template
     */
    public static void delateUselessLocation(Element template){
        Queue<String> queue = new LinkedList<String>();
        HashSet<String> save = new HashSet<>();
        HashSet<String> del = new HashSet<>();
        //HashSet<String> hasVisit = new HashSet<>();
        queue.offer("id0");
        while(!queue.isEmpty()){
            String temp = queue.poll();
            save.add(temp);
            for(Element transition : getAllTransitionInTemplate(template)){
                if(transition.element("source").attributeValue("ref").equals(temp)){
                    if(!save.contains(transition.element("target").attributeValue("ref"))){ //判断是否访问过该节点
                        queue.offer(transition.element("target").attributeValue("ref"));
                    }
                }
            }
        }
        for(Element location : getAllLocationsInTemplate(template)){
            if(!save.contains(location.attributeValue("id"))){
                del.add(location.attributeValue("id"));
                location.getParent().remove(location);
            }
        }
        /**下面删除涉及这些位置的边**/
        for(Element transition : getAllTransitionInTemplate(template)){
            if(del.contains(transition.element("source").attributeValue("ref")) || del.contains(transition.element("target").attributeValue("ref"))){
                transition.getParent().remove(transition);
            }
        }
    }

    /**
     * 创建err位置,err位置的id默认为1000
     * @param template
     */
    public static void createErrLocation(Element template){
        Element err = DocumentHelper.createElement("location");
        Element name = DocumentHelper.createElement("name");
        name.setText("err");
        err.addAttribute("id","1000");
        err.addAttribute("color","#ff0000");
        err.add(name);
        template.elements().add(template.elements().indexOf(template.element("init")),err);
    }

    /**
     * 获取边上的guard，如果有的话
     * @param transition
     * @return
     */
    public static String getGuard(Element transition){
        String guard = "";
        for(Element label : transition.elements("label")){
            if(label.attributeValue("kind").equals("guard")){
                guard = label.getText();
            }
        }
        return guard;
    }

    /**
     * 获取位置上的invariant，如果有的话
     * @param location
     * @return
     */
    public static String getInvariant(Element location){
        String invariant = "";
        for(Element label : location.elements("label")){
            if(label.attributeValue("kind").equals("invariant")){
                invariant = label.getText();
            }
        }
        return invariant;
    }
    /**
     * 获取边上的action，如果有的话
     * @param transition
     * @return
     */
    public static String getAction(Element transition){
        String action = "";
        for(Element label : transition.elements("label")){
            if(label.attributeValue("kind").equals("synchronisation")){
                action = label.getText();
            }
        }
        return action;
    }

    /**
     * 不等式转换为区间
     * @param inequal
     * @return
     */
    public static Interval getInterval(String inequal){
        Interval ret = new Interval();
        if(inequal.contains("==")){
            ret.left = Integer.parseInt(inequal.replaceAll("[^0-9]",""));
            ret.right = ret.left;
        }else if(inequal.length() == 4){
            if(inequal.contains("c<=")){
                ret.left = 0;
                ret.right = inequal.charAt(inequal.length()-1) - 48;
            }
            if(inequal.contains("c>=")){
                ret.left = inequal.charAt(inequal.length()-1) - 48;
                ret.right = Integer.MAX_VALUE;
            }
            if(inequal.contains("<=c")){
                ret.left = inequal.charAt(0) - 48;
                ret.right = Integer.MAX_VALUE;
            }
            if(inequal.contains(">=c")){
                ret.left = 0;
                ret.right = inequal.charAt(0) - 48;
            }
        }else if(inequal.length() == 3){
            if(inequal.contains("c<")){
                ret.left = 0;
                ret.right = inequal.charAt(inequal.length()-1) - 48;
                ret.ropen = 1;
            }
            if(inequal.contains("c>")){
                ret.left = inequal.charAt(inequal.length()-1) - 48;
                ret.right = Integer.MAX_VALUE;
                ret.lopen = 1;
            }
            if(inequal.contains("<c")){
                ret.left = inequal.charAt(0) - 48;
                ret.right = Integer.MAX_VALUE;
                ret.lopen = 1;
            }
            if(inequal.contains(">c")){
                ret.left = 0;
                ret.right = inequal.charAt(0) - 48;
                ret.ropen = 1;
            }
        }
        return ret;
    }

    public static Interval mergeInterval(Interval i1,Interval i2){
        Interval ret = new Interval();
        if(i1.left <= i2.left){
            ret.left = i2.left;
            ret.lopen = i2.lopen;
        }else{
            ret.left = i1.left;
            ret.lopen = i1.lopen;
        }
        if(i1.right <= i2.right){
            ret.right = i1.right;
            ret.ropen = i1.ropen;
        }else{
            ret.right = i2.right;
            ret.ropen = i2.ropen;
        }
        if(i2.left == i1.left && i1.lopen!= i2.lopen){
            ret.lopen = 1;
        }
        if(i2.right == i1.right && i1.ropen!= i2.ropen){
            ret.ropen = 1;
        }
        return ret;
    }
    /**
     * 处理不等式的合取
     * @param invariant
     * @param guard
     * @return
     */
    public static String conjunction(String invariant,String guard){
        String newGuard = "";
        if(invariant.equals("") && !guard.equals("")){ //无不变量有guard
            newGuard = guard;
        }
        if(!invariant.equals("") && guard.equals("")){ //有不变量无guard
            newGuard = invariant;
        }
        if(!invariant.equals("") && !guard.equals("")){ //有不变量有guard
            Interval interval = null;
            if(guard.contains("&&")){ //guard为有界区间
                Pattern patten = Pattern.compile("c<=?\\d+|c>=?\\d+|\\d+>=?c|\\d+<=?c");//编译正则表达式
                Matcher matcher = patten.matcher(guard);// 指定要匹配的字符串
                List<String> matchStrs = new ArrayList<>();
                while (matcher.find()) { //此处find（）每次被调用后，会偏移到下一个匹配
                    matchStrs.add(matcher.group());//获取当前匹配的值
                }
                Interval intervalGuard1 = getInterval(matchStrs.get(0));
                Interval intervalGuard2 = getInterval(matchStrs.get(1));
                Interval intervalInvariant = getInterval(invariant);
                interval = mergeInterval(intervalGuard1,intervalGuard2);
                interval = mergeInterval(intervalInvariant,interval);

            }else{ //guard为无界区间
                Interval intervalGuard = getInterval(guard);
                Interval intervalInvariant = getInterval(invariant);
                interval = mergeInterval(intervalInvariant,intervalGuard);
            }
            if(interval.right!=Integer.MAX_VALUE){
                if(interval.ropen == 0){
                    newGuard = "c<=" + interval.right;
                }else{
                    newGuard = "c<" + interval.right;
                }
                if(interval.lopen == 0){
                    newGuard = interval.left + "<=c" + " && " + newGuard;
                }else{
                    newGuard = interval.left + "<c" + " && " + newGuard;
                }
            }else{
                if(interval.lopen == 0){
                    newGuard = interval.left + "<=c";
                }else{
                    newGuard = interval.left + "<c";
                }
            }
        }
        return newGuard;
    }

    /**
     * 修改自动机的边，将起始节点的不变量与guard合取作为新的不变量
     * @param template
     */
    public static void modifyGuard(Element template){
        HashMap<String,Element> locations =  getAllLocationMapInTemplate(template);
        ArrayList<Element> transitions = getAllTransitionInTemplate(template);
        for(Element transition : transitions){
            String invariant = "";
            String guard = getGuard(transition); //获取边上的guard
            Element source = transition.element("source");
            if(locations.containsKey(source.attributeValue("ref"))){ //查询边上的起始节点
                Element location = locations.get(source.attributeValue("ref"));
                if(location.element("label") != null){ //查询起始节点的不变量
                    invariant = location.element("label").getText();
                }
            }
            String newGuard = conjunction(invariant,guard);
            if(!newGuard.equals("")){
                if(guard.equals("")){
                    Element label = DocumentHelper.createElement("label");
                    label.addAttribute("kind","guard");
                    label.setText(newGuard);

                    if(transition.elements().indexOf(transition.element("nail"))>=0){
                        transition.elements().add(transition.elements().indexOf(transition.element("nail")),label);
                    }else{
                        transition.add(label);
                    }

                }else{
                    for(Element label : transition.elements("label")) {
                        if (label.attributeValue("kind").equals("guard")) {
                            label.setText(newGuard);
                        }
                    }
                }
            }
        }
    }

    /**
     * 创建自循环
     * @param template
     * @param id
     * @param action
     */
    public static void createLoop(Element template,String id,String action){
        Element edge = DocumentHelper.createElement("transition");
        Element source = DocumentHelper.createElement("source");
        source.addAttribute("ref",id);
        Element target = DocumentHelper.createElement("target");
        target.addAttribute("ref",id);
        Element label = DocumentHelper.createElement("label");
        label.addAttribute("kind","synchronisation");
        label.setText(action);
        edge.add(source);
        edge.add(target);
        edge.add(label);
        template.elements().add(template.elements().indexOf(template.element("init"))+1,edge);
    }

    /**
     * 创建边
     * @param template
     * @param sourceId
     * @param targetId
     * @param guard
     * @param action
     */
    public static void createEdge(Element template,String sourceId,String targetId,String guard,String action){
        Element edge = DocumentHelper.createElement("transition");
        Element source = DocumentHelper.createElement("source");
        source.addAttribute("ref",sourceId);
        Element target = DocumentHelper.createElement("target");
        target.addAttribute("ref",targetId);
        Element guardLabel = DocumentHelper.createElement("label");
        if(!guard.equals("")){
            guardLabel.addAttribute("kind","guard");
            guardLabel.setText(guard);
        }
        Element actionLabel = DocumentHelper.createElement("label");
        if(!action.equals("")){
            actionLabel.addAttribute("kind","synchronisation");
            actionLabel.setText(action);
        }
        edge.add(source);
        edge.add(target);
        if(!guard.equals("")){
            edge.add(guardLabel);
        }
        if(!action.equals("")){
            edge.add(actionLabel);
        }
        template.elements().add(template.elements().indexOf(template.element("init"))+1,edge);
    }

    /**
     * 细化自动机过滤错误的非公开动作
     * @param template
     * @param locations
     * @param transitions
     * @param lowLevelActions
     * @param allActions
     */
    public static void actionErrFilter(Element template,ArrayList<Element> locations,ArrayList<Element> transitions, HashSet<String> lowLevelActions,HashSet<String> allActions){
        for(Element location : locations) {
            HashSet<String> outEdgeActions = new HashSet<>();
            for (Element transition : transitions) {
                Element source = transition.element("source");
                if (location.attributeValue("id").equals(source.attributeValue("ref"))) {
                    outEdgeActions.add(getAction(transition));
                }
            }
            if(outEdgeActions.size()!=0){
                HashSet<String> set1 = new HashSet<>();
                set1.addAll(outEdgeActions);
                HashSet<String> set2 = new HashSet<>();
                set2.addAll(allActions);
                set1.addAll(lowLevelActions);
                set2.removeAll(set1);
                for (String action : set2) {
                    createLoop(template, location.attributeValue("id"), action);
                }
            }
        }
    }
    /**
     * 处理执行错误的动作
     * @param
     */
    public static void actionErr(Element template,ArrayList<Element> locations,ArrayList<Element> transitions, HashSet<String> lowLevelActions,HashSet<String> allActions){
        for(Element location : locations){
            HashSet<String> outEdgeActions = new HashSet<>();
            for(Element transition : transitions){
                Element source = transition.element("source");
                if(location.attributeValue("id").equals(source.attributeValue("ref"))){
                    outEdgeActions.add(getAction(transition));
                }
            }
            if(outEdgeActions.size()!=0){
                HashSet<String> set1 = new HashSet<>();
                set1.addAll(outEdgeActions);
                HashSet<String> set2 = new HashSet<>();
                set2.addAll(allActions);
                set1.addAll(lowLevelActions);
                set2.removeAll(set1);
                for(String action : set2){
                    createLoop(template,location.attributeValue("id"),action);
                }
                set1.clear();
                set2.clear();
                set1.addAll(outEdgeActions);
                set2.addAll(lowLevelActions);
                set2.removeAll(set1);
                //System.out.println(set2);
                for(String action : set2){
                    createEdge(template,location.attributeValue("id"),"1000",getInvariant(location),action);
                }
            }
        }
    }

    /**
     * 将区间取反
     * @param interval
     */
    public static void NOTInterval(Interval interval){
        if(interval.right == Integer.MAX_VALUE){
            if(interval.lopen == 0){
                interval.ropen = 1;
            }else{
                interval.ropen = 0;
            }
            interval.right = interval.left;
            interval.left = 0;
        }else{
            if(interval.ropen == 0){
                interval.lopen = 1;
            }else{
                interval.lopen = 0;
            }
            interval.left = interval.right;
            interval.right = Integer.MAX_VALUE;
        }
    }
    /**
     * 将guard取反
     * @param guard
     * @return
     */
    public static HashSet<String> NOTGuard(String guard){
        HashSet<String> ret = new HashSet<>();
        if(guard.contains("&&")){
            Pattern patten = Pattern.compile("c<=?\\d+|c>=?\\d+|\\d+>=?c|\\d+<=?c");//编译正则表达式
            Matcher matcher = patten.matcher(guard);// 指定要匹配的字符串
            List<String> matchStrs = new ArrayList<>();
            while (matcher.find()) { //此处find（）每次被调用后，会偏移到下一个匹配
                matchStrs.add(matcher.group());//获取当前匹配的值
            }
            Interval intervalGuard1 = getInterval(matchStrs.get(0));
            NOTInterval(intervalGuard1);
            Interval intervalGuard2 = getInterval(matchStrs.get(1));
            NOTInterval(intervalGuard2);
            ret.add(intervalToGuard(intervalGuard1));
            ret.add(intervalToGuard(intervalGuard2));
        }else{
            Interval intervalGuard = getInterval(guard);
            NOTInterval(intervalGuard);
            ret.add(intervalToGuard(intervalGuard));
        }
        return ret;
    }
    public static String intervalToGuard(Interval interval){
        String ret = "";
        if(interval.right == Integer.MAX_VALUE){
            if(interval.lopen == 1){
                ret = "c>" + interval.left;
            }else{
                ret = "c>=" + interval.left;
            }
        }else{
            if(interval.ropen == 1){
                ret = "c<" + interval.right;
            }else{
                ret = "c<=" + interval.right;
            }
        }
        return ret;
    }
    /**
     * 处理错误时间执行的正确动作
     * @param
     */
    public static void timeErr(Element template,ArrayList<Element> transitions,HashSet<String> lowLevelActions){
        for(Element transition : transitions){
            if(!getGuard(transition).equals("")){
                String sourceId = transition.element("source").attributeValue("ref");
                String guard = getGuard(transition);
                String action = getAction(transition);
                HashSet<String> notGuard = NOTGuard(guard);
                if(lowLevelActions.contains(action)){
                    for(String s : notGuard){
                        createEdge(template,sourceId,"1000",s,action);
                    }
                }else{
                    for(String s : notGuard){
                        createEdge(template,sourceId,sourceId,s,action);
                    }
                }
            }
        }
    }

    /**
     * 细化自动机过滤非公开动作的时间错误
     * @param template
     * @param transitions
     * @param lowLevelActions
     */
    public static void timeErrFilter(Element template,ArrayList<Element> transitions,HashSet<String> lowLevelActions){
        for(Element transition : transitions){
            if(!getGuard(transition).equals("")){
                String sourceId = transition.element("source").attributeValue("ref");
                String guard = getGuard(transition);
                if(guard.contains("==")){
                    guard = guard.charAt(3) + "<=c && c<=" + guard.charAt(3);
                }
                String action = getAction(transition);
                HashSet<String> notGuard = NOTGuard(guard);
                if(!lowLevelActions.contains(action)){
                    for(String s : notGuard){
                        createEdge(template,sourceId,sourceId,s,action);
                    }
                }
            }
        }
    }

    /**
     * 执行case 3
     * @param template
     * @param transitions
     * @param lowLevelActions
     */
    public static void fallBack(Element template,ArrayList<Element> transitions,HashSet<String> lowLevelActions){
        String rId= "";
        String nId= "10000"; //n位置的id默认为10000
        int id = 1;
        Element n = DocumentHelper.createElement("location");
        n.addAttribute("id",nId);
        n.addElement("name").addText("n");
        template.elements().add(template.elements().indexOf(template.element("init")),n);
        for(Element transition : transitions){
            String action = getAction(transition);
            if(lowLevelActions.contains(action)){
                String guard = getGuard(transition);
                rId = "100" + (id++);
                Element r = DocumentHelper.createElement("location");
                r.addAttribute("id",rId);
                r.addElement("name").addText("r"+id);
                template.elements().add(template.elements().indexOf(template.element("init")),r);
                createEdge(template,transition.element("source").attributeValue("ref"),rId,guard,"");
                createEdge(template,rId,nId,"",action);
                createEdge(template,rId,"1000","","fallback?");
            }
        }
    }

    public static void SIRFallBack(Element template,ArrayList<Element> transitions,HashSet<String> lowLevelActions){
        String rId= "";
        String nId= "10000"; //n位置的id默认为10000
        int id = 1;
        Element n = DocumentHelper.createElement("location");
        n.addAttribute("id",nId);
        n.addElement("name").addText("n");
        template.elements().add(template.elements().indexOf(template.element("init")),n);
        for(Element transition : transitions){
            String action = getAction(transition);
            if(lowLevelActions.contains(action) && action.contains("!")){ //action为公开动作且为输入动作
                String guard = getGuard(transition);
                rId = "100" + (id++);
                Element r = DocumentHelper.createElement("location");
                r.addAttribute("id",rId);
                r.addElement("name").addText("r"+id);
                template.elements().add(template.elements().indexOf(template.element("init")),r);
                createEdge(template,transition.element("source").attributeValue("ref"),rId,guard,"");
                createEdge(template,rId,nId,"",action);
                createEdge(template,rId,"1000","","fallback?");
            }
        }
    }

    /**
     * 删除不变量
     * @param locations
     */
    public static void delateInvariant(ArrayList<Element> locations){
        for(Element location : locations){
            for(Element e : location.elements("label")){
                if(e.attributeValue("kind").equals("invariant")){
                    location.remove(e);
                }
            }
        }
    }

    /**
     * 添加fallback通道
     * @param document
     */
    public static void addFallback(Document document){
        Element declaration = document.getRootElement().element("declaration");
        String chan = declaration.getText();
        chan += "chan fallback;";
        declaration.setText(chan);
    }

    public static void addPriorityForBNNIandBSNNI(Document document){
        Element declaration = document.getRootElement().element("declaration");
        String chan = declaration.getText();
        chan += "chan priority fallback <default;";
        declaration.setText(chan);
    }

    public static void addPriorityForSIRNNI(Document document){
        Element declaration = document.getRootElement().element("declaration");
        String chan = declaration.getText();
        String[] ss = chan.split("\\n");
        ArrayList<String> s = new ArrayList<>();
        for(String t : ss){
            if(t.contains("chan ") && !t.contains("fallback")){
                t = t.replace("chan ","");
                t = t.replace(";","");
                s.add(t);
            }
        }
        String temp = s.get(0);
        for(int i = 1;i< s.size();i++){
            temp = temp + "," + s.get(i);
        }
        chan += "chan priority fallback < default;";
        declaration.setText(chan);
    }
    /**
     * 复制原自动机用于构造测试自动机和精化自动机
     * @param document
     * @return
     */
    public static Element createTemplate(Document document){
        Element template = (Element) getTemplate(document).clone();
        template.setParent(null);
        return template;
    }

    /**
     * 获取全局声明以便读取不同等级动作
     * @param document
     * @return
     */
    public static Element getGlobalDeclaration(Document document){
        Element globalD = document.getRootElement().element("declaration");
        return globalD;
    }

    /**
     * 获取全局声明中定义的动作的集合，按照先后定义的先后顺序分别为机密动作集合，公开动作集合；
     * @param doc
     * @return ArrayList<HashSet<String>>
     */
    public static ArrayList<HashSet<String>> getSet(Document doc){
        Element globalD = getGlobalDeclaration(doc);
        HashSet allActions = getAllActionsWithSuffix(getTemplate(doc));
        ArrayList<HashSet<String>> ret = new ArrayList<>();
        String s = globalD.getText();
        //第一步按照换行符将字符串拆分
        String[] ss = s.split("\\n");
        for(String t : ss){
            if(t.contains("chan ")){
                t = t.replace("chan ","");
                t = t.replace(";","");
                HashSet<String> set = new HashSet<>();
                String[] actions = t.split(",");
                for(String action : actions){
                    action = action.replaceAll("\\s","");
                    if(allActions.contains(action+"!")){
                        action = action + "?";
                    }else{
                        action = action + "!";
                    }
                    set.add(action);
                }
                ret.add(set);
            }
        }
        return ret;
    }

    /**
     * 获取所有的动作（带后缀的）
     * @param template
     * @return
     */
    public static HashSet<String> getAllActionsWithSuffix(Element template){
        HashSet<String> ret = new HashSet<>();
        ArrayList<Element> transitions = getAllTransitionInTemplate(template);
        for(Element transition : transitions){
            ret.add(getAction(transition));
        }
        return ret;
    }

    /**
     * 修改初始位置
     * @param path
     * @return
     */
    public static ArrayList<String> initLocationModify(String p,HashSet<String> highActions,String path, SymbolicTrace trace) throws DocumentException, IOException {
        ArrayList<String> ret = new ArrayList<>();
        ArrayList<String> traces = Verify.getAllTrace(trace);
        /**
         * 处理trace的逻辑,两种情况需要考虑：
         * 1.经过r位置到达err位置:这种情况说明ref不能提供test所要求的动作迁移
         * 2.不经过r位置直接到达err位置：这种情况说明ref提供的动作迁移不满足test的要求
         */
        ArrayList<String> temp = new ArrayList<>();
        String l_test = "";
        String l_ref = "";
        String s = traces.get(traces.size()-1); //最后一个迁移总是包含err的
        temp.add(s.split(", ")[0]);
        temp.add(s.split(", ")[1]);
        String err = "";
        String notErr = "";
        for(String s1 : temp){
            if(s1.contains("err")){
                err = s1;
            }else{
                notErr = s1;
            }
        }
        String test = err.split(" \u2192 ")[0];
        String ref = "";
        System.out.println(test);
        if(test.contains("r") && !p.equals("SIRNNI")){  //通过r到达的err
            if(traces.size()>=3){ //输出的trace超过三行，说明倒数第三行为l_test l_ref
                s = traces.get(traces.size()-3);
                if(s.split(", ")[0].contains("test")){
                    test = s.split(", ")[0];
                    ref = s.split(", ")[1];
                }else{
                    test = s.split(", ")[1];
                    ref = s.split(", ")[0];
                }
                l_test = test.split(" \u2192 ")[1];
                l_ref = ref.split(" \u2192 ")[1];
            }
        }else if(!test.contains("r")){ //直接到达的err
            l_test = test.split(": ")[1];
            l_ref = notErr.split(" \u2192 ")[1];
        }
        Document doc = getDocumentByPath(path);
        Element etest = null;
        Element eref = null;
        for(Element e : doc.getRootElement().elements("template")){
            if(e.element("name").getText().equals("test")){
                etest = e;
            }
            if(e.element("name").getText().equals("ref")){
                eref = e;
            }
        }
        if(ref.equals("") && !p.equals("SIRNNI")){  //如果到这里ref的值还是初始值的话,说明通过r到达的err的情况下输出的trace少于三行，说明当前精化自动机模型的初始位置就未通过测试自动机的测试
            l_ref = eref.element("init").attributeValue("ref");
            for(Element e : getAllLocationsInTemplate(eref)){
                if(e.attributeValue("id").equals(l_ref)){
                    l_ref = e.element("name").getText();
                    break;
                }
            }
            l_test = traces.get(0).split(" \u2192 ")[0].split(": ")[1];
        }
        //System.out.println("xxx"+l_ref);
        ArrayList<String> nexts = getAllHighNext(highActions,eref,l_ref);
        if(nexts.isEmpty()){
            return ret;
        }
        String l_test_id = "";
        for(Element e : getAllLocationsInTemplate(etest)){ //找到l_test的id
            if(e.element("name").getText().equals(l_test)){
                l_test_id = e.attributeValue("id");
            }
        }
        /********************输出文件的格式********************/
        /****/OutputFormat format = new OutputFormat();/****/
        /****/format.setIndentSize(2);                 /****/
        /****/format.setNewlines(true);                /****/
        /****/format.setTrimText(true);                /****/
        /****/format.setPadText(true);                 /****/
        /****/format.setEncoding("utf-8");             /****/
        /****/XMLWriter writer = null;                 /****/
        /***************************************************/
        for(int i=0;i<nexts.size();i++){ //生成xml文件
            Document document = doc.getDocument();
            for(Element e : document.getRootElement().elements("template")){
                if(e.element("name").getText().equals("test")){
                    Attribute attribute = e.element("init").attribute("ref");
                    attribute.setValue(l_test_id);
                }if(e.element("name").getText().equals("ref")){
                    Attribute attribute = e.element("init").attribute("ref");
                    attribute.setValue(nexts.get(i));
                }
            }
            FileOutputStream file =  new FileOutputStream(path.split("\\.")[0]+i+".xml");
            writer = new XMLWriter(file, format);
            writer.write(document);
            ret.add(path.split("\\.")[0]+i+".xml");
        }
        writer.close();
        return ret;
    }

    /**
     * 获取l_ref的next的集合
     * @param eref
     * @param l_ref
     * @return
     */
    public static ArrayList<String> getAllHighNext(HashSet<String> highActions,Element eref,String l_ref){
        ArrayList<String> ret = new ArrayList<>();
        ArrayList<String> highActionsNoSuffix = new ArrayList<>();
        for(String s : highActions){
            if(s.contains("!")){
                s = s.split("!")[0];
                highActionsNoSuffix.add(s);
            }
            if(s.contains("?")){
                s = s.split("\\?")[0];
                highActionsNoSuffix.add(s);
            }
        }
        System.out.println(highActionsNoSuffix);
        String l_ref_id = "";
        for(Element e : getAllLocationsInTemplate(eref)){
            if(e.element("name").getText().equals(l_ref)){
                l_ref_id = e.attributeValue("id");
            }
        }
        for(Element e : getAllTransitionInTemplate(eref)){
            for(Element e1 : e.elements("label")){
                if(e.element("source").attributeValue("ref").equals(l_ref_id) &&
                e1.attributeValue("kind").equals("synchronisation") &&
                        !e.element("target").attributeValue("ref").equals(l_ref_id)){
                    String ss = e1.getText();
                    if(ss.contains("!")){
                        ss = ss.split("!")[0];
                    }
                    if(ss.contains("?")){
                        ss = ss.split("\\?")[0];
                    }
                    if(highActionsNoSuffix.contains(ss)){
                        ret.add(e.element("target").attributeValue("ref"));
                    }
                }
            }
        }
        return ret;
    }

}
