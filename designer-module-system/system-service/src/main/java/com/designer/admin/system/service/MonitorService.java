package com.designer.admin.system.service;

import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 服务监控：CPU / 内存 / JVM / 磁盘
 */
@Component
public class MonitorService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    public Map<String, Object> serverInfo() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cpu", cpuInfo());
        data.put("mem", memInfo());
        data.put("jvm", jvmInfo());
        data.put("sys", sysInfo());
        data.put("disks", diskInfo());
        data.put("time", LocalDateTime.now().format(DT));
        return data;
    }

    private Map<String, Object> cpuInfo() {
        Map<String, Object> cpu = new LinkedHashMap<>();
        java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        cpu.put("name", os.getName());
        cpu.put("arch", os.getArch());
        cpu.put("availableProcessors", os.getAvailableProcessors());
        cpu.put("systemLoadAverage", round(os.getSystemLoadAverage()));

        double systemCpu = -1;
        double processCpu = -1;
        if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
            systemCpu = sun.getCpuLoad();
            processCpu = sun.getProcessCpuLoad();
            if (systemCpu < 0) {
                systemCpu = sun.getSystemCpuLoad();
            }
        }
        cpu.put("systemUsage", percentOrNull(systemCpu));
        cpu.put("processUsage", percentOrNull(processCpu));
        return cpu;
    }

    private Map<String, Object> memInfo() {
        Map<String, Object> mem = new LinkedHashMap<>();
        java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        long total = 0;
        long free = 0;
        if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
            total = sun.getTotalMemorySize();
            free = sun.getFreeMemorySize();
        }
        long used = Math.max(total - free, 0);
        mem.put("total", total);
        mem.put("used", used);
        mem.put("free", free);
        mem.put("totalHuman", formatBytes(total));
        mem.put("usedHuman", formatBytes(used));
        mem.put("freeHuman", formatBytes(free));
        mem.put("usage", total > 0 ? round(used * 100.0 / total) : 0);
        return mem;
    }

    private Map<String, Object> jvmInfo() {
        Map<String, Object> jvm = new LinkedHashMap<>();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();

        jvm.put("name", runtime.getVmName());
        jvm.put("version", runtime.getVmVersion());
        jvm.put("vendor", runtime.getVmVendor());
        jvm.put("javaVersion", System.getProperty("java.version"));
        jvm.put("startTime", LocalDateTime.ofInstant(Instant.ofEpochMilli(runtime.getStartTime()), ZoneId.systemDefault()).format(DT));
        jvm.put("runTime", formatDuration(Duration.ofMillis(runtime.getUptime())));
        jvm.put("inputArgs", String.join(" ", runtime.getInputArguments()));
        jvm.put("heapTotal", heap.getCommitted());
        jvm.put("heapUsed", heap.getUsed());
        jvm.put("heapMax", heap.getMax());
        jvm.put("heapTotalHuman", formatBytes(heap.getCommitted()));
        jvm.put("heapUsedHuman", formatBytes(heap.getUsed()));
        jvm.put("heapMaxHuman", formatBytes(heap.getMax() < 0 ? 0 : heap.getMax()));
        jvm.put("heapUsage", heap.getMax() > 0 ? round(heap.getUsed() * 100.0 / heap.getMax()) : round(heap.getUsed() * 100.0 / Math.max(heap.getCommitted(), 1)));
        jvm.put("nonHeapUsed", nonHeap.getUsed());
        jvm.put("nonHeapUsedHuman", formatBytes(nonHeap.getUsed()));
        jvm.put("threadCount", thread.getThreadCount());
        jvm.put("peakThreadCount", thread.getPeakThreadCount());
        return jvm;
    }

    private Map<String, Object> sysInfo() {
        Map<String, Object> sys = new LinkedHashMap<>();
        Properties props = System.getProperties();
        sys.put("osName", props.getProperty("os.name"));
        sys.put("osArch", props.getProperty("os.arch"));
        sys.put("osVersion", props.getProperty("os.version"));
        sys.put("userDir", props.getProperty("user.dir"));
        sys.put("userName", props.getProperty("user.name"));
        try {
            InetAddress addr = InetAddress.getLocalHost();
            sys.put("hostName", addr.getHostName());
            sys.put("hostIp", addr.getHostAddress());
        } catch (Exception e) {
            sys.put("hostName", "-");
            sys.put("hostIp", "-");
        }
        sys.put("appName", Solon.cfg().appName());
        sys.put("solonVersion", Solon.version());
        return sys;
    }

    private List<Map<String, Object>> diskInfo() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (File root : File.listRoots()) {
            long total = root.getTotalSpace();
            long free = root.getUsableSpace();
            long used = Math.max(total - free, 0);
            if (total <= 0) {
                continue;
            }
            Map<String, Object> disk = new LinkedHashMap<>();
            disk.put("path", root.getAbsolutePath());
            disk.put("total", total);
            disk.put("used", used);
            disk.put("free", free);
            disk.put("totalHuman", formatBytes(total));
            disk.put("usedHuman", formatBytes(used));
            disk.put("freeHuman", formatBytes(free));
            disk.put("usage", round(used * 100.0 / total));
            list.add(disk);
        }
        return list;
    }

    private static Double percentOrNull(double ratio) {
        if (ratio < 0) {
            return null;
        }
        return round(ratio * 100.0);
    }

    private static double round(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0;
        }
        return Math.round(v * 100.0) / 100.0;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double v = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int i = -1;
        do {
            v /= 1024;
            i++;
        } while (v >= 1024 && i < units.length - 1);
        return DF.format(v) + " " + units[i];
    }

    private static String formatDuration(Duration d) {
        long days = d.toDays();
        long hours = d.toHours() % 24;
        long minutes = d.toMinutes() % 60;
        long seconds = d.getSeconds() % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("小时");
        }
        sb.append(minutes).append("分").append(seconds).append("秒");
        return sb.toString();
    }
}
