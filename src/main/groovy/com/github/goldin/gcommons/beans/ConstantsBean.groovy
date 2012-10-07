package com.github.goldin.gcommons.beans

import java.util.regex.Pattern

 /**
 * Various constants
 */
class ConstantsBean extends BaseBean
{
    public static final String  CRLF             = System.getProperty( 'line.separator' )
    public static final String  USER_HOME        = System.getProperty( 'user.home' )
    public static final String  USER_DIR         = System.getProperty( 'user.dir' )
    public static final String  OS_NAME          = System.getProperty( 'os.name' )
    public static final String  FILEMODE         = '[0-7]{3}' // tar.gz filemode pattern: 700, 755

    public static final File    USER_DIR_FILE    = new File( USER_DIR )
    public static final File    USER_HOME_FILE   = new File( USER_HOME )

    public static final Pattern NETWORK_PATTERN  = ~/^(?i)(http|scp|ftp):(?:\/)+(.+):(.+)@(.+):(.+)$/

    public static final int    MILLIS_IN_SECOND  = 1000 // Milliseconds in a second
    public static final int    SECONDS_IN_MINUTE = 60   // Seconds in a minute
    public static final int    MILLIS_IN_MINUTE  = MILLIS_IN_SECOND * SECONDS_IN_MINUTE // Milliseconds in a minute
}
