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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.objectweb.fractal.adl.ADLException;
import org.objectweb.fractal.adl.Definition;
import org.objectweb.fractal.adl.Node;
import org.objectweb.fractal.adl.interfaces.Interface;
import org.objectweb.fractal.adl.interfaces.InterfaceContainer;
import org.objectweb.fractal.adl.merger.MergeException;
import org.objectweb.fractal.adl.util.FractalADLLogManager;
import org.ow2.mind.adl.AbstractDelegatingLoader;
import org.ow2.mind.adl.ast.ASTHelper;
import org.ow2.mind.adl.ast.Binding;
import org.ow2.mind.adl.ast.BindingContainer;
import org.ow2.mind.adl.ast.ImplementationContainer;
import org.ow2.mind.adl.ast.MindInterface;
import org.ow2.mind.adl.ast.Source;
import org.ow2.mind.annotation.AnnotationChecker;
import org.ow2.mind.annotation.AnnotationHelper.AnnotationDecoration;
import org.ow2.mind.cli.SrcPathOptionHandler;
import org.ow2.mind.ext.cli.ExtFilesOptionHandler;
import org.ow2.mind.ext.parser.EXTJTBParser;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class EXTLoader extends AbstractDelegatingLoader {

	@Inject
	EXTJTBParser processor;
	@Inject
	protected AnnotationChecker annotationCheckerItf;

	private Map<Object, Object> context;

	public List<Definition>	exts; 
	public static String 	EXT_EXTENSION = ".ext";

	/**
	 * Logger.
	 */
	private static Logger logger = FractalADLLogManager.getLogger("EXT");

	/**
	 * In this implementation, unlike with Think, we want to decorate/annotate
	 * only the current node, no recursion.
	 */
	public Definition load(String name, Map<Object, Object> context)
			throws ADLException {

		this.context = context;

		// reinitialize at every call (else we get duplicates, and the same annotations get applied multiple times which
		// the AnnotationHelper do not like at all)
		exts = new ArrayList<Definition>();

		// Call the EXTJTBProcessor/Parser to load extensions from the context as Definitions.
		// The extension Definitions list is then stored in "exts". 
		loadExtensions(context);

		// Load the target definition
		Definition d = clientLoader.load(name, context);

		// Apply
		if (!exts.isEmpty())
			applyExt(exts, d);

		return d;
	}

	/**
	 * Load extension file from source-path.
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	protected InputStream getEXT(final String fileName) {
		// running from command line ? (source-path is initialized in this case)
		ClassLoader loader = SrcPathOptionHandler.getSourceClassLoader(context);
		if (loader == null)
			// running from tests instead
			loader = getClass().getClassLoader();
		final InputStream is = loader.getResourceAsStream(fileName);
		return is;
	}

	private void loadExtensions(Map<Object, Object> context) throws ADLException {

		List<String> extFiles = ExtFilesOptionHandler.getExtFiles(context);
		List<Definition> extDefs = null;

		for (String extFile : extFiles) {

			InputStream is = getEXT(extFile);
			if (is == null) { // file not found
				logger.warning("Requested '" + extFile + "' extension could not be found - skipping");
				continue; // ignore erroneous extension file
			}

			// TODO: check if the 2 last parameters are really useful in our case
			extDefs = processor.parseEXT(is, extFile.substring(0, extFile.length() - 4), extFile);
			for (Definition extDef : extDefs) {
				// Inspired from the adl-frontend AnnotationLoader class
				// We wish to load the annotations from our extension definitions (it not only checks, but also converts
				// the annotation decoration text to real objects attached to the AST)
				annotationCheckerItf.checkAnnotations(extDef, context);
				exts.add(extDef);
			}
		}
	}

	/* Was used for "if ..." properties suffix, for example 
	 * provides fractal.api.AttributeController as att-controller in AttributeController if hasAttribute
	private boolean checkCondition(Node n, ComponentContainer container) {
		String cond = (String) n.astGetDecoration("adl-condition");
		if (cond != null) {
			if (cond.equals("hasAttribute")) {
				Attributes atts = ((AttributesContainer) container).getAttributes();
				if (atts == null) 
					return false;
				return (atts.getAttributes().length > 0);
			} else if (cond.equals("hasCltItf")) {
				Interface[] itfs = ((InterfaceContainer) container).getInterfaces();
				for (Interface itf : itfs) {
					if (((TypeInterface) itf).getRole().equals(TypeInterface.CLIENT_ROLE))
						return true;
				}
				return false;
			} else if (cond.equals("hasExtensibleCltItf")) {
				Interface[] itfs = ((InterfaceContainer) container).getInterfaces();
				for (Interface itf : itfs) {
					if (((TypeInterface) itf).getRole().equals(TypeInterface.CLIENT_ROLE)) {
						if (((TypeInterface) itf).getCardinality().equals(TypeInterface.COLLECTION_CARDINALITY)) {
							if (ASTHelper.hasProperty((Node) itf, NuptseProperty.EXTENSIBLE.value(), "true"))
								return true;
						}
					}
				}
				return false;
			} else if (cond.equals("hasOnlyFixedCltItf")) {
				Interface[] itfs = ((InterfaceContainer) container).getInterfaces();
				boolean hasCltItfs = false;
				for (Interface itf : itfs) {
					if (((TypeInterface) itf).getRole().equals(TypeInterface.CLIENT_ROLE)) {
						hasCltItfs = true;
						if (((TypeInterface) itf).getCardinality().equals(TypeInterface.COLLECTION_CARDINALITY)) {
							if (ASTHelper.hasProperty((Node) itf, NuptseProperty.EXTENSIBLE.value(), "true"))
								return false;
						}
					}
				}
				return hasCltItfs;
			} else if (cond.startsWith("hasNoImpl")) {
				String itfName = cond.substring(10, cond.length() - 1);
				boolean found = false;
				for (Interface itf : ((InterfaceContainer) container).getInterfaces()) {
					if (((TypeInterface) itf).getName().equals(itfName)) {
						found = true;
						break;
					}
				}
				if (!found) 
					return false;
				for (Implementation impl : ((ImplementationContainer) container).getImplementations()) {
					for (Interface itf : ((InterfaceContainer) impl).getInterfaces()) {
						if (((TypeInterface) itf).getName().equals(itfName)) {
							return false;
						}
					}
				}
				return true;
			}
			return true;
		}
		return true;
	}
	 */

	/* The ancestor of Annotations: Keeping the algorithm since it may be useful in the future
	private void applyProperties(Node ext, Node n) {
		if (ext instanceof PropertiesContainer) {
			Properties extProps = ((PropertiesContainer) ext).getProperties();
			Properties props = ((PropertiesContainer) n).getProperties();
			if (extProps != null) {
				if (props == null) {
					((PropertiesContainer) n).setProperties(ASTHelper.copyNode(extProps, true));
				} else {
					for (Property prop : extProps.getPropertys()) {
						Property p = ASTHelper.getProperty(n, prop.getDefinition());
						if (p != null) {
							if (!"true".equals(p.getIsFinal())) {
								p.setValue(prop.getValue());              
								p.setIsFinal(prop.getIsFinal());              
							} else {
								p = p;
							}
						} else {
							props.addProperty(ASTHelper.copyNode(prop, true));
						}
					}
				}
			}
		}
	}
	 */

	/**
	 * Merging annotations from the extension node and the target definition node.
	 * In the case of duplicates, the merge strategy is to override with the extension annotation.
	 * 
	 * @param The extension node from which to get annotations from.
	 * @param The definition node to apply annotations to.
	 * @throws ADLException 
	 */
	private void applyAnnotations(Node ext, Node n) {

		// Let's merge the annotations, taking shorcuts inspired from AnnotationHelper !

		AnnotationDecoration extDecoration = (AnnotationDecoration) ext.astGetDecoration("annotations");
		if (extDecoration == null)
			return;

		AnnotationDecoration nDecoration = (AnnotationDecoration) n.astGetDecoration("annotations");
		if (nDecoration == null) {
			nDecoration = new AnnotationDecoration();
		}

		AnnotationDecoration mergeResultDecoration = null;

		try {
			mergeResultDecoration = (AnnotationDecoration) nDecoration.mergeDecoration(extDecoration);
		} catch (MergeException e) {
			// This exception will never happen since there is not a single branch of code where it is thrown !
			e.printStackTrace();
		}

		if (mergeResultDecoration != null)
			n.astSetDecoration("annotations", mergeResultDecoration);

	}

	/*
	private void applyImplem(ComponentContainer ext, ComponentContainer container) {
		for (Implementation i : ((ImplementationContainer) ext).getImplementations()) {
			if (!checkCondition((Node) i, container)) continue;
			ExtendedImplementation impl = (ExtendedImplementation) i;
			String implName = impl.getName(); 
			if (implName.equals("*")) {
				for (Implementation i2 : ((ImplementationContainer) container).getImplementations()) {
					ExtendedImplementation impl2 = (ExtendedImplementation) i2;
					applyProperties((Node) impl, (Node) impl2);
					applyContent((ContentFileContainer) impl, (ContentFileContainer) impl2, container);
				}
			} else {
				boolean exist = false;
				ExtendedImplementation impl2 = null;
				for (Implementation i2 : ((ImplementationContainer) container).getImplementations()) {
					impl2 = (ExtendedImplementation) i2;
					if (impl2.getName().equals(implName)) {
						exist = true;
						break;
					}
				}
				if (!exist) {
					ASTHelper.addImplementation(container, impl);
					ExtendedImplementation copy = ASTHelper.copyNode(impl, true);
					for (ContentFile cf : ((ContentFileContainer) impl).getContentFiles()) {
						((ContentFileContainer) impl).removeContentFile(cf);
					}
					applyContent((ContentFileContainer) copy, (ContentFileContainer) impl, container);
				} else {
					applyProperties((Node) impl, (Node) impl2);
					applyContent((ContentFileContainer) impl, (ContentFileContainer) impl2, container);
				}
			}
		}
	}*/

	private List<Interface> matchingInterfaces(Interface[] itfs, MindInterface extItf) {
		List<Interface> matchingItfs = new ArrayList<Interface>(Arrays.asList(itfs));
		String extItfName = extItf.getName();
		String extItfSignature = extItf.getSignature();

		// check if both are provided or both required
		for (Interface itf : itfs) {
			assert itf instanceof MindInterface;
			MindInterface mItf = (MindInterface) itf;
			if (!extItf.getRole().equals(mItf.getRole()))
				matchingItfs.remove(itf);
		}

		// check itf instance name
		if (!"*".equals(extItfName)) {
			for (Iterator<Interface> iter = matchingItfs.iterator(); iter.hasNext();) {
				Interface itf = (Interface) iter.next();
				assert itf instanceof MindInterface;
				if (!((MindInterface) itf).getName().equals(extItfName)) {
					iter.remove();
				}
			}
		}

		// check the type
		// do we need to check the path too ? since people may use absolute names with package
		// not only imports
		if (!"*".equals(extItfSignature)) {
			for (Iterator<Interface> iter = matchingItfs.iterator(); iter.hasNext();) {
				Interface itf = (Interface) iter.next();
				assert itf instanceof MindInterface;
				if (!((MindInterface) itf).getSignature().equals(extItfSignature)) {
					iter.remove();
				}
			}
		}

		return matchingItfs;
	}


	private boolean areSameKind(Definition extDef, Definition targetDef) {
		return ((ASTHelper.isComposite(extDef) && ASTHelper.isComposite(targetDef))
				|| ((ASTHelper.isPrimitive(extDef) && ASTHelper.isPrimitive(targetDef))
						// in the case of a primitive, both should be abstract or both should not be abstract, but no incompatibility allowed
						&& ((ASTHelper.isAbstract(extDef) && ASTHelper.isAbstract(targetDef))
								|| (!ASTHelper.isAbstract(extDef) && !ASTHelper.isAbstract(targetDef))
								)
						)
						|| (ASTHelper.isType(extDef) && ASTHelper.isType(targetDef)));
	}

	private void applyItfs(Definition extDef, Definition targetDef) {
		if (! (extDef instanceof InterfaceContainer))
			return;

		if (!areSameKind(extDef, targetDef))
			return;

		for (Interface i : ((InterfaceContainer) extDef).getInterfaces()) {
			//if (!checkCondition((Node) i, targetDef)) continue; // example: "if hasCltItf" 
			assert i instanceof MindInterface;
			MindInterface extItf = (MindInterface) i;
			List<Interface> matchingItfs = 
					matchingInterfaces(((InterfaceContainer) targetDef).getInterfaces(), extItf);
			if (matchingItfs.size() == 0) {
				return; // we don't currently want to add architectural elements
				//				if (!"*".equals(extItf.getName()) && !"*".equals(extItf.getSignature()))
				//					ASTHelper.addInterface(targetDef, extItf);
			} else {
				for (Interface itf : matchingItfs) {
					applyAnnotations(extItf, itf);
				}
			}
		}
	}

	/*
	private void applyAttributes(ComponentContainer ext, ComponentContainer container) {
		Attributes extAtts = ((AttributesContainer) ext).getAttributes();
		if (extAtts == null) return;
		Attributes atts = ((AttributesContainer) container).getAttributes();

		for (Attribute extAtt : extAtts.getAttributes()) {
			if (!checkCondition((Node) extAtt, container)) continue;
			String extAttName = extAtt.getName();
			if (extAttName.equals("*")) {
				if (atts == null) 
					continue;
				for (Attribute att : atts.getAttributes()) {
					applyProperties((Node) extAtt, (Node) att);
				}
			} else {
				Attribute att = null;
				if (atts == null) {
					atts = ASTHelper.copyNode(extAtts, true);
					((AttributesContainer) container).setAttributes(atts);
					FieldContainer fc = (FieldContainer) ((RecordDefinitionContainer) atts).getRecordDefinition(); 
					for (Field f : fc.getFields()) {
						fc.removeField(f);
					}
					for (Attribute a : atts.getAttributes()) {
						atts.removeAttribute(a);
					}
				}
				for (Attribute a : atts.getAttributes()) {
					if (a.getName().equals(extAttName)) {
						att = a;
						break;
					}
				}
				if (att == null) { // no matching attribute
					atts.addAttribute(ASTHelper.copyNode(extAtt, true));
				Field[] fields = ((FieldContainer) ((RecordDefinitionContainer) extAtts).getRecordDefinition()).getFields();
				for (Field f : fields) {
					if (f.getName().equals(extAtt.getName())) {
						((FieldContainer) ((RecordDefinitionContainer) atts).
								getRecordDefinition()).addField(ASTHelper.copyNode(f,  true));
					}
				}
				} else {
					applyProperties((Node) extAtt, (Node) att);
				}
			}
		}
	}*/


	/**
	 * TODO: Optimize this ugly heavy algorithm !
	 * @param ext
	 * @param definition
	 * @throws ADLException 
	 */
	private void applyBindings(Definition ext, Definition definition) throws ADLException {
		for (Binding extBinding : ((BindingContainer) ext).getBindings()) {
			String extFromComponent = extBinding.getFromComponent();
			String extFromInterface = extBinding.getFromInterface();
			String extToComponent = extBinding.getToComponent();
			String extToInterface = extBinding.getToInterface();

			for (Binding b : ((BindingContainer) definition).getBindings()) {

				String defFromComponent = b.getFromComponent();
				String defFromInterface = b.getFromInterface();
				String defToComponent = b.getToComponent();
				String defToInterface = b.getToInterface();

				if ((extFromComponent.equals("*") || extFromComponent.equals(defFromComponent)) &&
						(extFromInterface.equals("*") || extFromInterface.equals(defFromInterface)) &&
						(extToComponent.equals("*") || extToComponent.equals(defToComponent)) &&
						(extToInterface.equals("*") || extToInterface.equals(defToInterface))) {

					// apply annotations to the targeted binding
					applyAnnotations(extBinding, b);
				} /*else {
					if (!biExtFrom.compName.equals("*") && !biExtFrom.itfName.equals("*") && 
							!biExtTo.compName.equals("*") && !biExtTo.itfName.equals("*")) {
						((BindingContainer) ext).addBinding(extBinding);
					}
				}*/ /* we don't want to add bindings yet, only apply annotations */
			}
		}
	}

	/* Adding sub-components isn't useful yet with Mind
	private void applyComponent(ComponentContainer ext, ComponentContainer container) {
		// only adding new component is supported at the moment
		for (Component c : ext.getComponents()) {
			Component copy = ASTHelper.copyNode(c, true);
			ASTHelper.addComponent(container, copy);
		}
		applyProperties((Node) ext, (Node) container);
	}
	 */

	private void applyDefinition(Definition extDef, Definition targetDef) {

		String extDefPackage 		= "";
		String extDefSimpleName 	= "";
		String targetDefPackage 	= "";
		String targetDefSimpleName 	= "";

		String extDefName = extDef.getName(); 
		String targetDefName = targetDef.getName();

		int extensionLastDotIndex 	= extDefName.lastIndexOf('.');
		int definitionLastDotIndex 	= targetDefName.lastIndexOf('.');

		if (extensionLastDotIndex != -1) {
			extDefPackage 	= extDefName.substring(0, extensionLastDotIndex);
			extDefSimpleName	= extDefName.substring(extensionLastDotIndex + 1);
		}

		if (definitionLastDotIndex != -1) {
			targetDefPackage	= targetDefName.substring(0, definitionLastDotIndex);
			targetDefSimpleName	= targetDefName.substring(definitionLastDotIndex + 1);;
		}

		// handle case when definition has no package
		if (definitionLastDotIndex == -1) {
			// only allow cases where there are both NO package, or extension package equals to "**" (any)
			if (extensionLastDotIndex != -1 && !extDefPackage.equals("**"))
				return;

			// in any case if there were both no package, the String will be "" (not null) so we can continue
		}

		// check if the extension can be applied to the target definition

		// first check compatibility
		if (areSameKind(extDef, targetDef)
				// now check names are ok
				&& ((extDefSimpleName.equals("*") || extDefSimpleName.equals(targetDefSimpleName)) &&
						(extDefPackage.equals("**") || extDefPackage.equals(targetDefPackage)) ))

			// apply annotations
			applyAnnotations(extDef, targetDef);
	}

	/* What the hell are directives useful for anyway ?
	private void applyDirectives(ComponentContainer ext, ComponentContainer container) {

		Printer.debug("apply extension " + ext);

		if (ext instanceof ImplementationContainer) {
			for (Implementation impl : ((ImplementationContainer) ext).getImplementations()) {
				if (impl instanceof DirectiveContainer) {
					for (Directive directive : ((DirectiveContainer) impl).getDirectives()) {
						Directive copy = ASTHelper.copyNode(directive, true);

						Implementation[] implems = ((ImplementationContainer) container).getImplementations();

						for (Implementation containerImpl : implems)
							((DirectiveContainer) containerImpl).addDirective(copy);
					}
				}
			}
		}
	}
	 */

	private void applyContent(Definition extDef, Definition targetDef) {

		assert ASTHelper.isPrimitive(extDef);
		assert ASTHelper.isPrimitive(targetDef);

		for (Source extSource : ((ImplementationContainer) extDef).getSources()) {
			for (Source targetSource : ((ImplementationContainer) targetDef).getSources()) {      
				if (targetSource.getPath() != null) {
					if(extSource.getPath().equals("*") || targetSource.getPath().equals(extSource.getPath()))
						applyAnnotations(extSource, targetSource);
					// TODO: else log...
				} else if (targetSource.getCCode() != null) {
					if(extSource.getPath().equals("*"))
						applyAnnotations(extSource, targetSource);
					// TODO: else log...
				}
			}
		}
	}

	/**
	 * The function applies appropriate extensions, from the list of extensions given in parameter,
	 * to a given component. The function then recurses to find more subcomponents concerned by
	 * element from this extensions list.
	 * @param exts : List<ComponentContainer> the list of extensions,
	 * @param definition : is the component to which the extensions list is attempted to be apply
	 * @throws SAXException
	 */
	private void applyExt(List<Definition> exts, Definition definition) {
		List<Definition> validExts = new ArrayList<Definition>();

		for (Definition ext : exts) {
			if (isConcernedByExt(ext, definition.getName())) {
				validExts.add(ext);

				if (definitionFullNameMatch(ext, definition)) {

					// TODO: check why a copy...?
					//Definition extCopy = ASTHelper.copyNode(ext, true);

					try {
						// Here type doesn't matter
						applyDefinition(ext, definition);
						applyItfs(ext, definition);

						if (ASTHelper.isPrimitive(ext) && ASTHelper.isPrimitive(definition))
							applyContent(ext, definition);

						// Here we need to check
						if (ASTHelper.isComposite(ext) && ASTHelper.isComposite(definition))
							applyBindings(ext, definition);
					} catch (ADLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//					applyItfs(extCopy, definition);
					//					applyImplem(extCopy, definition);
					//					applyAttributes(extCopy, definition);
					//					applyDirectives(extCopy, definition);
				}
			}
		}

		/* SSZ: Cancelling old-school recursion (may be useful later ?)
		// recurse
		// ext that doesn't match the components full name anymore are not used
		// in the recursion call
		for (Component c : definition.getComponents()){
			applyExt(exts, c);
		}
		 */
	}

	/**
	 * This function is used to test if the extension definition pattern matches the component definition full name.
	 * Definitions patterns can be defined as:
	 *    - **.* : to apply extensions to all components
	 *    - path.to.* : to apply extensions to all components having a path corresponding to path.to
	 *    - **.ComponentDefinition : to apply extensions to all components named ComponentDefinition
	 *    - path.to.ComponentDefinition : to apply extensions to a specific component definition
	 * @param extension
	 * @param compDefinition
	 * @return true if the component definition matches
	 */
	private boolean definitionFullNameMatch(Definition extension, Definition compDefinition) {
		String extDefinitionPackage = "";
		String extDefinitionName = "";
		String compDefPath = "";
		String compDefName = "";

		int extensionLastDotIndex 	= extension.getName().lastIndexOf(".");
		int definitionLastDotIndex 	= compDefinition.getName().lastIndexOf(".");

		if (extensionLastDotIndex != -1) {
			extDefinitionPackage = extension.getName().substring(0, extensionLastDotIndex);
			extDefinitionName = extension.getName().substring(extensionLastDotIndex + 1, extension.getName().length());
		}

		if (definitionLastDotIndex != -1) {
			compDefPath = compDefinition.getName().substring(0, definitionLastDotIndex);
			compDefName = compDefinition.getName().substring(definitionLastDotIndex + 1, compDefinition.getName().length());
		}

		// handle case when definition has no package
		if (definitionLastDotIndex == -1) {
			// only allow cases where there are both NO package, or extension package equals to "**" (any)
			if (extensionLastDotIndex != -1 && !extDefinitionPackage.equals("**"))
				return false;

			// in any case if there were both no package, the String will be "" (not null) so we can continue
		}

		// If the path is wrong, leave
		if (!extDefinitionPackage.equals("**") && !extDefinitionPackage.equals(compDefPath))
			return false;

		// Else finish evaluation
		return (extDefinitionName.equals("*") || extDefinitionName.equals(compDefName));
	}

	//	/**
	//	 * This function is used to test if the extension instance pattern matches the component instance full name.
	//	 * Components instances patterns can be defined as
	//	 *    - **.* : to apply extensions to all components instances
	//	 *    - path.to.* : to apply extensions to all components instances having a path corresponding to path.to
	//	 *    - path.**.* : to apply extensions to all sub-components instances of path
	//	 *    - **.Component : to apply extensions to all sub-components instances named Component
	//	 *    - path.to.Component : to apply extensions to a specific component instance
	//	 * @param extension
	//	 * @param compDefinition
	//	 * @return true if the component instance matches
	//	 */
	//	private boolean componentFullNameMatch(ComponentContainer extension, ComponentContainer compDefinition) {
	//		if (ASTHelper.getComponentFullname(extension) != null){
	//			String extComponentPath = ASTHelper.getComponentPath(extension);
	//			String extComponentName = ASTHelper.getComponentName(extension);
	//			String compPath = ASTHelper.getComponentPath(compDefinition);
	//			String compName = ASTHelper.getComponentName(compDefinition);
	//
	//			if (extComponentPath.contains(".**")){
	//				if (!compPath.startsWith(extComponentPath.substring(0, extComponentPath.indexOf(".**")))) return false;
	//			}else if (!extComponentPath.equals("**") && !extComponentPath.equals(compPath)) return false;
	//
	//			return (extComponentName.equals("*") 
	//					|| extComponentName.equals(compName));
	//		}else return true;
	//	}

	/**
	 * TODO: Check if it's still a good algorithm with Mind
	 * 
	 * isConcernedByExt is used to test if componentPartialPath sub-component's might be concern by
	 * a given extension
	 * @param extension
	 * @param definitionFullName
	 * @return true if some sub-components of componentPartialPath are possibly concerned by the extension
	 */
	private boolean isConcernedByExt(Definition extension, String definitionFullName){
		String extensionTargetName = extension.getName();
		String definitionPackage;
		int lastDotIndex = definitionFullName.lastIndexOf(".");
		if (lastDotIndex == -1) {
			definitionPackage = "";
		} else {
			definitionPackage = definitionFullName.substring(0, lastDotIndex);
		}
		
		if (extensionTargetName.contains(".**")){
			if(definitionFullName.length()>0){
				if(extensionTargetName.indexOf(".**") > definitionFullName.length()) {
					return false;
				} else {
					return extensionTargetName.startsWith(definitionFullName.substring(0, extensionTargetName.indexOf(".**")));
				}
			} else {
				return true;
			}
		} else {
			return extensionTargetName.startsWith("**") || extensionTargetName.startsWith(definitionPackage);
		}
	}

}
