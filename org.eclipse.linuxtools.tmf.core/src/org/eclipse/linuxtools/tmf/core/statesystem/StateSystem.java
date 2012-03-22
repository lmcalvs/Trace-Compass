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

package org.eclipse.linuxtools.tmf.core.statesystem;

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.linuxtools.tmf.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.tmf.core.statevalue.StateValueTypeException;
import org.eclipse.linuxtools.tmf.core.statevalue.TmfStateValue;

/**
 * This is the base class for the StateHistorySystem. It contains all the
 * current-state-updating methods.
 * 
 * It's not abstract, as it can be used by itself: in this case, no History tree
 * will be built underneath (no information will be saved to disk) and it will
 * only be able to respond to queries to the current, latest time.
 * 
 * @author alexmont
 * 
 */
public class StateSystem {

    /* References to the inner structures */
    protected AttributeTree attributeTree;
    protected TransientState transState;

    /**
     * Constructor. No configuration needed!
     */
    public StateSystem() {
        attributeTree = new AttributeTree(this);

        /* This will tell the builder to discard the intervals */
        transState = new TransientState(null);
    }

    /**
     * @name Quark-retrieving methods
     */

    /**
     * Basic quark-retrieving method. Pass an attribute in parameter as an array
     * of strings, the matching quark will be returned.
     * 
     * This version will NOT create any new attributes. If an invalid attribute
     * is requested, an exception will be thrown. This should ideally be used
     * for doing read-only operations on the system, like queries for example.
     * 
     * @param attribute
     *            Attribute given as its full path in the Attribute Tree
     * @return The quark of the requested attribute, if it existed.
     * @throws AttributeNotFoundException
     *             This exception is thrown if the requested attribute simply
     *             did not exist in the system.
     */
    public int getQuarkAbsolute(String... attribute)
            throws AttributeNotFoundException {
        return attributeTree.getQuarkDontAdd(-1, attribute);
    }

    /**
     * Basic quark-retrieving method. Pass an attribute in parameter as an array
     * of strings, the matching quark will be returned.
     * 
     * This version WILL create new attributes: if the attribute passed in
     * parameter is new in the system, it will be added and its new quark will
     * be returned.
     * 
     * @param attribute
     *            Attribute given as its full path in the Attribute Tree
     * @return The quark of the attribute (which either existed or just got
     *         created)
     */
    public int getQuarkAbsoluteAndAdd(String... attribute) {
        return attributeTree.getQuarkAndAdd(-1, attribute);
    }

    /**
     * "Relative path" quark-getting method. Instead of specifying a full path,
     * if you know the path is relative to another attribute for which you
     * already have the quark, use this for better performance.
     * 
     * This is useful for cases where a lot of modifications or queries will
     * originate from the same branch of the attribute tree : the common part of
     * the path won't have to be re-hashed for every access.
     * 
     * This version will NOT create any new attributes. If an invalid attribute
     * is requested, an exception will be thrown. This should ideally be used
     * for doing read-only operations on the system, like queries for example.
     * 
     * @param startingNodeQuark
     *            The quark of the attribute from which 'subPath' originates.
     * @param subPath
     *            "Rest" of the path to get to the final attribute
     * @return The matching quark, if it existed
     * @throws AttributeNotFoundException
     */
    public int getQuarkRelative(int startingNodeQuark, String... subPath)
            throws AttributeNotFoundException {
        return attributeTree.getQuarkDontAdd(startingNodeQuark, subPath);
    }

    /**
     * "Relative path" quark-getting method. Instead of specifying a full path,
     * if you know the path is relative to another attribute for which you
     * already have the quark, use this for better performance.
     * 
     * This is useful for cases where a lot of modifications or queries will
     * originate from the same branch of the attribute tree : the common part of
     * the path won't have to be re-hashed for every access.
     * 
     * This version WILL create new attributes: if the attribute passed in
     * parameter is new in the system, it will be added and its new quark will
     * be returned.
     * 
     * @param startingNodeQuark
     *            The quark of the attribute from which 'subPath' originates.
     * @param subPath
     *            "Rest" of the path to get to the final attribute
     * @return The matching quark, either if it's new of just got created.
     */
    public int getQuarkRelativeAndAdd(int startingNodeQuark, String... subPath) {
        return attributeTree.getQuarkAndAdd(startingNodeQuark, subPath);
    }

    /**
     * @name External methods related to insertions in the history -
     */

