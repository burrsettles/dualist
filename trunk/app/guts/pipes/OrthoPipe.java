package guts.pipes;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.Instance;

public class OrthoPipe extends Pipe {

    public Instance pipe (Instance carrier) {
        AugmentableFeatureVector fv = (AugmentableFeatureVector) carrier.getData();
        String np = (String) carrier.getName();
        String shape = np.replaceAll("[A-Z]", "A").replaceAll("[a-z]", "a").replaceAll("[0-9]", "0").replaceAll("[\\s+]", "_").replaceAll("[^Aa0_]", "x");
        String[] words = np.split("\\s+");
        int caps = 0;
        for (String word : words)
            if (word.matches("[A-Z]\\w+"))
                caps ++;
        fv.add("CAPTIALIZED", caps);

        // normalize!
        fv.timesEquals(1/fv.twoNorm());

        fv.add("SHAPE="+shape, 1.0);
        if (words.length > 1)
            fv.add("FIRST="+words[0].toLowerCase(), 1.0);
        String lastWord = words[words.length-1].toLowerCase();
        fv.add("LAST="+lastWord, 1.0);
        if (lastWord.length() > 5) {
            fv.add("SUFFIX="+lastWord.substring(lastWord.length()-4), 1.0);
            fv.add("SUFFIX="+lastWord.substring(lastWord.length()-3), 1.0);
        }
        carrier.setData(fv);
        return carrier;
    }

}
