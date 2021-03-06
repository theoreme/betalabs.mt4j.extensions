/**
 * This material was prepared as an account of work sponsored by an agency of the United States Government.<br>
 * Neither the United States Government nor the United States Department of Energy, nor any of their employees,<br> 
 * nor any of their contractors, subcontractors or their employees, makes any warranty, express or implied, or<br>
 * assumes any legal liability or responsibility for the accuracy, completeness, or usefulness or any information,<br> 
 * apparatus, product, or process disclosed, or represents that its use would not infringe privately owned rights.
 */
package org.mt4jx.components.visibleComponents.font.fontFactories;

import org.mt4j.util.font.fontFactories.IFontFactory;

/**
 * <p>
 * Extension of IFontFactory which adds a method for
 * extraction of the font name from a file without 
 * instantiation of the font.  Ideally, implementations should
 * quickly extract the font name.
 * </p>
 * 
 * @author R.Scarberry
 */
public interface IEnhancedFontFactory extends IFontFactory {
	
	/**
	 * Returns the name of the font stored in the specified file.
	 * 
	 * @param fontFileName
	 * 
	 * @return the name of the font, or null if no font is stored in the file
	 *   or the name cannot be extracted.
	 */
	public String extractFontName(String fontFileName);
}
