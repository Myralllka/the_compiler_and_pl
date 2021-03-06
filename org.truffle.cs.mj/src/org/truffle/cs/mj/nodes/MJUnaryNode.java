package org.truffle.cs.mj.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild(value = "x", type = MJExpresionNode.class)
public abstract class MJUnaryNode extends MJExpresionNode {

    public static abstract class NotNode extends MJUnaryNode {
        @Specialization
        public boolean not(boolean x) {
            return !x;
        }
    }

    public static abstract class NegNode extends MJUnaryNode {
        @Specialization
        public int neg(int x) {
            return -x;
        }

        @Specialization
        public float neg(float x) {
            return -x;
        }
    }
}
