package dev.kissoo.mod.modules.impl.combat;

import hoprc.obf.neko.NekoExclude;
import hoprc.obf.zkm.ZKMIndy;
import hoprc.obf.neko.NekoInclude;

@NekoExclude
public final class AuthEndpointLayerB {
    private AuthEndpointLayerB() {}

    @ZKMIndy
    @NekoInclude
    public static String segment() {
        return decode(new int[]{}, 0x37);
    }

    @ZKMIndy
    @NekoInclude
    private static String decode(int[] data, int key) {
        StringBuilder sb = new StringBuilder(data.length);
        for (int i = 0; i < data.length; i++) {
            sb.append((char) (data[i] ^ ((key + i * 11) & 0xFF)));
        }
        return sb.toString();
    }
}
