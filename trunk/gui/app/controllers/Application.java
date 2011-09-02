package controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import play.Logger;
import play.cache.Cache;
import play.data.validation.Required;
import play.mvc.Controller;
import cc.mallet.classify.NaiveBayes;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Labeling;
import cc.mallet.types.Multinomial;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

import dualist.classify.NaiveBayesWithPriorsTrainer;
import dualist.classify.Queries;
import dualist.tui.Util;

public class Application extends Controller {

    private static DecimalFormat df = new DecimalFormat("0.00000");
    private static String RESULTS_DIR = "results/";
    private static String MODELS_DIR = "models/";

    public static void index() {
        if ((new File(RESULTS_DIR)).mkdirs())
            Logger.info("Results output directory '%s' created", RESULTS_DIR);
        if ((new File(MODELS_DIR)).mkdirs())
            Logger.info("Model output directory '%s' created", MODELS_DIR);
        render();
    }

    public static void explore(String username) {
        render(username);
    }

    public static void setupExplore(@Required String username, @Required File dataset, @Required String labels, @Required String type, @Required int numInstances) throws Exception {

        if(validation.hasErrors()) {
            flash.error(validation.errors().toString());
            flash.error("Oops, you must be sure to fill in all fields!");
            explore(username);
        }

        // create label alphabet...
        LabelAlphabet labelAlphabet = new LabelAlphabet();
        String[] myLabels = labels.trim().split("\\s*,\\s*");
        for (String label : myLabels)
            labelAlphabet.lookupIndex(label, true);
        
        // set up the data-processing pipeline
        Pipe myPipe = Util.getPipe(type);

        // process the data set...
        Logger.info("Loading '%s' data set...", dataset);
        InstanceList ilist = Util.readZipData(dataset, myPipe, labelAlphabet);
        Alphabet dataAlphabet = ilist.getDataAlphabet();

        // set up train/test splits and store them in the cache
        Cache.set(session.getId()+"-testSet", ilist.cloneEmpty(), "90mn");
        Cache.set(session.getId()+"-unlabeledSet", ilist, "90mn");
        Cache.set(session.getId()+"-labeledSet", ilist.cloneEmpty(), "90mn");

        Cache.set(session.getId()+"-username", username, "90mn");
        Cache.set(session.getId()+"-dataset", dataset.getName(), "90mn");
        Cache.set(session.getId()+"-type", type, "90mn");
        Cache.set(session.getId()+"-mode", "dual", "90mn");
        Cache.set(session.getId()+"-numMinutes", 360, "90mn");
        Cache.set(session.getId()+"-numInstances", numInstances, "90mn");
        Cache.set(session.getId()+"-startTime", (System.currentTimeMillis()/1000), "90mn" );

        Cache.set(session.getId()+"-explore", true, "90mn");

        Logger.info("|featureSet|=%s", dataAlphabet.size());
        Logger.info("|labelSet|=%s", labelAlphabet.size());
        Logger.info("|dataSet|=%s", ilist.size());
        Logger.info("User: %s", username);

        clearResult();
        logResult("%% |featureSet|=" + dataAlphabet.size());
        logResult("%% |dataSet|=" + ilist.size());
        for (int li=0; li < labelAlphabet.size(); li++)
            logResult("%% " + li + "=" + labelAlphabet.lookupObject(li));

        learn("","","");
    }

    public static void experiment(String username) {
        render(username);
    }

