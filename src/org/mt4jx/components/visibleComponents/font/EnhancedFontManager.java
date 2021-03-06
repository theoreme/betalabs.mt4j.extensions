/**
 * *********************************************************************
 * mt4j Copyright (c) 2008 - 2009, C.Ruff, Fraunhofer-Gesellschaft All rights
 * reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 **********************************************************************
 */
package org.mt4jx.components.visibleComponents.font;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.mt4j.util.font.FontManager;
import org.mt4j.util.font.IFont;
import org.mt4j.util.MT4jSettings;
import org.mt4j.util.MTColor;
import org.mt4j.util.logging.ILogger;
import org.mt4j.util.logging.MTLoggerFactory;
import org.mt4jx.components.visibleComponents.font.fontFactories.EnhancedBitmapFontFactory;
import org.mt4jx.components.visibleComponents.font.fontFactories.EnhancedSvgFontFactory;
import org.mt4jx.components.visibleComponents.font.fontFactories.EnhancedTTFontFactory;
import org.mt4jx.components.visibleComponents.font.fontFactories.IEnhancedFontFactory;

import processing.core.PApplet;
import processing.core.PFont;

/**
 * <p>
 * Enhanced manager to obtaining and caching fonts. Based upon
 * <tt>org.mt4j.components.visibleComponents.font.FontManager</tt>
 *
 * @author Christopher Ruff -- the FontManager class from which this
 * class is derived.
 * @author R.Scarberry -- new methods and changes.
 */
public class EnhancedFontManager
{

    private static final ILogger LOG = MTLoggerFactory.getLogger(EnhancedFontManager.class.getName());

    ;

    static {
    LOG.setLevel(ILogger.ERROR);
  }

  /** The font manager. */
  private static EnhancedFontManager instance = new EnhancedFontManager();

    /**
     * Maps font names to lists containing fonts with those names.
     */
    private final Map<String, List<IFont>> fonts;

    /**
     * The suffix to factory.
     */
    private final HashMap<String, IEnhancedFontFactory> suffixToFactory;

  // Paths to directories containing fonts, separated by File.pathSeparator
    // similarly to a java classpath.
    private String fontPath = MT4jSettings.DEFAULT_FONT_PATH;

  // Maps font names to the files that contains them.
    // If the file path is null, it's a system font.
    private Map<String, String> availableFonts;

    // Reverse mapping of availableFonts, but omitting mappings for which the file path is null
    private Map<String, String> availableFontsReverse;

    // Maps font resource names to the temp files they've been stored in.
    private final Map<String, String> fontResourcesToFiles;

    private static final int CACHE_MAX_SIZE = 10;

    /**
     * Gets the singleton instance.
     *
     * @return the instance
     *
     * this VectorFontManager, use <code>createFont</code> to create a font with
     * it
     */
    public static EnhancedFontManager getInstance()
    {
        return instance;
    }

