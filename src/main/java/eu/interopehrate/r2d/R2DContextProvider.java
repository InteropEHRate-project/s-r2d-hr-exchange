package eu.interopehrate.r2d;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: Stores the Spring context used to instantiate beans
 */

@Component
public class R2DContextProvider implements ApplicationContextAware {

	private static ApplicationContext applicationContext;
	
	public static ApplicationContext getApplicationContext() {
        return R2DContextProvider.applicationContext;
    }
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		R2DContextProvider.applicationContext = applicationContext;
	}

}
