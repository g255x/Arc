package hoprc.api.utils.win;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ImeUtils {

    public interface TsfNative extends Library {
        int  TSF_Init();
        void TSF_Cleanup();
        int  TSF_GetComposition(Pointer buffer, int maxLen);
        int  TSF_GetCandidateCount();
        int  TSF_GetCandidate(int index, Pointer buffer, int maxLen);
    }
    public interface Win32API extends Library {
        Win32API IMM32 = Native.load("imm32", Win32API.class);
        Win32API USER32 = Native.load("user32", Win32API.class);

        Pointer GetForegroundWindow();
        Pointer ImmGetContext(Pointer hWnd);
        Pointer ImmAssociateContext(Pointer hWnd, Pointer hIMC);
        boolean ImmReleaseContext(Pointer hWnd, Pointer hIMC);
    }

    public static class ImeData {
        public String       composition = "";
        public List<String> candidates  = new ArrayList<>();
    }

    private static TsfNative        TSF_INSTANCE  = null;
    private static volatile boolean isInitialized = false;

    private static final Memory SHARED_BUFFER = new Memory(2048);
    private static Pointer savedHIMC = null;
    private static boolean isImeEnabled = true;

    static {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win") && CpuFeatures.isAvx2Supported()) {
            try {
                String res = "/assets/alienclient/win/TsfImeFix.dll";
                InputStream is = ImeUtils.class.getResourceAsStream(res);
                if (is != null) {
                    File tmp = File.createTempFile("TsfImeFix_", ".dll");
                    tmp.deleteOnExit();
                    Files.copy(is, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    is.close();
                    TSF_INSTANCE = Native.load(tmp.getAbsolutePath(), TsfNative.class);
                }
            } catch (Throwable t) {
                System.err.println("[CNInputFix] DLL load failed: " + t.getMessage());
            }
        }
    }

    public static void startImeMonitor() {
        if (TSF_INSTANCE == null || isInitialized) return;
        try {
            isInitialized = (TSF_INSTANCE.TSF_Init() != 0);
        } catch (Throwable t) {
            System.err.println("[CNInputFix] TSF_Init failed: " + t.getMessage());
        }
    }

    public static void stopImeMonitor() {
        if (TSF_INSTANCE == null || !isInitialized) return;
        try {
            TSF_INSTANCE.TSF_Cleanup();
            isInitialized = false;
        } catch (Throwable t) {
            System.err.println("[CNInputFix] TSF_Cleanup failed: " + t.getMessage());
        }
    }
    public static void setImeAllowed(boolean allowed) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        if (allowed == isImeEnabled) return;

        try {
            Pointer hwnd = Win32API.USER32.GetForegroundWindow();
            if (hwnd == null) return;

            if (!allowed) {
                Pointer currentHIMC = Win32API.IMM32.ImmGetContext(hwnd);
                if (currentHIMC != null) {
                    savedHIMC = currentHIMC;
                    Win32API.IMM32.ImmReleaseContext(hwnd, currentHIMC);
                }
                Win32API.IMM32.ImmAssociateContext(hwnd, Pointer.NULL);
                isImeEnabled = false;
            } else {
                if (savedHIMC != null) {
                    Win32API.IMM32.ImmAssociateContext(hwnd, savedHIMC);
                }
                isImeEnabled = true;
            }
        } catch (Throwable t) {
            System.err.println("[CNInputFix] Failed to change IME state: " + t.getMessage());
        }
    }

    public static ImeData getImeData() {
        ImeData data = new ImeData();
        if (!isInitialized || TSF_INSTANCE == null) return data;

        try {
            int compLen = TSF_INSTANCE.TSF_GetComposition(SHARED_BUFFER, 1024);
            if (compLen > 0) {
                data.composition = new String(SHARED_BUFFER.getCharArray(0, compLen));
            }

            int count = TSF_INSTANCE.TSF_GetCandidateCount();
            int loopCount = Math.min(count, 9);
            for (int i = 0; i < loopCount; i++) {
                int len = TSF_INSTANCE.TSF_GetCandidate(i, SHARED_BUFFER, 1024);
                if (len > 0) {
                    String word = new String(SHARED_BUFFER.getCharArray(0, len));
                    data.candidates.add((i + 1) + ". " + word);
                }
            }
        } catch (Throwable ignored) {
        }

        return data;
    }
}
