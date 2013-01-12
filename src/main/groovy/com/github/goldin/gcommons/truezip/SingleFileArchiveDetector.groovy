package com.github.goldin.gcommons.truezip

import de.schlichtherle.io.DefaultArchiveDetector
import de.schlichtherle.io.archive.spi.ArchiveDriver
import org.gcontracts.annotations.Requires


/**
 * {@link de.schlichtherle.io.DefaultArchiveDetector} extension detecting only the original archive.
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
    private final String archivePath


    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ archive.file && extension })
    SingleFileArchiveDetector ( File archive, String extension )
    {
        super( extension )
        archivePath = archive.canonicalPath
    }


    @Override
    @Requires({ path })
    ArchiveDriver getArchiveDriver ( String path )
    {
        ( new File( path ).canonicalPath == archivePath ) ? super.getArchiveDriver( path ) : null
    }
}