    /**
     * Basic attribute modification method, we simply specify a new value, for a
     * given attribute, effective at the given timestamp.
     * 
     * @param t
     *            Timestamp of the state change
     * @param value
     *            The State Value we want to assign to the attribute
     * @param attributeQuark
     *            Integer value of the quark corresponding to the attribute we
     *            want to modify
     * @throws TimeRangeException
     *             If the requested time is outside of the trace's range
     * @throws AttributeNotFoundException
     *             If the requested attribute quark is invalid
     */
    public void modifyAttribute(long t, ITmfStateValue value, int attributeQuark)
            throws TimeRangeException, AttributeNotFoundException {
        transState.processStateChange(t, value, attributeQuark);
    }

    /**
     * Increment attribute method. Reads the current value of a given integer
     * attribute (this value is right now in the Transient State), and increment
     * it by 1. Useful for statistics.
     * 
     * @param t
     *            Timestamp of the state change
     * @param attributeQuark
     *            Attribute to increment. If it doesn't exist it will be added,
     *            with a new value of 1.
     * @throws StateValueTypeException
     *             If the attribute already exists but is not of type Integer
     * @throws TimeRangeException
     *             If the given timestamp is invalid
     * @throws AttributeNotFoundException
     *             If the quark is invalid
     */
    public void incrementAttribute(long t, int attributeQuark)
            throws StateValueTypeException, TimeRangeException,
            AttributeNotFoundException {
        int prevValue = queryOngoingState(attributeQuark).unboxInt();
        /* prevValue should be == 0 if the attribute wasn't existing before */
        modifyAttribute(t, TmfStateValue.newValueInt(prevValue + 1),
                attributeQuark);
    }

    /**
     * "Push" helper method. This uses the given integer attribute as a stack:
     * The value of that attribute will represent the stack depth (always >= 1).
     * Sub-attributes will be created, their base-name will be the position in
     * the stack (1, 2, etc.) and their value will be the state value 'value'
     * that was pushed to this position.
     * 
     * @param t
     *            Timestamp of the state change
     * @param value
     *            State value to assign to this stack position.
     * @param attributeQuark
     *            The base attribute to use as a stack. If it does not exist if
     *            will be created (with depth = 1)
     * @throws TimeRangeException
     *             If the requested timestamp is invalid
     * @throws AttributeNotFoundException
     *             If the attribute is invalid
     * @throws StateValueTypeException
     *             If the attribute 'attributeQuark' already exists, but is not
     *             of integer type.
     */
    public void pushAttribute(long t, ITmfStateValue value, int attributeQuark)
            throws TimeRangeException, AttributeNotFoundException,
            StateValueTypeException {
        assert (attributeQuark >= 0);
        Integer stackDepth = 0;
        int subAttributeQuark;
        ITmfStateValue previousSV = transState.getOngoingStateValue(attributeQuark);

        if (previousSV.isNull()) {
            /*
             * If the StateValue was null, this means this is the first time we
             * use this attribute. Leave stackDepth at 0.
             */
        } else if (previousSV.getType() == 0) {
            /* Previous value was an integer, all is good, use it */
            stackDepth = previousSV.unboxInt();
            if (stackDepth >= 10) {
                /*
                 * Limit stackDepth to 10, to avoid having Attribute Trees grow
                 * out of control due to buggy insertions
                 */
                assert (false);
            }
        } else {
            /* Previous state of this attribute was another type? Not good! */
            assert (false);
        }

        stackDepth++;
        subAttributeQuark = getQuarkRelativeAndAdd(attributeQuark,
                stackDepth.toString());

        modifyAttribute(t, TmfStateValue.newValueInt(stackDepth),
                attributeQuark);
        transState.processStateChange(t, value, subAttributeQuark);
    }

