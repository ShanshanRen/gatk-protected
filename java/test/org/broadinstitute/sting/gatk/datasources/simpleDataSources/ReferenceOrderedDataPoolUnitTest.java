package org.broadinstitute.sting.gatk.datasources.simpleDataSources;

import org.broadinstitute.sting.BaseTest;
import org.broadinstitute.sting.gatk.refdata.ReferenceOrderedData;
import org.broadinstitute.sting.gatk.refdata.features.table.TableCodec;
import org.broadinstitute.sting.gatk.refdata.features.table.TableFeature;
import org.broadinstitute.sting.gatk.refdata.tracks.RMDTrack;
import org.broadinstitute.sting.gatk.refdata.tracks.builders.RMDTrackBuilder;
import org.broadinstitute.sting.gatk.refdata.utils.LocationAwareSeekableRODIterator;
import org.broadinstitute.sting.utils.GenomeLoc;
import org.broadinstitute.sting.utils.GenomeLocParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertTrue;
import net.sf.picard.reference.IndexedFastaSequenceFile;
/**
 * User: hanna
 * Date: May 21, 2009
 * Time: 11:03:04 AM
 * BROAD INSTITUTE SOFTWARE COPYRIGHT NOTICE AND AGREEMENT
 * Software and documentation are copyright 2005 by the Broad Institute.
 * All rights are reserved.
 *
 * Users acknowledge that this software is supplied without any warranty or support.
 * The Broad Institute is not responsible for its use, misuse, or
 * functionality.
 */

/**
 * Test the contents and number of iterators in the pool.
 */

public class ReferenceOrderedDataPoolUnitTest extends BaseTest {

    private RMDTrack rod = null;

    private final GenomeLoc testSite1 = GenomeLocParser.createGenomeLoc("chrM",10);
    private final GenomeLoc testSite2 = GenomeLocParser.createGenomeLoc("chrM",20);
    private final GenomeLoc testSite3 = GenomeLocParser.createGenomeLoc("chrM",30);

    @BeforeClass
    public static void init() throws FileNotFoundException {
        File sequenceFile = new File(hg18Reference);
        GenomeLocParser.setupRefContigOrdering(new IndexedFastaSequenceFile(sequenceFile));
    }

    @Before
    public void setUp() {
        File file = new File(testDir + "TabularDataTest.dat");
        RMDTrackBuilder builder = new RMDTrackBuilder();
        rod = builder.createInstanceOfTrack(TableCodec.class, "tableTest", file);
    }

    @Test
    public void testCreateSingleIterator() {
        ResourcePool iteratorPool = new ReferenceOrderedDataPool(null,rod);
        LocationAwareSeekableRODIterator iterator = (LocationAwareSeekableRODIterator)iteratorPool.iterator( new MappedStreamSegment(testSite1) );

        Assert.assertEquals("Number of iterators in the pool is incorrect", 1, iteratorPool.numIterators());
        Assert.assertEquals("Number of available iterators in the pool is incorrect", 0, iteratorPool.numAvailableIterators());

        TableFeature datum = (TableFeature)iterator.next().get(0).getUnderlyingObject();

        assertTrue(datum.getLocation().equals(testSite1));
        assertTrue(datum.get("COL1").equals("A"));
        assertTrue(datum.get("COL2").equals("B"));
        assertTrue(datum.get("COL3").equals("C"));

        iteratorPool.release(iterator);

        Assert.assertEquals("Number of iterators in the pool is incorrect", 1, iteratorPool.numIterators());
        Assert.assertEquals("Number of available iterators in the pool is incorrect", 1, iteratorPool.numAvailableIterators());        
    }

    @Test
    public void testCreateMultipleIterators() {
        ReferenceOrderedDataPool iteratorPool = new ReferenceOrderedDataPool(null,rod);
        LocationAwareSeekableRODIterator iterator1 = iteratorPool.iterator( new MappedStreamSegment(testSite1) );

        // Create a new iterator at position 2.
        LocationAwareSeekableRODIterator iterator2 = iteratorPool.iterator( new MappedStreamSegment(testSite2) );

        Assert.assertEquals("Number of iterators in the pool is incorrect", 2, iteratorPool.numIterators());
        Assert.assertEquals("Number of available iterators in the pool is incorrect", 0, iteratorPool.numAvailableIterators());

        // Test out-of-order access: first iterator2, then iterator1.
        // Ugh...first call to a region needs to be a seek. 
        TableFeature datum = (TableFeature)iterator2.seekForward(testSite2).get(0).getUnderlyingObject();
        assertTrue(datum.getLocation().equals(testSite2));
        assertTrue(datum.get("COL1").equals("C"));
        assertTrue(datum.get("COL2").equals("D"));
        assertTrue(datum.get("COL3").equals("E"));

        datum = (TableFeature)iterator1.next().get(0).getUnderlyingObject();
        assertTrue(datum.getLocation().equals(testSite1));
        assertTrue(datum.get("COL1").equals("A"));
        assertTrue(datum.get("COL2").equals("B"));
        assertTrue(datum.get("COL3").equals("C"));

        // Advance iterator2, and make sure both iterator's contents are still correct.
        datum = (TableFeature)iterator2.next().get(0).getUnderlyingObject();
        assertTrue(datum.getLocation().equals(testSite3));
        assertTrue(datum.get("COL1").equals("F"));
        assertTrue(datum.get("COL2").equals("G"));
        assertTrue(datum.get("COL3").equals("H"));

        datum = (TableFeature)iterator1.next().get(0).getUnderlyingObject();
        assertTrue(datum.getLocation().equals(testSite2));
        assertTrue(datum.get("COL1").equals("C"));
        assertTrue(datum.get("COL2").equals("D"));
        assertTrue(datum.get("COL3").equals("E"));

        // Cleanup, and make sure the number of iterators dies appropriately.
        iteratorPool.release(iterator1);

        Assert.assertEquals("Number of iterators in the pool is incorrect", 2, iteratorPool.numIterators());
        Assert.assertEquals("Number of available iterators in the pool is incorrect", 1, iteratorPool.numAvailableIterators());

        iteratorPool.release(iterator2);

        Assert.assertEquals("Number of iterators in the pool is incorrect", 2, iteratorPool.numIterators());
        Assert.assertEquals("Number of available iterators in the pool is incorrect", 2, iteratorPool.numAvailableIterators());        
    }

