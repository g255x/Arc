package dev.kissoo.loader;

import java.util.List;

record GuardReport(List<GuardFinding> findings) {
    boolean blocked() {
        return findings != null && findings.stream().anyMatch(GuardFinding::blocking);
    }

    boolean hasWarnings() {
        return findings != null && findings.stream().anyMatch(f -> !f.blocking());
    }

    String summary() {
        if ((findings == null || findings.isEmpty())) return "No environment notice.";
        StringBuilder sb = new StringBuilder();
        for (GuardFinding f : findings) {
            sb.append(f.blocking() ? "[BLOCK] " : "[NOTICE] ")
                    .append("[").append(f.category()).append("] ")
                    .append(f.name()).append(" - ")
                    .append(f.advice()).append('\n');
        }
        return sb.toString();
    }
}
