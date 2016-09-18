package me.blzr.github;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class UtilTest {

    @Test
    public void TryParse() throws Exception {
        List result = Util.tryParse(
                "https://github.com/GrahamCampbell " +
                "blah https://github.com/c9s blah https://github.com/kartik-v\n" +
                "https://github.com/GrahamCampbell");
        Assert.assertEquals(3, result.size());

        Assert.assertEquals("GrahamCampbell", result.get(0));
        Assert.assertEquals("c9s", result.get(1));
        Assert.assertEquals("kartik-v", result.get(2));
    }

}
