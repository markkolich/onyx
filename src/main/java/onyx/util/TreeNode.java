/*
 * Copyright (c) 2024 Mark S. Kolich
 * https://mark.koli.ch
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package onyx.util;

import static com.google.common.base.Preconditions.checkNotNull;

public final class TreeNode {

    private long resources_;
    private long size_;

    private TreeNode(
            final long resources,
            final long size) {
        resources_ = resources;
        size_ = size;
    }

    public long getResources() {
        return resources_;
    }

    public long getSize() {
        return size_;
    }

    public TreeNode plus(
            final TreeNode treeNode) {
        checkNotNull(treeNode, "Tree node cannot be null.");

        resources_ += treeNode.getResources();
        size_ += treeNode.getSize();

        return this;
    }

    public static TreeNode of() {
        return of(0L, 0L);
    }

    public static TreeNode of(
            final long resources,
            final long size) {
        return new TreeNode(resources, size);
    }

}