    @Test
    public void testIteratorConservation() {
        ReferenceOrderedDataPool iteratorPool = new ReferenceOrderedDataPool(null,rod);
        LocationAwareSeekableRODIterator iterator = iteratorPool.iterator( new MappedStreamSegment(testSite1) );

        Assert.assertEquals("Number of iterators in the pool is incorrect", 1, iteratorPool.numIterators());
        Assert.assertEquals("Number of available iterators in the pool is incorrect", 0, iteratorPool.numAvailableIterators());

        TableFeature datum = (TableFeature)iterator.next().get(0).getUnderlyingObject();
        assertTrue(datum.getLocation().equals(testSite1));
        assertTrue(datum.get("COL1").equals("A"));
        assertTrue(datum.get("COL2").equals("B"));
        assertTrue(datum.get("COL3").equals("C"));

        iteratorPool.release(iterator);

        // Create another iterator after the current iterator.
        iterator = iteratorPool.iterator( new MappedStreamSegment(testSite3) );

        // Make sure that the previously acquired iterator was reused.
        Assert.assertEquals("Number of iterators in the pool is incorrect", 1, iteratorPool.numIterators());
        Assert.assertEquals("Number of available iterators in the pool is incorrect", 0, iteratorPool.numAvailableIterators());

        datum = (TableFeature)iterator.seekForward(testSite3).get(0).getUnderlyingObject();
        assertTrue(datum.getLocation().equals(testSite3));
        assertTrue(datum.get("COL1").equals("F"));
        assertTrue(datum.get("COL2").equals("G"));
        assertTrue(datum.get("COL3").equals("H"));

        iteratorPool.release(iterator);

        Assert.assertEquals("Number of iterators in the pool is incorrect", 1, iteratorPool.numIterators());
        Assert.assertEquals("Number of available iterators in the pool is incorrect", 1, iteratorPool.numAvailableIterators());
    }

    @Test
    public void testIteratorCreation() {
        ReferenceOrderedDataPool iteratorPool = new ReferenceOrderedDataPool(null,rod);
        LocationAwareSeekableRODIterator iterator = iteratorPool.iterator( new MappedStreamSegment(testSite3) );

        Assert.assertEquals("Number of iterators in the pool is incorrect", 1, iteratorPool.numIterators());
        Assert.assertEquals("Number of available iterators in the pool is incorrect", 0, iteratorPool.numAvailableIterators());

        TableFeature datum = (TableFeature)iterator.seekForward(testSite3).get(0).getUnderlyingObject();
        assertTrue(datum.getLocation().equals(testSite3));
        assertTrue(datum.get("COL1").equals("F"));
        assertTrue(datum.get("COL2").equals("G"));
        assertTrue(datum.get("COL3").equals("H"));

        iteratorPool.release(iterator);

        // Create another iterator after the current iterator.
        iterator = iteratorPool.iterator(new MappedStreamSegment(testSite1) );

        // Make sure that the previously acquired iterator was reused.
        Assert.assertEquals("Number of iterators in the pool is incorrect", 2, iteratorPool.numIterators());
        Assert.assertEquals("Number of available iterators in the pool is incorrect", 1, iteratorPool.numAvailableIterators());

        datum = (TableFeature)iterator.next().get(0).getUnderlyingObject();
        assertTrue(datum.getLocation().equals(testSite1));
        assertTrue(datum.get("COL1").equals("A"));
        assertTrue(datum.get("COL2").equals("B"));
        assertTrue(datum.get("COL3").equals("C"));

        iteratorPool.release(iterator);

        Assert.assertEquals("Number of iterators in the pool is incorrect", 2, iteratorPool.numIterators());
        Assert.assertEquals("Number of available iterators in the pool is incorrect", 2, iteratorPool.numAvailableIterators());
    }

}
