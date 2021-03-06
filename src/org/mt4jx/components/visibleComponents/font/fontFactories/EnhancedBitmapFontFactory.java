 /**
 * This material was prepared as an account of work sponsored by an agency of the United States Government.<br>
 * Neither the United States Government nor the United States Department of Energy, nor any of their employees,<br> 
 * nor any of their contractors, subcontractors or their employees, makes any warranty, express or implied, or<br>
 * assumes any legal liability or responsibility for the accuracy, completeness, or usefulness or any information,<br> 
 * apparatus, product, or process disclosed, or represents that its use would not infringe privately owned rights.
 */
package org.mt4jx.components.visibleComponents.font.fontFactories;

import java.io.File;

//import org.mt4j.components.visibleComponents.font.fontFactories.BitmapFontFactory;

import org.mt4j.util.font.fontFactories.BitmapFontFactory;

/**
 * <p>
 * A factory for creating BitmapFont objects.
 * </p>
 * 
 * @author R.Scarberry
 */
public class EnhancedBitmapFontFactory extends BitmapFontFactory 
  implements IEnhancedFontFactory {
	
    /**
     * Not yet implemented.  This method simply returns null.
     */
	public String extractFontName(String fontFileName) {
	  int n = fontFileName.lastIndexOf('.');
	  if (n >= 0) {
	    String extension = fontFileName.substring(n).toLowerCase();
	    // If it's a true-type file, borrow code from the other class.
	    if (extension.equals(".ttf")) {
	      try {
            String[] names = EnhancedTTFontFactory.getFontNames(new File(fontFileName), 0);
            if (names.length > 1) {
              return names[1];
            }
	      } catch (Exception e) {
	        // Ignore.  Probably not a properly structured file.
          }
	    }
	  }
	  //TODO: Find out how to extract font names from .vlw and .otf files.
	  return null;	
	}
}