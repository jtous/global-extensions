package org.ow2.mind.ext;

import org.objectweb.fractal.adl.Loader;
import org.ow2.mind.adl.ADLFrontendModule;
import org.ow2.mind.adl.ADLLoader;
import org.ow2.mind.adl.BinaryADLLoader;
import org.ow2.mind.adl.CacheLoader;
import org.ow2.mind.adl.ExtendsLoader;
import org.ow2.mind.adl.InterfaceCheckerLoader;
import org.ow2.mind.adl.InterfaceNormalizerLoader;
import org.ow2.mind.adl.SubComponentNormalizerLoader;
import org.ow2.mind.adl.SubComponentResolverLoader;
import org.ow2.mind.adl.annotation.ADLLoaderPhase;
import org.ow2.mind.adl.annotation.AnnotationLoader;
import org.ow2.mind.adl.anonymous.AnonymousDefinitionLoader;
import org.ow2.mind.adl.attribute.AttributeCheckerLoader;
import org.ow2.mind.adl.attribute.AttributesNormalizerLoader;
import org.ow2.mind.adl.binding.BindingCheckerLoader;
import org.ow2.mind.adl.binding.BindingNormalizerLoader;
import org.ow2.mind.adl.binding.UnboundInterfaceCheckerLoader;
import org.ow2.mind.adl.factory.FactoryLoader;
import org.ow2.mind.adl.generic.GenericDefinitionLoader;
import org.ow2.mind.adl.generic.NoAnySubComponentLoader;
import org.ow2.mind.adl.generic.TemplateInstanceLoader;
import org.ow2.mind.adl.idl.InterfaceSignatureLoader;
import org.ow2.mind.adl.implementation.ImplementationLoader;
import org.ow2.mind.adl.imports.ImportCheckerLoader;
import org.ow2.mind.adl.membrane.CompositeInternalInterfaceLoader;
import org.ow2.mind.adl.membrane.MembraneCheckerLoader;
import org.ow2.mind.adl.parameter.ParameterNormalizerLoader;
import org.ow2.mind.adl.parser.ADLParser;

import org.ow2.mind.ext.EXTLoader;
import org.ow2.mind.ext.parser.EXTJTBParser;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

public class EXTModule extends ADLFrontendModule {
	
//	protected void configureDefaultLoader() {
//	    bind(defaultLoaderKey())
//	        .toChainStartingWith(CacheLoader.class)
//	        .followedBy(TemplateInstanceLoader.class)
//	        .followedBy(BinaryADLLoader.class)
//	        .followedBy(
//	            new AnnotationProcessorProvider(binder(),
//	                ADLLoaderPhase.AFTER_CHECKING))
//	        .followedBy(AttributeCheckerLoader.class)
//	        .followedBy(AttributesNormalizerLoader.class)
//	        .followedBy(ParameterNormalizerLoader.class)
//	        .followedBy(ImplementationLoader.class)
//	        .followedBy(BindingCheckerLoader.class)
//	        .followedBy(UnboundInterfaceCheckerLoader.class)
//	        .followedBy(BindingNormalizerLoader.class)
//	        .followedBy(MembraneCheckerLoader.class)
//	        .followedBy(CompositeInternalInterfaceLoader.class)
//	        .followedBy(InterfaceSignatureLoader.class)
//	        .followedBy(InterfaceCheckerLoader.class)
//	        .followedBy(InterfaceNormalizerLoader.class)
//	        .followedBy(
//	            new AnnotationProcessorProvider(binder(),
//	                ADLLoaderPhase.ON_SUB_COMPONENT))
//	        .followedBy(
//	            new AnnotationProcessorProvider(binder(),
//	                ADLLoaderPhase.AFTER_EXTENDS))
//	        .followedBy(NoAnySubComponentLoader.class)
//	        .followedBy(ExtendsLoader.class)
//	        .followedBy(SubComponentResolverLoader.class)
//	        .followedBy(SubComponentNormalizerLoader.class)
//	        .followedBy(AnonymousDefinitionLoader.class)
//	        .followedBy(GenericDefinitionLoader.class)
//	        .followedBy(ImportCheckerLoader.class)
//	        // SSZ: Here we go
//	        .followedBy(EXTLoader.class)
//	        //
//	        .followedBy(
//	            new AnnotationProcessorProvider(binder(),
//	                ADLLoaderPhase.AFTER_PARSING)).followedBy(FactoryLoader.class)
//	        .followedBy(ADLLoader.class).followedBy(AnnotationLoader.class)
//	        .endingWith(parserKey());
//	  }
//	
//	/**
//	   * Returns the {@link Key} used to bind the ADL parser. This module simply
//	   * binds this key to {@link ADLParser} class. But another module can
//	   * {@link Modules#override override} this binding to chance the parser.
//	   * 
//	   * @return the {@link Key} used to bind the ADL parser.
//	   */
//	  public static Key<Loader> extParserKey() {
//	    return Key.get(Loader.class, Names.named("ext-parser"));
//	  }
//	
//	protected void configureParser() {
//	    bind(extParserKey()).to(EXTJTBParser.class);
//	  }
	
}
