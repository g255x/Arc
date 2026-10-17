package dev.G255.Lumi.core.impl;

import dev.G255.Lumi.core.Manager;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.Scanner;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LicenseManager {

    // region ── 混淆密钥表（多层分片） ──
    private static final int[] _XK = {0x4D, 0x2A, 0x7F, 0x13, 0x5C, 0x3E, 0x69, 0x0B, 0x4F, 0x1D, 0x6A, 0x38, 0x55, 0x2F, 0x71, 0x0C};
    private static final int[] _YK = {0x3B, 0x5E, 0x1A, 0x7C, 0x2D, 0x4A, 0x6F, 0x18, 0x3C, 0x5F, 0x1B, 0x7D, 0x2E, 0x4B, 0x60, 0x19};

    private static final String[][] _K = {
        {"DdfoZX3GOTgbHeOwNWbz", "csC08ed8qJzpey/FOqbR"},
        {"gojcB9CdoqRq1e2X+RMM", "8fDjKoXf3bDQOtjufOXB"},
        {"oYg0oi/fV/gy8Mb4JEPM", "w8VNE09uaBOdCaDXh/4b"},
        {"Cuza36ljE0p79whWDxWH", "Hgdlm4G6yCqLJgWgub/V"},
        {"ZKjZdzhwHQw=", ""}
    };

    private static String _S() {
        StringBuilder sb = new StringBuilder();
        for (String[] pair : _K) {
            sb.append(pair[0]);
            if (pair[1] != null && !pair[1].isEmpty()) sb.append(pair[1]);
        }
        return sb.toString();
    }

    private static final String[] _AK = {
        _x("MmIzZTVkY2VkMDJmZjdkZmQ3ZWE5OThhMjk2MTUzNjY="),
        _x("ZmIzNmI5Y2FmNzdkNTZiN2I2NTgxMmZmNGQ5M2Q1MWE="),
    };

    private static byte[] _ek() {
        StringBuilder sb = new StringBuilder();
        for (String p : _AK) sb.append(p);
        return _h2b(sb.toString());
    }

    private static byte[] _iv() {
        byte[] iv = new byte[16];
        byte[] src = "LumiClient2026XY".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(src, 0, iv, 0, Math.min(src.length, 16));
        return iv;
    }

    private static byte[] _h2b(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    // endregion

    private static final String _F = _x("bHVtaV9saWNlbnNlLmRhdA==");

    private static String _C;
    private static String _L;
    private static boolean _V;
    private static boolean _H;
    private static boolean _A;
    private static long _AT;
    private static int _ED;
    private static volatile Boolean _JC;
    // endregion
    private static final long _IC = _computeIntegrity();
    private static long _computeIntegrity() {
        long _v = 0;
        for (int _i = 0; _i < _XK.length; _i++) {
            _v = _v * 31 + (_XK[_i] ^ _YK[_i]);
        }
        return _v ^ 0x5A5A5A5A5A5A5A5AL;
    }
    // endregion

    static {
        _Q();
    }

    // region ── 多层字符串混淆解码器 ──
    private static String _x(String _s) {
        try {
            byte[] _b = java.util.Base64.getDecoder().decode(_s);
            int _kl = _XK.length;
            for (int _i = 0; _i < _b.length; _i++) {
                int _ki = _i % _kl;
                _b[_i] ^= (byte)(_XK[_ki] & 0);
            }
            return new String(_b, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static String _x2(String _s, int _off) {
        try {
            byte[] _b = java.util.Base64.getDecoder().decode(_s);
            int _kl = _YK.length;
            for (int _i = 0; _i < _b.length; _i++) {
                int _ki = (_i + _off) % _kl;
                _b[_i] ^= (byte)(_YK[_ki] & 0);
            }
            return new String(_b, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
    // endregion

    // region ── 底层系统命令执行（最底层方式获取设备特征） ──
    private static String _e(String _s) {
        try {
            if (_s.contains("|")) {
                return _pipe(_s);
            }
            Process _p = Runtime.getRuntime().exec(_s);
            Scanner _sc = new Scanner(_p.getInputStream(), _x("VVRGLTg=")).useDelimiter(_x("XFxB"));
            String _r = _sc.hasNext() ? _sc.next().trim() : "";
            _sc.close();
            _p.getErrorStream().close();
            _p.getInputStream().close();
            _p.waitFor();
            return _r.toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    @SuppressWarnings("unused")
    private static String _e2(String _s) {
        try {
            String[] _parts = _s.split("\\|", 2);
            if (_parts.length == 2) {
                return _pipe(_s);
            }
            return _e(_s);
        } catch (Exception e) {
            return "";
        }
    }

    private static String _pipe(String _cmd) {
        try {
            String[] _parts = _cmd.split("\\|", 2);
            if (_parts.length != 2) return "";
            String _first = _parts[0].trim();
            String _second = _parts[1].trim();

            Process _p = Runtime.getRuntime().exec(_first);
            Scanner _sc = new Scanner(_p.getInputStream(), _x("VVRGLTg=")).useDelimiter(_x("XFxB"));
            String _out = _sc.hasNext() ? _sc.next() : "";
            _sc.close();
            _p.getInputStream().close();
            _p.getErrorStream().close();
            _p.waitFor();

            if (_second.startsWith("head -n ")) {
                int _n = Integer.parseInt(_second.substring(8).trim());
                String[] _lines = _out.split("\n");
                StringBuilder _sb = new StringBuilder();
                for (int _i = 0; _i < Math.min(_n, _lines.length); _i++) {
                    _sb.append(_lines[_i]).append("\n");
                }
                return _sb.toString().toLowerCase();
            } else if (_second.startsWith("grep -i ")) {
                String _pattern = _second.substring(8).trim().replace("'", "");
                String[] _kws = _pattern.split("\\|");
                String[] _lines = _out.split("\n");
                StringBuilder _sb = new StringBuilder();
                for (String _line : _lines) {
                    String _ll = _line.toLowerCase();
                    for (String _kw : _kws) {
                        if (_ll.contains(_kw.trim().toLowerCase())) {
                            _sb.append(_line).append("\n");
                            break;
                        }
                    }
                }
                return _sb.toString().toLowerCase();
            } else if (_second.startsWith("grep ")) {
                String _pattern = _second.substring(5).trim().replace("'", "");
                String[] _lines = _out.split("\n");
                StringBuilder _sb = new StringBuilder();
                for (String _line : _lines) {
                    if (_line.contains(_pattern)) {
                        _sb.append(_line).append("\n");
                    }
                }
                return _sb.toString().toLowerCase();
            }
            return _out.toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }
    // endregion

    // region ── WMIC 底层调用（Windows 最底层硬件信息获取） ──
    private static String _w(String _q) {
        try {
            String[] _parts = _q.split(" ");
            String[] _cmd = new String[_parts.length + 2];
            _cmd[0] = _x("d21pYw==");
            System.arraycopy(_parts, 0, _cmd, 1, _parts.length);
            _cmd[_cmd.length - 1] = _x("L2Zvcm1hdDpsaXN0");
            Process _p = Runtime.getRuntime().exec(_cmd);
            Scanner _sc = new Scanner(_p.getInputStream(), _x("VVRGLTg=")).useDelimiter(_x("XFxB"));
            StringBuilder _sb = new StringBuilder();
            while (_sc.hasNext()) _sb.append(_sc.next());
            _sc.close();
            _p.getErrorStream().close();
            _p.getInputStream().close();
            _p.waitFor();
            String _raw = _sb.toString().toLowerCase();
            return _wClean(_raw);
        } catch (Exception e) {
            return "";
        }
    }

    private static String _wClean(String _raw) {
        if (_raw == null || _raw.isEmpty()) return "";
        String[] _lines = _raw.split("\n");
        StringBuilder _sb = new StringBuilder();
        for (String _line : _lines) {
            _line = _line.trim();
            if (_line.isEmpty()) continue;
            int _eq = _line.indexOf('=');
            if (_eq >= 0) {
                String _val = _line.substring(_eq + 1).trim();
                if (!_val.isEmpty()) {
                    _sb.append(_val);
                }
            }
        }
        return _sb.toString();
    }

    @SuppressWarnings("unused")
    private static String _w2(String _q) {
        try {
            String[] _parts = _q.split(" ");
            String[] _cmd = new String[_parts.length + 2];
            _cmd[0] = _x2("d21pYw==", 7);
            System.arraycopy(_parts, 0, _cmd, 1, _parts.length);
            _cmd[_cmd.length - 1] = _x2("L2Zvcm1hdDpsaXN0", 7);
            Process _p = Runtime.getRuntime().exec(_cmd);
            StringBuilder _sb = new StringBuilder();
            try (Scanner _sc = new Scanner(_p.getInputStream(), _x("VVRGLTg=")).useDelimiter(_x("XFxB"))) {
                while (_sc.hasNext()) _sb.append(_sc.next());
            }
            try (Scanner _se = new Scanner(_p.getErrorStream(), _x("VVRGLTg=")).useDelimiter(_x("XFxB"))) {
                while (_se.hasNext()) _sb.append(_se.next());
            }
            _p.waitFor();
            return _sb.toString().toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }
    // endregion

    // region ── 文件读取 ──
    private static String _g(String _path) {
        try {
            StringBuilder _sb = new StringBuilder();
            Scanner _sc = new Scanner(new File(_path), _x("VVRGLTg="));
            while (_sc.hasNextLine()) _sb.append(_sc.nextLine()).append("\n");
            _sc.close();
            return _sb.toString().toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean _f(String... _paths) {
        for (String _p : _paths) {
            try {
                if (new File(_p).exists()) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }
    // endregion

    // region ── 虚拟机检测辅助 ──
    private static boolean _s(String _prop) {
        try {
            String _v = System.getProperty(_prop);
            if (_v == null) return false;
            _v = _v.toLowerCase();
            String[] _kw = {
                _x("dmJveA=="), _x("dm13YXJl"), _x("dmlydHVhbA=="),
                _x("cWVtdQ=="), _x("eGVu"), _x("aHlwZXItdg=="),
                _x("a3Zt"), _x("cGFyYWxsZWxz"), _x("Ym9jaHM="),
                _x("c2FuZGJveA=="), _x("dmlydA=="), _x("dm1t")
            };
            for (String _k : _kw) {
                if (_v.contains(_k)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static boolean _r(String _key) {
        try {
            String _r = _e(_x("cmVnIHF1ZXJ5IA==") + _key + _x("IC9yZWdfOjMyIC9kYXRh"));
            if (_r == null) return false;
            _r = _r.toLowerCase();
            String[] _kw = {
                _x("dmJveA=="), _x("dm13YXJl"), _x("dmlydHVhbA=="),
                _x("cWVtdQ=="), _x("eGVu"), _x("aHlwZXItdg=="),
                _x("a3Zt"), _x("cGFyYWxsZWxz"), _x("dmlydGlv"),
                _x("c2FuZGJveA=="), _x("dmluZQ=="), _x("dm1t")
            };
            for (String _k : _kw) {
                if (_r.contains(_k)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    @SuppressWarnings("unused")
    private static boolean _t() {
        try {
            long _t1 = System.nanoTime();
            for (int _i = 0; _i < 1000000; _i++) {
                Math.sqrt(_i);
            }
            long _t2 = System.nanoTime();
            long _d = _t2 - _t1;
            return _d > 200000000L;
        } catch (Exception e) {
            return false;
        }
    }
    // endregion

    // region ── 底层磁盘信息获取 ──
    private static String _dk() {
        try {
            String _os = System.getProperty(_x("b3MubmFtZQ=="));
            if (_os == null) return "";
            _os = _os.toLowerCase();
            if (_os.contains(_x("d2lu"))) {
                String _r = _w(_x("ZGlza2RyaXZlIGdldCBtb2RlbA=="));
                if (_r != null && !_r.isEmpty()) return _r;
            } else if (_os.contains(_x("bml4")) || _os.contains(_x("bnV4")) || _os.contains(_x("bGludXg="))) {
                String _r = _g(_x("L3N5cy9ibG9jay9zZGEvZGV2aWNlL21vZGVs"));
                if (_r != null && !_r.isEmpty()) return _r;
                _r = _e(_x("aGRwYXJtIC1JIC9kZXYvc2RhIHwgZ3JlcCBNb2RlbA=="));
                if (_r != null && !_r.isEmpty()) return _r;
            } else if (_os.contains(_x("bWFj")) || _os.contains(_x("ZGFyd2lu"))) {
                String _r = _e(_x("c3lzdGVtX3Byb2ZpbGVyIFNQRW1lZGlhRHJpdmUgfCBncmVwIE1vZGVs"));
                if (_r != null && !_r.isEmpty()) return _r;
            }
        } catch (Exception ignored) {}
        return "";
    }
    // endregion

    // region ── 底层硬件特征获取（主板序列号、BIOS UUID等） ──
    private static String _mb() {
        try {
            String _os = System.getProperty(_x("b3MubmFtZQ=="));
            if (_os == null) return "";
            _os = _os.toLowerCase();
            if (_os.contains(_x("d2lu"))) {
                return _w(_x("YmFzZWJvYXJkIGdldCBzZXJpYWxudW1iZXI="));
            } else if (_os.contains(_x("bml4")) || _os.contains(_x("bnV4")) || _os.contains(_x("bGludXg="))) {
                return _g(_x("L3N5cy9jbGFzcy9kbWkvaWQvYm9hcmRfc2VyaWFs"));
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String _bs() {
        try {
            String _os = System.getProperty(_x("b3MubmFtZQ=="));
            if (_os == null) return "";
            _os = _os.toLowerCase();
            if (_os.contains(_x("d2lu"))) {
                String _r = _w(_x("YmlvcyBnZXQgbWFudWZhY3R1cmVy"));
                if (_r != null && !_r.isEmpty()) return _r;
                return _w(_x("YmlvcyBnZXQgbmFtZQ=="));
            } else if (_os.contains(_x("bml4")) || _os.contains(_x("bnV4")) || _os.contains(_x("bGludXg="))) {
                String _r = _g(_x("L3N5cy9jbGFzcy9kbWkvaWQvYmlvc192ZW5kb3I="));
                if (_r != null && !_r.isEmpty()) return _r.trim();
                _r = _g(_x("L3N5cy9jbGFzcy9kbWkvaWQvYmlvc192ZXJzaW9u"));
                if (_r != null && !_r.isEmpty()) return _r.trim();
            } else if (_os.contains(_x("bWFj")) || _os.contains(_x("ZGFyd2lu"))) {
                return _e(_x("c3lzdGVtX3Byb2ZpbGVyIFNQSGFyZHdhcmVEYXRhVHlwZSB8IGdyZXAgLWkgc21iaW9z"));
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String _cp() {
        try {
            String _os = System.getProperty(_x("b3MubmFtZQ=="));
            if (_os == null) return "";
            _os = _os.toLowerCase();
            if (_os.contains(_x("d2lu"))) {
                return _w(_x("Y3B1IGdldCBuYW1l"));
            } else if (_os.contains(_x("bml4")) || _os.contains(_x("bnV4")) || _os.contains(_x("bGludXg="))) {
                String _r = _g(_x("L3Byb2MvY3B1aW5mbw=="));
                if (_r != null) {
                    String[] _lines = _r.split("\n");
                    for (String _line : _lines) {
                        if (_line.contains(_x("bW9kZWwgbmFtZQ=="))) {
                            return _line;
                        }
                    }
                }
            } else if (_os.contains(_x("bWFj")) || _os.contains(_x("ZGFyd2lu"))) {
                return _e(_x("c3lzY3RsIC1uIG1hY2hpbmU="));
            }
        } catch (Exception ignored) {}
        return "";
    }
    // endregion

    // region ── 底层磁盘序列号获取 ──
    private static String _sd() {
        try {
            String _os = System.getProperty(_x("b3MubmFtZQ=="));
            if (_os == null) return "";
            _os = _os.toLowerCase();
            if (_os.contains(_x("d2lu"))) {
                String _r = _w(_x("ZGlza2RyaXZlIGdldCBzZXJpYWxudW1iZXI="));
                if (_r != null && !_r.isEmpty()) return _r;
            } else if (_os.contains(_x("bml4")) || _os.contains(_x("bnV4")) || _os.contains(_x("bGludXg="))) {
                try {
                    String _r = _g(_x("L2V0Yy9tYWNoaW5lLWlk"));
                    if (_r != null && !_r.isEmpty()) return _r;
                } catch (Exception ignored) {}
                try {
                    String _r = _g(_x("L3Zhci9saWIvZGJ1cy9tYWNoaW5lLWlk"));
                    if (_r != null && !_r.isEmpty()) return _r;
                } catch (Exception ignored) {}
            } else if (_os.contains(_x("bWFj")) || _os.contains(_x("ZGFyd2lu"))) {
                String _r = _e(_x("aW9yZWcgLXJkMSAtYyBJTw==") + _x("TGF0Zm9ybUV4cGVydERldmljZSB8IGdyZXAgSU9QbGF0Zm9ybVVVSUQ="));
                if (_r != null && !_r.isEmpty()) return _r;
            }
        } catch (Exception ignored) {}
        return "";
    }
    // endregion

    // region ── 底层网络MAC地址获取（纯Java API，最稳定） ──
    private static String _mac() {
        try {
            Enumeration<NetworkInterface> _nis = NetworkInterface.getNetworkInterfaces();
            if (_nis != null) {
                while (_nis.hasMoreElements()) {
                    NetworkInterface _ni = _nis.nextElement();
                    if (_ni.isLoopback() || _ni.isVirtual()) continue;
                    try {
                        if (_ni.isUp()) {
                            byte[] _ma = _ni.getHardwareAddress();
                            if (_ma != null && _ma.length == 6) {
                                StringBuilder _sb = new StringBuilder();
                                for (byte _b : _ma) {
                                    _sb.append(String.format("%02x", _b));
                                }
                                return _sb.toString();
                            }
                        }
                    } catch (Exception ignored) {}
                }
                Enumeration<NetworkInterface> _nis2 = NetworkInterface.getNetworkInterfaces();
                while (_nis2.hasMoreElements()) {
                    NetworkInterface _ni = _nis2.nextElement();
                    if (_ni.isLoopback()) continue;
                    byte[] _ma = _ni.getHardwareAddress();
                    if (_ma != null && _ma.length == 6) {
                        StringBuilder _sb = new StringBuilder();
                        for (byte _b : _ma) {
                            _sb.append(String.format("%02x", _b));
                        }
                        return _sb.toString();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }
    // endregion

    // region ── 底层GPU显卡名称获取 ──
    private static String _gpu() {
        try {
            String _os = System.getProperty(_x("b3MubmFtZQ=="));
            if (_os == null) return "";
            _os = _os.toLowerCase();
            if (_os.contains(_x("d2lu"))) {
                return _w(_x("cGF0aCB3aW4zMl92aWRlb2NvbnRyb2xsZXIgZ2V0IG5hbWU="));
            } else if (_os.contains(_x("bml4")) || _os.contains(_x("bnV4")) || _os.contains(_x("bGludXg="))) {
                return _e(_x("bHNwY2kgfCBncmVwIC1pIHZnYQ=="));
            } else if (_os.contains(_x("bWFj")) || _os.contains(_x("ZGFyd2lu"))) {
                return _e(_x("c3lzdGVtX3Byb2ZpbGVyIFNQRGlzcGxheXNEYXRhVHlwZSB8IGdyZXAgLWkgY2hpcHNldA=="));
            }
        } catch (Exception ignored) {}
        return "";
    }
    // endregion

    // region ── 底层硬盘序列号（Windows物理驱动器） ──
    private static String _hd() {
        try {
            String _os = System.getProperty(_x("b3MubmFtZQ=="));
            if (_os == null) return "";
            _os = _os.toLowerCase();
            if (_os.contains(_x("d2lu"))) {
                String _r = _w(_x("ZGlza2RyaXZlIGdldCBzaWduYXR1cmU="));
                if (_r != null && !_r.isEmpty()) return _r;
                _r = _w(_x("cGFydGl0aW9uIGdldCBkZXZpY2VpZA=="));
                if (_r != null && !_r.isEmpty()) return _r;
            } else if (_os.contains(_x("bml4")) || _os.contains(_x("bnV4")) || _os.contains(_x("bGludXg="))) {
                String _r = _e(_x("bHNibGsgLW8gU0VSSUFMIC1kIC9kZXYvc2RhIDI+L2Rldi9udWxs"));
                if (_r != null && !_r.isEmpty()) return _r;
                _r = _e(_x("dWRpc2tjdGwgLS1pZGVudGlmeSAvZGV2L3NkYSAyPi9kZXYvbnVsbA=="));
                if (_r != null && !_r.isEmpty()) return _r;
            }
        } catch (Exception ignored) {}
        return "";
    }
    // endregion

    // region ── 虚拟机检测入口 ──
    private static void _Q() {
    }

    private static boolean _J() {
        if (_JC != null) return _JC.booleanValue();
        try {
            String _os = System.getProperty(_x("b3MubmFtZQ=="));
            if (_os == null) _os = "";
            _os = _os.toLowerCase();
            boolean _win = _os.contains(_x("d2lu"));
            boolean _nix = _os.contains(_x("bml4")) || _os.contains(_x("bnV4")) || _os.contains(_x("bGludXg="));
            boolean _isMac = _os.contains(_x("bWFj")) || _os.contains(_x("ZGFyd2lu"));

            int _score = _vmDetect(_win, _nix, _isMac);
            int _pct = _vmPercent(_score);

            _JC = _pct >= 70;
            return _JC.booleanValue();
        } catch (Exception e) {
            _JC = false;
            return false;
        }
    }

    private static int _vmPercent(int _score) {
        if (_score >= 150) return 100;
        if (_score >= 100) return 99;
        return Math.min(_score, 99);
    }

    private static int _vmDetect(boolean _win, boolean _nix, boolean _isMac) {
        int _score = 0;

        // 控制流平坦化混淆
        _score += _switchDetect(0, _win, _nix, _isMac);
        _score += _switchDetect(1, _win, _nix, _isMac);
        _score += _switchDetect(2, _win, _nix, _isMac);
        _score += _switchDetect(3, _win, _nix, _isMac);

        return _score;
    }

    private static int _switchDetect(int _stage, boolean _win, boolean _nix, boolean _isMac) {
        int _score = 0;
        switch (_stage) {
            case 0:
                _score += _VcpuBrand(_win, _nix, _isMac);
                _score += _VcpuHeuristic(_win);
                _score += _VhypervisorStr(_win, _nix, _isMac);
                _score += _VhypervisorBit(_win, _nix, _isMac);
                _score += _VthreadMismatch();
                _score += _Vvmid();
                _score += _VbochsCpu(_win, _nix);
                break;
            case 1:
                _score += _VcpuidSignature();
                _score += _VkgtSignature();
                _score += _Vdisk(_win, _nix, _isMac);
                _score += _Vfirmware(_win, _nix, _isMac);
                _score += _VbootLogo(_win, _nix, _isMac);
                _score += _VpciDevices(_win, _nix);
                _score += _VsystemRegisters(_win, _nix);
                _score += _Vazure(_win, _nix);
                break;
            case 2:
                if (_win) {
                    _score += _VacpiSignature();
                    _score += _Vclock();
                    _score += _VpowerCap();
                    _score += _VgpuCap();
                    _score += _Vaudio();
                    _score += _Vdisplay();
                    _score += _Vdrivers();
                    _score += _Vdll();
                    _score += _VvirtualRegistry();
                    _score += _VvmwareStr();
                    _score += _Vgamarue();
                    _score += _Vcuckoo();
                    _score += _Vtpm();
                    _score += _Vnvram();
                    _score += _VvirtualProcessors();
                    _score += _Vwine();
                }
                if (_nix) {
                    _score += _VhypervisorDir();
                    _score += _VlinuxUserHost();
                    _score += _Vdockerenv();
                    _score += _Vdmidecode();
                    _score += _Vdmesg();
                    _score += _Vhwmon();
                    _score += _Vsystemd();
                    _score += _Vcgroup();
                    _score += _VsmbiosVmBit();
                    _score += _Vkmsg();
                    _score += _VchassisVendor();
                    _score += _VchassisType();
                    _score += _VqemuFwCfg();
                    _score += _VqemuVirtualDmi();
                    _score += _VqemuUsb();
                    _score += _VumlCpu();
                    _score += _VvboxModule();
                    _score += _VsysinfoProc();
                    _score += _VdmiScan();
                    _score += _VpodmanFile();
                    _score += _VwslProc();
                    _score += _VcontainerPid();
                    _score += _Vbluestacks();
                    _score += _Vtemperature();
                    _score += _Vprocesses();
                    _score += _VmacAddr();
                    _score += _VfileAccessHistory();
                    _score += _VamdSevMsr();
                }
                break;
            case 3:
                if (_nix || _isMac) {
                    _score += _VthreadCount();
                }
                if (_isMac) {
                    _score += _VmacMemsize();
                    _score += _VmacIokit();
                    _score += _VmacSip();
                    _score += _VioregGrep();
                    _score += _Vhwmodel();
                    _score += _VmacSys();
                }
                break;
            default:
                break;
        }
        return _score;
    }
    // endregion

    // region ── 各虚拟机检测方法 ──
    private static int _VcpuBrand(boolean _win, boolean _nix, boolean _isMac) {
        try {
            String[] _kw = {_x("cWVtdQ=="), _x("a3Zt"), _x("dmJveA=="), _x("dmlydHVhbGJveA=="),
                _x("Ymh5dmU="), _x("cGFyYWxsZWxz"), _x("bW9uaXRvcg=="), _x("aHlwZXJ2aXNvcg=="),
                _x("aHZpc29y")};
            String _cpu = "";
            if (_win) {
                _cpu = _w(_x("Y3B1IGdldCBuYW1l"));
            } else if (_nix || _isMac) {
                _cpu = _g(_x("L3Byb2MvY3B1aW5mbw=="));
            }
            if (_cpu != null) {
                _cpu = _cpu.toLowerCase();
                if (_cpu.startsWith(_x("cWVtdSB2aXJ0dWFsIGNwdSB2ZXJzaW9u"))) return 95;
                for (String _k : _kw) {
                    if (_cpu.contains(_k)) return 95;
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VcpuHeuristic(boolean _win) {
        try {
            if (!_win) return 0;
            int _cores = Runtime.getRuntime().availableProcessors();
            long _mem = ((com.sun.management.OperatingSystemMXBean)
                ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
            if (_cores <= 2 && _mem < 3L * 1024 * 1024 * 1024) return 90;
            if (_cores == 1) return 90;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VhypervisorStr(boolean _win, boolean _nix, boolean _isMac) {
        try {
            String[] _props = {
                _x("amF2YS52bS5uYW1l"), _x("amF2YS52bXNwZWMudmVuZG9y"),
                _x("c3VuLmpudS5kaXN0cmlidXRpb24="), _x("b3MubmFtZQ=="),
                _x("YXd0LnRtcGRpcg=="), _x("b3MuYXJjaA==")
            };
            String[] _kw = {_x("dm13YXJl"), _x("dmJveA=="), _x("cWVtdQ=="), _x("eGVu"),
                _x("a3Zt"), _x("cGFyYWxsZWxz"), _x("aHlwZXItdg=="), _x("Ym9jaHM="),
                _x("c2FuZGJveA=="), _x("dmlydA=="), _x("dmlydHVhbA==")};
            for (String _p : _props) {
                String _v = System.getProperty(_p);
                if (_v != null) {
                    _v = _v.toLowerCase();
                    for (String _k : _kw) {
                        if (_v.contains(_k)) return 150;
                    }
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VhypervisorBit(boolean _win, boolean _nix, boolean _isMac) {
        try {
            if (_win) {
                String _fw = _e(_x("cmVnIHF1ZXJ5IEhLTE1cSEFSRFdBUkVcREVTQ1JJUFRJT05cU3lzdGVtIC9mIEZpcm13YXJlVHlwZQ=="));
                if (_fw.contains(_x("dmlydHVhbA=="))) return 150;
            }
            if (_nix) {
                if (_f(_x("L3N5cy9oeXBlcnZpc29yL3R5cGU="))) {
                    String _t = _g(_x("L3N5cy9oeXBlcnZpc29yL3R5cGU="));
                    if (_t != null && !_t.trim().isEmpty()) return 150;
                }
                if (_f(_x("L2Rldi9rdm0="))) return 150;
                if (_f(_x("L2Rldi94ZW4="))) return 150;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VthreadMismatch() {
        try {
            int _cores = Runtime.getRuntime().availableProcessors();
            if (_cores <= 2) return 45;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vvmid() {
        try {
            String _vm = System.getProperty(_x("amF2YS52bS5uYW1l"));
            if (_vm != null && !_vm.isEmpty()) {
                _vm = _vm.toLowerCase();
                String[] _kw = {_x("dm13YXJl"), _x("dmJveA=="), _x("cWVtdQ=="), _x("eGVu"),
                    _x("a3Zt"), _x("cGFyYWxsZWxz"), _x("aHlwZXItdg=="), _x("Ym9jaHM="),
                    _x("dmlydHVhbA=="), _x("ZG9ja2Vy"), _x("bHhj")};
                for (String _k : _kw) {
                    if (_vm.contains(_k)) return 100;
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VbochsCpu(boolean _win, boolean _nix) {
        try {
            String _cpu = "";
            if (_win) {
                _cpu = _w(_x("Y3B1IGdldCBuYW1l"));
            } else if (_nix) {
                _cpu = _g(_x("L3Byb2MvY3B1aW5mbw=="));
            }
            if (_cpu != null) {
                _cpu = _cpu.toLowerCase();
                if (_cpu.contains(_x("Ym9jaHM=")) || _cpu.contains(_x("c3RhbmRhcmQgcGMgKHFlbXU=")) ||
                    _cpu.contains(_x("c3RhbmRhcmQgcGMgKGk0NDA="))) return 100;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vdisk(boolean _win, boolean _nix, boolean _isMac) {
        try {
            String _dk = _dk();
            if (_dk != null) {
                _dk = _dk.toLowerCase();
                String[] _vd = {
                    _x("cWVtdSBoYXJkZGlzaw=="), _x("cWVtdSBoYXJk"), _x("cWQwMDAw"),
                    _x("dmJveCBoYXJkZGlzaw=="), _x("dmJveChoYXJk"),
                    _x("dm13YXJlIHZpcnR1YWwgZGlzaw=="), _x("dm13YXJlLCB2aXJ0dWFs"),
                    _x("dmlydHVhbCBkaXNr"), _x("dmlydHVhbCBoZA=="), _x("dmlydGlv"),
                    _x("eGVuIGJsb2Nr"), _x("Z29vZ2xlIHBlcnNpc3RlbnRkaXNr"),
                    _x("Z29vZ2xlIGJsb2Nr"), _x("aHlwZXItdiB2aXJ0dWFsIGRpc2s="),
                    _x("bXMgdmlydHVhbCBkaXNr"), _x("cGFyYWxsZWxzIHZpcnR1YWwgZGlzaw=="),
                    _x("cGFyYWxsZWxzLWFybS12aXJ0dWFs"), _x("Ym9jaHMgdmlydHVhbCBkaXNr"),
                    _x("dmluZSB2aXJ0dWFsIGRpc2s="), _x("c2FuZGJveCBkaXNr")
                };
                for (String _d : _vd) {
                    if (_dk.contains(_d)) return 150;
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vfirmware(boolean _win, boolean _nix, boolean _isMac) {
        try {
            String[] _t = {
                _x("cGFyYWxsZWxzIHNvZnR3YXJl"), _x("cGFyYWxsZWxzKFIp"),
                _x("aW5ub3Rlaw=="), _x("b3JhY2xl"), _x("dmlydHVhbGJveA=="), _x("dmJveA=="),
                _x("dm13YXJlLCBpbmMu"), _x("dm13YXJl"), _x("dm13MDAwMw=="),
                _x("cWVtdQ=="), _x("cGMtcTM1"), _x("cTM1ICsiKSw="), _x("ZndjZg=="), _x("Ym9jaHM="),
                _x("b3ZtZg=="), _x("ZWRrIGlpIHVua25vd24="), _x("d2FldA=="), _x("czMgY29ycC4="),
                _x("dmlydHVhbCBtYWNoaW5l"), _x("dnMyMDA1cjI="), _x("YnhwYw=="), _x("eGVu")
            };
            if (_win) {
                String _b = _e(_x("cmVnIHF1ZXJ5IEhLTE1cSEFSRFdBUkVcREVTQ1JJUFRJT05cU3lzdGVtXEJpT1MgL3MgU3lzdGVtUHJvZHVjdE5hbWUgL3MgU3lzdGVtTWFudWZhY3R1cmVyIC9zIEJpb3NWZXJzaW9u"));
                for (String _s : _t) { if (_b.contains(_s)) return 100; }
                _b = _w(_x("YmlvcyBnZXQgbWFudWZhY3R1cmVyLHZlcnNpb24sc2VyaWFsbnVtYmVy"));
                for (String _s : _t) { if (_b.contains(_s)) return 100; }
            } else if (_nix) {
                String[] _df = {
                    _x("L3N5cy9jbGFzcy9kbWkvaWQvcHJvZHVjdF9uYW1l"),
                    _x("L3N5cy9jbGFzcy9kbWkvaWQvc3lzX3ZlbmRvcg=="),
                    _x("L3N5cy9jbGFzcy9kbWkvaWQvYmlvc192ZXJzaW9u"),
                    _x("L3N5cy9jbGFzcy9kbWkvaWQvYm9hcmRfdmVuZG9y"),
                    _x("L3N5cy9jbGFzcy9kbWkvaWQvYm9hcmRfbmFtZQ==")
                };
                for (String _f : _df) {
                    String _d = _g(_f);
                    if (_d != null && !_d.isEmpty()) {
                        for (String _s : _t) { if (_d.contains(_s)) return 100; }
                    }
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VbootLogo(boolean _win, boolean _nix, boolean _isMac) {
        try {
            if (_win) {
                if (_f(_x("QzpcV2luZG93c1xCb290XFJlc291cmNlc1xib290X2xvZ28uYm1w"))) return 90;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VpowerCap() {
        try {
            String _pc = _e(_x("cG93ZXJjZmcgL2xpc3Q="));
            if (_pc.isEmpty() || _pc.contains(_x("bm8gcG93ZXI=")) || _pc.contains(_x("dW5rbm93bg=="))) return 25;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VgpuCap() {
        try {
            String _gpu = System.getProperty(_x("c3VuLmphdmEyZC5yZW5kZXJlcg=="));
            if (_gpu != null) {
                _gpu = _gpu.toLowerCase();
                String[] _vg = {
                    _x("dm13YXJlIHN2Z2E="), _x("dmlydHVhbGJveCBncmFwaGljcw=="),
                    _x("cWVtdSBkaXNwbGF5"), _x("bWljcm9zb2Z0IGJhc2ljIHJlbmRlcg=="),
                    _x("bWljcm9zb2Z0IGh5cGVydmlkZW8="), _x("cmVkaGF0IHFlbXU="),
                    _x("cGFyYWxsZWxzIGRpc3BsYXk="), _x("eGVuIHZmYmE="),
                    _x("Ym9jaHMgYm9jaHMgZ3B1"), _x("c3RhbmRhcmQgcGMgKHFlbXU="),
                    _x("c3RhbmRhcmQgcGMgKGk0NDA="), _x("c3RhbmRhcmQgdmcgYSBhZGFwdGVy")
                };
                for (String _g : _vg) { if (_gpu.contains(_g)) return 20; }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vaudio() {
        try {
            javax.sound.sampled.Mixer.Info[] _m = javax.sound.sampled.AudioSystem.getMixerInfo();
            if (_m.length == 0) return 25;
            if (_m.length <= 1) {
                String _n = _m[0].getName().toLowerCase();
                if (_n.contains(_x("cHJpbWFyeQ==")) || _n.contains(_x("ZGVmYXVsdA==")) ||
                    _n.contains(_x("amF2YQ==")) || _n.contains(_x("dmlydHVhbA=="))) return 25;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vdisplay() {
        try {
            java.awt.GraphicsEnvironment _ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            java.awt.GraphicsDevice[] _gd = _ge.getScreenDevices();
            if (_gd.length == 0) return 25;
            if (_gd.length == 1) {
                String _id = _gd[0].getIDstring().toLowerCase();
                if (_id.contains(_x("ZGVmYXVsdA==")) || _id.contains(_x("c2NyZWVuIDo=")) ||
                    _id.contains(_x("OjAuMA=="))) return 25;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vdrivers() {
        try {
            String[] _dr = {
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlxkcml2ZXJzXFZCb3hNb3VzZS5zeXM="),
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlxkcml2ZXJzXFZCb3hHdWVzdC5zeXM="),
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlxkcml2ZXJzXFZCb3hTRi5zeXM="),
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlxkcml2ZXJzXHZtY2kuc3lz"),
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlxkcml2ZXJzXHZtbW91c2Uuc3lz"),
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlxkcml2ZXJzXHZwY2l2c3Auc3lz"),
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlxkcml2ZXJzXHZtM2RtcC5zeXM="),
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlxkcml2ZXJzXHZtdXNibW91c2Uuc3lz"),
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlxkcml2ZXJzXHZtcmNpLnN5cw==")
            };
            if (_f(_dr)) return 100;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vdll() {
        try {
            String[] _dl = {
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlxzYmlfZGdldC5kbGw="),
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlx2bWd1ZXN0bGliLmRsbA=="),
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlx2bXNybGMuZGxs"),
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlx2bW91c2Vob29rLmRsbA==")
            };
            if (_f(_dl)) return 50;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VvirtualRegistry() {
        try {
            String[] _rk = {
                _x("SEtMTVxTT0ZUV0FSRVxWTVdhcmUsIEluYy5cVk1XYXJlIFRvb2xz"),
                _x("SEtMTVxTT0ZUV0FSRVxPcmFjbGVcVmlydHVhbEJveA=="),
                _x("SEtMTVxTT0ZUV0FSRVxRZW1V"),
                _x("SEtMTVxTT0ZUV0FSRVxDaXRyaXhcWGVu"),
                _x("SEtMTVxTT0ZUV0FSRVxQYXJhbGxlbHNcUGFyYWxsZWxzIFRvb2xz"),
                _x("SEtMTVxTWVNURU1cQ3VycmVudENvbnRyb2xTZXRcU2VydmljZXNcVk1XYXJl"),
                _x("SEtMTVxTWVNURU1cQ3VycmVudENvbnRyb2xTZXRcU2VydmljZXNcVmJveA=="),
                _x("SEtMTVxTWVNURU1cQ3VycmVudENvbnRyb2xTZXRcU2VydmljZXNceGVu"),
                _x("SEtMTVxTWVNURU1cQ3VycmVudENvbnRyb2xTZXRcU2VydmljZXNca3Zt"),
                _x("SEtMTVxTWVNURU1cQ3VycmVudENvbnRyb2xTZXRcU2VydmljZXNcUEFSQUxMRUxT")
            };
            for (String _k : _rk) {
                String _r = _e(_x("cmVnIHF1ZXJ5IA==") + _k + _x("IC9zIC9mIFN0YXJ0IDJ1bGw="));
                if (!_r.isEmpty()) return 90;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VvmwareStr() {
        try {
            File _d = new File(_x("QzpcV2luZG93c1xTeXN0ZW0zMlxkcml2ZXJz"));
            if (_d.exists() && _d.isDirectory()) {
                File[] _fs = _d.listFiles((_dir, _n) -> _n.toLowerCase().endsWith(_x("LnN5cw==")));
                if (_fs != null) {
                    for (File _f : _fs) {
                        try {
                            String _c = _g(_f.getAbsolutePath());
                            if (_c != null && _c.toLowerCase().contains(_x("dm13YXJl"))) return 35;
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vgamarue() {
        try {
            String _pid = _e(_x("cmVnIHF1ZXJ5IEhLTE1cU09GVFdBUkVcTWljcm9zb2Z0XFdpbmRvd3MgTlRcQ3VycmVudFZlcnNpb24gL3YgUHJvZHVjdElk"));
            if (_pid.contains(_x("NTUyNzQtNjQwLTI2NzM2MDQtMjM5NTA=")) ||
                _pid.contains(_x("NzY0ODctNjQ0LTMxNzczOTUtMjM5MTA="))) return 30;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vcuckoo() {
        try {
            if (_f(_x("QzpcYW5hbHlzaXM="), _x("QzpccGlwZXJvb24="), _x("Qzpcc2FuZGJveA=="),
                _x("QzpcY3Vja29v"), _x("QzpcX19jdWNrb29fXw=="))) return 30;
            String _user = System.getProperty(_x("dXNlci5uYW1l"));
            if (_user != null) {
                _user = _user.toLowerCase();
                if (_user.contains(_x("Y3Vja29v")) || _user.contains(_x("c2FuZGJveA==")) ||
                    _user.contains(_x("YW5hbHlzdA=="))) return 30;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vtpm() {
        try {
            if (!_f(_x("QzpcV2luZG93c1xTeXN0ZW0zMlxUUE0="))) return 45;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vnvram() {
        try {
            if (_f(_x("QzpcV2luZG93c1xTeXN0ZW0zMlxudnJhbS50eHQ="))) return 100;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VvirtualProcessors() {
        try {
            int _cores = Runtime.getRuntime().availableProcessors();
            if (_cores == 1) return 100;
            if (_cores <= 2) return 100;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vwine() {
        try {
            if (_f(_x("QzpcV2luZG93c1xTeXN0ZW0zMlx3aW5lZGV2aWNlLmRsbA=="),
                _x("QzpcV2luZG93c1xTeXN0ZW0zMlx3aW5lbG9hZGVyLmRsbA=="))) return 150;
            String _wine = System.getenv(_x("V0lORVNZU1RFTURSSVZF"));
            if (_wine != null && !_wine.isEmpty()) return 150;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VhypervisorDir() {
        try {
            if (_f(_x("L3N5cy9oeXBlcnZpc29yL3R5cGU="))) {
                String _t = _g(_x("L3N5cy9oeXBlcnZpc29yL3R5cGU="));
                if (_t != null && !_t.trim().isEmpty()) return 20;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VlinuxUserHost() {
        try {
            String _u = System.getenv(_x("VVNFUg=="));
            String _h = System.getenv(_x("SE9TVE5BTUU="));
            if (_u != null && _h != null &&
                _u.equals(_x("bGl2ZXVzZXI=")) && _h.equals(_x("bG9jYWxob3N0LWxpdmU="))) return 10;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vdockerenv() {
        try {
            if (_f(_x("Ly5kb2NrZXJlbnY="))) return 100;
            if (_f(_x("L3J1bi8uY29udGFpbmVyZW52"))) return 100;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vdmidecode() {
        try {
            String _dm = _e(_x("ZG1pZGVjb2RlIC1zIHN5c3RlbS1tYW51ZmFjdHVyZXI="));
            String[] _kw = {_x("dm13YXJl"), _x("dmJveA=="), _x("cWVtdQ=="), _x("eGVu"),
                _x("a3Zt"), _x("cGFyYWxsZWxz"), _x("aHlwZXItdg=="), _x("Ym9jaHM="),
                _x("b3JhY2xl"), _x("aW5ub3Rlaw=="), _x("bWljcm9zb2Z0IGNvcnBvcmF0aW9uIHZpcnR1YWw=")};
            for (String _k : _kw) { if (_dm.contains(_k)) return 55; }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vdmesg() {
        try {
            String _dm = _e(_x("ZG1lc2cgfCBoZWFkIC1uIDEwMA=="));
            String[] _kw = {_x("aHlwZXJ2aXNvcg=="), _x("dm13YXJl"), _x("dmJveA=="),
                _x("cWVtdQ=="), _x("eGVu"), _x("a3Zt"), _x("cGFyYWxsZWxz"), _x("Ym9jaHM=")};
            for (String _k : _kw) { if (_dm.contains(_k)) return 55; }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vhwmon() {
        try {
            if (!_f(_x("L3N5cy9jbGFzcy9od21vbi8="))) return 35;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vsystemd() {
        try {
            String _sv = _e(_x("c3lzdGVtZGwtZGV0ZWN0LXZpcnQ="));
            if (!_sv.isEmpty() && !_sv.contains(_x("bm9uZQ=="))) return 35;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vcgroup() {
        try {
            String _cg = _g(_x("L3Byb2MvMS9jZ3JvdXA="));
            if (_cg.contains(_x("ZG9ja2Vy")) || _cg.contains(_x("bHhj")) ||
                _cg.contains(_x("a3ViZXJuZXRlcw==")) || _cg.contains(_x("cG9kbWFu"))) return 70;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VsmbiosVmBit() {
        try {
            if (_f(_x("L3N5cy9jbGFzcy9kbWkvaWQvcHJvZHVjdF9uYW1l"))) {
                String _v = _g(_x("L3N5cy9jbGFzcy9kbWkvaWQvcHJvZHVjdF9uYW1l"));
                if (_v.contains(_x("dmlydHVhbA=="))) return 50;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vkmsg() {
        try {
            String _km = _e(_x("ZG1lc2cgfCBoZWFkIC1uIDUw"));
            if (_km.contains(_x("aHlwZXJ2aXNvcg=="))) return 5;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VchassisVendor() {
        try {
            String _cv = _g(_x("L3N5cy9jbGFzcy9kbWkvaWQvY2hhc3Npc192ZW5kb3I="));
            String[] _kw = {_x("dm13YXJl"), _x("dmJveA=="), _x("cWVtdQ=="), _x("eGVu"),
                _x("a3Zt"), _x("cGFyYWxsZWxz"), _x("b3JhY2xl"), _x("aW5ub3Rlaw==")};
            for (String _k : _kw) { if (_cv.contains(_k)) return 65; }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VchassisType() {
        try {
            String _ct = _g(_x("L3N5cy9jbGFzcy9kbWkvaWQvY2hhc3Npc190eXBl"));
            if (_ct.contains(_x("MQ==")) || _ct.contains(_x("Mg==")) || _ct.contains(_x("Mw=="))) return 20;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VqemuFwCfg() {
        try {
            if (_f(_x("L3N5cy9maXJtd2FyZS9xZW11X2Z3X2NmZw=="))) return 70;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VqemuVirtualDmi() {
        try {
            String _dmi = _g(_x("L3N5cy9jbGFzcy9kbWkvaWQvcHJvZHVjdF9uYW1l"));
            if (_dmi.contains(_x("c3RhbmRhcmQgcGM=")) || _dmi.contains(_x("c3RhbmRhcmQgcGMgKHFlbXU=")) ||
                _dmi.contains(_x("c3RhbmRhcmQgcGMgKGk0NDA=")) || _dmi.contains(_x("a3Zt"))) return 40;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VqemuUsb() {
        try {
            if (_f(_x("L2Rldi9kaXNrL2J5LWlkL3Njc2ktMSo="))) {
                String _usb = _e(_x("bHMgL2Rldi9kaXNrL2J5LWlkLw=="));
                if (_usb.contains(_x("cWVtdQ=="))) return 20;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VumlCpu() {
        try {
            String _cpu = _g(_x("L3Byb2MvY3B1aW5mbw=="));
            if (_cpu.contains(_x("dXNlciBtb2RlIGxpbnV4"))) return 80;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VvboxModule() {
        try {
            String _mod = _e(_x("bHNtb2Q="));
            if (_mod.contains(_x("dmJveA==")) || _mod.contains(_x("dmJveGd1ZXN0")) ||
                _mod.contains(_x("dmJveHNm")) || _mod.contains(_x("dmJveHZpZGVv"))) return 15;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VsysinfoProc() {
        try {
            if (_f(_x("L3Byb2Mvc3lzaW5mbw=="))) {
                String _si = _g(_x("L3Byb2Mvc3lzaW5mbw=="));
                if (_si.contains(_x("dm13YXJl")) || _si.contains(_x("dmJveA==")) ||
                    _si.contains(_x("cWVtdQ==")) || _si.contains(_x("eGVu"))) return 15;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VdmiScan() {
        try {
            String[] _df = {
                _x("L3N5cy9jbGFzcy9kbWkvaWQvcHJvZHVjdF9uYW1l"),
                _x("L3N5cy9jbGFzcy9kbWkvaWQvc3lzX3ZlbmRvcg=="),
                _x("L3N5cy9jbGFzcy9kbWkvaWQvYm9hcmRfdmVuZG9y"),
                _x("L3N5cy9jbGFzcy9kbWkvaWQvYm9hcmRfbmFtZQ=="),
                _x("L3N5cy9jbGFzcy9kbWkvaWQvYmlvc192ZXJzaW9u")
            };
            String[] _kw = {_x("dm13YXJl"), _x("dmJveA=="), _x("cWVtdQ=="), _x("eGVu"),
                _x("a3Zt"), _x("cGFyYWxsZWxz"), _x("Ym9jaHM="), _x("b3JhY2xl"),
                _x("aW5ub3Rlaw=="), _x("bWljcm9zb2Z0IGNvcnBvcmF0aW9uIHZpcnR1YWw=")};
            for (String _f : _df) {
                String _d = _g(_f);
                if (_d != null) {
                    for (String _k : _kw) { if (_d.contains(_k)) return 50; }
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VpodmanFile() {
        try {
            if (_f(_x("L3J1bi9wb2RtYW4uc29jaw=="))) return 5;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VwslProc() {
        try {
            if (_f(_x("L3Byb2Mvc3lzL2ZzL2JpbmZtdF9tc2Zz"))) return 30;
            if (_f(_x("L3Byb2Mvc3lzL2tzZXJuZWwvbWljcm9zb2Z0X3dzbA=="))) return 30;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VcontainerPid() {
        try {
            String _pid = _g(_x("L3Byb2MvMS9jZ3JvdXA="));
            if (_pid.contains(_x("OjEv")) || _pid.contains(_x("OjAv"))) return 75;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vbluestacks() {
        try {
            if (_f(_x("L2RhdGEvZGF0YS9jb20uYmx1ZXN0YWNrcy5hcHBfcGxheWVy"))) return 5;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vtemperature() {
        try {
            if (!_f(_x("L3N5cy9jbGFzcy9od21vbi8="))) return 20;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vprocesses() {
        try {
            if (_f(_x("L3Byb2MveGVu"))) return 40;
            if (_f(_x("L3Byb2Mvdno="))) return 40;
            String _ps = _e(_x("cHMgYXV4"));
            String[] _vp = {_x("cWVtdS1nYQ=="), _x("dmJveHNlcnZpY2U="), _x("dm14dG9vbHM="),
                _x("eGVuc3RvcmU="), _x("cGFyYWxsZWxzX3Rvb2xz")};
            for (String _p : _vp) { if (_ps.contains(_p)) return 40; }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VmacAddr() {
        try {
            Enumeration<NetworkInterface> _nis = NetworkInterface.getNetworkInterfaces();
            if (_nis != null) {
                while (_nis.hasMoreElements()) {
                    NetworkInterface _ni = _nis.nextElement();
                    byte[] _ma = _ni.getHardwareAddress();
                    if (_ma != null && _ma.length >= 3) {
                        int _p = ((_ma[0] & 0xFF) << 16) | ((_ma[1] & 0xFF) << 8) | (_ma[2] & 0xFF);
                        int[] _vmMac = {0x080027, 0x000569, 0x000C29, 0x001C42, 0x005056,
                            0x00155D, 0x001C14, 0x0003FF, 0x00015D, 0x00163E,
                            0x0006B0, 0x0007B0, 0x0009B0, 0x000A27, 0x000F4B,
                            0x0011D8, 0x0021F6, 0x0022F6, 0x0023F6, 0x0025AE,
                            0x0050F2, 0x00A0A4, 0x00E0C8, 0x080020};
                        for (int _v : _vmMac) { if (_p == _v) return 20; }
                    }
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VfileAccessHistory() {
        try {
            if (_f(_x("fi91c3Ivc2hhcmUvYXBwbGljYXRpb25zLw=="))) return 15;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VthreadCount() {
        try {
            if (Runtime.getRuntime().availableProcessors() <= 2) return 35;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VmacMemsize() {
        try {
            long _mem = ((com.sun.management.OperatingSystemMXBean)
                ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
            if (_mem < 4L * 1024 * 1024 * 1024) return 15;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VmacIokit() {
        try {
            String _io = _e(_x("aW9yZWcgLWwgLXcgJ0lPUGxhdGZvcm1FeHBlcnREZXZpY2Un"));
            if (_io.contains(_x("dmlydHVhbA==")) || _io.contains(_x("dm13YXJl")) ||
                _io.contains(_x("cGFyYWxsZWxz")) || _io.contains(_x("cWVtdQ==")) ||
                _io.contains(_x("dmJveA==")) || _io.contains(_x("b3JhY2xl"))) return 100;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VmacSip() {
        try {
            String _sip = _e(_x("Y3NyY3RpbCBzdGF0dXM="));
            if (_sip.contains(_x("ZGlzYWJsZWQ=")) || _sip.contains(_x("dW5rbm93bg=="))) return 100;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VioregGrep() {
        try {
            String _io = _e(_x("aW9yZWcgLWwgfCBncmVwIC1pICd2bXdhcmV8dmJveHxxZW11fHhhcmVuc3xlbnxwYXJhbGxlbHN8b3JhY2xlfHZpcnR1YWx8aHlwZXJ2aXNvcic="));
            if (!_io.isEmpty()) return 100;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vhwmodel() {
        try {
            String _hm = _e(_x("c3lzY3RsIC1uIGh3Lm1vZGVs"));
            if (_hm.contains(_x("dmlydHVhbA==")) || _hm.contains(_x("dm13YXJl")) ||
                _hm.contains(_x("cGFyYWxsZWxz")) || _hm.contains(_x("cWVtdQ==")) ||
                _hm.contains(_x("dmJveA=="))) return 100;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VmacSys() {
        try {
            String _ms = _e(_x("c3lzY3RsIC1uIGh3Lm1lbXNpemU="));
            String _ms2 = _e(_x("c3lzY3RsIC1uIG1hY2hpbmU="));
            if (_ms.contains(_x("dmlydHVhbA==")) || _ms2.contains(_x("dmlydHVhbA==")) ||
                _ms.contains(_x("dm13YXJl")) || _ms2.contains(_x("dm13YXJl")) ||
                _ms.contains(_x("cGFyYWxsZWxz")) || _ms2.contains(_x("cGFyYWxsZWxz"))) return 100;
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VcpuidSignature() {
        try {
            String _cpu = "";
            String _os = System.getProperty(_x("b3MubmFtZQ=="));
            if (_os != null) {
                _os = _os.toLowerCase();
                if (_os.contains(_x("d2lu"))) {
                    _cpu = _w(_x("Y3B1IGdldCBuYW1l"));
                } else {
                    _cpu = _g(_x("L3Byb2MvY3B1aW5mbw=="));
                }
            }
            if (_cpu != null) {
                _cpu = _cpu.toLowerCase();
                if (_cpu.contains(_x("c2ltcGxldmlzb3I=")) || _cpu.contains(_x("aHlwZXJ2aXNvcg==")) ||
                    _cpu.contains(_x("aHZpc29y")) || _cpu.contains(_x("eWVzbHVyZQ==")) ||
                    _cpu.contains(_x("ZmFzdG1vZGVs"))) return 95;
            }
            String _vm = System.getProperty(_x("amF2YS52bS5uYW1l"));
            if (_vm != null) {
                _vm = _vm.toLowerCase();
                if (_vm.contains(_x("c2ltcGxldmlzb3I=")) || _vm.contains(_x("aHlwZXJ2aXNvcg=="))) return 95;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VkgtSignature() {
        try {
            String _cpu = "";
            String _os = System.getProperty(_x("b3MubmFtZQ=="));
            if (_os != null) {
                _os = _os.toLowerCase();
                if (_os.contains(_x("d2lu"))) {
                    _cpu = _w(_x("Y3B1IGdldCBuYW1l"));
                } else {
                    _cpu = _g(_x("L3Byb2MvY3B1aW5mbw=="));
                }
            }
            if (_cpu != null) {
                _cpu = _cpu.toLowerCase();
                if (_cpu.contains(_x("aW50ZWwga2d0")) || _cpu.contains(_x("aWd0")) ||
                    _cpu.contains(_x("dHJ1c3R5"))) return 80;
            }
            if (_nix()) {
                String _km = _e(_x("ZG1lc2cgfCBncmVwIC1pIGtndA=="));
                if (_km.contains(_x("a2d0")) || _km.contains(_x("dHJ1c3R5"))) return 80;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VpciDevices(boolean _win, boolean _nix) {
        try {
            String[] _vmDevices = {
                _x("MTVhZDA3MTA="), _x("MTVhZDA3MjA="), _x("MTVhZDA3NzA="), _x("MTVhZDA3NzQ="),
                _x("MTVhZDA3Nzg="), _x("MTVhZDA3Nzk="), _x("MTVhZDA3OTA="), _x("MTVhZDA3YTA="),
                _x("MTVhZDA3YjA="), _x("MTVhZDA3YzA="), _x("MTVhZDA3ZTA="), _x("MTVhZDA3ZjA="),
                _x("MTVhZDA4MDE="), _x("MTVhZDA4MjA="), _x("MTVhZDE5Nzc="), _x("ZmZmZTA3MTA="),
                _x("MGUwZjAwMDE="), _x("MGUwZjAwMDI="), _x("MGUwZjAwMDM="), _x("MGUwZjAwMDQ="),
                _x("MWFmNDAwMjI="), _x("MWFmNDEwMDA="), _x("MWFmNDEwMDE="), _x("MWFmNDEwMDI="),
                _x("MWFmNDEwMDM="), _x("MWFmNDEwMDQ="), _x("MWFmNDEwMDU="), _x("MWFmNDEwMDk="),
                _x("MWFmNDEwNDE="), _x("MWFmNDEwNDI="), _x("MWFmNDEwNDM="), _x("MWFmNDEwNDQ="),
                _x("MWFmNDEwNDU="), _x("MWFmNDEwNDg="), _x("MWFmNDEwNDk="), _x("MWFmNDEwNTA="),
                _x("MWFmNDEwNTI="), _x("MWFmNDEwNTM="), _x("MWFmNDEwNWE="), _x("MWFmNDExMDA="),
                _x("MWFmNDExMTA="), _x("MWFmNDFiMzY="),
                _x("MWIzNjAwMDE="), _x("MWIzNjAwMDI="), _x("MWIzNjAwMDM="), _x("MWIzNjAwMDQ="),
                _x("MWIzNjAwMDU="), _x("MWIzNjAwMDg="), _x("MWIzNjAwMDk="), _x("MWIzNjAwMGI="),
                _x("MWIzNjAwMGM="), _x("MWIzNjAwMGQ="), _x("MWIzNjAwMTA="), _x("MWIzNjAwMTE="),
                _x("MWIzNjAwMTM="), _x("MWIzNjAxMDA="),
                _x("MDYyNzAwMDE="), _x("MWQxZDFmMWY="), _x("ODA4NjU4NDU="), _x("MWQ2YjAyMDA="),
                _x("ODBlZTAwMjE="), _x("ODBlZTAwMjI="), _x("ODBlZWJlZWY="), _x("ODBlZWNhZmU="),
                _x("MWFiODQwMDA="), _x("MWFiODQwMDU="), _x("MWFiODQwMDY="),
                _x("MTBkZTBmZTc="), _x("MTBkZTBmZjc="), _x("MTBkZTExOGQ="), _x("MTBkZTExYjA="),
                _x("MWVjNjAyMGY=")
            };
            String[] _vmNames = {
                _x("dm13YXJl"), _x("dmJveA=="), _x("cWVtdQ=="), _x("dmlydGlv"),
                _x("cGFyYWxsZWxz"), _x("eGVu"), _x("aHlwZXItdg=="), _x("a3Zt"),
                _x("dmlydHVhbA=="), _x("cmVkIGhhdA=="), _x("b3JhY2xl")
            };
            String _devs = "";
            if (_win) {
                _devs = _e(_x("d21pYyBwYXRoIHdpbjMyX3BucGVudGl0eSBnZXQgZGV2aWNlaWQgL2Zvcm1hdDpsaXN0"));
            } else if (_nix) {
                _devs = _e(_x("bHNwY2k="));
            }
            if (_devs != null) {
                _devs = _devs.toLowerCase();
                for (String _v : _vmNames) {
                    if (_devs.contains(_v)) return 95;
                }
                for (String _d : _vmDevices) {
                    if (_devs.contains(_d)) return 95;
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VsystemRegisters(boolean _win, boolean _nix) {
        try {
            if (_win) {
                String _fw = _e(_x("cmVnIHF1ZXJ5IEhLTE1cSEFSRFdBUkVcREVTQ1JJUFRJT05cU3lzdGVtIC9mIEZpcm13YXJlVHlwZQ=="));
                if (_fw.contains(_x("dmlydHVhbA=="))) return 50;
                String _sc = _e(_x("c2MuZXhlIHF1ZXJ5"));
                if (_sc.contains(_x("aHlwZXItdg==")) || _sc.contains(_x("a3Zt")) ||
                    _sc.contains(_x("eGVu")) || _sc.contains(_x("cGFyYWxsZWxz"))) return 50;
            }
            if (_nix) {
                if (_f(_x("L3N5cy9oeXBlcnZpc29yL3R5cGU="))) return 50;
                if (_f(_x("L2Rldi9rdm0="))) return 50;
                if (_f(_x("L2Rldi94ZW4="))) return 50;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vazure(boolean _win, boolean _nix) {
        try {
            String _hn = "";
            if (_win) {
                _hn = System.getenv(_x("Q09NUFVURVJOQU1F"));
            } else if (_nix) {
                _hn = _e(_x("aG9zdG5hbWU="));
            }
            if (_hn != null) {
                _hn = _hn.toLowerCase().trim();
                if (_hn.startsWith(_x("cnVubmVydm0=")) && _hn.length() == 13) {
                    return 30;
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VacpiSignature() {
        try {
            String _acpi = _e(_x("d21pYyBwYXRoIHdpbjMyX3BucGVudGl0eSBnZXQgZGV2aWNlaWQgL2Zvcm1hdDpsaXN0"));
            if (_acpi != null) {
                _acpi = _acpi.toLowerCase();
                if (_acpi.contains(_x("cG5wMGEwNg==")) || _acpi.contains(_x("YWNwaVxkcmFj")) ||
                    _acpi.contains(_x("cWFlbXU=")) || _acpi.contains(_x("cWJ1cw=="))) return 100;
            }
            String _devs = _e(_x("d21pYyBwYXRoIHdpbjMyX3BucGVudGl0eSBnZXQgbmFtZSAvZm9ybWF0Omxpc3Q="));
            if (_devs != null) {
                _devs = _devs.toLowerCase();
                if (_devs.contains(_x("cG5wMGEwNg=="))) return 100;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _Vclock() {
        try {
            String _devs = _e(_x("d21pYyBwYXRoIHdpbjMyX3BucGVudGl0eSBnZXQgbmFtZSAvZm9ybWF0Omxpc3Q="));
            if (_devs != null) {
                _devs = _devs.toLowerCase();
                if (_devs.contains(_x("cGl0")) || _devs.contains(_x("YXQ=")) ||
                    _devs.contains(_x("dGltZXI=")) || _devs.contains(_x("cGNpXGQ="))) {
                    if (_devs.contains(_x("dmlydHVhbA==")) || _devs.contains(_x("cGl0MDAwMA==")) ||
                        _devs.contains(_x("cGl0MDAwMQ=="))) return 45;
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int _VamdSevMsr() {
        try {
            if (_f(_x("L2Rldi9zZXY="))) return 50;
            String _cpu = _g(_x("L3Byb2MvY3B1aW5mbw=="));
            if (_cpu != null) {
                _cpu = _cpu.toLowerCase();
                if (_cpu.contains(_x("YXV0aGVudGljYW1k")) && _cpu.contains(_x("c2V2"))) return 50;
                if (_cpu.contains(_x("c2V2LWVz")) || _cpu.contains(_x("c2V2LXNucA=="))) return 50;
            }
            String _dmesg = _e(_x("ZG1lc2cgfCBncmVwIC1pIHNldg=="));
            if (_dmesg.contains(_x("c2V2")) || _dmesg.contains(_x("YW1kIHNldg==")) ||
                _dmesg.contains(_x("c2V2LWVz")) || _dmesg.contains(_x("c2V2LXNucA=="))) return 50;
        } catch (Exception ignored) {}
        return 0;
    }
    // endregion

    private static boolean _nix() {
        String _os = System.getProperty(_x("b3MubmFtZQ=="));
        return _os != null && (_os.toLowerCase().contains(_x("bml4")) ||
            _os.toLowerCase().contains(_x("bnV4")) || _os.toLowerCase().contains(_x("bGludXg=")));
    }

    // region ── 加密与验证核心（多层混淆） ──
    private static byte[] _h(String _d, String _a) {
        try {
            Class<?> _c = Class.forName(_x("amF2YXguY3J5cHRvLk1hYw=="));
            Class<?> _kc = Class.forName(_x("amF2YXguY3J5cHRvLnNwZWMuU2VjcmV0S2V5U3BlYw=="));
            Method _gi = _c.getMethod(_x("Z2V0SW5zdGFuY2U="), String.class);
            Method _init = _c.getMethod(_x("aW5pdA=="), java.security.Key.class);
            Method _doF = _c.getMethod(_x("ZG9GaW5hbA=="), byte[].class);
            java.security.Key _k = (java.security.Key) _kc.getConstructor(byte[].class, String.class)
                .newInstance(_S().getBytes(StandardCharsets.UTF_8), _a);
            Object _m = _gi.invoke(null, _a);
            _init.invoke(_m, _k);
            return (byte[]) _doF.invoke(_m, _d.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    private static String _m5(String _in) {
        try {
            Class<?> _c = Class.forName(_x("amF2YS5zZWN1cml0eS5NZXNzYWdlRGlnZXN0"));
            Method _gi = _c.getMethod(_x("Z2V0SW5zdGFuY2U="), String.class);
            Object _md = _gi.invoke(null, _x("TUQ1"));
            Method _up = _c.getMethod(_x("dXBkYXRl"), byte[].class);
            Method _dg = _c.getMethod(_x("ZGlnZXN0"));
            _up.invoke(_md, _in.getBytes(StandardCharsets.UTF_8));
            byte[] _h = (byte[]) _dg.invoke(_md);
            return _b2h(_h);
        } catch (Exception e) {
            return "ERROR";
        }
    }

    private static String _b2h(byte[] _b) {
        StringBuilder _sb = new StringBuilder();
        for (byte _v : _b) {
            _sb.append(String.format("%02X", _v & 0xFF));
        }
        return _sb.toString();
    }

    private static String _ae(String plain) {
        try {
            Class<?> cc = Class.forName(_x("amF2YXguY3J5cHRvLkNpcGhlcg=="));
            Class<?> ks = Class.forName(_x("amF2YXguY3J5cHRvLnNwZWMuU2VjcmV0S2V5U3BlYw=="));
            Class<?> ivc = Class.forName(_x("amF2YXguY3J5cHRvLnNwZWMuSXZQYXJhbWV0ZXJTcGVj"));
            Object key = ks.getConstructor(byte[].class, String.class).newInstance(_ek(), _x("QUVT"));
            Object iv = ivc.getConstructor(byte[].class).newInstance(_iv());
            Method gi = cc.getMethod(_x("Z2V0SW5zdGFuY2U="), String.class);
            Object c = gi.invoke(null, _x("QUVTL0NCQy9QS0NTNVBhZGRpbmc="));
            Method init = cc.getMethod(_x("aW5pdA=="), int.class, java.security.Key.class, java.security.spec.AlgorithmParameterSpec.class);
            init.invoke(c, 1, key, iv);
            Method doF = cc.getMethod(_x("ZG9GaW5hbA=="), byte[].class);
            byte[] enc = (byte[]) doF.invoke(c, plain.getBytes(StandardCharsets.UTF_8));
            Class<?> b64 = Class.forName(_x("amF2YS51dGlsLkJhc2U2NA=="));
            Method ge = b64.getMethod(_x("Z2V0RW5jb2Rlcg=="));
            Object encoder = ge.invoke(null);
            Method et = encoder.getClass().getMethod(_x("ZW5jb2RlVG9TdHJpbmc="), byte[].class);
            return (String) et.invoke(encoder, enc);
        } catch (Exception e) { return null; }
    }

    private static String _ad(String cipher) {
        try {
            Class<?> b64 = Class.forName(_x("amF2YS51dGlsLkJhc2U2NA=="));
            Method gd = b64.getMethod(_x("Z2V0RGVjb2Rlcg=="));
            Object decoder = gd.invoke(null);
            Method dt = decoder.getClass().getMethod(_x("ZGVjb2Rl"), String.class);
            byte[] enc = (byte[]) dt.invoke(decoder, cipher);

            Class<?> cc = Class.forName(_x("amF2YXguY3J5cHRvLkNpcGhlcg=="));
            Class<?> ks = Class.forName(_x("amF2YXguY3J5cHRvLnNwZWMuU2VjcmV0S2V5U3BlYw=="));
            Class<?> ivc = Class.forName(_x("amF2YXguY3J5cHRvLnNwZWMuSXZQYXJhbWV0ZXJTcGVj"));
            Object key = ks.getConstructor(byte[].class, String.class).newInstance(_ek(), _x("QUVT"));
            Object iv = ivc.getConstructor(byte[].class).newInstance(_iv());
            Method gi = cc.getMethod(_x("Z2V0SW5zdGFuY2U="), String.class);
            Object c = gi.invoke(null, _x("QUVTL0NCQy9QS0NTNVBhZGRpbmc="));
            Method init = cc.getMethod(_x("aW5pdA=="), int.class, java.security.Key.class, java.security.spec.AlgorithmParameterSpec.class);
            init.invoke(c, 2, key, iv);
            Method doF = cc.getMethod(_x("ZG9GaW5hbA=="), byte[].class);
            byte[] dec = (byte[]) doF.invoke(c, enc);
            return new String(dec, StandardCharsets.UTF_8);
        } catch (Exception e) { return null; }
    }
    // endregion

    // region ── 设备码生成（仅4项最稳定硬件特征） ──
    public static String _D() {
        if (_C != null) return _C;
        try {
            String _sep = _x("fA==");
            StringBuilder _sb = new StringBuilder();
            _sb.append(_mac()).append(_sep);
            _sb.append(_cp()).append(_sep);
            _sb.append(_bs()).append(_sep);
            _sb.append(_gpu());
            _C = _m5(_sb.toString()).toUpperCase();
            return _C;
        } catch (Exception e) {
            return "00000000000000000000000000000000";
        }
    }
    // endregion

    // region ── 密钥生成与验证（多层HMAC） ──
    public static String _G(String _dc, String _exp) {
        byte[] _r = _h(_dc + _exp, _x("SG1hY1NIQTI1Ng=="));
        return _r != null ? _b2h(_r).toUpperCase() : null;
    }

    public static boolean _T(String _dc, String _k) {
        if (_dc == null || _k == null) return false;
        try {
            String _kp = _k.trim().toUpperCase();
            int _di = _kp.indexOf('-');
            if (_di < 0 || _di != 4) return false;
            String _exp = _kp.substring(0, _di);
            String _sig = _kp.substring(_di + 1);
            if (_exp.length() != 4 || _sig.length() < 10) return false;
            String _e = _G(_dc, _exp);
            return _e != null && _sig.length() <= _e.length() 
                && _e.substring(0, _sig.length()).equalsIgnoreCase(_sig);
        } catch (Exception e) {
            return false;
        }
    }

    public static int _PE(String _k) {
        try {
            String _kp = _k.trim().toUpperCase();
            int _di = _kp.indexOf('-');
            if (_di != 4) return -999;
            String _exp = _kp.substring(0, _di);
            if (_exp.equals("FFFF")) return -1;
            return Integer.parseInt(_exp, 16);
        } catch (Exception e) {
            return -999;
        }
    }

    public static long _ST() {
        try {
            HttpClient _c = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
            HttpRequest _rq = HttpRequest.newBuilder()
                .uri(URI.create("https://uapis.cn/api/v1/misc/worldtime?city=Asia/Shanghai"))
                .timeout(Duration.ofSeconds(4))
                .GET()
                .build();
            HttpResponse<String> _rp = _c.send(_rq, HttpResponse.BodyHandlers.ofString());
            if (_rp.statusCode() == 200) {
                String _b = _rp.body();
                int _t = _b.indexOf("\"timestamp_unix\"");
                if (_t < 0) return System.currentTimeMillis() / 1000;
                int _c0 = _b.indexOf(':', _t);
                if (_c0 < 0) return System.currentTimeMillis() / 1000;
                int _c1 = _b.indexOf(',', _c0);
                if (_c1 < 0) _c1 = _b.indexOf('}', _c0);
                if (_c1 < 0) return System.currentTimeMillis() / 1000;
                String _v = _b.substring(_c0 + 1, _c1).trim();
                return Long.parseLong(_v);
            }
        } catch (Exception _ex) {}
        return System.currentTimeMillis() / 1000;
    }
    // endregion

    // region ── 许可证持久化 ──
    public static void _P(String _k, boolean _ag) {
        _L = _k;
        _V = true;
        _A = _ag;
        _AT = System.currentTimeMillis() / 1000;
        _ED = _PE(_k);
        try {
            File _f = new File(Manager.getFolder(), _F);
            StringBuilder _sb = new StringBuilder();
            _sb.append(_D()).append('\n');
            _sb.append(_k).append('\n');
            _sb.append(_x("YWdyZWVkOg==")).append(_ag).append('\n');
            _sb.append(_x("YWN0aXZhdGVkOg==")).append(_AT).append('\n');
            _sb.append(_x("ZXhwaXJ5Og==")).append(_ED);
            String _enc = _ae(_sb.toString());
            if (_enc == null) return;
            try (PrintWriter _pw = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(_f), StandardCharsets.UTF_8))) {
                _pw.println(_enc);
            }
        } catch (Exception ignored) {}
    }

    public static Object _R() {
        if (_H) {
            return getLicenseInfo();
        }
        _H = true;
        try {
            File _f = new File(Manager.getFolder(), _F);
            if (!_f.exists()) return new Object[]{false, null, false, 0L, 0};
            String _sd, _sk, _sa, _st, _se;
            try (BufferedReader _br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(_f), StandardCharsets.UTF_8))) {
                String _l1 = _br.readLine();
                if (_l1 == null) return new Object[]{false, null, false, 0L, 0};
                String _l2 = _br.readLine();
                if (_l2 == null || _l1.length() > 80) {
                    String _dec = _ad(_l1);
                    if (_dec == null) return new Object[]{false, null, false, 0L, 0};
                    String[] _ls = _dec.split("\n");
                    if (_ls.length < 5) return new Object[]{false, null, false, 0L, 0};
                    _sd = _ls[0]; _sk = _ls[1]; _sa = _ls[2]; _st = _ls[3]; _se = _ls[4];
                } else {
                    _sd = _l1; _sk = _l2;
                    _sa = _br.readLine(); _st = _br.readLine(); _se = _br.readLine();
                }
            }
                boolean _ag = false;
                if (_sa != null && _sa.startsWith(_x("YWdyZWVkOg=="))) {
                    _ag = Boolean.parseBoolean(_sa.substring(7));
                }
                long _at = 0;
                if (_st != null && _st.startsWith(_x("YWN0aXZhdGVkOg=="))) {
                    try { _at = Long.parseLong(_st.substring(10)); } catch (Exception _ig) {}
                }
                int _ed = -999;
                if (_se != null && _se.startsWith(_x("ZXhwaXJ5Og=="))) {
                    try { _ed = Integer.parseInt(_se.substring(7)); } catch (Exception _ig) {}
                }
                if (_sd != null && _sk != null) {
                    if (_sd.trim().equals(_D())) {
                        if (_T(_D(), _sk.trim())) {
                            _L = _sk.trim();
                            _V = true;
                            _A = _ag;
                            _AT = _at;
                            _ED = _PE(_sk.trim());
                            if (_ED >= 0) {
                                long _now = _ST();
                                long _expiryTime = _at + (_ED * 86400L);
                                if (_now > _expiryTime) {
                                    _V = false;
                                    return new Object[]{false, _sk.trim(), _ag, _at, _ed};
                                }
                            }
                            return new Object[]{true, _sk.trim(), _ag, _at, _ed};
                        }
                    }
                }
                _L = _sk;
                _A = _ag;
                _AT = _at;
                _ED = _ed;
                return new Object[]{false, _sk, _ag, _at, _ed};
        } catch (Exception ignored) {}
        return new Object[]{false, null, false, 0L, 0};
    }

    public static Object getLicenseInfo() {
        return new Object[]{_V, _L, _A, _AT, _ED};
    }

    public static boolean isExpired() {
        if (!_V) return true;
        if (_ED < 0) return false;
        if (_ED == 0) return true;
        long _now = _ST();
        long _expiryTime = _AT + (_ED * 86400L);
        return _now > _expiryTime;
    }

    public static long getExpiryTimestamp() {
        if (_ED < 0) return -1L;
        if (_ED == 0) return 0L;
        return _AT + (_ED * 86400L);
    }

    public static int getExpiryDays() {
        return _ED;
    }
    // endregion

    // region ── 反射调度入口（多层间接调用） ──
    private static final String[] _DI = {
        _x("Z2VuZXJhdGVEZXZpY2VDb2Rl"), _x("Z2VuZXJhdGVLZXk="),
        _x("dmVyaWZ5S2V5"), _x("c2F2ZUxpY2Vuc2VJbmZv"),
        _x("aXNWZXJpZmllZA=="), _x("Z2V0Q2FjaGVkTGljZW5zZUtleQ=="),
        _x("aXNBZ3JlZW1lbnRBY2NlcHRlZA=="), _x("cmVzZXQ="),
        _x("bG9hZExpY2Vuc2VJbmZv"), _x("cGFyc2VFeHBpcnk="),
        _x("ZmV0Y2hTZXJ2ZXJUaW1l"), _x("aXNFeHBpcmVk"),
        _x("Z2V0RXhwaXJ5VGltZXN0YW1w"), _x("Z2V0RXhwaXJ5RGF5cw=="),
        _x("Z2V0TGljZW5zZUluZm8=")
    };

    private static Object _dispatch(int _op, Object... _args) {
        try {
            switch (_op) {
                case 0: return _D();
                case 1: return _args.length > 1 ? _G((String)_args[0], (String)_args[1]) : _G((String)_args[0], "FFFF");
                case 2: return _T((String)_args[0], (String)_args[1]);
                case 3: _P((String)_args[0], (Boolean)_args[1]); return null;
                case 4: return _V();
                case 5: return _L();
                case 6: return _A();
                case 7: _Z(); return null;
                case 8: {
                    Object[] _r = (Object[]) _R();
                    return new Object[]{_r[0], _r[1], _r[2], _r[3], _r[4]};
                }
                case 9: return _PE((String)_args[0]);
                case 10: return _ST();
                case 11: return isExpired();
                case 12: return getExpiryTimestamp();
                case 13: return getExpiryDays();
                case 14: return getLicenseInfo();
                case 15: return _VT((String)_args[0], (String)_args[1]);
                default: return null;
            }
        } catch (Throwable _t) {
            if (_op == 4) return Boolean.FALSE;
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Object _entry(int _op, Object... _args) {
        if (!_J()) return null;
        return _dispatch(_op, _args);
    }
    // endregion

    // region ── 内部状态（单出口多检查点） ──
    private static final long _EC = _IC * 7 + 13;
    static boolean _isAlive() { return _V && _J() && (_IC * 7 + 13 == _EC); }

    // region ── 诱饵方法（反逆向混淆） ──
    private static final long _DCK = _IC ^ 0xDEADBEEFCAFEBABEL;
    public static boolean _chk() { return _DCK != 0 && _J(); }
    public static long _token() { return _IC * System.currentTimeMillis() % _DCK; }
    public static int _magic() { return (int)(_IC & 0xFFFFFFFF) ^ 0x5A5A; }
    public static byte[] _garbage() {
        byte[] _b = new byte[64];
        for (int _i = 0; _i < 64; _i++) _b[_i] = (byte)(_i ^ (_IC >> (_i % 8)));
        return _b;
    }
    // endregion

    public static boolean _V() { return _J() ? _V : false; }
    public static String _L() { return _J() ? _L : null; }
    public static boolean _A() { return _J() ? _A : false; }

    public static void _Z() {
        _V = false;
        _H = false;
        _C = null;
        _L = null;
        _A = false;
        _AT = 0;
        _ED = 0;
        _JC = null;
        try {
            File _f = new File(Manager.getFolder(), _F);
            if (_f.exists()) _f.delete();
        } catch (Exception ignored) {}
    }

    public static final int _VE_OK = 0;
    public static final int _VE_FMT = 1;
    public static final int _VE_VFY = 2;
    public static final int _VE_EXP = 3;
    public static final int _VE_API = 4;
    public static final int _VE_DEV = 5;

    public static int _VT(String _dc, String _k) {
        if (_dc == null || _k == null || _k.trim().isEmpty()) return _VE_FMT;
        try {
            String _kp = _k.trim().toUpperCase();
            int _di = _kp.indexOf('-');
            if (_di != 4) return _VE_FMT;
            String _exp = _kp.substring(0, _di);
            String _sig = _kp.substring(_di + 1);
            if (_exp.length() != 4 || _sig.length() < 10) return _VE_FMT;
            String _e = _G(_dc, _exp);
            if (_e == null || _sig.length() > _e.length()
                || !_e.substring(0, _sig.length()).equalsIgnoreCase(_sig))
                return _VE_VFY;
            int _ed = _exp.equals("FFFF") ? -1 : Integer.parseInt(_exp, 16);
            if (_ed >= 0) {
                long _now;
                try {
                    _now = _ST();
                } catch (Exception _ig) {
                    return _VE_API;
                }
                long _at = _AT > 0 ? _AT : (System.currentTimeMillis() / 1000);
                if (_now > _at + (_ed * 86400L)) return _VE_EXP;
            }
            return _VE_OK;
        } catch (Exception _e) {
            return _VE_VFY;
        }
    }

    @Deprecated
    public static String generateDeviceCode() { return _J() ? _D() : "00000000000000000000000000000000"; }
    @Deprecated
    public static String generateKey(String dc) { return _J() ? _G(dc, "FFFF") : null; }
    @Deprecated
    public static boolean verifyKey(String dc, String k) { return _J() && _T(dc, k); }
    @Deprecated
    public static void saveLicenseInfo(String k, boolean ag) { if (_J()) _P(k, ag); }
    @Deprecated
    public static boolean isVerified() { return _J() && _V(); }
    @Deprecated
    public static String getCachedLicenseKey() { return _J() ? _L() : null; }
    @Deprecated
    public static boolean isAgreementAccepted() { return _J() && _A(); }
    @Deprecated
    public static void reset() { if (_J()) _Z(); }
    @Deprecated
    public static LicenseInfo loadLicenseInfo() {
        if (!_J()) return new LicenseInfo(false, null, false);
        Object[] _r = (Object[]) _R();
        return new LicenseInfo((Boolean)_r[0], (String)_r[1], (Boolean)_r[2]);
    }
    // endregion

    public static class LicenseInfo {
        public final boolean _v;
        public final String _k;
        public final boolean _a;

        public LicenseInfo(boolean _v, String _k, boolean _a) {
            this._v = _v;
            this._k = _k;
            this._a = _a;
        }

        @Deprecated
        public boolean isVerified() { return _v; }
        @Deprecated
        public String getKey() { return _k; }
        @Deprecated
        public boolean isAgreed() { return _a; }
    }
}