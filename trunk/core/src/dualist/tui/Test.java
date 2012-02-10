package dualist.tui;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import cc.mallet.classify.NaiveBayes;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;

public class Test {

    private static DecimalFormat df = new DecimalFormat("0.0000");

    public static void main(String[] args) throws Exception {

        // require at least two arguments to proceed
        if (args.length != 2)
            System.exit(1);

        // load the trained model
        System.err.println("~ Loading trained model...");
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[0]));
        NaiveBayes nbModel = (NaiveBayes) ois.readObject();
        ois.close();

        // read in the data set
        Pipe myPipe = nbModel.getInstancePipe();
        InstanceList ilist = Util.readZipData(args[1], myPipe, null); 

        // writes out some data stats
        System.out.println("|data|=" + ilist.size() + 
                " |features|=" + ilist.getDataAlphabet().size() + 
                " |labels|=" + ilist.getTargetAlphabet().size());

        // compute 10-fold cross-validation accuracy
        InstanceList.CrossValidationIterator iter = ilist.crossValidationIterator(10);
        Set<Double> accuracies = new HashSet<Double>();
        while (iter.hasNext()) {
            InstanceList[] split = iter.nextSplit();
            InstanceList trainData = split[0];
            InstanceList evalData = split[1];

            NaiveBayesTrainer nbTrainer = new NaiveBayesTrainer();
            NaiveBayes cvModel = nbTrainer.train(trainData);

            accuracies.add(cvModel.getAccuracy(evalData));
        }
        
        // report results
        System.out.println();
        System.out.println("model accuracy:\t" + 
                df.format(nbModel.getAccuracy(ilist)));
        System.out.println("10-fold XV acc:\t" + 
                df.format(Util.average(accuracies)) + " +/- " + 
                df.format(Util.stdev(accuracies)));
        System.out.println();
    }

}
