package com.tencent.matrix.batterycanary.monitor;

import com.tencent.matrix.AppActiveMatrixDelegate;
import com.tencent.matrix.batterycanary.monitor.feature.JiffiesMonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.LooperTaskMonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.MonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.WakeLockMonitorFeature;
import com.tencent.matrix.util.MatrixLog;

import java.util.List;

public class BatteryMonitorCore implements JiffiesMonitorFeature.JiffiesListener, LooperTaskMonitorFeature.LooperTaskListener,
        WakeLockMonitorFeature.WakeLockListener {
    private static final String TAG = "Matrix.monitor.core";

    private volatile boolean isTurnOn = false;
    private boolean isAppForeground = AppActiveMatrixDelegate.INSTANCE.isAppForeground();
    private final BatteryMonitorConfig config;

    public BatteryMonitorCore(BatteryMonitorConfig config) {
        this.config = config;
        if (config.callback instanceof BatteryMonitorCallback.BatteryPrinter) ((BatteryMonitorCallback.BatteryPrinter) config.callback).attach(this);
        for (MonitorFeature plugin : config.features) {
            plugin.configure(this);
        }
    }

    public <T extends MonitorFeature> T getMonitorFeature(Class<T> clazz) {
        for (MonitorFeature plugin : config.features) {
            if (clazz.isAssignableFrom(plugin.getClass())) {
                //noinspection unchecked
                return (T) plugin;
            }
        }
        return null;
    }

    public BatteryMonitorConfig getConfig() {
        return config;
    }

    public boolean isTurnOn() {
        synchronized (BatteryMonitorCore.class) {
            return isTurnOn;
        }
    }

    public void start() {
        synchronized (BatteryMonitorCore.class) {
            if (!isTurnOn) {
                for (MonitorFeature plugin : config.features) {
                    plugin.onTurnOn();
                }
                isTurnOn = true;
            }
        }
    }

    public void stop() {
        synchronized (BatteryMonitorCore.class) {
            if (isTurnOn) {
                for (MonitorFeature plugin : config.features) {
                    plugin.onTurnOff();
                }
                isTurnOn = false;
            }
        }
    }

    public void onForeground(boolean isForeground) {
        isAppForeground = isForeground;
        for (MonitorFeature plugin : config.features) {
            plugin.onForeground(isForeground);
        }
    }

    public boolean isForeground() {
        return isAppForeground;
    }

    @Override
    public void onTraceBegin() {
        MatrixLog.d(TAG, "#onTraceBegin");
        getConfig().callback.onTraceBegin();
    }

    @Override
    public void onTraceEnd() {
        MatrixLog.d(TAG, "#onTraceEnd");
        getConfig().callback.onTraceEnd();
    }

    @Override
    public void onJiffies(JiffiesMonitorFeature.JiffiesResult result) {
        MatrixLog.d(TAG, "#onJiffies, diff = " + result.totalJiffiesDiff);
        getConfig().callback.onJiffies(result);
    }

    @Override
    public void onTaskTrace(Thread thread, List<LooperTaskMonitorFeature.TaskTraceInfo> sortList) {
        MatrixLog.d(TAG, "#onTaskTrace, thread = " + thread.getName());
        getConfig().callback.onTaskTrace(thread, sortList);
    }

    @Override
    public void onWakeLockTimeout(String tag, String packageName, int warningCount) {
        MatrixLog.d(TAG, "#onWakeLockTimeout, tag = " + tag + ", pkg = " + packageName + ", count = " + warningCount);
        getConfig().callback.onWakeLockTimeout(tag, packageName, warningCount);
    }
}
