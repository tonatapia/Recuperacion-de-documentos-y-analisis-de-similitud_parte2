package CosineSim;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Entrada:  salida TF-IDF
 *   "palabra \t idf \t numDocs \t docId|freq|total|tf|idf|tfidf|titulo;..."
 * Salida:   clave=docId   valor="palabra:tfidf"
 */
public class CosSim1Mapper extends Mapper<LongWritable, Text, Text, Text> {

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String line = value.toString().trim();
        if (line.isEmpty()) return;

        String[] parts = line.split("\t", 4);
        if (parts.length < 4) return;

        String palabra  = parts[0];
        String posting  = parts[3];

        for (String entry : posting.split(";")) {
            String[] campos = entry.split("\\|");
            if (campos.length < 6) continue;
            String docId  = campos[0];
            String tfidf  = campos[5];
            String titulo = campos.length >= 7 ? campos[6] : "";

            context.write(
                new Text(docId),
                new Text(palabra + ":" + tfidf + ":" + titulo)
            );
        }
    }
}