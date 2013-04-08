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

import org.objectweb.fractal.adl.Loader;
import org.ow2.mind.adl.ADLFrontendModule;
import org.ow2.mind.adl.ADLLoader;
import org.ow2.mind.adl.AbstractADLFrontendModule;
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
import org.ow2.mind.ext.parser.EXTJTBParser;
import org.ow2.mind.ext.parser.ExtJTBProcessor;
import org.ow2.mind.inject.AbstractMindModule;

import com.google.inject.Key;
import com.google.inject.name.Names;

public class EXTModule extends AbstractMindModule {

	public static Key<Loader> extLoaderKey() {
		return Key.get(Loader.class, Names.named("global-extensions-loader"));
	}

	protected void configureLoader() {
		bind(Loader.class).to(extLoaderKey());
	}
	
	protected void configureExtLoader() {
		bind(extLoaderKey())
		.toChainStartingWith(CacheLoader.class)
		.followedBy(TemplateInstanceLoader.class)
		.followedBy(BinaryADLLoader.class)
		.followedBy(
				new AbstractADLFrontendModule.AnnotationProcessorProvider(binder(),
						ADLLoaderPhase.AFTER_CHECKING))
						.followedBy(AttributeCheckerLoader.class)
						.followedBy(AttributesNormalizerLoader.class)
						.followedBy(ParameterNormalizerLoader.class)
						.followedBy(ImplementationLoader.class)
						.followedBy(BindingCheckerLoader.class)
						.followedBy(UnboundInterfaceCheckerLoader.class)
						.followedBy(BindingNormalizerLoader.class)
						.followedBy(MembraneCheckerLoader.class)
						.followedBy(CompositeInternalInterfaceLoader.class)
						.followedBy(InterfaceSignatureLoader.class)
						.followedBy(InterfaceCheckerLoader.class)
						.followedBy(InterfaceNormalizerLoader.class)
						.followedBy(
								new AbstractADLFrontendModule.AnnotationProcessorProvider(binder(),
										ADLLoaderPhase.ON_SUB_COMPONENT))
										.followedBy(
												new AbstractADLFrontendModule.AnnotationProcessorProvider(binder(),
														ADLLoaderPhase.AFTER_EXTENDS))
														.followedBy(NoAnySubComponentLoader.class)
														.followedBy(ExtendsLoader.class)
														.followedBy(SubComponentResolverLoader.class)
														.followedBy(SubComponentNormalizerLoader.class)
														.followedBy(AnonymousDefinitionLoader.class)
														.followedBy(GenericDefinitionLoader.class)
														.followedBy(ImportCheckerLoader.class)
														// SSZ: Here we go: TODO: check if it's the best place
														.followedBy(EXTLoader.class)
														//
														.followedBy(
																new AbstractADLFrontendModule.AnnotationProcessorProvider(binder(),
																		ADLLoaderPhase.AFTER_PARSING)).followedBy(FactoryLoader.class)
																		.followedBy(ADLLoader.class).followedBy(AnnotationLoader.class)
																		.endingWith(ADLFrontendModule.parserKey());
	}

	protected void configureEXTJTBParser() {
		bind(EXTJTBParser.class).to(ExtJTBProcessor.class);
	}

}
