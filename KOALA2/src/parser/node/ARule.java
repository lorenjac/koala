/* This file was generated by SableCC (http://www.sablecc.org/). */

package parser.node;

import java.util.*;
import parser.analysis.*;

@SuppressWarnings("nls")
public final class ARule extends PRule
{
    private PHead _head_;
    private final LinkedList<PExpr> _ask_ = new LinkedList<PExpr>();
    private final LinkedList<PExpr> _tell_ = new LinkedList<PExpr>();
    private final LinkedList<PExpr> _body_ = new LinkedList<PExpr>();

    public ARule()
    {
        // Constructor
    }

    public ARule(
        @SuppressWarnings("hiding") PHead _head_,
        @SuppressWarnings("hiding") List<?> _ask_,
        @SuppressWarnings("hiding") List<?> _tell_,
        @SuppressWarnings("hiding") List<?> _body_)
    {
        // Constructor
        setHead(_head_);

        setAsk(_ask_);

        setTell(_tell_);

        setBody(_body_);

    }

    @Override
    public Object clone()
    {
        return new ARule(
            cloneNode(this._head_),
            cloneList(this._ask_),
            cloneList(this._tell_),
            cloneList(this._body_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseARule(this);
    }

    public PHead getHead()
    {
        return this._head_;
    }

    public void setHead(PHead node)
    {
        if(this._head_ != null)
        {
            this._head_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._head_ = node;
    }

    public LinkedList<PExpr> getAsk()
    {
        return this._ask_;
    }

    public void setAsk(List<?> list)
    {
        for(PExpr e : this._ask_)
        {
            e.parent(null);
        }
        this._ask_.clear();

        for(Object obj_e : list)
        {
            PExpr e = (PExpr) obj_e;
            if(e.parent() != null)
            {
                e.parent().removeChild(e);
            }

            e.parent(this);
            this._ask_.add(e);
        }
    }

    public LinkedList<PExpr> getTell()
    {
        return this._tell_;
    }

    public void setTell(List<?> list)
    {
        for(PExpr e : this._tell_)
        {
            e.parent(null);
        }
        this._tell_.clear();

        for(Object obj_e : list)
        {
            PExpr e = (PExpr) obj_e;
            if(e.parent() != null)
            {
                e.parent().removeChild(e);
            }

            e.parent(this);
            this._tell_.add(e);
        }
    }

    public LinkedList<PExpr> getBody()
    {
        return this._body_;
    }

    public void setBody(List<?> list)
    {
        for(PExpr e : this._body_)
        {
            e.parent(null);
        }
        this._body_.clear();

        for(Object obj_e : list)
        {
            PExpr e = (PExpr) obj_e;
            if(e.parent() != null)
            {
                e.parent().removeChild(e);
            }

            e.parent(this);
            this._body_.add(e);
        }
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._head_)
            + toString(this._ask_)
            + toString(this._tell_)
            + toString(this._body_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._head_ == child)
        {
            this._head_ = null;
            return;
        }

        if(this._ask_.remove(child))
        {
            return;
        }

        if(this._tell_.remove(child))
        {
            return;
        }

        if(this._body_.remove(child))
        {
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._head_ == oldChild)
        {
            setHead((PHead) newChild);
            return;
        }

        for(ListIterator<PExpr> i = this._ask_.listIterator(); i.hasNext();)
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

        for(ListIterator<PExpr> i = this._tell_.listIterator(); i.hasNext();)
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

        for(ListIterator<PExpr> i = this._body_.listIterator(); i.hasNext();)
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

        throw new RuntimeException("Not a child.");
    }
}