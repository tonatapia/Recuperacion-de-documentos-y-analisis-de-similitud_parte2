package TFIDF;

import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Lee el total de documentos desde la configuración (clave "totalDocs").
 * Salida: "palabra \t idf \t numDocs \t posting_con_tfidf"
 *
 * Cada entrada del posting pasa de:
 *   docId:freq:total:tf
 * a:
 *   docId|freq|total|tf|idf|tfidf|titulo   (titulo se agrega vacío aquí;
 *   si tu JSONL tiene título, añádelo al TFMapper)
 */
public class TFIDFReducer extends Reducer<Text, Text, Text, Text> {

    private int totalDocs = 1;

    @Override
    protected void setup(Context context) {
        totalDocs = context.getConfiguration().getInt("totalDocs", 1);
    }

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        // Solo debe llegar 1 valor por palabra (el mapper lo emite así)
        String data = "";
        for (Text val : values) {
            data = val.toString();
            break;
        }

        String[] parts = data.split("\t", 2);
        if (parts.length != 2) return;

        int    numDocs  = Integer.parseInt(parts[0]);
        String posting  = parts[1];

        double idf = Math.log((double) totalDocs / numDocs);

        StringBuilder newPosting = new StringBuilder();
        for (String entry : posting.split("\\|")) {
            String[] campos = entry.split(":");
            if (campos.length < 4) continue;

            String docId      = campos[0];
            String frecuencia = campos[1];
            String total      = campos[2];
            double tf         = Double.parseDouble(campos[3]);
            double tfidf      = tf * idf;
            // título vacío por defecto; cámbialo si propagas el título desde BagOfWords
            String titulo = campos.length >= 5 ? campos[4] : "";

            if (newPosting.length() > 0) newPosting.append(";");
            newPosting.append(
                docId + "|" + frecuencia + "|" + total + "|" +
                String.format("%.6f", tf) + "|" +
                String.format("%.6f", idf) + "|" +
                String.format("%.6f", tfidf) + "|" + titulo
            );
        }

        context.write(key,
            new Text(String.format("%.6f", idf) + "\t" + numDocs + "\t" + newPosting));
    }
}