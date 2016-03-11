package ysoserial;

import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;
import org.xeustechnologies.jcl.context.DefaultContextLoader;
import ysoserial.payloads.ObjectPayload;
import ysoserial.payloads.util.PayloadRunner;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

public class BootstrapMain {
    public static void main(String[] args) throws Exception {
        JarClassLoader jcl = new JarClassLoader();
        jcl.add("output/libs");

        //TODO: Make the classpath selection dynamic based on the gadget selected
        jcl.add("output/gadget-cc3");
        jcl.add("output/gadget-cc4");

        JclObjectFactory factory = JclObjectFactory.getInstance();
        final Object generatePayload = factory.create(jcl, "ysoserial.GeneratePayload");

        //Invoke main method
        Method m = generatePayload.getClass().getDeclaredMethod("main",String[].class);
        m.invoke(null,new Object[] { args });
    }

}
