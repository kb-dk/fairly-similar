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
 
## Storage
 
Using the [Lucene](https://lucene.apache.org/) index format as inspiration, the storage of image data
is made up of collections, which are made up of segments, which contains files representing specific
data for the images.
 
### Collection

A folder containing

 * `collection.dat` holds collection title, colorspace (RGB|greyscale), maximum side for cached images, position vector layout etc.
 * `segment*`
 * `segments.dat` holds a list of active segments.

### Segment

Multiple files sharing a prefix made up of `_` and 4 lower-case letters (a-z).

 * `<prefix>_bitmap.dat` image pixel cache (binary, fixed bytes/image)
 * `<prefix>_meta.dat` metadata for the images (String based, 1 line/image)
 * `<prefix>_position_vector_X.dat` position vectors from ML, where `X` is a counter starting from 0, usable if there are more than one vector/image (binary, fixed bytes/image)
 
The number if images is determined by the number of lines in `<prefix>_meta.dat`. This means that merging of segments can be done by simple concatenation.

### `<prefix>_bitmap.dat`

Binary format, fixed size/image. Data depends on greyscale or RGB colorspace: 1 byte/pixel for greyscale, 3 bytes/pixel for RGB. Multiple pixels are represented left->right, top->bottom:

```
[1x1][2x2][4x4]...[128x128]
```


### `<prefix>_meta.dat`

1 line/image, represented as JSON:
```
{ image: image-url, width: long, height: long, title: string }\n
```

### `<prefix>_position_vector_X.dat`

Binary format, fixed size/image. Depends on setup, e.g.
```
4096*[float as 32 bits]
```

## Fairly Writer

There is at most a single writer instance.

Collections are created by creating a folder with the same name as the collection. An empty `segments.dat` is created in the folder and a `collection.dat` is created with the setup for the collection. The properties for colorspace, maximum side of cached images and position vector layout are immutable, as they define the segment layouts.

The writer holds an open segment and appends image data to it. When a suitable (this should be an option) segment size is reached, the segment is flushed and the file `segments.dat` is updated to contain the new segment. Old segments can be merged in the background by the writer as they are only activated when `segments.dat` is updated.

When an image is added, the pixels for the scaled down versions are calculated and an ML-analysis is performed. This is done by threaded workers. The writing of the data from the workers needs to be done synchronized, to ensure same order across segment files.

## Fairly Reader

The reader opens `collection.dat` and `segments.dat`. After that, the segments defined in `segments.dat` are scanned and a lookup structure for `image-url` to [imageID (sequential count from 0), width, height] is created. For each `<prefix>_bitmap.dat` and `<prefix>_position_vector_X.dat` a random access file reader is opened.

Separate In-memory image caches for `[1x1]`, `[2x2]`, `[4x4]` image representations up to a specified limit are created and loaded from the `<prefix>_bitmap.dat` files.

In-memory position_vector caches are created from `<prefix>_position_vector_X.dat`. What they hold depends on the algorithm for finding nearest images. Using the `StrongestSignalFinder` algorithm this would be 30 floats/image.

There can be multiple readers. This is used when the collection is updated and `segments.dat` has changed.

## API

The API will be specified in detail using the OpenAPI 1.3 standard. This is just a broad overview.

Core concepts are `group`s, `set`s and `collage`s.

`set` is a named list of `imageURL`s:
```
{ setA: ["http://example.com/1.jpg", "http://example.com/foo.jpg"] }
```

`group` is a named list of named `set`s:
```
groupA: { 
  setA: ["http://example.com/1A.jpg", "http://example.com/fooA.jpg"], 
  setB: ["http://example.com/1B.jpg", "http://example.com/fooB.jpg"]
}
```

`collage` is a named grid of `imageURL`s with setIDs:
```
collageA : {
  gridWidth: 4,
  gridHeight: 3,
  aspectRatio: 1.33,
  sort: "rainbow",
  sortOrder: "topLeftBottomRight",
  sources: [
    { imageURL: "http://example.com/1B.jpg", setID: "setB" },
    { imageURL: "http://example.com/fooA.jpg", setID: "setA" },
    { imageURL: "http://example.com/1A.jpg", setID: "setA" },
    { imageURL: "http://example.com/fooB.jpg", setID: "setB" },
    ...
  ]
}
```


 * `getSimilar(imageURL, limit): <to be defined>` return up to `limit` `imageURL`s, sorted by visual similarity distance from the input image. Note that this might involve an ML-analysis of the input image, if it is not already present in the collection.
 * `defineSet(setID, imageURL*): void` create a persistent set of images with the given `setID` for future use.
 * `defineroup(groupID, set*|setID): void` create a persistent group of sets with the given `groupID` for future use.
 * `createCollage(collageID, set|setID|group|groupID, gridWidth, gridHeight, aspectRatio, sort, sortOrder): collage` create an internal representation of a collage of images. The concrete grid layout is determined by `gridWidth`, `gridHeight`, `aspectRatio` from what is defined (values <= 0 means undefined). `sort` can be `[imageID, similarity, rainbow, brightness]`. `sortOrder` can be `[topDown, bottomUp, leftRight, rightLeft, topLeftBottomRight, topRightBottomLeft, bottomLeftTopRight, bottomRightTopLeft, insideOut, outsideIn]` (some `sortOrder`s  are invalid with `sort=similarity`).
 * `collageTiles/collageID/*` DeepZoom-compatible tile source for a previously defined collage.
