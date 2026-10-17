package dev.kissoo.loader;

import hoprc.obf.neko.NekoExclude;

@NekoExclude
public final class ForeverLoader {
    private ForeverLoader() {
    }

    public static void main(String[] args) {
        LoaderBootstrap.start(args);
    }
}