    public static void setupExperiment(@Required String username, @Required File trainFile, File testFile, @Required String type, @Required String mode, @Required int numMinutes, @Required int numInstances) throws Exception {

        if(validation.hasErrors()) {
            flash.error(validation.errors().toString());
            flash.error("Oops, you must be sure to fill in all fields!");
            experiment(username);
        }

        InstanceList trainSet;
        InstanceList testSet;
        Alphabet dataAlphabet;
        LabelAlphabet labelAlphabet;
        
        // set up the data-processing pipeline
        Pipe myPipe = Util.getPipe(type);

        // if a test set is supplied, use its annotations 
        // and allow the training corpus to be unlabeled
        if (testFile != null) {
            Logger.info("Loading '%s' test set...", testFile);
            testSet = Util.readZipData(testFile, myPipe, null);
            dataAlphabet = testSet.getDataAlphabet();
            labelAlphabet = (LabelAlphabet) testSet.getTargetAlphabet();
            Logger.info("Loading '%s' training set...", trainFile);
            trainSet = Util.readZipData(trainFile, myPipe, labelAlphabet);
        }
        // otherwise, assume all training data is labeled, 
        // sample 10% as a held-out test set
        else {
            Logger.info("Loading '%s' data set...", trainFile);
            InstanceList ilist = Util.readZipData(trainFile, myPipe, null);
            dataAlphabet = ilist.getDataAlphabet();
            labelAlphabet = (LabelAlphabet) ilist.getTargetAlphabet();
            Logger.info("Splitting train/set set automatically...");
            InstanceList[] split = ilist.split(new Random(27), new double[]{0.9,0.1});
            trainSet = split[0];
            testSet = split[1];
        }

        // sanity check: inspect the label distribution of the test set
        int[] counts = new int[labelAlphabet.size()];
        for (Instance instance : testSet) {
            int li = ((Labeling)instance.getTarget()).getBestIndex();
            counts[li]++;
        }
        for (int li = 0; li < counts.length; li++)
            Logger.info("Test label '%s': %s instances", labelAlphabet.lookupObject(li), counts[li]);

        // set up train/test splits and store them in the cache
        Cache.set(session.getId()+"-testSet", testSet, "90mn");
        Cache.set(session.getId()+"-unlabeledSet", trainSet, "90mn");
        Cache.set(session.getId()+"-labeledSet", trainSet.cloneEmpty(), "90mn");

        Cache.set(session.getId()+"-username", username, "90mn");
        Cache.set(session.getId()+"-dataset", trainFile.getName(), "90mn");
        Cache.set(session.getId()+"-type", type, "90mn");
        Cache.set(session.getId()+"-mode", mode, "90mn");
        Cache.set(session.getId()+"-numMinutes", numMinutes, "90mn");
        Cache.set(session.getId()+"-numInstances", numInstances, "90mn");
        Cache.set(session.getId()+"-startTime", (System.currentTimeMillis()/1000), "90mn" );

        Cache.set(session.getId()+"-explore", false, "90mn");

        Logger.info("|featureSet|=%s", dataAlphabet.size());
        Logger.info("|labelSet|=%s", labelAlphabet.size());
        Logger.info("|trainSet|=%s", trainSet.size());
        Logger.info("|testSet|=%s", testSet.size());
        Logger.info("User: %s", username);

        clearResult();
        if (testFile != null)
            logResult("%% separate train/test sets were supplied");
        else
            logResult("%% data set automatically split into train (90%) / test (10%)");
        logResult("%% |featureSet|=" + dataAlphabet.size());
        logResult("%% |trainSet|=" + trainSet.size());
        logResult("%% |testSet|=" + testSet.size());
        for (int li=0; li < labelAlphabet.size(); li++)
            logResult("%% " + li + "=" + labelAlphabet.lookupObject(li));

        learn("","","");
    }

