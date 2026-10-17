package dev.perish.mod.utils.music;

public class UserProfile {
    public final long userId;
    public final String nickname;
    public final String avatarUrl;

    public UserProfile(long userId, String nickname, String avatarUrl) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }
}
