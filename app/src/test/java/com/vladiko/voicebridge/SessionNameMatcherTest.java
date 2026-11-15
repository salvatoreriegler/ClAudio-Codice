package com.vladiko.voicebridge;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class SessionNameMatcherTest {
    private SessionNameMatcher.Candidate chat(String id, String name) {
        return new SessionNameMatcher.Candidate(id, name);
    }
    private String find(String spoken, SessionNameMatcher.Candidate... chats) {
        SessionNameMatcher.Candidate result = SessionNameMatcher.INSTANCE.match(spoken, Arrays.asList(chats));
        return result == null ? null : result.getSid();
    }
    @Test public void stripsWindowsAndWslAnnotations() {
        assertEquals("wsl", find("SECURDOC 1",
            chat("windows", "Claude - SECURDOC 2 [2d9dd9a6]"),
            chat("wsl", "Claude WSL Ubuntu - SECURDOC 1 [98f0e69c]")));
        assertEquals("windows", find("PRIMA 2", chat("windows", "Claude - PRIMA 2 [ca178f4e]")));
        assertEquals("ubuntu22", find("SECURDOC 1", chat("ubuntu22", "Claude WSL Ubuntu-22.04 — SECURDOC 1 [98f0e69c]")));
    }
    @Test public void acceptsSpokenNumbersAndRecognitionSpacing() {
        SessionNameMatcher.Candidate item = chat("a", "Claude WSL Ubuntu - SECURDOC 1 [98f0e69c]");
        assertEquals("a", find("securdoc uno", item));
        assertEquals("a", find("secur doc 1", item));
        assertEquals("a", find("SECURDOC1", item));
    }
    @Test public void preservesNumbersAndExactForkNames() {
        assertNull(find("SECURDOC 2", chat("a", "Claude - SECURDOC 1 [98f0e69c]")));
        SessionNameMatcher.Candidate main = chat("main", "Claude - RARE - Dashboard [cecf6aca]");
        SessionNameMatcher.Candidate fork = chat("fork", "Claude - RARE - Dashboard (fork) [2d9dd9a6]");
        assertEquals("main", find("RARE Dashboard", fork, main));
        assertEquals("fork", find("RARE Dashboard fork", main, fork));
        assertNull(find("Dashboard", main, fork));
    }
    @Test public void rejectsDuplicateNamesAndAllowsAnExplicitOrigin() {
        SessionNameMatcher.Candidate windows = chat("windows", "Claude - SECURDOC 1 [cecf6aca]");
        SessionNameMatcher.Candidate wsl = chat("wsl", "Claude WSL Ubuntu - SECURDOC 1 [98f0e69c]");
        assertNull(find("SECURDOC 1", windows, wsl));
        assertNull(find("SECURDOC 1", wsl, windows));
        assertEquals("wsl", find("Claude WSL Ubuntu SECURDOC 1", windows, wsl));
    }
    @Test public void uniquePartialNamesWorkButSharedPrefixesDoNot() {
        SessionNameMatcher.Candidate one = chat("one", "Claude - SECURDOC 1 [cecf6aca]");
        SessionNameMatcher.Candidate two = chat("two", "Claude WSL Ubuntu - SECURDOC 2 [98f0e69c]");
        assertEquals("one", find("SECURDOC", one));
        assertNull(find("SECURDOC", one, two));
        assertNull(find("Claude", one));
        assertNull(find("progetto inesistente", one, two));
    }
    @Test public void normalizesAccentsAndToleratesOnlyDistinctTypos() {
        assertEquals("a", find("contabilita", chat("a", "Claude Windows - Contabilità [cecf6aca]")));
        assertEquals("a", find("securdok 1", chat("a", "Claude - SECURDOC 1 [cecf6aca]")));
        assertNull(find("alfa sito", chat("a", "Alfa zito"), chat("b", "Alfa rito")));
    }
    @Test public void searchesBeyondTheTenMostRecentChats() {
        List<SessionNameMatcher.Candidate> chats = new ArrayList<>();
        for (int i = 0; i < 12; i++) chats.add(chat("other" + i, "Claude - Progetto " + i));
        chats.add(chat("wanted", "Claude WSL Ubuntu - SECURDOC 1 [98f0e69c]"));
        assertEquals("wanted", SessionNameMatcher.INSTANCE.match("SECURDOC 1", chats).getSid());
    }
}
