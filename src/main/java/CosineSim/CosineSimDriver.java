package CosineSim;

import java.net.URI;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class CosineSimDriver {

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println(
                "Uso: CosineSimDriver <tfidf_input> <vectores_output> <coseno_output> <vectores_cache_path>");
            System.exit(-1);
        }

        //Job 1: construir vectores por documento
        Configuration conf1 = new Configuration();
        Job job1 = Job.getInstance(conf1, "Cosine Sim - Build Vectors");
        job1.setJarByClass(CosineSimDriver.class);
        job1.setMapperClass(CosSim1Mapper.class);
        job1.setReducerClass(CosSim1Reducer.class);
        job1.setOutputKeyClass(Text.class);
        job1.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job1, new Path(args[0]));
        FileOutputFormat.setOutputPath(job1, new Path(args[1]));
        if (!job1.waitForCompletion(true)) System.exit(1);

        //Job 2: calcular pares de similitud
        Configuration conf2 = new Configuration();
        Job job2 = Job.getInstance(conf2, "Cosine Sim - Pairwise");
        job2.setJarByClass(CosineSimDriver.class);
        job2.setMapperClass(CosSim2Mapper.class);
        job2.setReducerClass(CosSim2Reducer.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(Text.class);

        // El archivo de vectores va al Distributed Cache
        job2.addCacheFile(new URI(args[3] + "/part-r-00000"));

        FileInputFormat.addInputPath(job2, new Path(args[1]));
        FileOutputFormat.setOutputPath(job2, new Path(args[2]));
        System.exit(job2.waitForCompletion(true) ? 0 : 1);
    }
}