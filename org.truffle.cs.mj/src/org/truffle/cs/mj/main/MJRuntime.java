
package org.truffle.cs.mj.main;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

import org.truffle.cs.mj.parser.RecursiveDescendScanner;
import org.truffle.cs.mj.parser.RecursiveDescentParser;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleRuntime;

public class MJRuntime {

    public static void main(String[] args) {
// parseRD(mjSimpleRD);
// parseRD(mjProgramRD);
// parseRD(mjSimpletIfRD);
// parseRD(mjSimpletWhileRD);
// parseRD(ifProgram);
// parseRD(test_f);
// parseRD(factorial);
// parseRD(ifProgram);
// parseRD(ifProgram);
// parseRD(divAlgorithm);
// parseRD(absTets);
// parseRD(staticTypingTets);
// parseRD(finalTypesTets);
        parseRD(charTest);
    }

    static String charTest = ""//
                    + "program Sample final int c_i = 11; { "//

                    + "     void main(int arg) char c; int i; {\n"//
// + " i = 41;"//
// + " i += c_i;"//
// + " print(i);"//
                    + " print(c_i);"//
                    + "     }\n"//
                    + "}";

    static String factorial = ""//
                    + "program Sample { "//
                    + ""//
                    + "     int func(int i){"//
                    + "             if (i == 1){return 1;}"//
                    + "             else "//
                    + "             if (i == 2){"//
                    + "                 return 1;} "//
                    + "             else {"//
                    + "                 return func(i-2) + func(i-1);"//
                    + "             }"//
                    + "         }"//
                    + "     void main(int arg) int k, l; float p, f, s;{\n"//
// + " print(func(6));"//
                    + "         p = 34;"//
                    + "         f = 345;"//
                    + "         s = p/f;"//
                    + "         print (s);"//
    static String finalTypesTets = ""//
                    + "program Sample final int c_i = 11;  final float c_f = 11; final boolean c_b = 11;{ "//

                    + "     void pp(int i, float f, boolean b) {\n"//
                    + "         print(b);\n"//
                    + "         print(i);\n"//
                    + "         print(f);\n"//
                    + "     }\n"//

                    + "     void main(int arg) int i; float f; boolean b; {\n"//
                    + "         i = 6;"//
                    + "         f = 7.0;"//
                    + "         b = true;"//
// + " pp(i, f, b);"//
                    + "         pp(c_i, c_f, c_b);"//
                    + "         b = true;"//
                    // + " i = 3.0;"//
// + " f = 5;"//
// + " b = 23;"//
                    + " pp(i, f, b);"//
                    + "     }\n"//
                    + "}";

    static String staticTypingTets = ""//
                    + "program Sample { "//
                    + " void pp(int i, float f, boolean b) {\n"//
                    + "     print(b);\n"//
                    + "     print(i);\n"//
                    + "     print(f);\n"//
                    + " }\n"//
                    + "     void main(int arg) int i; float f; boolean b; {\n"//
                    + "         i = 6;"//
                    + "         f = 7.0;"//
                    + "         b = true;"//
                    + "         pp(i, f, b);"//
// + " i = 3.0;"//
// + " f = 5;"//
// + " b = 23;"//
                    + " pp(i, f, b);"//
                    + "     }\n"//
                    + "}";

    static String absTets = ""//
                    + "program Sample { "//
                    + "     void main(int i){\n"//
                    + "         print(abs(6));"//
                    + "         print(abs(-6));"//
                    + "         print(abs(6.1));"//
                    + "         print(abs(-6.1));"//
                    + "     }\n"//
                    + "}";

    static String factorial = ""//
                    + "program Sample { "//
                    + ""//
                    + "     int func(int i){"//
                    + "             if (i == 1){return 1;}"//
                    + "             else "//
                    + "             if (i == 2){"//
                    + "                 return 1;} "//
                    + "             else {"//
                    + "                 return func(i-2) + func(i-1);"//
                    + "             }"//
                    + "         }"//
                    + "     void main(int i){\n"//
                    + "         print(func(6));"//
                    + "     }\n"//
                    + "}";

    static String mjSimpletWhileRD = ""//
                    + "program Sample { "//
                    + "     void main(int i) int p; {\n"//
                    + "         p = 123;\n"//
                    + "         while(p < 1000){\n"//
                    + "             print(p);"//
                    + "             p = p + 1;\n"//
                    + "         }\n"//
                    + "         return p;\n"//
                    + "     }\n"//
                    + "}";

    static String recursionTest = ""//
                    + "program Sample { "//
                    + " void foo(int i) {\n" //
                    + "     print(i);\n" //
                    + "     if (i > 0) {\n"//
                    + "         return foo(i - 1);\n" //
                    + "     }\n" //
                    + "}\n"//

                    + " void main(int i) int p; {\n" //
                    + "     foo(i);\n" //
                    + " }\n"//
                    + "}";

    static String mjSimpleRD = ""//
                    + "program Sample { "//
                    + " void main(int i) int p; {\n"//
                    + "     p=i+1;\n"//
                    + "     print(p);\n"//
                    + " }\n"//
                    + "}";

