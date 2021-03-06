package org.truffle.cs.mj.parser;

import static org.truffle.cs.mj.parser.Token.Kind.and;
import static org.truffle.cs.mj.parser.Token.Kind.assign;
import static org.truffle.cs.mj.parser.Token.Kind.break_;
import static org.truffle.cs.mj.parser.Token.Kind.charConst;
import static org.truffle.cs.mj.parser.Token.Kind.class_;
import static org.truffle.cs.mj.parser.Token.Kind.comma;
import static org.truffle.cs.mj.parser.Token.Kind.continue_;
import static org.truffle.cs.mj.parser.Token.Kind.else_;
import static org.truffle.cs.mj.parser.Token.Kind.eof;
import static org.truffle.cs.mj.parser.Token.Kind.final_;
import static org.truffle.cs.mj.parser.Token.Kind.ident;
import static org.truffle.cs.mj.parser.Token.Kind.if_;
import static org.truffle.cs.mj.parser.Token.Kind.lbrace;
import static org.truffle.cs.mj.parser.Token.Kind.lbrack;
import static org.truffle.cs.mj.parser.Token.Kind.lpar;
import static org.truffle.cs.mj.parser.Token.Kind.minus;
import static org.truffle.cs.mj.parser.Token.Kind.new_;
import static org.truffle.cs.mj.parser.Token.Kind.number;
import static org.truffle.cs.mj.parser.Token.Kind.or;
import static org.truffle.cs.mj.parser.Token.Kind.period;
import static org.truffle.cs.mj.parser.Token.Kind.plus;
import static org.truffle.cs.mj.parser.Token.Kind.print;
import static org.truffle.cs.mj.parser.Token.Kind.program;
import static org.truffle.cs.mj.parser.Token.Kind.rbrace;
import static org.truffle.cs.mj.parser.Token.Kind.rbrack;
import static org.truffle.cs.mj.parser.Token.Kind.read;
import static org.truffle.cs.mj.parser.Token.Kind.rem;
import static org.truffle.cs.mj.parser.Token.Kind.return_;
import static org.truffle.cs.mj.parser.Token.Kind.rpar;
import static org.truffle.cs.mj.parser.Token.Kind.semicolon;
import static org.truffle.cs.mj.parser.Token.Kind.slash;
import static org.truffle.cs.mj.parser.Token.Kind.times;
import static org.truffle.cs.mj.parser.Token.Kind.void_;
import static org.truffle.cs.mj.parser.Token.Kind.while_;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.truffle.cs.mj.nodes.MJAbsNodeGen;
import org.truffle.cs.mj.nodes.MJBinaryNodeFactory;
import org.truffle.cs.mj.nodes.MJBlock;
import org.truffle.cs.mj.nodes.MJBreakNode;
import org.truffle.cs.mj.nodes.MJContinueNode;
import org.truffle.cs.mj.nodes.MJContstantBooleanNodeGen;
import org.truffle.cs.mj.nodes.MJContstantCharNodeGen;
import org.truffle.cs.mj.nodes.MJContstantFloatNodeGen;
import org.truffle.cs.mj.nodes.MJContstantIntNodeGen;
import org.truffle.cs.mj.nodes.MJExpresionNode;
import org.truffle.cs.mj.nodes.MJFunction;
import org.truffle.cs.mj.nodes.MJIfNode;
import org.truffle.cs.mj.nodes.MJInvokeNode;
import org.truffle.cs.mj.nodes.MJPrintNodeGen;
import org.truffle.cs.mj.nodes.MJReadParameterNode;
import org.truffle.cs.mj.nodes.MJReturnNode;
import org.truffle.cs.mj.nodes.MJStatementExpresion;
import org.truffle.cs.mj.nodes.MJStatementNode;
import org.truffle.cs.mj.nodes.MJUnaryNodeFactory;
import org.truffle.cs.mj.nodes.MJVariableNode.MJReadLocalVariableNode;
import org.truffle.cs.mj.nodes.MJVariableNodeFactory;
import org.truffle.cs.mj.nodes.MJWhileLoop;
import org.truffle.cs.mj.parser.Token.Kind;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

public final class RecursiveDescentParser {
    /** Maximum number of global variables per program */
    protected static final int MAX_GLOBALS = 32767;

    /** Maximum number of fields per class */
    protected static final int MAX_FIELDS = 32767;

