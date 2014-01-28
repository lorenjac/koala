/* This file was generated by SableCC (http://www.sablecc.org/). */

package parser.node;

import parser.analysis.*;

@SuppressWarnings("nls")
public final class TBlank extends Token
{
    public TBlank(String text)
    {
        setText(text);
    }

    public TBlank(String text, int line, int pos)
    {
        setText(text);
        setLine(line);
        setPos(pos);
    }

    @Override
    public Object clone()
    {
      return new TBlank(getText(), getLine(), getPos());
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseTBlank(this);
    }
}
