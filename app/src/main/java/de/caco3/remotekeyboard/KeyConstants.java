package de.caco3.remotekeyboard;

/**
 * Key constants that were formerly provided by the wimpi telnet library's TerminalIO class.
 * Defined here to make the keyboard input pipeline independent of any telnet dependency.
 */
class KeyConstants {
    public static final int ESCAPE = 27;

    public static final int ENTER = 1300;
    public static final int TABULATOR = 1301;
    public static final int DELETE = 1302;
    public static final int BACKSPACE = 1303;
    public static final int COLORINIT = 1304;

    public static final int DEL = 127;
    public static final int CR = 13;
    public static final int LF = 10;
    public static final int BS = 8;
}
