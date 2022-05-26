package eu.interopehrate.r2d.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.interopehrate.r2d.Configuration;

@RestController
@RequestMapping("/admin")
public class RestAdminController {
	
	@GetMapping(produces = "application/json")
	public String ping() {
		return String.format("R2DAccess Service version %s is up and running!",
				Configuration.getProperty("r2da.version"));
	}

	
	@GetMapping(path="/config", produces = "application/json")
	public String config() {
		
		return String.format("R2DAccess Service Configuration: \n "
				+ "Version: {}\n"
				+ "MaxRunningRequest: {}\n"
				+ "EquivalencePeriodInDays: {}\n"
				+ "ExpirationTimeInDays: {}\n"
				+ "MaxRunningRequest: {}\n",
				Configuration.getProperty("r2da.version"),
				Configuration.getProperty("r2da.maxConcurrentRunningRequestPerDay"),
				Configuration.getProperty("r2da.equivalencePeriodInDays"),
				Configuration.getProperty("r2da.expirationTimeInDays"));
	}

}
