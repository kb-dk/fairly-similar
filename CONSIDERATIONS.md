# Fairly Similar

## Vision

To provide a service for image collections that combines traditional search (queries, faceting etc)
with high quality visual similarity search (e.g. Euclidian distance of vectors from ML-analysis).

Large (thousands, maybe tens of thousands?) result sets can be delivered as a zoomable collage with
all images visible at the same time. Metadata for the images in the collage should be available when
focusing on a single image.

## Challenges

 * Sorting thousands of images by similarity, using an origo image
 * Creating dynamic tile-based collages with thousands of images
 * Low-cost updates of the image structures with new or changed images

## Nice to haves

 * Separation of traditional search (i.e. Solr) from image handling, making the search part optional
 * Mosaic-generation (creating a larger image from smaller ones)
 * Color-sorting

## Machine Learning considerations

A known technique for image similiarity search is perform ML-based image analysis and compare the
vectors representing a specific layer in the network (typically the penultimate or the ultimate).
Using a generic network this layer can be quite large, e.g. 4096 floats (32 bit). This means at
least 16KB/image, which becomes a problem to hold in memory if the number of images goes into the
millions.

The `StrongestSignalFinder` in this repository caches 30 floats/image to perform a coarse
selection of images to do the distance calculations for. This indicates that it is feasible to
store the full vectors on SSD and only hold the relatively small cache in memory, at the cost of
some precision.

## Colleage generation considerations

Experience from [Zoom](http://labs.statsbiblioteket.dk/zoom/) shows that to provide fast collage
generation and tile delivery for zoom, the source images must be cached at different scales.
Assuming square images, the DeepZoom-compatible scales would be

 * 1*1 = 1 pixel
 * 2*2 = 4 pixels
 * 4*4 = 16 pixels
 * 8*8 = 64 pixels
 * 16*16 = 256 pixels
 * 32*32 = 1024 pixels
 * 64*64 = 4096 pixels
 * 128*128 = 16378 pixels

Going above this holds little gain as an image server, such at [IIPImage](https://iipimage.sourceforge.io/)
is better suited: If a browser can show 4 million pixels, the worst-case number of external image server
hits will be 4M/65K ~= 60.


Representing the pixels as classic RGB would require 3 bytes/pixel. Maybe the pixels could be represented
in an alternate way?

 * A fixed 255 color palette (+1 to represent no pixels for non-square images) would take up only 1 byte/pixel. But the old GIF-days showed that it works vsually poorly.
 * A dynamic 255 color palette (1 palette/image) would take up 1 byte/pixel + an overhead of 255*3 bytes/image. This might work well as the maximum image size is 128x128 pixels.
 * `11*11*10` bit RGB color space would take up 2 bytes/pixel. This would likely work visually well, but might be too costly to unpack?
 *  A [HSL/HSV](https://en.wikipedia.org/wiki/HSL_and_HSV)-like color space with 1 byte for intensity ("greyscale") and 1 bye for [hue](https://en.wikipedia.org/wiki/Hue). Might work well for comparison purposes. Visual qualities are unknown.

