package com.devfactory.ancover

class ProjectLinesCounterScript {

    static void main(String[] args) {

//        File projectDir = new File('C:\\Projects\\Aurea\\XChange')
        File projectDir = new File('C:\\Projects\\Aurea\\CLEANUP\\brucetoo-pickview')

        int total = 0

        projectDir.eachFileRecurse() { f ->
            if((f.name.endsWith('.java')) &&
               !f.path.contains("test${File.separator}java") &&
               !f.path.contains("androidTest${File.separator}java"))
            {
                int lines = LineCountToolkit.countLines(f);
                total += lines
                println "${f.name}: ${lines}"
            }
        }

        println total
    }
}
