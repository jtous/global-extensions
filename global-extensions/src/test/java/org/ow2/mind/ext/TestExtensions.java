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

package org.ow2.mind.ext;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.fractal.adl.Definition;
import org.objectweb.fractal.adl.Loader;
import org.ow2.mind.CommonFrontendModule;
import org.ow2.mind.adl.AbstractADLFrontendModule;
import org.ow2.mind.adl.annotation.predefined.Singleton;
import org.ow2.mind.adl.annotation.predefined.Static;
import org.ow2.mind.adl.ast.Binding;
import org.ow2.mind.adl.ast.BindingContainer;
import org.ow2.mind.adl.parser.ADLParser;
import org.ow2.mind.annotation.AnnotationHelper;
import org.ow2.mind.ext.cli.ExtFilesOptionHandler;
import org.ow2.mind.ext.parser.EXTJTBParser;
import org.ow2.mind.ext.parser.ExtJTBProcessor;
import org.ow2.mind.idl.IDLFrontendModule;
import org.ow2.mind.plugin.PluginLoaderModule;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TestExtensions {

	protected static final String DTD = "classpath://org/ow2/mind/adl/mind_v1.dtd";
	ExtJTBProcessor processor;
	Map<Object, Object> context;
	Loader              loader;


	@BeforeMethod(alwaysRun = true)
	protected void setUp() throws Exception {

		final Injector injector = Guice.createInjector(new CommonFrontendModule(),
				new PluginLoaderModule(), new IDLFrontendModule(), new AbstractADLFrontendModule() {

			@SuppressWarnings("unused")
			protected void configureTest() {
				bind(Loader.class).toChainStartingWith(EXTLoader.class)
				.endingWith(ADLParser.class);
			}

			@SuppressWarnings("unused")
			protected void configureEXTJTBParser() {
				bind(EXTJTBParser.class).to(ExtJTBProcessor.class);
			}
		});

		loader = injector.getInstance(Loader.class);

		processor = injector.getInstance(ExtJTBProcessor.class);

		context = new HashMap<Object, Object>();
	}

	protected InputStream getEXT(final String fileName) throws Exception {
		final ClassLoader loader = getClass().getClassLoader();
		final InputStream is = loader.getResourceAsStream(fileName);
		assertNotNull(is, "Can't find input file \"" + fileName + "\"");
		return is;
	}

	/**
	 * Test if the extension loader can apply @Static successfully on all bindings of a composite, with the help
	 * of the all-static extension.
	 * @throws Exception
	 */
	@Test(groups = {"functional"})
	public void testApplyAllStatic() throws Exception {

		// Init the list of ext-files
		List<String> extFiles = new ArrayList<String>();
		extFiles.add("all-static.ext");
		context.put(ExtFilesOptionHandler.EXT_FILES_CONTEXT_KEY, extFiles);
		
		Definition d = loader.load("simple.Composite", context);
		assertTrue(d instanceof BindingContainer, "Loaded composite didn't contain any binding.");

		// For all bindings check that the annotation has been applied correctly
		Binding[] bindings = ((BindingContainer) d).getBindings();
		for (Binding b : bindings) {
			Static staticAnno = AnnotationHelper.getAnnotation(b, Static.class);
			assertNotNull(staticAnno, "Binding wasn't annotated @Static ! all-static.ext failed.");
		}

		return;
	}

	/**
	 * Test if the extension loader can apply @Singleton successfully on a composite, with the help
	 * of the composite-singleton extension.
	 * @throws Exception
	 */
	@Test(groups = {"functional"})
	public void testApplyCompositeSingleton() throws Exception {

		// Init the list of ext-files
		List<String> extFiles = new ArrayList<String>();
		extFiles.add("composite-singleton.ext");
		context.put(ExtFilesOptionHandler.EXT_FILES_CONTEXT_KEY, extFiles);

		Definition d = loader.load("simple.Composite", context);

		Singleton singletonAnno = AnnotationHelper.getAnnotation(d, Singleton.class);

		assertNotNull(singletonAnno, "Expected definition to be transformed as a Singleton, but was not - composite-singleton.ext failed.");

		return;
	}

	/**
	 * Just a combination of the two previous tests.
	 * @throws Exception
	 */
	@Test(groups = {"functional"})
	public void testApplyCompositeSingletonAndAllStatic() throws Exception {

		// Init the list of ext-files
		List<String> extFiles = new ArrayList<String>();
		extFiles.add("all-static.ext");
		extFiles.add("composite-singleton.ext");
		context.put(ExtFilesOptionHandler.EXT_FILES_CONTEXT_KEY, extFiles);

		Definition d = loader.load("simple.Composite2", context);

		// Here come the checks
		Singleton singletonAnno = AnnotationHelper.getAnnotation(d, Singleton.class);		
		assertNotNull(singletonAnno, "Expected definition to be transformed as a Singleton, but was not - composite-singleton.ext failed.");

		assertTrue(d instanceof BindingContainer, "Loaded composite didn't contain any binding.");

		Binding[] bindings = ((BindingContainer) d).getBindings();
		// For all bindings check that the annotation has been applied correctly
		for (Binding b : bindings) {
			Static staticAnno = AnnotationHelper.getAnnotation(b, Static.class);
			assertNotNull(staticAnno, "Binding wasn't annotated @Static ! all-static.ext failed.");
		}

		return;
	}
	
	/**
	 * Just a combination of the two first tests, except the 2 are in the same ext file.
	 * @throws Exception
	 */
	@Test(groups = {"functional"})
	public void testApplyCompositeAllSingletonStatic() throws Exception {

		// Init the list of ext-files
		List<String> extFiles = new ArrayList<String>();
		extFiles.add("composite-all-static-singleton.ext");
		context.put(ExtFilesOptionHandler.EXT_FILES_CONTEXT_KEY, extFiles);

		Definition d = loader.load("simple.Composite", context);

		// Here come the checks
		Singleton singletonAnno = AnnotationHelper.getAnnotation(d, Singleton.class);		
		assertNotNull(singletonAnno, "Expected definition to be transformed as a Singleton, but was not - composite-singleton.ext failed.");

		assertTrue(d instanceof BindingContainer, "Loaded composite didn't contain any binding.");

		Binding[] bindings = ((BindingContainer) d).getBindings();
		// For all bindings check that the annotation has been applied correctly
		for (Binding b : bindings) {
			Static staticAnno = AnnotationHelper.getAnnotation(b, Static.class);
			assertNotNull(staticAnno, "Binding wasn't annotated @Static ! all-static.ext failed.");
		}

		return;
	}
	
	/**
	 * Test if the extension loader can apply @Static successfully on all bindings of a composite, with the help
	 * of the all-static extension.
	 * @throws Exception
	 */
	@Test(groups = {"functional"})
	public void testApplyAllStaticToTemplate() throws Exception {

		// Init the list of ext-files
		List<String> extFiles = new ArrayList<String>();
		extFiles.add("all-static.ext");
		context.put(ExtFilesOptionHandler.EXT_FILES_CONTEXT_KEY, extFiles);
		
		// Do the job
		Definition d = loader.load("simple.TemplateComposite<Primitive1>", context);
		assertTrue(d instanceof BindingContainer, "Loaded composite didn't contain any binding.");

		// For all bindings check that the annotation has been applied correctly
		Binding[] bindings = ((BindingContainer) d).getBindings();
		for (Binding b : bindings) {
			Static staticAnno = AnnotationHelper.getAnnotation(b, Static.class);
			assertNotNull(staticAnno, "Binding wasn't annotated @Static ! all-static.ext failed.");
		}

		return;
	}
}
