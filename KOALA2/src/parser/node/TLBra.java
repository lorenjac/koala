/* This file was generated by SableCC (http://www.sablecc.org/). */

package parser.node;

import parser.analysis.*;

@SuppressWarnings("nls")
public final class TLBra extends Token
{
    public TLBra()
    {
        super.setText("[");
    }

    public TLBra(int line, int pos)
    {
        super.setText("[");
        setLine(line);
        setPos(pos);
    }

    @Override
    public Object clone()
    {
      return new TLBra(getLine(), getPos());
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseTLBra(this);
    }

    @Override
    public void setText(@SuppressWarnings("unused") String text)
    {
        throw new RuntimeException("Cannot change TLBra text.");
    }
}
