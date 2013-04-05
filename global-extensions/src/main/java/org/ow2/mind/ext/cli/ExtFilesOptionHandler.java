package org.ow2.mind.ext.cli;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ow2.mind.cli.CmdOption;
import org.ow2.mind.cli.CmdPathOption;
import org.ow2.mind.cli.CommandLine;
import org.ow2.mind.cli.CommandOptionHandler;
import org.ow2.mind.cli.InvalidCommandLineException;
import org.ow2.mind.plugin.util.Assert;

public class ExtFilesOptionHandler implements CommandOptionHandler {

	/** The ID of the "src-path" option. */
	public static final String  EXT_FILES_ID             = "org.ow2.mind.ext.ExtFiles";

	private static final String EXT_FILES_CONTEXT_KEY = "ext-files";

	@SuppressWarnings("unchecked")
	public static List<String> getExtFiles(final Map<Object, Object> context) {
		List<String> ext = (List<String>) context.get(EXT_FILES_CONTEXT_KEY);
		if (ext == null) ext = Collections.emptyList();
		return ext;
	}

	// TODO: allow using an ext-folder where to find ext-files ?
	public void processCommandOption(CmdOption cmdOption, CommandLine cmdLine,
			Map<Object, Object> context) throws InvalidCommandLineException {

		Assert.assertEquals(cmdOption.getId(), EXT_FILES_ID);
		final CmdPathOption extFilesOpt = Assert.assertInstanceof(cmdOption,
				CmdPathOption.class);
		
		List<String> newExtFiles = extFilesOpt.getPathValue(cmdLine);
		
		// do nothing if no ext files
		if (newExtFiles == null) return;
		
		// new list if did not exist in context, else we complete the old values
		// with the new ones and replace the old list with the new one in the context
		List<String> contextExtFiles = ExtFilesOptionHandler.getExtFiles(context);
		
		for (String extFile : newExtFiles) {
			// check ext files
			final File f = new File(extFile + ".ext");
			if (!f.exists()) {
				System.out.println("Warning '" + f.getAbsolutePath() + ".ext"
						+ "' extension can't be found ");
			} else if (f.isDirectory()) {
				System.out.println("Warning: \"" + extFile + ".ext"
						+ "\" is a directory, extension ignored.");
			} else {
				// Add when file exists
				contextExtFiles.add(extFile + ".ext");
			}
		}
		
		context.put(EXT_FILES_CONTEXT_KEY, contextExtFiles);
	}

}
