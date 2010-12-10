/*
 * Copyright 2010 Brookhaven National Laboratory
 * All rights reserved. Use is subject to license terms.
 */

package org.epics.pvmanager.data;

import java.text.NumberFormat;
import java.util.List;
import org.epics.pvmanager.TimeStamp;

/**
 *
 * @author carcassi
 */
public class IVIntArray extends IVNumeric implements VIntArray {

    private final int[] array;
    private final List<Integer> sizes;

    public IVIntArray(int[] array, List<Integer> sizes,
            AlarmSeverity alarmSeverity, AlarmStatus alarmStatus,
            TimeStamp timeStamp, Integer timeUserTag, Double lowerDisplayLimit,
            Double lowerCtrlLimit, Double lowerAlarmLimit, Double lowerWarningLimit,
            String units, NumberFormat format, Double upperWarningLimit, Double upperAlarmLimit,
            Double upperCtrlLimit, Double upperDisplayLimit) {
        super(alarmSeverity, alarmStatus, timeStamp, timeUserTag, lowerDisplayLimit,
                lowerCtrlLimit, lowerAlarmLimit, lowerWarningLimit, units, format, upperWarningLimit,
                upperAlarmLimit, upperCtrlLimit, upperDisplayLimit);
        this.array = array;
        this.sizes = sizes;
    }

    @Override
    public int[] getArray() {
        return array;
    }

    @Override
    public List<Integer> getSizes() {
        return sizes;
    }

}