    /** Maximum number of local variables per method */
    protected static final int MAX_LOCALS = 127;

    /** Last recognized token; */
    protected Token t;

    /** Lookahead token (not recognized).) */
    protected Token la;

    /** Shortcut to kind attribute of lookahead token (la). */
    protected Token.Kind sym;

    /** According scanner */
    public final RecursiveDescendScanner scanner;

    public RecursiveDescentParser(RecursiveDescendScanner scanner) {
        this.scanner = scanner;
        // Avoid crash when 1st symbol has scanner error.
        la = new Token(Token.Kind.none, 1, 1);
        firstExpr = EnumSet.of(ident, number, charConst, minus, lpar, new_);
        firstStat = EnumSet.of(ident, semicolon, lbrace, break_, continue_, if_,
                        print, read, return_, while_);
        firstMethodDecl = EnumSet.of(void_, ident);
    }

    public static enum CompOp {
        eq,
        ne,
        lt,
        le,
        gt,
        ge;

        public static CompOp invert(CompOp op) {
            switch (op) {
                case eq:
                    return ne;
                case ne:
                    return eq;
                case lt:
                    return ge;
                case le:
                    return gt;
                case gt:
                    return le;
                case ge:
                    return lt;
            }
            throw new IllegalArgumentException("Unexpected compare operator");
        }
    }

    private static enum Operands {
        B(1), // byte ( 8 bit signed)
        S(2), // short (16 bit signed)
        W(4); // word (32 bit signed)

        /** Size in bytes (8 bit) */
        public final int size;

        Operands(int size) {
            this.size = size;
        }
    }

    private static final Operands[] B = new Operands[]{Operands.B};
    private static final Operands[] S = new Operands[]{Operands.S};
    private static final Operands[] W = new Operands[]{Operands.W};
    private static final Operands[] BB = new Operands[]{Operands.B,
                    Operands.B};

    public static enum OpCode {
        load(B), //
        load_0, //
        load_1, //
        load_2, //
        load_3, //
        store(B), //
        store_0, //
        store_1, //
        store_2, //
        store_3, //
        getstatic(S), //
        putstatic(S), //
        getfield(S), //
        putfield(S), //
        const_0, //
        const_1, //
        const_2, //
        const_3, //
        const_4, //
        const_5, //
        const_m1, //
        const_(W), //
        add, //
        sub, //
        mul, //
        div, //
        rem, //
        neg, //
        shl, //
        shr, //
        inc(BB), //
        new_(S), //
        newarray(B), //
        aload, //
        astore, //
        baload, //
        bastore, //
        arraylength, //
        pop, //
        dup, //
        dup2, //
        jmp(S), //
        jeq(S), //
        jne(S), //
        jlt(S), //
        jle(S), //
        jgt(S), //
        jge(S), //
        call(S), //
        return_, //
        enter(BB), //
        exit, //
        read, //
        print, //
        bread, //
        bprint, //
        trap(B), //
        nop;

        private final Operands[] ops;

        private OpCode(Operands... operands) {
            this.ops = operands;
        }

        protected Collection<Operands> getOps() {
            return Arrays.asList(ops);
        }

        public int numOps() {
            return ops.length;
        }

        public int getOpsSize() {
            int size = 0;
            for (Operands op : ops) {
                size += op.size;
            }
            return size;
        }

        public int code() {
            return ordinal() + 1;
        }

        public String cleanName() {
            String name = name();
            if (name.endsWith("_")) {
                name = name.substring(0, name.length() - 1);
            }
            return name;
        }

        public static OpCode get(int code) {
            if (code < 1 || code > values().length) {
                return null;
            }
            return values()[code - 1];
        }
    }

    // TODO Exercise 3 - 6: implementation of parser

    /** Sets of starting tokens for some productions. */
    private EnumSet<Token.Kind> firstExpr, firstStat, firstMethodDecl;

    /** Reads ahead one symbol. */
    private void scan() {
        t = la;
        la = scanner.next();
        sym = la.kind;
    }

    /** Verifies symbol and reads ahead. */
    private void check(Token.Kind expected) {
        if (sym == expected) {
            scan();
        } else {
            throw new Error("Token " + expected + " excpeted");
        }
    }

