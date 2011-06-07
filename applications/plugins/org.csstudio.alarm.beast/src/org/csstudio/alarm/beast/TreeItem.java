/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** Base for alarm tree items: Tree hierarchy, ID, name, path.
 *
 *  Client and server code need additional but different data in the alarm tree:
 *  Display info like guidance, or alarm state determination details.
 *
 *  When client and server code uses tree items derived from this base
 *  tree item, hierarchy related methods like <code>getParent()</code>,
 *  <code>getChild()</code> usually require a cast to the actual derived data type.
 *
 *  One could use a generic <code>TreeItem&lt;T&gt;</code> to add a member variable
 *  <code>T info</code> to the tree item to remain type-safe without casts.
 *  That "info" would then be specific to client or server code.
 *  But the data member needs access to the tree item that owns it to
 *  get the path etc., adding another reference back to the tree item.
 *
 *  A middle way seems to be that derived classes 'X' have a <code>getParentX()</code>
 *  method that performs the cast in one place.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TreeItem
{
    /** RDB ID
     *  @see #setID(int)
     */
    private int id;

    /** Visible name of the item */
    final private String name;

    /** Parent node */
    private TreeItem parent;

    /** Sub-tree elements of this item */
    final private List<TreeItem> children = new ArrayList<TreeItem>();

    // According to JProfiler, equals()/getPathName()/hashCode()
    // are called A LOT whenever the JFace tree view is updated or
    // whenever an alarm item is added to/removed from various lists,
    // so they have to be fast.

    /** Full path name of this item.
     *  Like parent and hash_code it's final so that it can be computed once,
     *  because it is used very often.
     */
    final private String path_name;

    /** Hash code of this item */
    final private int hash_code;

    /** Initialize
     *  @param parent Parent item or <code>null</code> for root
     *  @param name Name of this item
     *  @param id RDB ID. May initially be -1 if set later
     *  @see #setID(int)
     */
    public TreeItem(final TreeItem parent, final String name, final int id)
    {
        this.parent = parent;
        this.name = name;
        this.id = id;
        if (parent == null)
            path_name = AlarmTreePath.makePath(null, name);
        else
        {
            path_name = AlarmTreePath.makePath(parent.getPathName(), name);
            parent.addChild(this);
        }
        hash_code = getPathName().hashCode();
    }

    /** @return Parent item. <code>null</code> for root */
    final synchronized public TreeItem getParent()
    {
        return parent;
    }

    /** @return Alarm tree root element */
    final public TreeItem getRoot()
    {
        TreeItem root = this;
        TreeItem p = getParent();
        while (p != null)
        {
            root = p;
            p = p.getParent();
        }
        return root;
    }

    /** @return RDB ID */
    final public synchronized int getID()
    {
        return id;
    }

    /** Set the RDB ID
     *  The ID cannot be changed as such, but tree items that start out in
     *  memory may have a tree ID of -1 until their full detail is read
     *  from the RDB
     *  @param id
     *  @throws IllegalStateException if ID was already set
     */
    final public synchronized void setID(final int id)
    {
        if (this.id >= 0)
            throw new IllegalStateException("ID already set for " + toString());
        this.id = id;
    }

    /** @return Name */
    final public String getName()
    {
        return name;
    }

    /** @return Full path name to this item, including the item name itself */
    final public String getPathName()
    {
        return path_name;
    }

    /** @return Number of child nodes */
    final public synchronized int getChildCount()
    {
        return children.size();
    }

    /** @param index 0 ... <code>getChildCount()-1</code>
     *  @return Child item
     *  @throws IndexOutOfBoundsException if the index is out of range
     *          (<tt>index &lt; 0 || index &gt;= getChildCount()</tt>)
     */
    final public synchronized TreeItem getChild(final int index)
    {
        return children.get(index);
    }

    /** Locate child element by name.
     *  @param child_name Name of child to locate.
     *  @return Child with given name or <code>null</code> if not found.
     */
    final public synchronized TreeItem getChild(final String name)
    {
        for (TreeItem child : children)
            if (child.getName().equals(name))
                return child;
        return null;
    }

    /** Add child item.
     *  'private' because child items add themself in constructor.
     *  @param child New child item
     */
    final synchronized private void addChild(final TreeItem child)
    {
        children.add(child);
    }

    /** Detach item from parent: Remove from parent's list of children
     *  @return <code>true</code> if anything was done,
     *          <code>false</code> if there was nothing to do
     *  @throws Error if parent didn't know about this child item
     */
    final public boolean detachFromParent()
    {
        final TreeItem p;
        synchronized (this)
        {
            if (parent == null)
                return false;
            p = parent;
            parent = null;
        }
        p.removeChild(this);
        return true;
    }

    /** Remove child
     *  @param child
     *  @throws Error if child not known
     */
    private synchronized void removeChild(final TreeItem child)
    {
        if (! children.remove(child))
            throw new Error("Corrupted tree item: " + toString());
    }

    /** Dump this item and sub-items.
     *  To be called by outside code.
     *  Derived classes should override <code>dump_item()</code>
     *  @param out PrintStream
     */
    final synchronized public void dump(final PrintStream out)
    {
        dump(out, "");
    }

    /** Dump this item and sub-items.
     *  Derived classes can override to show more detail.
     *  @param out PrintStream
     *  @param level Indentation level
     */
    final private void dump(final PrintStream out, final String indent)
    {
        dump_item(out, indent);
        final String next_indent = indent + "    ";
        for (TreeItem child : children)
            child.dump(out, next_indent);
    }

    /** Dump information for this item.
     *  Derived classes can override to show more detail.
     *  @param out PrintStream
     *  @param level Indentation level
     */
    protected void dump_item(final PrintStream out, final String indent)
    {
        out.println(indent + "* " + toString());
    }

    /** Perform hierarchy consistency check
     *  @throws Exception on error
     */
    final synchronized public void check() throws Exception
    {
        // All subtree entries must point back to this as their parent
        for (TreeItem child : children)
        {
            if (child.getParent() != this)
                throw new Exception("Hierarchy error from " + child + " to " + this);
            child.check();
        }
    }

    /** Compare by path name
     *  @param obj Other object
     *  @return <code>true</code> if path names match
     */
    @Override
    final public boolean equals(final Object obj)
    {
        if (obj == this)
            return true;
        if (! (obj instanceof TreeItem))
            return false;
        final TreeItem other = (TreeItem) obj;
        return getPathName().equals(other.getPathName());
    }

    /** @return Hash code of path name */
    @Override
    final public int hashCode()
    {
        return hash_code;
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return path_name + " (ID " + id + ")";
    }
}