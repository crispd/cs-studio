/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.csstudio.scan.command.CommandSequence;
import org.csstudio.scan.command.Comparison;
import org.csstudio.scan.command.LogCommand;
import org.csstudio.scan.command.LoopCommand;
import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.SetCommand;
import org.csstudio.scan.command.WaitCommand;
import org.junit.Test;

/** JUnit test of the Command printout
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CommandSequenceUnitTest
{
    @Test
    public void testCommandSequence()
    {
    	final CommandSequence commands = new CommandSequence();
    	// Add commands
    	commands.add(new SetCommand("setpoint", 1));
    	commands.add(new WaitCommand("readback", Comparison.EQUALS, 1.0, 0.1, 0.0));
    	// Add same commands via shortcut
    	commands.set("setpoint", 1);
    	commands.wait("readback", 1.0, 0.1);
    	// Add a loop
    	commands.loop("setpoint", 1, 5, 1, new LogCommand("readback"));
    	// and another root command
        commands.log("readback");
    	commands.dump();
    	// Check the list
    	final List<ScanCommand> list = commands.getCommands();
        assertEquals(6, list.size());
        assertSame(list.get(0).getClass(), list.get(2).getClass());
        assertSame(list.get(1).getClass(), list.get(3).getClass());
        // Addresses should be 0, 1, ..., 4
        for (int i=0; i<5; ++i)
            assertEquals(i, list.get(i).getAddress());
        // .. continuing inside the loop
        assertSame(LoopCommand.class, list.get(4).getClass());
        assertEquals(5, ((LoopCommand)list.get(4)).getBody().get(0).getAddress());
        // .. and then back to the root list
        assertEquals(6, list.get(5).getAddress());
    }
}
