/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.ast.internal;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.NodeStream.DescendantNodeStream;

/**
 * Object performing tree traversals. Configuration options can be
 * extended later on.
 *
 * @see DescendantNodeStream
 */
final class TreeWalker {

    private static final TreeWalker CROSS = new TreeWalker(true);
    private static final TreeWalker DONT_CROSS = new TreeWalker(false);

    /**
     * Default traversal config used by methods like {@link Node#descendants()}
     */
    static final TreeWalker DEFAULT = DONT_CROSS;

    private final boolean crossFindBoundaries;

    private TreeWalker(boolean crossFindBoundaries) {
        this.crossFindBoundaries = crossFindBoundaries;
    }

    public boolean isCrossFindBoundaries() {
        return crossFindBoundaries;
    }

    /**
     * Returns a new walker with the given behaviour for find boundaries.
     */
    TreeWalker crossFindBoundaries(boolean cross) {
        return cross ? CROSS : DONT_CROSS;
    }

    /**
     * Configure the given node stream to use this walker.
     */
    <T extends Node> DescendantNodeStream<T> apply(DescendantNodeStream<T> stream) {
        return stream.crossFindBoundaries(crossFindBoundaries);
    }

    <T> void findDescendantsMatching(final Node node,
                                     final Filtermap<? super Node, ? extends T> filtermap,
                                     final List<T> results) {

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            final Node child = node.jjtGetChild(i);
            final T mapped = filtermap.apply(child);
            if (mapped != null) {
                results.add(mapped);
            }

            if (isCrossFindBoundaries() || !child.isFindBoundary()) {
                this.findDescendantsMatching(child, filtermap, results);
            }
        }
    }

    <T extends Node> T getFirstDescendantOfType(final Node node, final Filtermap<? super Node, ? extends T> filtermap) {
        final int n = node.jjtGetNumChildren();
        for (int i = 0; i < n; i++) {
            Node child = node.jjtGetChild(i);
            final T t = filtermap.apply(child);
            if (t != null) {
                return t;
            } else if (isCrossFindBoundaries() || !child.isFindBoundary()) {
                final T n2 = this.getFirstDescendantOfType(child, filtermap);
                if (n2 != null) {
                    return n2;
                }
            }
        }
        return null;
    }

    <T> List<T> findDescendantsMatching(final Node node, final Filtermap<? super Node, ? extends T> filtermap) {
        List<T> results = new ArrayList<>();
        findDescendantsMatching(node, filtermap, results);
        return results;
    }


    Iterator<Node> descendantOrSelfIterator(Node top) {
        return new DescendantOrSelfIterator(top, this);
    }

    Iterator<Node> descendantIterator(Node top) {
        DescendantOrSelfIterator iter = new DescendantOrSelfIterator(top, this);
        iter.next(); // skip self
        return iter;
    }

    /** Iterates over a node and its descendants. */
    private static class DescendantOrSelfIterator implements Iterator<@NonNull Node> {

        private final Deque<Node> queue = new ArrayDeque<>();
        private final TreeWalker config;

        /** Always {@link #hasNext()} after exiting the constructor. */
        DescendantOrSelfIterator(Node top, TreeWalker walker) {
            this.config = walker;
            queue.addFirst(top);
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }


        @Override
        public @NonNull Node next() {
            Node node = queue.removeFirst();
            enqueueChildren(node);
            return node;
        }


        private void enqueueChildren(Node n) {
            if (config.isCrossFindBoundaries() || !n.isFindBoundary()) {
                for (int i = n.jjtGetNumChildren() - 1; i >= 0; i--) {
                    queue.addFirst(n.jjtGetChild(i));
                }
            }
        }
    }

}
