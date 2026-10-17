package dev.kissoo.api.utils.notebot.song;

import java.util.*;

public class Song {
    private final Map<Integer, List<Note>> notesMap;
    private final String title;
    private int lastTick;
    private final Set<Note> requirements = new HashSet<>();
    private boolean finishedLoading = false;

    public Song(Map<Integer, List<Note>> notesMap, String title) {
        this.notesMap = notesMap;
        this.title = title;
    }

    public void put(int tick, Note note) {
        this.notesMap.computeIfAbsent(tick, k -> new ArrayList<>()).add(note);
    }

    public List<Note> get(int tick) {
        return this.notesMap.getOrDefault(tick, Collections.emptyList());
    }

    public void finishLoading() {
        if (finishedLoading) throw new IllegalStateException("Song has already finished loading!");

        this.lastTick = notesMap.isEmpty() ? 0 : Collections.max(notesMap.keySet());
        for (List<Note> notes : notesMap.values()) {
            requirements.addAll(notes);
        }

        finishedLoading = true;
    }

    public Set<Note> getRequirements() {
        if (!finishedLoading) throw new IllegalStateException("Song is still loading!");
        return requirements;
    }

    public int getLastTick() {
        if (!finishedLoading) throw new IllegalStateException("Song is still loading!");
        return lastTick;
    }

    public String getTitle() {
        return title;
    }
}
