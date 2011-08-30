package guts;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import guts.Util;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.NaiveBayes;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureCounts;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.InfoGain;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelVector;
import cc.mallet.types.Labeling;
import cc.mallet.types.Multinomial;
import cc.mallet.types.PerLabelInfoGain;
import cc.mallet.types.RankedFeatureVector;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class Queries {

    public static Multimap<Integer, String> randomFeaturesPerLabel(
            HashMultimap<Integer, String> labeledFeatures,
            InstanceList ilist, int num) {

        Random rand = new Random();
        Alphabet dataAlphabet = ilist.getAlphabet();
        Alphabet targetAlphabet = ilist.getTargetAlphabet();

        Multimap<Integer, String> ret = HashMultimap.create();
        for (int li = 0; li < targetAlphabet.size(); li++) {
            for (int k = 0; k < num; k++)
                ret.put(li, dataAlphabet.lookupObject(rand.nextInt(dataAlphabet.size())).toString());
        }

        return ret;
    }

    public static Multimap<Integer, String> commonFeaturesPerLabel(
            HashMultimap<Integer, String> labeledFeatures,
            InstanceList ilist, int num) {

        Alphabet targetAlphabet = ilist.getTargetAlphabet();
        FeatureCounts fc = new FeatureCounts(ilist); 

        Multimap<Integer, String> ret = HashMultimap.create();
        for (int li = 0; li < targetAlphabet.size(); li++) {
            for (int rank = 0; rank < num; rank++)
                ret.put(li, fc.getObjectAtRank(rank).toString());
        }

        return ret;
    }


    public static Multimap<Integer, String> queryFeaturesPerLabelMI(Classifier c, 
            HashMultimap<Integer, String> labeledFeatures, 
            InstanceList ilist, int numQueries) {

        Alphabet dataAlphabet = ilist.getAlphabet();
        Alphabet targetAlphabet = ilist.getTargetAlphabet();

        double[] lCounts = new double[targetAlphabet.size()];
        double[][] fCounts = new double[targetAlphabet.size()][dataAlphabet.size()];
        for (Instance instance : ilist) {
            FeatureVector fv = (FeatureVector) instance.getData();
            Labeling l = (Labeling) instance.getTarget();
            l.addTo(lCounts);
            for (int li = 0; li < l.numLocations(); li++) {
                double val = l.value(li);
                fv.addTo(fCounts[li], val);
            }
        }
        for (int li = 0; li < lCounts.length; li++) {
            for (int fi = 0; fi < fCounts[li].length; fi++) {
                fCounts[li][fi] /= lCounts[li];
            }
        }

        PerLabelInfoGain ig = new PerLabelInfoGain(ilist);
        Multimap<Integer, String> ret = HashMultimap.create();

        // iterate over labels
        for (int li = 0; li < targetAlphabet.size(); li++) {
            // try up to N queries per label
            for (int n = 0; n < numQueries; n++) {
                String feature = (String) ig.getInfoGain(li).getObjectAtRank(n);
                int idx = ig.getInfoGain(li).getIndexAtRank(n);
                if (!labeledFeatures.containsEntry(li, feature) && labelAndFeatureCorrelated(li, idx, fCounts))
                    ret.put(li, feature);
            }
        }

        return ret;
    }

    private static boolean labelAndFeatureCorrelated(int li, int idx, double[][] counts) {
        double best_value = 0;		
        for (int c = 0; c < counts.length; c++)
            if (counts[c][idx] > best_value)
                best_value = counts[c][idx]; 
        return counts[li][idx] > best_value * 0.75;
    }


    private static Set<String> pullClasses(double[][] counts, Alphabet targetAlphabet, int idx) {
        Set<String> ret = new HashSet<String>();
        double best_value = 0;		
        for (int c = 0; c < counts.length; c++) {
            if (counts[c][idx] > best_value) {
                best_value = counts[c][idx]; 
            }
        }
        for (int c = 0; c < counts.length; c++)
            if (counts[c][idx] > best_value * 0.75)
                ret.add( (String)targetAlphabet.lookupObject(c) );
        return ret;
    }

    public static InstanceList randomInstances(InstanceList poolData, int num) {
        poolData.shuffle(new Random());
        InstanceList ret = poolData.cloneEmpty();
        for (int ii=0; ii<num; ii++)
            ret.add(poolData.get(ii));
        return ret;
    }

    public static InstanceList queryInstances(Classifier c, InstanceList poolData, int num) {
        return queryInstances(c, poolData, num, "margin");
    }

    public static InstanceList queryInstances(Classifier c, InstanceList poolData, int num, String mode) {

        HashMap<Instance,Double> instanceMap = Maps.newHashMap();
        for (Instance instance : poolData) {
            LabelVector lv = c.classify(instance).getLabelVector();
            // entropy query strategy
            if (mode.equals("entropy"))
                instanceMap.put(instance, Util.entropy(lv.getValues()));
            // "least confident" strategy
            else if (mode.equals("lc"))
                instanceMap.put(instance, Util.leastConfident(lv));
            // margin strategy (default)
            else
                instanceMap.put(instance, Util.margin(lv));
        }
        InstanceList ret = poolData.cloneEmpty();
        int numQueries = 0;
        for (Map.Entry<Instance, Double> entry: rankMapByValue(instanceMap)) {
            Instance instance = entry.getKey();
            ret.add(instance);
            numQueries++;
            if (numQueries >= num)
                break;
        }
        return ret;
    }

    private static List<Map.Entry<Instance, Double>> rankMapByValue(Map<Instance,Double> map) {
        List<Map.Entry<Instance, Double>> ret = new Vector<Map.Entry<Instance, Double>>(map.entrySet());
        java.util.Collections.sort(ret, new Comparator<Map.Entry<Instance, Double>>(){
            public int compare(Map.Entry<Instance, Double> entry, Map.Entry<Instance, Double> entry1) {
                // Return 0 for a match, +1 for less than and -1 for more than
                return (entry.getValue().equals(entry1.getValue()) 
                        ? 0 : (entry.getValue() > entry1.getValue() 
                                ? -1 : 1));
            }
        });
        return ret;
    }

}
