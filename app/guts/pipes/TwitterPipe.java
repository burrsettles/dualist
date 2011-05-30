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
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.types.Instance;
import cc.mallet.util.CharSequenceLexer;

public class TwitterPipe extends Pipe {

    private boolean anonmymize = true;
    private boolean emoticons = true;

    private Pipe myPipe = new SerialPipes(new Pipe[] {
            new CopyData2Source(),
            new CharSequenceLowercase(),
            anonmymize 
            ? new CharSequenceReplace(Pattern.compile("http\\:\\/\\/.*\\b"), "HTTPLINK") 
            : new Noop(),
            anonmymize 
            ? new CharSequenceReplace(Pattern.compile("\\@\\w+"), "@USERLINK") 
            : new Noop(),
            new CharSequenceReplace(Pattern.compile("\\'"), ""),
            new CharSequenceReplace(Pattern.compile("\\!\\!+"), "!!"),
            new CharSequenceReplace(Pattern.compile("\\?\\?+"), "??"),
            new CharSequenceReplace(Pattern.compile("\\bno\\s+"), "no_"),
            new CharSequenceReplace(Pattern.compile("\\bnot\\s+"), "not_"),
            new CharSequenceReplace(Pattern.compile("\\bdoesnt\\s+"), "doesnt_"),
            new CharSequenceReplace(Pattern.compile("\\bdont\\s+"), "dont_"),
            new CharSequenceReplace(Pattern.compile("\\baint\\s+"), "aint_"),
            emoticons
            ? new CharSequence2TokenSequence("([\\@\\#]?\\w[\\w'_]+)|([:;=x][-o^]?[)(/\\\\dp])|([/\\\\)(dp][-o^]?[:;=x])|([!?]+)")
            : new CharSequence2TokenSequence("[\\@\\#]?\\w[\\w'_]+"),
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
