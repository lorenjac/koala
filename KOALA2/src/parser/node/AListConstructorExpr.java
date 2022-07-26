/* This file was generated by SableCC (http://www.sablecc.org/). */

package parser.node;

import java.util.*;
import parser.analysis.*;

@SuppressWarnings("nls")
public final class AListConstructorExpr extends PExpr
{
    private final LinkedList<PExpr> _head_ = new LinkedList<PExpr>();
    private PExpr _tail_;

    public AListConstructorExpr()
    {
        // Constructor
    }

    public AListConstructorExpr(
        @SuppressWarnings("hiding") List<?> _head_,
        @SuppressWarnings("hiding") PExpr _tail_)
    {
        // Constructor
        setHead(_head_);

        setTail(_tail_);

    }

    @Override
    public Object clone()
    {
        return new AListConstructorExpr(
            cloneList(this._head_),
            cloneNode(this._tail_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAListConstructorExpr(this);
    }

    public LinkedList<PExpr> getHead()
    {
        return this._head_;
    }

    public void setHead(List<?> list)
    {
        for(PExpr e : this._head_)
        {
            e.parent(null);
        }
        this._head_.clear();

        for(Object obj_e : list)
        {
            PExpr e = (PExpr) obj_e;
            if(e.parent() != null)
            {
                e.parent().removeChild(e);
            }

            e.parent(this);
            this._head_.add(e);
        }
    }

    public PExpr getTail()
    {
        return this._tail_;
    }

    public void setTail(PExpr node)
    {
        if(this._tail_ != null)
        {
            this._tail_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._tail_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._head_)
            + toString(this._tail_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._head_.remove(child))
        {
            return;
        }

        if(this._tail_ == child)
        {
            this._tail_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        for(ListIterator<PExpr> i = this._head_.listIterator(); i.hasNext();)
        {
            if(i.next() == oldChild)
            {
                if(newChild != null)
                {
                    i.set((PExpr) newChild);
                    newChild.parent(this);
                    oldChild.parent(null);
                    return;
                }

                i.remove();
                oldChild.parent(null);
                return;
            }
        }

        if(this._tail_ == oldChild)
        {
            setTail((PExpr) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}
