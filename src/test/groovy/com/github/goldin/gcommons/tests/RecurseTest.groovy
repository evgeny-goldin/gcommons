package com.github.goldin.gcommons.tests

import com.github.goldin.gcommons.BaseTest
import groovy.io.FileType
import groovy.util.logging.Slf4j
import org.junit.Test


/**
 * "File.metaClass.recurse" tests
 */
@Slf4j
class RecurseTest extends BaseTest
{
    private File prepareTestRecurse()
    {
        def testDir = testDir( 'recurse' )
        def write   = { String path, String content ->
            def file = new File( testDir, path )
            fileBean.mkdirs( file.parentFile )
            file.write( content )
        }

        write( '1/2/3.txt',  'aaaaaaaaaaaa' ) /* length is 12 */
        write( '5/6/7.txt',  'bbbbbbbbbbb'  ) /* length is 11 */
        write( '7/8/22.txt', 'cccccccccc'   ) /* length is 10 */

        testDir
    }


    @Test
    void testRecurseTypes()
    {
        def testDir = prepareTestRecurse()
        def recurse = { Map configs, Closure c -> fileBean.recurse( testDir, configs, c )}

        recurse([ type : FileType.FILES       ]){ File it -> assert it.file }
        recurse([ type : FileType.DIRECTORIES ]){ File it -> assert it.directory }
        recurse([ type : FileType.ANY         ]){ File it -> assert it.file || it.directory }

        recurse([ filterType: FileType.FILES,
                  filter    : { File it -> assert it.file }]){}

        recurse([ filterType: FileType.DIRECTORIES,
                  filter    : { File it -> assert it.directory }]){}

        recurse([ filterType: FileType.ANY,
                 filter    : { File it -> assert it.file || it.directory }]){}

        recurse([ type      : FileType.DIRECTORIES,
                  filterType: FileType.FILES,
                  filter    : { File it -> assert it.file }]){ File it -> assert it.directory }

        recurse([ type      : FileType.FILES,
                  filterType: FileType.DIRECTORIES,
                  filter    : { File it -> assert it.directory }]){ File it -> assert it.file }

        recurse([ type      : FileType.FILES,
                  filterType: FileType.ANY,
                  filter    : { File it -> assert it.file || it.directory }]){ File it -> assert it.file }
    }


    @Test
    void testRecurseFiles()
    {
        def testDir   = prepareTestRecurse()
        def parentDir = testDir.parentFile.parentFile.parentFile.parentFile
        def names     = []
        def c         = { names << it.name }
        def recurse   = { Map configs, Closure callback -> fileBean.recurse( testDir, configs, callback )}

        assert testDir.splitWith  ( 'eachFileRecurse' ) == testDir.splitWith  ( 'recurse' )
        assert parentDir.splitWith( 'eachFileRecurse' ) == parentDir.splitWith( 'recurse' )

        recurse([ type: FileType.FILES ], c )
        assertSameLists( names, [ '3.txt', '7.txt', '22.txt' ])

        names = []
        recurse([ type   : FileType.FILES,
                  filter : { File it -> it.name.endsWith( '3.txt' )} ], c )
        assert names == [ '3.txt' ]

        names = []
        recurse([ type   : FileType.FILES,
                  filter : { File it -> it.name.endsWith( '.txt' )} ], c )
        assertSameLists( names, [ '3.txt', '7.txt', '22.txt' ])

        names = []
        recurse([ type   : FileType.FILES,
                  filter : { File it -> it.name.endsWith( '.pdf' )} ], c )
        assert names == []

        names = []
        recurse([ type   : FileType.FILES,
                  filter : { File it -> it.text.contains( 'b' )} ]){ File it ->
                                                                     assert it.text.contains( 'b' )
                                                                     names << it.name }
        assert names == [ '7.txt' ]

        def counter = 0
        recurse([ type   : FileType.FILES,
                  filter : { File it -> it.file && it.text.size() == 10 } ]){ ++counter; false }
        assert counter == 1
    }


