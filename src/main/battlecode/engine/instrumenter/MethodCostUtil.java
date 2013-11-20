package battlecode.engine.instrumenter;

import battlecode.engine.ErrorReporter;
import org.objectweb.asm.ClassReader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import static org.objectweb.asm.ClassReader.SKIP_DEBUG;

/**
 * MethodCostUtil is a singleton used for looking up MethodData associated with some methods.
 * <p/>
 *
 * @author adamd
 */
public class MethodCostUtil {

    private MethodCostUtil() {
    }

    /**
     * This is a map from method names (in the format 'ClassName/methodName'), to the MethodData associated with each method.
     */
    private final static Map<String, MethodData> methodCosts;

    /**
     * This is a map from binary class names, to all the classes/interfaces that the class transitively implements/extends.
     */
    private final static Map<String, String[]> interfacesMap;

    /**
     * A struct that stores data about a method -- what its lookup bytecode cost is, and whether it should end the basic block or not.
     */
    public static class MethodData {
        public final int cost;
        public final boolean shouldEndRound;

        public MethodData(int cost, boolean shouldEndRound) {
            this.cost = cost;
            this.shouldEndRound = shouldEndRound;
        }
    }

    static {
        BufferedReader reader;
        String line;

        methodCosts = new HashMap<String, MethodData>();
        // load method costs
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream("MethodCosts.txt")));
            while ((line = reader.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                if (st.countTokens() != 3)
                    ClassReferenceUtil.fileLoadError("MethodCosts.txt");
                methodCosts.put(st.nextToken(), new MethodData(Integer.parseInt(st.nextToken()), Boolean.parseBoolean(st.nextToken())));
            }
        } catch (IOException e) {
            ClassReferenceUtil.fileLoadError("MethodCosts.txt");
        }

        interfacesMap = new HashMap<String, String[]>();
    }

    public static MethodData getMethodDataRaw(String fullName) {
        return methodCosts.get(fullName);
    }

    /**
     * Returns the MethodData associated with the given method, or null if no MethodData exists for the given method.
     *
     * @param className  the binary name of the class to which the given method belongns
     * @param methodName the name of the given class
     */
    public static MethodData getMethodData(String className, String methodName) {
        if (className.charAt(0) == '[')
            return null;
        String key = className + "/" + methodName;

        if (methodCosts.containsKey(key))
            return methodCosts.get(key);

        String[] interfaces = null;
        if (interfacesMap.containsKey(className))
            interfaces = interfacesMap.get(className);
        else {
            ClassReader cr;
            try {
                cr = new ClassReader(className);
            } catch (IOException ioe) {
                ErrorReporter.report("Can't find the class \"" + className + "\", and this wasn't caught until the MethodData stage.", true);
                // this isn't all that bad an error, so don't throw an InstrumentationException
                return null;
            }
            InterfaceReader ir = new InterfaceReader();
            cr.accept(ir, SKIP_DEBUG);
            interfaces = ir.getInterfaces();
            interfacesMap.put(className, interfaces);
        }

        for (int i = 0; i < interfaces.length; i++) {
            key = interfaces[i] + "/" + methodName;
            if (methodCosts.containsKey(key))
                return methodCosts.get(key);
        }

        return null;
    }


}
