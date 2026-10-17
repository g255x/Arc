package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

import java.nio.charset.StandardCharsets;

@NekoInclude
final class NoiseMesh {
    private NoiseMesh() {
    }

    static int touch(String s) {
        int a = NoiseFold.fold(s);
        int b = NoiseBloom.bloom(a, s.length());
        int c = NoiseMirror.mirror(s);
        int d = NoiseLatch.latch(a ^ b ^ c);
        int e = ObfDecoyWeaver.weave(s, d);
        int f = ObfDecoyAtlas.trace(s, e);
        int g = ObfDecoyGraph.walk(s, f);
        int h = ObfDecoyState.pulse(s, g);
        int i = ObfDecoyScript.score(s, h);
        int j = ObfDecoyCipher.curl(s, i ^ e);
        int k = ObfDecoyMatrix.matrix(s, j ^ f);
        int l = ObfDecoyLedger.balance(s, k ^ g);
        int m = ObfDecoyMaze.route(s, l ^ h);
        int n = ObfDecoyOracle.ask(s, m ^ i);
        int o = ObfDecoyRibbon.braid(s, n ^ j);
        int p = ObfDecoyClockwork.tick(s, o ^ k);
        return NoiseSpiral.spin(d)
                ^ NoiseVault.seal(s, d)
                ^ NoiseNeedle.thread(s)
                ^ e ^ f ^ g ^ h ^ i ^ j ^ k ^ l ^ m ^ n ^ o ^ p;
    }
}

final class NoiseFold {
    static int fold(String s) {
        int x = 0x7f4a7c15;
        for (char c : s.toCharArray()) x = (x << 4) ^ (x >>> 3) ^ c;
        return x;
    }
}

final class NoiseBloom {
    static int bloom(int a, int b) {
        int x = a ^ 0x5bd1e995;
        for (int i = 0; i < 6; i++) x = Integer.rotateLeft(x + b + i * 31, i + 3);
        return x;
    }
}

final class NoiseMirror {
    static int mirror(String s) {
        int x = 0x10203040;
        for (int i = s.length() - 1; i >= 0; i--) x ^= Integer.rotateRight(s.charAt(i) * 131, i & 15);
        return x;
    }
}

final class NoiseLatch {
    static int latch(int v) {
        v ^= v << 13;
        v ^= v >>> 17;
        v ^= v << 5;
        return v;
    }
}

final class NoiseSpiral {
    static int spin(int v) {
        int x = v;
        for (int i = 1; i < 9; i++) x = Integer.rotateLeft(x ^ (i * 0x9e37), i);
        return x;
    }
}

final class NoiseVault {
    static int seal(String s, int seed) {
        int x = seed ^ 0x6a09e667;
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) x = (x * 257) ^ (b & 0xff);
        return x;
    }
}

final class NoiseNeedle {
    static int thread(String s) {
        int x = 0x3c6ef372;
        for (int i = 0; i < s.length(); i += 2) x += (s.charAt(i) ^ i) * 17;
        return Integer.reverse(x);
    }
}