    static String mjSimpletIfRD = ""//
                    + "program Sample { "//
                    + " void main(int i) {\n"//
                    + "     if(i==0 || i > 5000){\n"//
                    + "         print(0);\n"//
                    + "     } else {\n"//
                    + "         print(1);\n"//
                    + "     }\n"//
                    + " }\n"//
                    + "}";

    static String mjProgramRD = ""//
                    + "program Sample { "//
                    + "void main() int i; int j; { \n"//
                    + "                 print(0);"//
                    + "                 print(12); \n" //
                    + "                 i = 3;\n"//
                    + "                 print(i);\n"//
                    + "                 print(i+12);\n"//
                    + "                 j=12;\n"//
                    + "                 print(i+j+12);\n"//
                    + "         }\n"//
                    + "}";

    static String whileLoopRD = "program P {"//
                    + "             void foo(int i, float j) {print(i+j);}" //
                    + "             void main () int i;{ "//
                    + "                 i =0; "//
                    + "                 while(i<10) {"//
                    + "                     print(i); "//
                    + "                     i=i+1;"//
                    + "                     foo(i,2.5);" //
                    + "                 }"//
                    + "             }"//
                    + "}";

    static String ifProgram = "program P {"//
                    + "             void foo(int i,int j) {"//
                    + "                 if(i>j) {"//
                    + "                     print(i);" //
                    + "                 }else {"//
                    + "                     print(j);"//
                    + "                 }"//
                    + "                 print(0);"//
                    + "             }" //
                    + "             void main () int i;{ "//
                    + "                 i =0; "//
                    + "                 while(i<10) {"//
                    + "                     i=i+1;"//
                    + "                     foo(i,5);" //
                    + "                 }"//
                    + "             }"//
                    + "}";

    static String divAlgorithm = "program DivAlgorithm {"//
                    + "             int flipSign(int a) int neg;int tmp; int tmpA; {" //
                    + "                 neg = 0;"//
                    + "                 tmp = 0;" //
                    + "                 tmpA = a;"//
                    + "                 if(a < 0){"//
                    + "                     tmp = 1;"//
                    + "                 } else {"//
                    + "                     tmp = -1;"//
                    + "                 }" //
                    + "                 while(tmpA != 0) {"//
                    + "                     neg = neg + tmp;"//
                    + "                     tmpA = tmpA + tmp;"//
                    // + " print(tmpA);"//
                    // + " print(neg);"//
                    + "                 }"//
                    + "                 return neg;"//
                    + "             }"//

                    + "             int sub(int a,int b) {"//
                    + "                 return a + flipSign(b);"//
                    + "             }"//

                    + "             int mul(int a,int b) int sum;int i; {"//
                    + "                 if(a<b) {"//
                    + "                     return mul(b,a);"//
                    + "                 }"//
                    + "                 sum = 0;"//
                    + "                 i =abs(b);"//
                    + "                 while(i>0) {"//
                    + "                     sum = sum +a;"//
                    + "                     i = i-1;"//
                    + "                 }"//
                    + "                 if(b < 0){"//
                    + "                     sum = flipSign(sum);"//
                    + "                 }"//
                    + "                 return sum;"//
                    + "             }" //

                    + "             void main (int a,int b){ " //
                    + "                 print(mul(a,b));"//
                    + "             }"//
                    + "}";

    static void parseRD(String code) {
        InputStream is = new ByteArrayInputStream(code.getBytes());
        RecursiveDescendScanner scanner = new RecursiveDescendScanner(new InputStreamReader(is));
        RecursiveDescentParser parser = new RecursiveDescentParser(scanner);
        parser.parse();
        TruffleRuntime runtime = Truffle.getRuntime();
        System.out.println("Calling main function...");
        CallTarget callTarget = runtime.createCallTarget(parser.getMain());
        for (int i = 0; i < 1; i++) {
            callTarget.call(i, i + 1000);
        }
    }

    static void parseRDBenchmark(String code) {
        InputStream is = new ByteArrayInputStream(code.getBytes());
        RecursiveDescendScanner scanner = new RecursiveDescendScanner(new InputStreamReader(is));
        RecursiveDescentParser parser = new RecursiveDescentParser(scanner);
        parser.parse();
        TruffleRuntime runtime = Truffle.getRuntime();
        System.out.println("Calling main function...");
        CallTarget callTarget = runtime.createCallTarget(parser.getMain());
        System.out.println("#################################################################");
        Random r = new Random(17);

        long start = System.currentTimeMillis();
        callTarget.call(123123, -12312312);
        long initialTimeNeeded = (System.currentTimeMillis() - start);
        System.out.println("Time needed " + initialTimeNeeded);

        // warmup
        for (int i = 0; i < 100; i++) {
            callTarget.call(i, i % 2 == 0 ? -i : i);
            callTarget.call(i % 2 == 0 ? -i : i, i);
        }
        for (int i = 0; i < 1000; i++) {
            callTarget.call(i, r.nextInt(1000));
        }
        System.out.println("#################################################################");
        start = System.currentTimeMillis();
        callTarget.call(123123, -12312312);
        System.out.println("Time needed " + (System.currentTimeMillis() - start) + "  | vs initial time=" + initialTimeNeeded);
        System.out.println("#################################################################");
    }

}
