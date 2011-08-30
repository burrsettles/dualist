package guts.pipes;


import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.CharSequenceReplace;
import cc.mallet.pipe.FeatureSequence2AugmentableFeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Noop;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.util.CharSequenceLexer;

public class VanillaPipe extends Pipe {

    private String delimiter = "\\|\\|";

    public VanillaPipe(String delimiter) {
        this.delimiter = delimiter;
    }
    public VanillaPipe() {}

    public Instance pipe (Instance carrier) {
        Alphabet dataAlphabet = this.getDataAlphabet();
        if (dataAlphabet == null) {
            dataAlphabet = new Alphabet();
            setDataAlphabet(dataAlphabet);
        }
        AugmentableFeatureVector fv = new AugmentableFeatureVector(dataAlphabet);
        String[] bits = ((String) carrier.getData()).split("\\t");		
        for (int k = 1; k < bits.length; k++) {
            String[] feature = bits[k].split(delimiter);
            if (feature.length == 2)
                fv.add(feature[0], Double.parseDouble(feature[1]));
        }
        carrier.setSource(bits[0]);
        carrier.setName(bits[0].replaceAll("\\s+", "_"));
        carrier.setData(fv);
        return carrier;
    }

}
