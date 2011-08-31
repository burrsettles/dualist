package dualist.pipes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;

public class FixEmoticons extends Pipe {
    
    private Pattern regex;
    
    public FixEmoticons(Pattern regex) {
        this.regex = regex;
    }

    public Instance pipe (Instance carrier) {
        String text = (String) carrier.getData();
        Matcher m = regex.matcher(text);

        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            sb.append(text.substring(last, m.start()));
            sb.append(m.group(0).toUpperCase());
            last = m.end();
        }
        sb.append(text.substring(last));

        carrier.setData(sb.toString());
        return carrier;
    }

}