    public static void learn(String features, String instances, String log) throws Exception {

        // evaluation bidness
        logResult(log);

        // retrieve resources from cache
        long startTime = (Long) Cache.get(session.getId()+"-startTime");
        String dataset = (String) Cache.get(session.getId()+"-dataset");
        String mode = (String) Cache.get(session.getId()+"-mode");
        int numMinutes = (Integer) Cache.get(session.getId()+"-numMinutes");
        int numInstances = (Integer) Cache.get(session.getId()+"-numInstances");
        String username = (String) Cache.get(session.getId()+"-username");
        InstanceList testSet = (InstanceList) Cache.get(session.getId()+"-testSet");
        InstanceList labeledSet = (InstanceList) Cache.get(session.getId()+"-labeledSet");
        InstanceList unlabeledSet = (InstanceList) Cache.get(session.getId()+"-unlabeledSet");
        boolean explore = (Boolean) Cache.get(session.getId()+"-explore");

        HashMultimap<Integer,String> labeledFeatures = HashMultimap.create();

        long timeSoFar = (System.currentTimeMillis()/1000) - startTime;

        // process newly-labeled features for this round
        LabelAlphabet labelAlphabet = (LabelAlphabet) labeledSet.getTargetAlphabet();
        for (String labeledFeature : features.trim().split("\\s+")) {
            if (!labeledFeature.isEmpty()) {
                String[] bits = labeledFeature.split("\\|\\|");
                int labelIndex = Integer.parseInt(bits[0]);
                String feature = bits[1];
                labeledFeatures.put(labelIndex, feature);
            }
        }

        // process newly-labeled instances
        InstanceList deleteSet = unlabeledSet.cloneEmpty();
        for (String labeledInstance : instances.trim().split("\\s+")) {
            if (!labeledInstance.isEmpty()) {
                String[] bits = labeledInstance.split("\\|\\|");
                int li = Integer.parseInt(bits[0]);
                String instanceName = bits[1];
                // slow, but offhand the best way to search for a labeled instance...
                for (Instance instance : unlabeledSet) {
                    if (instanceName.equals(instance.getName().toString())) {
                        deleteSet.add(instance);
                        if (li >= 0) {
                            int ti = labelAlphabet.lookupIndex( instance.getTarget().toString() );
                            if (!explore && li != ti) {
                                logResult(timeSoFar + "\toracleError\t" + li + "|" + ti + "|" + instance.getName().toString());
                            }
                            instance.unLock();
                            instance.setTarget(labelAlphabet.lookupLabel(li));
                            instance.lock();
                            labeledSet.add(instance);
                        }
                    }
                }
            }
        }
        unlabeledSet.removeAll(deleteSet);

        Logger.info("|U|=%s, |L|=%s, |feats|=%s", unlabeledSet.size(), labeledSet.size(), labeledFeatures.size());

        // set up trainer object for this iteration
        NaiveBayesWithPriorsTrainer nbTrainer = new NaiveBayesWithPriorsTrainer(labeledSet.getPipe()); 
        nbTrainer.setPriorMultinomialEstimator(new Multinomial.MEstimator(5));
        nbTrainer.setAlpha(50);
        for (int li : labeledFeatures.keySet()) {
            String label = labelAlphabet.lookupObject(li).toString();
            for (String feature : labeledFeatures.get(li)) {
                nbTrainer.addLabelFeature(label, feature);
            }
        }

        // train the initial model
        NaiveBayes nbModel; 
        // if we're querying features too, do the 1-step EM part of things
        if (mode.equals("dual")) {
            nbModel = nbTrainer.train( labeledSet.cloneEmpty() );
            InstanceList trainSet2 = Util.probabilisticData(nbModel, labeledSet, unlabeledSet);
            nbModel = nbTrainer.train (trainSet2);
        }
        // otherwise, just train using the labeled data
        else
            nbModel = nbTrainer.train( labeledSet );

        // save the learned classifier (timestamped)
        String modelFile = MODELS_DIR + getTrialID() + "." + String.format("%04d", (int)timeSoFar) + ".model";
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelFile));
        oos.writeObject(nbModel);
        oos.close();
        
        // TODO: save the labeled instances and features, as well! 

        // TODO: for some reason this eval is broken for the time being...
        // just do evals using saved models (code above)
        //        if (!explore) {
        //            double accuracy = nbModel.getAccuracy(testSet);
        //            logResult(timeSoFar + "\taccuracy\t" + accuracy);
        //            double[] f1s = new double[labelAlphabet.size()];
        //            for (int li = 0; li < labelAlphabet.size(); li++) {
        //                f1s[li] = nbModel.getF1(testSet, li);
        //                logResult(timeSoFar + "\tF1:" + li + "\t" + f1s[li]);
        //            }
        //            double macroF1 = Util.average(f1s);
        //            logResult(timeSoFar + "\tmacroF1\t" + macroF1);
        //        }


        // FIRST!!! -- determine if too much time has passed, if so then die.
        if (timeSoFar > numMinutes * 60) {
            flash.error("Time is up! Please select another experiment to run.");
            experiment(username);
        }

        // query container objects
        Multimap<Integer,String> queryFeatures = null;
        InstanceList queryInstances = null;

        // misc. tests to see if it's safe to switch to active learning
        Set<String> instLabels = new HashSet<String>();
        for (Instance inst : labeledSet)
            instLabels.add(inst.getTarget().toString());
        boolean instancesCovered = ( labelAlphabet.size() == instLabels.size() ) 
        && ( labeledSet.size() > 1.5 * labelAlphabet.size() );
        boolean featuresCovered = ( labelAlphabet.size() == labeledFeatures.keySet().size() );

        // if we're in passive mode, or have insufficient labels, do passive selection
        if (mode.equals("passive") || ( !featuresCovered && !instancesCovered ) ) {
            Logger.info("PASSIVE QUERYING");
            logResult(timeSoFar+"\tPASSIVE");
            queryInstances = Queries.randomInstances(unlabeledSet, numInstances );
            //			queryFeatures = Queries.randomFeaturesPerLabel(labeledFeatures, unlabeledSet, 50);
            queryFeatures = Queries.commonFeaturesPerLabel(labeledFeatures, unlabeledSet, 100);
        }
        // otherwise, query actively
        else {
            Logger.info("ACTIVE QUERYING");
            logResult(timeSoFar+"\tACTIVE");
            queryInstances = Queries.queryInstances(nbModel, unlabeledSet, numInstances, "entropy" );
            // do the per-label query thang
            queryFeatures = Queries.queryFeaturesPerLabelMI(nbModel, labeledFeatures, 
                    Util.probabilisticData(nbModel, labeledSet, unlabeledSet), 100);
        }

        // update cache
        Cache.set(session.getId()+"-testSet", testSet, "90mn");
        Cache.set(session.getId()+"-unlabeledSet", unlabeledSet, "90mn");
        Cache.set(session.getId()+"-labeledSet", labeledSet, "90mn");

        // done! render!
        render(mode, username, dataset, timeSoFar, labelAlphabet, queryInstances, queryFeatures, labeledFeatures);
    }

    private static void clearResult() {
        File f = new File(RESULTS_DIR + getTrialID() + ".txt");
        f.delete();
    }

    private static void logResult(String string) {
        try {
            Files.append(string.trim() + "\n", 
                    new File(RESULTS_DIR + getTrialID() + ".txt"), 
                    Charsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }		
    }

    private static String getTrialID() {
        String mode = (String) Cache.get(session.getId()+"-mode");
        String dataset = (String) Cache.get(session.getId()+"-dataset");
        String username = (String) Cache.get(session.getId()+"-username");
        boolean explore = (Boolean) Cache.get(session.getId()+"-explore");
        String trialID = username+"_"+dataset+"_"+mode;
        if (explore)
            trialID = trialID + "_explore";
        return trialID;
    }
    
    public static void predict(String features, String instances) {

        // retrieve resources from cache
        String mode = (String) Cache.get(session.getId()+"-mode");
        InstanceList labeledSet = (InstanceList) Cache.get(session.getId()+"-labeledSet");
        InstanceList unlabeledSet = (InstanceList) Cache.get(session.getId()+"-unlabeledSet");

        HashMultimap<Integer,String> labeledFeatures = HashMultimap.create();

        // process newly-labeled features for this round
        LabelAlphabet labelAlphabet = (LabelAlphabet) labeledSet.getTargetAlphabet();
        for (String labeledFeature : features.trim().split("\\s+")) {
            if (!labeledFeature.isEmpty()) {
                String[] bits = labeledFeature.split("\\|\\|");
                int labelIndex = Integer.parseInt(bits[0]);
                String feature = bits[1];
                labeledFeatures.put(labelIndex, feature);
            }
        }

        // set up trainer object for this iteration
        NaiveBayesWithPriorsTrainer nbTrainer = new NaiveBayesWithPriorsTrainer(labeledSet.getPipe()); 
        nbTrainer.setPriorMultinomialEstimator(new Multinomial.MEstimator(4));
        nbTrainer.setAlpha(100);
        for (int li : labeledFeatures.keySet()) {
            String label = labelAlphabet.lookupObject(li).toString();
            for (String feature : labeledFeatures.get(li)) {
                nbTrainer.addLabelFeature(label, feature);
            }
        }
        // train the initial model
        NaiveBayes nbModel; 
        // if we're querying features too, do the 1-step EM part of things
        if (mode.equals("dual")) {
            nbModel = nbTrainer.train( labeledSet.cloneEmpty() );
            InstanceList trainSet2 = Util.probabilisticData(nbModel, labeledSet, unlabeledSet);
            nbModel = nbTrainer.train (trainSet2);
        }
        // otherwise, just train using the labeled data
        else {
            nbModel = nbTrainer.train( labeledSet );
        }

        StringBuffer sb = new StringBuffer();

        for (Instance instance : unlabeledSet) {
            Labeling l = nbModel.classify(instance).getLabeling();
            String summary = instance.getSource().toString().trim().replaceAll("\\s+", " ");
            if (summary.length() > 150)
                summary = summary.substring(0, 150) + "...";
            sb.append(l.getBestLabel().toString() + "\t" + df.format(l.getBestValue()) 
                    + "\t" + instance.getName() + "\t" + summary + "\n");
        }

        // write out labeled features
        sb.append("\n##############################################\n");
        for (int li : labeledFeatures.keySet()) {
            String label = labelAlphabet.lookupObject(li).toString();
            sb.append("#\t" + label);
            for (String feature : labeledFeatures.get(li)) {
                sb.append("\t" + feature);
            }
            sb.append("\n");
        }

        // writ out labeled instances
        sb.append("\n##############################################\n");
        for (Instance instance : labeledSet) {
            Labeling l = instance.getLabeling();
            String summary = instance.getSource().toString().trim().replaceAll("\\s+", " ");
            if (summary.length() > 150)
                summary = summary.substring(0, 150) + "...";
            sb.append("# " + l.getBestLabel().toString() 
                    + "\t" + instance.getName() + "\t" + summary + "\n");
        }

        render(sb);

    }

}