package guts;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import cc.mallet.classify.Boostable;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.NaiveBayes;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.pipe.Noop;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AlphabetCarrying;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import cc.mallet.types.Multinomial;

/**
 * Class used to generate a NaiveBayes classifier from a training data 
 * AND user-provided domain knowledge (i.e., "feature labels").
 * 
 * Prior knowledge is incorporated via simple pseudocounts on the feature multinomials. 
 * 
 * This class implements a MALLET trainer class for NaiveBayes classifiers, modified 
 * from the trainer in the MALLET source distribution.
 * 
 * @see NaiveBayes, NaiveBayesTrainer
 * @author Burr Settles
 */
public class NaiveBayesWithPriorsTrainer extends ClassifierTrainer<NaiveBayes> implements ClassifierTrainer.ByInstanceIncrements<NaiveBayes>, Boostable, AlphabetCarrying, Serializable
{
    // These function as default selections for the kind of Estimator used
    Multinomial.Estimator featureEstimator = new Multinomial.LaplaceEstimator();
    Multinomial.Estimator priorEstimator = new Multinomial.LaplaceEstimator();

    // Added to support incremental training.
    // These are the counts formed after NaiveBayes training.  Note that
    // these are *not* the estimates passed to the NaiveBayes classifier;
    // rather the estimates are formed from these counts.
    // we could break these five fields out into a inner class.
    Multinomial.Estimator[] me;
    Multinomial.Estimator pe;
    double docLengthNormalization = -1;  // A value of -1 means don't do any document length normalization
    NaiveBayes classifier;

    // If this style of incremental training is successful, the following members
    // should probably be moved up into IncrementalClassifierTrainer
    Pipe instancePipe;        // Needed to construct a new classifier
    Alphabet dataAlphabet;    // Extracted from InstanceList. Must be the same for all calls to incrementalTrain()
    Alphabet targetAlphabet; // Extracted from InstanceList. Must be the same for all calls to incrementalTrain

    // These are used to compute background knowledge on features
    Multimap<Object,Object> labelFeatures;	// data structure storing a map from labels to features 
    private double alpha = 100;

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public NaiveBayesWithPriorsTrainer (NaiveBayes initialClassifier) {
        if (initialClassifier != null) {
            this.instancePipe = initialClassifier.getInstancePipe();
            this.dataAlphabet = initialClassifier.getAlphabet();
            this.targetAlphabet = initialClassifier.getLabelAlphabet();
            this.classifier = initialClassifier;
        }
    }

    public NaiveBayesWithPriorsTrainer (Pipe instancePipe) {
        this.instancePipe = instancePipe;
        this.dataAlphabet = instancePipe.getDataAlphabet();
        this.targetAlphabet = instancePipe.getTargetAlphabet();
    }

    public NaiveBayesWithPriorsTrainer () {
    }

    public NaiveBayesWithPriorsTrainer (Multimap<Object,Object> labelFeatures) {
        this.labelFeatures = labelFeatures;
    }

    public NaiveBayes getClassifier () { return classifier; }

    public NaiveBayesWithPriorsTrainer setDocLengthNormalization (double d) {
        docLengthNormalization = d;
        return this;
    }

    public double getDocLengthNormalization () {
        return docLengthNormalization;
    }

    /**
     *  Get the MultinomialEstimator instance used to specify the type of estimator
     *  for features.
     *
     * @return  estimator to be cloned on next call to train() or first call
     * to incrementalTrain()
     */
    public Multinomial.Estimator getFeatureMultinomialEstimator ()
    {
        return featureEstimator;
    }

    /**
     * Set the Multinomial Estimator used for features. The MulitnomialEstimator
     * is internally cloned and the clone is used to maintain the counts
     * that will be used to generate probability estimates
     * the next time train() or an initial incrementalTrain() is run.
     * Defaults to a Multinomial.LaplaceEstimator()
     * @param me to be cloned on next call to train() or first call
     * to incrementalTrain()
     */
    public NaiveBayesWithPriorsTrainer setFeatureMultinomialEstimator (Multinomial.Estimator me)
    {
        if (instancePipe != null)
            throw new IllegalStateException("Can't set after incrementalTrain() is called");
        featureEstimator = me;
        return this;
    }

