package TFIDF;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;


public class TFIDFMapper extends Mapper<LongWritable, Text, Text, Text> {

    private Text outputKey   = new Text();
    private Text outputValue = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String line = value.toString().trim();
        if (line.isEmpty()) return;

        String[] parts = line.split("\t", 3);
        if (parts.length != 3) return;

        outputKey.set(parts[0]);                    // palabra
        outputValue.set(parts[1] + "\t" + parts[2]); // numDocs\tposting
        context.write(outputKey, outputValue);
    }
}