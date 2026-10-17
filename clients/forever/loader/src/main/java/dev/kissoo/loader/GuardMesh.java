package dev.kissoo.loader;

import hoprc.obf.neko.NekoInclude;

@NekoInclude
final class GuardMesh {
    private GuardMesh() {
    }

    static boolean crossCheck() {
        try {
            String a = Fingerprint.id();
            String b = Fingerprint.id();
            int c = CanaryA.mix(a) ^ CanaryB.mix(b) ^ CanaryC.mix(a + b) ^ NoiseMesh.touch(a);
            return !a.isBlank() && a.equals(b) && c != 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static String marker() {
        try {
            return Integer.toHexString(CanaryA.mix(Fingerprint.id()) ^ CanaryC.mix(SessionEntropy.nonce()) ^ NoiseMesh.touch("marker"));
        } catch (Throwable ignored) {
            return "0";
        }
    }
}
