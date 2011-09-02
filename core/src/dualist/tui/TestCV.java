package dualist.tui;

import java.text.DecimalFormat;

import cc.mallet.classify.NaiveBayes;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;

import com.google.common.collect.HashMultimap;

public class TestCV {

    private static DecimalFormat df = new DecimalFormat("0.0000");

    public static void main(String[] args) throws Exception {

        boolean clArg = args[0].startsWith("--");

        Pipe myPipe = clArg 
        ? Util.getPipe(args[0].substring(2)) : Util.getPipe("document");

        InstanceList ilist = Util.readZipData(args[clArg ? 1 : 0], myPipe, null); 
        Alphabet labelAlphabet = ilist.getTargetAlphabet();
        
        System.out.println("|data|=" + ilist.size() + 
                " |features|=" + ilist.getDataAlphabet().size() + 
                " |labels|=" + labelAlphabet.size());

        HashMultimap<String,Double> resultMap = HashMultimap.create();
        
        InstanceList.CrossValidationIterator iter = ilist.crossValidationIterator(10, 27);
        while (iter.hasNext()) {
            InstanceList[] split = iter.nextSplit();
            InstanceList trainData = split[0];
            InstanceList evalData = split[1];

            NaiveBayesTrainer nbTrainer = new NaiveBayesTrainer(myPipe);
            NaiveBayes nbModel = nbTrainer.train(trainData);

            double accuracy = nbModel.getAccuracy(evalData);
            resultMap.put("accuracy", accuracy);

            double[] f1s = new double[labelAlphabet.size()];
            for (int li = 0; li < labelAlphabet.size(); li++) {
                f1s[li] = nbModel.getF1(evalData, li);
                resultMap.put("F1:" + labelAlphabet.lookupObject(li), f1s[li]);
            }
            
            double macroF1 = Util.average(f1s);
            resultMap.put("macro-F1", macroF1);
        }
        
        System.out.println("metric  \tavg\tstdev");
        for (String key : resultMap.keySet()) {
            double avg = Util.average(resultMap.get(key));
            double stdev = Util.stdev(resultMap.get(key));
            System.out.println(key + "\t" + df.format(avg) + "\t" + df.format(stdev));
        }

    }

}
