package TFIDF;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class TFIDFDriver {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Uso: TFIDFDriver <input> <output> <totalDocs>");
            System.exit(-1);
        }
        Configuration conf = new Configuration();
        conf.setInt("totalDocs", Integer.parseInt(args[2]));

        Job job = Job.getInstance(conf, "TF-IDF");
        job.setJarByClass(TFIDFDriver.class);
        job.setMapperClass(TFIDFMapper.class);
        job.setReducerClass(TFIDFReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}