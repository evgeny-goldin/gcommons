package com.github.goldin.gcommons.truezip

import static com.github.goldin.gcommons.truezip.TrueZip.*
import de.schlichtherle.io.DefaultArchiveDetector
import de.schlichtherle.io.archive.spi.ArchiveDriver
import org.gcontracts.annotations.Requires


/**
 * {@link DefaultArchiveDetector} extension detecting only the original archive.
 *
 * Used in {@link com.github.goldin.gcommons.beans.FileBean#unpack(File, File)} so that
 * only the original archive is processed by TrueZip and if archive being unpacked
 * contains other archives - they are not detected as archives and are not processed by TrueZip.
 *
 * Otherwise, nested archives are repacked (I think) and their original size is modified
 * which makes comparing contents impossible.
 */
class SingleFileArchiveDetector extends DefaultArchiveDetector
{
    private final String                 archivePath
    private final DefaultArchiveDetector delegate


    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ archive.file && extension })
    SingleFileArchiveDetector ( File archive, String extension, DefaultArchiveDetector delegate = null )
    {
        super( '' )

        this.archivePath = archive.canonicalPath
        this.delegate    = delegate
    }


    @Override
    @Requires({ path })
    ArchiveDriver getArchiveDriver ( String path )
    {
        final driver = ( new File( path ).canonicalPath == archivePath ) ?
            (( delegate ? delegate.getArchiveDriver( path ) : getDefaultArchiveDriver( path ))) :
            null
        driver
    }
}
