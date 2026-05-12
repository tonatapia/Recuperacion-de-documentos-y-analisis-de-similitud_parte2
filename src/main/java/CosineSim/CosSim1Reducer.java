package CosineSim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;


public class CosSim1Reducer extends Reducer<Text, Text, Text, Text> {

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        String docId   = key.toString();
        String titulo  = "";
        StringBuilder vector = new StringBuilder();

        for (Text val : values) {
            String[] parts = val.toString().split(":");
            if (parts.length < 2) continue;
            String palabra = parts[0];
            String tfidf   = parts[1];
            if (parts.length >= 3 && titulo.isEmpty()) titulo = parts[2];

            if (vector.length() > 0) vector.append(" ");
            vector.append(palabra + ":" + tfidf);
        }

        context.write(new Text(docId), new Text(titulo + "\t" + vector.toString()));
    }
}