package org.ow2.mind.ext;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.fractal.adl.ADLException;
import org.objectweb.fractal.adl.Definition;
import org.objectweb.fractal.adl.Node;
import org.objectweb.fractal.adl.interfaces.Interface;
import org.objectweb.fractal.adl.types.TypeInterface;
import org.objectweb.fractal.adl.util.ClassLoaderHelper;
import org.ow2.mind.NameHelper;
import org.ow2.mind.PathHelper;
import org.ow2.mind.adl.AbstractDelegatingLoader;
import org.ow2.mind.adl.ast.ASTHelper;
import org.ow2.mind.adl.ast.Binding;
import org.ow2.mind.adl.ast.BindingContainer;
import org.ow2.mind.annotation.Annotation;
import org.ow2.mind.annotation.AnnotationChecker;
import org.ow2.mind.annotation.AnnotationHelper;
import org.ow2.mind.ext.cli.ExtFilesOptionHandler;
import org.ow2.mind.ext.parser.EXTJTBParser;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class EXTLoader extends AbstractDelegatingLoader {

	@Inject
	EXTJTBParser processor;
	@Inject
	protected AnnotationChecker annotationCheckerItf;

	public List<Definition>	exts = new ArrayList<Definition>(); 
	public static String 	EXT_EXTENSION = ".ext";

	/**
	 * In this implementation, unlike with Think, we want to decorate/annotate
	 * only the current node, no recursion.
	 */
	public Definition load(String name, Map<Object, Object> context)
			throws ADLException {

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
	 * Utility allowing to load an EXT file. Inspired from BasicADLLocator.
	 * @param name
	 * @param context
	 * @return
	 */
	public URL findSourceEXT(final String name, final Map<Object, Object> context) {
		return ClassLoaderHelper.getClassLoader(this, context).getResource(
				PathHelper.fullyQualifiedNameToPath(name, EXT_EXTENSION).substring(1));
	}

	/**
	 * TODO: check if return info can be null ?
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	protected InputStream getEXT(final String fileName) {
		final ClassLoader loader = getClass().getClassLoader();
		final InputStream is = loader.getResourceAsStream(fileName);
		return is;
	}

	private void loadExtensions(Map<Object, Object> context) throws ADLException {

		List<String> extFiles = ExtFilesOptionHandler.getExtFiles(context);

		Definition extDef = null;

		for (String extFile : extFiles) {
			// TODO: check if the 2 last parameters are really useful in our case
			extDef = processor.parseEXT(getEXT(extFile), extFile.substring(0, extFile.length() - 4), extFile);

			// Inspired from the adl-frontend AnnotationLoader class
			// We wish to load the annotations from our extension definitions
			annotationCheckerItf.checkAnnotations(extDef, context);

			exts.add(extDef);
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
	 * @param The extension node from which to get annotations from.
	 * @param The definition node to apply annotations to.
	 * @throws ADLException 
	 */
	private void applyAnnotations(Node ext, Node n) throws ADLException {
		Annotation[] extAnnos = AnnotationHelper.getAnnotations(ext);

		for (Annotation extAnno : extAnnos)
			AnnotationHelper.addAnnotation(n, extAnno);

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

	private List<Interface> matchingInterfaces(Interface[] itfs, TypeInterface extItf) {
		List<Interface> matchingItfs = new ArrayList<Interface>(Arrays.asList(itfs));
		String extItfName = extItf.getName();
		String extItfSignature = extItf.getSignature();
		for (Interface itf : itfs) {
			if (!extItf.getRole().equals(((TypeInterface) itf).getRole())) {
				matchingItfs.remove(itf);

			}
		}

		if (!"*".equals(extItfName)) {
			for (Iterator<Interface> iter = matchingItfs.iterator(); iter.hasNext();) {
				TypeInterface itf = (TypeInterface) iter.next();
				if (!itf.getName().equals(extItfName)) {
					iter.remove();
				}
			}
		}

		if (!"*".equals(extItfSignature)) {
			for (Iterator<Interface> iter = matchingItfs.iterator(); iter.hasNext();) {
				TypeInterface itf = (TypeInterface) iter.next();
				if (!itf.getSignature().equals(extItfSignature)) {
					iter.remove();
				}
			}
		}
		return matchingItfs;
	}

	/*
	private void applyItfs(ComponentContainer ext, ComponentContainer container) {
		for (Interface i : ((InterfaceContainer) ext).getInterfaces()) {
			if (!checkCondition((Node) i, container)) continue;
			TypeInterface extItf = (TypeInterface) i;
			List<Interface> matchingItfs = 
					matchingInterfaces(((InterfaceContainer) container).getInterfaces(), extItf);
			if (matchingItfs.size() == 0) {
				if (!"*".equals(extItf.getName()) && !"*".equals(extItf.getSignature()))
					ASTHelper.addInterface(container, extItf);
			} else {
				for (Interface itf : matchingItfs) {
					applyProperties((Node) extItf, (Node) itf);
				}
			}
		}
	}


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

		String extDefName = extDef.getName(); 
		String targetDefName = targetDef.getName();

		String extDefPackage 	= extDefName.substring(0, extDefName.lastIndexOf('.'));
		String extDefSimpleName	= extDefName.substring(extDefName.lastIndexOf('.') + 1);
		String targetDefPackage		= targetDefName.substring(0, targetDefName.lastIndexOf('.'));
		String targetDefSimpleName	= targetDefName.substring(targetDefName.lastIndexOf('.') + 1);;


		// check if the extension can be applied to the target definition

		// first check compatibility
		if (	((ASTHelper.isComposite(extDef) && ASTHelper.isComposite(targetDef))
				|| ((ASTHelper.isPrimitive(extDef) && ASTHelper.isPrimitive(targetDef))
						// in the case of a primitive, both should be abstract or both should not be abstract, but no incompatibility allowed
						&& ((ASTHelper.isAbstract(extDef) && ASTHelper.isAbstract(targetDef))
								|| (!ASTHelper.isAbstract(extDef) && !ASTHelper.isAbstract(targetDef))
								)
						)
						|| (ASTHelper.isType(extDef) && ASTHelper.isType(targetDef))
				)

				// now check names are ok
				&& ((extDefSimpleName.equals("*") || extDefSimpleName.equals(targetDefSimpleName)) &&
						(extDefPackage.equals("**") || extDefPackage.equals(targetDefPackage)) ))

			// apply annotations
			try {
				applyAnnotations(extDef, targetDef);
			} catch (ADLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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

	/* We don't want to add source files yet
	private void applyContent(ContentFileContainer ext, ContentFileContainer container, ComponentContainer comp) {

		Printer.debug("apply extension " + ext);

		for (ContentFile contentFile : ((ContentFileContainer) ext).getContentFiles()) {
			if (!checkCondition((Node) contentFile, comp)) continue;


			String contentFileName = contentFile.getName(); 
			//TODO: complete if we want to apply properties to content
			//		    if (contentFileName.equals("*")) {
			//		    for (Implementation i2 : ((ImplementationContainer) container).getImplementations()) {
			//		    ExtendedImplementation impl2 = (ExtendedImplementation) i2;
			//		    applyProperties((Node) impl, (Node) impl2);
			//		    }
			//		    } else {

			boolean exist = false;

			for (ContentFile contentFile2 : ((ContentFileContainer) container).getContentFiles()) {          
				if (contentFile2.getName().equals(contentFileName)) {
					exist = true;
					break;
				}
			}
			if(!exist) {
				ASTHelper.addContentFile(container, ASTHelper.copyNode(contentFile, false));
			}

			//		    }
			//		    }
		}
	}*/

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
						applyDefinition(ext, definition);
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
		int lastDotIndex = extension.getName().lastIndexOf(".");
		String extDefinitionPath = extension.getName().substring(0, lastDotIndex);
		String extDefinitionName = extension.getName().substring(lastDotIndex + 1, extension.getName().length());

		lastDotIndex = compDefinition.getName().lastIndexOf(".");
		String compDefPath = compDefinition.getName().substring(0, lastDotIndex);
		String compDefName = compDefinition.getName().substring(lastDotIndex + 1, compDefinition.getName().length());

		if (!extDefinitionPath.equals("**") && !extDefinitionPath.equals(compDefPath)) return false;
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
	 * isConcernByExt is used to test if componentPartialPath sub-component's might be concern by
	 * a given extension
	 * @param extension
	 * @param definitionPartialPath
	 * @return true if some sub-components of componentPartialPath are possibly concerned by the extension
	 */
	private boolean isConcernedByExt(Definition extension, String definitionPartialPath){
		String extensionTargetName = extension.getName();
		if (extensionTargetName.contains(".**")){
			if(definitionPartialPath.length()>0){
				if(extensionTargetName.indexOf(".**") > definitionPartialPath.length())
					return false;

				return extensionTargetName.startsWith(definitionPartialPath.substring(0, extensionTargetName.indexOf(".**")));
			} else
				return true;
		} else
			return extensionTargetName.startsWith("**") || extensionTargetName.startsWith(definitionPartialPath);
	}

}
