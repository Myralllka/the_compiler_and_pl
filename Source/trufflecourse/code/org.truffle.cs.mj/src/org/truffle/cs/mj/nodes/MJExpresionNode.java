package org.truffle.cs.mj.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo
public abstract class MJExpresionNode extends Node {

    public abstract Object executeGeneric(VirtualFrame frame);

    public int executeI32(VirtualFrame frame) {
        Object result = executeGeneric(frame);
        if (result instanceof Integer) {
            return (int) result;
        }
        CompilerDirectives.transferToInterpreter();
        throw new Error("type mismatch");
    }

    public boolean executeBool(VirtualFrame frame) {
        Object result = executeGeneric(frame);
        if (result instanceof Integer) {
            return (boolean) result;
        }
        CompilerDirectives.transferToInterpreter();
        throw new Error("type mismatch");
    }
// public float executeF32(VirtualFrame frame) {
// Object result = executeGeneric(frame);
// if (result instanceof Integer) {
// return (float) result;
// }
// CompilerDirectives.transferToInterpreter();
// throw new Error("type mismatch");
// }

}
