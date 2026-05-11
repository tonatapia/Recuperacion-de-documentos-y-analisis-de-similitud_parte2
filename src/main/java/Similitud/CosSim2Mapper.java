package CosineSim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Usa el Distributed Cache para cargar TODOS los vectores de documentos.
 * Por cada documento del input calcula coseno con los documentos del cache
 * y emite los pares con similitud > 0.
 *
 * Salida: "docA\tdocB"  →  "tituloA\ttituloB\tcoseno"
 */
public class CosSim2Mapper extends Mapper<LongWritable, Text, Text, Text> {

    // docId → { palabra → tfidf }
    private Map<String, Map<String, Double>> allVectors = new HashMap<>();
    private Map<String, String> allTitulos = new HashMap<>();

    @Override
    protected void setup(Context context) throws IOException {
        URI[] cacheFiles = context.getCacheFiles();
        if (cacheFiles == null) return;

        for (URI uri : cacheFiles) {
            BufferedReader br = new BufferedReader(new FileReader(uri.getPath()));
            String line;
            while ((line = br.readLine()) != null) {
                parseLine(line);
            }
            br.close();
        }
    }

    private void parseLine(String line) {
        String[] parts = line.split("\t", 3);
        if (parts.length < 3) return;
        String docId  = parts[0];
        String titulo = parts[1];
        Map<String, Double> vec = new HashMap<>();
        for (String token : parts[2].split(" ")) {
            String[] kv = token.split(":");
            if (kv.length == 2) {
                try { vec.put(kv[0], Double.parseDouble(kv[1])); }
                catch (NumberFormatException ignored) {}
            }
        }
        allVectors.put(docId, vec);
        allTitulos.put(docId, titulo);
    }

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String line = value.toString().trim();
        if (line.isEmpty()) return;

        String[] parts = line.split("\t", 3);
        if (parts.length < 3) return;

        String docA   = parts[0];
        String tituloA = parts[1];
        Map<String, Double> vecA = new HashMap<>();
        for (String token : parts[2].split(" ")) {
            String[] kv = token.split(":");
            if (kv.length == 2) {
                try { vecA.put(kv[0], Double.parseDouble(kv[1])); }
                catch (NumberFormatException ignored) {}
            }
        }

        for (Map.Entry<String, Map<String, Double>> entry : allVectors.entrySet()) {
            String docB = entry.getKey();
            if (docB.compareTo(docA) <= 0) continue; // evita duplicados

            Map<String, Double> vecB = entry.getValue();
            double coseno = cosine(vecA, vecB);

            if (coseno > 0.0) {
                String tituloB = allTitulos.getOrDefault(docB, "");
                String pairKey = docA + "\t" + docB;
                String pairVal = String.format("%.6f", coseno) + "\t" + tituloA + "\t" + tituloB;
                context.write(new Text(pairKey), new Text(pairVal));
            }
        }
    }

    private double cosine(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0, normA = 0, normB = 0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            normA += e.getValue() * e.getValue();
            if (b.containsKey(e.getKey())) dot += e.getValue() * b.get(e.getKey());
        }
        for (double v : b.values()) normB += v * v;
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}