    /**
     * Program = <br>
     * "program" ident <br>
     * { ConstDecl | VarDecl | ClassDecl } <br>
     * "{" { MethodDecl } "}" .
     */
    private void Program() {
        check(program);
        check(ident);
        currentFun = new MJFunction(PUBLIC_NAMESPACE, null, new FrameDescriptor());
        slots.put(currentFun.getName(), new HashMap<>());
        currentFrameDescriptor = currentFun.getFrameDescriptor();

        for (;;) {
            if (sym == final_) {
                ConstDecl();
            } else if (sym == ident) {
                VarDecl();
            } else if (sym == class_) {
                throw new Error("Classes not supported yet!");
// ClassDecl();
            } else {
                break;
            }
        }
        check(lbrace);
        currentFrameDescriptor = null;
        for (;;) {
            if (sym == rbrace || sym == eof) {
                break;
            } else if (firstMethodDecl.contains(sym)) {
                MethodDecl();
            }
        }
        check(rbrace);
    }

    private HashMap<String, Integer> finalVars = new HashMap<>();

    private final int VAR_NOT_INITIALISED = 2136812; // number that show only the
                                                     // creation without initialization

    private final int VAR_INITIALISED = VAR_NOT_INITIALISED + 456; // number of changed values of a
                                                                   // variables (due to code
                                                                   // specification)

    /** ConstDecl = "final" Type ident "=" ( number | charConst ) ";" . */
    private void ConstDecl() {
        check(final_);
        String type = Type();
        String name = Designator();
        if (finalVars.get(name) != null) {
            throw new Error("Constant " + name + " already exist!");
        } else {
            finalVars.put(name, VAR_NOT_INITIALISED);
        }

        check(assign);
        createLocalVarWrite(name, getDefaultTypeInstance(type));
        writeLocalVarWrite(name, Expr());
        check(semicolon);
    }

    private static FrameSlotKind getDefaultTypeInstance(String type) {
        switch (type) {
            case "int":
                return FrameSlotKind.Int;
            case "float":
                return FrameSlotKind.Float;
            case "boolean":
                return FrameSlotKind.Boolean;
            case "char":
                return FrameSlotKind.Byte;
            default:
                throw new Error(type + " this type is not suported yet!");
        }
// switch (type) {
// case "int":
// return MJContstantIntNodeGen.create(0);
// case "float":
// return MJContstantFloatNodeGen.create(0);
// case "boolean":
// return MJContstantBooleanNodeGen.create(false);
// case "char":
// return MJContstantCharNodeGen.create('\0');
// default:
// throw new Error(type + " this type is not suported yet!");
// }
    }

    /** VarDecl = Type ident { "," ident } ";" . */
    private void VarDecl() {
        String typeName = Type();
        String name = Designator();
        createLocalVarWrite(name, getDefaultTypeInstance(typeName));

        while (sym == comma) {
            scan();
            name = Designator();
            createLocalVarWrite(name, getDefaultTypeInstance(typeName));
        }
        check(semicolon);
    }

    /** ClassDecl = "class" ident "{" { VarDecl } "}" . */
    private void ClassDecl() {
        throw new Error("Classes not implemented yet");
// check(class_);
// check(ident);
// check(lbrace);
// while (sym == ident) {
// VarDecl();
// }
// check(rbrace);
    }

    FrameDescriptor currentFrameDescriptor;

    public Map<String, Map<String, FrameSlot>> slots = new HashMap<>();

    private final String PUBLIC_NAMESPACE = "SUPER_UNREAL_PUBLICA_NAMESPACE";
    private HashSet<String> publicVars = new HashSet<>();

    public void createLocalVarWrite(String name, FrameSlotKind type) {
        if (currentFun.getName() == PUBLIC_NAMESPACE) {
            publicVars.add(name);
        }

        FrameSlot frameSlot = slots.get(currentFun.getName()).get(name);

        if (frameSlot == null) {
            frameSlot = currentFrameDescriptor.addFrameSlot(name);
            slots.get(currentFun.getName()).put(name, frameSlot);

        } else {
            throw new Error("Local variable " + name + " is already initialized!");
        }
        currentFrameDescriptor.setFrameSlotKind(frameSlot, type);

// return MJVariableNodeFactory.MJWriteLocalVariableNodeGen.create(value, frameSlot);
    }

