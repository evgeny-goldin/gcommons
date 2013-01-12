package com.github.goldin.gcommons.truezip

import de.schlichtherle.truezip.file.TArchiveDetector
import de.schlichtherle.truezip.file.TFile
import de.schlichtherle.truezip.file.TVFS
import de.schlichtherle.truezip.fs.FsDriverProvider
import de.schlichtherle.truezip.fs.archive.tar.TarDriverService
import de.schlichtherle.truezip.fs.archive.zip.ZipDriverService
import de.schlichtherle.truezip.util.JSE7
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * TrueZip library wrapper.
 *
 * Some calls catch NoClassDefFoundError thrown on Java 6:
 * http://stackoverflow.com/questions/14270141/truezip-7-requires-java-7-noclassdeffounderror-java-nio-file-path-on-java-6
 */
final class TrueZip
{
    private TrueZip (){}


    static Set<String> zipExtensions (){ extensions( ZipDriverService, GCommonsFsDriverService ) }
    static Set<String> tarExtensions (){ extensions( TarDriverService ) }

    @Requires({ classes })
    @Ensures ({ result  })
    private static Set<String> extensions ( Class<? extends FsDriverProvider> ... classes )
    {
        classes.collect { it.newInstance().get().keySet()*.toString() }.flatten()
    }


    @Requires({ sourceArchive && destinationDirectory })
    static boolean unpackArchive( File sourceArchive, File destinationDirectory )
    {
        try
        {
            TFile.cp_rp( new TFile( sourceArchive ), new TFile( destinationDirectory ), TArchiveDetector.NULL )
            umount()
            true
        }
        catch ( NoClassDefFoundError ignored )
        {
            assert ( ! JSE7.AVAILABLE )
            false
        }
    }


    @Requires({ sourceFile && filePath && destinationArchive })
    static boolean addFileToArchive( File sourceFile, String filePath, File destinationArchive )
    {
        try
        {
            new TFile( sourceFile, TArchiveDetector.NULL ).cp_p( new TFile( destinationArchive, filePath ))
            true
        }
        catch ( NoClassDefFoundError ignored )
        {
            assert ( ! JSE7.AVAILABLE )
            false
        }
    }

    static void umount (){ TVFS.umount() }
}
