package com.up.codingame.tools;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author rush
 */
public class CodingameCompiler {
    
    private static final Pattern importpat = Pattern.compile("import\\s+(.+?);", Pattern.DOTALL);
    private static final Pattern packagepat = Pattern.compile("package\\s+(.+?);", Pattern.DOTALL);
    private static final Pattern classdefpat = Pattern.compile("(?:(public|private|protected)\\s+)?(class|enum|interface)\\s+([^ \t\n\r{<]+)", Pattern.DOTALL);
    private static final Pattern removepat = Pattern.compile("(import|package)\\s+.+?;\\s*", Pattern.DOTALL);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String compiled = compile(parseClasses(new File(args[0])));
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(compiled), null);
        System.out.println(compiled);
    }
    
    private static List<JavaClass> parseClasses(File root) {
        return parseDir(new File(root, "src/main/java/"));
    }
    
    private static List<JavaClass> parseDir(File dir) {
        List<JavaClass> classes = new ArrayList<>();
        for (File f : dir.listFiles()) {
            if (!f.isDirectory()) {
                //Make this correct
                String[] parts = getFileNameParts(f.getName());
                if (!"java".equals(parts[1]) || CodingameCompiler.class.getSimpleName().equals(parts[0])) continue;
                try {
                    JavaClass cls = new JavaClass();
                    String body = new BufferedReader(new FileReader(f)).lines().collect(Collectors.joining("\n"));
                    Matcher pm = packagepat.matcher(body);
                    if (pm.find()) {
                        cls.pkg = pm.group(1);
                    }
                    Matcher im = importpat.matcher(body);
                    while (im.find()) {
                        cls.imports.add(im.group(1));
                    }
                    Matcher cm = classdefpat.matcher(body);
                    while (cm.find()) {
                        if (cm.group(3).equals(parts[0])) {
                            cls.name = cm.group(3);
                            body = body.replace(cm.group(), cm.group(2) + " " + cls.name);
                        }
                    }
                    cls.body = body.replaceAll(removepat.pattern(), "");
                    classes.add(cls);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(CodingameCompiler.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                classes.addAll(parseDir(f));
            }
        }
        return classes;
    }
    
    private static String[] getFileNameParts(String name) {
        int i = name.lastIndexOf(".");
        return i < 1 ? new String[] {name, ""} : new String[] {name.substring(0, i), name.substring(i + 1)};
    }
    
    private static String compile(List<JavaClass> classes) {
        String imports = classes.stream()
                .flatMap(c -> c.imports.stream().filter(i -> !classes.stream().anyMatch(c2 -> !c2.pkg.isEmpty() && i.startsWith(c2.pkg))))
                .map(i -> "import " + i + ";")
                .collect(Collectors.toCollection(() -> new TreeSet<>()))
                .stream().collect(Collectors.joining("\n"))
            + "\n\n";
        String bodies = classes.stream()
                .map(c -> c.body)
                .collect(Collectors.joining("\n\n"));
        
        return imports + bodies;
    }
    
    
    public static String simpleCompile() {
        File root = new File("src/main/java/");
        String compiled = "";
        TreeSet<String> imports = new TreeSet<>();
        for (File f : root.listFiles()) {
            if (!f.isDirectory()) {
                try {
                    List<String> lines = new BufferedReader(new FileReader(f)).lines().collect(Collectors.toList());
                    for (String line : lines) {
                        Matcher m = importpat.matcher(line);
                        int lastend = 0;
                        while (m.find()) {
                            String simport = m.group();
                            imports.add(simport);
                            lastend = m.end();
                        }
                        compiled += line.substring(lastend) + "\n";
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(CodingameCompiler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return imports.stream().collect(Collectors.joining("\n")) + "\n\n" + compiled;
    }
    
    
    private static class JavaClass {
        
        private String pkg = "";
        private String name;
        private TreeSet<String> imports = new TreeSet<>();
        private String body;

        @Override
        public String toString() {
            return "package " + pkg + ";\n" + imports.stream().map(i -> "import " + i + ";").collect(Collectors.joining("\n")) + "\n" + body;
        }
        
    }
    
}
