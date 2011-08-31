package dualist;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.text.DecimalFormat;

import org.apache.commons.io.IOUtils;

import cc.mallet.classify.NaiveBayes;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import dualist.pipes.DocumentPipe;

public class TUI {
    
    private static DecimalFormat df = new DecimalFormat("0.00000");

    public static void main(String[] args) throws Exception {
        
        // require at least two arguments to proceed
        if (args.length < 2)
            System.exit(1);

        System.err.println("~ Loading trained model...");
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[0]));
        NaiveBayes nbModel = (NaiveBayes) ois.readObject();

        Pipe myPipe = nbModel.getInstancePipe();
        boolean docPipe = (myPipe instanceof DocumentPipe);

        InstanceList ilist = new InstanceList (myPipe);

        System.err.println("~ Classifying input instances...");
        for (int ii = 1; ii < args.length; ii++) {
            Object fakeLabel = nbModel.getLabelAlphabet().lookupLabel(0);
            String name = (new File(args[ii])).getName();
            String text = IOUtils.toString(new FileInputStream(args[ii]), "UTF-8").trim();
            if (text.isEmpty())
                continue;
            // if we're doing whole documents, then load 'er up...
            if (docPipe)
                ilist.addThruPipe(new Instance(text, fakeLabel, name, null));
            // otherwise, assume each line represents and instance...
            else {
                String[] inputs = text.split("\\n+");
                for (int i = 0; i< inputs.length; i++)
                    ilist.addThruPipe(new Instance(inputs[i], fakeLabel, name+":"+i, null));
            }        
        }
        
        for (Instance instance : ilist) {
            Labeling l = nbModel.classify(instance).getLabeling();
            System.out.print(instance.getName());
            for (int rank = 0; rank < l.numLocations(); rank++)
                System.out.print("\t" + l.getLabelAtRank(rank) + "\t" + df.format(l.getValueAtRank(rank)));
            System.out.println();
        }


    }

}
