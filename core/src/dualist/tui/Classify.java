package dualist.tui;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.text.DecimalFormat;

import cc.mallet.classify.NaiveBayes;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;

public class Classify {
    
    private static DecimalFormat df = new DecimalFormat("0.0000");

    public static void main(String[] args) throws Exception {
        
        // require at least two arguments to proceed
        if (args.length < 2)
            System.exit(1);

        System.err.println("~ Loading trained model...");
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[0]));
        NaiveBayes nbModel = (NaiveBayes) ois.readObject();
        ois.close();
        
        Pipe myPipe = nbModel.getInstancePipe();

        System.err.println("~ Classifying input instances...");
        InstanceList ilist = new InstanceList (myPipe);
        for (int ii = 1; ii < args.length; ii++) {
            if (args[ii].toLowerCase().endsWith(".zip"))
                ilist.addAll(Util.readZipData(args[ii], myPipe, nbModel.getLabelAlphabet()));
            else
                ilist.addAll(Util.readTextData(args[ii], myPipe, nbModel.getLabelAlphabet()));
        }

        System.out.println("|data|=" + ilist.size() + 
                " |features|=" + ilist.getDataAlphabet().size() + 
                " |labels|=" + ilist.getTargetAlphabet().size());
        
        for (Instance instance : ilist) {
            Labeling l = nbModel.classify(instance).getLabeling();
            System.out.print(instance.getName());
            for (int rank = 0; rank < l.numLocations(); rank++)
                System.out.print("\t" + l.getLabelAtRank(rank) + "\t" + df.format(l.getValueAtRank(rank)));
            String summary = new String(instance.getSource().toString().trim().replaceAll("\\s+", " ").getBytes(), "UTF8");
            if (summary.length() > 150)
                summary = summary.substring(0, 150) + "...";
            System.out.print("\t" + summary);
            System.out.println();
        }


    }

}