    @Test
    void testRecurseDirectories()
    {
        def testDir = prepareTestRecurse()
        def names   = []
        def c       = { File it -> names << it.name }
        def recurse = { Map configs, Closure callback -> fileBean.recurse( testDir, configs, callback )}


        recurse([ type : FileType.DIRECTORIES ], c )
        assertSameLists( names, [ '1', '2', '5', '6', '7', '8' ])

        names = []
        recurse([ type   : FileType.DIRECTORIES,
                  filter : { File it -> fileBean.directorySize( it ) < 11 } ], c )
        assertSameLists( names, [ '7', '8' ])

        names = []
        recurse([ type   : FileType.DIRECTORIES,
                  filter : { File it -> it.listFiles().name.contains( '8' )} ], c )
        assert names == [ '7' ]

        names = []
        recurse([ type   : FileType.DIRECTORIES,
                  filter : { File it -> it.listFiles().name.contains( '7.txt' ) } ], c )
        assert names == [ '6' ]

        names = []
        recurse([ type   : FileType.DIRECTORIES,
                  filter : { File it -> it.listFiles().name.contains( 'aaa.exe' ) } ], c )
        assert names == []

        names = []
        recurse([ type   : FileType.DIRECTORIES,
                  filter : { File it -> ( it.listFiles() as List ).contains( new File( 'aaaa' )) } ], c )
        assert names == []

        names = []
        recurse([ type   : FileType.DIRECTORIES,
                  filter : { File dir -> dir.listFiles().name.any{ String s -> s ==~ /.*\.txt/ }} ], c )
        assertSameLists( names, [ '2', '6', '8' ])

        def sizes = [:]
        recurse([ type   : FileType.DIRECTORIES ]){ File it -> sizes[ it.name ] = fileBean.directorySize( it ) }
        assertSameMaps( sizes, [ '1': 12, '2':12, '5':11, '6':11, '7':10, '8':10 ])

        def counter = -1
        recurse([ type   : FileType.DIRECTORIES ]){ counter++ }
        assert counter == 5

        counter = -1
        recurse([ type        : FileType.DIRECTORIES,
                  filter      : { true },
                  stopOnFalse : true ]){ counter++ }
        assert counter == 5

        counter = 0
        recurse([ type        : FileType.DIRECTORIES,
                  filter      : { true },
                  stopOnFalse : true ]){ counter++ }
        assert counter == 5

        counter = 0
        recurse([ type        : FileType.DIRECTORIES,
                  filter      : { true },
                  stopOnFalse : true ]){ ++counter }
        assert counter == 6

        counter = -1
        recurse([ type        : FileType.DIRECTORIES,
                  stopOnFalse : true ]){ ++counter }
        assert counter == 4

        counter = 0
        recurse([ type        : FileType.DIRECTORIES,
                  stopOnFalse : true ]){ counter++; ( counter < 2 ) }
        assert counter == 4
    }


    @Test
    void testRecurseStopOnFalse()
    {
        def testDir = prepareTestRecurse()
        def counter = 0
        def recurse = { Map configs, Closure callback -> fileBean.recurse( testDir, configs, callback )}


        recurse([ type : FileType.ANY ]) { counter++ }
        assert counter == 9

        counter = 0
        recurse([ type : FileType.ANY ]){ ++counter; ( counter < 5 ) }
        assert counter == 9

        counter = 0
        recurse([ type : FileType.ANY ]){ ++counter; false }
        assert counter == 9

        counter = 0
        recurse([ type        : FileType.ANY,
                  filter      : { File it -> it.directory },
                  stopOnFalse : true ]) { ++counter; ( counter < 3 ) }
        assert counter == 4

        counter = 0
        recurse([ type        : FileType.DIRECTORIES,
                  filter      : { File it -> ! it.file },
                  stopOnFalse : true ]){ counter++; ( counter == 1 ) }
        assert counter == 4

        counter = 0
        recurse([ type        : FileType.DIRECTORIES,
                  filter      : { File it -> ! it.file },
                  stopOnFalse : true ]) { counter++; ( counter > 0 ) }
        assert counter == 6
    }


