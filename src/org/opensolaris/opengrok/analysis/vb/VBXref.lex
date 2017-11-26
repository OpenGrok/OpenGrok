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
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017, Chris Fraire <cfraire@me.com>.
 */

/*
 * Cross reference a VB file
 */

package org.opensolaris.opengrok.analysis.vb;

import org.opensolaris.opengrok.analysis.JFlexXrefSimple;
import org.opensolaris.opengrok.web.HtmlConsts;
import org.opensolaris.opengrok.web.Util;
%%
%public
%class VBXref
%extends JFlexXrefSimple
%unicode
%ignorecase
%int
%include CommonXref.lexh

File = [a-zA-Z]{FNameChar}* "." ("vb"|"cls"|"frm"|"vbs"|"bas"|"ctl")

%state  STRING COMMENT

%include Common.lexh
%include CommonURI.lexh
%include CommonPath.lexh
%include VB.lexh
%%
<YYINITIAL>{

{Identifier} {
    String id = yytext();
    writeSymbol(id, Consts.reservedKeywords, yyline, false);
}

"<" ({File}|{FPath}) ">" {
        out.write("&lt;");
        String path = yytext();
        path = path.substring(1, path.length() - 1);
        out.write("<a href=\""+urlPrefix+"path=");
        out.write(path);
        appendProject();
        out.write("\">");
        out.write(path);
        out.write("</a>");
        out.write("&gt;");
}

/*{Hier}
        { out.write(Util.breadcrumbPath(urlPrefix+"defs=",yytext(),'.'));}
*/
 {Number}        {
    disjointSpan(HtmlConsts.NUMBER_CLASS);
    out.write(yytext());
    disjointSpan(null);
 }

 \"   {
    pushSpan(STRING, HtmlConsts.STRING_CLASS);
    out.write(htmlize(yytext()));
 }
 \'   {
    pushSpan(COMMENT, HtmlConsts.COMMENT_CLASS);
    out.write(htmlize(yytext()));
 }
}

<STRING> {
 \"\" |
 \" {WhiteSpace} \"    { out.write(htmlize(yytext())); }
 \"     {
    out.write(htmlize(yytext()));
    yypop();
 }
 {WhspChar}*{EOL}    {
    disjointSpan(null);
    startNewLine();
    disjointSpan(HtmlConsts.STRING_CLASS);
 }
}

<COMMENT> {
  {WhspChar}*{EOL} {
    yypop();
    startNewLine();
  }
}


<YYINITIAL, STRING, COMMENT> {
[&<>\'\"]    { out.write(htmlize(yytext())); }
{WhspChar}*{EOL}      { startNewLine(); }
 {WhiteSpace}   { out.write(yytext()); }
 [!-~]  { out.write(yycharat(0)); }
 [^\n]      { writeUnicodeChar(yycharat(0)); }
}

<STRING, COMMENT> {
{FPath}
        { out.write(Util.breadcrumbPath(urlPrefix+"path=",yytext(),'/'));}

{File}
        {
        String path = yytext();
        out.write("<a href=\""+urlPrefix+"path=");
        out.write(path);
        appendProject();
        out.write("\">");
        out.write(path);
        out.write("</a>");}

{BrowseableURI}    {
          appendLink(yytext(), true);
        }

{FNameChar}+ "@" {FNameChar}+ "." {FNameChar}+
        {
          writeEMailAddress(yytext());
        }
}
