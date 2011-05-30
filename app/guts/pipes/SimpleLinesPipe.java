package guts.pipes;


import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.CharSequenceReplace;
import cc.mallet.pipe.FeatureSequence2AugmentableFeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Noop;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.PrintInputAndTarget;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequence2FeatureSequenceWithBigrams;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.types.Instance;
import cc.mallet.util.CharSequenceLexer;

public class SimpleLinesPipe extends Pipe {

    private Pipe myPipe = new SerialPipes(new Pipe[] {
            new CharSequenceReplace(Pattern.compile("\\<.*?>"), ""),
            new CopyData2Source(),
//            new CharSequenceReplace(Pattern.compile("&(.*?);"), ""),
//            new CharSequenceReplace(Pattern.compile("[0-9]+"), "00"),
            new CharSequenceLowercase(),
            new CharSequence2TokenSequence(CharSequenceLexer.LEX_WORD_CLASSES),
            new TokenSequenceBiGrammer(),
            new TokenSequenceRemoveStopwords(),
            new TokenSequence2FeatureSequence(),
            new FeatureSequence2AugmentableFeatureVector(),
            new Labelize(),
//            new PrintInputAndTarget(),
    });

    public Instance pipe (Instance carrier) {
        carrier.setSource(carrier.getData());
        return myPipe.pipe(carrier);
    }

    public java.util.Iterator<Instance> newIteratorFrom(java.util.Iterator<Instance> carrier) {
        return myPipe.newIteratorFrom(carrier);
    }

}
