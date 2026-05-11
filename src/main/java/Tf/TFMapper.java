package TF;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Entrada:  salida de BagOfWords  →  "docId\tpalabra \t frecuencia"
 * Salida:   clave="docId"  valor="palabra:frecuencia"
 * El reducer necesita agrupar por docId para sumar el total de palabras.
 */
public class TFMapper extends Mapper<LongWritable, Text, Text, Text> {

    private Text outputKey   = new Text();
    private Text outputValue = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String line = value.toString().trim();
        if (line.isEmpty()) return;

        // Formato entrada: "docId\tpalabra\tfrecuencia"
        String[] parts = line.split("\t");
        if (parts.length != 3) return;

        String docId     = parts[0];
        String palabra   = parts[1];
        String frecuencia = parts[2];

        // Agrupamos por docId para que el reducer conozca todas las palabras
        outputKey.set(docId);
        outputValue.set(palabra + ":" + frecuencia);
        context.write(outputKey, outputValue);
    }
}