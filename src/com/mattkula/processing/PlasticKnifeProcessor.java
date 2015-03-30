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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.mattkula.processing.annotations.Bind;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

@SupportedAnnotationTypes(value= {"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class PlasticKnifeProcessor extends AbstractProcessor {
	
	public static final String INJECTOR_SUFFIX = "Injector";
	
	private Filer filer;
		
	Map<Element, List<Element>> parentToChildrenMap = new HashMap<>();
	 
	@Override
	public void init(ProcessingEnvironment env) {
		filer = env.getFiler();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		
		processBind(roundEnv);

		return false;
	}
	
	private void processBind(RoundEnvironment roundEnv) {
		if (!roundEnv.processingOver()) {
			Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Bind.class);
			
			for (Element element : elements) {
				Element parent = element.getEnclosingElement();
				if (!parentToChildrenMap.keySet().contains(element)) {
					parentToChildrenMap.put(parent, new ArrayList<Element>());
				}
				parentToChildrenMap.get(parent).add(element);
			}

			for (Element classElement : parentToChildrenMap.keySet()) {
				String className = classElement.getSimpleName().toString();
				TypeName classType = TypeName.get(classElement.asType());

				TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className + INJECTOR_SUFFIX)
						.addSuperinterface(ParameterizedTypeName.get(ClassName.get(Injector.class), classType));

				MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("inject")
						.addModifiers(Modifier.PUBLIC)
						.addParameter(classType, "injector");

				List<Element> childElements = parentToChildrenMap.get(classElement);
				for (Element element : childElements) {
					Bind annotation = element.getAnnotation(Bind.class);
					methodBuilder.addStatement("injector.$L = ($T)injector.findViewById($L)",
							element.getSimpleName(), element.asType(), annotation.value());
				}
				
				classBuilder.addMethod(methodBuilder.build());
				
				JavaFile javaFile = JavaFile
						.builder("com.mattkula.processing", classBuilder.build())
						.build();
				try {
					javaFile.writeTo(filer);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}
}
