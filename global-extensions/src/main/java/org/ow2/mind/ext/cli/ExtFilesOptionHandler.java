/**
 * Copyright (C) 2013 Schneider-Electric
 *
 * This file is part of "Mind Compiler" is free software: you can redistribute 
 * it and/or modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact: mind@ow2.org
 *
 * Authors: Stephane Seyvoz
 * Contributors: 
 */

package org.ow2.mind.ext.cli;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.objectweb.fractal.adl.util.ClassLoaderHelper;
import org.ow2.mind.PathHelper;
import org.ow2.mind.cli.CmdOption;
import org.ow2.mind.cli.CmdPathOption;
import org.ow2.mind.cli.CommandLine;
import org.ow2.mind.cli.CommandOptionHandler;
import org.ow2.mind.cli.InvalidCommandLineException;
import org.ow2.mind.plugin.util.Assert;

public class ExtFilesOptionHandler implements CommandOptionHandler {

	/** The ID of the "src-path" option. */
	public static final String  EXT_FILES_ID             = "org.ow2.mind.ext.ExtFiles";

	public static final String EXT_FILES_CONTEXT_KEY = "ext-files";

	@SuppressWarnings("unchecked")
	public static List<String> getExtFiles(final Map<Object, Object> context) {
		List<String> ext = (List<String>) context.get(EXT_FILES_CONTEXT_KEY);
		if (ext == null) ext = Collections.emptyList();
		return ext;
	}

	/**
	 * Utility allowing to load an EXT file. Inspired from BasicADLLocator.
	 * @param name
	 * @param context
	 * @return
	 */
	public URL findSourceEXT(final String name, final Map<Object, Object> context) {
		return ClassLoaderHelper.getClassLoader(this, context).getResource(name + ".ext");
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
		
		// need storage for the new list that will replace the previous one, but
		// with valid content
		List<String> validContextExtFiles = new ArrayList<String>();
		
		for (String extFile : newExtFiles) {
			// check ext files
			
			URL extURL = findSourceEXT(extFile, context);
			
			final File f = new File(extURL.getPath());
			if (!f.exists()) {
				System.out.println("Warning '" + f.getAbsolutePath()
						+ "' extension can't be found ");
			} else if (f.isDirectory()) {
				System.out.println("Warning: \"" + extFile
						+ "\" is a directory, extension ignored.");
			} else {
				// Add when file exists
				validContextExtFiles.add(extFile + ".ext");
			}
		}

		context.put(EXT_FILES_CONTEXT_KEY, validContextExtFiles);
	}

}
