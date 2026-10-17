package dev.kissoo.loader;

record AuthSession(String token, String owner, long expiresAt, boolean personalized) {
    boolean valid() {
        return token != null && !token.isBlank();
    }

    String displayOwner() {
        return owner == null || owner.isBlank() ? "Verified user" : owner;
    }
}
