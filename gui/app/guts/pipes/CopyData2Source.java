package guts.pipes;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;

public class CopyData2Source extends Pipe {
    private static final long serialVersionUID = 1;

    public Instance pipe (Instance carrier) {
        carrier.setSource(carrier.getData());
        return carrier;
    }

}
