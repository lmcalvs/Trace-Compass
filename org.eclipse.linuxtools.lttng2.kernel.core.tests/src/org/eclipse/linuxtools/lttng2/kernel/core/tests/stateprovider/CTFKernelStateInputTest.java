/*******************************************************************************
 * Copyright (c) 2012 Ericsson
 * Copyright (c) 2010, 2011 École Polytechnique de Montréal
 * Copyright (c) 2010, 2011 Alexandre Montplaisir <alexandre.montplaisir@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/

package org.eclipse.linuxtools.lttng2.kernel.core.tests.stateprovider;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.eclipse.linuxtools.tmf.core.statesystem.StateSystem;
import org.eclipse.linuxtools.tmf.core.statesystem.helpers.IStateChangeInput;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.stateprovider.CTFKernelStateInput;

/**
 * Tests for the LTTng 2.0 kernel state provider
 * 
 * @author alexmont
 *
 */
public class CTFKernelStateInputTest {

    protected static IStateChangeInput input;

    @BeforeClass
    public static void initialize() throws FileNotFoundException {
        input = new CTFKernelStateInput(CTFTestFiles.getTestTrace());

    }

    @AfterClass
    public static void cleanup() {
        //
    }

    @Test
    public void testOpening() {
        long testStartTime;
        testStartTime = input.getStartTime();
        assertEquals(testStartTime, CTFTestFiles.startTime);
    }

    @Test
    public void testRunning() {
        StateSystem ss = new StateSystem();
        input.assignTargetStateSystem(ss);
        input.getStateSystem(); /* Let's pad that coverage... */
        input.run();
    }

}