    /**
     * Instantiates a new font manager.
     */
    private EnhancedFontManager()
    {
        fonts = new HashMap<String, List<IFont>>();
        suffixToFactory = new HashMap<String, IEnhancedFontFactory>();
        fontResourcesToFiles = new HashMap<String, String>();

        registerFontFactory(".svg", new EnhancedSvgFontFactory());

        final EnhancedBitmapFontFactory bitmapFontFactory = new EnhancedBitmapFontFactory();

    // Register default font factories
    // I've noticed on my windows 7 system that loading ttf fonts
        // with the bitmap font factory yields more pleasing results.
        // TODO: investigate to see if the ttf font factory ignores the antialiasing hint.
        registerFontFactory(".ttf", new EnhancedTTFontFactory() /*
         * bitmapFontFactory
         */);
        registerFontFactory("", bitmapFontFactory);
        registerFontFactory(".vlw", bitmapFontFactory);
        registerFontFactory(".otf", bitmapFontFactory);

        registerFontFactory(".bold", bitmapFontFactory);
        registerFontFactory(".bolditalic", bitmapFontFactory);
        registerFontFactory(".italic", bitmapFontFactory);
        registerFontFactory(".plain", bitmapFontFactory);

        // Start with the universal default.
        String fontPath = MT4jSettings.DEFAULT_FONT_PATH;
        switch (PApplet.platform)
        {
            case PApplet.WINDOWS:
                // 99% of the time in C:\Windows\Fonts
                final String drives = "CDEABFGH";
                final int len = drives.length();
                for (int i = 0; i < len; i++)
                {
                    final String fontDir = drives.substring(i, i + 1) + ":\\Windows\\Fonts";
                    if (new File(fontDir).isDirectory())
                    {
                        fontPath += File.pathSeparator + fontDir;
                        break;
                    }
                }
                break;
            case PApplet.MACOSX:
                // TODO:
                break;
            case PApplet.LINUX:
                // TODO:
                break;
            case PApplet.OTHER:
                // TODO:
                break;
        }

        this.fontPath = fontPath;
    }

    /**
     * Set the font path. This may be a list of directories containing font
     * files, separated by path separators. In other words, it uses the same
     * format as a java class path.
     *
     * @param fontPath
     */
    public void setFontPath(final String fontPath)
    {
        if (fontPath == null)
        {
            throw new NullPointerException();
        }
        if (!fontPath.equals(this.fontPath))
        {
            this.fontPath = fontPath;
            availableFonts = null;
        }
    }

    /**
     * Returns the font path, which is a list of directories in class path
     * format.
     *
     * @return the font path specification.
     */
    public String getFontPath()
    {
        return fontPath;
    }

    /**
     * Returns the font path parsed into individual directories.
     *
     * @return
     */
    public String[] fontPaths()
    {
        final StringTokenizer tokenizer = new StringTokenizer(getFontPath(), File.pathSeparator);
        final List<String> pathList = new ArrayList<String>(tokenizer.countTokens());
        while (tokenizer.hasMoreTokens())
        {
            final String path = tokenizer.nextToken().trim();
            if (path.length() > 0)
            {
                pathList.add(path);
            }
        }
        return pathList.toArray(new String[pathList.size()]);
    }

    // Ensures information on the available fonts is loaded and current.
    private void checkAvailableFontsCurrent()
    {
        if (availableFonts == null)
        {
            loadAvailableFonts();
        }
    }

  // Load the information on available fonts.
    //
    private void loadAvailableFonts()
    {

        final Map<String, String> availableFonts = new TreeMap<String, String>();
        final Map<String, String> availableFontsReverse = new TreeMap<String, String>();

        final String[] fps = fontPaths();

        // For filtering out files without extensions that map to factories.
        final Set<String> fileExtensions = new HashSet<String>(suffixToFactory.keySet());

        // Loop through the individual font directories.
        for (final String fp : fps)
        {

            final File dir = new File(fp);

            // Be sure it's an existing directory.
            if (dir.isDirectory())
            {

                // List only the files with font factories.
                final File[] files = dir.listFiles(new FileFilter()
                {
                    @Override
                    public boolean accept(final File f)
                    {
                        if (f.isFile())
                        {
                            final String fn = f.getName();
                            final int n = fn.lastIndexOf('.');
                            final String ext = (n >= 0) ? fn.substring(n) : "";
                            return fileExtensions.contains(ext);
                        }
                        return false;
                    }
                });

                // For each file, have the font factory extract the font name.
                for (final File file : files)
                {
                    final String fn = file.getName();
                    final int n = fn.lastIndexOf('.');
                    final String ext = (n >= 0) ? fn.substring(n) : "";
                    final IEnhancedFontFactory factory = suffixToFactory.get(ext);
                    if (factory != null)
                    {
                        final String filePath = file.getAbsolutePath();
            // It's important that this method return quickly and not consume
                        // many resources.
                        final String fontName = factory.extractFontName(filePath);
                        // Store font name mapped to its file path.
                        if ((fontName != null) && (fontName.length() > 0))
                        {
                            availableFonts.put(fontName, filePath);
                            availableFontsReverse.put(filePath, fontName);
                        }
                    }
                }

            }
        }

    // Several system fonts may not map to any of the files found,
        // yet they are still available. Add to the available fonts, but
        // map them to null.
        final String[] allFontNames = PFont.list();
        for (final String fn : allFontNames)
        {
            if (!availableFonts.containsKey(fn))
            {
                availableFonts.put(fn, null);
            }
        }

        /*
         * Just prints out all the info.
         */
    // Iterator<String> it = availableFonts.keySet().iterator();
        // while(it.hasNext()) {
        // String fn = it.next();
        // String filename = availableFonts.get(fn);
        // if (filename == null) filename = "[SYSTEM]";
        // System.out.println("\t\t... " + fn + " ==> " + filename);
        // }
    /*
         * 
         */
        this.availableFonts = availableFonts;
        this.availableFontsReverse = availableFontsReverse;
    }

