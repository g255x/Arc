package dev.kissoo.loader;

record GuardFinding(String category, String name, String advice, GuardSeverity severity) {
    boolean blocking() {
        return severity == GuardSeverity.BLOCK;
    }
}
