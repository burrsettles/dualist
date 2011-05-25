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
//				ret.put(li, fc.getObjectAtRank(rank).toString());
		}
		
		
		return ret;
	}

	public static Multimap<Integer, String> commonFeaturesPerLabel(
			HashMultimap<Integer, String> labeledFeatures,
			InstanceList ilist, int num) {

//		Alphabet dataAlphabet = ilist.getAlphabet();
		Alphabet targetAlphabet = ilist.getTargetAlphabet();
		FeatureCounts fc = new FeatureCounts(ilist); 
		
		Multimap<Integer, String> ret = HashMultimap.create();
		for (int li = 0; li < targetAlphabet.size(); li++) {
			for (int rank = 0; rank < num; rank++)
				ret.put(li, fc.getObjectAtRank(rank).toString());
		}

		return ret;
	}


	public static Multimap<Integer, String> queryFeaturesPerLabelMI(Classifier c, HashMultimap<Integer, String> labeledFeatures, InstanceList ilist, int numQueries) {
		
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
		
//		InfoGain ig = new InfoGain(ilist);
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

//		for (int i = 0; i < numQueries; i++) {
//			String feature = (String) ig.getObjectAtRank(i);
//			int idx = ig.getIndexAtRank(i);
//			Set<String> labels = pullClasses(fCounts, targetAlphabet, idx);
//			for (String label : labels) {
//				int li = targetAlphabet.lookupIndex( label );
//				if (!labeledFeatures.containsEntry(li, feature))
//					ret.put(li, feature);
//			}
//		}
		
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


    //	public static Multimap<String, String> perLabelQueryFeatures2(NaiveBayes c, Set<String> featureQueries, InstanceList trainSet, int numQueries) {
//	Multinomial.Logged[] params = c.getMultinomials();
//	Alphabet dataAlphabet = c.getAlphabet();
//	Alphabet labelAlphabet = c.getLabelAlphabet();
//
//	// this holds a utility score for each feature
//	double[] scores = new double[params[0].size()];
//	int[] toplabel = new int[params[0].size()];
//
//	// count vector for frequency biasing
//	double[] counts = new double[dataAlphabet.size()];
//	for (Instance instance : trainSet)
//		((FeatureVector)instance.getData()).addTo(counts);
//
//	for (int fi = 0; fi < scores.length; fi++) {
//		double[] probs = new double[params.length];
//		double sum = 0;
//		for (int li = 0; li < probs.length; li++) {
//			probs[li] = params[li].probability(fi);
//			sum += probs[li];
//		}
//		int best = 0;
//		double best_val = Double.MIN_VALUE;
//		for (int li = 0; li < probs.length; li++) {
//			probs[li] /= sum;
//			if (probs[li] > best_val) {
//				best_val = probs[li];
//				best = li;
//			}
//		}
//		toplabel[fi] = best;
//		scores[fi] = Math.exp( - Util.entropy(probs) ) * Math.log(1 + counts[fi]);
//	}
//	
//	RankedFeatureVector rfv = new RankedFeatureVector(params[0].getAlphabet(), scores);
//	
//	Multimap<String, String> ret = HashMultimap.create();
//
//	int ct = 0;
//	int rank = 0;
//	while (ct < numQueries) {
//		String feature = (String) rfv.getObjectAtRank(rank);
//		if (!featureQueries.contains(feature)) {
//			String label = labelAlphabet.lookupObject(toplabel[rfv.getIndexAtRank(rank)]).toString();
//			ret.put(label, feature);
//			ct++;
//		}
//		rank++;
//	}
//	
////	System.err.println("FEATURE QUERIES: " + ret);
//	
//	return ret;
//}
//
//
//
//
//
//public static Multimap<String, String> getPerLabelQueries(NaiveBayes c, Set<String> featureQueries, InstanceList trainSet, int numQueries) {
//
//	Multimap<String, String> ret = HashMultimap.create();
//	
//	Multinomial.Logged priors = c.getPriors();
//	Multinomial.Logged[] params = c.getMultinomials();
//	Alphabet dataAlphabet = c.getAlphabet();
//	Alphabet labelAlphabet = c.getLabelAlphabet();
//	
//	// count vector for frequency biasing
//	double[] counts = new double[dataAlphabet.size()];
//	for (Instance instance : trainSet)
//		((FeatureVector)instance.getData()).addTo(counts);
//	
//	double[][] scores = new double[labelAlphabet.size()][dataAlphabet.size()];
//
//	for (int fi = 0; fi < dataAlphabet.size(); fi++) {
//		double[] probs = new double[params.length];
//		double sum = 0;
//		for (int li = 0; li < probs.length; li++) {
////			probs[li] = priors.probability(li) * params[li].probability(fi);
//			probs[li] = params[li].probability(fi);
//			sum += probs[li];
//		}
//		for (int li = 0; li < probs.length; li++) {
//			probs[li] /= sum;
////			scores[li][fi] = Math.log(counts[fi]) * Math.log(probs[li] / (1-probs[li]) );
//			scores[li][fi] = Math.log(probs[li] / (1-probs[li]) );
//		}
//	}
//	
//	for (int li = 0; li < labelAlphabet.size(); li++) {
//		RankedFeatureVector rfv = new RankedFeatureVector(dataAlphabet, scores[li]);
//		String label = labelAlphabet.lookupObject(li).toString();
//		
//		int ct = 0;
//		int rank = 0;
//		while (ct < numQueries) {
//			String feature = (String) rfv.getObjectAtRank(rank);
//			if (!featureQueries.contains(feature)) {
//				ret.put(label, feature);
//				ct++;
//			}
//			rank++;
//		}
//	}
//
//	for (String label : ret.keySet()) {
//		System.err.print("#### " + label);
//		for (String feature : ret.get(label)) {
//			System.err.print("  " + feature);
//		}
//		System.err.println();
//	}
//	
//	return ret;
//}
//
//public static Set<String> queryFeatures(NaiveBayes c, Set<String> featureQueries, RankedFeatureVector counter, int num) {
//	Multinomial.Logged priors = c.getPriors();
//	Multinomial.Logged[] params = c.getMultinomials();
//	
//	// this holds a utility score for each feature
//	double[] scores = new double[params[0].size()];
//	int[] toplabel = new int[params[0].size()];
//	
//	for (int fi = 0; fi < scores.length; fi++) {
//		double[] probs = new double[params.length];
//		double sum = 0;
//		for (int li = 0; li < probs.length; li++) {
//			probs[li] = priors.probability(li) * params[li].probability(fi);
////			probs[li] = params[li].probability(fi);
//			sum += probs[li];
//		}
//		int best = 0;
//		double best_val = Double.MIN_VALUE;
//		for (int li = 0; li < probs.length; li++) {
//			probs[li] /= sum;
//			if (probs[li] > best_val) {
//				best_val = probs[li];
//				best = li;
//			}
//		}
//		toplabel[fi] = best;
//		scores[fi] = Math.exp( - Util.entropy(probs) ) * Math.log(1 + counter.value(fi));
//	}
//	
//	RankedFeatureVector rfv = new RankedFeatureVector(params[0].getAlphabet(), scores);
//	
//	Alphabet labelAlphabet = c.getLabelAlphabet();
//	
//	HashMap<String,Double> ret = Maps.newHashMap();
//	int numQueries = 0;
//	int rank = 0;
//	while (numQueries < num) {
//		String feature = (String) rfv.getObjectAtRank(rank);
//		if (!featureQueries.contains(feature)) {
//			String label = labelAlphabet.lookupObject(toplabel[rfv.getIndexAtRank(rank)]).toString();
//			ret.put(label+"||"+feature, rfv.getValueAtRank(rank));
//			numQueries++;
//		}
//		rank++;
//	}
//	
//	return ret.keySet();
//}


	
}
