package com.devfactory.ancover

class AndroidCoverageCheckScript {

    static void main(String[] args) {

        if (args.length == 0) {
            println 'Working directory is not specified'
            return
        }

        File rootDir  = new File(args[0])
        File repoList = new File(rootDir, 'repos.txt')
        File logs = new File(rootDir, "log-${new Date().format('MM-dd-yyyy_HH-mm-ss')}.csv")

        logs << "REPO;LINES;COVERED;COVERAGE\r\n"

        println 'Starting ANCOVER'

        repoList.eachLine { l ->

            // retrieve info
            String url = l.endsWith('.git') ? l : l + '.git'
            String repo = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'))

            // clone repo
            println "Cloning ${url}"
            int cloneCode = AncoverToolkit.clone(rootDir, url)
            if (cloneCode != 0) {
                println "Failed to clone ${repo}"
                logs << "${url};ERROR;not cloned\r\n"
                return
            }

            println "${repo} is cloned"

            // check if it is android
            boolean isAndroid = AncoverToolkit.isAndroid(rootDir, repo)

            if (!isAndroid) {
                println "${repo} is not Android. Skipping..."
                logs << "${url};ERROR;not Android\r\n"
                return
            }

            println "${repo} is Android"


            // check if it is has tests (if not - create them with fake test)
            AncoverToolkit.verifyTests(rootDir, repo)

            // install jacoco
            AncoverToolkit.installJacoco(rootDir, repo)

            // run task with jacoco
            println "Creating jacoco report for ${url}"
            int reportCode = AncoverToolkit.report(rootDir, repo)
            if (reportCode != 0) {
                println "Failed to create jacoco report for ${repo}"
                logs << "${url};ERROR;jacoco report not created\r\n"
                return
            }

            // retrieve info from jacoco report
            def stats = AncoverToolkit.coverage(rootDir, repo)

            // save results
            def coverage = stats.lines > 0 ? Math.floor(stats.covered / stats.lines * 100) : 0
            println "COVERAGE SUMMARY for ${repo}: ${stats.covered}/${stats.lines} - ${coverage}%"
            logs << "${url};${stats.lines};${stats.covered};${coverage}\r\n"
        }

        println 'ANCOVER finished'
    }
}