    /**
     *  Get the MultinomialEstimator instance used to specify the type of estimator
     *  for priors.
     *
     * @return  estimator to be cloned on next call to train() or first call
     * to incrementalTrain()
     */
    public Multinomial.Estimator getPriorMultinomialEstimator ()
    {
        return priorEstimator;
    }

    /**
     * Set the Multinomial Estimator used for priors. The MulitnomialEstimator
     * is internally cloned and the clone is used to maintain the counts
     * that will be used to generate probability estimates
     * the next time train() or an initial incrementalTrain() is run.
     * Defaults to a Multinomial.LaplaceEstimator()
     * @param me to be cloned on next call to train() or first call
     * to incrementalTrain()
     */
    public NaiveBayesWithPriorsTrainer setPriorMultinomialEstimator (Multinomial.Estimator me)
    {
        //		if (instancePipe != null)
        //			throw new IllegalStateException("Can't set after incrementalTrain() is called");
        priorEstimator = me;
        return this;
    }



    /**
     * Create a NaiveBayes classifier from a set of training data.
     * The trainer uses counts of each feature in an instance's feature vector
     * to provide an estimate of p(Labeling| feature).  The internal state
     * of the trainer is thrown away ( by a call to reset() ) when train() returns. Each
     * call to train() is completely independent of any other.
     * @param trainingList        The InstanceList to be used to train the classifier.
     * Within each instance the data slot is an instance of FeatureVector and the
     * target slot is an instance of Labeling
     * @param validationList      Currently unused
     * @param testSet             Currently unused
     * @param evaluator           Currently unused
     * @param initialClassifier   Currently unused
     * @return The NaiveBayes classifier as trained on the trainingList
     */
    public NaiveBayes train (InstanceList trainingList)
    {
        // Forget all the previous sufficient statistics counts;
        me = null; pe = null;
        // Train a new classifier based on this data
        this.classifier = trainIncremental (trainingList);
        return classifier;
    }

    public NaiveBayes trainIncremental (InstanceList trainingInstancesToAdd) 
    {
        // Initialize and check instance variables as necessary...
        setup(trainingInstancesToAdd, null);

        // Incrementally add the counts of this new training data
        for (Instance instance : trainingInstancesToAdd)
            incorporateOneInstance(instance, trainingInstancesToAdd.getInstanceWeight(instance));

        // Estimate multinomials, and return a new naive Bayes classifier.  
        // Note that, unlike MaxEnt, NaiveBayes is immutable, so we create a new one each time.
        classifier = new NaiveBayes (instancePipe, pe.estimate(), estimateFeatureMultinomials());
        return classifier;
    }

    public NaiveBayes trainIncremental (Instance instance) {
        setup (null, instance);

        // Incrementally add the counts of this new training instance
        incorporateOneInstance (instance, 1.0);
        if (instancePipe == null)
            instancePipe = new Noop (dataAlphabet, targetAlphabet);
        classifier = new NaiveBayes (instancePipe, pe.estimate(), estimateFeatureMultinomials());
        return classifier;
    }


