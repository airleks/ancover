package com.devfactory.ancover

class ProjectLinesCounterScript {

    static void main(String[] args) {

        if (args.length == 0) {
            println 'Working directory is not specified'
            return
        }

        File rootDir = new File(args[0])

        File repoList = new File(rootDir, 'repos.txt')
        File logs = new File(rootDir, "line-counter-${new Date().format('MM-dd-yyyy_HH-mm-ss')}.csv")

        logs << "REPO;LINES;TEST LINES;MESSAGE\r\n"

        println 'Starting Line Counter'

        int projectsCounter = 0;

        repoList.eachLine { l ->
            // retrieve info
            String url = l.endsWith('.git') ? l : l + '.git'
            String repo = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'))

            projectsCounter++;

            // clone repo
            println "${projectsCounter}: Cloning ${url}"
            int cloneCode = AncoverToolkit.clone(rootDir, url)
            if (cloneCode != 0) {
                println "Failed to clone ${repo}"
                logs << "${url};ERROR;not cloned\r\n"
                return
            }

            println "${projectsCounter}: ${repo} is cloned"

            // count lines
            File repoDir = new File(rootDir, repo)
            int codeLines = 0
            int testLines = 0

            repoDir.eachFileRecurse() { f ->
                if (f.name.endsWith('.java'))
                {
                    int lines = LineCountToolkit.countLines(f)

                    if (lines < 0) return

                    if (!f.path.contains("test${File.separator}java") &&
                            !f.path.contains("androidTest${File.separator}java"))
                    {
                        codeLines += lines
                        println "${f.name}: ${lines}"
                    } else {
                        testLines += LineCountToolkit.countLines(f)
                    }
                }
            }

            println "${projectsCounter}: ${repo} lines: ${codeLines}, test lines: ${testLines}"
            logs << "${url};${codeLines};${testLines};\r\n"
        }
    }
}
