FTL Profile/SavedGame Editor
https://github.com/Vhati/ftl-profile-editor


About

  Also known as the "ComaToes Profile/SavedGame Editor", this is a
  3rd-party tool to edit user files. It depends on resources from the game
  directory, but the game itself will not be modified.

  With this, you can unlock any or all ships and achievements in your user
  profile, or tweak most aspects of saved games: crew, systems, weapons,
  fires, breaches, etc.


Requirements

  Java (1.6 or higher).
    http://www.java.com/en/download/

  FTL (1.01-1.03.3, Windows/OSX/Linux, Steam/GOG/Standalone).
    http://www.ftlgame.com/

  * WinXP SP1 can't run Java 1.7.
    (1.7 was built with VisualStudio 2010, causing a DecodePointer error.)
    To get 1.6, you may have to google "jdk-6u45-windows-i586.exe".


Setup

  Extract the files from this archive anywhere.

  On the first run, you may be prompted to locate your
  FTL resources. Specifically "data.dat" in the "resources\"
  directory under your FTL install.

  In most cases, this should be located automatically.


Usage

  Exit FTL. The game must NOT be running.
  Double-click FTLProfileEditor.exe.
  Switch to the appropriate tab: "Profile" or "Saved Game".
  Open a profile (prof.sav) or saved game (continue.sav).
  Make any desired changes.
  Save, and close the editor.
  Fire up FTL and try out your new ship.


Troubleshooting

* If you get "java.lang.UnsupportedClassVersionError" on startup...
    You need a newer version of Java.

* Error reading profile. [...] Initial int not expected value: 2...
    You likely tried to open a saved game while in the "Profile" tab.

* Error reading saved game. [...] Unexpected first byte...
    You likely tried to open a profile while in the "Saved Game" tab.
