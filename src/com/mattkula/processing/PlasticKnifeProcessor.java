package com.mattkula.processing;

import java.io.IOException;
import java.lang.annotation.Annotation;
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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.mattkula.processing.annotations.Bind;
import com.mattkula.processing.annotations.OnClick;
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

	ClassName onClickListenerClassName = ClassName.get("android.view", "View", "OnClickListener");
	ClassName viewClassName = ClassName.get("android.view", "View");
	
	private Filer filer;

	@SuppressWarnings("serial")
	static List<Class<? extends Annotation>> myAnnotations = new ArrayList<Class<? extends Annotation>>() {{
		add(Bind.class);
		add(OnClick.class);
	}};
	
	@Override
	public void init(ProcessingEnvironment env) {
		filer = env.getFiler();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Map<Element, List<Element>> parentToChildrenMap = new HashMap<>();

		if (!roundEnv.processingOver()) {
			for (Class<? extends Annotation> annotation : myAnnotations) {
				sort(annotation, parentToChildrenMap, roundEnv);
			}

			for (Element classElement : parentToChildrenMap.keySet()) {
				String className = classElement.getSimpleName().toString();
				TypeName classType = TypeName.get(classElement.asType());

				TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className + INJECTOR_SUFFIX)
						.addSuperinterface(ParameterizedTypeName.get(ClassName.get(Injector.class), classType));

				MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("inject")
						.addModifiers(Modifier.PUBLIC)
						.addParameter(classType, "injector", Modifier.FINAL);

				List<Element> childElements = parentToChildrenMap.get(classElement);

				for (Element element : childElements) {
					if (element instanceof ExecutableElement) {
						getOnClickStatement((ExecutableElement)element, methodBuilder);
					} else {
						getBindStatement(element, methodBuilder);
					}
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
		return false;
	}

	private void getOnClickStatement(ExecutableElement element, MethodSpec.Builder parentMethodBuilder) {
		TypeSpec onClickClass = TypeSpec.anonymousClassBuilder("")
				.addSuperinterface(onClickListenerClassName)
				.addMethod(MethodSpec.methodBuilder("onClick")
						.addAnnotation(Override.class)
						.addModifiers(Modifier.PUBLIC)
						.addParameter(viewClassName, "view")
						.addStatement("injector.$N($L)", element.getSimpleName(), "view")
						.build())
                .build();

		OnClick annotation = element.getAnnotation(OnClick.class);
		parentMethodBuilder.addStatement("injector.findViewById($L).setOnClickListener($L)",
				annotation.value(), onClickClass.toString());
	}

	private void getBindStatement(Element element, MethodSpec.Builder parentMethodBuilder) {
		Bind annotation = element.getAnnotation(Bind.class);
		parentMethodBuilder.addStatement("injector.$L = ($T)injector.findViewById($L)",
				element.getSimpleName(), element.asType(), annotation.value());
	}
	
	public void sort(Class<? extends Annotation> annotation, Map<Element, List<Element>> map, RoundEnvironment roundEnv) {
		Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);

		for (Element element : elements) {
			Element parent = element.getEnclosingElement();
			if (!map.keySet().contains(parent)) {
				map.put(parent, new ArrayList<Element>());
			}
			map.get(parent).add(element);
		}
	}
}
