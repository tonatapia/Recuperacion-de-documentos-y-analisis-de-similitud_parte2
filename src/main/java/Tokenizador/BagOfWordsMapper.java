package Tokenizador;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.json.JSONObject;

public class BagOfWordsMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
    private final static IntWritable one = new IntWritable(1);
    private Text outputKey = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
        String line = value.toString().trim();
        if (line.isEmpty()) return;

        try {
            JSONObject json = new JSONObject(line);
            String docId = json.getString("id");
            String texto = json.getString("texto");

            String[] palabras = texto.split("\\s+");
            for (String palabra : palabras) {
                if (palabra.length() > 0) {
                    outputKey.set(docId + "\t" + palabra);
                    context.write(outputKey, one);
                }
            }
        } catch (Exception e) {
            System.err.println("Error en mapper: " + line);
        }
    }
}