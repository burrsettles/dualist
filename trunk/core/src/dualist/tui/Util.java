package dualist.tui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.NaiveBayes;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import dualist.pipes.DocumentPipe;
import dualist.pipes.EntityPipe;
import dualist.pipes.SimpleLinesPipe;
import dualist.pipes.TwitterPipe;

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

    public static double average(Collection<Double> values) {
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

    public static double stdev(Collection<Double> values) {
        double ret = 0;
        double mean = average(values);
        for (Double val : values)
            ret += Math.pow(val - mean, 2);
        return Math.sqrt( ret / (values.size()-1) );
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
    
    public static Pipe getPipe(String pipeName) {
        Pipe ret = new DocumentPipe();
        if (pipeName.equals("simple"))
            ret = new SimpleLinesPipe();
        else if (pipeName.equals("twitter"))
            ret = new TwitterPipe();
        else if (pipeName.equals("entity"))
            ret = new EntityPipe();
        return ret;
    }

    public static InstanceList probabilisticData(NaiveBayes nbModel,
            InstanceList labeledSet, InstanceList unlabeledSet) {
        return probabilisticData(nbModel, labeledSet, unlabeledSet, false);
    }

    public static InstanceList probabilisticData(NaiveBayes nbModel,
            InstanceList labeledSet, InstanceList unlabeledSet, boolean useMax) {

        InstanceList ret = labeledSet.shallowClone();
        for (Instance inst : unlabeledSet) {

            Instance inst2 = inst.shallowCopy();
            inst2.unLock();

            if (useMax)
                inst2.setLabeling(nbModel.classify(inst).getLabelVector().getBestLabel());
            else
                inst2.setLabeling(nbModel.classify(inst).getLabeling());

            inst2.lock();
            ret.add(inst2, 0.1);
        }
        return ret;
    }

    public static InstanceList readZipData(String dataset, Pipe myPipe, LabelAlphabet labelAlphabet) {
        return readZipData(new File(dataset), myPipe, labelAlphabet);
    }
    
    public static InstanceList readZipData(File dataset, Pipe myPipe, LabelAlphabet labelAlphabet) {

        boolean labeledData = (labelAlphabet == null);
        if (!labeledData && myPipe.getTargetAlphabet() == null)
            myPipe.setTargetAlphabet(labelAlphabet);

        InstanceList ilist = new InstanceList (myPipe);
        try {
            ZipFile zf = new ZipFile(dataset);
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
                ZipEntry ze = e.nextElement();
                String name = ze.getName();
                Object label = labeledData ? name.replaceAll("\\/.*$", "") : labelAlphabet.lookupLabel(0);
                addLabeledInstances(ilist, zf.getInputStream(ze), name, label);
            }
            zf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (myPipe.getDataAlphabet() == null)
            myPipe.setDataAlphabet(ilist.getDataAlphabet());
        if (labeledData && myPipe.getTargetAlphabet() == null)
            myPipe.setTargetAlphabet(ilist.getTargetAlphabet());

        return ilist;
    }

    public static InstanceList readTextData(String dataset, Pipe myPipe, LabelAlphabet labelAlphabet) {
        return readTextData(new File(dataset), myPipe, labelAlphabet);
    }
    
    public static InstanceList readTextData(File dataset, Pipe myPipe, LabelAlphabet labelAlphabet) {
        InstanceList ilist = new InstanceList (myPipe);

        try {
            Object label = labelAlphabet.lookupLabel(0);
            String name = dataset.getName();
            addLabeledInstances(ilist, new FileInputStream(dataset), name, label);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return ilist;
    }    

    public static void addLabeledInstances(InstanceList ilist, InputStream input, String name, Object label) {
        try {
            String text = IOUtils.toString(input, "UTF-8").trim();
            if (!text.isEmpty()) {
                // if we're doing whole documents, then load 'er up...
                if (ilist.getPipe() instanceof DocumentPipe)
                    ilist.addThruPipe(new Instance(text, label, name, null));
                // otherwise, assume each line represents and instance...
                else {
                    String[] inputs = text.split("\\n+");
                    for (int i = 0; i< inputs.length; i++)
                        ilist.addThruPipe(new Instance(inputs[i], label, name+":"+i, null));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
