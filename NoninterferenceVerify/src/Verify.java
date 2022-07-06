import com.uppaal.engine.*;
import com.uppaal.model.core2.Document;
import com.uppaal.model.core2.PrototypeDocument;
import com.uppaal.model.core2.Query;
import com.uppaal.model.system.SystemEdge;
import com.uppaal.model.system.UppaalSystem;
import com.uppaal.model.system.concrete.ConcreteTrace;
import com.uppaal.model.system.symbolic.SymbolicTrace;
import com.uppaal.model.system.symbolic.SymbolicTransition;
import org.dom4j.DocumentException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

public class Verify {
    public static SymbolicTrace strace = null;
    public static ConcreteTrace ctrace = null;

    public static String uppaal_path = "uppaal64-4.1.26-1/bin-Linux/server";
    /**
     * 载入UPPAAL模型
     * @param location
     * @return
     * @throws IOException
     */
    public static Document loadModel(String location) throws IOException
    {
        try {
            // try URL scheme (useful to fetch from Internet):
            return new PrototypeDocument().load(new URL(location));
        } catch (MalformedURLException ex) {
            // not URL, retry as it were a local filepath:
            return new PrototypeDocument().load(new URL("file", null, location));
        }
    }

    /**
     * 连接到验证引擎
     * @return
     * @throws EngineException
     * @throws IOException
     */
    public static Engine connectToEngine() throws EngineException, IOException
    {
        String path = uppaal_path;//"/home/suncong/TINIVER/uppaal64-4.1.26-1/bin-Linux/server";
        String os = System.getProperty("os.name");
//        String here = System.getProperty("user.dir");
        if(os.equals("MAC OS X")){
            path = "/Applications/uppaal64-4.1.24/bin-Darwin/server";
        }
//        System.out.println(os+"---"+here);
        Engine engine = new Engine();
        engine.setServerPath(path);
        engine.setServerHost("localhost");
        engine.setConnectionMode(EngineStub.BOTH);
        engine.connect();
        return engine;
    }

    /**
     * 编译UPPAAL模型为system
     * @param engine
     * @param doc
     * @return
     * @throws EngineException
     * @throws IOException
     */
    public static UppaalSystem compile(Engine engine, Document doc)
            throws EngineException, IOException
    {
        // compile the model into system:
        ArrayList<Problem> problems = new ArrayList<>();
        UppaalSystem sys = engine.getSystem(doc, problems);
        if (!problems.isEmpty()) {
            boolean fatal = false;
            System.out.println("There are problems with the document:");
            for (Problem p : problems) {
                System.out.println(p.toString());
                if (!"warning".equals(p.getType())) { // ignore warnings
                    fatal = true;
                }
            }
            if (fatal) {
                System.exit(1);
            }
        }
        return sys;
    }

    /**
     * 输出符号trace
     * @param trace
     */
    public static void printTrace(SymbolicTrace trace)
    {
        if (trace == null) {
            System.out.println("(null trace)");
            return;
        }
        Iterator<SymbolicTransition> it = trace.iterator();
        it.next().getTarget();
        while (it.hasNext()) {
            SymbolicTransition tr = it.next();
            if (tr.getSize()==0) {
                // no edges, something special (like "deadlock" or initial state):
                System.out.println(tr.getEdgeDescription());
            } else {
                // one or more edges involved, print them:
                boolean first = true;
                for (SystemEdge e: tr.getEdges()) {
                    if (first) first = false; else System.out.print(", ");
                    System.out.print(e.getProcessName()+": "
                            + e.getEdge().getSource().getPropertyValue("name")
                            + " \u2192 "
                            + e.getEdge().getTarget().getPropertyValue("name"));
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    public static ArrayList<String> getAllTrace(SymbolicTrace trace){
        ArrayList<String> ret = new ArrayList<>();
        Iterator<SymbolicTransition> it = trace.iterator();
        it.next().getTarget();
        while (it.hasNext()) {
            SymbolicTransition tr = it.next();
            if (tr.getSize()==0) {
                // no edges, something special (like "deadlock" or initial state):
                System.out.println(tr.getEdgeDescription());
            } else {
                // one or more edges involved, print them:
                boolean first = true;
                String temp = "";
                for (SystemEdge e: tr.getEdges()) {
                    if(first){
                        first = false;
                    }else{
                        temp += ", ";
                    }
                    temp += e.getProcessName()+": "
                            + e.getEdge().getSource().getPropertyValue("name")
                            + " \u2192 "
                            + e.getEdge().getTarget().getPropertyValue("name");
                }
                ret.add(temp);
            }
        }
        return ret;
    }

    //用于保存可达性分析结果
    public static boolean result = false;
    /**
     * 验证方法,使用栈实现递归
     * @param path
     * @throws IOException
     * @throws EngineException
     */
    public static void verify(String p,HashSet<String> highActions, String path) throws IOException, EngineException, DocumentException {
        // connect to the engine server:
        Engine engine = connectToEngine();
        Stack<String> to_be_validated = new Stack<>();
        to_be_validated.push(path);
        Document doc = null;
        while(!to_be_validated.isEmpty()){
            String temp = to_be_validated.pop();
            doc = loadModel(temp);
            queryErrReachability(engine,doc);
            if(null != strace){
                result = result || false;
                //修改初始位置得到性的xml文件然后入栈
                for(String s : Parse.initLocationModify(p,highActions,temp,strace)){
                    to_be_validated.push(s);
                }
            }else{
                result = result || true;
                break;
            }
        }
        if(result){
            System.out.println("满足"+p);
        }else{
            System.out.println("不满足"+p);
        }
        engine.disconnect();
    }
    public static void queryErrReachability(Engine engine,Document doc) throws EngineException, IOException {
        // compile the document into system representation:
        UppaalSystem sys = compile(engine, doc);
        Query query = new Query("A[] not test.err", null);
        System.out.println("===== Symbolic check: "+query.getFormula()+" =====");
        strace = null;
        System.out.println("Result: "
                +engine.query(sys, options, query, qf));
        printTrace(strace);
    }
    public static final String options = "--search-order 0 --diagnostic 0";
    // see "verifyta --help" for the description of options

    public static QueryFeedback qf =
            new QueryFeedback() {
                @Override
                public void setProgressAvail(boolean availability)
                {
                }

                @Override
                public void setProgress(int load, long vm, long rss, long cached, long avail, long swap, long swapfree, long user, long sys, long timestamp)
                {
                }

                @Override
                public void setSystemInfo(long vmsize, long physsize, long swapsize)
                {
                }

                @Override
                public void setLength(int length)
                {
                }

                @Override
                public void setCurrent(int pos)
                {
                }

                @Override
                public void setTrace(char result, String feedback,
                                     SymbolicTrace trace, QueryResult queryVerificationResult)
                {
                    strace = trace;
                }

                public void setTrace(char result, String feedback,
                                     ConcreteTrace trace, QueryResult queryVerificationResult)
                {
                    ctrace = trace;
                }
                @Override
                public void setFeedback(String feedback)
                {
                    if (feedback != null && feedback.length() > 0) {
                        System.out.println("Feedback: "+feedback);
                    }
                }

                @Override
                public void appendText(String s)
                {
                    if (s != null && s.length() > 0) {
                        System.out.println("Append: "+s);
                    }
                }

                @Override
                public void setResultText(String s)
                {
                    if (s != null && s.length() > 0) {
                        System.out.println("Result: "+s);
                    }
                }
            };
}
