# fairly-similar
Library for the heavy lifting part of finding similar images based on pixels.

## Overall problem

Given a specific image in a collection, find X similar images in the same collection, 
where similar uses visual similarity such as color, shape or elements ("bike", "snow", 
"balloon") on the image.

## Nearest Neighbor

When using a neural network for image classification and extracting feature vectors for 
determining image similarity, there is a need for finding the 
[Nearest Neighbour](https://en.wikipedia.org/wiki/Nearest_neighbor_search) in a 2048 
dimensional vector space.

The current purpose of the `fairly-similar`-repository is to explore ways of locating 
such neighbours with a mix of pre-processing, accuracy and computing power that works 
the the Royal Danish Library and its digital image collection.

Currently there are about 300K images in the collection, but the solution should scale 
into the lower millions. Let's say 5 million. The collection is semi-dynamic, meaning 
that images are added in batches. If a solution uses pre-processing, this pre-processing 
should be fast enough to be done on a nightly basis without excessive hardware requirements.

As we are currently experimenting, we are measuring CPU-core-requirements: If a solution 
uses 4 threads, its speed (number of images checked/second) will be divided by 4 when 
comparing to other solutions. Do not that it _is_ a positive feature it it can take 
advantage of multiple cores when running in production.

## Test data

A block with 270707 image vectors (2048 dimensions, represented with 4 byte floats) can 
be downloaded as 
[binary (2.1 GB)](https://labs-devel.statsbiblioteket.dk/pixplot/kb_all/pixplot_vectors_270707.bin) or 
[text (2.0 GB)](https://labs-devel.statsbiblioteket.dk/pixplot/kb_all/pixplot_vectors_270707.txt.gz).

The numbers are all positive and there is a tendency for vectors to have a fairly small 
(10-100) number of dimensions that are significant, i.e. have a relatively large number.

## How to run

1. Download the [binary](https://labs-devel.statsbiblioteket.dk/pixplot/kb_all/pixplot_vectors_270707.bin)
sample data and put then in `src/test/resources/`
2. Make a package (`mav package`)
3. Run `java -cp target/similar-lib-0.1-SNAPSHOT.jar:src/test/resources/ dk.kb.similar.NearestNeighbour`

## How to fight

1. Create a new class in `dk.kb.similar.finder` that implements `NearestFinder`
2. Add an instance of your class to `NearestNeighbour.createFinders`
3. Run the `NearestNeighbour` as described above and compare the results

## Current status

On an older 4 core i5 desktop:
`java -cp target/similar-lib-0.1-SNAPSHOT.jar:src/test/resources/ dk.kb.similar.NearestNeighbour 50,000 20`
```
...
EarlyNearestFinder: 1571ms (636,443 points/sec, totalDist=3444.3)
StrongestSignalsFinder: 194ms (5,128,772 points/sec, totalDist=3459.1)
DiceNearestFinder: 170ms (5,877,439 points/sec, totalDist=4579.0)
RandomFinder: 3ms (306,026,768 points/sec, totalDist=7974.7)
```

`EarlyNearestFinder` guarantees "correct" results, meaning the points that are nearest to each other, measured by Eucliduan distance.
