package com.devfactory.ancover

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection

import java.nio.file.Files

class AncoverToolkit {

    static int clone(File rootDir, String url) {

        def p = "git clone ${url}".execute((String[])null, rootDir)
        p.waitFor()
        p.text.eachLine {println it}
        p.exitValue()
    }

    static boolean isAndroid(File rootDir, String repo) {

        boolean isAndroid = false

        new File(rootDir, repo).eachFileRecurse() { f ->
            if(f.name.equalsIgnoreCase('AndroidManifest.xml')) isAndroid = true
        }

        isAndroid
    }

    static void verifyTests(File rootDir, String repo) {

        new File(rootDir, repo).eachDirRecurse() { d ->
            if (d.name.equals('src')) {

                // check if module has test dir
                File testDir = new File(d, 'test')
                if (!testDir.exists()) {
                    testDir.mkdir()
                }

                // check if test dir contains files with valid files
                boolean hasValidTests = false

                testDir.eachFileRecurse() { f ->
                    if (f.isFile()) hasValidTests = hasValidTests || f.text.contains('@Test')
                }

                if (!hasValidTests) {
                    File javaDir = new File(testDir, 'java')
                    if (!javaDir.exists()) javaDir.mkdir()

                    File packageDir = new File(javaDir, 'com')
                    if (!packageDir.exists()) packageDir.mkdir()

                    new File(packageDir, 'SampleTest.java') << "package com;\n" +
                            "import org.junit.Test;\n" +
                            "import static org.junit.Assert.assertEquals;\n" +
                            "public class SampleTest {\n" +
                            "   @Test\n" +
                            "   public void testExample() {\n" +
                            "       assertEquals(true, true);\n" +
                            "   }\n" +
                            "}\n"
                }
            }
        }
    }

    static void installJacoco(File rootDir, String repo) {
        // copy jacoco gradle
        File repoDir = new File(rootDir, repo)
        File jacocoFile = new File(repoDir, 'jacoco.gradle')
        if (!jacocoFile.exists()) Files.copy(new File(rootDir, 'jacoco.gradle').toPath(), jacocoFile.toPath())

        // setup root build.gradle
        repoDir.eachFileRecurse() { f ->
            if (f.name.equalsIgnoreCase('build.gradle') &&
                    !f.text.contains('jacoco.gradle ') &&
                    f.text.contains('android ')) {
                f << '\napply from: \'../jacoco.gradle\''
            }
        }
    }

    static int report(File rootDir, String repo) {

        ProjectConnection connection = GradleConnector.newConnector().
                forProjectDirectory(new File(rootDir, repo)).connect()

        try {
            connection.newBuild().forTasks('testDebugUnitTestCoverage').run()
        }
        catch (Exception e) {
            return -1
        }
        finally
        {
            connection.close()
        }

        0
    }

    static def coverage(File rootDir, String repo) {

        def stats = [:]
        stats.lines = 0
        stats.covered = 0

        new File(rootDir, repo).eachFileRecurse() { f ->
            if (f.name.equalsIgnoreCase('testDebugUnitTestCoverage.xml')) {

                def parser = new XmlSlurper()
                parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
                parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

                def parsed = parser.parse(f)

                parsed.package.counter.each { node->

                    if (node['@type'] == 'LINE')
                    {
                        int coveredAttr = node['@covered'].toInteger();
                        int missedAttr = node['@missed'].toInteger();

                        stats.covered += coveredAttr
                        stats.lines += coveredAttr + missedAttr
                    }
                }
            }
        }

        stats
    }

}
