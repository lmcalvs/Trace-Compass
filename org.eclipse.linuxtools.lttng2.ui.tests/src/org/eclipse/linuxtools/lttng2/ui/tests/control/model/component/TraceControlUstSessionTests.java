/**********************************************************************
 * Copyright (c) 2012 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Bernd Hufmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.linuxtools.lttng2.ui.tests.control.model.component;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.linuxtools.internal.lttng2.stubs.dialogs.CreateChannelDialogStub;
import org.eclipse.linuxtools.internal.lttng2.stubs.dialogs.CreateSessionDialogStub;
import org.eclipse.linuxtools.internal.lttng2.stubs.dialogs.DestroyConfirmDialogStub;
import org.eclipse.linuxtools.internal.lttng2.stubs.dialogs.EnableEventsDialogStub;
import org.eclipse.linuxtools.internal.lttng2.stubs.dialogs.GetEventInfoDialogStub;
import org.eclipse.linuxtools.internal.lttng2.stubs.service.TestRemoteSystemProxy;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.dialogs.TraceControlDialogFactory;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.ITraceControlComponent;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.LogLevelType;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.TargetNodeState;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.TraceEnablement;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.TraceEventType;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.TraceLogLevel;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.TraceSessionState;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.impl.ChannelInfo;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.impl.TargetNodeComponent;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.impl.TraceChannelComponent;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.impl.TraceEventComponent;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.impl.TraceSessionComponent;
import org.eclipse.linuxtools.lttng2.ui.tests.Activator;
import org.eclipse.rse.core.model.Host;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.internal.core.model.SystemProfile;
import org.junit.After;
import org.junit.Before;

/**
 * The class <code>TraceControlUstSessionTests</code> contains UST session/channel/event
 * handling test cases.
 */
@SuppressWarnings("nls")
public class TraceControlUstSessionTests extends TestCase {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------
    private static final String TEST_STREAM = "CreateTreeTest.cfg";
    private static final String SCEN_SCENARIO4_TEST = "Scenario4";

    // ------------------------------------------------------------------------
    // Test data
    // ------------------------------------------------------------------------
    private TraceControlTestFacility fFacility;
    private TestRemoteSystemProxy fProxy;
    private String fTestFile; 
    
    // ------------------------------------------------------------------------
    // Static methods
    // ------------------------------------------------------------------------

    /**
     * Returns test setup used when executing test case stand-alone.
     * @return Test setup class 
     */
    public static Test suite() {
        return new ModelImplTestSetup(new TestSuite(TraceControlUstSessionTests.class));
    }

    // ------------------------------------------------------------------------
    // Housekeeping
    // ------------------------------------------------------------------------

    /**
     * Perform pre-test initialization.
     *
     * @throws Exception
     *         if the initialization fails for some reason
     *
     */
    @Override
    @Before
    public void setUp() throws Exception {
        fFacility = TraceControlTestFacility.getInstance();
        fProxy = new TestRemoteSystemProxy();
        URL location = FileLocator.find(Activator.getDefault().getBundle(), new Path(TraceControlTestFacility.DIRECTORY + File.separator + TEST_STREAM), null);
        File testfile = new File(FileLocator.toFileURL(location).toURI());
        fTestFile = testfile.getAbsolutePath();
    }

    /**
     * Perform post-test clean-up.
     *
     * @throws Exception
     *         if the clean-up fails for some reason
     *
     */
    @Override
    @After
    public void tearDown()  throws Exception {
    }
    
