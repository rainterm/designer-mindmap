package com.designer.admin.system.service.monitor.impl;

import com.designer.admin.system.constant.SysMonitorConsts;
import com.designer.admin.system.model.vo.monitor.CpuInfoVO;
import com.designer.admin.system.model.vo.monitor.DiskInfoVO;
import com.designer.admin.system.model.vo.monitor.JvmInfoVO;
import com.designer.admin.system.model.vo.monitor.MemInfoVO;
import com.designer.admin.system.model.vo.monitor.ServerInfoVO;
import com.designer.admin.system.model.vo.monitor.SysInfoVO;
import com.designer.admin.system.service.monitor.MonitorService;
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
import java.util.List;
import java.util.Properties;

/**
 * 服务监控：CPU / 内存 / JVM / 磁盘
 *
 * @author 雨夜终端
 */
@Component
public class MonitorServiceImpl implements MonitorService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    @Override
    public ServerInfoVO getServerInfo() {
        return ServerInfoVO.builder()
                .cpu(cpuInfo())
                .mem(memInfo())
                .jvm(jvmInfo())
                .sys(sysInfo())
                .disks(diskInfo())
                .time(LocalDateTime.now().format(DT))
                .build();
    }

    private CpuInfoVO cpuInfo() {
        java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();

        double systemCpu = -1;
        double processCpu = -1;
        if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
            // JDK 14 起统一用 getCpuLoad()，不可用时同样返回负值，无需再退回已过时的 getSystemCpuLoad()
            systemCpu = sun.getCpuLoad();
            processCpu = sun.getProcessCpuLoad();
        }
        return CpuInfoVO.builder()
                .name(os.getName())
                .arch(os.getArch())
                .availableProcessors(os.getAvailableProcessors())
                .systemLoadAverage(round(os.getSystemLoadAverage()))
                .systemUsage(percentOrNull(systemCpu))
                .processUsage(percentOrNull(processCpu))
                .build();
    }

    private MemInfoVO memInfo() {
        java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        long total = 0;
        long free = 0;
        if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
            total = sun.getTotalMemorySize();
            free = sun.getFreeMemorySize();
        }
        long used = Math.max(total - free, 0);
        return MemInfoVO.builder()
                .total(total)
                .used(used)
                .free(free)
                .totalHuman(formatBytes(total))
                .usedHuman(formatBytes(used))
                .freeHuman(formatBytes(free))
                .usage(total > 0 ? round(used * 100.0 / total) : 0)
                .build();
    }

    private JvmInfoVO jvmInfo() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();
        double heapUsage = heap.getMax() > 0
                ? round(heap.getUsed() * 100.0 / heap.getMax())
                : round(heap.getUsed() * 100.0 / Math.max(heap.getCommitted(), 1));
        return JvmInfoVO.builder()
                .name(runtime.getVmName())
                .version(runtime.getVmVersion())
                .vendor(runtime.getVmVendor())
                .javaVersion(System.getProperty("java.version"))
                .startTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(runtime.getStartTime()),
                        ZoneId.systemDefault()).format(DT))
                .runTime(formatDuration(Duration.ofMillis(runtime.getUptime())))
                .inputArgs(String.join(" ", runtime.getInputArguments()))
                .heapTotal(heap.getCommitted())
                .heapUsed(heap.getUsed())
                .heapMax(heap.getMax())
                .heapTotalHuman(formatBytes(heap.getCommitted()))
                .heapUsedHuman(formatBytes(heap.getUsed()))
                .heapMaxHuman(formatBytes(heap.getMax() < 0 ? 0 : heap.getMax()))
                .heapUsage(heapUsage)
                .nonHeapUsed(nonHeap.getUsed())
                .nonHeapUsedHuman(formatBytes(nonHeap.getUsed()))
                .threadCount(thread.getThreadCount())
                .peakThreadCount(thread.getPeakThreadCount())
                .build();
    }

    private SysInfoVO sysInfo() {
        Properties props = System.getProperties();
        String hostName;
        String hostIp;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostName = addr.getHostName();
            hostIp = addr.getHostAddress();
        } catch (Exception e) {
            hostName = "-";
            hostIp = "-";
        }
        return SysInfoVO.builder()
                .osName(props.getProperty("os.name"))
                .osArch(props.getProperty("os.arch"))
                .osVersion(props.getProperty("os.version"))
                .userDir(props.getProperty("user.dir"))
                .userName(props.getProperty("user.name"))
                .hostName(hostName)
                .hostIp(hostIp)
                .appName(Solon.cfg().appName())
                .solonVersion(Solon.version())
                .build();
    }

    private List<DiskInfoVO> diskInfo() {
        List<DiskInfoVO> list = new ArrayList<>();
        for (File root : File.listRoots()) {
            long total = root.getTotalSpace();
            long free = root.getUsableSpace();
            long used = Math.max(total - free, 0);
            if (total <= 0) {
                continue;
            }
            list.add(DiskInfoVO.builder()
                    .path(root.getAbsolutePath())
                    .total(total)
                    .used(used)
                    .free(free)
                    .totalHuman(formatBytes(total))
                    .usedHuman(formatBytes(used))
                    .freeHuman(formatBytes(free))
                    .usage(round(used * 100.0 / total))
                    .build());
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
        if (bytes < SysMonitorConsts.BYTES_PER_KB) {
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
