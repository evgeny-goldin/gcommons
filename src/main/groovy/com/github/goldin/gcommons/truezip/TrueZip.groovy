package com.github.goldin.gcommons.truezip

import com.github.goldin.gcommons.beans.FileBean
import de.schlichtherle.io.ArchiveDetector
import de.schlichtherle.io.GlobalArchiveDriverRegistry
import de.schlichtherle.io.archive.spi.ArchiveDriver
import de.schlichtherle.io.archive.tar.TarDriver
import de.schlichtherle.io.archive.zip.ZipDriver
import de.schlichtherle.io.Files
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * TrueZip library wrapper.
 */
final class TrueZip
{
    private TrueZip (){}

    @Ensures ({ result })
    static Set<String> zipExtensions (){ driverExtensions( ZipDriver ) }

    @Ensures ({ result })
    static Set<String> tarExtensions (){ driverExtensions( TarDriver ) }


    @SuppressWarnings([ 'GroovyAccessibility '])
    @Requires({ sourceArchive && archiveExtension && destinationDirectory })
    static void unpackArchive( File sourceArchive, String archiveExtension, File destinationDirectory )
    {
        final detector = new SingleFileArchiveDetector( sourceArchive, archiveExtension )
        Files.cp_r( true, newTFile( sourceArchive, detector ), destinationDirectory, detector, detector )
        umount()
    }


    @Requires({ sourceFile && filePath && destinationArchive })
    static void addFileToArchive( File sourceFile, String filePath, File destinationArchive )
    {
        de.schlichtherle.io.File.cp_p( sourceFile, newTFile( destinationArchive, filePath ))
    }


    static void umount (){ de.schlichtherle.io.File.umount() }


    @Requires({ file && detector })
    private static de.schlichtherle.io.File newTFile( File file, ArchiveDetector detector )
    {
        new de.schlichtherle.io.File( file, detector )
    }


    @Requires({ file && path })
    private static de.schlichtherle.io.File newTFile( File file, String path )
    {
        new de.schlichtherle.io.File( file, path )
    }


    @SuppressWarnings([ 'GroovyAccessibility' ])
    @Requires({ requiredDriverClass })
    @Ensures ({ result })
    private static Set<String> driverExtensions ( Class<? extends ArchiveDriver> requiredDriverClass )
    {
        (( Map<String,?> ) GlobalArchiveDriverRegistry.INSTANCE ).findAll {
            def extension, driver ->
            final driverClass = (( driver instanceof ArchiveDriver ) ? driver.class :
                                 ( driver instanceof String        ) ? FileBean.class.classLoader.loadClass( driver, true ) :
                                                                       null )
            driverClass && requiredDriverClass.isAssignableFrom( driverClass )
        }.
        keySet()*.toLowerCase()
    }
}
