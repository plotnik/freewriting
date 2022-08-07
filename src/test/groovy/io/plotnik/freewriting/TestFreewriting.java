package io.plotnik.freewriting;

import static org.junit.Assert.fail;
import org.junit.Test;
import static java.lang.System.*;

public class TestFreewriting {

    public TestFreewriting() {
    }

    //@Test
    public void testFreewriting() {
        try {
            new Freewriting("../pages");

        } catch (FwException ex) {
            fail(ex.getMessage());
        }
    }

    //@Test
    public void testSearchString() {
        try {
            SearchPatterns searchPatterns = new SearchPatterns("..", "http://127.0.0.1:4200/home", "asciidoctor");
            searchPatterns.readPatternsFolder();

            Freewriting fw = new Freewriting("../pages");
            searchPatterns.setFdates(fw.getFdates());

            String result = "Chesterton";
            searchPatterns.moveToListTop(result);
            searchPatterns.extract(result);

        } catch (FwException ex) {
            fail(ex.getMessage());
        }
    }
}
