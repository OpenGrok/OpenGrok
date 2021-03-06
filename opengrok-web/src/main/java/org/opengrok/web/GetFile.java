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
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2020, Chris Fraire <cfraire@me.com>.
 * Portions Copyright (c) 2011, Jens Elkner.
 */
package org.opengrok.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.opengrok.indexer.history.HistoryGuru;
import org.opengrok.indexer.web.Prefix;

public class GetFile extends HttpServlet {

    public static final long serialVersionUID = -1;

    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        PageConfig cfg = PageConfig.get(request);
        cfg.checkSourceRootExistence();

        String redir = cfg.canProcess();
        if (redir == null || redir.length() > 0) {
            if (redir != null) {
                response.sendRedirect(redir);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }

        File f = cfg.getResourceFile();
        String revision = cfg.getRequestedRevision();
        if (revision.length() == 0) {
            revision = null;
        }

        InputStream in;
        try {
            if (revision != null) {
                in = HistoryGuru.getInstance().getRevision(f.getParent(),
                        f.getName(), revision);
            } else {
                long flast = cfg.getLastModified();
                if (request.getDateHeader("If-Modified-Since") >= flast) {
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }
                response.setContentLength((int) f.length());
                response.setDateHeader("Last-Modified", f.lastModified());
                in = new FileInputStream(f);
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            String mimeType = getServletContext().getMimeType(f.getAbsolutePath());
            response.setContentType(mimeType);

            if (cfg.getPrefix() == Prefix.DOWNLOAD_P) {
                response.setHeader("content-disposition", "attachment; filename="
                        + f.getName());
            } else {
                response.setHeader("content-type", "text/plain");
            }
            OutputStream o = response.getOutputStream();
            byte[] buffer = new byte[8192];
            int nr;
            while ((nr = in.read(buffer)) > 0) {
                o.write(buffer, 0, nr);
            }
            o.flush();
            o.close();
        } finally {
            in.close();
        }
    }
}