    /**
     * Returns the names of the available fonts.
     *
     * @return the names as an array of strings.
     */
    public synchronized String[] availableFonts()
    {
        checkAvailableFontsCurrent();
        final Set<String> keys = availableFonts.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    /**
     * Returns true if a font with the specified name is available.
     *
     * @param fontName
     *
     * @return
     */
    public synchronized boolean isFontAvailable(final String fontName)
    {
        checkAvailableFontsCurrent();
        return availableFonts.containsKey(fontName);
    }

    /**
     * Returns the full path to the file holding the specified font.
     * This returns null for some system fonts.
     *
     * @param fontName
     *
     * @return
     */
    public synchronized String fontFilePath(final String fontName)
    {
        checkAvailableFontsCurrent();
        return availableFonts.get(fontName);
    }

    /**
     * Returns the file name extension for the file containing the specified
     * font if
     * the font is available. If not available, returns null.
     *
     * @param fontName
     *
     * @return file name extension for the font file. If null, the font with the
     *         specified name is not available. If non-null but blank, it is probably a
     *         system font
     *         that either is not in one of the font path directories, or is contained
     *         in a file with
     *         an extension not associated with a font factory.
     */
    public synchronized String fontFileExtension(final String fontName)
    {
        checkAvailableFontsCurrent();
        if (isFontAvailable(fontName))
        {
            final String fontPath = availableFonts.get(fontName);
            if (fontPath != null)
            {
                return getFontSuffix(fontPath);
            }
            return "";
        }
        return null;
    }

    /**
     * Gets the default font.
     *
     * @param app
     *            the app
     *
     * @return the default font
     */
    public IFont getDefaultFont(final PApplet app)
    {
        return this.createFontByName(app,
                FontManager.DEFAULT_FONT,
                FontManager.DEFAULT_FONT_SIZE,
                new MTColor(FontManager.DEFAULT_FONT_STROKE_COLOR),
                FontManager.DEFAULT_FONT_ANTIALIASING);
    }

    /**
     * Returns the name of the default font.
     *
     * @return
     */
    public String defaultFontName()
    {
        return FontManager.DEFAULT_FONT;
    }

    /**
     * Creates the font.
     *
     * @param pa
     *                     the pa
     * @param fontFileName
     *                     the font file name
     * @param fontSize
     *                     the font size
     * @param antiAliased
     *                     the anti aliased
     *
     * @return the i font
     */
    public IFont createFont(final PApplet pa, final String fontFileName, final int fontSize, final boolean antiAliased)
    {
        return this.createFontByName(pa, fontFileName, fontSize, new MTColor(FontManager.DEFAULT_FONT_FILL_COLOR), antiAliased);
    }

    /**
     * Loads and returns a font from a file. <br>
     * The file has to be located in the ./data/ directory of the program. <br>
     * Example: "IFont font = FontManager.createFont(papplet, "Pakenham.svg",
     * 100);"
     *
     * @param pa
     *                     the pa
     * @param fontFileName
     *                     the font file name
     * @param fontSize
     *                     the font size
     *
     * @return the i font
     */
    public IFont createFont(final PApplet pa, final String fontFileName, final int fontSize)
    {
        return this.createFont(pa, fontFileName, fontSize, new MTColor(FontManager.DEFAULT_FONT_FILL_COLOR));
    }

    /**
     * Loads and returns a font from a file. The font should be located in
     * one of the font directories. The returned font will be anti-aliased.
     *
     * @param pa
     *                     the pa
     * @param fontFileName
     *                     the name of the font file without the path.
     * @param fontSize
     *                     the font size
     * @param fillColor
     *                     the color
     *
     * @return the i font
     */
    public IFont createFont(final PApplet pa, final String fontFileName, final int fontSize, final MTColor color)
    {
        return this.createFontByName(pa, fontFileName, fontSize, color, true);
    }

    /**
     * Loads and returns a font from a file. The font should be located in
     * one of the font directories.
     *
     * @param pa
     *                     the pa
     * @param fontFileName
     *                     the name of the font file without the path.
     * @param fontSize
     *                     the font size
     * @param fillColor
     *                     the color
     * @param antiAliased
     *                     whether or not to anti-alias the font
     *
     * @return the i font
     */
    public IFont createFontByName(final PApplet pa, final String fontFileName, final int fontSize, final MTColor color, final boolean antiAliased)
    {

        checkAvailableFontsCurrent();

        // Try to find a file that exists in one of the font directories with that file name.
        String fontAbsolutePath = fontFileName;
        final String[] fontPaths = fontPaths();
        // Loop over the available extensions and check for requested font
        for (final String fileExtension : suffixToFactory.keySet())
        {
            for (final String fontPath : fontPaths)
            {
                final File f = new File(new File(fontPath), fontFileName + fileExtension);
                if (f.exists() && f.isFile())
                {
                    fontAbsolutePath = f.getAbsolutePath();
                    break;
                }
            }
        }

        return createFontFromFile(pa, fontAbsolutePath, fontSize, color, antiAliased);
    }

    /**
     * Loads and returns a font specified by name. The font name should
     * be one of those returned by availableFonts(). The returned font
     * will be anti-aliased.
     *
     * @param pa
     *                  the pa
     * @param fontName
     *                  the name of the font.
     * @param fontSize
     *                  the font size
     * @param fillColor
     *                  the color
     *
     * @return the font (null if not available)
     */
    public IFont createFontByName(final PApplet pa, final String fontName, final int fontSize, final MTColor color)
    {
        return createFontByName(pa, fontName, fontSize, color, true);
    }

    public IFont createFontFromResource(final PApplet pa, final String fontResourceName, final int fontSize, final MTColor color)
    {
        return this.createFontFromResource(pa, fontResourceName, fontSize, color, true);
    }

    /**
     * Loads and returns a font stored as a resource in a jar instead of
     * from a file in one of the font directories. The resource should be in the
     * jar at the path &quot;/data&quot;
     *
     * @param pa
     *                         the pa
     * @param fontResourceName
     *                         name of the font resource
     * @param fontSize
     *                         the font size
     * @param fillColor
     *                         the color
     * @param antiAliased
     *                         whether or not to anti-alias the font
     *
     * @return the font (null if not available)
     */
    public IFont createFontFromResource(final PApplet pa, final String fontResourceName, final int fontSize, final MTColor color, final boolean antiAliased)
    {

        String fontAbsolutePath = fontResourcesToFiles.get(fontResourceName);

        if (fontAbsolutePath == null)
        {

            final String resourcePath = MT4jSettings.DEFAULT_FONT_PATH + fontResourceName;

            try
            {
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
                // Copy it to a temp file, so the normal method can be used
                if (in != null)
                {

                    final File tempFile = File.createTempFile("tmpfont", getFontSuffix(fontResourceName));
                    tempFile.deleteOnExit();

                    try
                    {
                        OutputStream out = new FileOutputStream(tempFile);
                    
                        final byte[] buffer = new byte[4096];

                        int bytesRead = 0;
                        while ((bytesRead = in.read(buffer)) != -1)
                        {
                            out.write(buffer, 0, bytesRead);
                        }

                        fontAbsolutePath = tempFile.getAbsolutePath();
                        fontResourcesToFiles.put(fontResourceName, fontAbsolutePath);
                    } catch (final IOException e)
                    {
                        LOG.error("Error writing" + fontResourceName + " to temporary file");
                    }
                }
            } catch (final IOException e1)
            {
                LOG.error("Error creating temporary file for " + fontResourceName + " processing");
            }
        }

        return createFontFromFile(pa, fontAbsolutePath, fontSize, color, antiAliased);
    }

  // Private method for creating the font usually using the full font path.
    // If fontAbsolutePath is the name of a system font, the EnhancedBitmapFontFactory will
    // load it as a PFont.
    //
    private IFont createFontFromFile(final PApplet pa, String fontAbsolutePath, final int fontSize, final MTColor color, final boolean antiAliased)
    {

        checkAvailableFontsCurrent();

        String fontName = availableFontsReverse.get(fontAbsolutePath);

        if (fontName == null)
        {
            // It might be one of the system fonts that doesn't map to a file name.
            final File f = new File(fontAbsolutePath);

            fontName = f.getName();

            if (!f.isFile())
            {
                // Work-around for a bug in BitmapFontFactoryProxy.
                fontAbsolutePath = File.separator + fontAbsolutePath;
            }
        }

        IFont loadedFont = null;

        if (fontName != null)
        {

            // Return cached font if there
            final IFont font = getCachedFont(fontName, fontSize, color, antiAliased);
            if (font != null)
            {
                return font;
            }

            try
            {

                final String suffix = getFontSuffix(fontAbsolutePath);

                // Check which factory to use for this file type
                final IEnhancedFontFactory factoryToUse = getFactoryForFileSuffix(suffix);

                if (factoryToUse != null)
                {
                    LOG.debug("Loading new font \"" + fontName + "\" with factory: " + factoryToUse.getClass().getName());
                    LOG.debug("Font file = " + fontAbsolutePath);

                    loadedFont = factoryToUse.createFont(pa, fontAbsolutePath, fontSize, color, antiAliased);

                    // Have to be sure it's not null.
                    if (loadedFont != null)
                    {
                        cacheFont(fontName, loadedFont);
                    }
                } else
                {
                    LOG.error("Couldnt find a appropriate font factory for: " + fontName + " Suffix: " + suffix);
                }
            } catch (final Exception e)
            {
                LOG.error("Error while trying to create the font: " + fontName);
                LOG.error(e.getStackTrace());
            }
        }

        return (loadedFont);
    }

  // Returns the file name extension.
    //
    private String getFontSuffix(final String fontFileName)
    {
        final int indexOfPoint = fontFileName.lastIndexOf(".");
        String suffix;
        if (indexOfPoint != -1)
        {
            suffix = fontFileName.substring(indexOfPoint, fontFileName.length());
            suffix = suffix.toLowerCase();
        } else
        {
            suffix = "";
        }
        return suffix;
    }

    /**
     * Register a new fontfactory for a file type.
     *
     * @param enhancedFontFactory
     *                            the factory
     * @param fileSuffix
     *                            the file suffix to use with that factory. ".ttf" for example.
     */
    public void registerFontFactory(String fileSuffix, final IEnhancedFontFactory enhancedFontFactory)
    {
        fileSuffix = fileSuffix.toLowerCase();
        suffixToFactory.put(fileSuffix, enhancedFontFactory);
    }

    /**
     * Unregister a fontfactory for a file type.
     *
     * @param factory
     *                the factory
     */
    public void unregisterFontFactory(final IEnhancedFontFactory factory)
    {
        final Set<String> suffixesInHashMap = suffixToFactory.keySet();
        for (final String suffix : suffixesInHashMap)
        {
            if (getFactoryForFileSuffix(suffix).equals(factory))
            {
                suffixToFactory.remove(suffix);
            }
        }
    }

    /**
     * Gets the registered factories.
     *
     * @return the registered factories
     */
    public IEnhancedFontFactory[] getRegisteredFactories()
    {
        final Collection<IEnhancedFontFactory> factoryCollection = suffixToFactory.values();
        return factoryCollection.toArray(new IEnhancedFontFactory[factoryCollection.size()]);
    }

    /**
     * Gets the factory for file suffix.
     *
     * @param suffix
     *               the suffix
     *
     * @return the factory for file suffix
     */
    public IEnhancedFontFactory getFactoryForFileSuffix(final String suffix)
    {
        return suffixToFactory.get(suffix);
    }

    /**
     * Gets the cached font.
     *
     * @param fontAbsoultePath
     *                         the font absoulte path
     * @param fontSize
     *                         the font size
     * @param fillColor
     *                         the fill color
     * @param strokeColor
     *                         the stroke color
     *
     * @return the cached font
     */
    public IFont getCachedFont(final String fontName, final int fontSize, final MTColor fillColor, final boolean antiAliased)
    {
        // Get a list of fonts registered under the given name.
        final List<IFont> fontList = fonts.get(fontName);
        if (fontList != null)
        {
            for (final IFont font : fontList)
            {
        // Don't need to get the font name from the font object, since all fonts in
                // the list have the specified fontName.
                if ((font.getOriginalFontSize() == fontSize)
                        && font.getFillColor().equals(fillColor)
                        && (font.isAntiAliased() == antiAliased))
                {
                    LOG.debug("Using cached font: " + fontName + " Fontsize: " + fontSize + " FillColor: " + fillColor);
                    return font;
                }
            }
        }
        return null;
    }

    private void cacheFont(final String fontName, final IFont font)
    {
        List<IFont> fontList = fonts.get(fontName);
        if (fontList == null)
        {
            fontList = new ArrayList<IFont>();
            fonts.put(fontName, fontList);
        }
        fontList.add(font);
        checkFontCacheSize();
    }

    // Trims down the size of the font cache, if necessary.
    private void checkFontCacheSize()
    {
        int totalSz = 0;
        Iterator<List<IFont>> it = fonts.values().iterator();
        while (it.hasNext())
        {
            totalSz += it.next().size();
        }
        if ((totalSz > CACHE_MAX_SIZE) && (totalSz > 0))
        {
            it = fonts.values().iterator();
            while (it.hasNext())
            {
                final List<IFont> fontList = it.next();
                if (fontList.size() > 0)
                {
                    fontList.remove(0);
                    totalSz--;
                    if ((totalSz == CACHE_MAX_SIZE) || (totalSz == 0))
                    {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Removes the font from the cache. <br>
     * <b>NOTE:</b> does not destroy the font! To cleanly destroy a font
     * AND remove it from the fontmanager cache call
     * <code>font.destroy()</code>.
     *
     * @param font
     *             the font
     *
     * @return true, if successful
     */
    public boolean removeFromCache(final IFont font)
    {

        final Iterator<List<IFont>> it = fonts.values().iterator();
        while (it.hasNext())
        {
            final List<IFont> list = it.next();
            final int n = list.indexOf(font);
            if (n >= 0)
            {
                list.remove(n);
                return true;
            }
        }

        return false;
    }
}
