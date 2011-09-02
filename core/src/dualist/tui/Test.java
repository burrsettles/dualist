package dualist.tui;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

import cc.mallet.classify.NaiveBayes;
import cc.mallet.types.InstanceList;

public class Test {

    public static void main(String[] args) throws Exception {

        // require at least two arguments to proceed
        if (args.length != 2)
            System.exit(1);

        System.err.println("~ Loading trained model...");
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[0]));
        NaiveBayes nbModel = (NaiveBayes) ois.readObject();
        ois.close();

        InstanceList ilist = Util.readZipData(args[1], nbModel.getInstancePipe(), null); 

        System.out.println("|data|=" + ilist.size() + 
                " |features|=" + ilist.getDataAlphabet().size() + 
                " |labels|=" + ilist.getTargetAlphabet().size());

        System.out.println(nbModel.getAccuracy(ilist));
        
    }

}
