package TF;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;


public class TFReducer extends Reducer<Text, Text, Text, Text> {

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        String docId = key.toString();
        List<String[]> pares = new ArrayList<>();
        int totalPalabras = 0;

        for (Text val : values) {
            String[] kv = val.toString().split(":");
            if (kv.length != 2) continue;
            int freq = Integer.parseInt(kv[1]);
            totalPalabras += freq;
            pares.add(new String[]{ kv[0], kv[1] });
        }

        for (String[] par : pares) {
            String palabra    = par[0];
            int    frecuencia = Integer.parseInt(par[1]);
            double tf         = (double) frecuencia / totalPalabras;

            // clave: "palabra\tdocId"   valor: "frecuencia\ttotalPalabras\ttf"
            context.write(
                new Text(palabra + "\t" + docId),
                new Text(frecuencia + "\t" + totalPalabras + "\t" + String.format("%.6f", tf))
            );
        }
    }
}