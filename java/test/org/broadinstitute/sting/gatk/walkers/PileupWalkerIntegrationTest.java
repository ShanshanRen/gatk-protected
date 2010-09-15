package org.broadinstitute.sting.gatk.walkers;

import org.broadinstitute.sting.WalkerTest;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: chartl
 * Date: Dec 1, 2009
 * Time: 9:03:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class PileupWalkerIntegrationTest extends WalkerTest {

    @Test
    public void testGnarleyFHSPileup() {
        String gatk_args = "-T Pileup -I " + validationDataLocation + "FHS_Pileup_Test.bam "
                 + "-R " + hg18Reference
                 +  " -L chr15:46,347,148 -o %s";
        String expected_md5 = "872e89df166b90e06dd2737535c5d8b3";
        WalkerTestSpec spec = new WalkerTestSpec(gatk_args, 1, Arrays.asList(expected_md5));
        executeTest("Testing the standard (no-indel) pileup on three merged FHS pools with 27 deletions in 969 bases", spec);
    }

    @Test
    public void testExtendedEventPileup() {
        String gatk_args = "-T Pileup -I " + validationDataLocation + "OV-0930.normal.chunk.bam "
                 + "-R " + hg18Reference
                + " -show_indels -o %s";
        String expected_md5="06eedc2e7927650961d99d703f4301a4";
        WalkerTestSpec spec = new WalkerTestSpec(gatk_args,1,Arrays.asList(expected_md5));
        executeTest("Testing the extended pileup with indel records included on a small chunk of Ovarian dataset with 20 indels (1 D, 19 I)", spec);

    }
}
