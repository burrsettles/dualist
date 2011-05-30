package guts;

import guts.pipes.CopyData2Source;
import guts.pipes.DocumentPipe;
import guts.pipes.EntityPipe;
import guts.pipes.SimpleLinesPipe;
import guts.pipes.TwitterPipe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

import play.Logger;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.NaiveBayes;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.CharSequenceReplace;
import cc.mallet.pipe.FeatureSequence2AugmentableFeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.PrintInputAndTarget;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelVector;
import cc.mallet.types.Labeling;
import cc.mallet.util.CharSequenceLexer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class Util {

    public static Multimap<String, String> readOracleFromFile(String oracleFile) {
        Multimap<String,String> ret = HashMultimap.create();
        try{
            BufferedReader br = new BufferedReader(new FileReader(new File(oracleFile)));
            String line;
            while ((line = br.readLine()) != null)   {
                if (!line.startsWith("#")) {
                    String[] bits = line.split("\\t");
                    ret.put(bits[1], bits[0]);					
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }

    public static double average(List<Double> values) {
        double ret = 0;
        for (Double val : values)
            ret += val;
        return ret / values.size();
    }

    public static double average(double[] values) {
        double ret = 0;
        for (int i = 0; i < values.length; i++)
            ret += values[i];
        return ret / values.length;
    }

    public static double stdev(double[] values) {
        double ret = 0;
        double mean = average(values);
        for (int i = 0; i < values.length; i++)
            ret += Math.pow(values[i] - mean, 2);
        return Math.sqrt( ret / (values.length-1) );
    }

    public static InstanceList stripLabels(InstanceList ilist) {
        InstanceList ret = new InstanceList (ilist.getPipe());
        for (Instance instance : ilist) {
            Instance inst = instance.shallowCopy();
            ret.add(inst);
            inst.unLock();
            inst.setLabeling(null);
            inst.lock();
        }
        return ret;
    }

    public static double macroF1(Classifier c, InstanceList ilist) {
        double ret = 0;
        int numLabels = ilist.getTargetAlphabet().size();
        for (int li = 0; li < numLabels; li++) {
            double f1 = c.getF1(ilist, li);
            if (!Double.isNaN(f1))
                ret += c.getF1(ilist, li) / numLabels;
        }
        return ret;
    }

    public static double entropy(double[] probs) {
        double ret = 0;
        for (int i=0; i<probs.length; i++)
            ret -= (probs[i] > 1e-7)
            ? probs[i] * Math.log(probs[i]) 
                    : 0;
            return ret;
    }

    public static InstanceList probabilisticData(NaiveBayes nbModel,
            InstanceList labeledSet, InstanceList unlabeledSet) {

        if (nbModel == null)
            Logger.warn("MODEL IS NULL!!");

        InstanceList ret = labeledSet.shallowClone();
        for (Instance inst : unlabeledSet) {

            if (inst == null)
                Logger.warn("INSTANCE IS NULL!");

            Instance inst2 = inst.shallowCopy();
            inst2.unLock();

            nbModel.classify(inst);

            inst2.setLabeling(
                    nbModel.classify(
                            inst).getLabeling());
            inst2.lock();
            ret.add(inst2, 0.5);
        }
        return ret;
    }

    public static double margin(LabelVector lv) {
        return Math.abs(lv.getValueAtRank(0) - lv.getValueAtRank(1));
    }

    public static double leastConfident(LabelVector lv) {
        return 1.0 - lv.getValueAtRank(0);
    }

    public static InstanceList readData(File dataset, String dataType, LabelAlphabet labelAlphabet) {

        // set the input pipe accordingly
        Pipe myPipe = new DocumentPipe();
        if (dataType.equals("simple"))
            myPipe = new SimpleLinesPipe();
        else if (dataType.equals("twitter"))
            myPipe = new TwitterPipe();
        else if (dataType.equals("entity"))
            myPipe = new EntityPipe();

        boolean labeledData = (labelAlphabet == null);
        if (!labeledData)
            myPipe.setTargetAlphabet(labelAlphabet);

        InstanceList ilist = new InstanceList (myPipe);
        try {
            ZipFile zf = new ZipFile(dataset);
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
                ZipEntry ze = e.nextElement();
                String name = ze.getName();
                // this is a hack to that the label isn't null
                Object label = labeledData ? name.replaceAll("\\/.*$", "") : labelAlphabet.lookupLabel(0);
                String text = IOUtils.toString(zf.getInputStream(ze), "UTF-8").trim();
                if (text.isEmpty())
                    continue;
                // if we're doing whole documents, then load 'er up...
                if (dataType.equals("document"))
                    ilist.addThruPipe(new Instance(text, label, name, null));
                // otherwise, assume each line represents and instance...
                else {
                    String[] inputs = text.split("\\n+");
                    for (int i = 0; i< inputs.length; i++)
                        ilist.addThruPipe(new Instance(inputs[i], label, name+":"+i, null));
                }
            }
            zf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        myPipe.setDataAlphabet(ilist.getDataAlphabet());
        if (labeledData)
            myPipe.setTargetAlphabet(ilist.getTargetAlphabet());

        return ilist;
    }


}
