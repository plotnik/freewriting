package io.plotnik.freewriting;

import static org.junit.Assert.fail;
import org.junit.Test;

public class TestFreewriting {
    
    public TestFreewriting() {
    }
    
    @Test
    public void testFreewriting() {      
        try {
            new Freewriting("../pages");
        } catch (FwException ex) {
            fail(ex.getMessage());
        }
    }
}
