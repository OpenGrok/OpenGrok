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
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 */
package org.opensolaris.opengrok.configuration.messages.handler;

import org.json.simple.parser.ParseException;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;
import org.opensolaris.opengrok.configuration.messages.Message;
import org.opensolaris.opengrok.configuration.messages.MessageHandler;
import org.opensolaris.opengrok.configuration.messages.Response;
import org.opensolaris.opengrok.web.Statistics;
import org.opensolaris.opengrok.web.Util;

import java.io.IOException;

public class StatsMessageHandler implements MessageHandler {

    private RuntimeEnvironment env;

    public StatsMessageHandler(RuntimeEnvironment env) {
        this.env = env;
    }

    @Override
    public Response handle(Message message) throws HandleException {
        String command = message.getText().toLowerCase();

        switch (command) {
            case "reload":
                try {
                    env.loadStatistics();
                } catch (IOException | ParseException e) {
                    throw new HandleException(e.getMessage());
                }
                break;
            case "clean":
                env.setStatistics(new Statistics());
                break;
            case "get": // statistics are always returned at the end so do nothing
                break;
            default:
                throw new HandleException("Unknown command " + message.getText());
        }
        return Response.of(Util.statisticToJson((env.getStatistics())).toString());
    }

}
