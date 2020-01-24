package org.truffle.cs.mj.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;
//import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

@NodeInfo
public class MJFunction extends RootNode {

    final String name;
    @Child MJStatementNode body;

    public MJFunction(String name, MJStatementNode body, FrameDescriptor frameDescriptor) {
        super(null, frameDescriptor);
        this.name = name;
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            body.execute(frame);
        } catch (MJReturnNode.MJReturnException e) {
            return e.value;
        }
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
}
