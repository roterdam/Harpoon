// Save.java, created by wbeebee
// Copyright (C) 2003 Wes Beebee <wbeebee@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package imagerec.graph;

import imagerec.util.ImageDataManip;

/**
 * {@link Save} will save images that are sent to it.  
 * Currently only <code>.ppm.gz</code> format is supported.
 *
 * @see Load
 *
 * @author Wes Beebee <<a href="mailto:wbeebee@mit.edu">wbeebee@mit.edu</a>>
 */

public class Save extends Node {
    private String filePrefix;
    private boolean inorder;
    private int count=0;

    /** Construct a new {@link Save} node that will save a series of images.
     *  The filenames are <code>filePrefix.#</code> where <code>#</code> represents
     *  the <code>id</code> number of the image.
     *
     *  @param filePrefix The beginning of the file name to save.
     */
    public Save(String filePrefix) {
	this(filePrefix, true);
    }

    /*
     * {@link Save} can use the original <code>id</code> of the image so that if
     * images are received out of order, the movie can be
     * reconstructed. Images that are dropped can be examined later.
     * For this behavior (the default), specify <code>inorder</code>
     * to be true.
     *
     * Alternatively, {@link Save} could number the images
     * sequentially as they arrive, by specifying <code>inorder</code>
     * to be false.  
     *
     * @param filePrefix The beginning of the fileName to save (see above).
     * @param inorder Whether to use the <code>id</code> or count images coming in.
     */
    public Save(String filePrefix, boolean inorder) {
	super();
	this.filePrefix = filePrefix;
	this.inorder = inorder;
    }

    /** Save the images as they come in.
     *
     *  @param id The image to save. 
     */
    public synchronized void process(ImageData id) {
	System.out.println("Saving image "+filePrefix+"."+(inorder?id.id:count));
	ImageDataManip.writePPM(id, filePrefix+"."+(inorder?id.id:(count++)));
    }
}