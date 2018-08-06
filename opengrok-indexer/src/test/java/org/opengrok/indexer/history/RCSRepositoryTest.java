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
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
 */
package org.opengrok.indexer.history;

import java.io.File;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.opengrok.indexer.condition.ConditionalRun;
import org.opengrok.indexer.condition.ConditionalRunRule;
import org.opengrok.indexer.condition.RepositoryInstalled;
import org.opengrok.indexer.util.TestRepository;

import static org.junit.Assert.*;

/**
 *
 * @author Vladimir Kotal
 */
@ConditionalRun(RepositoryInstalled.RCSInstalled.class)
public class RCSRepositoryTest {

    @Rule
    public ConditionalRunRule rule = new ConditionalRunRule();

    static private TestRepository repository = new TestRepository();

    @BeforeClass
    public static void setUpClass() throws IOException {
        repository.create(RCSRepositoryTest.class.getResourceAsStream("repositories.zip"));
    }

    @AfterClass
    public static void tearDownClass() {
        repository.destroy();
        repository = null;
    }

    @Test
    public void testRepositoryDetection() throws Exception {
        File root = new File(repository.getSourceRoot(), "rcs_test");
        Object ret = RepositoryFactory.getRepository(root);
        assertTrue(ret instanceof RCSRepository);
    }

    @Test
    public void testAnnotation() throws Exception {
        File root = new File(repository.getSourceRoot(), "rcs_test");
        RCSRepository repo = (RCSRepository) RepositoryFactory.getRepository(root);
        File header = new File(root, "header.h");
        Annotation annotation = repo.annotate(header, null);
        assertNotNull(annotation);
        Annotation expAnnotation = new Annotation(header.getName());
        expAnnotation.addLine("1.1", "kah", true);
        expAnnotation.addLine("1.1", "kah", true);
        assertEquals(expAnnotation.toString(), annotation.toString());
    }
}
