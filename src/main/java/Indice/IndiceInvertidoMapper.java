package IndiceInvertido;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Entrada:  salida de TF  →  "palabra\tdocId \t frecuencia\ttotal\ttf"
 * Salida:   clave="palabra"   valor="docId:frecuencia:total:tf"
 */
public class IndiceInvertidoMapper extends Mapper<LongWritable, Text, Text, Text> {

    private Text outputKey   = new Text();
    private Text outputValue = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String line = value.toString().trim();
        if (line.isEmpty()) return;

        // Formato: "palabra\tdocId\tfrecuencia\ttotal\ttf"
        String[] parts = line.split("\t");
        if (parts.length != 5) return;

        String palabra    = parts[0];
        String docId      = parts[1];
        String frecuencia = parts[2];
        String total      = parts[3];
        String tf         = parts[4];

        outputKey.set(palabra);
        outputValue.set(docId + ":" + frecuencia + ":" + total + ":" + tf);
        context.write(outputKey, outputValue);
    }
}