/*
 * Copyright (c) 2010 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.broadinstitute.sting.gatk.traversals;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMRecord;
import org.broadinstitute.sting.BaseTest;
import org.broadinstitute.sting.utils.GenomeLocParser;
import org.broadinstitute.sting.utils.sam.ArtificialSAMUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * @author aaron
 *         <p/>
 *         Class TraverseDuplicatesUnitTest
 *         <p/>
 *         test the meat of the traverse dupplicates.
 */
public class TraverseDuplicatesUnitTest extends BaseTest {

    private TraverseDuplicates obj = new TraverseDuplicates();
    private SAMFileHeader header;


    @Before
    public void doBefore() {
        header = ArtificialSAMUtils.createArtificialSamHeader(1, 1, 1000);
        GenomeLocParser.setupRefContigOrdering(header.getSequenceDictionary());
    }

    @Test
    public void testAllDupplicatesNoPairs() {
        List<SAMRecord> list = new ArrayList<SAMRecord>();
        for (int x = 0; x < 10; x++) {
            SAMRecord read = ArtificialSAMUtils.createArtificialRead(header, "SWEET_READ" + x, 0, 1, 100);
            read.setDuplicateReadFlag(true);
            list.add(read);
        }
        Set<List<SAMRecord>> myPairings = obj.uniqueReadSets(list);
        Assert.assertEquals(1, myPairings.size());
        Assert.assertEquals(10, myPairings.iterator().next().size()); // dup's
    }

    @Test
    public void testNoDupplicatesNoPairs() {
        List<SAMRecord> list = new ArrayList<SAMRecord>();
        for (int x = 0; x < 10; x++) {
            SAMRecord read = ArtificialSAMUtils.createArtificialRead(header, "SWEET_READ" + x, 0, 1, 100);
            read.setDuplicateReadFlag(false);
            list.add(read);
        }

        Set<List<SAMRecord>> myPairing = obj.uniqueReadSets(list);
        Assert.assertEquals(10, myPairing.size()); // unique
    }

    @Test
    public void testFiftyFiftyNoPairs() {
        List<SAMRecord> list = new ArrayList<SAMRecord>();
        for (int x = 0; x < 5; x++) {
            SAMRecord read = ArtificialSAMUtils.createArtificialRead(header, "SWEET_READ" + x, 0, 1, 100);
            read.setDuplicateReadFlag(true);
            list.add(read);
        }
        for (int x = 10; x < 15; x++)
            list.add(ArtificialSAMUtils.createArtificialRead(header, String.valueOf(x), 0, x, 100));

        Set<List<SAMRecord>> myPairing = obj.uniqueReadSets(list);
        Assert.assertEquals(6, myPairing.size());  // unique
    }

    @Test
    public void testAllDupplicatesAllPairs() {
        List<SAMRecord> list = new ArrayList<SAMRecord>();
        for (int x = 0; x < 10; x++) {
            SAMRecord read = ArtificialSAMUtils.createArtificialRead(header, "SWEET_READ"+ x, 0, 1, 100);
            read.setDuplicateReadFlag(true);
            read.setMateAlignmentStart(100);
            read.setMateReferenceIndex(0);
            read.setReadPairedFlag(true);
            list.add(read);
        }

        Set<List<SAMRecord>> myPairing = obj.uniqueReadSets(list);
        Assert.assertEquals(1, myPairing.size());  // unique
    }

    @Test
    public void testNoDupplicatesAllPairs() {
        List<SAMRecord> list = new ArrayList<SAMRecord>();
        for (int x = 0; x < 10; x++) {
            SAMRecord read = ArtificialSAMUtils.createArtificialRead(header, "SWEET_READ"+ x, 0, 1, 100);
            if (x == 0) read.setDuplicateReadFlag(true); // one is a dup but (next line)
            read.setMateAlignmentStart(100); // they all have a shared start and mate start so they're dup's
            read.setMateReferenceIndex(0);
            read.setReadPairedFlag(true);
            list.add(read);
        }

        Set<List<SAMRecord>> myPairing = obj.uniqueReadSets(list);
        Assert.assertEquals(1, myPairing.size());  // unique
    }

    @Test
    public void testAllDupplicatesAllPairsDifferentPairedEnd() {
        List<SAMRecord> list = new ArrayList<SAMRecord>();
        for (int x = 0; x < 10; x++) {
            SAMRecord read = ArtificialSAMUtils.createArtificialRead(header, "SWEET_READ" + x, 0, 1, 100);
            if (x == 0) read.setDuplicateReadFlag(true); // one is a dup
            read.setMateAlignmentStart(100 + x);
            read.setMateReferenceIndex(0);
            read.setReadPairedFlag(true);
            list.add(read);
        }

        Set<List<SAMRecord>> myPairing = obj.uniqueReadSets(list);
        Assert.assertEquals(10, myPairing.size());  // unique
    }
}
