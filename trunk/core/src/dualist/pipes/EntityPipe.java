package dualist.pipes;

import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Instance;

public class EntityPipe extends Pipe {

    private Pipe myPipe = new SerialPipes(new Pipe[] {
            new VanillaPipe("\\|\\|"),
            new OrthoPipe(),
            new Labelize(),
//            new PrintInputAndTarget(),
    });

    public Instance pipe (Instance carrier) {
        return myPipe.pipe(carrier);
    }

    public java.util.Iterator<Instance> newIteratorFrom(java.util.Iterator<Instance> carrier) {
        return myPipe.newIteratorFrom(carrier);
    }

}
