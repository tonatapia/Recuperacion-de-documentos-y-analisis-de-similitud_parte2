package CosineSim;

import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class CosSim2Reducer extends Reducer<Text, Text, Text, Text> {

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        double maxCos = -1;
        String bestVal = "";

        for (Text val : values) {
            String[] p = val.toString().split("\t", 3);
            if (p.length < 1) continue;
            double cos = Double.parseDouble(p[0]);
            if (cos > maxCos) {
                maxCos = cos;
                bestVal = val.toString();
            }
        }

        if (maxCos >= 0) {
            context.write(key, new Text(bestVal));
        }
    }
}