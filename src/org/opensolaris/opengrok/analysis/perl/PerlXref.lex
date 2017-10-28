/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017, Chris Fraire <cfraire@me.com>.
 */

/*
 * Cross reference a Perl file
 */

package org.opensolaris.opengrok.analysis.perl;

import java.io.IOException;
import org.opensolaris.opengrok.analysis.JFlexXref;
import org.opensolaris.opengrok.web.Util;

%%
%public
%class PerlXref
%extends JFlexXref
%implements PerlLexListener
%unicode
%int
%char
%init{
        h = new PerlLexHelper(QUO, QUOxN, QUOxL, QUOxLxN, this,
            HERE, HERExN, HEREin, HEREinxN);
%init}
%{
  // TODO move this into an include file when bug #16053 is fixed
  @Override
  protected int getLineNumber() { return yyline; }
  @Override
  protected void setLineNumber(int x) { yyline = x; }

    private final PerlLexHelper h;

    public void pushState(int state) { yypush(state, null); }

    public void popState() throws IOException { yypop(); }

    public void switchState(int state) { yybegin(state); }

    public void take(String value) throws IOException {
        out.write(value);
    }

    public void takeNonword(String value) throws IOException {
        out.write(htmlize(value));
    }

    public void takeUnicode(String value) throws IOException {
        for (int i = 0; i < value.length(); i++){
            char c = value.charAt(i);
            writeUnicodeChar(c);
        }
    }

    public boolean takeSymbol(String value, int captureOffset,
        boolean ignoreKwd)
            throws IOException {
        if (ignoreKwd) {
            if (value.length() > 1) {
                return writeSymbol(value, null, yyline);
            } else {
                out.write(value);
                return false;
            }
        } else {
            return writeSymbol(value, Consts.kwd, yyline);
        }
    }

    public void skipSymbol() {
        // noop
    }

    public void takeKeyword(String value) throws IOException {
        writeKeyword(value, yyline);
    }

    public void doStartNewLine() throws IOException {
        startNewLine();
    }

    public void abortQuote() throws IOException {
        yypop();
        if (h.areModifiersOK()) yypush(QM, null);
        take(Consts.ZS);
    }

    public void pushback(int numChars) {
        yypushback(numChars);
    }

    // If the state is YYINITIAL, then transitions to INTRA; otherwise does
    // nothing, because other transitions would have saved the state.
    public void maybeIntraState() {
        if (yystate() == YYINITIAL) yybegin(INTRA);
    }

    protected boolean takeAllContent() {
        return true;
    }

    protected boolean returnOnSymbol() {
        return false;
    }

    protected int getSymbolReturn() {
        return 0; // irrelevant value because returnOnSymbol() is false
    }

    protected String getUrlPrefix() { return urlPrefix; }
%}

%include PerlProductions.lexh