    @Test
    void testRecurseStopOnFilter()
    {
        def testDir = prepareTestRecurse()
        def names   = []
        def c       = { File it -> names << it.name }
        def recurse = { Map configs, Closure callback -> fileBean.recurse( testDir, configs, callback )}


        recurse([ type         : FileType.FILES,
                  filterType   : FileType.DIRECTORIES,
                  filter       : { File dir -> ['1', '2'].any{ it == dir.name } /* Dir name is '1' or '2' */ },
                  stopOnFilter : true ], c )
        assert names == [ '3.txt' ]

        names = []
        recurse([ type         : FileType.FILES,
                  filterType   : FileType.DIRECTORIES,
                  filter       : { File dir -> ['1', '2'].any{ it == dir.name }} /* Dir name is '1' or '2' */ ], c )
        assertSameLists( names, [ '3.txt', '7.txt', '22.txt' ] )

        names = []
        recurse([ filterType   : FileType.DIRECTORIES,
                  filter       : {},
                  stopOnFilter : true ], c )
        assert names == []

        names = []
        recurse([ filterType   : FileType.DIRECTORIES,
                  filter       : { false },
                  stopOnFilter : true ], c )
        assert names == []

        names = []
        recurse([ type         : FileType.ANY,
                  filterType   : FileType.DIRECTORIES,
                  filter       : { File it -> it.name != '5' },
                  stopOnFilter : true ], c )
        assertSameLists( names, [ '1', '2', '3.txt', '7', '8', '22.txt' ])

        names = []
        recurse([ type         : FileType.FILES,
                  filterType   : FileType.DIRECTORIES,
                  filter       : { true },
                  stopOnFilter : true ], c )
        assertSameLists( names, [ '3.txt', '7.txt', '22.txt' ] )

        names   = []
        recurse([ type         : FileType.FILES,
                  filterType   : FileType.DIRECTORIES,
                  filter       : { File it -> it.name == '5' },
                  stopOnFilter : true ], c )
        assert names   == []

        names   = []
        recurse([ type         : FileType.FILES,
                  filterType   : FileType.DIRECTORIES,
                  filter       : { File dir -> [ '5', '6' ].any{ it == dir.name }},
                  stopOnFilter : true ], c )
        assert names == [ '7.txt' ]

        names   = []
        recurse([ type         : FileType.FILES,
                  filterType   : FileType.DIRECTORIES,
                  filter       : { File dir -> [ '5', '6' ].any{ it == dir.name }},
                  stopOnFilter : false ], c )
        assertSameLists( names, [ '3.txt', '7.txt', '22.txt' ] )

        names   = []
        recurse([ type         : FileType.DIRECTORIES,
                  filterType   : FileType.DIRECTORIES,
                  filter       : { File dir -> dir.listFiles()[ 0 ].name == '22.txt' },
                          stopOnFilter : false ], c )
        assert names == [ '8' ]

        names   = []
        recurse([ type         : FileType.DIRECTORIES,
                  filterType   : FileType.DIRECTORIES,
                  filter       : { File dir -> [ '8', '22.txt' ].any{ it == dir.listFiles()[ 0 ].name }},
                  stopOnFilter : true ], c )
        assertSameLists( names, [ '7', '8' ])
    }


    @Test
    void recurseShouldFindPOMs()
    {
        def testDir = testDir( 'poms' )
        def write   = { String path ->
            def file = new File( testDir, path )
            fileBean.mkdirs( file.parentFile )
            file.write( 'aaa' )
        }

        write( 'build/something/pom.xml' )
        write( 'pom.xml' )
        write( 'moduleA/pom.xml' )
        write( 'moduleA/build/aa/pom.xml' )
        write( 'moduleA/dist/bb/pom.xml' )
        write( 'moduleB/pom.xml' )
        write( 'moduleB/dist/pom.xml' )
        write( 'moduleB/build/pom.xml' )

        List<String> poms =  []
        fileBean.recurse( testDir, [ type         : FileType.FILES,
                                     filterType   : FileType.DIRECTORIES,
                                     filter       : { File dir -> ( ! ( [ 'build', 'dist' ].any{ it == dir.name } )) },
                                     stopOnFilter : true ] ) { File it -> poms << it.canonicalPath.replace( '\\', '/' ) }

        assert poms.size() == 3
        assert poms.every { String path -> [ '/poms/moduleA/pom.xml', '/poms/moduleB/pom.xml', '/poms/pom.xml' ].any{ path.endsWith( it ) }}
    }


