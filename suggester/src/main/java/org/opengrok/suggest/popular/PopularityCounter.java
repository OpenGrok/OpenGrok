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
package org.opengrok.suggest.popular;

import org.apache.lucene.util.BytesRef;

/**
 * Simple interface for accessing the popularity data for specific terms.
 */
@FunctionalInterface
public interface PopularityCounter {

    /**
     * For the term {@code key} returns the number the term was searched for.
     * @param key the term to retrieve data for
     * @return number of times the {@key} was searched for
     */
    int get(BytesRef key);

}