    public MJStatementNode writeLocalVarWrite(String name, MJExpresionNode value) {

        FrameSlot frameSlot = slots.get(currentFun.getName()).get(name);

        if (frameSlot == null) {
            if (publicVars.contains(name)) {
                frameSlot = slots.get(PUBLIC_NAMESPACE).get(name);
            } else {
                throw new Error("Variable " + name + " is not initialised!");
            }

        } else if (finalVars.containsKey(name)) {
            if (finalVars.get(name) == VAR_INITIALISED) {
                throw new Error("Variable " + name + " is a final and can not be changed!");
            } else {
                finalVars.put(name, VAR_INITIALISED);
            }
        }
        return MJVariableNodeFactory.MJWriteLocalVariableNodeGen.create(value, frameSlot);
    }

    public MJReadLocalVariableNode readLocalVarRead(String name) {
        FrameSlot frameSlot = slots.get(currentFun.getName()).get(name);
        if (frameSlot == null) {
            if (publicVars.contains(name)) {
                frameSlot = slots.get(PUBLIC_NAMESPACE).get(name);
            } else {
                throw new Error("No variable " + name + "found in this scope!4");
            }
        }
        return MJVariableNodeFactory.MJReadLocalVariableNodeGen.create(frameSlot);
    }

    public List<MJFunction> functions = new ArrayList<>();

    public HashMap<MJFunction, CallTarget> callAble = new HashMap<>();

    public MJFunction getFunction(String Name) {
        for (MJFunction f : functions) {
            if (f.getName().equals(Name))
                return f;
        }
        return null;
    }

    public MJFunction getMain() {
        for (MJFunction f : functions) {
            if (f.getName().equals("main"))
                return f;
        }
        return null;
    }

    private ArrayList<String> parameterNames;

    MJFunction currentFun = null;

    /**
     * MethodDecl = <br>
     * ( Type | "void" ) ident "(" [ FormPars ] ")" <br>
     * ( ";" | { VarDecl } Block ) .
     */
    private MJFunction MethodDecl() {
        currentFrameDescriptor = new FrameDescriptor();
        if (sym == ident) {
            Type();
        } else if (sym == void_) {
            scan();
        } else {
            throw new Error("Method declaration");
        }
        check(ident);
        String name = t.str;

        currentFun = new MJFunction(name, null, currentFrameDescriptor);
        functions.add(currentFun);
        slots.put(currentFun.getName(), new HashMap<>());

        check(lpar);
        if (sym == ident) {
            parameterNames = FormPars();
        }
        check(rpar);
        while (sym == ident) {
            VarDecl();
        }

        MJStatementNode block = Block();

        getFunction(name).setBody(block);
        return currentFun;
    }

    /** FormPars = Type ident { "," Type ident } . */
    private ArrayList<String> FormPars() {
        ArrayList<String> parNames = new ArrayList<>();
        Type();
        check(ident);
        parNames.add(t.str);
        while (sym == comma) {
            scan();
            Type();
            check(ident);
            parNames.add(t.str);
        }
        return parNames;
    }

    /** Type = ident . */
    private String Type() {
        String type = la.str;
        check(ident);
        if (sym == lbrack) {
            throw new Error("Arrays are ignored for now");
// scan();
// check(rbrack);
        }
        return type;
    }

    /** Block = "{" { Statement } "}" . */
    private MJStatementNode Block() {
        check(lbrace);
        List<MJStatementNode> statements = Statements();
        MJBlock currentBlockOfStatements = new MJBlock(statements.toArray(new MJStatementNode[statements.size()]));
        check(rbrace);
        return currentBlockOfStatements;
    }

    private List<MJStatementNode> Statements() {
        List<MJStatementNode> stats = new ArrayList<>();
        for (;;) {
            if (firstStat.contains(sym)) {
                stats.add(Statement());
            } else {
                break;
            }
        }
        return stats;
    }

