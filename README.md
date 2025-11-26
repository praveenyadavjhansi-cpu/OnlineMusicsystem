TerminalMusicSystemNoDB 🎵

A fully functional terminal-based music management system, built in pure Java without any external database.
All data (users, artists, songs, playlists) are stored locally using Java Serialization (music_data.ser).

📌 Features
👥 User Management

Register new users

Login / Logout

Default admin account:

username: admin
password: admin123


Admin privileges (add artists, add/delete songs, manage users)

🎶 Music Management

List all songs

Search songs by title or artist

Follow artists

Play a song (progress bar simulation)

📂 Playlist Management

Create playlists

Add/remove songs in playlists

View and manage your own playlists

Delete playlists

🛠 Admin Panel

Add artist

Add song

Delete song

List users

Delete user (except yourself)

📁 Project Structure

Since this is a single-file project:

TerminalMusicSystemNoDB.java     → main program
music_data.ser                   → auto-generated data file
README.md                        → documentation

🚀 How to Run
1. Compile the program
javac TerminalMusicSystemNoDB.java

2. Run the program
java TerminalMusicSystemNoDB

3. Data Persistence

The system automatically creates and updates:

music_data.ser


This file stores all users, artists, playlists, and songs.

🌱 Initial Seed Data

When run for the first time, the system auto-adds:

Artists:

The Echoes

Solaris

Blue Moon

Acoustic Tales

Sample Songs:

Waves (120s)

Sunrise (150s)

Midnight Blues (200s)

Campfire (180s)

Horizon (215s)

Orbit (175s)

🎛 Interactive Menu
Main Menu
1) Register
2) Login
3) Logout
4) List songs
5) Search songs
6) Follow artist
7) Playlists
8) Play song
9) Admin menu
0) Exit

💾 Data Storage Approach

Instead of using SQL/JDBC, the system uses:

ObjectOutputStream → save() data to music_data.ser
ObjectInputStream  → load() data from music_data.ser


Classes that are serialized:

User

Artist

Song

Playlist

DB (container)

🔒 Dependencies

No external dependencies.
Runs on any Java 8+ environment.

📜 License

Free to use and modify for learning or personal use.

If you'd like, I can also create:
✔ A more stylish README
✔ Screenshots / usage examples
✔ A GitHub-ready README with badges
✔ A version with an architecture diagram
