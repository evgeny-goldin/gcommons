package com.github.goldin.gcommons.util

import static com.github.goldin.gcommons.GCommons.*

/**
 * {@link com.github.goldin.gcommons.beans.GroovyBean#eval} configuration
 */
@SuppressWarnings( 'StatelessClass' )
class GroovyConfig
{
    String[] classpaths
    String   classpath
    boolean verbose        = true
    boolean verboseBinding = false

    List<String> classpaths() { general().list( this.classpaths, this.classpath )}
}