    /**
     * Statement = <br>
     * Designator ( Assignop Expr | ActPars | "++" | "--" ) ";" <br>
     * | "if" "(" Condition ")" Statement [ "else" Statement ] <br>
     * | "while" "(" Condition ")" Statement <br>
     * | "break" ";" <br>
     * | "return" [ Expr ] ";" <br>
     * | "read" "(" Designator ")" ";" <br>
     * | "print" "(" Expr [ comma number ] ")" ";" <br>
     * | Block <br>
     * | ";" .
     */
    private MJStatementNode Statement() {
        MJStatementNode currentParsStatement = null;
        MJExpresionNode expr;
        switch (sym) {
            // ----- assignment, method call, in- or decrement
            // ----- Designator ( Assignop Expr | ActPars | "++" | "--" ) ";"
            case ident:
                String des = Designator();
                expr = null;
                switch (sym) {
                    case assign:
                        Assignop();
                        currentParsStatement = writeLocalVarWrite(des, Expr());
                        break;
                    case plusas:
                        Assignop();
                        expr = readLocalVarRead(des);
                        currentParsStatement = writeLocalVarWrite(des, MJBinaryNodeFactory.AddNodeGen.create(expr, Expr()));
                        break;
                    case minusas:
                        Assignop();
                        expr = readLocalVarRead(des);
                        currentParsStatement = writeLocalVarWrite(des, MJBinaryNodeFactory.SubNodeGen.create(expr, Expr()));
                        break;
                    case timesas:
                        Assignop();
                        expr = readLocalVarRead(des);
                        currentParsStatement = writeLocalVarWrite(des, MJBinaryNodeFactory.MulNodeGen.create(expr, Expr()));
                        break;
                    case slashas:
                        Assignop();
                        expr = readLocalVarRead(des);
                        currentParsStatement = writeLocalVarWrite(des, MJBinaryNodeFactory.DivNodeGen.create(expr, Expr()));
                        break;
                    case remas:
                        Assignop();
                        expr = readLocalVarRead(des);
                        currentParsStatement = writeLocalVarWrite(des, MJBinaryNodeFactory.ModNodeGen.create(expr, Expr()));
                        break;
                    case lpar:
                        List<MJExpresionNode> parameters = ActPars();
                        MJFunction callee = getFunction(des);
                        CallTarget callTarget = callAble.get(callee);
                        if (callTarget == null) {
                            callTarget = Truffle.getRuntime().createCallTarget(callee);
                            callAble.put(callee, callTarget);
                        }
                        MJExpresionNode invoke = new MJInvokeNode(callTarget,
                                        parameters.toArray(new MJExpresionNode[parameters.size()]));
                        currentParsStatement = new MJStatementExpresion(invoke);
                        break;
                    case pplus:
                        scan();
                        if (!slots.containsKey(des)) {
                            throw new Error("Variable " + des + " does not exist");
                        }
                        expr = readLocalVarRead(des);
                        currentParsStatement = writeLocalVarWrite(des, MJBinaryNodeFactory.AddNodeGen.create(expr, MJContstantIntNodeGen.create(1)));
                        break;
                    case mminus:
                        scan();
                        if (!slots.containsKey(des)) {
                            throw new Error("Variable " + des + " does not exist");
                        }
                        expr = readLocalVarRead(des);
                        currentParsStatement = writeLocalVarWrite(des, MJBinaryNodeFactory.SubNodeGen.create(expr, MJContstantIntNodeGen.create(1)));
                        break;
                    default:
                        throw new Error("Designator Follow");
                }
                check(semicolon);
                break;
            // ----- "if" "(" Condition ")" Statement [ "else" Statement ]
            case if_:
                scan();
                check(lpar);
                MJExpresionNode condition = Condition();
                check(rpar);
                MJStatementNode trueBranch = Statement();
                MJStatementNode falseBranch = null;
                if (sym == else_) {
                    scan();
                    falseBranch = Statement();
                }
                currentParsStatement = new MJIfNode(condition, trueBranch, falseBranch);
                break;
            // ----- "while" "(" Condition ")" Statement
            case while_:
                scan();
                check(lpar);
                condition = Condition();
                check(rpar);
                MJStatementNode loopBody = Statement();
                currentParsStatement = new MJWhileLoop(condition, loopBody);
                break;
            // ----- "break" ";"
            case break_:
                scan();
                currentParsStatement = new MJBreakNode();
                check(semicolon);
                break;

            // ----- "break" ";"
            case continue_:
                scan();
                currentParsStatement = new MJContinueNode();
                check(semicolon);
                break;
            // ----- "return" [ Expr ] ";"
            case return_:
                scan();
                MJExpresionNode retValue = null;
                if (sym != semicolon) {
                    retValue = Expr(); // debug
                }
                currentParsStatement = new MJReturnNode(retValue);
                check(semicolon);
                break;
            // ----- "read" "(" Designator ")" ";"
            case read:
                scan();
                check(lpar);
                Designator();
                check(rpar);
                check(semicolon);
                break;
            // ----- "print" "(" Expr [ comma number ] ")" ";"
            case print:
                scan();
                check(lpar);
                expr = Expr();

                if (sym == comma) {
                    scan();
                    check(number);
                }
                check(rpar);
                check(semicolon);

                currentParsStatement = MJPrintNodeGen.create(expr);
                break;
            case lbrace:
                currentParsStatement = Block();
                break;
            case semicolon:
                scan();
                break;
            default:
                throw new Error("Invalid start...");
        }
        return currentParsStatement;
    }

