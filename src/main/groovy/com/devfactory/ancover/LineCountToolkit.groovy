package com.devfactory.ancover

import japa.parser.JavaParser
import japa.parser.ast.CompilationUnit
import japa.parser.ast.body.BodyDeclaration
import japa.parser.ast.body.ConstructorDeclaration
import japa.parser.ast.body.MethodDeclaration
import japa.parser.ast.stmt.BlockStmt
import japa.parser.ast.visitor.VoidVisitorAdapter

class LineCountToolkit {

    static int countLines(File file) {

        try {
            // parsing and cleaning up the comments
            CompilationUnit cu = JavaParser.parse(file, 'UTF-8', false);
            CompilationUnit ccu = JavaParser.parse(new StringReader(cu.toString()), false)

            // counting
            LineCounter counter = new LineCounter()
            counter.visit(ccu, null)

            return counter.totalLines
        }
        catch (Exception e) {
            println "Failed to parse ${file.name}"
        }

        return -1
    }


    private static class LineCounter extends VoidVisitorAdapter
    {
        int totalLines = 0;
        def ranges = []

        LineCounter() {}

        @Override
        void visit(ConstructorDeclaration n, Object arg) {
            super.visit(n, arg)

            if (n.block == null) return
            totalLines += countBlockSize(n, n.block)
        }

        @Override
        void visit(MethodDeclaration n, Object arg) {
            super.visit(n, arg)

            if (n.getBody() == null) return
            totalLines += countBlockSize(n, n.body)
        }

        int countBlockSize(BodyDeclaration declaration, BlockStmt block) {
            if (block.getStmts() == null || block.getStmts().isEmpty()) return 0

            int begin = block.getStmts().first().beginLine
            int end = block.getStmts().last().endLine

            int total = end - begin + 1

            for (def range : ranges) {
                if ((begin >= range.begin) && (end <= range.end)) {
                    total -= (range.end - range.begin + 1)
                    ranges.remove(range)
                }
            }

            ranges.add([begin : declaration.beginLine, end : declaration.endLine])
            total
        }

        boolean containRange(int begin, int end) {
            for (def range : ranges) {
                if ((begin >= range.begin) && (end <= range.end)) return true
            }

            false
        }
    }
}
