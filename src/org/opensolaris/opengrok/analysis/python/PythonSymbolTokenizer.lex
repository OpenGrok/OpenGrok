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
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017, Chris Fraire <cfraire@me.com>.
 */

/*
 * Gets Python symbols - ignores comments, strings, keywords
 */

package org.opensolaris.opengrok.analysis.python;

import org.opensolaris.opengrok.analysis.JFlexSymbolMatcher;
%%
%public
%class PythonSymbolTokenizer
%extends JFlexSymbolMatcher
%unicode
%init{
%init}
%int
%include CommonTokenizer.lexh
%char

%state STRING LSTRING SCOMMENT QSTRING LQSTRING

%include Common.lexh
%include Python.lexh
%%

<YYINITIAL> {
{Identifier} {
    String id = yytext();
                if(!Consts.kwd.contains(id)){
                        onSymbolMatched(id, yychar, yychar + yylength());
                        return yystate();
                }
 }

 {Number}        {}

 \"     { yybegin(STRING); }
 \"\"\" { yybegin(LSTRING); }
 \'     { yybegin(QSTRING); }
 \'\'\' { yybegin(LQSTRING); }
 "#"   { yybegin(SCOMMENT); }
 }

<STRING> {
 \\[\"\\]    {}
 \"     { yybegin(YYINITIAL); }
 {EOL}  { yybegin(YYINITIAL); }
}

<QSTRING> {
 \\[\'\\]    {}
 \'     { yybegin(YYINITIAL); }
 {EOL}  { yybegin(YYINITIAL); }
}

<LSTRING> {
 \\[\"\\]    {}
 \"\"\" { yybegin(YYINITIAL); }
}

<LQSTRING> {
 \\[\'\\]    {}
 \'\'\' { yybegin(YYINITIAL); }
}

<SCOMMENT> {
 {WhiteSpace}    {}
 {EOL}    { yybegin(YYINITIAL);}
}

<YYINITIAL, STRING, LSTRING, SCOMMENT, QSTRING , LQSTRING> {
[^]    {}
}