    /**
     * Antagonist of the pushAttribute(), pops the top-most attribute on the
     * stack-attribute. If this brings it back to depth = 0, the attribute is
     * kept with depth = 0. If the value is already 0, or if the attribute
     * doesn't exist, nothing is done.
     * 
     * @param t
     *            Timestamp of the state change
     * @param attributeQuark
     *            Quark of the stack-attribute to pop
     * @throws AttributeNotFoundException
     *             If the attribute is invalid
     * @throws TimeRangeException
     *             If the timestamp is invalid
     * @throws StateValueTypeException
     *             If the target attribute already exists, but its state value
     *             type is invalid (not an integer)
     */
    public void popAttribute(long t, int attributeQuark)
            throws AttributeNotFoundException, TimeRangeException,
            StateValueTypeException {
        assert (attributeQuark >= 0);
        Integer stackDepth;
        int subAttributeQuark;
        ITmfStateValue previousSV = transState.getOngoingStateValue(attributeQuark);

        if (previousSV.isNull()) {
            /*
             * If the StateValue was null, this means this is the first time we
             * use this attribute.
             */
            stackDepth = 0;
        } else {
            assert (previousSV.getType() == 0);
            stackDepth = previousSV.unboxInt();
        }

        if (stackDepth == 0) {
            /*
             * Trying to pop an empty stack. This often happens at the start of
             * traces, for example when we see a syscall_exit, without having
             * the corresponding syscall_entry in the trace. Just ignore
             * silently.
             */
            return;
        } else if (stackDepth < 0) {
            /* This on the other hand should not happen... */
            assert (false);
        }

        /* The attribute should already exist... */
        subAttributeQuark = getQuarkRelative(attributeQuark,
                stackDepth.toString());

        stackDepth--;
        modifyAttribute(t, TmfStateValue.newValueInt(stackDepth),
                attributeQuark);
        removeAttribute(t, subAttributeQuark);
    }

    /**
     * Remove attribute method. Similar to the above modify- methods, with value
     * = 0 / null, except we will also "nullify" all the sub-contents of the
     * requested path (a bit like "rm -rf")
     * 
     * @param t
     *            Timestamp of the state change
     * @param attributeQuark
     *            Attribute to remove
     * @throws TimeRangeException
     *             If the timestamp is invalid
     * @throws AttributeNotFoundException
     *             If the quark is invalid
     */
    public void removeAttribute(long t, int attributeQuark)
            throws TimeRangeException, AttributeNotFoundException {
        assert (attributeQuark >= 0);
        /* "Nullify our children first, recursively */
        List<Integer> childAttributes = attributeTree.getSubAttributes(attributeQuark);
        for (Integer childNodeQuark : childAttributes) {
            assert (attributeQuark != childNodeQuark);
            removeAttribute(t, childNodeQuark);
        }
        /* Nullify ourselves */
        transState.processStateChange(t, TmfStateValue.nullValue(),
                attributeQuark);
    }

    /**
     * @name "Current" query/update methods -
     */

    /**
     * Returns the current state value we have (in the Transient State) for the
     * given attribute.
     * 
     * This is useful even for a StateHistorySystem, as we are guaranteed it
     * will only do a memory access and not go look on disk (and we don't even
     * have to provide a timestamp!)
     * 
     * @param attributeQuark
     *            For which attribute we want the current state
     * @return The State value that's "current" for this attribute
     * @throws AttributeNotFoundException
     *             If the requested attribute is invalid
     */
    public ITmfStateValue queryOngoingState(int attributeQuark)
            throws AttributeNotFoundException {
        return transState.getOngoingStateValue(attributeQuark);
    }

    /**
     * Modify a current "ongoing" state (instead of inserting a state change,
     * like modifyAttribute() and others).
     * 
     * This can be used to update the value of a previous state change, for
     * example when we get information at the end of the state and not at the
     * beginning. (return values of system calls, etc.)
     * 
     * Note that past states can only be modified while they are still in
     * memory, so only the "current state" can be updated. Once they get
     * committed to disk (by inserting a new state change) it becomes too late.
     * 
     * @param newValue
     *            The new value that will overwrite the "current" one.
     * @param attributeQuark
     *            For which attribute in the system
     * @throws AttributeNotFoundException
     *             If the requested attribute is invalid
     */
    public void updateOngoingState(ITmfStateValue newValue, int attributeQuark)
            throws AttributeNotFoundException {
        transState.changeOngoingStateValue(attributeQuark, newValue);
    }

    /**
     * @name Debugging methods
     */

    /**
     * This returns the slash-separated path of an attribute by providing its
     * quark
     * 
     * @param attributeQuark
     *            The quark of the attribute we want
     * @return One single string separated with '/', like a filesystem path
     */
    public String getFullAttributePath(int attributeQuark) {
        return attributeTree.getFullAttributeName(attributeQuark);
    }

    /**
     * Print out the contents of the inner structures.
     * 
     * @param writer
     *            The PrintWriter in which to print the output
     */
    public void debugPrint(PrintWriter writer) {
        attributeTree.debugPrint(writer);
        transState.debugPrint(writer);
    }

}