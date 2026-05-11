package IndiceInvertido;

import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Entrada:  palabra  →  [docId:freq:total:tf, ...]
 * Salida:   "palabra \t numDocs \t docId:freq:total:tf|docId:freq:total:tf|..."
 */
public class IndiceInvertidoReducer extends Reducer<Text, Text, Text, Text> {

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        StringBuilder postingList = new StringBuilder();
        int numDocs = 0;

        for (Text val : values) {
            if (postingList.length() > 0) postingList.append("|");
            postingList.append(val.toString());
            numDocs++;
        }

        // clave: palabra   valor: "numDocs\tposting_list"
        context.write(key, new Text(numDocs + "\t" + postingList.toString()));
    }
}