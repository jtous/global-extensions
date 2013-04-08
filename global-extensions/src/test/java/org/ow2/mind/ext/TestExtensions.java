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
import org.objectweb.fractal.adl.Node;
import org.ow2.mind.CommonFrontendModule;
import org.ow2.mind.adl.ADLFrontendModule;
import org.ow2.mind.adl.AbstractADLFrontendModule;
import org.ow2.mind.adl.BasicDefinitionReferenceResolver;
import org.ow2.mind.adl.CacheLoader;
import org.ow2.mind.adl.CachingDefinitionReferenceResolver;
import org.ow2.mind.adl.DefinitionReferenceResolver;
import org.ow2.mind.adl.ErrorLoader;
import org.ow2.mind.adl.ExtendsLoader;
import org.ow2.mind.adl.annotation.predefined.Singleton;
import org.ow2.mind.adl.annotation.predefined.Static;
import org.ow2.mind.adl.ast.Binding;
import org.ow2.mind.adl.ast.BindingContainer;
import org.ow2.mind.adl.imports.ImportDefinitionReferenceResolver;
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

			protected void configureTest() {
				bind(Loader.class).toChainStartingWith(EXTLoader.class)
				.endingWith(ADLParser.class);
			}
			
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

	@Test(groups = {"functional"})
	public void testApplyAllStatic() throws Exception {

		// Init the list of ext-files
		List<String> extFiles = new ArrayList<String>();
		extFiles.add("all-static.ext");
		context.put(ExtFilesOptionHandler.EXT_FILES_CONTEXT_KEY, extFiles);
		
		// TODO: we should check if the @Static annotation is always available (the annotation processor throws
		// an exception an returns no annotation otherwise)
		Definition d = loader.load("simple.Composite", context);
		assertTrue(d instanceof BindingContainer, "Loaded composite didn't contain any binding.");
		
		Binding[] bindings = ((BindingContainer) d).getBindings();
		
		// For all bindings check that the annotation has been applied correctly
		for (Binding b : bindings) {
			Static staticAnno = AnnotationHelper.getAnnotation(b, Static.class);
			assertNotNull(staticAnno, "Binding wasn't annotated @Static ! all-static.ext failed.");
		}
		
		return;
	}
	
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
}
