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
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
 */
package org.opensolaris.opengrok.configuration.messages;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * project specific message
 *
 * @author Vladimir Kotal
 */
public class ProjectMessage extends Message {

    private static final Set<String> allowedText = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "add",
            "delete",
            "list",
            "list-indexed",
            "indexed",
            "get-repos",
            "get-repos-type"
    )));

    ProjectMessage() {
    }

    /**
     * Validate ProjectMessage.
     * Tags are project names, text is command (add/delete)
     * @throws ValidationException if message has invalid format
     */
    @Override
    public void validate() throws ValidationException {
        String command = getText();

        // Text field carries the command.
        if (command == null) {
            throw new ValidationException("The message text must contain one of '" + allowedText.toString() + "'");
        }
        if (!allowedText.contains(command)) {
            throw new ValidationException("The message text must contain one of '" + allowedText.toString() + "'");
        }

        if (!command.startsWith("list") && getTags().isEmpty()) {
            throw new ValidationException("The message must contain a tag (project name(s))");
        }

        super.validate();
    }
}
