package com.github.goldin.gcommons.truezip

import static com.github.goldin.gcommons.truezip.TrueZip.*
import de.schlichtherle.io.DefaultArchiveDetector
import de.schlichtherle.io.archive.spi.ArchiveDriver
import org.gcontracts.annotations.Requires


/**
 * {@link DefaultArchiveDetector} extension detecting custom archive formats.
 */
class CustomFormatsArchiveDetector extends DefaultArchiveDetector
{
    private final List<String> zipExtensions
    private final List<String> tarExtensions
    private final List<String> tarGzExtensions


    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ ( zipExtensions != null ) && ( tarExtensions != null ) && ( tarGzExtensions != null ) })
    CustomFormatsArchiveDetector ( List<String> zipExtensions, List<String> tarExtensions, List<String> tarGzExtensions )
    {
        super( '' )

        this.zipExtensions   = zipExtensions.asImmutable()
        this.tarExtensions   = tarExtensions.asImmutable()
        this.tarGzExtensions = tarGzExtensions.asImmutable()
    }


    @Override
    @SuppressWarnings([ 'GroovyAccessibility' ])
    @Requires({ path })
    ArchiveDriver getArchiveDriver ( String path )
    {
        final endsWith = { List<String> extensions -> extensions.any { path.endsWith( ".$it" ) }}
        final driver   = endsWith( zipExtensions   ) ? getDefaultArchiveDriver ( 'zip' ) :
                         endsWith( tarExtensions   ) ? getDefaultArchiveDriver ( 'tar' ) :
                         endsWith( tarGzExtensions ) ? getDefaultArchiveDriver ( 'tgz' ) :
                                                       getDefaultArchiveDriver ( path )
        driver
    }
}
