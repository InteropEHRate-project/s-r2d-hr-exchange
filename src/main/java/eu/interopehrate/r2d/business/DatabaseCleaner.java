package eu.interopehrate.r2d.business;

import java.io.File;
import java.io.FileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.Configuration;

/**
 * 
 * This class is used to remove from the database the 
 * old responses already downloaded.
 * 
 * @author alessiograziani
 *
 */
public class DatabaseCleaner {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public void deleteExpiredFiles() {
		logger.info("Checking for expired files...");
		int expInDays = Integer.parseInt(Configuration.getProperty("r2da.expirationTimeInDays"));
		if (expInDays <= 0) {
			logger.info("Expiration time not set, no expired file.");
			return;
		}
			
		
		File db = new File(Configuration.getDBPath());
		File[] filesToDelete = db.listFiles(new ExpiredFileFilter());
		
		long now = System.currentTimeMillis();
		
		long diffInMillis = expInDays * 24 * 60 * 60 * 1000L;
		for (File f : filesToDelete) {
			if (now - f.lastModified() > diffInMillis) {
				logger.info("Deleting expired file: {}", f.getName());
				f.delete();
			}
		}
		
	}
	
	class ExpiredFileFilter implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			if (pathname.isFile() && pathname.getName().endsWith(".json"))
				return true;
			
			return false;
		}
	}
	
}