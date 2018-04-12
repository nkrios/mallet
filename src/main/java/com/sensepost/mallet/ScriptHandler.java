package com.sensepost.mallet;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

@Sharable
public class ScriptHandler extends ChannelInitializer<Channel> {

	private ScriptEngineManager sem = new ScriptEngineManager();
	private ScriptEngine engine;

	private ChannelHandler handler;

	private static String SCRIPT = null;

	static {
		try {
			InputStream is = ScriptHandler.class.getResourceAsStream("script.groovy");
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			StringBuilder b = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				b.append(line).append("\n");
			}
			SCRIPT = b.toString();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			SCRIPT = null;
		}
	}

	public ScriptHandler() throws Exception {
		this(SCRIPT, "groovy");
	}

	public ScriptHandler(String script, String language) throws Exception {
		this.engine = sem.getEngineByName(language);
		try {
			Object scriptResult = engine.eval(script);
			handler = getScriptHandler(engine, scriptResult);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public ScriptHandler(String filename) throws FileNotFoundException, ScriptException {
		InputStream is = null;
		try {
			File f = new File(filename);
			if (f.exists())
				is = new FileInputStream(f);
			else
				is = getClass().getClassLoader().getResourceAsStream(filename);
			if (is == null)
				throw new FileNotFoundException(filename);
			
			String extension = filename.substring(filename.lastIndexOf('.'));
			this.engine = sem.getEngineByExtension(extension);
			handler = getScriptHandler(engine, engine.eval(new InputStreamReader(is)));
		} catch (IOException ioe) {
			throw new ScriptException(ioe);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {}
		}
	}

	private ChannelHandler getScriptHandler(ScriptEngine engine, Object script) {
		if (script == null)
			throw new NullPointerException("script result is null");

		if (!Invocable.class.isAssignableFrom(engine.getClass()))
			throw new RuntimeException("Script engine cannot implement objects");

		if (script instanceof ChannelDuplexHandler)
			return (ChannelDuplexHandler) script;
		if (script instanceof ChannelHandler) {
			return (ChannelHandler) script;
		} else
			throw new ClassCastException("Script does not implement ChannelHandler");
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		String name = ch.pipeline().context(this).name();
		ch.pipeline().addAfter(name, null, handler);
	}

}