    /**
     * Run the TraceControlComponent.
     */
    public void testTraceSessionTree() throws Exception {
        
        fProxy.setTestFile(fTestFile);
        fProxy.setScenario(TraceControlTestFacility.SCEN_INIT_TEST);
        
        ITraceControlComponent root = TraceControlTestFacility.getInstance().getControlView().getTraceControlRoot();

        @SuppressWarnings("restriction")
        IHost host = new Host(new SystemProfile("myProfile", true));
        host.setHostName("127.0.0.1");

        TargetNodeComponent node = new TargetNodeComponent("myNode", root, host, fProxy);

        root.addChild(node);
        node.connect();

        fFacility.waitForJobs();

        // Verify that node is connected
        assertEquals(TargetNodeState.CONNECTED, node.getTargetNodeState());

        // Get provider groups
        ITraceControlComponent[] groups = node.getChildren();
        assertNotNull(groups);
        assertEquals(2, groups.length);

        // Initialize dialog implementations for command execution
        TraceControlDialogFactory.getInstance().setCreateSessionDialog(new CreateSessionDialogStub());
        TraceControlDialogFactory.getInstance().setGetEventInfoDialog(new GetEventInfoDialogStub());
        TraceControlDialogFactory.getInstance().setConfirmDialog(new DestroyConfirmDialogStub());

        // Initialize session handling scenario
        fProxy.setScenario(TraceControlTestFacility.SCEN_SCENARIO_SESSION_HANDLING);
 
        // ------------------------------------------------------------------------
        // Create session
        // ------------------------------------------------------------------------
        TraceSessionComponent session = fFacility.createSession(groups[1]);
        
        // Verify that session was created
        assertNotNull(session);
        assertEquals("mysession", session.getName());
        assertEquals("/home/user/lttng-traces/mysession-20120314-132824", session.getSessionPath());
        assertEquals(TraceSessionState.INACTIVE, session.getSessionState());

        // Initialize scenario
        fProxy.setScenario(SCEN_SCENARIO4_TEST);
        
        // ------------------------------------------------------------------------
        // Enable default channel on created session above
        // ------------------------------------------------------------------------
        CreateChannelDialogStub channelStub = new CreateChannelDialogStub();
        channelStub.setIsKernel(false);
        TraceControlDialogFactory.getInstance().setCreateChannelDialog(channelStub);

        fFacility.executeCommand(session, "createChannelOnSession");
        
        // Verify that Kernel domain was created
        ITraceControlComponent[] domains = session.getChildren();
        assertNotNull(domains);
        assertEquals(1, domains.length);

        assertEquals("UST global", domains[0].getName());
        
        // Verify that channel was created with correct data
        ITraceControlComponent[] channels =  domains[0].getChildren();
        assertNotNull(channels);
        assertEquals(1, channels.length);

        assertTrue(channels[0] instanceof TraceChannelComponent);
        TraceChannelComponent channel = (TraceChannelComponent) channels[0]; 
        assertEquals("mychannel", channel.getName());
        assertEquals(4, channel.getNumberOfSubBuffers());
        assertEquals("mmap()", channel.getOutputType());
        assertEquals(true, channel.isOverwriteMode());
        assertEquals(200, channel.getReadTimer());
        assertEquals(TraceEnablement.ENABLED, channel.getState());
        assertEquals(16384, channel.getSubBufferSize());
        assertEquals(100, channel.getSwitchTimer());

        // ------------------------------------------------------------------------
        // Enable channel on domain
        // ------------------------------------------------------------------------
        ChannelInfo info = (ChannelInfo)channelStub.getChannelInfo();
        info.setName("mychannel2");
        info.setOverwriteMode(false);
        info.setSubBufferSize(32768);
        info.setNumberOfSubBuffers(2);
        info.setSwitchTimer(100);
        info.setReadTimer(200);
        channelStub.setChannelInfo(info);
        
        fFacility.executeCommand(domains[0], "createChannelOnDomain");

        // Get Kernel domain component instance
        domains = session.getChildren();
        assertNotNull(domains);
        assertEquals(1, domains.length);

        // Verify that channel was created with correct data
        channels =  domains[0].getChildren();
        assertNotNull(channels);
        assertEquals(2, channels.length);

        assertTrue(channels[1] instanceof TraceChannelComponent);
        channel = (TraceChannelComponent) channels[1]; 
        assertEquals("mychannel2", channel.getName());
        assertEquals(2, channel.getNumberOfSubBuffers());
        assertEquals("mmap()", channel.getOutputType());
        assertEquals(false, channel.isOverwriteMode());
        assertEquals(200, channel.getReadTimer());
        assertEquals(TraceEnablement.ENABLED, channel.getState());
        assertEquals(32768, channel.getSubBufferSize());
        assertEquals(100, channel.getSwitchTimer());

        // ------------------------------------------------------------------------
        // Enable event (tracepoint) on session and default channel
        // ------------------------------------------------------------------------
        EnableEventsDialogStub eventsDialogStub = new EnableEventsDialogStub();
        eventsDialogStub.setIsTracePoints(true);
        List<String> events = new ArrayList<String>();
        events.add("ust_tests_hello:tptest_sighandler");
        eventsDialogStub.setNames(events);
        eventsDialogStub.setIsKernel(false);
        TraceControlDialogFactory.getInstance().setEnableEventsDialog(eventsDialogStub);
        
        fFacility.executeCommand(session, "enableEventOnSession");
        
        // Get Kernel domain component instance
        domains = session.getChildren();
        assertNotNull(domains);
        assertEquals(1, domains.length);

        // Verify that channel was created with correct data
        channels =  domains[0].getChildren();
        assertNotNull(channels);
        assertEquals(3, channels.length);

        assertTrue(channels[2] instanceof TraceChannelComponent);
        channel = (TraceChannelComponent) channels[2]; 
        assertEquals("channel0", channel.getName());
        // No need to check parameters of default channel because that has been done in other tests

        ITraceControlComponent[] channel0Events = channel.getChildren();
        assertEquals(1, channel0Events.length);
        
        assertTrue(channel0Events[0] instanceof TraceEventComponent);

        TraceEventComponent event = (TraceEventComponent) channel0Events[0];
        assertEquals("ust_tests_hello:tptest_sighandler", event.getName());
        assertEquals(TraceLogLevel.LEVEL_UNKNOWN, event.getLogLevel()); // TODO
        assertEquals(TraceEventType.TRACEPOINT, event.getEventType());
        assertEquals(TraceEnablement.ENABLED, event.getState());

        // ------------------------------------------------------------------------
        // Enable event (tracepoint) on domain and default channel
        // ------------------------------------------------------------------------
        events.clear();
        events.add("ust_tests_hello:tptest");
        eventsDialogStub.setNames(events);
        
        fFacility.executeCommand(domains[0], "enableEventOnDomain");
        
        // Get Kernel domain component instance
        domains = session.getChildren();
        assertNotNull(domains);
        assertEquals(1, domains.length);

        // Verify that channel was created with correct data
        channels =  domains[0].getChildren();
        channel = (TraceChannelComponent) channels[2]; 
        // No need to check parameters of default channel because that has been done in other tests

        channel0Events = channel.getChildren();
        assertEquals(2, channel0Events.length);
        
        assertTrue(channel0Events[1] instanceof TraceEventComponent);

        event = (TraceEventComponent) channel0Events[1];
        assertEquals("ust_tests_hello:tptest", event.getName());
        assertEquals(TraceLogLevel.LEVEL_UNKNOWN, event.getLogLevel()); // TODO
        assertEquals(TraceEventType.TRACEPOINT, event.getEventType());
        assertEquals(TraceEnablement.ENABLED, event.getState());

        // ------------------------------------------------------------------------
        // Enable event (all tracepoints) on specific channel
        // ------------------------------------------------------------------------
        events.clear();
        eventsDialogStub.setNames(events);
        eventsDialogStub.setIsAllTracePoints(true);

        fFacility.executeCommand(channels[1], "enableEventOnChannel");

        // Get Kernel domain component instance
        domains = session.getChildren();
        assertNotNull(domains);
        assertEquals(1, domains.length);

        // Verify that channel was created with correct data
        channels =  domains[0].getChildren();
        channel = (TraceChannelComponent) channels[1]; 
        // No need to check parameters of default channel because that has been done in other tests

        channel = (TraceChannelComponent) channels[1];
        
        channel0Events = channel.getChildren();
        assertEquals(1, channel0Events.length);
        
        assertTrue(channel0Events[0] instanceof TraceEventComponent);

        event = (TraceEventComponent) channel0Events[0];
        assertEquals("*", event.getName());
        assertEquals(TraceLogLevel.LEVEL_UNKNOWN, event.getLogLevel());
        assertEquals(TraceEventType.TRACEPOINT, event.getEventType());
        assertEquals(TraceEnablement.ENABLED, event.getState());

        // ------------------------------------------------------------------------
        // Enable event (wildcard) on specific channel
        // ------------------------------------------------------------------------
        events.clear();
        eventsDialogStub.setIsTracePoints(false);        
        eventsDialogStub.setIsAllTracePoints(false);
        eventsDialogStub.setIsWildcard(true);
        eventsDialogStub.setWildcard("ust*");

        fFacility.executeCommand(channels[0], "enableEventOnChannel");

        // Get Kernel domain component instance
        domains = session.getChildren();
        assertNotNull(domains);
        assertEquals(1, domains.length);

        // Verify that channel was created with correct data
        channels =  domains[0].getChildren();
        channel = (TraceChannelComponent) channels[0]; 
        // No need to check parameters of default channel because that has been done in other tests

        channel0Events = channel.getChildren();
        assertEquals(1, channel0Events.length);
        
        assertTrue(channel0Events[0] instanceof TraceEventComponent);
        
        event = (TraceEventComponent) channel0Events[0];
        assertEquals("ust*", event.getName());
        assertEquals(TraceLogLevel.LEVEL_UNKNOWN, event.getLogLevel());
        assertEquals(TraceEventType.TRACEPOINT, event.getEventType());
        assertEquals(TraceEnablement.ENABLED, event.getState());

        // ------------------------------------------------------------------------
        // Enable event (wildcard) on domain
        // ------------------------------------------------------------------------
        events.clear();
        eventsDialogStub.setIsTracePoints(false);        
        eventsDialogStub.setIsAllTracePoints(false);
        eventsDialogStub.setIsWildcard(true);
        eventsDialogStub.setWildcard("ust*");

        fFacility.executeCommand(domains[0], "enableEventOnDomain");

        // Get Kernel domain component instance
        domains = session.getChildren();
        assertNotNull(domains);
        assertEquals(1, domains.length);

        // Verify that channel was created with correct data
        channels =  domains[0].getChildren();
        channel = (TraceChannelComponent) channels[0]; 
        // No need to check parameters of default channel because that has been done in other tests

        channel0Events = channel.getChildren();
        assertEquals(1, channel0Events.length);
        
        assertTrue(channel0Events[0] instanceof TraceEventComponent);
        
        event = (TraceEventComponent) channel0Events[0];
        assertEquals("ust*", event.getName());
        assertEquals(TraceLogLevel.LEVEL_UNKNOWN, event.getLogLevel());
        assertEquals(TraceEventType.TRACEPOINT, event.getEventType());
        assertEquals(TraceEnablement.ENABLED, event.getState());

        // ------------------------------------------------------------------------
        // Enable event (wildcard) on session
        // ------------------------------------------------------------------------
        events.clear();
        eventsDialogStub.setIsTracePoints(false);        
        eventsDialogStub.setIsAllTracePoints(false);
        eventsDialogStub.setIsWildcard(true);
        eventsDialogStub.setWildcard("ust*");

        fFacility.executeCommand(domains[0], "enableEventOnDomain");

        // Get Kernel domain component instance
        domains = session.getChildren();
        assertNotNull(domains);
        assertEquals(1, domains.length);

        // Verify that channel was created with correct data
        channels =  domains[0].getChildren();
        channel = (TraceChannelComponent) channels[2]; 
        // No need to check parameters of default channel because that has been done in other tests

        channel0Events = channel.getChildren();
        assertEquals(4, channel0Events.length);
        
        assertTrue(channel0Events[0] instanceof TraceEventComponent);
        
        event = (TraceEventComponent) channel0Events[0];
        assertEquals("u*", event.getName());
        assertEquals(TraceLogLevel.LEVEL_UNKNOWN, event.getLogLevel());
        assertEquals(TraceEventType.TRACEPOINT, event.getEventType());
        assertEquals(TraceEnablement.ENABLED, event.getState());

        // ------------------------------------------------------------------------
        // Enable event (loglevel) on domain
        // ------------------------------------------------------------------------
        events.clear();
        eventsDialogStub.setIsWildcard(false);
        eventsDialogStub.setIsLogLevel(true);
        eventsDialogStub.setLogLevelEventName("myevent1");
        eventsDialogStub.setLogLevelType(LogLevelType.LOGLEVEL);
        eventsDialogStub.setLogLevel(TraceLogLevel.TRACE_WARNING);
        
        fFacility.executeCommand(domains[0], "enableEventOnDomain");

        // Get Kernel domain component instance
        domains = session.getChildren();
        assertNotNull(domains);
        assertEquals(1, domains.length);

        // Verify that channel was created with correct data
        channels =  domains[0].getChildren();
        channel = (TraceChannelComponent) channels[2]; 
        // No need to check parameters of default channel because that has been done in other tests

        channel0Events = channel.getChildren();
        assertEquals(5, channel0Events.length);
        
        assertTrue(channel0Events[0] instanceof TraceEventComponent);
        
        event = (TraceEventComponent) channel0Events[0];
        assertEquals("myevent1", event.getName());
        assertEquals(TraceLogLevel.TRACE_WARNING, event.getLogLevel());
        assertEquals(TraceEventType.TRACEPOINT, event.getEventType());
        assertEquals(TraceEnablement.ENABLED, event.getState());

        // ------------------------------------------------------------------------
        // Enable event (loglevel) on session
        // ------------------------------------------------------------------------
        eventsDialogStub.setLogLevelEventName("myevent2");
        eventsDialogStub.setLogLevelType(LogLevelType.LOGLEVEL_ONLY);
        eventsDialogStub.setLogLevel(TraceLogLevel.TRACE_DEBUG_FUNCTION);
        
        fFacility.executeCommand(session, "enableEventOnSession");
        
        // Get Kernel domain component instance
        domains = session.getChildren();
        assertNotNull(domains);
        assertEquals(1, domains.length);

        // Verify that channel was created with correct data
        channels =  domains[0].getChildren();
        channel = (TraceChannelComponent) channels[2]; 
        // No need to check parameters of default channel because that has been done in other tests

        channel0Events = channel.getChildren();
        assertEquals(6, channel0Events.length);
        
        assertTrue(channel0Events[0] instanceof TraceEventComponent);
        
        event = (TraceEventComponent) channel0Events[0];
        assertEquals("myevent2", event.getName());
        assertEquals(TraceLogLevel.TRACE_DEBUG_FUNCTION, event.getLogLevel());
        assertEquals(TraceEventType.TRACEPOINT, event.getEventType());
        assertEquals(TraceEnablement.ENABLED, event.getState());

        // ------------------------------------------------------------------------
        // Enable event (loglevel) on channel
        // ------------------------------------------------------------------------
        eventsDialogStub.setLogLevelEventName("myevent0");
        eventsDialogStub.setLogLevelType(LogLevelType.LOGLEVEL_ONLY);
        eventsDialogStub.setLogLevel(TraceLogLevel.TRACE_DEBUG_FUNCTION);
        
        fFacility.executeCommand(channels[0], "enableEventOnChannel");
        
        // Get Kernel domain component instance
        domains = session.getChildren();
        assertNotNull(domains);
        assertEquals(1, domains.length);

        // Verify that channel was created with correct data
        channels =  domains[0].getChildren();
        channel = (TraceChannelComponent) channels[0]; 
        // No need to check parameters of default channel because that has been done in other tests

        channel0Events = channel.getChildren();
        assertEquals(2, channel0Events.length);
        
        assertTrue(channel0Events[0] instanceof TraceEventComponent);
        
        event = (TraceEventComponent) channel0Events[0];
        assertEquals("myevent0", event.getName());
        assertEquals(TraceLogLevel.TRACE_DEBUG_FUNCTION, event.getLogLevel());
        assertEquals(TraceEventType.TRACEPOINT, event.getEventType());
        assertEquals(TraceEnablement.ENABLED, event.getState());
        
        // ------------------------------------------------------------------------
        // Destroy session 
        // ------------------------------------------------------------------------
        // Initialize session handling scenario
        fProxy.setScenario(TraceControlTestFacility.SCEN_SCENARIO_SESSION_HANDLING);

        fFacility.destroySession(session);
        
        // Verify that no more session components exist
        assertEquals(0, groups[1].getChildren().length);

        //-------------------------------------------------------------------------
        // Disconnect node
        //-------------------------------------------------------------------------
        fFacility.executeCommand(node, "disconnect");
        assertEquals(TargetNodeState.DISCONNECTED, node.getTargetNodeState());

        //-------------------------------------------------------------------------
        // Delete node
        //-------------------------------------------------------------------------
        
        fFacility.executeCommand(node, "delete");

        assertEquals(0,fFacility.getControlView().getTraceControlRoot().getChildren().length);
    }
}