package io.plotnik.freewriting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static java.lang.System.*;

@Command(header = {
    "@|cyan     ,dPYb,                         |@",
    "@|cyan     IP'`Yb                         |@",
    "@|cyan     I8  8I                         |@",
    "@|cyan     I8  8'                         |@",
    "@|cyan     I8 dP   gg    gg    gg         |@",
    "@|cyan     I8dP    I8    I8    88bg       |@",
    "@|cyan     I8P     I8    I8    8I         |@",
    "@|cyan    ,d8b,_  ,d8,  ,d8,  ,8I         |@",
    "@|cyan    PI8\"8888P\"\"Y88P\"\"Y88P\"          |@",
    "@|cyan     I8 `8,                         |@",
    "@|cyan     I8  `8,                        |@",
    "@|cyan     I8   8I                        |@",
    "@|cyan     I8   8I                        |@",
    "@|cyan     I8, ,8'                        |@",
    "@|cyan      \"Y8P'                         |@"
}, name = "fw", description = "Managing database of freewrite notes.",
        mixinStandardHelpOptions = true, version = "1.0")
public class Main implements Callable<Integer> {

    @Option(names = {"-fw"}, defaultValue = ".", description = "Freewrite folder")
    String fwHome;

    @Option(names = {"-p", "--pattern"}, description = "Name of pattern file to extract")
    String patternFile;

    @Option(names = {"-a", "--asciidoctor"}, defaultValue = "asciidoctor",
            description = "Asciidoctor command")
    String asciidoctor;

    @Option(names = {"-s", "--server"}, defaultValue = "http://127.0.0.1:4200/home",
           description = "Server URL")
    String serverURL;

    @Option(names = {"-m", "--moveSeason"},
            description = "Season name")
    String seasonToMove;


    // каталог, в котором находятся фрирайты
    final String pagesFolder = "pages/";

    SearchPatterns searchPatterns;

    FreewritingFrame frame;

    String pgHome;

    Freewriting fw;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        //System.out.println("Exit code: " + exitCode);
    }

    @Override
    public Integer call() {
        try {
            searchPatterns = new SearchPatterns(fwHome, serverURL, asciidoctor);
            searchPatterns.readPatternsFolder();

            pgHome = new File(fwHome, pagesFolder).getPath();
            fw = new Freewriting(pgHome);
            searchPatterns.setFdates(fw.getFdates());

            if (patternFile != null) {
                String pname = searchPatterns.getPatternName(patternFile);
                extractButtonClicked(pname);

            } else
            if (seasonToMove != null) {
                moveSeasonButtonClicked(seasonToMove);

            } else {
                frame = new FreewritingFrame(searchPatterns.getSortedNames(), this);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
            return 0;

        } catch (FwException e) {
            out.println("[ERROR] " + e.getMessage());
            return 1;
        }
    }

    void extractButtonClicked(String pname) {
        searchPatterns.moveToListTop(pname);
        searchPatterns.extract(pname);
    }

    void moveSeasonButtonClicked(String result) {
        try {
            SeasonFiles seasonFiles = new SeasonFiles(pgHome);
            seasonFiles.setFdates(fw.getFdates());
            seasonFiles.moveSeason(result);

        } catch (FwException e) {
            out.println("[ERROR] " + e.getMessage());
        }
    }

}
