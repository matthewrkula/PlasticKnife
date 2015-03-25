package com.mattkula.processing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import com.mattkula.processing.annotations.SetValue;

@SupportedAnnotationTypes(value= {"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class PlasticKnifeProcessor extends AbstractProcessor {
	
	public static final String INJECTOR_SUFFIX = "Injector";
	
	private Filer filer;
		
	Map<String, List<Element>> parentToChildrenMap = new HashMap<>();
	String plasticKnifeHeader = "package com.mattkula.processing;\n\n" + "import com.mattkula.processing.Injector;\n";
	 
	@Override
	public void init(ProcessingEnvironment env) {
		filer = env.getFiler();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		
		if (!roundEnv.processingOver()) {
			Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(SetValue.class);
			
			for (Element element : elements) {
				Element parent = element.getEnclosingElement();
				String parentName = parent.getSimpleName().toString();

				if (!parentToChildrenMap.keySet().contains(parentName)) {
					parentToChildrenMap.put(parentName, new ArrayList<Element>());
					addImport(parent.asType().toString());
				}
				parentToChildrenMap.get(parentName).add(element);
			}

			for (String classElement : parentToChildrenMap.keySet()) {
				String className = classElement + INJECTOR_SUFFIX;
				String plasticKnifeContent = String.format("public class %s implements Injector<%s> {\n\n", className, classElement);

				plasticKnifeContent += String.format("\tpublic void inject(%s injector) {\n", classElement);
				List<Element> childElements = parentToChildrenMap.get(classElement);

				for (Element element : childElements) {
					SetValue fv = element.getAnnotation(SetValue.class);
					plasticKnifeContent += String.format("\t\tinjector.%s = %d;\n", element.getSimpleName(), fv.value());
				}
				plasticKnifeContent += "\t}\n\n" + "}\n";

				JavaFileObject file = null;
				try {
					file = filer.createSourceFile("com/mattkula/processing/" + className);
					file.openWriter()
					.append(plasticKnifeHeader)
					.append(plasticKnifeContent)
					.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}

		return false;
	}
	
	public void addImport(String packageName) {
		plasticKnifeHeader += "import " + packageName + ";\n";
	}

}
