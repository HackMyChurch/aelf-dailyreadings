package co.epitre.aelf_lectures.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.RandomAccessFile;
import java.util.Locale;

public class HardwareDetection {
    private static final String TAG = "HardwareDetection";
    private static HardwareDetection.PerformanceClass performanceClass = PerformanceClass.UNKNOWN;

    // From https://github.com/DrKLO/Telegram/blob/master/TMessagesProj/src/main/java/org/telegram/messenger/SharedConfig.java#L1554-L1556
    public enum PerformanceClass {
        UNKNOWN,
        LOW,
        AVERAGE,
        HIGH
    }

    // From https://github.com/DrKLO/Telegram/blob/5bc1c3dce0e9108615c784a565051e54246fe0cb/TMessagesProj/src/main/java/org/telegram/messenger/SharedConfig.java#L346-L358
    private static final int[] LOW_SOC = {
            -1775228513, // EXYNOS 850
            802464304,  // EXYNOS 7872
            802464333,  // EXYNOS 7880
            802464302,  // EXYNOS 7870
            2067362118, // MSM8953
            2067362060, // MSM8937
            2067362084, // MSM8940
            2067362241, // MSM8992
            2067362117, // MSM8952
            2067361998, // MSM8917
            -1853602818 // SDM439
    };

    public static PerformanceClass getGuessedPerformanceClass(Context ctx) {
        if (performanceClass.equals(PerformanceClass.UNKNOWN)) {
            performanceClass = guessDevicePerformanceClass(ctx);
        }

        return performanceClass;
    }

    // From https://github.com/DrKLO/Telegram/blob/5bc1c3dce0e9108615c784a565051e54246fe0cb/TMessagesProj/src/main/java/org/telegram/messenger/SharedConfig.java#L1577-L1639
    private static PerformanceClass guessDevicePerformanceClass(Context ctx) {
        // Fast path: exclude known low-end SOC immediately
        if (isLowEndSoc()) {
            return PerformanceClass.LOW;
        }

        // Guess performance class from device characteristics
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        int androidVersion = Build.VERSION.SDK_INT;
        int memoryClass = activityManager.getMemoryClass();
        int cpuCount = Runtime.getRuntime().availableProcessors();
        int maxCpuFreq = getMaxCpuFreq();
        long totalRam = getTotalRam(activityManager);

        PerformanceClass performanceClass;
        if (
                androidVersion < 21 || cpuCount <= 2 || memoryClass <= 100 ||
                cpuCount <= 4 && maxCpuFreq != -1 && maxCpuFreq <= 1250 ||
                cpuCount <= 4 && maxCpuFreq <= 1600 && memoryClass <= 128 && androidVersion <= 21 ||
                cpuCount <= 4 && maxCpuFreq <= 1300 && memoryClass <= 128 && androidVersion <= 24 ||
                totalRam != -1 && totalRam < 2L * 1024L * 1024L * 1024L
        ) {
            performanceClass = PerformanceClass.LOW;
        } else if (
                cpuCount < 8 || memoryClass <= 160 ||
                maxCpuFreq != -1 && maxCpuFreq <= 2055 ||
                maxCpuFreq == -1 && cpuCount == 8 && androidVersion <= 23
        ) {
            performanceClass = PerformanceClass.AVERAGE;
        } else {
            performanceClass = PerformanceClass.HIGH;
        }

        Log.i(TAG, "Selected performance class = " + performanceClass + " (cpu_count = " + cpuCount + ", freq = " + maxCpuFreq + ", memoryClass = " + memoryClass + ", android version " + androidVersion + ", manufacturer " + Build.MANUFACTURER + ")");
        return performanceClass;
    }

    private static boolean isLowEndSoc() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int hash = Build.SOC_MODEL.toUpperCase().hashCode();
            for (int j : LOW_SOC) {
                if (j == hash) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int getMaxCpuFreq() {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        int totalCpuFreq = 0;
        int freqResolved = 0;
        for (int i = 0; i < cpuCount; i++) {
            try {
                RandomAccessFile reader = new RandomAccessFile(String.format(Locale.ENGLISH, "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq", i), "r");
                String line = reader.readLine();
                if (line != null) {
                    totalCpuFreq += Integer.parseInt(line) / 1000;
                    freqResolved++;
                }
                reader.close();
            } catch (Throwable ignore) {}
        }

        return freqResolved == 0 ? -1 : (int) Math.ceil(totalCpuFreq / (float) freqResolved);
    }

    private static long getTotalRam(ActivityManager activityManager) {
        try {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.totalMem;
        } catch (Exception ignore) {}

        return -1;
    }
}
