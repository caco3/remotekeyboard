package de.caco3.remotekeyboard;

/**
 * Key constants extracted from the former TerminalIO to remove telnet dependency.
 */
class KeyConstants {
    public static final int IOERROR = -1;
    public static final int HANDLED = 1305;

    public static final int ESCAPE = 1200;

    public static final int ENTER = 1300;
    public static final int TABULATOR = 1301;
    public static final int DELETE = 1302;
    public static final int BACKSPACE = 1303;
    public static final int COLORINIT = 1304;
    public static final int LOGOUTREQUEST = 1306;

    public static final int DEL = 127;
    public static final int CR = 13;
    public static final int LF = 10;
    public static final int BS = 8;
}