    @Test
    @SuppressWarnings( 'AbcComplexity' )
    void recurseShouldFindSvn()
    {
        def testDir = testDir( 'svn' )
        def write   = { String path ->
            def file = new File( testDir, path )
            fileBean.mkdirs( file.parentFile )
            file.write( 'aaazzzxxx' )
        }

        write( 'project/.svn/1.txt' )
        write( 'project/moduleA/.svn/1.txt' )
        write( 'project/moduleA/1.txt' )
        write( 'project/moduleA/src/.svn/1.txt' )
        write( 'project/moduleA/src/main/.svn/1.txt' )
        write( 'project/moduleA/src/main/resources/.svn/1.txt' )
        write( 'project/moduleA/src/main/resources/1.txt' )
        write( 'project/moduleA/src/test/.svn/1.txt' )
        write( 'project/moduleA/src/test/resources/.svn/.s1.txt' )
        write( 'project/moduleA/src/test/resources/1.txt' )
        write( 'project/moduleB/.svn/1.txt' )
        write( 'project/moduleB/1.txt' )

        write( 'project2/.svn/1.txt' )
        write( 'project2/moduleA/.svn/1.txt' )
        write( 'project2/moduleA/1.txt' )
        write( 'project2/moduleA/src/.svn/1.txt' )
        write( 'project2/moduleA/src/main/.svn/1.txt' )
        write( 'project2/moduleA/src/main/resources/.svn/1.txt' )
        write( 'project2/moduleA/src/main/resources/1.txt' )
        write( 'project2/moduleA/src/test/.svn/1.txt' )
        write( 'project2/moduleA/src/test/resources/.svn/.s1.txt' )
        write( 'project2/moduleA/src/test/resources/1.txt' )
        write( 'project2/moduleB/.svn/1.txt' )
        write( 'project2/moduleB/1.txt' )

        write( 'project3/project4/.svn/1.txt' )
        write( 'project3/project4/src/.svn/1.txt' )
        write( 'project3/project4/src/main/.svn/1.txt' )
        write( 'project3/project4/src/main/groovy/.svn/1.txt' )
        write( 'project3/project4/src/main/groovy/1.groovy' )


        def hasSvn = { File[] dirs -> dirs.every { File dir -> dir.listFiles().any{ File f -> ( f.name == '.svn' ) }}}
        List<String> projectRoots = []
        def          counter      = 0
        fileBean.recurse( testDir, [ type : FileType.DIRECTORIES, stopOnFalse: true ] ) {
            File dir ->

            counter++
            if ( hasSvn( dir ))
            {
                projectRoots << dir.canonicalPath.replace( '\\', '/' )
                false
            }
            else
            {
                true
            }
        }

        assert projectRoots.size() == 3
        assert counter             == 4
        assert projectRoots.every { String path -> [ '/svn/project', '/svn/project2', '/svn/project3/project4' ].any{ path.endsWith( it ) }}

        projectRoots = []
        counter      = 0

        fileBean.recurse( testDir, [ filterType   : FileType.DIRECTORIES,
                                     type         : FileType.DIRECTORIES,
                                     stopOnFilter : true,
                                     filter       : { File dir -> (( dir.name != '.svn' ) && ( ! hasSvn( dir, dir.parentFile ))) } ] ) {
            File it ->
            counter++
            if ( hasSvn( it )) { projectRoots << it.canonicalPath.replace( '\\', '/' ) }
        }

        assert projectRoots.size() == 3
        assert counter             == 4
        assert projectRoots.every { String path -> [ '/svn/project', '/svn/project2', '/svn/project3/project4' ].any{ path.endsWith( it ) }}
    }
}
