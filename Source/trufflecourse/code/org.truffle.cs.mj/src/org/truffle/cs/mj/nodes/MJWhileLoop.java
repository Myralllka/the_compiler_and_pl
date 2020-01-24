package org.truffle.cs.mj.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public class MJWhileLoop extends MJStatementNode {

    @Child MJExpresionNode condition;
    @Child MJStatementNode loopBody;

    public MJWhileLoop(MJExpresionNode condition, MJStatementNode loopBody) {
        this.condition = condition;
        this.loopBody = loopBody;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        while (condition.executeBool(frame)) {
            loopBody.execute(frame);
        }
        return null;
    }
}
