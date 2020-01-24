/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.api.library.test.examples;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.test.examples.ArrayStrategy1Factory.ArrayReadNodeGen;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

/**
 * Runnable source code for array strategy1 from the
 * <a href="https://github.com/oracle/graal/blob/master/truffle/docs/TruffleLibaries.md">motivation
 * article</a>.
 */
public class ArrayStrategy1 {

    final class BufferArray {
        int length;
        int[] buffer;

        BufferArray(int length) {
            this.length = length;
            this.buffer = new int[length];
        }
    }

    final class SequenceArray {
        final int start;
        final int stride;
        final int length;

        SequenceArray(int start, int stride, int length) {
            this.start = start;
            this.stride = stride;
            this.length = length;
        }
    }

    abstract static class ExpressionNode extends Node {
        abstract Object execute(VirtualFrame frame);
    }

    @NodeChild
    @NodeChild
    abstract static class ArrayReadNode extends ExpressionNode {

        @Specialization
        int doBuffer(BufferArray array, int index) {
            return array.buffer[index];
        }

        @Specialization
        int doSequence(SequenceArray seq, int index) {
            return seq.start + seq.stride * index;
        }
    }

    static final class ArgumentNode extends ExpressionNode {
        private final int index;

        ArgumentNode(int index) {
            this.index = index;
        }

        @Override
        Object execute(VirtualFrame frame) {
            return frame.getArguments()[index];
        }
    }

    static final class ExampleRootNode extends RootNode {

        @Child private ExpressionNode node;

        ExampleRootNode(ExpressionNode node) {
            super(null);
            this.node = node;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return node.execute(frame);
        }

    }

    @Test
    public void runExample() {
        ArrayReadNode read = ArrayReadNodeGen.create(new ArgumentNode(0), new ArgumentNode(1));
        CallTarget target = Truffle.getRuntime().createCallTarget(new ExampleRootNode(read));

        assertEquals(3, target.call(new SequenceArray(1, 2, 3), 1));
        assertEquals(0, target.call(new BufferArray(2), 1));
    }

}
