/* This file was generated by SableCC (http://www.sablecc.org/). */

package parser.node;

import parser.analysis.*;

@SuppressWarnings("nls")
public final class TWildcard extends Token
{
    public TWildcard()
    {
        super.setText("_");
    }

    public TWildcard(int line, int pos)
    {
        super.setText("_");
        setLine(line);
        setPos(pos);
    }

    @Override
    public Object clone()
    {
      return new TWildcard(getLine(), getPos());
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseTWildcard(this);
    }

    @Override
    public void setText(@SuppressWarnings("unused") String text)
    {
        throw new RuntimeException("Cannot change TWildcard text.");
    }
}
