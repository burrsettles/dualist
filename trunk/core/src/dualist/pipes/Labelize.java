package dualist.pipes;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;

public class Labelize extends Pipe {
    private final static long serialVersionUID = -461155063551297878L; //-8390758647439705273L; 

    public Labelize (Alphabet dataAlphabet, LabelAlphabet labelAlphabet) {
        super(dataAlphabet, labelAlphabet);
    }

    public Labelize ()
    {
        this(null, new LabelAlphabet());
    }

    public Labelize (LabelAlphabet labelAlphabet)
    {
        this(null, labelAlphabet);
    }

    public Instance pipe (Instance carrier)
    {
        if (carrier.getTarget() != null) {
            if (carrier.getTarget() instanceof Label)
                return carrier;
            LabelAlphabet ldict = (LabelAlphabet) getTargetAlphabet();
            carrier.setTarget(ldict.lookupLabel (carrier.getTarget()));
        }
        return carrier;
    }

}