    private void setup (InstanceList instances, Instance instance) {
        assert (instances != null || instance != null);
        //		if (instance == null && instances != null)
        //			instance = instances.get(0);
        // Initialize the alphabets
        if (dataAlphabet == null) {
            this.dataAlphabet = instances.getDataAlphabet();
            this.targetAlphabet = instances.getTargetAlphabet();
        }
        else if (!Alphabet.alphabetsMatch(instances, this))
            // Make sure the alphabets match 
            throw new IllegalArgumentException ("Training set alphabets do not match those of NaiveBayesTrainer.");

        // Initialize or check the instancePipe
        if (instances != null) {
            if (instancePipe == null)
                instancePipe = instances.getPipe();
            else if (instancePipe != instances.getPipe())
                // Make sure that this pipes match.  Is this really necessary??  
                // I don't think so, but it could be confusing to have each returned classifier have a different pipe?  -akm 1/08
                throw new IllegalArgumentException ("Training set pipe does not match that of NaiveBayesTrainer.");
        }

        if (me == null) {
            int numLabels = targetAlphabet.size();
            me = new Multinomial.Estimator[numLabels];
            for (int i = 0; i < numLabels; i++) {
                me[i] = (Multinomial.Estimator) featureEstimator.clone();
                me[i].setAlphabet(dataAlphabet);
            }
            pe = (Multinomial.Estimator) priorEstimator.clone();
            pe.setAlphabet(targetAlphabet);
        }

        if (targetAlphabet.size() > me.length) {
            // target alphabet grew. increase size of our multinomial array
            int targetAlphabetSize = targetAlphabet.size();
            // copy over old values
            Multinomial.Estimator[] newMe = new Multinomial.Estimator[targetAlphabetSize];
            System.arraycopy (me, 0, newMe, 0, me.length);
            // initialize new expanded space
            for (int i= me.length; i<targetAlphabetSize; i++){
                Multinomial.Estimator mest = (Multinomial.Estimator)featureEstimator.clone ();
                mest.setAlphabet (dataAlphabet);
                newMe[i] = mest;
            }
            me = newMe;
        }
    }

    private void incorporateOneInstance (Instance instance, double instanceWeight) 
    {
        Labeling labeling = instance.getLabeling ();
        if (labeling == null) return; // Handle unlabeled instances by skipping them
        FeatureVector fv = (FeatureVector) instance.getData ();
        double oneNorm = fv.oneNorm();
        if (oneNorm <= 0) return; // Skip instances that have no features present
        if (docLengthNormalization > 0)
            // Make the document have counts that sum to docLengthNormalization
            // I.e., if 20, it would be as if the document had 20 words.
            instanceWeight *= docLengthNormalization / oneNorm;
        assert (instanceWeight > 0 && !Double.isInfinite(instanceWeight));
        for (int lpos = 0; lpos < labeling.numLocations(); lpos++) {
            int li = labeling.indexAtLocation (lpos);
            double labelWeight = labeling.valueAtLocation (lpos);
            if (labelWeight == 0) continue;
            //System.out.println ("NaiveBayesTrainer me.increment "+ labelWeight * instanceWeight);
            me[li].increment (fv, labelWeight * instanceWeight);
            // This relies on labelWeight summing to 1 over all labels
            pe.increment (li, labelWeight * instanceWeight);
        }
    }

    private Multinomial[] estimateFeatureMultinomials () {
        int numLabels = targetAlphabet.size();
        Multinomial[] m = new Multinomial[numLabels];

        // first add all the appropriate pseudocounts (derived from feature labels)
        // to the multinomial estimator
        if (labelFeatures != null) {
            filterlabelFeatures();
            for (Object label : labelFeatures.keySet()) {
                int li = targetAlphabet.lookupIndex(label);
                for (Object feature : labelFeatures.get(label)) {
                    int fi = dataAlphabet.lookupIndex(feature);
                    me[li].increment(fi, alpha);
                }
            }
        }		

        // now estimate conditionals from data
        for (int li = 0; li < numLabels; li++) {
            m[li] = me[li].estimate();
            //			System.out.println(m[li]);
        }

        return m;
    }

    public void addLabelFeature(Object label, Object feature) {
        if (labelFeatures == null)
            labelFeatures = HashMultimap.create();

        if (targetAlphabet.lookupIndex(label) != -1 &&
                dataAlphabet.lookupIndex(feature) != -1)
            labelFeatures.put(label, feature);
    }

    public void removeLabelFeature(Object label, Object feature) {
        if (labelFeatures == null)
            labelFeatures = HashMultimap.create();

        if (targetAlphabet.lookupIndex(label) != -1 &&
                dataAlphabet.lookupIndex(feature) != -1)
            labelFeatures.remove(label, feature);
    }

    public boolean hasPriorsSet() {
        return labelFeatures != null;
    }

