package guts.pipes;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.TokenSequence;

public class TokenSequenceBiGrammer extends Pipe {

    public Instance pipe (Instance carrier) {
        TokenSequence ts = (TokenSequence) carrier.getData();
        int size = ts.size();
        for (int i = 1; i < size; i++) {
            ts.add (ts.get(i-1).getText() + "_" + ts.get(i).getText());
        }
        carrier.setData(ts);
        return carrier;
    }

}
