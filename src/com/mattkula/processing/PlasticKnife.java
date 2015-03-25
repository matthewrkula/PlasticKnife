package com.mattkula.processing;

public class PlasticKnife {
	
	@SuppressWarnings("unchecked")
	public static void inject(Object object) {
		try {
			Class<?> c = Class.forName("com.mattkula.processing." + object.getClass().getSimpleName() + PlasticKnifeProcessor.INJECTOR_SUFFIX);
			Injector<Object> injector = (Injector<Object>)c.newInstance();
			injector.inject(object);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
	}

}
