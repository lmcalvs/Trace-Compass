/*******************************************************************************
 * Copyright (c) 2014 Ericsson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marc Khouzam (Ericsson) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.dsf.core.service;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.ImmediateExecutor;
import org.eclipse.cdt.dsf.concurrent.Immutable;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.AbstractDMContext;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.datamodel.IDMData;
import org.eclipse.cdt.dsf.debug.service.ICachingService;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS2;
import org.eclipse.cdt.dsf.service.AbstractDsfService;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.dsf.core.DsfTraceCorePlugin;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.Attributes;
import org.eclipse.tracecompass.lttng2.kernel.core.analysis.cpuusage.LttngKernelCpuUsageAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.osgi.framework.BundleContext;

/** */
public class TraceHardwareAndOSService extends AbstractDsfService implements IGDBHardwareAndOS2, ICachingService {

    final private ITmfTrace fTrace;
    final private LttngKernelCpuUsageAnalysis fCPUModule;
    final private ITmfStateSystem fStateSys;
    final private Map<ICPUDMContext, ICoreDMContext[]> fMapCPUToCores = new HashMap<>();

    @Immutable
    private static class GDBCPUDMC extends AbstractDMContext
            implements ICPUDMContext
    {
        /**
         * String ID that is used to identify the thread in the GDB/MI protocol.
         */
        private final String fId;

        /**
         * @param sessionId
         *            The session
         * @param targetDmc
         *            The target
         * @param id
         *            the CPU id
         */
        protected GDBCPUDMC(String sessionId, IHardwareTargetDMContext targetDmc, String id) {
            super(sessionId, targetDmc == null ? new IDMContext[0] : new IDMContext[] { targetDmc });
            fId = id;
        }

        @Override
        public String getId() {
            return fId;
        }

        @Override
        public String toString() {
            return baseToString() + ".CPU[" + fId + "]";} //$NON-NLS-1$ //$NON-NLS-2$

        @Override
        public boolean equals(Object obj) {
            return baseEquals(obj) && ((GDBCPUDMC) obj).fId.equals(fId);
        }

        @Override
        public int hashCode() {
            return baseHashCode() ^ fId.hashCode();
        }
    }

    @Immutable
    private static class GDBCoreDMC extends AbstractDMContext
            implements ICoreDMContext
    {
        /**
         * E.g. The name given to the State system node representing a cpu core
         */
        private final String fId;
        /**
         * The node in the Tmftrace State system,
         */
        private final Integer fNode;

        public GDBCoreDMC(String sessionId, ICPUDMContext CPUDmc, Integer coreNode, String id) {
            super(sessionId, CPUDmc == null ? new IDMContext[0] : new IDMContext[] { CPUDmc });
            fId = id;
            fNode = coreNode;
        }

        @Override
        public String getId() {
            return fId;
        }

        // The State system node is the key to access any additional
        // information, however the value does not represent the actual core id
        public Integer getNode() {
            return fNode;
        }

        @Override
        public String toString() {
            return baseToString() + ".core[" + fId + "]";} //$NON-NLS-1$ //$NON-NLS-2$

        @Override
        public boolean equals(Object obj) {
            return baseEquals(obj) &&
                    (((GDBCoreDMC) obj).fId == null ? fId == null : ((GDBCoreDMC) obj).fId.equals(fId));
        }

        @Override
        public int hashCode() {
            return baseHashCode() ^ (fId == null ? 0 : fId.hashCode());
        }
    }

    // @Immutable
    // private static class GDBCPUDMData implements ICPUDMData {
    // final int fNumCores;
    //
    // public GDBCPUDMData(int num) {
    // fNumCores = num;
    // }
    //
    // @Override
    // public int getNumCores() { return fNumCores; }
    // }
    //
    // @Immutable
    // private static class GDBCoreDMData implements ICoreDMData {
    // final String fPhysicalId;
    //
    // public GDBCoreDMData(String id) {
    // fPhysicalId = id;
    // }
    //
    // @Override
    // public String getPhysicalId() { return fPhysicalId; }
    // }

    @Immutable
    private class GDBLoadInfo implements ILoadInfo {
        private int fILoad;
        private String fLoad;
        private Map<String, String> fDetailedLoad;

        public GDBLoadInfo(String load, Map<String, String> detailedLoad) {
            fLoad = load;
            fILoad = Integer.valueOf(load);
            fDetailedLoad = detailedLoad;
        }

        public GDBLoadInfo(int load) {
            this(String.valueOf(load), null);
            fILoad = load;
        }

        public int getILoad() {
            return fILoad;
        }

        @Override
        public String getLoad() {
            return fLoad;
        }

        @Override
        public Map<String, String> getDetailedLoad() {
            return fDetailedLoad;
        }
    }

