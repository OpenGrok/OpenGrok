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
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package org.opensolaris.opengrok.analysis.plain;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.opensolaris.opengrok.analysis.Definitions;
import org.opensolaris.opengrok.analysis.FileAnalyzerFactory;
import org.opensolaris.opengrok.analysis.JFlexTokenizer;
import org.opensolaris.opengrok.analysis.JFlexXref;
import org.opensolaris.opengrok.analysis.StreamSource;
import org.opensolaris.opengrok.configuration.Project;
import org.opensolaris.opengrok.history.Annotation;

/**
 *
 * @author Lubos Kosco
 */
public abstract class AbstractSourceCodeAnalyzer extends PlainAnalyzer {

    /**
     * Creates a new instance of abstract analyzer
     */
    protected AbstractSourceCodeAnalyzer(FileAnalyzerFactory factory) {
        super(factory);
    }

    /**
     * Create a symbol tokenizer for the language supported by this analyzer.
     * @param reader the data to tokenize
     * @return a symbol tokenizer
     */
    protected abstract JFlexTokenizer newSymbolTokenizer(Reader reader);

    /**
     * Create an xref for the language supported by this analyzer.
     * @param reader the data to produce xref for
     * @return an xref instance
     */
    @Override
    protected abstract JFlexXref newXref(Reader reader);

    @Override
    public void analyze(Document doc, StreamSource src, Writer xrefOut) throws IOException {
        super.analyze(doc, src, xrefOut);
        doc.add(new TextField("refs", getReader(src.getStream())));
    }

    @Override
    public Analyzer.TokenStreamComponents createComponents(String fieldName, Reader reader) {
        if ("refs".equals(fieldName)) {
            return new TokenStreamComponents(newSymbolTokenizer(reader));
        }
        return super.createComponents(fieldName, reader);
    }

    /**
     * Write a cross referenced HTML file reads the source from in
     *
     * @param in Input source
     * @param out Output xref writer
     * @param defs definitions for the file (could be null)
     * @param annotation annotation for the file (could be null)
     */
    static protected void writeXref(JFlexXref lxref, Reader in, Writer out, Definitions defs, Annotation annotation, Project project) throws IOException {
        if (lxref != null) {
            lxref.reInit(in);
            lxref.annotation = annotation;
            lxref.project = project;
            lxref.setDefs(defs);
            lxref.write(out);
        }
    }
}
