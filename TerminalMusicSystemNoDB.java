
import java.io.*;
import java.util.*;

/**
 * TerminalMusicSystemNoDB.java
 *
 * Single-file terminal music system WITHOUT SQLite/JDBC.
 * Persists data to 'music_data.ser' using Java serialization.
 *
 * How to compile:
 *   javac TerminalMusicSystemNoDB.java
 *
 * How to run:
 *   java TerminalMusicSystemNoDB
 *
 * Default admin: username=admin password=admin123
 *
 * Note: The file music_data.ser will be created in the same folder.
 */
public class TerminalMusicSystemNoDB {

    // ---------- Data model (Serializable) ----------
    private static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        String username;
        String password;
        boolean isAdmin;
        Set<Integer> followedArtistIds = new HashSet<>();

        User(int id, String username, String password, boolean isAdmin) {
            this.id = id; this.username = username; this.password = password; this.isAdmin = isAdmin;
        }
        @Override public String toString() {
            return id + ": " + username + (isAdmin ? " (admin)" : "");
        }
    }

    private static class Artist implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        String name;
        Artist(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return id + ": " + name; }
    }

    private static class Song implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        String title;
        int artistId;
        int durationSeconds;
        Song(int id, String title, int artistId, int durationSeconds) {
            this.id = id; this.title = title; this.artistId = artistId; this.durationSeconds = durationSeconds;
        }
        @Override public String toString() { return id + ": " + title + " (" + durationSeconds + "s)"; }
    }

    private static class Playlist implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        int userId;
        String name;
        List<Integer> songIds = new ArrayList<>();
        Playlist(int id, int userId, String name) { this.id = id; this.userId = userId; this.name = name; }
        @Override public String toString() { return id + ": " + name + " (owner=" + userId + ")"; }
    }

    // Container for all data
    private static class DB implements Serializable {
        private static final long serialVersionUID = 1L;
        Map<Integer, User> users = new HashMap<>();
        Map<Integer, Artist> artists = new HashMap<>();
        Map<Integer, Song> songs = new HashMap<>();
        Map<Integer, Playlist> playlists = new HashMap<>();
        int nextUserId = 1, nextArtistId = 1, nextSongId = 1, nextPlaylistId = 1;
    }

    // ---------- App state ----------
    private static final String DATA_FILE = "music_data.ser";
    private DB db;
    private Scanner scanner = new Scanner(System.in);
    private User currentUser = null;

    // ---------- Main ----------
    public static void main(String[] args) {
        TerminalMusicSystemNoDB app = new TerminalMusicSystemNoDB();
        app.start();
    }

    // ---------- Startup / persistence ----------
    private void start() {
        loadOrCreate();
        seedIfEmpty();
        mainLoop();
        save();
        System.out.println("Exiting. Data saved to " + DATA_FILE);
    }

    @SuppressWarnings("unchecked")
    private void loadOrCreate() {
        File f = new File(DATA_FILE);
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                db = (DB) ois.readObject();
                System.out.println("Loaded data from " + DATA_FILE);
            } catch (Exception e) {
                System.err.println("Failed to load data file: " + e.getMessage());
                db = new DB();
            }
        } else {
            db = new DB();
        }
    }

    private void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(db);
        } catch (Exception e) {
            System.err.println("Failed to save data: " + e.getMessage());
        }
    }

    private void seedIfEmpty() {
        if (db.users.isEmpty()) {
            User admin = new User(db.nextUserId++, "admin", "admin123", true);
            db.users.put(admin.id, admin);
        }
        if (db.artists.isEmpty()) {
            int a1 = db.nextArtistId++; db.artists.put(a1, new Artist(a1, "The Echoes"));
            int a2 = db.nextArtistId++; db.artists.put(a2, new Artist(a2, "Solaris"));
            int a3 = db.nextArtistId++; db.artists.put(a3, new Artist(a3, "Blue Moon"));
            int a4 = db.nextArtistId++; db.artists.put(a4, new Artist(a4, "Acoustic Tales"));
            addSongInternal("Waves", a1, 120);
            addSongInternal("Sunrise", a2, 150);
            addSongInternal("Midnight Blues", a3, 200);
            addSongInternal("Campfire", a4, 180);
            addSongInternal("Horizon", a1, 215);
            addSongInternal("Orbit", a2, 175);
            save();
        }
    }

    // ---------- Main menu ----------
    private void mainLoop() {
        while (true) {
            printMainMenu();
            String c = prompt("Choice");
            switch (c) {
                case "1": register(); break;
                case "2": login(); break;
                case "3": logout(); break;
                case "4": listSongs(); break;
                case "5": searchSongs(); break;
                case "6": followArtist(); break;
                case "7": managePlaylists(); break;
                case "8": playSong(); break;
                case "9":
                    if (requireLogin() && currentUser.isAdmin) adminMenu();
                    else System.out.println("Admin only. Login as admin.");
                    break;
                case "0": return;
                default: System.out.println("Unknown option.");
            }
        }
    }

    private void printMainMenu() {
        System.out.println("\n=== Terminal Music System (No DB) ===");
        System.out.println("1) Register");
        System.out.println("2) Login");
        System.out.println("3) Logout");
        System.out.println("4) List songs");
        System.out.println("5) Search songs");
        System.out.println("6) Follow artist");
        System.out.println("7) Playlists (create/manage)");
        System.out.println("8) Play a song (simulate)");
        System.out.println("9) Admin menu (admin only)");
        System.out.println("0) Exit");
        if (currentUser != null) System.out.println("Logged in as: " + currentUser.username + (currentUser.isAdmin ? " (admin)" : ""));
    }

    // ---------- Helpers ----------
    private String prompt(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    private boolean requireLogin() {
        if (currentUser == null) { System.out.println("Please login first."); return false; }
        return true;
    }

    // ---------- Authentication ----------
    private void register() {
        System.out.println("--- Register ---");
        String username = prompt("Choose username");
        if (username.isEmpty()) { System.out.println("Username required."); return; }
        for (User u : db.users.values()) if (u.username.equals(username)) { System.out.println("Username taken."); return; }
        String password = prompt("Choose password");
        if (password.isEmpty()) { System.out.println("Password required."); return; }
        User u = new User(db.nextUserId++, username, password, false);
        db.users.put(u.id, u);
        save();
        System.out.println("Registered. You can login now.");
    }

    private void login() {
        System.out.println("--- Login ---");
        String username = prompt("Username");
        String password = prompt("Password");
        for (User u : db.users.values()) {
            if (u.username.equals(username) && u.password.equals(password)) {
                currentUser = u;
                System.out.println("Welcome, " + u.username + "!");
                return;
            }
        }
        System.out.println("Invalid credentials.");
    }

    private void logout() {
        if (currentUser != null) {
            System.out.println("Goodbye, " + currentUser.username);
            currentUser = null;
        } else System.out.println("Not logged in.");
    }

    // ---------- Admin ----------
    private void adminMenu() {
        while (true) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1) Add artist");
            System.out.println("2) Add song");
            System.out.println("3) Remove song");
            System.out.println("4) List users");
            System.out.println("5) Delete user");
            System.out.println("0) Back");
            String o = prompt("Choice");
            switch (o) {
                case "1": addArtist(); break;
                case "2": addSong(); break;
                case "3": removeSong(); break;
                case "4": listUsers(); break;
                case "5": deleteUser(); break;
                case "0": return;
                default: System.out.println("Unknown"); break;
            }
        }
    }

    private void addArtist() {
        String name = prompt("Artist name");
        if (name.isEmpty()) { System.out.println("Name required."); return; }
        for (Artist a : db.artists.values()) if (a.name.equalsIgnoreCase(name)) { System.out.println("Artist exists."); return; }
        int id = db.nextArtistId++;
        db.artists.put(id, new Artist(id, name));
        save();
        System.out.println("Artist added: " + name);
    }

    private void addSong() {
        String title = prompt("Song title");
        if (title.isEmpty()) { System.out.println("Title required."); return; }
        String artistName = prompt("Artist name (existing or new)");
        if (artistName.isEmpty()) { System.out.println("Artist required."); return; }
        int artistId = findOrCreateArtistByName(artistName);
        int dur;
        try { dur = Integer.parseInt(prompt("Duration in seconds (e.g. 180)")); } catch (Exception e) { System.out.println("Invalid duration."); return; }
        int sid = db.nextSongId++;
        db.songs.put(sid, new Song(sid, title, artistId, dur));
        save();
        System.out.println("Song added: " + title + " by " + artistName);
    }

    private void removeSong() {
        try {
            int id = Integer.parseInt(prompt("Song ID to remove"));
            if (db.songs.remove(id) != null) {
                // also remove from playlists
                for (Playlist p : db.playlists.values()) p.songIds.removeIf(sid -> sid == id);
                save();
                System.out.println("Song removed.");
            } else System.out.println("Song not found.");
        } catch (NumberFormatException e) { System.out.println("Invalid ID."); }
    }

    private void listUsers() {
        System.out.println("ID | Username | isAdmin");
        db.users.values().stream().sorted(Comparator.comparingInt(u -> u.id)).forEach(u -> System.out.println(u.id + " | " + u.username + " | " + u.isAdmin));
    }

    private void deleteUser() {
        try {
            int id = Integer.parseInt(prompt("User ID to delete"));
            if (currentUser != null && currentUser.id == id) { System.out.println("Cannot delete yourself."); return; }
            if (db.users.remove(id) != null) {
                // remove playlists owned by user
                db.playlists.values().removeIf(p -> p.userId == id);
                save();
                System.out.println("User deleted.");
            } else System.out.println("User not found.");
        } catch (NumberFormatException e) { System.out.println("Invalid ID."); }
    }

    // ---------- Songs / search ----------
    private void listSongs() {
        System.out.println("ID | Title | Artist | Dur(s)");
        db.songs.values().stream().sorted(Comparator.comparingInt(s -> s.id)).forEach(s -> {
            Artist a = db.artists.get(s.artistId);
            String an = a == null ? "Unknown" : a.name;
            System.out.printf("%3d | %-30s | %-20s | %4d%n", s.id, s.title, an, s.durationSeconds);
        });
    }

    private void searchSongs() {
        String q = prompt("Search (title or artist)");
        if (q.isEmpty()) { System.out.println("Empty."); return; }
        String Q = q.toLowerCase();
        db.songs.values().stream().sorted(Comparator.comparingInt(s -> s.id)).filter(s ->
            s.title.toLowerCase().contains(Q) || (db.artists.get(s.artistId) != null && db.artists.get(s.artistId).name.toLowerCase().contains(Q))
        ).forEach(s -> {
            Artist a = db.artists.get(s.artistId);
            String an = a == null ? "Unknown" : a.name;
            System.out.printf("%3d | %-30s | %-20s | %4d%n", s.id, s.title, an, s.durationSeconds);
        });
    }

    // ---------- Follow artist ----------
    private void followArtist() {
        if (!requireLogin()) return;
        String name = prompt("Artist name to follow");
        if (name.isEmpty()) { System.out.println("Required."); return; }
        int aid = findArtistIdByName(name);
        if (aid == -1) {
            String c = prompt("Artist not found. Create? (y/n)");
            if (c.toLowerCase().startsWith("y")) {
                aid = findOrCreateArtistByName(name);
            } else return;
        }
        currentUser.followedArtistIds.add(aid);
        save();
        System.out.println("Now following: " + db.artists.get(aid).name);
    }

    // ---------- Playlists ----------
    private void managePlaylists() {
        if (!requireLogin()) return;
        while (true) {
            System.out.println("\n--- Playlists ---");
            System.out.println("1) Create playlist");
            System.out.println("2) List my playlists");
            System.out.println("3) View playlist (and add/remove songs)");
            System.out.println("4) Delete playlist");
            System.out.println("0) Back");
            String o = prompt("Choice");
            switch (o) {
                case "1": createPlaylist(); break;
                case "2": listMyPlaylists(); break;
                case "3": viewPlaylist(); break;
                case "4": deletePlaylist(); break;
                case "0": return;
                default: System.out.println("Unknown"); break;
            }
        }
    }

    private void createPlaylist() {
        String name = prompt("Playlist name");
        if (name.isEmpty()) { System.out.println("Required."); return; }
        int id = db.nextPlaylistId++;
        Playlist p = new Playlist(id, currentUser.id, name);
        db.playlists.put(id, p);
        save();
        System.out.println("Created playlist id: " + id);
    }

    private void listMyPlaylists() {
        System.out.println("ID | Name");
        db.playlists.values().stream().filter(p -> p.userId == currentUser.id).forEach(p -> System.out.println(p.id + " | " + p.name));
    }

    private void viewPlaylist() {
        try {
            int pid = Integer.parseInt(prompt("Playlist ID"));
            Playlist p = db.playlists.get(pid);
            if (p == null || p.userId != currentUser.id) { System.out.println("Playlist not found or not yours."); return; }
            System.out.println("Playlist: " + p.name);
            while (true) {
                System.out.println("a) Add song by ID\nb) Remove song by ID\nc) List songs\nd) Back");
                String ch = prompt("Choice");
                if (ch.equalsIgnoreCase("a")) {
                    try { int sid = Integer.parseInt(prompt("Song ID to add")); if (db.songs.containsKey(sid)) { p.songIds.add(sid); save(); System.out.println("Added."); } else System.out.println("Song not found."); }
                    catch (NumberFormatException e) { System.out.println("Invalid"); }
                } else if (ch.equalsIgnoreCase("b")) {
                    try { int sid = Integer.parseInt(prompt("Song ID to remove")); if (p.songIds.removeIf(x -> x == sid)) { save(); System.out.println("Removed."); } else System.out.println("Not in playlist."); }
                    catch (NumberFormatException e) { System.out.println("Invalid"); }
                } else if (ch.equalsIgnoreCase("c")) {
                    if (p.songIds.isEmpty()) System.out.println("(empty)");
                    else {
                        System.out.println("ID | Title | Artist | Dur(s)");
                        for (int sid : p.songIds) {
                            Song s = db.songs.get(sid);
                            if (s == null) continue;
                            Artist a = db.artists.get(s.artistId);
                            System.out.printf("%3d | %-30s | %-20s | %4d%n", s.id, s.title, a == null ? "Unknown" : a.name, s.durationSeconds);
                        }
                    }
                } else if (ch.equalsIgnoreCase("d")) break;
                else System.out.println("Unknown");
            }
        } catch (NumberFormatException e) { System.out.println("Invalid ID."); }
    }

    private void deletePlaylist() {
        try {
            int pid = Integer.parseInt(prompt("Playlist ID to delete"));
            Playlist p = db.playlists.get(pid);
            if (p == null || p.userId != currentUser.id) { System.out.println("Not found or not yours."); return; }
            db.playlists.remove(pid);
            save();
            System.out.println("Deleted.");
        } catch (NumberFormatException e) { System.out.println("Invalid"); }
    }

    // ---------- Play (simulate) ----------
    private void playSong() {
        try {
            int sid = Integer.parseInt(prompt("Song ID to play"));
            Song s = db.songs.get(sid);
            if (s == null) { System.out.println("Not found."); return; }
            Artist a = db.artists.get(s.artistId);
            System.out.println("Now playing: " + s.title + " - " + (a == null ? "Unknown" : a.name) + " (" + s.durationSeconds + "s)");
            simulateProgress(s.durationSeconds);
        } catch (NumberFormatException e) { System.out.println("Invalid ID."); }
    }

    private void simulateProgress(int seconds) {
        int ticks = Math.min(seconds, 30);
        if (ticks <= 0) { System.out.println("Finished."); return; }
        for (int i = 0; i <= ticks; i++) {
            int percent = (int) ((i / (double) ticks) * 100);
            StringBuilder bar = new StringBuilder();
            for (int j = 0; j < i; j++) bar.append('#');
            for (int j = i; j < ticks; j++) bar.append('-');
            System.out.print("\r[" + bar.toString() + "] " + percent + "%");
            try { Thread.sleep(Math.max(50, seconds * 1000L / ticks)); } catch (InterruptedException ignored) {}
        }
        System.out.println("\nFinished.");
    }

    // ---------- Utility helpers ----------
    private int findArtistIdByName(String name) {
        for (Artist a : db.artists.values()) if (a.name.equalsIgnoreCase(name)) return a.id;
        return -1;
    }

    private int findOrCreateArtistByName(String name) {
        int id = findArtistIdByName(name);
        if (id != -1) return id;
        int nid = db.nextArtistId++;
        db.artists.put(nid, new Artist(nid, name));
        save();
        return nid;
    }

    // internal add used during seed
    private void addSongInternal(String title, int artistId, int duration) {
        int sid = db.nextSongId++;
        db.songs.put(sid, new Song(sid, title, artistId, duration));
    }
}