    /**
     * @param session
     *            The DSF session for this service.
     * @param trace
     *            The trace associated to this service
     * @throws CoreException
     */
    public TraceHardwareAndOSService(@NonNull DsfSession session, @NonNull ITmfTrace trace) throws CoreException {
        super(session);
        fTrace = trace;

        // initialize cpu data source
        fCPUModule = TmfTraceUtils.getAnalysisModuleOfClass(fTrace, LttngKernelCpuUsageAnalysis.class, LttngKernelCpuUsageAnalysis.ID);
        if (fCPUModule == null) {
            // Notify of incorrect initialization
            throw new CoreException(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, INVALID_HANDLE, "Unable to resolve Cpu Usage Analysis module from trace: " + trace, null)); //$NON-NLS-1$
        }

        fCPUModule.schedule();
        fCPUModule.waitForInitialization();
        fStateSys = fCPUModule.getStateSystem();
        if (fStateSys == null) {
            // Notify of incorrect initialization
            throw new CoreException(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, INVALID_HANDLE, "Unable to resolve the State System from trace: " + trace, null)); //$NON-NLS-1$
        }
    }

    /**
     * This method initializes this service.
     *
     * @param requestMonitor
     *            The request monitor indicating the operation is finished
     */
    @Override
    public void initialize(final RequestMonitor requestMonitor) {
        super.initialize(new RequestMonitor(ImmediateExecutor.getInstance(), requestMonitor) {
            @Override
            protected void handleSuccess() {
                doInitialize(requestMonitor);
            }
        });
    }

    /**
     * This method initializes this service after our superclass's initialize()
     * method succeeds.
     *
     * @param requestMonitor
     *            The call-back object to notify when this service's
     *            initialization is done.
     */
    private void doInitialize(RequestMonitor requestMonitor) {
        // Register this service.
        register(new String[] { IGDBHardwareAndOS.class.getName(),
                IGDBHardwareAndOS2.class.getName(),
                TraceHardwareAndOSService.class.getName() },
                new Hashtable<String, String>());

        requestMonitor.done();
    }

    @Override
    public void shutdown(RequestMonitor requestMonitor) {
        unregister();
        super.shutdown(requestMonitor);
    }

    /**
     * @return The bundle context of the plug-in to which this service belongs.
     */
    @Override
    protected BundleContext getBundleContext() {
        return DsfTraceCorePlugin.getBundleContext();
    }

    @Override
    public void getCPUs(final IHardwareTargetDMContext dmc, final DataRequestMonitor<ICPUDMContext[]> rm) {
        // Assuming one CPU per trace for the time being
        if (fMapCPUToCores.keySet().size() > 0) {
            // CPU's already resolved for the associated trace
            ICPUDMContext[] cpus = fMapCPUToCores.keySet().toArray(new ICPUDMContext[fMapCPUToCores.size()]);
            rm.done(cpus);
            return;
        }

        // Need to resolve the associated CPU's
        // TODO: Trace compass does not seem to divide core and cpu today,
        // For now lets go with one CPU per HW target, and use the Trace compass
        // CPU's as cores
        ICPUDMContext cpuDmc = createCPUContext(dmc, "1"); //$NON-NLS-1$

        ICPUDMContext[] cpuDmcs = new ICPUDMContext[] { cpuDmc };
        System.out.println("returning " + cpuDmcs.length + " CPU");
        rm.done(cpuDmcs);
    }

    @Override
    public void getCores(IDMContext dmc, final DataRequestMonitor<ICoreDMContext[]> rm) {
        ICPUDMContext cpuDmc = DMContexts.getAncestorOfType(dmc, ICPUDMContext.class);

        // Not allowed to process a context with no cpu context to force well
        // formed contexts in any future calls
        if (cpuDmc == null) {
            rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, INVALID_HANDLE, "Initialization problem, No ICPUDMContext found in context: " + dmc, null)); //$NON-NLS-1$
            return;
        }

        // Check if the ICoreDMContexts exist in our cpu map
        ICoreDMContext[] coreDmcs = fMapCPUToCores.get(cpuDmc);
        if (coreDmcs != null) {
            // We have previously resolved the cores, so lets return them
            rm.done(coreDmcs);
            return;
        }

        // Not available, so get the cores from the trace state system and cache
        // them
        rm.done(resolveCoreContexts(cpuDmc));
    }

    @Override
    public void getExecutionData(IDMContext dmc, DataRequestMonitor<IDMData> rm) {
        if (dmc instanceof ICoreDMContext) {
            rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Not done yet", null)); //$NON-NLS-1$
        } else if (dmc instanceof ICPUDMContext) {
            rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Not done yet", null)); //$NON-NLS-1$
        } else {
            rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, INVALID_HANDLE, "Invalid DMC type", null)); //$NON-NLS-1$
        }
    }

    @Override
    public ICPUDMContext createCPUContext(IHardwareTargetDMContext targetDmc, String CPUId) {
        ICPUDMContext cpuDmc = new GDBCPUDMC(getSession().getId(), targetDmc, CPUId);
        // Lets leave the resolution of cores to its actual call
        fMapCPUToCores.put(cpuDmc, null);
        return cpuDmc;
    }

    private ICoreDMContext[] resolveCoreContexts(ICPUDMContext cpuDmc) {
        // For now treat a Trace CPU as Core
        try {
            int cpusNode = fStateSys.getQuarkAbsolute(Attributes.CPUS);
            List<Integer> cpuNodes = fStateSys.getSubAttributes(cpusNode, false);

            ICoreDMContext[] coreDmcs = new ICoreDMContext[cpuNodes.size()];
            int i = 0;
            for (Integer coreNode : cpuNodes) {
                String cpuName = fStateSys.getAttributeName(coreNode);
                coreDmcs[i] = createCoreContext(cpuDmc, coreNode, cpuName);
                i++;
                System.out.println("resolved core #" + cpuName);
            }

            fMapCPUToCores.put(cpuDmc, coreDmcs);
            // Normal scenario, returning contexts for each core
            System.out.println("resolved " + cpuNodes.size() + " Cores"); //$NON-NLS-1$ //$NON-NLS-2$

            return coreDmcs;
        } catch (AttributeNotFoundException e) {
            // No core attributes found
            return new ICoreDMContext[0];
        }
    }

    @Override
    public ICoreDMContext createCoreContext(ICPUDMContext cpuDmc, String coreId) {
        // FIXME: Not being used at the moment, but need to discuss and improve
        // the API as we need an additional element i.e. the coreNode as seen in
        // the private method below
        assert false;
        return null;
    }

    /**
     * @param cpuDmc
     *            - Parent cpu context
     * @param coreNode
     *            - Reference to the state system model
     * @param coreId
     *            - Id displayed by the UI
     * @return - The created core context
     */
    public ICoreDMContext createCoreContext(ICPUDMContext cpuDmc, Integer coreNode, String coreId) {
        return new GDBCoreDMC(getSession().getId(), cpuDmc, coreNode, coreId);
    }

    @Override
    public void flushCache(IDMContext context) {
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void getResourceClasses(IDMContext dmc,
            DataRequestMonitor<IResourceClass[]> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    @Override
    public void getResourcesInformation(IDMContext dmc, String resourceClassId,
            DataRequestMonitor<IResourcesInformation> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    @Override
    public void getLoadInfo(final IDMContext context, final DataRequestMonitor<ILoadInfo> rm) {
        if (context instanceof ICoreDMContext) {
            rm.done(getCoreLoadInfo((ICoreDMContext) context));
            return;
        }

        if (context instanceof ICPUDMContext) {
            // Resolve the load for given cpu context
            rm.done(getCPULoadInfo((ICPUDMContext) context));
            return;
        }

        // we only support getting the load for a CPU or a core
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, INVALID_HANDLE, "Load information not supported for this context type", null)); //$NON-NLS-1$
        return;

    }

    private ILoadInfo getCoreLoadInfo(ICoreDMContext coreDmc) {
        // TODO: Need to extend the API to provide the load of the current
        // selected time, this may
        long startTime = fStateSys.getStartTime();
        long endTime = fStateSys.getCurrentEndTime();
        double duration = endTime - startTime;

        // validate time ranges and prevent division by zero
        if (duration < 1) {
            assert false;
            throw new TimeRangeException();
        }

        // Validate context, and provide a handle to the internal class
        // implementation
        assert (coreDmc instanceof GDBCoreDMC);
        GDBCoreDMC context = (GDBCoreDMC) coreDmc;

        Integer[] coreNode = new Integer[] { context.getNode() };

        Map<String, Long> tidToCPUTime = fCPUModule.getCpuUsageInRange(coreNode, startTime, endTime);
        // The map from thread to time spent includes a grand total identified
        // with the key "total"
        double totCpuTime = tidToCPUTime.get("total"); //$NON-NLS-1$
        String cpuName = fStateSys.getAttributeName(context.getNode());
        // The idle time is represented by a thread id of zero over the
        // following key=value format "cpuname/threadId=value"
        double idleCpuTime = tidToCPUTime.get(cpuName + "/0"); //$NON-NLS-1$
        totCpuTime = totCpuTime - idleCpuTime;

        double loadPercent = (totCpuTime / duration) * 100;
        System.out.println("Core " + context.getId() + ", load: " + (int) loadPercent);

        return new GDBLoadInfo((int) loadPercent);
    }

    private ILoadInfo getCPULoadInfo(ICPUDMContext cpuDmc) {
        int loadInfo = 0;

        // Resolve the context for the cores
        ICoreDMContext[] coreDmcs = fMapCPUToCores.get(cpuDmc);
        if (coreDmcs == null) {
            coreDmcs = resolveCoreContexts(cpuDmc);
        }

        if (coreDmcs != null && coreDmcs.length > 0) {
            for (ICoreDMContext core : coreDmcs) {
                ILoadInfo coreLoadInfo = getCoreLoadInfo(core);
                assert (coreLoadInfo instanceof GDBLoadInfo);
                loadInfo += ((GDBLoadInfo) coreLoadInfo).getILoad();
            }

            // Take average load of all cores
            loadInfo = loadInfo / coreDmcs.length;
        }

        System.out.println("CPU load: " + loadInfo); //$NON-NLS-1$

        return new GDBLoadInfo(loadInfo);
    }

}