    /**
     * Make sure that all keys and values in the background knowledge are actually in our alphabets!
     */
    private void filterlabelFeatures() {
        for (Object key : labelFeatures.keySet()) {
            // remove if we don't know this label
            if (targetAlphabet.lookupIndex(key, false) == -1)
                labelFeatures.removeAll(key);
            else {
                // remove if we do know the label but not the feature
                for (Object value : labelFeatures.get(key)) {
                    if (dataAlphabet.lookupIndex(value, false) == -1)
                        labelFeatures.remove(key, value);
                }
            }
        }
    }

    /**
     * Create a NaiveBayes classifier from a set of training data and the
     * previous state of the trainer.  Subsequent calls to incrementalTrain()
     * add to the state of the trainer.  An incremental training session
     * should consist only of calls to incrementalTrain() and have no
     * calls to train();     *
     * @param trainingList        The InstanceList to be used to train the classifier.
     * Within each instance the data slot is an instance of FeatureVector and the
     * target slot is an instance of Labeling
     * @param validationList      Currently unused
     * @param testSet             Currently unused
     * @param evaluator           Currently unused
     * @param initialClassifier   Currently unused
     * @return The NaiveBayes classifier as trained on the trainingList and the previous
     * trainingLists passed to incrementalTrain()
     */

    public String toString()
    {
        return "NaiveBayesPoolingTrainer";
    }


    // AlphabetCarrying interface
    public boolean alphabetsMatch(AlphabetCarrying object) {
        return Alphabet.alphabetsMatch (this, object);
    }

    public Alphabet getAlphabet() {
        return dataAlphabet;
    }

    public Alphabet[] getAlphabets() {
        return new Alphabet[] { dataAlphabet, targetAlphabet };
    }


    // Serialization
    // serialVersionUID is overriden to prevent innocuous changes in this
    // class from making the serialization mechanism think the external
    // format has changed.

    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 1;

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.writeInt(CURRENT_SERIAL_VERSION);

        //default selections for the kind of Estimator used
        out.writeObject(featureEstimator);
        out.writeObject(priorEstimator);

        // These are the counts formed after NaiveBayes training.
        out.writeObject(me);
        out.writeObject(pe);

        // pipe and alphabets
        out.writeObject(instancePipe);
        out.writeObject(dataAlphabet);
        out.writeObject(targetAlphabet);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        if (version != CURRENT_SERIAL_VERSION)
            throw new ClassNotFoundException("Mismatched NaiveBayesTrainer versions: wanted " +
                    CURRENT_SERIAL_VERSION + ", got " +
                    version);

        //default selections for the kind of Estimator used
        featureEstimator = (Multinomial.Estimator) in.readObject();
        priorEstimator = (Multinomial.Estimator) in.readObject();

        // These are the counts formed after NaiveBayes training.
        me = (Multinomial.Estimator []) in.readObject();
        pe = (Multinomial.Estimator) in.readObject();

        // pipe and alphabets
        instancePipe = (Pipe) in.readObject();
        dataAlphabet = (Alphabet) in.readObject();
        targetAlphabet = (Alphabet) in.readObject();
    }

    public static class Factory extends ClassifierTrainer.Factory<NaiveBayesTrainer> 
    {
        Multinomial.Estimator featureEstimator = new Multinomial.LaplaceEstimator();
        Multinomial.Estimator priorEstimator = new Multinomial.LaplaceEstimator();
        double docLengthNormalization = -1;

        public NaiveBayesTrainer newClassifierTrainer(Classifier initialClassifier) {
            return new NaiveBayesTrainer ((NaiveBayes)initialClassifier);
        }
        public NaiveBayesWithPriorsTrainer.Factory setDocLengthNormalization (double docLengthNormalization) {
            this.docLengthNormalization = docLengthNormalization;
            return this;
        }

        public NaiveBayesWithPriorsTrainer.Factory setFeatureMultinomialEstimator (Multinomial.Estimator featureEstimator) {
            this.featureEstimator = featureEstimator;
            return this;
        }

        public NaiveBayesWithPriorsTrainer.Factory setPriorMultinomialEstimator (Multinomial.Estimator priorEstimator) {
            this.priorEstimator = priorEstimator;
            return this;
        }

    }

}
