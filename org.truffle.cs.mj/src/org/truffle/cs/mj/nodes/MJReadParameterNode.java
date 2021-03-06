package org.truffle.cs.mj.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public class MJReadParameterNode extends MJExpresionNode {
    final int index;

    public MJReadParameterNode(int index) {
        this.index = index;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return frame.getArguments()[index];
    }

}
