import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.filecache.DistributedCache;

public class TFIDFDriver {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Uso: TFIDFDriver <input tf> <input idf> <output tfidf>");
            System.exit(-1);
        }
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "TF-IDF");
        job.setJarByClass(TFIDFDriver.class);
        job.setMapperClass(TFIDFMapper.class);
        job.setNumReduceTasks(0); // No reducer, solo map
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        DistributedCache.addCacheFile(new Path(args[1]).toUri(), job.getConfiguration());
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
