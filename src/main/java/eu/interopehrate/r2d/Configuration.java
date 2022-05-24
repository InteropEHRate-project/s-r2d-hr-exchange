package eu.interopehrate.r2d;

import java.io.IOException;
import java.util.Properties;

public final class Configuration {

	public static final String R2DA_ENDPOINT = "r2da.endpoint";
	public static final String R2DA_R2D_CONTEXT = "r2da.r2d.context";
	public static final String R2DA_SERVICES_CONTEXT = "r2da.services.context";
	public static final String R2DA_CREDENTIALS = "r2da.credentials";
	public static final String EHR_MW_ENDPOINT = "ehr.endpoint";
	public static final String EHR_MW_R2D_CONTEXT = "ehr.r2d.context";
	public static final String EHR_MW_SERVICES_CONTEXT = "ehr.services.context";
	
	private static Properties config = new Properties();
	
	
	static {
		try {
			config.load(Configuration.class.getClassLoader().getResourceAsStream("application.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getProperty(String name) {
		return config.getProperty(name);
	}

	public static String getR2DContextPath() {
		return config.getProperty(R2DA_ENDPOINT) + "/" + config.getProperty(R2DA_R2D_CONTEXT);
	}

	public static String getR2DServicesContextPath() {
		return config.getProperty(R2DA_ENDPOINT) + "/" + config.getProperty(R2DA_SERVICES_CONTEXT);
	}

	public static String getEHRMWContextPath() {
		return config.getProperty(EHR_MW_ENDPOINT) + "/" + config.getProperty(EHR_MW_R2D_CONTEXT);
	}

	public static String getEHRMWServicesContextPath() {
		return config.getProperty(EHR_MW_ENDPOINT) + "/" + config.getProperty(EHR_MW_SERVICES_CONTEXT);
	}
	
	public static String getDBPath() {
		return config.getProperty("r2da.storage.path");
	}
	
}
