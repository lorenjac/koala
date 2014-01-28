/* This file was generated by SableCC (http://www.sablecc.org/). */

package parser.node;

import parser.analysis.*;

@SuppressWarnings("nls")
public final class ANumberExpr extends PExpr
{
    private TNumber _value_;

    public ANumberExpr()
    {
        // Constructor
    }

    public ANumberExpr(
        @SuppressWarnings("hiding") TNumber _value_)
    {
        // Constructor
        setValue(_value_);

    }

    @Override
    public Object clone()
    {
        return new ANumberExpr(
            cloneNode(this._value_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseANumberExpr(this);
    }

    public TNumber getValue()
    {
        return this._value_;
    }

    public void setValue(TNumber node)
    {
        if(this._value_ != null)
        {
            this._value_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._value_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._value_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._value_ == child)
        {
            this._value_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._value_ == oldChild)
        {
            setValue((TNumber) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}