    /** ActPars = "(" [ Expr { "," Expr } ] ")" . */
    private List<MJExpresionNode> ActPars() {
        ArrayList<MJExpresionNode> exprs = new ArrayList<>();
        check(lpar);
        if (firstExpr.contains(sym)) {
            for (;;) {
                exprs.add(Expr());
                if (sym == comma) {
                    scan();
                } else {
                    break;
                }
            }
        }
        check(rpar);
        return exprs;
    }

    /** Condition = CondTerm { "||" CondTerm } . */
    private MJExpresionNode Condition() {
        MJExpresionNode a = CondTerm();
        while (sym == or) {
            scan();
            a = MJBinaryNodeFactory.BooleanOrNodeGen.create(a, CondTerm());
        }
        return a;
    }

    /** CondTerm = CondFact { "&&" CondFact } . */
    private MJExpresionNode CondTerm() {
        MJExpresionNode a = CondFact();
        while (sym == and) {
            scan();
            a = MJBinaryNodeFactory.BooleanAndNodeGen.create(a, CondFact());
        }
        return a;
    }

    /** CondFact = Expr Relop Expr . */
    private MJExpresionNode CondFact() {
        MJExpresionNode a = Expr();
        if (a instanceof MJContstantBooleanNodeGen) {
            return a;
        } /// ..................................................................

        CompOp comp = Relop();
        switch (comp) {
            case ne:
                a = MJBinaryNodeFactory.NotEqualNodeGen.create(a, Expr());
                break;
            case lt:
                a = MJBinaryNodeFactory.LessThanNodeGen.create(a, Expr());
                break;
            case le:
                a = MJBinaryNodeFactory.LessEqualNodeGen.create(a, Expr());
                break;
            case eq:
                a = MJBinaryNodeFactory.EqualNodeGen.create(a, Expr());
                break;
            case ge:
                a = MJBinaryNodeFactory.GreaterEqualNodeGen.create(a, Expr());
                break;
            case gt:
                a = MJBinaryNodeFactory.GreaterThanNodeGen.create(a, Expr());
                break;
            default:
                break;
        }
        return a;
    }

    /** Expr = [ "-" ] Term { Addop Term } . */
    private MJExpresionNode Expr() {
        MJExpresionNode expr = null;

        if (sym == minus) {
            scan();
            expr = MJUnaryNodeFactory.NegNodeGen.create(Term());
        } else if (sym == Kind.true_) {
            scan();
            return MJContstantBooleanNodeGen.create(true);
        } else if (sym == Kind.false_) {
            scan();
            return MJContstantBooleanNodeGen.create(false);
        } else {
            expr = Term();
        }

        while (sym == plus || sym == minus) {
            if (sym == plus) {
                Addop();
                expr = MJBinaryNodeFactory.AddNodeGen.create(expr, Term());
            } else if (sym == minus) {
                Addop();
                expr = MJBinaryNodeFactory.SubNodeGen.create(expr, Term());
            }
        }
        return expr;
    }

    /** Term = Factor { Mulop Factor } . */
    private MJExpresionNode Term() {
        MJExpresionNode expr = Factor();

        while (sym == times || sym == slash || sym == rem) {
// Mulop();
            switch (sym) {
                case times:
                    scan();
                    expr = MJBinaryNodeFactory.MulNodeGen.create(expr, Factor());
                    break;
                case slash:
                    scan();
                    expr = MJBinaryNodeFactory.DivNodeGen.create(expr, Factor());
                    break;
                case rem:
                    scan();
                    expr = MJBinaryNodeFactory.ModNodeGen.create(expr, Factor());
                    break;
                default:
                    break;
            }
        }
        return expr;
    }

    /**
     * Factor = <br>
     * Designator [ ActPars ] <br>
     * | number <br>
     * | charConst <br>
     * | "new" ident [ "[" Expr "]" ] <br>
     * | "(" Expr ")" .
     */
    private MJExpresionNode Factor() {
        MJExpresionNode expr = null;
        switch (sym) {
            case abs:
                scan();
                check(lpar);
                expr = MJAbsNodeGen.create(Expr());
                check(rpar);
                break;
            case ident:
                String varname = Designator();
                if (sym == lpar) {
                    List<MJExpresionNode> parameters = ActPars();
                    MJFunction callee = getFunction(varname);
                    CallTarget callTarget = callAble.get(callee);
                    if (callTarget == null) {
                        callTarget = Truffle.getRuntime().createCallTarget(callee);
                        callAble.put(callee, callTarget);
                    }
                    expr = new MJInvokeNode(callTarget,
                                    parameters.toArray(new MJExpresionNode[parameters.size()]));
                } else {
                    int index = parameterNames != null ? parameterNames.indexOf(varname) : -1;
                    if (index >= 0) {
                        expr = new MJReadParameterNode(index);
                    } else {
                        expr = readLocalVarRead(varname);
                    }

                }
                break;
            case number:
                scan();
                int val = t.val;
                if (la.kind == period) {
                    scan();
                    check(number);
                    float newVal = val + t.val / (float) Math.pow(10, t.str.length());
                    expr = MJContstantFloatNodeGen.create(newVal);
                } else {
                    expr = MJContstantIntNodeGen.create(val);
                }
                break;
            case charConst:
                scan();
                char ch = (char) t.val;

                expr = MJContstantCharNodeGen.create(ch);
                break;
            case new_:
// scan();
// check(ident);
// if (sym == lbrack) {
// scan();
// Expr();
// check(rbrack);
// }
                throw new Error("New not supported yet");
// break;
            case lpar:
                scan();
                Expr();
                check(rpar);
                break;
            default:
                throw new Error("Invalid fact");
        }
        return expr;
    }

    /** Designator = ident { "." ident | "[" Expr "]" } . */
    private String Designator() {
        check(ident);
        while (sym == period || sym == lbrack) {
            if (sym == period) {
                scan();
                check(ident);
                throw new Error("Fields ignored for now");
            } else {
                scan();
                Expr();
                check(rbrack);
                throw new Error("Arrays ignored for now...");
            }
        }
        return t.str;
    }

    /** Assignop = "=" | "+=" | "-=" | "*=" | "/=" | "%=" . */
    private OpCode Assignop() {
        OpCode op = OpCode.store;
        switch (sym) {
            case assign:
                op = OpCode.store;
                scan();
                break;
            case plusas:
                op = OpCode.add;
                scan();
                break;
            case minusas:
                op = OpCode.sub;
                scan();
                break;
            case timesas:
                op = OpCode.mul;
                scan();
                break;
            case slashas:
                op = OpCode.div;
                scan();
                break;
            case remas:
                op = OpCode.rem;
                scan();
                break;
            default:
                throw new Error("invalid assign operation");
        }
        return op;
    }

    /** Relop = "==" | "!=" | ">" | ">=" | "<" | "<=" . */
    private CompOp Relop() {
        CompOp op = CompOp.eq;
        switch (sym) {
            case eql:
                op = CompOp.eq;
                scan();
                break;
            case neq:
                op = CompOp.ne;
                scan();
                break;
            case lss:
                op = CompOp.lt;
                scan();
                break;
            case leq:
                op = CompOp.le;
                scan();
                break;
            case gtr:
                op = CompOp.gt;
                scan();
                break;
            case geq:
                op = CompOp.ge;
                scan();
                break;
            default:
                throw new Error("invalid rel operation " + sym);
        }
        return op;
    }

    /** Addop = "+" | "-" . */
    private OpCode Addop() {
        OpCode op = OpCode.add;
        switch (sym) {
            case plus:
                op = OpCode.add;
                scan();
                break;
            case minus:
                op = OpCode.sub;
                scan();
                break;
            default:
                throw new Error("invalid add operation");
        }
        return op;
    }

    /** Mulop = "*" | "/" | "%" . */
    private OpCode Mulop() {
        OpCode op = OpCode.mul;
        switch (sym) {
            case times:
                op = OpCode.mul;
                scan();
                break;
            case slash:
                op = OpCode.div;
                scan();
                break;
            case rem:
                op = OpCode.rem;
                scan();
                break;
            default:
                throw new Error("invalid mul operation");
        }
        return op;
    }

    public void parse() {
        scan(); // scan first symbol
        Program(); // start analysis

        check(eof);
    }
}